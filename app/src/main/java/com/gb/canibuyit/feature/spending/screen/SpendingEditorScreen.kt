package com.gb.canibuyit.feature.spending.screen

import android.text.SpannableString
import android.text.SpannableStringBuilder
import com.gb.canibuyit.feature.project.data.Project
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.screen.Screen

interface SpendingEditorScreen : Screen {
    fun onSpendingDeleted()
    fun onSpendingLoaded(spending: Spending)
    fun applyProjectSettingsToScreen(project: Project)
    fun showCycleSpendDetails(title: String, text: SpannableStringBuilder)
    fun hideCycleSpendDetails()
}