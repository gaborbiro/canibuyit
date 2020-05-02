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
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.model.DBSpending
import com.gb.canibuyit.util.bold
import io.reactivex.disposables.Disposable
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SpendingEditorPresenter @Inject constructor(
    private val spendingInteractor: SpendingInteractor,
    private val projectInteractor: ProjectInteractor,
    private val monzoInteractor: MonzoInteractor
) : BasePresenter() {

    private var projectSettings: Project? = null
    private var disposable: Disposable? = null
    private val dayFormat = DateTimeFormatter.ofPattern("EEEE ")
    private val restFormat = DateTimeFormatter.ofPattern(" 'of' MMM, HH:mm:ss")

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

    fun onViewSpentByCycleDetails(cycleSpending: CycleSpending, category: DBSpending.Category) {
        disposable?.dispose()
        var cycleSpentText = cycleSpending.run { "$from - $to: $amount" }

        disposable = monzoInteractor.getRawTransactions(ACCOUNT_ID_RETAIL, cycleSpending.from.atStartOfDay(),
            cycleSpending.to)
            .subscribe({
                it.error?.let(this::onError)
                it.content?.let {
                    screen.hideCycleSpendDetails()
                    var inTotal = 0.0
                    var outTotal = 0.0
                    val text = it
                        .filter { it.category == category }
                        .mapIndexed { index, transaction ->
                            if (transaction.amount < 0) {
                                outTotal += transaction.amount
                            } else {
                                inTotal += transaction.amount
                            }
                            val amount = transaction.amount / 100.0
                            val date = transaction.created.let {
                                it.format(dayFormat) + it.dayOfMonth + getDayOfMonthSuffix(
                                    it.dayOfMonth) + it.format(restFormat)
                            }
                            "${index + 1}. ${date}: $amount\n\"${transaction.description?.replace(
                                Regex("[\\s]+"), " ")}\"\n[${transaction.originalCategory.capitalize()}]".bold(amount.toString())
                        }.joinTo(buffer = SpannableStringBuilder(), separator = "\n\n")
                    cycleSpentText += "\nOut: ${outTotal / 100} In: ${inTotal / 100}"
                    cycleSpending.apply {
                        screen.showCycleSpendDetails(
                            title = cycleSpentText,
                            text = text)
                    }
                }
            }, this::onError)
        cycleSpending.apply {
            screen.showCycleSpendDetails(
                title = cycleSpentText,
                text = SpannableStringBuilder("Loading..."))
        }
    }

    fun onCloseSpentByCycleDetails() {
        disposable?.dispose()
    }

    fun getDayOfMonthSuffix(n: Int) = when {
        n in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
}