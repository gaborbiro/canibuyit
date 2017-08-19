package com.gb.canibuythat.rx;

import javax.inject.Inject;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SchedulerProviderImpl implements SchedulerProvider {

    @Inject
    SchedulerProviderImpl() {
        super();
    }

    @Override
    public Scheduler io() {
        return Schedulers.io();
    }

    @Override
    public Scheduler mainThread() {
        return AndroidSchedulers.mainThread();
    }
}
