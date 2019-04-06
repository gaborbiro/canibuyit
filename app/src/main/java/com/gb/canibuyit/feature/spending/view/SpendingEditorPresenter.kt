package com.gb.canibuyit.feature.spending.view

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteConstraintException
import android.text.SpannableStringBuilder
import com.gb.canibuyit.base.view.BasePresenter
import com.gb.canibuyit.feature.monzo.ACCOUNT_ID_RETAIL
import com.gb.canibuyit.feature.monzo.data.MonzoInteractor
import com.gb.canibuyit.feature.project.data.Project
import com.gb.canibuyit.feature.project.data.ProjectInteractor
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import com.gb.canibuyit.feature.spending.model.CycleSpent
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.util.bold
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class SpendingEditorPresenter @Inject constructor(
    private val spendingInteractor: SpendingInteractor,
    private val projectInteractor: ProjectInteractor,
    private val monzoInteractor: MonzoInteractor
) : BasePresenter() {

    private var projectSettings: Project? = null
    private var disposable: Disposable? = null

    val screen: SpendingEditorScreen by screenDelegate()

    @SuppressLint("CheckResult")
    fun saveSpending(spending: Spending) {
        spendingInteractor.createOrUpdate(spending)
                .subscribe({
                    screen.onSpendingLoaded(spending)
                }) {
                    var throwable: Throwable = it
                    onError(throwable)
                    do {
                        if (throwable.cause == null || throwable is SQLiteConstraintException) {
                            break
                        } else {
                            throwable = throwable.cause as Throwable
                        }
                    } while (true)
                    onError(throwable)
                }
    }

    fun deleteSpentByCycle(spending: Spending) {
        spendingInteractor.deleteSpentByCycleData(spending)
    }

    fun deleteSpending(spending: Spending) {
        disposeOnDestroy(spendingInteractor.delete(spending.id!!)
                .subscribe(screen::onSpendingDeleted, this::onError))
    }

    fun showSpending(spendingId: Int) {
        disposeOnDestroy(spendingInteractor.get(spendingId)
                .subscribe(screen::onSpendingLoaded, this::onError))
        disposeOnDestroy(projectInteractor.getProject().subscribe({ project ->
            this.projectSettings = project
            screen.applyProjectSettingsToScreen(project)
        }, this::onError))
    }

    fun onViewSpentByCycleDetails(spentByCycle: CycleSpent, category: ApiSpending.Category) {
        disposable?.dispose()
        disposable = monzoInteractor.getRawTransactions(ACCOUNT_ID_RETAIL, spentByCycle.from,
                spentByCycle.to)
                .subscribe({
                    it.error?.let(this::onError)
                    it.content?.let {
                        screen.hideCycleSpendDetails()
                        val text = it
                                .filter { it.category == category }
                                .mapIndexed { index, transaction ->
                                    val amount = transaction.amount / 100.0
                                    "${index + 1}. ${transaction.created}: $amount\n\"${transaction.description?.replace(
                                            Regex("[\\s]+"), " ")}\"".bold(amount.toString())
                                }.joinTo(buffer = SpannableStringBuilder(), separator = "\n\n")
                        spentByCycle.apply {
                            screen.showCycleSpendDetails(
                                    title = "$from $to: $amount ($count)",
                                    text = text)
                        }
                    }
                }, this::onError)
        spentByCycle.apply {
            screen.showCycleSpendDetails(
                    title = "$from $to: $amount ($count)",
                    text = SpannableStringBuilder("Loading..."))
        }
    }

    fun onCloseSpentByCycleDetails() {
        disposable?.dispose()
    }
}