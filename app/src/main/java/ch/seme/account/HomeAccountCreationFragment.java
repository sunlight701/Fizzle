/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
package ch.seme.account;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import ch.seme.R;
import ch.seme.application.FizzleApplication;
import ch.seme.databinding.FragAccHomeCreateBinding;
import ch.seme.mvp.BaseSupportFragment;
import ch.seme.utils.AndroidFileUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class HomeAccountCreationFragment extends BaseSupportFragment<HomeAccountCreationPresenter> implements HomeAccountCreationView {
    private static final int ARCHIVE_REQUEST_CODE = 42;

    public static final String TAG = HomeAccountCreationFragment.class.getSimpleName();

    private FragAccHomeCreateBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccHomeCreateBinding.inflate(inflater, container, false);
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
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);
        binding.ringAddAccount.setOnClickListener(v -> presenter.clickOnLinkAccount());
        binding.ringCreateBtn.setOnClickListener(v -> presenter.clickOnCreateAccount());
        binding.accountConnectServer.setOnClickListener(v -> presenter.clickOnConnectAccount());
        binding.ringImportAccount.setOnClickListener(v -> performFileSearch());
        binding.idTextLinkDevice.setPaintFlags(binding.idTextLinkDevice.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        binding.idTextBackup.setPaintFlags(binding.idTextBackup.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        binding.idTextLinkDevice.setOnClickListener(v -> presenter.clickOnLinkAccount());
        binding.idTextBackup.setOnClickListener(v -> performFileSearch());
    }

    @Override
    public void goToAccountCreation() {
        Fragment fragment = new FizzleAccountCreationFragment();
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @Override
    public void goToAccountLink() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(true);
        Fragment fragment = FizzleLinkAccountFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @Override
    public void goToAccountConnect() {
        AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
        ringAccountViewModel.setLink(true);
        Fragment fragment = FizzleAccountConnectFragment.newInstance(ringAccountViewModel);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    private void performFileSearch() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*");
            startActivityForResult(intent, ARCHIVE_REQUEST_CODE);
        } catch (Exception e) {
            View v = getView();
            if (v != null)
                Snackbar.make(v, "No file browser available on this device", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                if (uri != null) {
                    AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(file -> {
                                AccountCreationModelImpl ringAccountViewModel = new AccountCreationModelImpl();
                                ringAccountViewModel.setLink(true);
                                ringAccountViewModel.setArchive(file);
                                Fragment fragment = FizzleLinkAccountFragment.newInstance(ringAccountViewModel);
                                replaceFragmentWithSlide(fragment, R.id.wizard_container);
                            }, e-> {
                                View v = getView();
                                if (v != null)
                                    Snackbar.make(v, "Can't import archive: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                }
            }
        }
    }
}
