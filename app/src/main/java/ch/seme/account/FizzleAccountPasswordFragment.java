/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 * Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package ch.seme.account;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import ch.seme.R;
import ch.seme.application.FizzleApplication;
import ch.seme.databinding.FragAccFizzlePasswordBinding;
import ch.seme.mvp.AccountCreationModel;
import ch.seme.mvp.BaseSupportFragment;
import ch.seme.utils.RegisteredNameFilter;

public class FizzleAccountPasswordFragment extends BaseSupportFragment<FizzleAccountCreationPresenter>
        implements FizzleAccountCreationView {

    private static final String KEY_MODEL = "model";
    private AccountCreationModel model;
    private FragAccFizzlePasswordBinding binding;

    private boolean mIsChecked = false;

    public static FizzleAccountPasswordFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        FizzleAccountPasswordFragment fragment = new FizzleAccountPasswordFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (model != null)
            outState.putSerializable(KEY_MODEL, model);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccFizzlePasswordBinding.inflate(inflater, container, false);
        ((FizzleApplication) requireActivity().getApplication()).getInjectionComponent().inject(this);
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
        if (savedInstanceState != null) {
            model = (AccountCreationModelImpl) savedInstanceState.getSerializable(KEY_MODEL);
        }
        enableNextButton(false);
        binding.ringUsername.setFilters(new InputFilter[]{new RegisteredNameFilter()});
        binding.createAccount.setOnClickListener(v -> presenter.createAccount());
        binding.ringUsername.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity().
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.ringUsername, InputMethodManager.SHOW_IMPLICIT);
//        binding.switchRingPush.setOnCheckedChangeListener((buttonView, isChecked) -> presenter.setPush(isChecked));
        binding.ringUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                presenter.userNameChanged(s.toString());
            }
        });
        binding.ringUsername.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && binding.createAccount.isEnabled()) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                    presenter.createAccount();
                    return true;
                }
                return false;
            }
        });

        binding.createAccount.setOnClickListener(v -> presenter.createAccount());
        binding.ringPasswordSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mIsChecked = isChecked;
            if (isChecked) {
                binding.passwordTxtBox.setVisibility(View.VISIBLE);
                binding.ringPasswordRepeatTxtBox.setVisibility(View.VISIBLE);
                binding.placeholder.setVisibility(View.GONE);
                CharSequence password = binding.ringPassword.getText();
                presenter.passwordChanged(password == null ? null : password.toString(), binding.ringPasswordRepeat.getText());
            } else {
                binding.passwordTxtBox.setVisibility(View.GONE);
                binding.ringPasswordRepeatTxtBox.setVisibility(View.GONE);
                binding.placeholder.setVisibility(View.VISIBLE);
                presenter.passwordUnset();
            }
        });
        binding.ringPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.passwordChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.ringPasswordRepeat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.passwordConfirmChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.ringPasswordRepeat.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.createAccount();
            }
            return false;
        });
        binding.ringPasswordRepeat.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.createAccount.isEnabled()) {
                InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                presenter.createAccount();
                return true;
            }
            return false;
        });

        presenter.init(model);
    }

    @Override
    public void updateUsernameAvailability(UsernameAvailabilityStatus status) {
        binding.ringUsernameAvailabilitySpinner.setVisibility(View.GONE);
        switch (status){
            case ERROR:
                binding.ringUsernameTxtBox.setErrorEnabled(true);
                binding.ringUsernameTxtBox.setError(getString(R.string.unknown_error));
                binding.ringUsernameTxtBox.setEndIconMode(TextInputLayout.END_ICON_NONE);
                enableNextButton(false);
                break;
            case ERROR_USERNAME_INVALID:
                binding.ringUsernameTxtBox.setErrorEnabled(true);
                binding.ringUsernameTxtBox.setError(getString(R.string.invalid_username));
                binding.ringUsernameTxtBox.setEndIconMode(TextInputLayout.END_ICON_NONE);
                enableNextButton(false);
                break;
            case ERROR_USERNAME_TAKEN:
                binding.ringUsernameTxtBox.setErrorEnabled(true);
                binding.ringUsernameTxtBox.setError(getString(R.string.username_already_taken));
                binding.ringUsernameTxtBox.setEndIconMode(TextInputLayout.END_ICON_NONE);
                enableNextButton(false);
                break;
            case LOADING:
                binding.ringUsernameTxtBox.setErrorEnabled(false);
                binding.ringUsernameTxtBox.setEndIconMode(TextInputLayout.END_ICON_NONE);
                binding.ringUsernameAvailabilitySpinner.setVisibility(View.VISIBLE);
                enableNextButton(false);
                break;
            case AVAILABLE:
                binding.ringUsernameTxtBox.setErrorEnabled(false);
                binding.ringUsernameTxtBox.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                binding.ringUsernameTxtBox.setEndIconDrawable(R.drawable.ic_good_green);
                enableNextButton(true);
                break;
            case RESET:
                binding.ringUsernameTxtBox.setErrorEnabled(false);
                binding.ringUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                enableNextButton(false);
        }
    }

    @Override
    public void showInvalidPasswordError(final boolean display) {
        if (display) {
            binding.passwordTxtBox.setError(getString(R.string.error_password_char_count));
        } else {
            binding.passwordTxtBox.setError(null);
        }
    }

    @Override
    public void showNonMatchingPasswordError(final boolean display) {
        if (display) {
            binding.ringPasswordRepeatTxtBox.setError(getString(R.string.error_passwords_not_equals));
        } else {
            binding.ringPasswordRepeatTxtBox.setError(null);
        }
    }

    @Override
    public void enableNextButton(final boolean enabled) {
        binding.createAccount.setEnabled(enabled);
//        if (!mIsChecked) {
//            binding.createAccount.setEnabled(true);
//            return;
//        }
    }

    @Override
    public void enableNextButtonAfterPasswordChanged(final boolean enabled) {
    }

    @Override
    public void enableNextButtonAfterUsernameChanged(final boolean enabled) {
    }

    @Override
    public void goToAccountCreation(AccountCreationModel accountCreationModel) {
        Activity wizardActivity = getActivity();
        if (wizardActivity instanceof AccountWizardActivity) {
            AccountWizardActivity wizard = (AccountWizardActivity) wizardActivity;
            wizard.createAccount(accountCreationModel);
            FizzleAccountCreationFragment parent = (FizzleAccountCreationFragment) getParentFragment();
            if (parent != null) {
                parent.scrollPagerFragment(accountCreationModel);
                InputMethodManager imm = (InputMethodManager) wizard.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(binding.ringPassword.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void cancel() {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null) {
            wizardActivity.onBackPressed();
        }
    }

    public void setUsername(String username) {
        model.setUsername(username);
    }

}
