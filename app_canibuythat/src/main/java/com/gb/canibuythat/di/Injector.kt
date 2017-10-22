package com.gb.canibuythat.di

import android.app.Application
import android.content.Context
import android.support.v4.app.FragmentManager

import com.gb.canibuythat.exception.ContextSource

import java.util.ArrayList

enum class Injector {

    INSTANCE;

    lateinit var graph: CanIBuyThatGraph

    private val contextSources = ArrayList<ContextSource>()

    fun initializeCanIBuyThatComponent(application: Application) {
        graph = DaggerCanIBuyThatComponent.builder().application(application).build()
    }

    fun registerContextSource(contextSource: ContextSource) {
        this.contextSources.add(contextSource)
    }

    fun unregisterContextSource(contextSource: ContextSource) {
        this.contextSources.remove(contextSource)
    }

    val fragmentManager: FragmentManager?
        get() {
            if (contextSources.size > 0) {
                return contextSources[contextSources.size - 1].supportFragmentManager
            } else {
                return null
            }
        }

    val context: Context?
        get() {
            if (contextSources.size > 0) {
                return contextSources[contextSources.size - 1].baseContext
            } else {
                return null
            }
        }
}
