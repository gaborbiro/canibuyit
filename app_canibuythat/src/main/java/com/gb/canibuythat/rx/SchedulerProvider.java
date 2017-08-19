package com.gb.canibuythat.rx;

import io.reactivex.Scheduler;

public interface SchedulerProvider {
    Scheduler io();

    Scheduler mainThread();
}
