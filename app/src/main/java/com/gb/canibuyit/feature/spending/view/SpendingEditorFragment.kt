package com.gb.canibuyit.feature.spending.view

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.gb.canibuyit.R
import com.gb.canibuyit.base.ui.PromptDialog
import com.gb.canibuyit.base.view.BaseFragment
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.feature.monzo.MONZO_CATEGORY
import com.gb.canibuyit.feature.project.data.Project
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.model.plus
import com.gb.canibuyit.feature.spending.model.times
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.feature.spending.ui.PlusOneAdapter
import com.gb.canibuyit.feature.spending.ui.ValidationError
import com.gb.canibuyit.util.DialogUtils
import com.gb.canibuyit.util.TextChangeListener
import com.gb.canibuyit.util.add
import com.gb.canibuyit.util.bold
import com.gb.canibuyit.util.formatDayMonthYearWithPrefix
import com.gb.canibuyit.util.hideKeyboard
import com.gb.canibuyit.util.link
import com.gb.canibuyit.util.orNull
import kotlinx.android.synthetic.main.fragment_spending_editor.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A fragment representing a single Spending detail screen. This fragment is either
 * contained in a [MainActivity] in two-pane mode (on tablets) or a
 * [SpendingEditorActivity] on handsets.
 */
class SpendingEditorFragment : BaseFragment<SpendingEditorScreen, SpendingEditorPresenter>(),
        SpendingEditorScreen {

    private var originalSpending: Spending? = null
    private var cycleMultiplierChanged: Boolean = false
    private var deleteBtn: MenuItem? = null
    private lateinit var rootView: ViewGroup
    private val keyboardDismisser = View.OnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN && rootView.focusedChild != null) {
            rootView.focusedChild.hideKeyboard()
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_spending_editor, container, false)
    }

    /**
     * @throws ValidationError
     */
    private fun getDisplayedSpending(): Spending {
        val fromStartDate = period_date_picker.startDate
        val fromEndDate = period_date_picker.endDate
        val cycle =
            if (cycle_picker.selectedItem is ApiSpending.Cycle) cycle_picker.selectedItem as ApiSpending.Cycle else throw ValidationError(
                    ValidationError.TYPE_NON_INPUT_FIELD, null,
                    "Please select a cycle")
        if (fromStartDate > fromEndDate) {
            throw ValidationError(
                    ValidationError.TYPE_NON_INPUT_FIELD, null,
                    "Start date must not be higher then end date")
        }
        val cycleMultiplier = cycle_multiplier_input.text.orNull()?.toString()?.toInt()
                ?: throw ValidationError(
                        ValidationError.TYPE_INPUT_FIELD,
                        cycle_multiplier_input,
                        "Please fill in")
        val lastValidDate = (fromStartDate + cycleMultiplier * cycle).minusDays(1)
        if (fromEndDate > lastValidDate) {
            throw ValidationError(
                    ValidationError.TYPE_NON_INPUT_FIELD, null,
                    "End date cannot be higher than " + lastValidDate.formatDayMonthYearWithPrefix())
        }
        val valueStr = average_input.text.orNull()?.toString()
                ?: throw ValidationError(
                        ValidationError.TYPE_INPUT_FIELD,
                        average_input,
                        "Please specify an amount")
        return Spending(
                id = originalSpending?.id,
                name = name_input.text.orNull()?.toString()
                        ?: throw ValidationError(
                                ValidationError.TYPE_INPUT_FIELD,
                                name_input,
                                "Please specify a name"),
                notes = notes_input.text.orNull()?.toString(),
                type = if (category_picker.selectedItem is ApiSpending.Category) category_picker.selectedItem as ApiSpending.Category else throw ValidationError(
                        ValidationError.TYPE_NON_INPUT_FIELD,
                        null, "Please select a category"),
                value = valueStr.toBigDecimal(),
                total = BigDecimal.ZERO,
                fromStartDate = fromStartDate,
                fromEndDate = fromEndDate,
                occurrenceCount = occurrence_count_input.text.orNull()?.toString()?.toInt(),
                cycleMultiplier = cycleMultiplier,
                cycle = cycle,
                enabled = enabled_switch.isChecked,
                sourceData = originalSpending?.sourceData,
                spent = originalSpending?.spent ?: BigDecimal.ZERO,
                spentByCycle = originalSpending?.spentByCycle,
                // if target was changed, add a new entry to the list or update the last one if it's still from today
                // because savings calculations are done at end of day, today's saving target will be locked down at midnight
                targets = target_input.text.orNull()?.toString()?.toInt()?.let { target ->
                    val now = LocalDate.now()
                    val targetHistory = originalSpending?.targets?.toMutableMap()
                    targetHistory?.maxBy { it.key }?.let {
                        if (it.value != target) {
                            if (it.key < now) {
                                targetHistory[now] = target
                            } else {
                                targetHistory[it.key] = target
                            }
                        }
                    }
                    targetHistory ?: mutableMapOf(Pair(now, target))
                },
                savings = null)
    }

    private fun setDisplayedSpending(spending: Spending) {
        activity?.title = "ID: ${spending.id}"
        name_input.setText(spending.name)
        average_input.setText(spending.value.toPlainString())
        spending.target?.let {
            target_input.setText(it.toString())
        } ?: let {
            target_input.text = null
        }
        enabled_switch.isChecked = spending.enabled
        category_picker.setSelection(spending.type.ordinal + 1)
        period_date_picker.startDate = spending.fromStartDate
        period_date_picker.endDate = spending.fromEndDate
        spending.occurrenceCount?.let {
            occurrence_count_input.setText(it.toString())
            average_lbl.text = "Value*: "
        } ?: let {
            occurrence_count_input.text = null
            average_lbl.text = "Average*: "
        }
        spending.cycleMultiplier.let { cycleMultiplier ->
            cycle_multiplier_input.setText(cycleMultiplier.toString())
            context?.resources?.apply {
                average_cycle_lbl.text =
                    " per $cycleMultiplier ${getQuantityString(spending.cycle.strRes,
                            cycleMultiplier)}"
                target_cycle_lbl.text =
                    " per $cycleMultiplier ${getQuantityString(spending.cycle.strRes,
                            cycleMultiplier)}"
            }
        }
        cycle_picker.setSelection(spending.cycle.ordinal + 1)
        notes_input.setText(spending.notes)
        spending.sourceData?.get(MONZO_CATEGORY)?.let {
            source_category_lbl.text = "(original Monzo category: $it)"
            source_category_lbl.isVisible = true
        }
        spending.spentByCycle?.let { list ->
            spent_by_cycle_list.isVisible = true
            spent_by_cycle_list.removeAllViews()
            spent_by_cycle_lbl.isVisible = !list.isEmpty()
            list.forEach { cycleSpent ->
                spent_by_cycle_list.add<TextView>(
                        com.gb.canibuyit.R.layout.list_item_spent_by_cycle)
                        .apply {
                            tag = cycleSpent.id!!
                            text =
                                "${cycleSpent.from} - ${cycleSpent.to}: ${cycleSpent.amount} (${cycleSpent.count})".link()
                                        .bold(cycleSpent.amount.toString())
                            setOnClickListener {
                                presenter.onViewSpentByCycleDetails(cycleSpent,
                                        originalSpending!!.type)
                            }
                        }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view as ViewGroup

        category_picker.adapter = PlusOneAdapter(activity!!, ApiSpending.Category.values())
        category_picker.setOnTouchListener(keyboardDismisser)

        cycle_picker.adapter = PlusOneAdapter(activity!!, ApiSpending.Cycle.values())
        cycle_picker.setOnTouchListener(keyboardDismisser)

        arguments?.let {
            if (originalSpending == null && it.containsKey(EXTRA_SPENDING_ID)) {
                showSpending(it.getInt(EXTRA_SPENDING_ID))
            } else {
                showSpending(null)
            }
        }

        cycle_multiplier_input.addTextChangedListener(object : TextChangeListener() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                cycleMultiplierChanged = true
            }
        })
        period_date_picker.touchInterceptor = { _ ->
            keyboardDismisser.onTouch(period_date_picker,
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            false
        }
    }

    override fun inject() {
        Injector.INSTANCE.graph.inject(this)
    }

    /**
     * @param spendingId can be null, in which case the content is cleared
     */
    fun showSpending(spendingId: Int?) {
        if (spendingId != null) {
            presenter.showSpending(spendingId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit, menu)
        this.deleteBtn = menu.findItem(R.id.menu_delete)

        arguments?.let {
            if (!it.containsKey(EXTRA_SPENDING_ID)) {
                deleteBtn?.isVisible = false
            }
        } ?: let {
            deleteBtn?.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val contentSaveStatus = shouldContentBeSaved()
                when (contentSaveStatus) {
                    is ContentStatus.ContentWasChanged -> {
                        presenter.saveSpending(contentSaveStatus.spending)
                    }
                    is ContentStatus.ContentWasChangedInSensitiveWays -> {
                        DialogUtils.getSaveDialog(context!!,
                                "Your modification will cause the \"Spent by cycle\" data to be recalculated.") {
                            presenter.deleteSpentByCycle(contentSaveStatus.spending)
                            presenter.saveSpending(contentSaveStatus.spending)
                        }.show()
                    }
                    is ContentStatus.ContentInvalid -> context?.let(
                            contentSaveStatus.validationError::showError)
                }
            }
            R.id.menu_delete -> {
                originalSpending?.let {
                    if (it.isPersisted) {
                        presenter.deleteSpending(it)
                    }
                }
            }
        }
        return false
    }

    override fun onSpendingDeleted() {
        deleteBtn?.isVisible = false
    }

    fun onFragmentClose(onFinish: () -> Unit) {
        val contentSaveStatus = shouldContentBeSaved()
        when (contentSaveStatus) {
            is ContentStatus.ContentWasChanged -> {
                DialogUtils.getSaveOrDiscardDialog(context!!, null, {
                    presenter.saveSpending(contentSaveStatus.spending)
                    true
                }, onFinish).show()
            }
            is ContentStatus.ContentWasChangedInSensitiveWays -> {
                DialogUtils.getSaveOrDiscardDialog(context!!,
                        "Your modification will cause the \"Spent by cycle\" data to be recalculated.",
                        {
                            presenter.deleteSpentByCycle(contentSaveStatus.spending)
                            presenter.saveSpending(contentSaveStatus.spending)
                            true
                        }, onFinish).show()
            }
            is ContentStatus.ContentUnchanged -> onFinish.invoke()
            is ContentStatus.ContentInvalid -> {
                DialogUtils.getDiscardDialog(context!!,
                        contentSaveStatus.validationError.errorMessage, onFinish)
                        .show()
            }
        }
    }

    override fun onSpendingLoaded(spending: Spending) {
        this.originalSpending = spending
        setDisplayedSpending(spending)
        deleteBtn?.isVisible = true
    }

    override fun applyProjectSettingsToScreen(project: Project) {
        name_override_cb.isChecked = project.namePinned
        category_override_cb.isChecked = project.categoryPinned
        average_override_cb.isChecked = project.averagePinned
        cycle_override_cb.isChecked = project.cyclePinned
        period_override_cb.isChecked = project.whenPinned

        name_override_cb.setOnCheckedChangeListener { _, isChecked ->
            project.namePinned = isChecked
        }
        category_override_cb.setOnCheckedChangeListener { _, isChecked ->
            project.categoryPinned = isChecked
        }
        average_override_cb.setOnCheckedChangeListener { _, isChecked ->
            project.averagePinned = isChecked
        }
        cycle_override_cb.setOnCheckedChangeListener { _, isChecked ->
            project.cyclePinned = isChecked
        }
        period_override_cb.setOnCheckedChangeListener { _, isChecked ->
            project.whenPinned = isChecked
        }

    }

    /**
     * @return true if the content differs from the originally loaded Spending, or if
     * this fragment contains unsaved user input
     */
    private fun shouldContentBeSaved(): ContentStatus {
        return try {
            val newSpending = getDisplayedSpending()
            val isNew = originalSpending == null && !isEmpty()
            val comparison = originalSpending?.compareForEditing(newSpending, false, false)
            when {
                isNew || comparison == Spending.ComparisonResult.Different -> ContentStatus.ContentWasChanged(
                        newSpending)
                comparison == Spending.ComparisonResult.DifferentSensitive -> ContentStatus.ContentWasChangedInSensitiveWays(
                        newSpending)
                else -> ContentStatus.ContentUnchanged
            }
        } catch (ve: ValidationError) {
            ContentStatus.ContentInvalid(ve)
        }
    }

    private var cycleSpendDetailsDialog: PromptDialog? = null

    override fun showCycleSpendDetails(title: String, text: SpannableStringBuilder) {
        cycleSpendDetailsDialog = PromptDialog.bigMessageDialog(title, text)
                .setPositiveButton(android.R.string.ok) {
                    presenter.onCloseSpentByCycleDetails()
                }
        cycleSpendDetailsDialog?.show(supportFragmentManager!!, "CycleSpendDetails")
    }

    override fun hideCycleSpendDetails() {
        cycleSpendDetailsDialog?.hide(supportFragmentManager!!)
    }

    private sealed class ContentStatus {
        object ContentUnchanged : ContentStatus()
        class ContentWasChanged(val spending: Spending) : ContentStatus()
        class ContentWasChangedInSensitiveWays(val spending: Spending) : ContentStatus()
        class ContentInvalid(val validationError: ValidationError) : ContentStatus()
    }

    private fun isEmpty(): Boolean {
        return name_input.text.isEmpty() &&
                notes_input.text.isEmpty() &&
                category_picker.selectedItem !is ApiSpending.Category &&
                average_input.text.isEmpty() &&
                !period_date_picker.isStartDateChanged &&
                !period_date_picker.isEndDateChanged &&
                occurrence_count_input.text.isEmpty() &&
                cycle_multiplier_input.text.toString() == "1" &&
                cycle_picker.selectedItem !is ApiSpending.Cycle &&
                !enabled_switch.isChecked &&
                target_input.text.isEmpty()
    }
}

const val EXTRA_SPENDING_ID = "spending_id"
