package com.gb.canibuyit.di

import android.app.Application
import android.content.Context
import android.support.v4.app.FragmentManager
import com.gb.canibuyit.exception.ContextSource
import java.util.*

enum class Injector {

    INSTANCE;

    lateinit var graph: CanIBuyItGraph

    private val contextSources = ArrayList<ContextSource>()

    fun initializeCanIBuyItComponent(application: Application) {
        graph = DaggerCanIBuyItComponent.builder().application(application).build()
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
