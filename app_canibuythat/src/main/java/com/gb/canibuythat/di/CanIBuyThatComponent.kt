package com.gb.canibuythat.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CanIBuyThatModule::class))
interface CanIBuyThatComponent : CanIBuyThatGraph {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): CanIBuyThatComponent
    }
}
