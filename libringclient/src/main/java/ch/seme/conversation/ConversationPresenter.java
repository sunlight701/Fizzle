/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package ch.seme.conversation;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import ch.seme.facades.ConversationFacade;
import ch.seme.model.Account;
import ch.seme.model.CallContact;
import ch.seme.model.Conference;
import ch.seme.model.Conversation;
import ch.seme.model.DataTransfer;
import ch.seme.model.Error;
import ch.seme.model.Interaction;
import ch.seme.model.SipCall;
import ch.seme.model.TrustRequest;
import ch.seme.model.Uri;
import ch.seme.mvp.RootPresenter;
import ch.seme.utils.Log;
import ch.seme.utils.StringUtils;
import ch.seme.utils.Tuple;
import ch.seme.utils.VCardUtils;
import cx.ring.daemon.Blob;
import ch.seme.services.AccountService;
import ch.seme.services.ContactService;
import ch.seme.services.DeviceRuntimeService;
import ch.seme.services.HardwareService;
import ch.seme.services.PreferencesService;
import ch.seme.services.VCardService;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class ConversationPresenter extends RootPresenter<ch.seme.conversation.ConversationView> {

    private static final String TAG = ConversationPresenter.class.getSimpleName();
    private final ContactService mContactService;
    private final AccountService mAccountService;
    private final HardwareService mHardwareService;
    private final ch.seme.facades.ConversationFacade mConversationFacade;
    private final VCardService mVCardService;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final PreferencesService mPreferencesService;

    private ch.seme.model.Conversation mConversation;
    private ch.seme.model.Uri mContactUri;
    private String mAccountId;

    private CompositeDisposable mConversationDisposable;
    private final CompositeDisposable mVisibilityDisposable = new CompositeDisposable();

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private final Subject<ch.seme.model.Conversation> mConversationSubject = BehaviorSubject.create();

    @Inject
    public ConversationPresenter(ContactService contactService,
                                 AccountService accountService,
                                 HardwareService hardwareService,
                                 ConversationFacade conversationFacade,
                                 VCardService vCardService,
                                 DeviceRuntimeService deviceRuntimeService, PreferencesService preferencesService) {
        mContactService = contactService;
        mAccountService = accountService;
        mHardwareService = hardwareService;
        mConversationFacade = conversationFacade;
        mVCardService = vCardService;
        mDeviceRuntimeService = deviceRuntimeService;
        mPreferencesService = preferencesService;
    }

    @Override
    public void bindView(ch.seme.conversation.ConversationView view) {
        super.bindView(view);
        mCompositeDisposable.add(mVisibilityDisposable);
        if (mConversationDisposable == null && mConversation != null)
            initView(mConversation, view);
    }

    public void init(ch.seme.model.Uri contactRingId, String accountId) {
        ch.seme.utils.Log.w(TAG, "init " + contactRingId + " " + accountId);
        mContactUri = contactRingId;
        mAccountId = accountId;
        ch.seme.model.Account account = mAccountService.getAccount(accountId);
        if (account != null) {
            initContact(account, contactRingId, getView());
            mCompositeDisposable.add(mConversationFacade.loadConversationHistory(account, contactRingId)
                    .observeOn(mUiScheduler)
                    .subscribe(this::setConversation, e -> getView().goToHome()));
        } else {
            getView().goToHome();
            return;
        }

        mCompositeDisposable.add(Observable.combineLatest(
                mHardwareService.getConnectivityState(),
                mAccountService.getObservableAccount(account),
                (isConnected, a) -> isConnected || a.isRegistered())
                .observeOn(mUiScheduler)
                .subscribe(isOk -> {
                    ch.seme.conversation.ConversationView view = getView();
                    if (view != null) {
                        if (isOk)
                            view.hideErrorPanel();
                        else
                            view.displayNetworkErrorPanel();
                    }
                }));

        getView().setReadIndicatorStatus(setReadIndicatorStatus());
    }

    private void setConversation(final ch.seme.model.Conversation conversation) {
        if (conversation == null || mConversation == conversation)
            return;
        mConversation = conversation;
        mConversationSubject.onNext(conversation);
        ch.seme.conversation.ConversationView view = getView();
        if (view != null)
            initView(conversation, view);
    }

    public void pause() {
        mVisibilityDisposable.clear();
        if (mConversation != null) {
            mConversation.setVisible(false);
        }
    }

    public void resume(boolean isBubble) {
        ch.seme.utils.Log.w(TAG, "resume " + mConversation + " " + mAccountId + " " + mContactUri);
        mVisibilityDisposable.clear();
        mVisibilityDisposable.add(mConversationSubject
                .firstOrError()
                .subscribe(conversation -> {
                    conversation.setVisible(true);
                    updateOngoingCallView(conversation);
                    mConversationFacade.readMessages(mAccountService.getAccount(mAccountId), conversation, !isBubble);
                }, e -> ch.seme.utils.Log.e(TAG, "Error loading conversation", e)));
    }

    private ch.seme.model.CallContact initContact(final ch.seme.model.Account account, final ch.seme.model.Uri uri,
                                                  final ch.seme.conversation.ConversationView view) {
        ch.seme.model.CallContact contact;
        if (account.isFizzle()) {
            String rawId = uri.getRawRingId();
            contact = account.getContact(rawId);
            if (contact == null) {
                contact = account.getContactFromCache(uri);
                TrustRequest req = account.getRequest(uri);
                if (req == null) {
                    view.switchToUnknownView(contact.getRingUsername());
                } else {
                    view.switchToIncomingTrustRequestView(req.getDisplayname());
                }
            } else {
                view.switchToConversationView();
            }
            ch.seme.utils.Log.w(TAG, "initContact " + contact.getUsername());
            if (contact.getUsername() == null) {
                mAccountService.lookupAddress(mAccountId, "", rawId);
            }
        } else {
            contact = mContactService.findContact(account, uri);
            view.switchToConversationView();
        }
        view.displayContact(contact);
        return contact;
    }

    private void initView(final ch.seme.model.Conversation c, final ch.seme.conversation.ConversationView view) {
        ch.seme.utils.Log.w(TAG, "initView");
        if (mConversationDisposable == null) {
            mConversationDisposable = new CompositeDisposable();
            mCompositeDisposable.add(mConversationDisposable);
        }
        mConversationDisposable.clear();
        view.hideNumberSpinner();

        Account account = mAccountService.getAccount(mAccountId);


        mConversationDisposable.add(c.getSortedHistory()
                .subscribe(view::refreshView, e -> ch.seme.utils.Log.e(TAG, "Can't update element", e)));
        mConversationDisposable.add(c.getCleared()
                .observeOn(mUiScheduler)
                .subscribe(view::refreshView, e -> ch.seme.utils.Log.e(TAG, "Can't update elements", e)));
        mConversationDisposable.add(mContactService.getLoadedContact(c.getAccountId(), c.getContact())
                .observeOn(mUiScheduler)
                .subscribe(contact -> initContact(account, mContactUri, view), e -> ch.seme.utils.Log.e(TAG, "Can't get contact", e)));
        mConversationDisposable.add(c.getUpdatedElements()
                .observeOn(mUiScheduler)
                .subscribe(elementTuple -> {
                    switch(elementTuple.second) {
                        case ADD:
                            view.addElement(elementTuple.first);
                            break;
                        case UPDATE:
                            view.updateElement(elementTuple.first);
                            break;
                        case REMOVE:
                            view.removeElement(elementTuple.first);
                            break;
                    }
                }, e -> ch.seme.utils.Log.e(TAG, "Can't update element", e)));
        if (showTypingIndicator()) {
            mConversationDisposable.add(c.getComposingStatus()
                    .observeOn(mUiScheduler)
                    .subscribe(view::setComposingStatus));
        }
        mConversationDisposable.add(c.getLastDisplayed()
                .observeOn(mUiScheduler)
                .subscribe(view::setLastDisplayed));
        mConversationDisposable.add(c.getCalls()
                .observeOn(mUiScheduler)
                .subscribe(calls -> updateOngoingCallView(mConversation), e -> ch.seme.utils.Log.e(TAG, "Can't update call view", e)));
        mConversationDisposable.add(c.getColor()
                .observeOn(mUiScheduler)
                .subscribe(view::setConversationColor, e -> ch.seme.utils.Log.e(TAG, "Can't update conversation color", e)));

        ch.seme.utils.Log.e(TAG, "getLocationUpdates subscribe");
        mConversationDisposable.add(account
                .getLocationUpdates(c.getContact().getPrimaryUri())
                .observeOn(mUiScheduler)
                .subscribe(u -> {
                    ch.seme.utils.Log.e(TAG, "getLocationUpdates: update");
                    getView().showMap(c.getAccountId(), c.getContact().getPrimaryUri().getUri(), false);
                }));
    }

    public void openContact() {
        if (mConversation != null)
            getView().goToContactActivity(mAccountId, mConversation.getContact().getPrimaryNumber());
    }

    public void sendTextMessage(String message) {
        if (StringUtils.isEmpty(message) || mConversation == null) {
            return;
        }
        ch.seme.model.Conference conference = mConversation.getCurrentCall();
        if (conference == null || !conference.isOnGoing()) {
            mConversationFacade.sendTextMessage(mAccountId, mConversation, mContactUri, message).subscribe();
        } else {
            mConversationFacade.sendTextMessage(mConversation, conference, message);
        }
    }

    public void selectFile() {
        getView().openFilePicker();
    }

    public void sendFile(File file) {
        mConversationFacade.sendFile(mAccountId, mContactUri, file).subscribe();
    }

    /**
     * Gets the absolute path of the file dataTransfer and sends both the DataTransfer and the
     * found path to the ConversationView in order to start saving the file
     *
     * @param interaction an interaction representing a datat transfer
     */
    public void saveFile(ch.seme.model.Interaction interaction) {
        ch.seme.model.DataTransfer transfer = (ch.seme.model.DataTransfer) interaction;
        String fileAbsolutePath = getDeviceRuntimeService().
                getConversationPath(transfer.getPeerId(), transfer.getStoragePath())
                .getAbsolutePath();
        getView().startSaveFile(transfer, fileAbsolutePath);
    }

    public void shareFile(ch.seme.model.Interaction interaction) {
        ch.seme.model.DataTransfer file = (ch.seme.model.DataTransfer) interaction;
        File path = getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
        getView().shareFile(path);
    }

    public void openFile(ch.seme.model.Interaction interaction) {
        ch.seme.model.DataTransfer file = (DataTransfer) interaction;
        File path = getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
        getView().openFile(path);
    }

    public void deleteConversationItem(ch.seme.model.Interaction element) {
        mConversationFacade.deleteConversationItem(element);
    }

    public void cancelMessage(Interaction message) {
        mConversationFacade.cancelMessage(message);
    }

    private void sendTrustRequest() {
        final String accountId = mAccountId;
        final Uri contactId = mContactUri;
        ch.seme.model.CallContact contact = mContactService.findContact(mAccountService.getAccount(accountId), contactId);
        if (contact != null) {
            contact.setStatus(CallContact.Status.REQUEST_SENT);
        }
        mVCardService.loadSmallVCard(accountId, VCardService.MAX_SIZE_REQUEST)
                .subscribeOn(Schedulers.computation())
                .subscribe(vCard -> mAccountService.sendTrustRequest(accountId, contactId.getRawRingId(), Blob.fromString(VCardUtils.vcardToString(vCard))),
                        e -> mAccountService.sendTrustRequest(accountId, contactId.getRawRingId(), null));
    }

    public void clickOnGoingPane() {
        ch.seme.model.Conference conf = mConversation == null ? null : mConversation.getCurrentCall();
        if (conf != null) {
            getView().goToCallActivity(conf.getId());
        } else {
            getView().displayOnGoingCallPane(false);
        }
    }

    public void goToCall(boolean audioOnly) {
        if (audioOnly && !mHardwareService.hasMicrophone()) {
            getView().displayErrorToast(ch.seme.model.Error.NO_MICROPHONE);
            return;
        }

        mCompositeDisposable.add(mConversationSubject
                .firstElement()
                .subscribe(conversation -> {
                    ConversationView view = getView();
                    if (view != null) {
                        ch.seme.model.Conference conf = mConversation.getCurrentCall();
                        if (conf != null
                                && !conf.getParticipants().isEmpty()
                                && conf.getParticipants().get(0).getCallStatus() != ch.seme.model.SipCall.CallStatus.INACTIVE
                                && conf.getParticipants().get(0).getCallStatus() != ch.seme.model.SipCall.CallStatus.FAILURE) {
                            view.goToCallActivity(conf.getId());
                        } else {
                            view.goToCallActivityWithResult(mAccountId, mContactUri.getRawUriString(), audioOnly);
                        }
                    }
                }));
    }

    private void updateOngoingCallView(Conversation conversation) {
        Conference conf = conversation == null ? null : conversation.getCurrentCall();
        if (conf != null && (conf.getState() == ch.seme.model.SipCall.CallStatus.CURRENT || conf.getState() == ch.seme.model.SipCall.CallStatus.HOLD || conf.getState() == SipCall.CallStatus.RINGING)) {
            getView().displayOnGoingCallPane(true);
        } else {
            getView().displayOnGoingCallPane(false);
        }
    }

    public void onBlockIncomingContactRequest() {
        String accountId = mAccountId == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountId;
        mConversationFacade.discardRequest(accountId, mContactUri);
        mAccountService.removeContact(accountId, mContactUri.getHost(), true);

        getView().goToHome();
    }

    public void onRefuseIncomingContactRequest() {
        String accountId = mAccountId == null ? mAccountService.getCurrentAccount().getAccountID() : mAccountId;

        mConversationFacade.discardRequest(accountId, mContactUri);
        getView().goToHome();
    }

    public void onAcceptIncomingContactRequest() {
        mConversationFacade.acceptRequest(mAccountId, mContactUri);
        getView().switchToConversationView();
    }

    public void onAddContact() {
        sendTrustRequest();
        getView().switchToConversationView();
    }

    public DeviceRuntimeService getDeviceRuntimeService() {
        return mDeviceRuntimeService;
    }

    public void noSpaceLeft() {
        Log.e(TAG, "configureForFileInfoTextMessage: no space left on device");
        getView().displayErrorToast(Error.NO_SPACE_LEFT);
    }

    public void setConversationColor(int color) {
        mCompositeDisposable.add(mConversationSubject
                .firstElement()
                .subscribe(conversation -> conversation.setColor(color)));
    }

    public void cameraPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.isVideoAvailable()) {
            mHardwareService.initVideo()
                    .onErrorComplete()
                    .subscribe();
        }
    }

    public void shareLocation() {
        getView().startShareLocation(mAccountId, mContactUri.getUri());
    }

    public ch.seme.utils.Tuple<String, String> getPath() {
        return new Tuple<>(mAccountId, mContactUri.getUri());
    }

    public void onComposingChanged(boolean hasMessage) {
        if (mConversation == null || !showTypingIndicator()) {
            return;
        }
        mConversationFacade.setIsComposing(mAccountId, mContactUri, hasMessage);
    }

    public boolean showTypingIndicator() {
        return mPreferencesService.getSettings().isAllowTypingIndicator();
    }

    private boolean setReadIndicatorStatus() {
        return mPreferencesService.getSettings().isAllowReadIndicator();
    }

}
