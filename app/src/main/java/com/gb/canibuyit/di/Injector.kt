package com.gb.canibuyit.di

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.gb.canibuyit.error.ContextSource
import java.util.ArrayList

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
            return if (contextSources.size > 0) {
                contextSources[contextSources.size - 1].supportFragmentManager
            } else {
                null
            }
        }

    val context: Context?
        get() {
            return if (contextSources.size > 0) {
                contextSources[contextSources.size - 1].baseContext
            } else {
                null
            }
        }
}
