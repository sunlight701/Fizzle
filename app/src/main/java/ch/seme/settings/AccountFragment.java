/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author:     AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.seme.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import ch.seme.R;
import ch.seme.account.AccountEditionFragment;
import ch.seme.account.FizzleAccountSummaryFragment;
import ch.seme.application.FizzleApplication;
import ch.seme.client.HomeActivity;
import ch.seme.databinding.FragAccountBinding;
import ch.seme.model.Account;
import ch.seme.services.AccountService;

/**
 * TODO: improvements : handle multiples permissions for feature.
 */
public class AccountFragment extends Fragment implements ViewTreeObserver.OnScrollChangedListener {

    private static final int SCROLL_DIRECTION_UP = -1;

    public static AccountFragment newInstance(@NonNull String accountId) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        AccountFragment accountFragment = new AccountFragment();
        accountFragment.setArguments(bundle);
        return accountFragment;
    }

    private FragAccountBinding mBinding;
    private Account mAccount;

    @Inject
    AccountService mAccountService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccountBinding.inflate(inflater, container, false);
        ((FizzleApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        String accountId = getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY);
        mAccount = mAccountService.getAccount(accountId);

        mBinding.settingsChangePassword.setVisibility(mAccount.hasManager() ? View.GONE : View.VISIBLE);
        mBinding.settingsExport.setVisibility(mAccount.hasManager() ? View.GONE : View.VISIBLE);
        mBinding.systemChangePasswordTitle.setText(mAccount.hasPassword()? R.string.account_password_change : R.string.account_password_set);
        mBinding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        mBinding.settingsChangePassword.setOnClickListener(v -> ((FizzleAccountSummaryFragment) getParentFragment()).onPasswordChangeAsked());
        mBinding.settingsExport.setOnClickListener(v -> ((FizzleAccountSummaryFragment) getParentFragment()).onClickExport());
        mBinding.settingsDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog deleteDialog = createDeleteDialog();
                deleteDialog.show();
            }
        });
        mBinding.settingsBlackList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FizzleAccountSummaryFragment summaryFragment = ((FizzleAccountSummaryFragment) AccountFragment.this.getParentFragment());
                if (summaryFragment != null) {
                    summaryFragment.goToBlackList(accountId);
                }
            }
        });
    }

    @Override
    public void onScrollChanged() {
        if (mBinding != null) {
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).setToolbarElevation(mBinding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

    @NonNull
    private AlertDialog createDeleteDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.account_delete_dialog_message)
                .setTitle(R.string.account_delete_dialog_title)
                .setPositiveButton(R.string.menu_delete, (dialog, whichButton) -> mAccountService.removeAccount(mAccount.getAccountID()))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        Activity activity = getActivity();
        if (activity != null)
            alertDialog.setOwnerActivity(getActivity());
        return alertDialog;
    }

}
