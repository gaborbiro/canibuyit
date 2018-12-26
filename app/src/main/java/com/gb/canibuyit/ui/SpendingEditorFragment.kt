package com.gb.canibuyit.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.gb.canibuyit.R
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.interactor.Project
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.SpentByCycleUpdateUiModel
import com.gb.canibuyit.model.plus
import com.gb.canibuyit.model.times
import com.gb.canibuyit.model.toDomainCycle
import com.gb.canibuyit.presenter.SpendingEditorPresenter
import com.gb.canibuyit.screen.SpendingEditorScreen
import com.gb.canibuyit.util.DialogUtils
import com.gb.canibuyit.util.TextChangeListener
import com.gb.canibuyit.util.ValidationError
import com.gb.canibuyit.util.add
import com.gb.canibuyit.util.formatDayMonthYearWithPrefix
import com.gb.canibuyit.util.hideKeyboard
import com.gb.canibuyit.util.invisible
import com.gb.canibuyit.util.orNull
import com.gb.canibuyit.util.show
import kotlinx.android.synthetic.main.fragment_spending_editor.*
import kotlinx.android.synthetic.main.list_item_spent_by_cycle.view.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A fragment representing a single Spending detail screen. This fragment is either
 * contained in a [MainActivity] in two-pane mode (on tablets) or a
 * [SpendingEditorActivity] on handsets.
 */
class SpendingEditorFragment : BaseFragment<SpendingEditorScreen, SpendingEditorPresenter>(), SpendingEditorScreen {

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

    private var displayedSpending: Spending
        get() {
            val fromStartDate = period_date_picker.startDate
            val fromEndDate = period_date_picker.endDate
            val cycle = if (cycle_picker.selectedItem is ApiSpending.Cycle) cycle_picker.selectedItem as ApiSpending.Cycle else throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a cycle")
            if (fromStartDate > fromEndDate) {
                throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Start date must not be higher then end date")
            }
            val lastValidDate = (fromStartDate + cycleMultiplierFromScreen * cycle).minusDays(1)
            if (fromEndDate > lastValidDate) {
                throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null,
                        "End date cannot be higher than " + lastValidDate.formatDayMonthYearWithPrefix())
            }
            val valueStr = average_input.text.orNull()?.toString()
                    ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, average_input, "Please specify an amount")
            return Spending(
                    id = originalSpending?.id,
                    name = name_input.text.orNull()?.toString()
                            ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, name_input, "Please specify a name"),
                    notes = notes_input.text.orNull()?.toString(),
                    type = if (category_picker.selectedItem is ApiSpending.Category) category_picker.selectedItem as ApiSpending.Category else throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a category"),
                    value = valueStr.toBigDecimal(),
                    fromStartDate = fromStartDate,
                    fromEndDate = fromEndDate,
                    occurrenceCount = occurrence_count_input.text.orNull()?.toString()?.toInt(),
                    cycleMultiplier = cycleMultiplierFromScreen,
                    cycle = cycle.toDomainCycle(),
                    enabled = enabled_switch.isChecked,
                    sourceData = originalSpending?.sourceData,
                    spent = originalSpending?.spent ?: BigDecimal.ZERO,
                    spentByCycle = originalSpending?.spentByCycle,
                    // delete target history if empty
                    targets = target_input.text.orNull()?.toString()?.toInt()?.let { target ->
                        val now = LocalDate.now()
                        val targetHistory = originalSpending?.targets?.toMutableMap()
                        targetHistory?.maxBy { it.key }?.let {
                            if (it.value != target) {
                                if (it.key < LocalDate.now()) {
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
        @SuppressLint("SetTextI18n")
        set(spending) {
            name_input.setText(spending.name)
            average_input.setText(spending.value.toPlainString())
            spending.target?.let {
                target_input.setText(it)
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
                    average_cycle_lbl.text = " per $cycleMultiplier ${getQuantityString(spending.cycle.apiCycle.strRes, cycleMultiplier)}"
                    target_cycle_lbl.text = " per $cycleMultiplier ${getQuantityString(spending.cycle.apiCycle.strRes, cycleMultiplier)}"
                }
            }
            cycle_picker.setSelection(spending.cycle.apiCycle.ordinal + 1)
            notes_input.setText(spending.notes)
            spending.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY)?.let {
                source_category_lbl.text = "(original Monzo category: $it)"
                source_category_lbl.show()
            }
            spending.spentByCycle?.let { list ->
                spent_by_cycle_list.show()
                spent_by_cycle_lbl.show()
                list.forEach { cycleSpent ->
                    spent_by_cycle_list.add<ViewGroup>(R.layout.list_item_spent_by_cycle).apply {
                        tag = cycleSpent.id!!
                        toggle.isChecked = cycleSpent.enabled
                        toggle.text = "${cycleSpent.from} ${cycleSpent.to}: ${cycleSpent.amount} (${cycleSpent.count})"
                        toggle.setOnCheckedChangeListener { _, isChecked ->
                            presenter.onSpentByCycleChecked(cycleSpent, isChecked)
                        }
                    }
                }
                spent_by_cycle_toggle_all.apply {
                    show()
                    toggle.text = "Enable/disable all"
                    toggle.isChecked = list.any { it.enabled }
                    toggle.setOnCheckedChangeListener { _, isChecked -> presenter.onAllSpentByCycleChecked(spending, isChecked) }
                }
            }
        }

    private val cycleMultiplierFromScreen: Int
        get() = cycle_multiplier_input.text.orNull()?.toString()?.toInt()
                ?: originalSpending?.cycleMultiplier
                ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, cycle_multiplier_input, "Please fill in")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_spending_editor, container, false)
    }

    override fun setSpentByCycleEnabled(uiModel: SpentByCycleUpdateUiModel) {
        when (uiModel) {
            is SpentByCycleUpdateUiModel.AllLoading -> {
                spent_by_cycle_list.forEachChild {
                    it.toggle.isEnabled = false
                    it.progress_indicator.show()
                }
            }
            is SpentByCycleUpdateUiModel.AllFinished -> {
                spent_by_cycle_list.forEachChild {
                    it.toggle.isEnabled = true
                    it.progress_indicator.invisible()
                }
            }
            is SpentByCycleUpdateUiModel.Loading -> {
                spent_by_cycle_list.findViewWithTag<ViewGroup>(uiModel.cycleSpent?.id).apply {
                    toggle.isEnabled = false
                    progress_indicator.show()
                }
            }
            is SpentByCycleUpdateUiModel.Success, is SpentByCycleUpdateUiModel.Error -> {
                spent_by_cycle_list.findViewWithTag<ViewGroup>(uiModel.cycleSpent?.id).apply {
                    toggle.isEnabled = true
                    toggle.isChecked = uiModel.cycleSpent?.enabled ?: throw IllegalArgumentException("cycleSpent missing")
                    progress_indicator.invisible()
                }
            }
        }
    }

    private fun ViewGroup.forEachChild(apply: (View) -> Unit) {
        for (i in 0 until childCount) {
            apply(getChildAt(i))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view as ViewGroup

        category_picker.adapter = PlusOneAdapter(ApiSpending.Category.values())
        category_picker.setOnTouchListener(keyboardDismisser)

        cycle_picker.adapter = PlusOneAdapter(ApiSpending.Cycle.values())
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
            keyboardDismisser.onTouch(period_date_picker, MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            false
        }
    }

    override fun inject() {
        Injector.INSTANCE.graph.inject(this)
    }

    private inner class PlusOneAdapter internal constructor(items: Array<*>) : ArrayAdapter<Any>(activity, android.R.layout.simple_list_item_1, items) {

        override fun getCount(): Int {
            return super.getCount() + 1
        }

        override fun getItem(position: Int): Any? {
            return if (position == 0) "Select one" else super.getItem(position - 1)
        }

        override fun getItemId(position: Int): Long {
            return if (position == 0) -1 else super.getItemId(position - 1)
        }
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
            R.id.menu_save -> saveUserInputOrShowError()
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

    fun saveAndRun(onFinish: Runnable) {
        if (shouldSave()) {
            DialogUtils.getSaveOrDiscardDialog(activity, object : DialogUtils.Executable() {
                override fun run(): Boolean {
                    return saveUserInputOrShowError()
                }
            }, onFinish).show()
        } else {
            onFinish.run()
        }
    }

    /**
     * @return true if user data is valid
     */
    @Synchronized
    private fun saveUserInputOrShowError(): Boolean {
        return try {
            val newSpending = displayedSpending
            presenter.saveSpending(newSpending)
            true
        } catch (error: ValidationError) {
            context?.let(error::showError)
            false
        }
    }

    override fun onSpendingSaved(spending: Spending) {
        this@SpendingEditorFragment.originalSpending = spending
        deleteBtn?.isVisible = true
    }

    override fun onSpendingLoaded(spending: Spending) {
        this.originalSpending = spending
        displayedSpending = spending
        deleteBtn?.isVisible = true
    }

    override fun applyProjectSettingsToScreen(project: Project) {
        name_override_cb.isChecked = project.namePinned
        category_override_cb.isChecked = project.categoryPinned
        average_override_cb.isChecked = project.averagePinned
        cycle_override_cb.isChecked = project.cyclePinned
        period_override_cb.isChecked = project.whenPinned

        name_override_cb.setOnCheckedChangeListener { _, isChecked -> project.namePinned = isChecked }
        category_override_cb.setOnCheckedChangeListener { _, isChecked -> project.categoryPinned = isChecked }
        average_override_cb.setOnCheckedChangeListener { _, isChecked -> project.averagePinned = isChecked }
        cycle_override_cb.setOnCheckedChangeListener { _, isChecked -> project.cyclePinned = isChecked }
        period_override_cb.setOnCheckedChangeListener { _, isChecked -> project.whenPinned = isChecked }

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

    /**
     * @return true if the content differs from the originally loaded Spending, or if
     * this fragment contains unsaved user input
     */
    private fun shouldSave(): Boolean {
        return try {
            val newSpending = displayedSpending
            val isNew = originalSpending == null && !isEmpty()
            val changed = originalSpending?.let { !it.compareForEditing(newSpending, false, false) }
                    ?: false
            isNew || changed
        } catch (ve: ValidationError) {
            true
        }
    }

    companion object {
        const val EXTRA_SPENDING_ID = "spending_id"
    }
}
