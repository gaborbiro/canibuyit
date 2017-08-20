package com.gb.canibuythat.interactor;

import com.gb.canibuythat.exception.MonzoException;
import com.gb.canibuythat.model.Account;
import com.gb.canibuythat.model.Login;
import com.gb.canibuythat.repository.MonzoRepository;
import com.gb.canibuythat.rx.SchedulerProvider;

import javax.inject.Inject;

import io.reactivex.Single;

public class MonzoInteractor {

    private SchedulerProvider schedulerProvider;
    private MonzoRepository monzoRepository;

    @Inject
    public MonzoInteractor(SchedulerProvider schedulerProvider, MonzoRepository monzoRepository) {
        this.schedulerProvider = schedulerProvider;
        this.monzoRepository = monzoRepository;
    }

    public Single<Login> login(String authorizationCode) {
        return monzoRepository.login(authorizationCode)
                .onErrorResumeNext(throwable -> Single.error(new MonzoException(throwable)))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Single<Login> refresh(String refreshToken) {
        return monzoRepository.refresh(refreshToken)
                .onErrorResumeNext(throwable -> Single.error(new MonzoException(throwable)))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Single<Account[]> accounts() {
        return monzoRepository.accounts()
                .onErrorResumeNext(throwable -> Single.error(new MonzoException(throwable)))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }
}
