/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package ch.seme.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import ch.seme.model.Account;
import ch.seme.model.CallContact;
import ch.seme.model.Settings;
import ch.seme.model.Uri;
import ch.seme.utils.Log;
import ch.seme.utils.StringUtils;
import ezvcard.VCard;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * This service handles the contacts
 * - Load the contacts stored in the system
 * - Keep a local cache of the contacts
 * - Provide query tools to search contacts by id, number, ...
 */
public abstract class ContactService {
    private final static String TAG = ContactService.class.getSimpleName();

    @Inject
    PreferencesService mPreferencesService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    AccountService mAccountService;

    public abstract Map<Long, ch.seme.model.CallContact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);

    protected abstract ch.seme.model.CallContact findContactByIdFromSystem(Long contactId, String contactKey);
    protected abstract ch.seme.model.CallContact findContactBySipNumberFromSystem(String number);
    protected abstract ch.seme.model.CallContact findContactByNumberFromSystem(String number);

    public abstract Completable loadContactData(ch.seme.model.CallContact callContact, String accountId);

    public abstract void saveVCardContactData(ch.seme.model.CallContact contact, String accountId, VCard vcard);
    public abstract Single<VCard> saveVCardContact(String accountId, String uri, String displayName, String pictureB64);

    public ContactService() {}

    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    public Single<Map<Long, ch.seme.model.CallContact>> loadContacts(final boolean loadRingContacts, final boolean loadSipContacts, final ch.seme.model.Account account) {
        return Single.fromCallable(() -> {
            Settings settings = mPreferencesService.getSettings();
            if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {
                return loadContactsFromSystem(loadRingContacts, loadSipContacts);
            }
            return new HashMap<>();
        });
    }

    public Observable<ch.seme.model.CallContact> observeContact(String accountId, ch.seme.model.CallContact contact, boolean withPresence) {
        ch.seme.model.Uri uri = contact.getPrimaryUri();
        String uriString = uri.getRawUriString();
        synchronized (contact) {
            if (contact.getPresenceUpdates() == null) {
                contact.setPresenceUpdates(Observable.<Boolean>create(emitter -> {
                    emitter.onNext(false);
                    contact.setPresenceEmitter(emitter);
                    mAccountService.subscribeBuddy(accountId, uriString, true);
                    emitter.setCancellable(() -> {
                        mAccountService.subscribeBuddy(accountId, uriString, false);
                        contact.setPresenceEmitter(null);
                        emitter.onNext(false);
                    });
                })
                        .replay(1)
                        .refCount(5, TimeUnit.SECONDS));
            }

            if (contact.getUpdates() == null) {
                contact.setUpdates(contact.getUpdatesSubject()
                        .doOnSubscribe(d -> {
                            if (!contact.isUsernameLoaded())
                                mAccountService.lookupAddress(accountId, "", uri.getRawRingId());
                            loadContactData(contact, accountId)
                                    .subscribe(() -> {}, e -> {/*Log.e(TAG, "Error loading contact data: " + e.getMessage())*/});
                        })
                        .filter(c -> c.isUsernameLoaded() && c.detailsLoaded)
                        .replay(1)
                        .refCount(5, TimeUnit.SECONDS));
            }

            return withPresence
                    ? Observable.combineLatest(contact.getUpdates(), contact.getPresenceUpdates(), (c, p) -> c)
                    : contact.getUpdates();
        }
    }

    public Observable<List<ch.seme.model.CallContact>> observeContact(String accountId, List<ch.seme.model.CallContact> contacts, boolean withPresence) {
        if (contacts.size() == 1) {
            return observeContact(accountId, contacts.get(0), withPresence).map(Collections::singletonList);
        } else {
            List<Observable<ch.seme.model.CallContact>> observables = new ArrayList<>(contacts.size());
            for (ch.seme.model.CallContact contact : contacts)
                observables.add(observeContact(accountId, contact, false));
            return Observable.combineLatest(observables, a -> {
                List<ch.seme.model.CallContact> obs = new ArrayList<>(a.length);
                for (Object o : a)
                    obs.add((ch.seme.model.CallContact) o);
                return obs;
            });
        }
    }

    public Single<ch.seme.model.CallContact> getLoadedContact(String accountId, ch.seme.model.CallContact contact) {
        return observeContact(accountId, contact, false)
                .filter(c -> c.isUsernameLoaded() && c.detailsLoaded)
                .firstOrError();
    }

    public Single<List<ch.seme.model.CallContact>> getLoadedContact(String accountId, List<ch.seme.model.CallContact> contacts) {
        return observeContact(accountId, contacts, false)
                .filter(cts -> {
                    for (ch.seme.model.CallContact c : cts) {
                        if (!c.isUsernameLoaded() || !c.detailsLoaded)
                            return false;
                    }
                    return true;
                })
                .firstOrError();
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @return The found/created contact
     */
    public ch.seme.model.CallContact findContactByNumber(ch.seme.model.Account account, String number) {
        if (StringUtils.isEmpty(number) || account == null) {
            return null;
        }
        return findContact(account, new ch.seme.model.Uri(number));
    }

    public ch.seme.model.CallContact findContact(Account account, Uri uri) {
        if (uri == null || account == null) {
            return null;
        }

        CallContact contact = account.getContactFromCache(uri);
        // TODO load system contact info into SIP contact
        if (account.isSip()) {
            loadContactData(contact, account.getAccountID()).subscribe(() -> {}, e -> Log.e(TAG, "Can't load contact data"));
        }
        return contact;
    }
}