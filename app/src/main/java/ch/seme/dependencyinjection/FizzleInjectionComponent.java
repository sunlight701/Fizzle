/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package ch.seme.dependencyinjection;

import javax.inject.Singleton;

import ch.seme.account.AccountEditionFragment;
import ch.seme.account.FizzleAccountPasswordFragment;
import ch.seme.account.FizzleAccountUsernameFragment;
import ch.seme.account.FizzleLinkAccountPasswordFragment;
import ch.seme.application.FizzleApplication;
import ch.seme.contactrequests.BlockListFragment;
import ch.seme.fragments.ContactListFragment;
import ch.seme.fragments.GeneralAccountFragment;
import ch.seme.fragments.LinkDeviceFragment;
import ch.seme.fragments.LocationSharingFragment;
import ch.seme.account.AccountWizardActivity;
import ch.seme.account.HomeAccountCreationFragment;
import ch.seme.account.FizzleAccountConnectFragment;
import ch.seme.account.ProfileCreationFragment;
import ch.seme.account.RegisterNameDialog;
import ch.seme.account.FizzleAccountSummaryFragment;
import ch.seme.client.ContactDetailsActivity;
import ch.seme.client.HomeActivity;
import ch.seme.client.RingtoneActivity;
import ch.seme.contactrequests.ContactRequestsFragment;
import ch.seme.facades.ConversationFacade;
import ch.seme.fragments.AccountMigrationFragment;
import ch.seme.fragments.AdvancedAccountFragment;
import ch.seme.fragments.CallFragment;
import ch.seme.fragments.ConversationFragment;
import ch.seme.client.ConversationSelectionActivity;
import ch.seme.fragments.MediaPreferenceFragment;
import ch.seme.fragments.SIPAccountCreationFragment;
import ch.seme.fragments.SecurityAccountFragment;
import ch.seme.fragments.ShareWithFragment;
import ch.seme.fragments.SmartListFragment;
import ch.seme.history.DatabaseHelper;
import ch.seme.launch.LaunchActivity;
import ch.seme.service.BootReceiver;
import ch.seme.service.CallNotificationService;
import ch.seme.service.DRingService;
import ch.seme.service.FizzleJobService;
import ch.seme.services.AccountService;
import ch.seme.services.CallService;
import ch.seme.services.ConferenceService;
import ch.seme.services.ContactServiceImpl;
import ch.seme.services.DaemonService;
import ch.seme.services.DataTransferService;
import ch.seme.services.DeviceRuntimeServiceImpl;
import ch.seme.services.FizzleChooserTargetService;
import ch.seme.services.HardwareService;
import ch.seme.services.HistoryServiceImpl;
import ch.seme.services.LocationSharingService;
import ch.seme.services.NotificationServiceImpl;
import ch.seme.services.SharedPreferencesServiceImpl;
import ch.seme.services.SyncService;
import ch.seme.settings.AccountFragment;
import ch.seme.settings.SettingsFragment;
import ch.seme.share.ShareFragment;
import ch.seme.tv.account.TVAccountExport;
import ch.seme.tv.account.TVAccountWizard;
import ch.seme.tv.account.TVFizzleAccountCreationFragment;
import ch.seme.tv.account.TVFizzleLinkAccountFragment;
import ch.seme.tv.account.TVHomeAccountCreationFragment;
import ch.seme.tv.account.TVProfileCreationFragment;
import ch.seme.tv.account.TVProfileEditingFragment;
import ch.seme.tv.account.TVSettingsFragment;
import ch.seme.tv.account.TVShareFragment;
import ch.seme.tv.call.TVCallActivity;
import ch.seme.tv.call.TVCallFragment;
import ch.seme.tv.cards.iconcards.IconCardPresenter;
import ch.seme.tv.conversation.TvConversationFragment;
import ch.seme.tv.contact.TVContactFragment;
import ch.seme.tv.main.MainFragment;
import ch.seme.tv.search.ContactSearchFragment;
import dagger.Component;

@Singleton
@Component(modules = {FizzleInjectionModule.class, ServiceInjectionModule.class})
public interface FizzleInjectionComponent {
    void inject(FizzleApplication app);

    void inject(HomeActivity activity);

    void inject(DatabaseHelper helper);

    void inject(AccountWizardActivity activity);

    void inject(AccountEditionFragment activity);

    void inject(RingtoneActivity activity);

    void inject(AccountMigrationFragment fragment);

    void inject(SIPAccountCreationFragment fragment);

    void inject(FizzleAccountSummaryFragment fragment);

    void inject(CallFragment fragment);

    void inject(SmartListFragment fragment);

    void inject(ContactListFragment fragment);

    void inject(ConversationSelectionActivity fragment);

    void inject(FizzleAccountUsernameFragment fragment);

    void inject(FizzleAccountPasswordFragment fragment);

    void inject(MediaPreferenceFragment fragment);

    void inject(SecurityAccountFragment fragment);

    void inject(ShareFragment fragment);

    void inject(SettingsFragment fragment);

    void inject(AccountFragment fragment);

    void inject(ProfileCreationFragment fragment);

    void inject(RegisterNameDialog dialog);

    void inject(ConversationFragment fragment);

    void inject(ContactRequestsFragment fragment);

    void inject(BlockListFragment fragment);

    void inject(DRingService service);

    void inject(DeviceRuntimeServiceImpl service);

    void inject(DaemonService service);

    void inject(CallService service);

    void inject(ConferenceService service);

    void inject(AccountService service);

    void inject(HardwareService service);

    void inject(SharedPreferencesServiceImpl service);

    void inject(HistoryServiceImpl service);

    void inject(ContactServiceImpl service);

    void inject(NotificationServiceImpl service);

    void inject(ConversationFacade service);

    void inject(CallNotificationService service);

    void inject(DataTransferService service);

    void inject(BootReceiver receiver);

    void inject(AdvancedAccountFragment fragment);

    void inject(GeneralAccountFragment fragment);

    void inject(HomeAccountCreationFragment fragment);

    void inject(FizzleLinkAccountPasswordFragment fragment);

    void inject(FizzleAccountConnectFragment fragment);

    void inject(LaunchActivity activity);

    //    AndroidTV section
    void inject(TVCallFragment fragment);

    void inject(MainFragment fragment);

    void inject(ContactSearchFragment fragment);

    void inject(ch.seme.tv.main.HomeActivity activity);

    void inject(TVCallActivity activity);

    void inject(TVAccountWizard activity);

    void inject(TVHomeAccountCreationFragment fragment);

    void inject(TVProfileCreationFragment fragment);

    void inject(TVFizzleAccountCreationFragment fragment);

    void inject(TVFizzleLinkAccountFragment fragment);

    void inject(TVAccountExport fragment);

    void inject(TVProfileEditingFragment activity);

    void inject(TVShareFragment activity);

    void inject(TVContactFragment fragment);

    void inject(TvConversationFragment fragment);

    void inject(TVSettingsFragment tvSettingsFragment);

    void inject(TVSettingsFragment.PrefsFragment prefsFragment);

    void inject(FizzleChooserTargetService service);

    void inject(LocationSharingFragment service);

    void inject(FizzleJobService service);

    void inject(ShareWithFragment fragment);

    void inject(ContactDetailsActivity fragment);

    void inject(IconCardPresenter presenter);

    void inject(LocationSharingService service);

    void inject(SyncService syncService);

    void inject(LinkDeviceFragment linkDeviceFragment);
}
