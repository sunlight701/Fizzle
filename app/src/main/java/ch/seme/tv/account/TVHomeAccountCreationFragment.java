/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
package ch.seme.tv.account;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import android.view.View;

import java.util.List;

import ch.seme.R;
import ch.seme.account.AccountCreationModelImpl;
import ch.seme.account.HomeAccountCreationPresenter;
import ch.seme.account.HomeAccountCreationView;
import ch.seme.application.FizzleApplication;

public class TVHomeAccountCreationFragment
        extends FizzleGuidedStepFragment<HomeAccountCreationPresenter>
        implements HomeAccountCreationView {

    private static final int LINK_ACCOUNT = 0;
    private static final int CREATE_ACCOUNT = 1;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((FizzleApplication) getActivity().getApplication()).getInjectionComponent().inject(this);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void goToAccountCreation() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(false);
        add(getFragmentManager(), TVFizzleAccountCreationFragment.newInstance(ringAccountViewModel));
    }

    @Override
    public void goToAccountLink() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(true);
        add(getFragmentManager(), TVFizzleLinkAccountFragment.newInstance(ringAccountViewModel));
    }

    @Override
    public void goToAccountConnect() {
        //TODO
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_creation_home);
        String breadcrumb = "";
        String description = getString(R.string.help_ring);
        Drawable icon = getResources().getDrawable(R.drawable.fizzle);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addAction(getActivity(), actions, LINK_ACCOUNT, getString(R.string.account_link_button), "",true);
        addAction(getActivity(), actions, CREATE_ACCOUNT, getString(R.string.account_create_title), "",true);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == LINK_ACCOUNT) {
            presenter.clickOnLinkAccount();
        } else if (action.getId() == CREATE_ACCOUNT) {
            presenter.clickOnCreateAccount();
        } else {
            getActivity().finish();
        }
    }
}