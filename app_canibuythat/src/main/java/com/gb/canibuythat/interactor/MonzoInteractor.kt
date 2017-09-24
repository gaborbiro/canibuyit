package com.gb.canibuythat.interactor

import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.Webhook
import com.gb.canibuythat.model.Webhooks
import com.gb.canibuythat.repository.MonzoRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
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
        return monzoRepository.login(authorizationCode)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    loginSubject.onNext(Lce.loading())
                }
                .subscribe({
                    loginSubject.onNext(Lce.content(it))
                }, { throwable ->
                    loginSubject.onNext(Lce.error(MonzoException(throwable)))
                })
    }

    fun getSpendingsDataStream(): Observable<Lce<List<Spending>>> {
        return spendingInteractor.getSpendingsDataStream()
    }

    fun loadSpendings(accountId: String): Disposable {
        return monzoRepository.getSpendings(accountId)
                .subscribeOn(schedulerProvider.io())
                .doOnSubscribe {
                    spendingInteractor.getSpendingsDataStream().onNext(Lce.loading())
                }
                .subscribe(spendingInteractor::createOrUpdateMonzoCategories, { throwable ->
                    onError(throwable, {
                        loadSpendings(accountId)
                    }, {
                        spendingInteractor.getSpendingsDataStream().onNext(Lce.error(it))
                    })
                })
    }

    fun registerWebhook(accountId: String, url: String): Completable {
        return monzoRepository.registerWebhook(accountId, url)
                .onErrorResumeNext { Completable.error(DomainException("Error registering for Monzo push notification", it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getWebhooks(accountId: String): Single<Webhooks> {
        return monzoRepository.getWebhooks(accountId)
                .onErrorResumeNext { Single.error(DomainException("Error fetching webhooks", it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun deleteWebhook(webhook: Webhook): Completable {
        return monzoRepository.deleteWebhook(webhook.id)
                .onErrorResumeNext { Completable.error(DomainException("Error deleting webhook", it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    /**
     * @param onRecover if the error is recoverable in a synchronous way, do this after successful recovery
     */
    private fun onError(throwable: Throwable, onRecover: () -> Unit, onFailToRecover: (MonzoException) -> Unit) {
        val e: MonzoException = MonzoException(throwable)
        if (e.action == DomainException.Action.LOGIN) {
            onFailToRecover(e)
            loginSubject.onNext(Lce.error(e))
        } else {
            onFailToRecover(e)
        }
    }
}
