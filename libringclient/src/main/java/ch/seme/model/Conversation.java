/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
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

package ch.seme.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import ch.seme.model.Interaction.InteractionType;
import ch.seme.utils.Log;
import ch.seme.utils.Tuple;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Conversation extends ConversationHistory {


    private static final String TAG = Conversation.class.getSimpleName();

    private final String mAccountId;
    private final ch.seme.model.Uri mKey;
    private final List<ch.seme.model.CallContact> mContacts;

    private final NavigableMap<Long, ch.seme.model.Interaction> mHistory = new TreeMap<>();
    private final ArrayList<Conference> mCurrentCalls = new ArrayList<>();
    private final ArrayList<ch.seme.model.Interaction> mAggregateHistory = new ArrayList<>(32);
    private ch.seme.model.Interaction lastDisplayed = null;

    private final Subject<ch.seme.utils.Tuple<ch.seme.model.Interaction, ElementStatus>> updatedElementSubject = PublishSubject.create();
    private final Subject<ch.seme.model.Interaction> lastDisplayedSubject = BehaviorSubject.create();
    private final Subject<List<ch.seme.model.Interaction>> clearedSubject = PublishSubject.create();
    private final Subject<List<Conference>> callsSubject = BehaviorSubject.create();
    private final Subject<ch.seme.model.Account.ComposingStatus> composingStatusSubject = BehaviorSubject.createDefault(ch.seme.model.Account.ComposingStatus.Idle);
    private final Subject<Integer> color = BehaviorSubject.create();

    private Single<Conversation> isLoaded = null;

    // runtime flag set to true if the user is currently viewing this conversation
    private boolean mVisible = false;

    // indicate the list needs sorting
    private boolean mDirty = false;

    public Conversation(String accountId, ch.seme.model.CallContact contact) {
        mAccountId = accountId;
        mContacts = Collections.singletonList(contact);
        mKey = contact.getPrimaryUri();
        mParticipant = contact.getPrimaryUri().getUri();
    }

    public Conference getConference(String id) {
        for (Conference c : mCurrentCalls)
            if (c.getId().contentEquals(id) || c.getCallById(id) != null) {
                return c;
            }
        return null;
    }

    public void composingStatusChanged(ch.seme.model.CallContact contact, ch.seme.model.Account.ComposingStatus composing) {
        composingStatusSubject.onNext(composing);
    }

    public Uri getUri() {
        return mKey;
    }

    public CharSequence getDisplayName() {
        return mContacts.get(0).getDisplayName();
    }

    public void addContact(ch.seme.model.CallContact contact) {
        mContacts.add(contact);
    }

    public String getTitle() {
        if (mContacts.isEmpty()) {
            return null;
        } else if (mContacts.size() == 1) {
            return mContacts.get(0).getDisplayName();
        }
        StringBuilder ret = new StringBuilder();
        Iterator<ch.seme.model.CallContact> it = mContacts.iterator();
        while (it.hasNext()) {
            ch.seme.model.CallContact c = it.next();
            ret.append(c.getDisplayName());
            if (it.hasNext())
                ret.append(", ");
        }
        return ret.toString();
    }

    public String getUriTitle() {
        if (mContacts.isEmpty()) {
            return null;
        } else if (mContacts.size() == 1) {
            return mContacts.get(0).getDisplayName();
        }
        StringBuilder ret = new StringBuilder();
        Iterator<ch.seme.model.CallContact> it = mContacts.iterator();
        while (it.hasNext()) {
            ch.seme.model.CallContact c = it.next();
            ret.append(c.getDisplayName());
            if (it.hasNext())
                ret.append(", ");
        }
        return ret.toString();
    }

    public enum ElementStatus {
        UPDATE, REMOVE, ADD
    }

    public Observable<ch.seme.utils.Tuple<ch.seme.model.Interaction, ElementStatus>> getUpdatedElements() {
        return updatedElementSubject;
    }

    public Observable<ch.seme.model.Interaction> getLastDisplayed() {
        return lastDisplayedSubject;
    }

    public Observable<List<ch.seme.model.Interaction>> getCleared() {
        return clearedSubject;
    }

    public Observable<List<Conference>> getCalls() {
        return callsSubject;
    }

    public Observable<Account.ComposingStatus> getComposingStatus() {
        return composingStatusSubject;
    }

    public void addConference(final Conference conference) {
        if (conference == null) {
            return;
        }
        for (int i = 0; i < mCurrentCalls.size(); i++) {
            final Conference currentConference = mCurrentCalls.get(i);
            if (currentConference == conference) {
                return;
            }
            if (currentConference.getId().equals(conference.getId())) {
                mCurrentCalls.set(i, conference);
                return;
            }
        }
        mCurrentCalls.add(conference);
        callsSubject.onNext(mCurrentCalls);
    }

    public void removeConference(Conference c) {
        mCurrentCalls.remove(c);
        callsSubject.onNext(mCurrentCalls);
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setLoaded(Single<Conversation> loaded) {
        isLoaded = loaded;
    }

    public Single<Conversation> getLoaded() {
        return isLoaded;
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    public List<ch.seme.model.CallContact> getContacts() {
        return mContacts;
    }

    @Deprecated
    public ch.seme.model.CallContact getContact() {
        return mContacts.get(0);
    }

    public void addCall(ch.seme.model.SipCall call) {
        if (getCallHistory().contains(call)) {
            return;
        }
        mDirty = true;
        mAggregateHistory.add(call);
        updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(call, ElementStatus.ADD));
    }

    public void addTextMessage(TextMessage txt) {
        if (mVisible) {
            txt.read();
        }
        if (txt.getConversation() == null) {
            ch.seme.utils.Log.e(TAG, "Error in conversation class... No conversation is attached to this interaction");
        }
        if (txt.getContact() == null) {
            txt.setContact(getContact());
        }
        mHistory.put(txt.getTimestamp(), txt);
        mDirty = true;
        mAggregateHistory.add(txt);
        updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(txt, ElementStatus.ADD));
    }

    public void addRequestEvent(TrustRequest request) {
        ch.seme.model.ContactEvent event = new ch.seme.model.ContactEvent(getContact(), request);
        mDirty = true;
        mAggregateHistory.add(event);
        updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(event, ElementStatus.ADD));
    }

    public void addContactEvent() {
        ch.seme.model.ContactEvent event = new ch.seme.model.ContactEvent(getContact());
        mDirty = true;
        mAggregateHistory.add(event);
        updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(event, ElementStatus.ADD));
    }

    public void addContactEvent(ch.seme.model.ContactEvent contactEvent) {
        mDirty = true;
        mAggregateHistory.add(contactEvent);
        updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(contactEvent, ElementStatus.ADD));
    }

    public void addFileTransfer(ch.seme.model.DataTransfer dataTransfer) {
        if (mAggregateHistory.contains(dataTransfer)) {
            return;
        }
        mDirty = true;
        mAggregateHistory.add(dataTransfer);
        updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(dataTransfer, ElementStatus.ADD));
    }

    public void updateTextMessage(TextMessage text) {
        text.setContact(getContact());
        long time = text.getTimestamp();
        NavigableMap<Long, ch.seme.model.Interaction> msgs = mHistory.subMap(time, true, time, true);
        for (ch.seme.model.Interaction txt : msgs.values()) {
            if (txt.getId() == text.getId()) {
                txt.setStatus(text.getStatus());
                updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(txt, ElementStatus.UPDATE));
                if (text.getStatus() == ch.seme.model.Interaction.InteractionStatus.DISPLAYED) {
                    if (lastDisplayed == null || lastDisplayed.getTimestamp() < text.getTimestamp()) {
                        lastDisplayed = text;
                        lastDisplayedSubject.onNext(text);
                    }
                }
                return;
            }
        }
        Log.e(TAG, "Can't find message to update: " + text.getId());
    }

    public ArrayList<ch.seme.model.Interaction> getAggregateHistory() {
        return mAggregateHistory;
    }

    private final Single<List<ch.seme.model.Interaction>> sortedHistory = Single.fromCallable(() -> {
        sortHistory();
        return mAggregateHistory;
    });

    public void sortHistory() {
        if (mDirty) {
            synchronized (mAggregateHistory) {
                Collections.sort(mAggregateHistory, (c1, c2) -> Long.compare(c1.getTimestamp(), c2.getTimestamp()));
            }
            mDirty = false;
        }
    }

    public Single<List<ch.seme.model.Interaction>> getSortedHistory() {
        return sortedHistory;
    }

    public ch.seme.model.Interaction getLastEvent() {
        sortHistory();
        if (mAggregateHistory.isEmpty())
            return null;
        return mAggregateHistory.get(mAggregateHistory.size() - 1);
    }

    public Conference getCurrentCall() {
        if (mCurrentCalls.isEmpty()) {
            return null;
        }
        return mCurrentCalls.get(0);
    }

    public ArrayList<Conference> getCurrentCalls() {
        return mCurrentCalls;
    }

    public Collection<ch.seme.model.SipCall> getCallHistory() {
        List<ch.seme.model.SipCall> result = new ArrayList<>();
        for (ch.seme.model.Interaction interaction : mAggregateHistory) {
            if (interaction.getType() == InteractionType.CALL) {
                result.add((ch.seme.model.SipCall) interaction);
            }
        }
        return result;
    }

    public TreeMap<Long, TextMessage> getUnreadTextMessages() {
        TreeMap<Long, TextMessage> texts = new TreeMap<>();
        for (Map.Entry<Long, ch.seme.model.Interaction> entry : mHistory.descendingMap().entrySet()) {
            ch.seme.model.Interaction value = entry.getValue();
            if (value.getType() == InteractionType.TEXT) {
                TextMessage message = (TextMessage) value;
                if (message.isRead())
                    break;
                texts.put(entry.getKey(), message);
            }
        }
        return texts;
    }

    public NavigableMap<Long, ch.seme.model.Interaction> getRawHistory() {
        return mHistory;
    }


    private ch.seme.model.Interaction findConversationElement(int transferId) {
        for (ch.seme.model.Interaction interaction : mAggregateHistory) {
            if (interaction != null && interaction.getType() == (InteractionType.DATA_TRANSFER)) {
                if (transferId == (interaction.getId())) {
                    return interaction;
                }
            }
        }
        return null;
    }


    private boolean removeInteraction(long interactionId) {
        Iterator<ch.seme.model.Interaction> it = mAggregateHistory.iterator();
        while (it.hasNext()) {
            ch.seme.model.Interaction interaction = it.next();
            Integer id = interaction == null ? null : interaction.getId();
            if (id != null && interactionId == id) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the conversation cache.
     * @param delete true if you do not want to re-add contact events
     */
    public void clearHistory(boolean delete) {
        mAggregateHistory.clear();
        mHistory.clear();
        mDirty = false;
        if(!delete)
            mAggregateHistory.add(new ch.seme.model.ContactEvent(getContact()));
        clearedSubject.onNext(mAggregateHistory);
    }

    static private ch.seme.model.Interaction getTypedInteraction(ch.seme.model.Interaction interaction) {
        switch (interaction.getType()) {
            case TEXT:
                return new TextMessage(interaction);
            case CALL:
                return new ch.seme.model.SipCall(interaction);
            case CONTACT:
                return new ch.seme.model.ContactEvent(interaction);
            case DATA_TRANSFER:
                return new ch.seme.model.DataTransfer(interaction);
        }
        return interaction;
    }

    public void setHistory(List<ch.seme.model.Interaction> loadedConversation) {
        mAggregateHistory.ensureCapacity(loadedConversation.size());
        ch.seme.model.Interaction last = null;
        for (ch.seme.model.Interaction i : loadedConversation) {
            ch.seme.model.Interaction interaction = getTypedInteraction(i);
            interaction.setAccount(mAccountId);
            interaction.setContact(getContact());
            mAggregateHistory.add(interaction);
            mHistory.put(interaction.getTimestamp(), interaction);
            if (!i.isIncoming() && i.getStatus() == ch.seme.model.Interaction.InteractionStatus.DISPLAYED)
                last = i;
        }
        if (last != null) {
            lastDisplayed = last;
            lastDisplayedSubject.onNext(last);
        }
        mDirty = false;
    }

    public void addElement(ch.seme.model.Interaction interaction) {
        interaction.setAccount(mAccountId);
        interaction.setContact(getContact());
        if (interaction.getType() == InteractionType.TEXT) {
            TextMessage msg = new TextMessage(interaction);
            addTextMessage(msg);
        } else if (interaction.getType() == InteractionType.CALL) {
            ch.seme.model.SipCall call = new SipCall(interaction);
            addCall(call);
        } else if (interaction.getType() == InteractionType.CONTACT) {
            ch.seme.model.ContactEvent event = new ContactEvent(interaction);
            addContactEvent(event);
        } else if (interaction.getType() == InteractionType.DATA_TRANSFER) {
            ch.seme.model.DataTransfer dataTransfer = new ch.seme.model.DataTransfer(interaction);
            addFileTransfer(dataTransfer);
        }
    }

    public void updateFileTransfer(ch.seme.model.DataTransfer transfer, ch.seme.model.Interaction.InteractionStatus eventCode) {
        ch.seme.model.DataTransfer dataTransfer = (DataTransfer) findConversationElement(transfer.getId());
        if (dataTransfer != null) {
            dataTransfer.setStatus(eventCode);
            updatedElementSubject.onNext(new ch.seme.utils.Tuple<>(dataTransfer, ElementStatus.UPDATE));
        }
    }

    public void removeInteraction(Interaction interaction) {
        if (removeInteraction(interaction.getId()))
            updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.REMOVE));
    }


    public void removeAll() {
        mAggregateHistory.clear();
        mCurrentCalls.clear();
        mHistory.clear();
        mDirty = true;
    }

    public void setColor(int c) {
        color.onNext(c);
    }

    public Observable<Integer> getColor() {
        return color;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public interface ConversationActionCallback {

        void removeConversation(ch.seme.model.CallContact callContact);

        void clearConversation(CallContact callContact);

        void copyContactNumberToClipboard(String contactNumber);

    }

}
