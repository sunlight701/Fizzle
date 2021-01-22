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

import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

import ch.seme.application.FizzleApplication;
import ch.seme.facades.ConversationFacade;
import ch.seme.services.AccountService;
import ch.seme.services.CallService;
import ch.seme.services.ContactService;
import ch.seme.services.ContactServiceImpl;
import ch.seme.services.DaemonService;
import ch.seme.services.DeviceRuntimeService;
import ch.seme.services.DeviceRuntimeServiceImpl;
import ch.seme.services.HardwareService;
import ch.seme.services.HardwareServiceImpl;
import ch.seme.services.HistoryService;
import ch.seme.services.HistoryServiceImpl;
import ch.seme.services.LogService;
import ch.seme.services.LogServiceImpl;
import ch.seme.services.NotificationService;
import ch.seme.services.NotificationServiceImpl;
import ch.seme.services.PreferencesService;
import ch.seme.services.SharedPreferencesServiceImpl;
import ch.seme.services.VCardService;
import ch.seme.services.VCardServiceImpl;
import ch.seme.utils.Log;
import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Module
public class ServiceInjectionModule {

    private final FizzleApplication mFizzleApplication;

    public ServiceInjectionModule(FizzleApplication app) {
        mFizzleApplication = app;
    }

    @Provides
    @Singleton
    PreferencesService provideSettingsService() {
        SharedPreferencesServiceImpl settingsService = new SharedPreferencesServiceImpl();
        mFizzleApplication.getInjectionComponent().inject(settingsService);
        return settingsService;
    }

    @Provides
    @Singleton
    HistoryService provideHistoryService() {
        HistoryServiceImpl historyService = new HistoryServiceImpl();
        mFizzleApplication.getInjectionComponent().inject(historyService);
        return historyService;
    }

    @Provides
    @Singleton
    LogService provideLogService() {
        LogService service = new LogServiceImpl();
        Log.injectLogService(service);
        return service;
    }

    @Provides
    @Singleton
    NotificationService provideNotificationService() {
        NotificationServiceImpl service = new NotificationServiceImpl();
        mFizzleApplication.getInjectionComponent().inject(service);
        service.initHelper();
        return service;
    }

    @Provides
    @Singleton
    DeviceRuntimeService provideDeviceRuntimeService(LogService logService) {
        DeviceRuntimeServiceImpl runtimeService = new DeviceRuntimeServiceImpl();
        mFizzleApplication.getInjectionComponent().inject(runtimeService);
        runtimeService.loadNativeLibrary();
        return runtimeService;
    }

    @Provides
    @Singleton
    DaemonService provideDaemonService(DeviceRuntimeService deviceRuntimeService) {
        DaemonService daemonService = new DaemonService(deviceRuntimeService);
        mFizzleApplication.getInjectionComponent().inject(daemonService);
        return daemonService;
    }

    @Provides
    @Singleton
    CallService provideCallService() {
        CallService callService = new CallService();
        mFizzleApplication.getInjectionComponent().inject(callService);
        return callService;
    }

    @Provides
    @Singleton
    AccountService provideAccountService() {
        AccountService accountService = new AccountService();
        mFizzleApplication.getInjectionComponent().inject(accountService);
        return accountService;
    }

    @Provides
    @Singleton
    HardwareService provideHardwareService(Context context) {
        HardwareServiceImpl hardwareService = new HardwareServiceImpl(context);
        mFizzleApplication.getInjectionComponent().inject(hardwareService);
        return hardwareService;
    }

    @Provides
    @Singleton
    ContactService provideContactService(PreferencesService sharedPreferencesService) {
        ContactServiceImpl contactService = new ContactServiceImpl();
        mFizzleApplication.getInjectionComponent().inject(contactService);
        return contactService;
    }

    @Provides
    @Singleton
    ConversationFacade provideConversationFacade(
            HistoryService historyService,
            CallService callService,
            ContactService contactService,
            AccountService accountService,
            NotificationService notificationService) {
        ConversationFacade conversationFacade = new ConversationFacade(historyService, callService, accountService, contactService, notificationService);
        mFizzleApplication.getInjectionComponent().inject(conversationFacade);
        return conversationFacade;
    }

    @Provides
    @Singleton
    VCardService provideVCardService(Context context) {
        return new VCardServiceImpl(context);
    }

    @Provides
    @Named("DaemonExecutor")
    @Singleton
    ScheduledExecutorService provideDaemonExecutorService() {
        return Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "DRing"));
    }

    @Provides
    @Named("UiScheduler")
    @Singleton
    Scheduler provideUiScheduler() {
        return AndroidSchedulers.mainThread();
    }
}
