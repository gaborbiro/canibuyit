package com.gb.canibuythat.interactor

import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Webhook
import com.gb.canibuythat.model.Webhooks
import com.gb.canibuythat.repository.MonzoRepository
import com.gb.canibuythat.rx.SchedulerProvider
import com.gb.canibuythat.util.clearLowerBits
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoInteractor @Inject
constructor(private val schedulerProvider: SchedulerProvider,
            private val monzoRepository: MonzoRepository,
            private val spendingInteractor: SpendingInteractor) {

    private val loginSubject: Subject<Lce<Login>> = PublishSubject.create<Lce<Login>>()

    fun getLoginDataStream(): Subject<Lce<Login>> {
        return loginSubject
    }

    fun login(authorizationCode: String): Disposable {
        return onErrorPrep(monzoRepository.login(authorizationCode)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    loginSubject.onNext(Lce.loading())
                })
                .subscribe({
                    loginSubject.onNext(Lce.content(it))
                }, { throwable ->
                    loginSubject.onNext(Lce.error(MonzoException(throwable)))
                })
    }

    fun loadSpendings(accountIds: List<String>, lastXMonths: Int?): Disposable {
        val since = lastXMonths?.let {
            clearLowerBits()
                    .also { it.add(Calendar.MONTH, -lastXMonths) }
                    .also { it.set(Calendar.DAY_OF_MONTH, 1) }
                    .time
        }
        return onErrorPrep(monzoRepository.getSpendings(accountIds, since)
                .subscribeOn(schedulerProvider.io())
                .doOnSubscribe {
                    spendingInteractor.getSpendingsDataStream().onNext(Lce.loading())
                })
                .subscribe(spendingInteractor::createOrUpdateMonzoCategories, { throwable ->
                    if (throwable is MonzoException && throwable.action == DomainException.Action.LOGIN) {
                        loginSubject.onNext(Lce.error(throwable))
                    } else {
                        spendingInteractor.getSpendingsDataStream().onNext(Lce.error(throwable))
                    }
                })
    }

    fun registerWebhook(accountId: String, url: String): Completable {
        return onErrorPrep(monzoRepository.registerWebhook(accountId, url)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread()))
    }

    fun getWebhooks(accountId: String): Single<Webhooks> {
        return onErrorPrep(monzoRepository.getWebhooks(accountId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread()))
    }

    fun deleteWebhook(webhook: Webhook): Completable {
        return onErrorPrep(monzoRepository.deleteWebhook(webhook.id)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread()))
    }

    private fun <T> onErrorPrep(single: Single<T>): Single<T> {
        return single.onErrorResumeNext {
            Single.error(handleException(it, this::handleHttpException, this::handleNetworkException, this::handleGenericException))
        }
    }

    private fun onErrorPrep(completable: Completable): Completable {
        return completable.onErrorResumeNext {
            Completable.error(handleException(it, this::handleHttpException, this::handleNetworkException, this::handleGenericException))
        }
    }

    private fun handleException(throwable: Throwable,
                                httpExceptionHandler: (Throwable) -> (DomainException),
                                networkExceptionHandler: (Throwable) -> (DomainException),
                                genericExceptionHandler: (Throwable) -> (DomainException)): DomainException {
        val domainException = DomainException(throwable)
        return when {
            domainException.kind == DomainException.Kind.HTTP -> httpExceptionHandler(throwable)
            domainException.kind == DomainException.Kind.NETWORK -> networkExceptionHandler(throwable)
            domainException.kind == DomainException.Kind.GENERIC -> genericExceptionHandler(throwable)
            else -> genericExceptionHandler(throwable)
        }
    }

    private fun handleHttpException(throwable: Throwable): MonzoException {
        return MonzoException(throwable)
    }

    private fun handleNetworkException(throwable: Throwable): DomainException {
        return DomainException("There seems to be a problem with your connection. Try again later", throwable)
    }

    private fun handleGenericException(throwable: Throwable): DomainException {
        return DomainException("Oops! Something went wrong", throwable)
    }
}
