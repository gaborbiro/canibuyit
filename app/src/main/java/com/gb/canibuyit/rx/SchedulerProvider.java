package com.gb.canibuyit.rx;

import io.reactivex.Scheduler;

public interface SchedulerProvider {
    Scheduler io();

    Scheduler mainThread();
}
