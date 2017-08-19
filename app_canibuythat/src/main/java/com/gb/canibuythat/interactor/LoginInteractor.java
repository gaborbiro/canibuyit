package com.gb.canibuythat.interactor;

import com.gb.canibuythat.exception.DomainException;
import com.gb.canibuythat.model.Login;
import com.gb.canibuythat.repository.LoginRepository;
import com.gb.canibuythat.rx.SchedulerProvider;

import javax.inject.Inject;

import io.reactivex.Single;

public class LoginInteractor {

    private SchedulerProvider schedulerProvider;
    private LoginRepository loginRepository;

    @Inject
    public LoginInteractor(SchedulerProvider schedulerProvider, LoginRepository loginRepository) {
        this.schedulerProvider = schedulerProvider;
        this.loginRepository = loginRepository;
    }

    public Single<Login> login(String authorizationCode) {
        return loginRepository.login(authorizationCode)
                .onErrorResumeNext(throwable -> Single.error(new DomainException(throwable)))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }
}
