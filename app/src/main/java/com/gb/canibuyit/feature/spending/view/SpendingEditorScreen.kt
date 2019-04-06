package com.gb.canibuyit.feature.spending.view

import android.text.SpannableStringBuilder
import com.gb.canibuyit.base.view.Screen
import com.gb.canibuyit.feature.project.data.Project
import com.gb.canibuyit.feature.spending.model.Spending

interface SpendingEditorScreen : Screen {

    fun onSpendingDeleted()
    fun onSpendingLoaded(spending: Spending)
    fun applyProjectSettingsToScreen(project: Project)
    fun showCycleSpendDetails(title: String, text: SpannableStringBuilder)
    fun hideCycleSpendDetails()
}