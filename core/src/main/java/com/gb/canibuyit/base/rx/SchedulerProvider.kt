package com.gb.canibuyit.base.rx

import io.reactivex.Scheduler

interface SchedulerProvider {

    fun io(): Scheduler

    fun mainThread(): Scheduler
}
