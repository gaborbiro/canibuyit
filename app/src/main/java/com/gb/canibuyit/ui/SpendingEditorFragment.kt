package com.gb.canibuyit.ui

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteConstraintException
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
import com.gb.canibuyit.interactor.ProjectInteractor
import com.gb.canibuyit.interactor.SpendingInteractor
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.times
import com.gb.canibuyit.model.plus
import com.gb.canibuyit.model.toDomainCycle
import com.gb.canibuyit.presenter.BasePresenter
import com.gb.canibuyit.screen.Screen
import com.gb.canibuyit.util.CurrencyUtils
import com.gb.canibuyit.util.DialogUtils
import com.gb.canibuyit.util.TextChangeListener
import com.gb.canibuyit.util.ValidationError
import com.gb.canibuyit.util.formatDayMonthYearWithPrefix
import com.gb.canibuyit.util.hideKeyboard
import com.gb.canibuyit.util.orNull
import kotlinx.android.synthetic.main.fragment_spending_editor.*
import java.text.NumberFormat
import java.time.LocalDate
import javax.inject.Inject

/**
 * A fragment representing a single Spending detail screen. This fragment is either
 * contained in a [MainActivity] in two-pane mode (on tablets) or a
 * [SpendingEditorActivity] on handsets.
 */
class SpendingEditorFragment : BaseFragment() {

    @Inject lateinit var spendingInteractor: SpendingInteractor
    @Inject lateinit var projectInteractor: ProjectInteractor
    @Inject lateinit var currencyUtils: CurrencyUtils

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
    private var projectSettings: Project? = null

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
            return Spending(
                    id = originalSpending?.id,
                    name = name_input.text.orNull()?.toString()
                            ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, name_input, "Please specify a name"),
                    notes = notes_input.text.orNull()?.toString(),
                    type = if (category_picker.selectedItem is ApiSpending.Category) category_picker.selectedItem as ApiSpending.Category else throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a category"),
                    value = NumberFormat.getInstance().parse((average_input.text.orNull()
                            ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, average_input, "Please specify an amount")).toString()).toDouble(),
                    fromStartDate = fromStartDate,
                    fromEndDate = fromEndDate,
                    occurrenceCount = occurrence_count_input.text.orNull()?.toString()?.toInt(),
                    cycleMultiplier = cycleMultiplierFromScreen,
                    cycle = cycle.toDomainCycle(),
                    enabled = enabled_switch.isChecked,
                    sourceData = originalSpending?.sourceData,
                    spent = originalSpending?.spent ?: 0.0,
                    // delete target history if empty
                    targets = target_input.text.orNull()?.toString()?.toDouble()?.let { target ->
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
            average_input.setText(currencyUtils.formatDecimal(spending.value, 20))
            spending.target?.let {
                target_input.setText(currencyUtils.formatDecimal(it, 20))
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
        name_override_cb.setOnCheckedChangeListener { _, isChecked -> projectSettings?.namePinned = isChecked }
        category_override_cb.setOnCheckedChangeListener { _, isChecked -> projectSettings?.categoryPinned = isChecked }
        average_override_cb.setOnCheckedChangeListener { _, isChecked -> projectSettings?.averagePinned = isChecked }
        cycle_override_cb.setOnCheckedChangeListener { _, isChecked -> projectSettings?.cyclePinned = isChecked }
        period_override_cb.setOnCheckedChangeListener { _, isChecked -> projectSettings?.whenPinned = isChecked }
    }

    override fun inject(): BasePresenter<Screen>? {
        Injector.INSTANCE.graph.inject(this)
        return null
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
            spendingInteractor.get(spendingId)
                    .subscribe(this::onSpendingLoaded, this::onError)
            projectInteractor.getProject().subscribe({ project ->
                this.projectSettings = project
                applyProjectSettingsToScreen(project)
            }, this::onError)
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
                        spendingInteractor.delete(it.id!!)
                                .subscribe({ deleteBtn?.isVisible = false }, this::onError)
                    }
                }
            }
        }
        return false
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
        try {
            val newSpending = displayedSpending
            spendingInteractor.createOrUpdate(newSpending)
                    .subscribe({
                        this@SpendingEditorFragment.originalSpending = newSpending
                        deleteBtn?.isVisible = true
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
            return true
        } catch (error: ValidationError) {
            context?.let(error::showError)
            return false
        }
    }

    private fun onSpendingLoaded(spending: Spending) {
        this.originalSpending = spending
        displayedSpending = spending
        deleteBtn?.isVisible = true
    }

    private fun applyProjectSettingsToScreen(project: Project) {
        name_override_cb.isChecked = project.namePinned
        category_override_cb.isChecked = project.categoryPinned
        average_override_cb.isChecked = project.averagePinned
        cycle_override_cb.isChecked = project.cyclePinned
        period_override_cb.isChecked = project.whenPinned
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
        try {
            val newSpending = displayedSpending
            val isNew = originalSpending == null && !isEmpty()
            val changed = originalSpending?.let { !it.compareForEditing(newSpending, false, false) }
                    ?: false
            return isNew || changed
        } catch (ve: ValidationError) {
            return true
        }
    }

    companion object {
        const val EXTRA_SPENDING_ID = "spending_id"
    }
}
