package com.gb.canibuyit.screen

import com.gb.canibuyit.interactor.Project
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.SpentByCycleUpdateUiModel

interface SpendingEditorScreen : Screen {
    fun onSpendingDeleted()
    fun setSpentByCycleEnabled(uiModel: SpentByCycleUpdateUiModel)
    fun onSpendingLoaded(spending: Spending)
    fun applyProjectSettingsToScreen(project: Project)
}