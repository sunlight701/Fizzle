/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package ch.seme.fragments;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import ch.seme.R;
import ch.seme.adapters.SmartListAdapter;
import ch.seme.application.FizzleApplication;
import ch.seme.client.CallActivity;
import ch.seme.client.ContactDetailsActivity;
import ch.seme.client.ConversationActivity;
import ch.seme.client.HomeActivity;
import ch.seme.databinding.FragSmartlistBinding;
import ch.seme.model.CallContact;
import ch.seme.model.Conversation;
import ch.seme.model.Interaction;
import ch.seme.mvp.BaseSupportFragment;
import ch.seme.services.AccountService;
import ch.seme.smartlist.SmartListPresenter;
import ch.seme.smartlist.SmartListView;
import ch.seme.smartlist.SmartListViewModel;
import ch.seme.utils.ActionHelper;
import ch.seme.utils.ClipboardHelper;
import ch.seme.utils.ConversationPath;
import ch.seme.utils.DeviceUtils;
import ch.seme.viewholders.SmartListViewHolder;

public class SmartListFragment extends BaseSupportFragment<SmartListPresenter> implements SearchView.OnQueryTextListener,
        SmartListViewHolder.SmartListListeners,
        Conversation.ConversationActionCallback,
        ClipboardHelper.ClipboardHelperCallback,
        SmartListView
{
    private static final String TAG = SmartListFragment.class.getSimpleName();
    private static final String STATE_LOADING = TAG + ".STATE_LOADING";
    public static final String KEY_ACCOUNT_ID = "accountId";

    private static final int SCROLL_DIRECTION_UP = -1;

    @Inject
    AccountService mAccountService;

    private SmartListAdapter mSmartListAdapter;

    private SearchView mSearchView = null;
    private MenuItem mSearchMenuItem = null;
    private MenuItem mDialpadMenuItem = null;
    private FragSmartlistBinding binding;

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        menu.clear();

        inflater.inflate(R.menu.smartlist_menu, menu);
        mSearchMenuItem = menu.findItem(R.id.menu_contact_search);
        mDialpadMenuItem = menu.findItem(R.id.menu_contact_dial);
        //Added by slark
        mSearchMenuItem.setVisible(false);

        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mDialpadMenuItem.setVisible(false);
                binding.newconvFab.hide();
                setOverflowMenuVisible(menu, true);
                changeSeparatorHeight(false);
                binding.qrCode.setVisibility(View.GONE);
                setTabletQRLayout(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mDialpadMenuItem.setVisible(true);
                binding.newconvFab.hide();
                setOverflowMenuVisible(menu, false);
                changeSeparatorHeight(true);
                binding.qrCode.setVisibility(View.VISIBLE);
                setTabletQRLayout(true);
                return true;
            }
        });

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.searchbar_hint));
        mSearchView.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.MATCH_PARENT));
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_GO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            EditText editText = mSearchView.findViewById(R.id.search_src_text);
            if (editText != null) {
                editText.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity  activity = getActivity();
        Intent intent = activity == null ? null : activity.getIntent();
        if (intent != null)
            handleIntent(intent);
    }

    public void handleIntent(@NonNull Intent intent) {
        if (mSearchView != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_VIEW:
                case Intent.ACTION_CALL:
                    mSearchView.setQuery(intent.getDataString(), true);
                    break;
                case Intent.ACTION_DIAL:
                    mSearchMenuItem.expandActionView();
                    mSearchView.setQuery(intent.getDataString(), false);
                    break;
                case Intent.ACTION_SEARCH:
                    mSearchMenuItem.expandActionView();
                    mSearchView.setQuery(intent.getStringExtra(SearchManager.QUERY), true);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contact_search:
                mSearchView.setInputType(EditorInfo.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                );
                return false;
            case R.id.menu_contact_dial:
                if (mSearchView.getInputType() == EditorInfo.TYPE_CLASS_PHONE) {
                    mSearchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                    mDialpadMenuItem.setIcon(R.drawable.baseline_dialpad_24);
                } else {
                    mSearchView.setInputType(EditorInfo.TYPE_CLASS_PHONE);
                    mDialpadMenuItem.setIcon(R.drawable.baseline_keyboard_24);
                }
                return true;
            case R.id.menu_settings:
                ((HomeActivity) getActivity()).goToSettings();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // presenter.newContactClicked();
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // if there's another fragment on top of this one, when a rotation is done, this fragment is destroyed and
        // in the process of recreating it, as it is not shown on the top of the screen, the "onCreateView" method is never called, so the mLoader is null
        if (binding != null)
            outState.putBoolean(STATE_LOADING, binding.loadingIndicator.isShown());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onQueryTextChange(final String query) {
        presenter.queryTextChanged(query);
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragSmartlistBinding.inflate(inflater, container, false);
        ((FizzleApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);

        binding.qrCode.setOnClickListener(v -> presenter.clickQRSearch());

        binding.confsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                boolean canScrollUp = recyclerView.canScrollVertically(SCROLL_DIRECTION_UP);
                ExtendedFloatingActionButton btn = binding.newconvFab;
                boolean isExtended = btn.isExtended();
                if (dy > 0 && isExtended) {
                    btn.shrink();
                } else if ((dy < 0 || !canScrollUp) && !isExtended) {
                    btn.extend();
                }

                HomeActivity activity = (HomeActivity) getActivity();
                if (activity != null)
                    activity.setToolbarElevation(canScrollUp);
            }
        });

        DefaultItemAnimator animator = (DefaultItemAnimator) binding.confsList.getItemAnimator();
        if (animator != null) {
            animator.setSupportsChangeAnimations(false);
        }

        binding.newconvFab.setOnClickListener(v -> presenter.fabButtonClicked());
    }

    @Override
    public void setLoading(final boolean loading) {
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * Handles the visibility of some menus to hide / show the overflow menu
     *
     * @param menu    the menu containing the menuitems we need to access
     * @param visible true to display the overflow menu, false otherwise
     */
    private void setOverflowMenuVisible(final Menu menu, boolean visible) {
        if (null != menu) {
            MenuItem overflowMenuItem = menu.findItem(R.id.menu_overflow);
            if (null != overflowMenuItem) {
                //Modified by slark
                //overflowMenuItem.setVisible(visible);
            }
        }
    }

    @Override
    public void removeConversation(CallContact callContact) {
        presenter.removeConversation(callContact);
    }

    @Override
    public void clearConversation(CallContact callContact) {
        presenter.clearConversation(callContact);
    }

    @Override
    public void copyContactNumberToClipboard(String contactNumber) {
        ClipboardHelper.copyNumberToClipboard(getActivity(), contactNumber, this);
    }

    @Override
    public void clipBoardDidCopyNumber(String copiedNumber) {
        String snackbarText = getString(R.string.conversation_action_copied_peer_number_clipboard,
                ActionHelper.getShortenedNumber(copiedNumber));
        Snackbar.make(binding.listCoordinator, snackbarText, Snackbar.LENGTH_LONG).show();
    }

    public void onFabButtonClicked() {
        presenter.fabButtonClicked();
    }

    @Override
    public void displayChooseNumberDialog(final CharSequence[] numbers) {
        final Context context = requireContext();
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.choose_number)
                .setItems(numbers, (dialog, which) -> {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(CallActivity.ACTION_CALL)
                            .setClass(context, CallActivity.class)
                            .setData(Uri.parse(selected.toString()));
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
                })
                .show();
    }

    @Override
    public void displayNoConversationMessage() {
        binding.placeholder.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideNoConversationMessage() {
        binding.placeholder.setVisibility(View.GONE);
    }

    @Override
    public void displayConversationDialog(final SmartListViewModel smartListViewModel) {
        new MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.conversation_actions, (dialog, which) -> {
                    switch (which) {
                        case ActionHelper.ACTION_COPY:
                            presenter.copyNumber(smartListViewModel);
                            break;
                        case ActionHelper.ACTION_CLEAR:
                            presenter.clearConversation(smartListViewModel);
                            break;
                        case ActionHelper.ACTION_DELETE:
                            presenter.removeConversation(smartListViewModel);
                            break;
                        case ActionHelper.ACTION_BLOCK:
                            presenter.banContact(smartListViewModel);
                            break;
                    }
                })
                .show();
    }

    @Override
    public void displayClearDialog(CallContact callContact) {
        ActionHelper.launchClearAction(getActivity(), callContact, SmartListFragment.this);
    }

    @Override
    public void displayDeleteDialog(CallContact callContact) {
        ActionHelper.launchDeleteAction(getActivity(), callContact, SmartListFragment.this);
    }

    @Override
    public void copyNumber(CallContact callContact) {
        ActionHelper.launchCopyNumberToClipboardFromContact(getActivity(), callContact, this);
    }

    @Override
    public void displayMenuItem() {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.expandActionView();
        }
    }

    @Override
    public void hideList() {
        binding.confsList.setVisibility(View.GONE);
    }

    @Override
    public void updateList(@Nullable final List<SmartListViewModel> smartListViewModels) {
        if (binding == null)
            return;
        List<SmartListViewModel> vms = new ArrayList<>();
        for (SmartListViewModel ob : smartListViewModels) {
            if (ob.getLastEvent().getType() != Interaction.InteractionType.CONTACT)
                vms.add((SmartListViewModel) ob);
        }
        if (binding.confsList.getAdapter() == null) {
            mSmartListAdapter = new SmartListAdapter(vms, SmartListFragment.this);
            binding.confsList.setAdapter(mSmartListAdapter);
            binding.confsList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            llm.setOrientation(RecyclerView.VERTICAL);
            binding.confsList.setLayoutManager(llm);
        } else {
            mSmartListAdapter.update(vms);
        }
        binding.confsList.setVisibility(View.VISIBLE);
    }

    @Override
    public void update(int position) {
        Log.w(TAG, "update " + position + " " + mSmartListAdapter);
        if (mSmartListAdapter != null) {
            mSmartListAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void update(SmartListViewModel model) {
        if (mSmartListAdapter != null)
            mSmartListAdapter.update(model);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HomeActivity.REQUEST_CODE_QR_CONVERSATION && data != null && resultCode == Activity.RESULT_OK) {
            String contactID = data.getStringExtra(ConversationFragment.KEY_CONTACT_RING_ID);
            if (contactID != null) {
                presenter.startConversation(new ch.seme.model.Uri(contactID));
            }
        }
    }

    @Override
    public void goToConversation(String accountId, ch.seme.model.Uri contactId) {
        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }

        if (!DeviceUtils.isTablet(getContext())) {
            startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, contactId.toString()), requireContext(), ConversationActivity.class));
        } else {
            ((HomeActivity) requireActivity()).startConversationTablet(ConversationPath.toBundle(accountId, contactId.toString()));
        }
    }

    @Override
    public void goToCallActivity(String accountId, String contactId) {
        Intent intent = new Intent(CallActivity.ACTION_CALL)
                .setClass(requireActivity(), CallActivity.class)
                .putExtra(CallFragment.KEY_AUDIO_ONLY, false)
                .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
    }

    @Override
    public void goToQRFragment() {
        QRCodeFragment qrCodeFragment = QRCodeFragment.newInstance(QRCodeFragment.INDEX_SCAN);
        qrCodeFragment.show(getParentFragmentManager(), QRCodeFragment.TAG);
        binding.qrCode.setVisibility(View.GONE);
        setTabletQRLayout(false);
    }

    @Override
    public void scrollToTop() {
        if (binding != null)
            binding.confsList.scrollToPosition(0);
    }

    @Override
    public void onItemClick(SmartListViewModel smartListViewModel) {
        presenter.conversationClicked(smartListViewModel);
    }

    @Override
    public void onItemLongClick(SmartListViewModel smartListViewModel) {
        presenter.conversationLongClicked(smartListViewModel);
    }
    @Override
    public void goToContactActivity(String accountId, String contactId) {
        startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, contactId),
                requireActivity().getApplicationContext(), ContactDetailsActivity.class));
    }

    private void changeSeparatorHeight(boolean open) {
        if (binding == null || binding.separator == null)
            return;

        if (DeviceUtils.isTablet(getContext())) {
            int margin = 0;

            if (open) {
                Activity activity = getActivity();
                if (activity != null) {
                    Toolbar toolbar = activity.findViewById(R.id.main_toolbar);
                    if (toolbar != null)
                        margin = toolbar.getHeight();
                }
            }

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.separator.getLayoutParams();
            params.topMargin = margin;
            binding.separator.setLayoutParams(params);
        }
    }

    private void selectFirstItem() {
        if (mSmartListAdapter != null && mSmartListAdapter.getItemCount() > 0) {
            new Handler().postDelayed(() -> {
                if (binding != null) {
                    RecyclerView.ViewHolder holder = binding.confsList.findViewHolderForAdapterPosition(0);
                    if (holder != null)
                        holder.itemView.performClick();
                }

            }, 100);
        }
    }

    private void setTabletQRLayout(boolean show) {
        if (!DeviceUtils.isTablet(getContext()))
            return;

        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) binding.listCoordinator.getLayoutParams();
        if (show) {
            params.addRule(RelativeLayout.BELOW, R.id.qr_code);
            params.topMargin = 0;
        } else {
            params.removeRule(RelativeLayout.BELOW);
            TypedValue value = new TypedValue();
            if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true)) {
                params.topMargin = TypedValue.complexToDimensionPixelSize(value.data, getResources().getDisplayMetrics());
            }
        }
        binding.listCoordinator.setLayoutParams(params);
    }

}
