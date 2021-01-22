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

import ch.seme.application.FizzleApplication;
import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Module
public class FizzleInjectionModule {

    private final FizzleApplication mFizzleApplication;

    public FizzleInjectionModule(FizzleApplication app) {
        mFizzleApplication = app;
    }

    @Provides
    FizzleApplication provideRingApplication() {
        return mFizzleApplication;
    }

    @Provides
    Context provideContext() {
        return mFizzleApplication;
    }

    @Provides
    Scheduler provideMainSchedulers() {
        return AndroidSchedulers.mainThread();
    }

}
