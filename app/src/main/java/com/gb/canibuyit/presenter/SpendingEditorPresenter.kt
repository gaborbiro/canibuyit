package com.gb.canibuyit.presenter

import android.database.sqlite.SQLiteConstraintException
import com.gb.canibuyit.interactor.Project
import com.gb.canibuyit.interactor.ProjectInteractor
import com.gb.canibuyit.interactor.SpendingInteractor
import com.gb.canibuyit.model.CycleSpent
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.screen.SpendingEditorScreen
import javax.inject.Inject

class SpendingEditorPresenter @Inject constructor(
    private val spendingInteractor: SpendingInteractor,
    private val projectInteractor: ProjectInteractor
) : BasePresenter<SpendingEditorScreen>() {

    private var projectSettings: Project? = null

    fun saveSpending(spending: Spending) {
        spendingInteractor.createOrUpdate(spending)
                .subscribe({
                    getScreen().onSpendingLoaded(spending)
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
        spendingInteractor.delete(spending.id!!)
                .subscribe(getScreen()::onSpendingDeleted, this::onError)
    }

    fun onSpentByCycleChecked(spentByCycle: CycleSpent, enabled: Boolean) {
        spendingInteractor.setSpentByCycleEnabled(spentByCycle, enabled)
    }

    fun onAllSpentByCycleChecked(spending: Spending, enabled: Boolean) {
        spendingInteractor.setAllSpentByCycleEnabled(spending, enabled)
    }

    fun showSpending(spendingId: Int) {
        disposeOnFinish(spendingInteractor.get(spendingId)
                .subscribe(getScreen()::onSpendingLoaded, this::onError))
        disposeOnFinish(projectInteractor.getProject().subscribe({ project ->
            this.projectSettings = project
            getScreen().applyProjectSettingsToScreen(project)
        }, this::onError))
    }
}