package com.gb.canibuythat.di

import android.app.Application

import javax.inject.Singleton

import dagger.BindsInstance
import dagger.Component

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
