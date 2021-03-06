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

package ch.seme.navigation;

import ch.seme.model.Account;

public class HomeNavigationViewModel {
    final private ch.seme.model.Account mAccount;
    final private String mAlias;

    public HomeNavigationViewModel(ch.seme.model.Account account, String alias) {
        mAccount = account;
        mAlias = alias;
    }

    public Account getAccount() {
        return mAccount;
    }

    public String getAlias() {
        return mAlias;
    }
}
