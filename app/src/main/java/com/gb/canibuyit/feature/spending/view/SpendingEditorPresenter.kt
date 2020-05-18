package com.gb.canibuyit.feature.spending.view

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteConstraintException
import android.text.SpannableString
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
import com.gb.canibuyit.util.*
import io.reactivex.disposables.Disposable
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.absoluteValue

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

    fun onViewSpentByCycleDetails(cycleSpending: CycleSpending, category: DBSpending.Category) {
        disposable?.dispose()
        var title: SpannableString
        val since = max(cycleSpending.from.atStartOfDay(), LocalDateTime.now().minusDays(90))
        val before = cycleSpending.to
        disposable = monzoInteractor.getRawTransactions(ACCOUNT_ID_RETAIL, since, before).subscribe({
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
                        val amount = transaction.amount / 100f
                        val date = transaction.created.let {
                            it.format(TimeUtils.dayFormat) + it.dayOfMonth + getDayOfMonthSuffix(
                                it.dayOfMonth) + it.format(TimeUtils.restFormat)
                        }
                        "${index + 1}. ${date}: ${amount.reverseSign()}\n\"${transaction.description?.replace(
                            Regex("[\\s]+"), " ")}\"\n[${transaction.originalCategory.capitalize()}]".bold(amount.reverseSign())
                    }.joinTo(buffer = SpannableStringBuilder(), separator = "\n\n")

                val from = cycleSpending.from.toMonthDay()
                val to = cycleSpending.to.toMonthDay()
                val out = (outTotal / 100).absoluteValue
                val in_ = inTotal / 100
                fun Float.reverseSign() = (if (this >= 0) "+" else "-") + absoluteValue.toString()
                val total = cycleSpending.amount.toFloat().reverseSign()
                title = "Between $from and $to you\nspent: $out and received: $in_\n(Balance: $total)".bold(from, to, in_.toString(), out.toString(), total)
                cycleSpending.apply {
                    screen.showCycleSpendDetails(
                        title = title,
                        text = text)
                }
            }
        }, this::onError)
        cycleSpending.apply {
            screen.showCycleSpendDetails(
                title = SpannableStringBuilder("Loading..."),
                text = null)
        }
    }

    fun onCloseSpentByCycleDetails() {
        disposable?.dispose()
    }

    private fun getDayOfMonthSuffix(n: Int) = when {
        n in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
}