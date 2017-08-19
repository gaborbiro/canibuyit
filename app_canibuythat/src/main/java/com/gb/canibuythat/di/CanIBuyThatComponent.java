package com.gb.canibuythat.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = {CanIBuyThatModule.class})
public interface CanIBuyThatComponent extends CanIBuyThatGraph {

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        CanIBuyThatComponent build();
    }
}
