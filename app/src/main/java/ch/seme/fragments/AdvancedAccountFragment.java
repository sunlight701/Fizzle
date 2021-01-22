/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package ch.seme.fragments;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;

import ch.seme.R;
import ch.seme.account.AccountEditionFragment;
import ch.seme.application.FizzleApplication;
import ch.seme.model.AccountConfig;
import ch.seme.model.ConfigKey;
import ch.seme.mvp.BasePreferenceFragment;
import ch.seme.views.EditTextIntegerPreference;
import ch.seme.views.EditTextPreferenceDialog;
import ch.seme.views.PasswordPreference;

public class AdvancedAccountFragment extends BasePreferenceFragment<AdvancedAccountPresenter> implements AdvancedAccountView, Preference.OnPreferenceChangeListener {

    public static final String TAG = AdvancedAccountFragment.class.getSimpleName();

    private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        ((FizzleApplication) requireActivity().getApplication()).getInjectionComponent().inject(this);
        super.onCreatePreferences(bundle, s);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.account_advanced_prefs);

        Bundle args = getArguments();
        presenter.init(args == null  ? null : args.getString(AccountEditionFragment.ACCOUNT_ID_KEY));
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        FragmentManager fragmentManager = requireFragmentManager();
        if (fragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        if (preference instanceof EditTextIntegerPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_NUMBER);
            f.setTargetFragment(this, 0);
            f.show(fragmentManager, DIALOG_FRAGMENT_TAG);
        } else if (preference instanceof PasswordPreference) {
            EditTextPreferenceDialog f = EditTextPreferenceDialog.newInstance(preference.getKey(), EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            f.setTargetFragment(this, 0);
            f.show(fragmentManager, DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void initView(AccountConfig config, ArrayList<CharSequence> networkInterfaces) {
        for (ConfigKey confKey : config.getKeys()) {
            Preference pref = findPreference(confKey.key());
            if (pref != null) {
                pref.setOnPreferenceChangeListener(this);
                if (confKey == ConfigKey.LOCAL_INTERFACE) {
                    String val = config.get(confKey);
                    CharSequence[] display = networkInterfaces.toArray(new CharSequence[networkInterfaces.size()]);
                    ListPreference listPref = (ListPreference) pref;
                    listPref.setEntries(display);
                    listPref.setEntryValues(display);
                    listPref.setSummary(val);
                    listPref.setValue(val);
                } else if (!confKey.isTwoState()) {
                    String val = config.get(confKey);
                    pref.setSummary(val);
                    if (pref instanceof EditTextPreference) {
                        ((EditTextPreference) pref).setText(val);
                    }
                } else {
                    ((TwoStatePreference) pref).setChecked(config.getBool(confKey));
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ConfigKey key = ConfigKey.fromString(preference.getKey());

        presenter.preferenceChanged(key, newValue);
        if (preference instanceof TwoStatePreference) {
            presenter.twoStatePreferenceChanged(key, newValue);
        } else if (preference instanceof PasswordPreference) {
            presenter.passwordPreferenceChanged(key, newValue);
            preference.setSummary(TextUtils.isEmpty(newValue.toString()) ? "" : "******");
        } else {
            presenter.preferenceChanged(key, newValue);
            preference.setSummary(newValue.toString());
        }
        return true;
    }
}
