package com.gb.canibuyit.model

sealed class SpentByCycleUpdateUiModel(val cycleSpent: CycleSpent? = null) {
    object AllLoading : SpentByCycleUpdateUiModel()
    object AllFinished : SpentByCycleUpdateUiModel()
    class Loading(cycleSpent: CycleSpent) : SpentByCycleUpdateUiModel(cycleSpent)
    class Success(cycleSpent: CycleSpent) : SpentByCycleUpdateUiModel(cycleSpent)
    class Error(cycleSpent: CycleSpent, val error: String) : SpentByCycleUpdateUiModel(cycleSpent)
}