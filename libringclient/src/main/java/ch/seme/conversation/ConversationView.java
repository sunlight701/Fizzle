/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
import java.util.List;

import ch.seme.model.Account;
import ch.seme.model.CallContact;
import ch.seme.model.Conversation;
import ch.seme.model.DataTransfer;
import ch.seme.model.Error;
import ch.seme.model.Interaction;
import ch.seme.model.Uri;
import ch.seme.mvp.BaseView;

public interface ConversationView extends BaseView {

    void refreshView(List<ch.seme.model.Interaction> conversation);

    void scrollToEnd();

    void displayContact(ch.seme.model.CallContact contact);

    void displayOnGoingCallPane(boolean display);

    void displayNumberSpinner(Conversation conversation, Uri number);

    void displayErrorToast(Error error);

    void hideNumberSpinner();

    void clearMsgEdit();

    void goToHome();

    void goToAddContact(CallContact callContact);

    void goToCallActivity(String conferenceId);

    void goToCallActivityWithResult(String accountId, String contactRingId, boolean audioOnly);

    void goToContactActivity(String accountId, String contactRingId);

    void switchToUnknownView(String name);

    void switchToIncomingTrustRequestView(String message);

    void switchToConversationView();

    void askWriteExternalStoragePermission();

    void openFilePicker();

    void shareFile(File path);

    void openFile(File path);

    void addElement(ch.seme.model.Interaction e);
    void updateElement(ch.seme.model.Interaction e);
    void removeElement(ch.seme.model.Interaction e);
    void setComposingStatus(Account.ComposingStatus composingStatus);
    void setLastDisplayed(Interaction interaction);

    void setConversationColor(int integer);

    void startSaveFile(DataTransfer currentFile, String fileAbsolutePath);

    void startShareLocation(String accountId, String contactId);

    void showMap(String accountId, String contactId, boolean open);
    void hideMap();

    void hideErrorPanel();

    void displayNetworkErrorPanel();

    void setReadIndicatorStatus(boolean show);

}
