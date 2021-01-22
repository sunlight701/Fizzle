/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

import ch.seme.model.Account;
import ch.seme.model.CallContact;
import ch.seme.model.Conference;
import ch.seme.model.Conversation;
import ch.seme.model.DataTransfer;
import ch.seme.model.SipCall;
import ch.seme.model.Uri;

public interface NotificationService {
    String TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID = "trustRequestNotificationAccountId";
    String TRUST_REQUEST_NOTIFICATION_FROM = "trustRequestNotificationFrom";
    String KEY_CALL_ID = "callId";
    String KEY_HOLD_ID = "holdId";
    String KEY_END_ID = "endId";
    String KEY_NOTIFICATION_ID = "notificationId";

    Object showCallNotification(int callId);

    void showTextNotification(String accountId, Conversation conversation);

    void cancelCallNotification();

    void cancelTextNotification(Uri contact);

    void cancelTextNotification(String ringId);

    void cancelAll();

    void showIncomingTrustRequestNotification(ch.seme.model.Account account);

    void cancelTrustRequestNotification(String accountID);

    void showFileTransferNotification(ch.seme.model.DataTransfer info, ch.seme.model.CallContact contact);

    void showMissedCallNotification(SipCall call);

    void cancelFileNotification(int id, boolean isMigratingToService);

    void updateNotification(Object notification, int notificationId);

    Object getServiceNotification();

    void handleCallNotification(Conference conference, boolean remove);

    void handleDataTransferNotification(DataTransfer transfer, ch.seme.model.CallContact contact, boolean remove);

    void removeTransferNotification(long transferId);

    Object getDataTransferNotification(int notificationId);

    void onConnectionUpdate(Boolean b);

    void showLocationNotification(ch.seme.model.Account first, ch.seme.model.CallContact contact);
    void cancelLocationNotification(Account first, CallContact contact);

}
