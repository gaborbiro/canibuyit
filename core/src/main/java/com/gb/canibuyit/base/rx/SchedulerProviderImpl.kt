package com.gb.canibuyit.base.rx

import javax.inject.Inject

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SchedulerProviderImpl @Inject
internal constructor() : SchedulerProvider {

    override fun io(): Scheduler {
        return Schedulers.io()
    }

    override fun mainThread(): Scheduler {
        return AndroidSchedulers.mainThread()
    }
}
