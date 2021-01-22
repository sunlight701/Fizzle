/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
 *          AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.seme.account;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import ch.seme.R;
import ch.seme.application.FizzleApplication;
import ch.seme.client.HomeActivity;
import ch.seme.contactrequests.BlockListFragment;
import ch.seme.databinding.FragAccountSettingsBinding;
import ch.seme.fragments.AdvancedAccountFragment;
import ch.seme.fragments.GeneralAccountFragment;
import ch.seme.fragments.MediaPreferenceFragment;
import ch.seme.fragments.SecurityAccountFragment;
import ch.seme.interfaces.BackHandlerInterface;
import ch.seme.mvp.BaseSupportFragment;
import ch.seme.utils.DeviceUtils;

public class AccountEditionFragment extends BaseSupportFragment<AccountEditionPresenter> implements
        BackHandlerInterface,
        AccountEditionView,
        ViewTreeObserver.OnScrollChangedListener  {
    private static final String TAG = AccountEditionFragment.class.getSimpleName();

    public static final String ACCOUNT_ID_KEY = AccountEditionFragment.class.getCanonicalName() + "accountid";
    static final String ACCOUNT_HAS_PASSWORD_KEY = AccountEditionFragment.class.getCanonicalName() + "hasPassword";
    public static final String ACCOUNT_ID = TAG + "accountID";

    private static final int SCROLL_DIRECTION_UP = -1;

    private FragAccountSettingsBinding mBinding;

    private boolean mIsVisible;

    private String mAccountId;
    private boolean mAccountIsFizzle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccountSettingsBinding.inflate(inflater, container, false);
        // dependency injection
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

        mAccountId = getArguments().getString(ACCOUNT_ID);

        HomeActivity activity = (HomeActivity) getActivity();
        if (activity != null && DeviceUtils.isTablet(activity)) {
            activity.setTabletTitle(R.string.navigation_item_account);
        }

        mBinding.fragmentContainer.getViewTreeObserver().addOnScrollChangedListener(this);

        presenter.init(mAccountId);
    }

    @Override
    public void displaySummary(String accountId) {
        toggleView(accountId, true);
        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(FizzleAccountSummaryFragment.TAG);
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        if (existingFragment == null) {
            FizzleAccountSummaryFragment fragment = new FizzleAccountSummaryFragment();
            fragment.setArguments(args);
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment, FizzleAccountSummaryFragment.TAG)
                    .commit();
        } else {
            if (!existingFragment.isStateSaved())
                existingFragment.setArguments(args);
            ((FizzleAccountSummaryFragment) existingFragment).setAccount(accountId);
        }
    }

    @Override
    public void displaySIPView(String accountId) {
        toggleView(accountId, false);
    }

    @Override
    public void initViewPager(String accountId, boolean isFizzle) {
        mBinding.pager.setOffscreenPageLimit(4);
        mBinding.slidingTabs.setupWithViewPager(mBinding.pager);
        mBinding.pager.setAdapter(new PreferencesPagerAdapter(getChildFragmentManager(), getActivity(), accountId, isFizzle));
        BlockListFragment existingFragment = (BlockListFragment) getChildFragmentManager().findFragmentByTag(BlockListFragment.TAG);
        if (existingFragment != null) {
            Bundle args = new Bundle();
            args.putString(ACCOUNT_ID_KEY, accountId);
            if (!existingFragment.isStateSaved())
                existingFragment.setArguments(args);
            existingFragment.setAccount(accountId);
        }
    }

    @Override
    public void goToBlackList(String accountId) {
        BlockListFragment blockListFragment = new BlockListFragment();
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        blockListFragment.setArguments(args);
        getChildFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(BlockListFragment.TAG)
                .replace(R.id.fragment_container, blockListFragment, BlockListFragment.TAG)
                .commit();
        mBinding.slidingTabs.setVisibility(View.GONE);
        mBinding.pager.setVisibility(View.GONE);
        mBinding.fragmentContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        setBackListenerEnabled(false);
    }

    public boolean onBackPressed() {
        mIsVisible = false;
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).setToolbarOutlineState(true);
        if (mBinding.fragmentContainer.getVisibility() != View.VISIBLE) {
            toggleView(mAccountId, mAccountIsFizzle);
            return true;
        }
        FizzleAccountSummaryFragment summaryFragment = (FizzleAccountSummaryFragment) getChildFragmentManager().findFragmentByTag(FizzleAccountSummaryFragment.TAG);
        if (summaryFragment != null && summaryFragment.onBackPressed()) {
            return true;
        }
        return getChildFragmentManager().popBackStackImmediate();
    }

    private void toggleView(String accountId, boolean isFizzle) {
        mAccountId = accountId;
        mAccountIsFizzle = isFizzle;
        mBinding.slidingTabs.setVisibility(isFizzle? View.GONE : View.VISIBLE);
        mBinding.pager.setVisibility(isFizzle? View.GONE : View.VISIBLE);
        mBinding.fragmentContainer.setVisibility(isFizzle? View.VISIBLE : View.GONE);
        setBackListenerEnabled(isFizzle);
    }

    @Override
    public void exit() {
        Activity activity = getActivity();
        if (activity != null)
            activity.onBackPressed();
    }

    private void setBackListenerEnabled(boolean enable) {
        if (getActivity() instanceof HomeActivity)
            ((HomeActivity) getActivity()).setAccountFragmentOnBackPressedListener(enable ? this : null);
    }

    private static class PreferencesPagerAdapter extends FragmentStatePagerAdapter {
        private Context mContext;
        private String accountId;
        private boolean isFizzleAccount;

        PreferencesPagerAdapter(FragmentManager fm, Context mContext, String accountId, boolean isFizzleAccount) {
            super(fm);
            this.mContext = mContext;
            this.accountId = accountId;
            this.isFizzleAccount = isFizzleAccount;
        }

        @StringRes
        private static int getRingPanelTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.account_preferences_basic_tab;
                case 1:
                    return R.string.account_preferences_media_tab;
                case 2:
                    return R.string.account_preferences_advanced_tab;
                default:
                    return -1;
            }
        }

        @StringRes
        private static int getSIPPanelTitle(int position) {
            switch (position) {
                case 0:
                    return R.string.account_preferences_basic_tab;
                case 1:
                    return R.string.account_preferences_media_tab;
                case 2:
                    return R.string.account_preferences_advanced_tab;
                case 3:
                    return R.string.account_preferences_security_tab;
                default:
                    return -1;
            }
        }

        @Override
        public int getCount() {
            return isFizzleAccount ? 3 : 4;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return isFizzleAccount ? getFizzlePanel(position) : getSIPPanel(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int resId = isFizzleAccount ? getRingPanelTitle(position) : getSIPPanelTitle(position);
            return mContext.getString(resId);
        }

        @NonNull
        private Fragment getFizzlePanel(int position) {
            switch (position) {
                case 0:
                    return fragmentWithBundle(new GeneralAccountFragment());
                case 1:
                    return fragmentWithBundle(new MediaPreferenceFragment());
                case 2:
                    return fragmentWithBundle(new AdvancedAccountFragment());
                default:
                    throw new IllegalArgumentException();
            }
        }

        @NonNull
        private Fragment getSIPPanel(int position) {
            switch (position) {
                case 0:
                    return GeneralAccountFragment.newInstance(accountId);
                case 1:
                    return MediaPreferenceFragment.newInstance(accountId);
                case 2:
                    return fragmentWithBundle(new AdvancedAccountFragment());
                case 3:
                    return fragmentWithBundle(new SecurityAccountFragment());
                default:
                    throw new IllegalArgumentException();
            }
        }

        private Fragment fragmentWithBundle(Fragment result) {
            Bundle args = new Bundle();
            args.putString(ACCOUNT_ID_KEY, accountId);
            result.setArguments(args);
            return result;
        }
    }

    @Override
    public void onScrollChanged() {
        setupElevation();
    }

    private void setupElevation() {
        if (mBinding == null || !mIsVisible) {
            return;
        }
        Activity activity = getActivity();
        if (!(activity instanceof HomeActivity))
            return;
        LinearLayout ll = (LinearLayout) mBinding.pager.getChildAt(mBinding.pager.getCurrentItem());
        if (ll == null) return;
        RecyclerView rv = (RecyclerView)((FrameLayout) ll.getChildAt(0)).getChildAt(0);
        if (rv == null) return;
        HomeActivity homeActivity = (HomeActivity) activity;
        if (rv.canScrollVertically(SCROLL_DIRECTION_UP)) {
            mBinding.slidingTabs.setElevation(mBinding.slidingTabs.getResources().getDimension(R.dimen.toolbar_elevation));
            homeActivity.setToolbarElevation(true);
            homeActivity.setToolbarOutlineState(false);
        } else {
            mBinding.slidingTabs.setElevation(0);
            homeActivity.setToolbarElevation(false);
            homeActivity.setToolbarOutlineState(true);
        }
    }
}
