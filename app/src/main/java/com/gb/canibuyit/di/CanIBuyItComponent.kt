package com.gb.canibuyit.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CanIBuyItModule::class))
interface CanIBuyItComponent : CanIBuyItGraph {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): CanIBuyItComponent
    }
}
