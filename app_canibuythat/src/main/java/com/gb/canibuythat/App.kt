package com.gb.canibuythat

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.Toast

import com.gb.canibuythat.di.Injector
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.jakewharton.threetenabp.AndroidThreeTen

class App : Application() {

    private var activityCount = 0

    init {
        Injector.INSTANCE.initializeCanIBuyThatComponent(this)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ActivityWatcher())
        AndroidThreeTen.init(this)
    }

    private inner class ActivityWatcher : ActivityLifecycleCallbacksAdapter() {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            super.onActivityCreated(activity, savedInstanceState)
            activityCount++
        }

        override fun onActivityDestroyed(activity: Activity) {
            activityCount++
            if (activityCount <= 0) {
                Toast.makeText(this@App, "Releasing sqlite connection blah", Toast.LENGTH_SHORT).show()
                OpenHelperManager.releaseHelper()
            }
        }
    }
}
