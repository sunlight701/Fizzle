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
package ch.seme.smartlist;

import java.util.List;

import ch.seme.model.CallContact;
import ch.seme.model.Uri;
import ch.seme.mvp.BaseView;

public interface SmartListView extends BaseView {

    void displayChooseNumberDialog(CharSequence numbers[]);

    void displayNoConversationMessage();

    void displayConversationDialog(SmartListViewModel smartListViewModel);

    void displayClearDialog(ch.seme.model.CallContact callContact);

    void displayDeleteDialog(ch.seme.model.CallContact callContact);

    void copyNumber(CallContact callContact);

    void setLoading(boolean display);

    void displayMenuItem();

    void hideList();

    void hideNoConversationMessage();

    void updateList(List<SmartListViewModel> smartListViewModels);
    void update(SmartListViewModel model);
    void update(int position);

    void goToConversation(String accountId, Uri contactId);

    void goToCallActivity(String accountId, String contactId);

    void goToQRFragment();

    void scrollToTop();

    void goToContactActivity(String accountId, String contactRingId);
}
