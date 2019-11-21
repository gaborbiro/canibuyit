package com.gb.canibuyit.feature.monzo.data

import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.base.model.Lce
import com.gb.canibuyit.base.rx.SchedulerProvider
import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.monzo.error.MonzoException
import com.gb.canibuyit.feature.monzo.model.Login
import com.gb.canibuyit.feature.monzo.model.Transaction
import com.gb.canibuyit.feature.monzo.model.Webhook
import com.gb.canibuyit.feature.monzo.model.Webhooks
import com.gb.canibuyit.feature.spending.model.Spending
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoInteractor @Inject
constructor(private val schedulerProvider: SchedulerProvider,
            private val monzoRepository: MonzoRepository,
            private val userPreferences: UserPreferences) {

    private val loginSubject = PublishSubject.create<Lce<Login>>()
    private val monzoSpendingsSubject = PublishSubject.create<Lce<List<Spending>>>()

    fun subscribeToLogin(onNext: (Lce<Login>) -> Unit, onError: (Throwable) -> Unit): Disposable {
        return loginSubject.subscribe(onNext, onError)
    }

    fun subscribeToMonzoSpendings(onNext: (Lce<List<Spending>>) -> Unit,
                                  onError: (Throwable) -> Unit): Disposable {
        return monzoSpendingsSubject.subscribe(onNext, onError)
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

    fun loadSpendings(accountId: String, lastXMonths: Long?): Disposable {
        val since = lastXMonths?.let {
            LocalDateTime.now().minusMonths(it)
        }
        return monzoRepository.getSpendings(accountId, since, before = LocalDate.now())
            .subscribeOn(schedulerProvider.io())
            .doOnSubscribe {
                monzoSpendingsSubject.onNext(Lce.loading())
            }
            .onErrorResumeNext {
                Single.error(DomainException(it))
            }
            .subscribe({
                monzoSpendingsSubject.onNext(Lce.content(it))
                userPreferences.lastUpdate = LocalDateTime.now()
            }, { throwable ->
                if (throwable is MonzoException && throwable.action == DomainException.Action.LOGIN) {
                    loginSubject.onNext(Lce.error(throwable))
                } else {
                    monzoSpendingsSubject.onNext(Lce.error(throwable))
                }
            })
    }

    fun getRawTransactions(accountId: String, since: LocalDateTime,
                           before: LocalDate): Observable<Lce<List<Transaction>>> {
        return monzoRepository.getRawTransactions(accountId, since, before)
            .toObservable()
            .map(this::mapSuccess)
            .startWith(Lce.loading())
            .onErrorReturn { throwable: Throwable ->
                Lce.error(DomainException(throwable))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun registerWebhook(accountId: String, url: String): Completable {
        return monzoRepository.registerWebHook(accountId, url)
            .onErrorResumeNext {
                Completable.error(DomainException(it))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun getWebhooks(accountId: String): Single<Webhooks> {
        return monzoRepository.getWebHooks(accountId)
            .onErrorResumeNext {
                Single.error(DomainException(it))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun deleteWebhook(webhook: Webhook): Completable {
        return monzoRepository.deleteWebHook(webhook.id)
            .onErrorResumeNext {
                Completable.error(DomainException(it))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    private fun mapSuccess(success: List<Transaction>): Lce<List<Transaction>> {
        return Lce.content(content = success)
    }
}
