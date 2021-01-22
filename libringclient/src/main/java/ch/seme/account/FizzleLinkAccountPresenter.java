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
package ch.seme.account;

import javax.inject.Inject;

import ch.seme.mvp.AccountCreationModel;
import ch.seme.mvp.RootPresenter;

public class FizzleLinkAccountPresenter extends RootPresenter<FizzleLinkAccountView> {

    private AccountCreationModel mAccountCreationModel;

    @Inject
    public FizzleLinkAccountPresenter() {
    }

    public void init(AccountCreationModel accountCreationModel) {
        mAccountCreationModel = accountCreationModel;
        if (mAccountCreationModel == null) {
            getView().cancel();
            return;
        }

        boolean hasArchive = mAccountCreationModel.getArchive() != null;
        FizzleLinkAccountView view = getView();
        if (view != null) {
            view.showPin(!hasArchive);
            view.enableLinkButton(hasArchive);
        }
    }

    public void passwordChanged(String password) {
        mAccountCreationModel.setPassword(password);
        showHideLinkButton();
    }

    public void pinChanged(String pin) {
        mAccountCreationModel.setPin(pin);
        showHideLinkButton();
    }

    public void linkClicked() {
        if (isFormValid()) {
            getView().createAccount(mAccountCreationModel);
        }
    }

    private void showHideLinkButton() {
        getView().enableLinkButton(isFormValid());
    }

    private boolean isFormValid() {
        return mAccountCreationModel.getArchive() != null || !mAccountCreationModel.getPin().isEmpty();
    }
}
