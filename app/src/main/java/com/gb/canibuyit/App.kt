package com.gb.canibuyit

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.Toast

import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.util.ActivityLifecycleCallbacksAdapter
import com.j256.ormlite.android.apptools.OpenHelperManager

class App : Application() {

    private var activityCount = 0

    init {
        Injector.INSTANCE.initializeCanIBuyItComponent(this)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ActivityWatcher())
    }

    private inner class ActivityWatcher : ActivityLifecycleCallbacksAdapter() {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            super.onActivityCreated(activity, savedInstanceState)
            activityCount++
        }

        override fun onActivityDestroyed(activity: Activity) {
            activityCount--
            if (activityCount <= 0) {
                Toast.makeText(this@App, "Releasing sqlite connection", Toast.LENGTH_SHORT).show()
                OpenHelperManager.releaseHelper()
            }
        }
    }
}
