package com.gb.canibuythat.ui

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
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.gb.canibuythat.R
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.interactor.Project
import com.gb.canibuythat.interactor.ProjectInteractor
import com.gb.canibuythat.interactor.SpendingInteractor
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.add
import com.gb.canibuythat.presenter.BasePresenter
import com.gb.canibuythat.screen.Screen
import com.gb.canibuythat.util.DateUtils
import com.gb.canibuythat.util.DialogUtils
import com.gb.canibuythat.util.TextChangeListener
import com.gb.canibuythat.util.ValidationError
import com.gb.canibuythat.util.clearLowerBits
import com.gb.canibuythat.util.hideKeyboard
import com.gb.canibuythat.util.orNull
import com.gb.canibuythat.util.toLocalDate
import org.threeten.bp.LocalDate
import java.util.*
import javax.inject.Inject

/**
 * A fragment representing a single Spending detail screen. This fragment is either
 * contained in a [MainActivity] in two-pane mode (on tablets) or a
 * [SpendingEditorActivity] on handsets.
 */
class SpendingEditorFragment : BaseFragment() {

    @Inject
    lateinit var spendingInteractor: SpendingInteractor
    @Inject
    lateinit var projectInteractor: ProjectInteractor

    private val nameInput: EditText by lazy { rootView.findViewById(R.id.name_input) as EditText }
    private val averageInput: EditText by lazy { rootView.findViewById(R.id.average_input) as EditText }
    private val averageLbl: TextView by lazy { rootView.findViewById(R.id.average_text) as TextView }
    private val targetInput: EditText by lazy { rootView.findViewById(R.id.target_input) as EditText }
    private val enabledCB: CompoundButton by lazy { rootView.findViewById(R.id.enabled_switch) as CompoundButton }
    private val categoryPicker: Spinner by lazy { rootView.findViewById(R.id.category_picker) as Spinner }
    private val occurrenceInput: EditText by lazy { rootView.findViewById(R.id.occurrence_count_input) as EditText }
    private val cycleMultiplierInput: EditText by lazy { rootView.findViewById(R.id.cycle_multiplier_input) as EditText }
    private val cyclePicker: Spinner by lazy { rootView.findViewById(R.id.cycle_picker) as Spinner }
    private val fromDatePicker: DateRangePicker by lazy { rootView.findViewById(R.id.from_date_picker) as DateRangePicker }
    private val notesInput: EditText by lazy { rootView.findViewById(R.id.notes_input) as EditText }
    //    private val spendingEventsLbl: TextView by lazy { rootView.findViewById(R.id.spending_events_text) as TextView }
    private val sourceCategoryLbl: TextView by lazy { rootView.findViewById(R.id.source_category_lbl) as TextView }
    private val nameOverrideCB: CheckBox by lazy { rootView.findViewById(R.id.name_override_cb) as CheckBox }
    private val categoryOverrideCB: CheckBox by lazy { rootView.findViewById(R.id.category_override_cb) as CheckBox }
    private val averageOverrideCB: CheckBox by lazy { rootView.findViewById(R.id.average_override_cb) as CheckBox }
    private val cycleOverrideCB: CheckBox by lazy { rootView.findViewById(R.id.cycle_override_cb) as CheckBox }
    private val whenOverrideCB: CheckBox by lazy { rootView.findViewById(R.id.when_override_cb) as CheckBox }
    private val averageCycleLbl: TextView by lazy { rootView.findViewById(R.id.average_cycle_lbl) as TextView }
    private val targetCycleLbl: TextView by lazy { rootView.findViewById(R.id.target_cycle_lbl) as TextView }

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
            val fromStartDate = fromDatePicker.startDate
            val fromEndDate = fromDatePicker.endDate
            val cycle = if (cyclePicker.selectedItem is ApiSpending.Cycle) cyclePicker.selectedItem as ApiSpending.Cycle else throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a cycle")
            if (fromStartDate.after(fromEndDate)) {
                throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Start date must not be higher then end date")
            }
            Calendar.getInstance().let {
                it.time = fromStartDate
                it.clearLowerBits()
                it.add(cycle, cycleMultiplierFromScreen)
                it.add(Calendar.DAY_OF_MONTH, -1)
                if (fromEndDate.after(it.time)) {
                    throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null,
                            "End date cannot be higher than " + DateUtils.formatDayMonthYearWithPrefix(it.time))
                }
            }
            return Spending(
                    id = originalSpending?.id,
                    name = nameInput.text.orNull()?.toString()
                            ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, nameInput, "Please specify a name"),
                    notes = notesInput.text.orNull()?.toString(),
                    type = if (categoryPicker.selectedItem is ApiSpending.Category) categoryPicker.selectedItem as ApiSpending.Category else throw ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a category"),
                    value = averageInput.text.orNull()?.toString()?.toDouble()
                            ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, averageInput, "Please specify a value"),
                    fromStartDate = fromStartDate,
                    fromEndDate = fromEndDate,
                    occurrenceCount = occurrenceInput.text.orNull()?.toString()?.toInt(),
                    cycleMultiplier = cycleMultiplierFromScreen,
                    cycle = cycle,
                    enabled = enabledCB.isChecked,
                    sourceData = originalSpending?.sourceData,
                    spent = originalSpending?.spent,
                    // delete target history if empty
                    targets = targetInput.text.orNull()?.toString()?.toDouble()?.let { target ->
                        val now = Date()
                        val targetHistory = originalSpending?.targets?.toMutableMap()
                        targetHistory?.maxBy { it.key }?.let {
                            if (it.value != target) {
                                if (it.key.toLocalDate().isBefore(LocalDate.now())) {
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
            nameInput.setText(spending.name)
            averageInput.setText(getString(R.string.detail_amount, spending.value))
            spending.target?.let {
                targetInput.setText(getString(R.string.detail_amount, it))
            } ?: let {
                targetInput.text = null
            }
            enabledCB.isChecked = spending.enabled
            categoryPicker.setSelection(spending.type.ordinal + 1)
            fromDatePicker.startDate = spending.fromStartDate
            fromDatePicker.endDate = spending.fromEndDate
            spending.occurrenceCount?.let {
                occurrenceInput.setText(it.toString())
                averageLbl.text = "Value*: "
            } ?: let {
                occurrenceInput.text = null
                averageLbl.text = "Average*: "
            }
            spending.cycleMultiplier.let { cycleMultiplier ->
                cycleMultiplierInput.setText(cycleMultiplier.toString())
                context.resources.apply {
                    averageCycleLbl.text = " per $cycleMultiplier ${getQuantityString(spending.cycle.strRes, cycleMultiplier)}"
                    targetCycleLbl.text = " per $cycleMultiplier ${getQuantityString(spending.cycle.strRes, cycleMultiplier)}"
                }
            }
            cyclePicker.setSelection(spending.cycle.ordinal + 1)
            notesInput.setText(spending.notes)
            spending.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY)?.let {
                sourceCategoryLbl.text = "\t(monzo: $it)"
            }
        }

    private val cycleMultiplierFromScreen: Int
        get() = cycleMultiplierInput.text.orNull()?.toString()?.toInt()
                ?: originalSpending?.cycleMultiplier
                ?: throw ValidationError(ValidationError.TYPE_INPUT_FIELD, cycleMultiplierInput, "Please fill in")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_spending_editor, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view as ViewGroup

        categoryPicker.adapter = PlusOneAdapter(ApiSpending.Category.values())
        categoryPicker.setOnTouchListener(keyboardDismisser)

        cyclePicker.adapter = PlusOneAdapter(ApiSpending.Cycle.values())
        cyclePicker.setOnTouchListener(keyboardDismisser)

        if (originalSpending == null && arguments != null && arguments.containsKey(EXTRA_SPENDING_ID)) {
            showSpending(arguments.getInt(EXTRA_SPENDING_ID))
        } else {
            showSpending(null)
        }

        cycleMultiplierInput.addTextChangedListener(object : TextChangeListener() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                cycleMultiplierChanged = true
            }
        })
        fromDatePicker.setTouchInterceptor { _ ->
            keyboardDismisser.onTouch(fromDatePicker, MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            false
        }
        nameOverrideCB.setOnCheckedChangeListener { _, isChecked -> projectSettings?.namePinned = isChecked }
        categoryOverrideCB.setOnCheckedChangeListener { _, isChecked -> projectSettings?.categoryPinned = isChecked }
        averageOverrideCB.setOnCheckedChangeListener { _, isChecked -> projectSettings?.averagePinned = isChecked }
        cycleOverrideCB.setOnCheckedChangeListener { _, isChecked -> projectSettings?.cyclePinned = isChecked }
        whenOverrideCB.setOnCheckedChangeListener { _, isChecked -> projectSettings?.whenPinned = isChecked }
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
                    .subscribe({ spending -> onSpendingLoaded(spending) }, { this.onError(it) })
            projectInteractor.getProject().subscribe({ project ->
                this.projectSettings = project
                applyProjectSettingsToScreen(project)
            }, this::onError)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit, menu)
        this.deleteBtn = menu.findItem(R.id.menu_delete)

        if (arguments == null || !arguments.containsKey(EXTRA_SPENDING_ID)) {
            deleteBtn?.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveUserInputOrShowError()
            R.id.menu_delete -> if (originalSpending != null && originalSpending!!.isPersisted) {
                spendingInteractor.delete(originalSpending!!.id!!)
                        .subscribe({ deleteBtn?.isVisible = false }, this::onError)
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
            error.showError(activity)
            return false
        }
    }

    private fun onSpendingLoaded(spending: Spending) {
        this.originalSpending = spending
        displayedSpending = spending
        deleteBtn?.isVisible = true
    }

    private fun applyProjectSettingsToScreen(project: Project) {
        nameOverrideCB.isChecked = project.namePinned
        categoryOverrideCB.isChecked = project.categoryPinned
        averageOverrideCB.isChecked = project.averagePinned
        cycleOverrideCB.isChecked = project.cyclePinned
        whenOverrideCB.isChecked = project.whenPinned
    }

    private fun isEmpty(): Boolean {
        return nameInput.text.isEmpty() &&
                notesInput.text.isEmpty() &&
                categoryPicker.selectedItem !is ApiSpending.Category &&
                averageInput.text.isEmpty() &&
                !fromDatePicker.isStartDateChanged &&
                !fromDatePicker.isEndDateChanged &&
                occurrenceInput.text.isEmpty() &&
                cycleMultiplierInput.text.toString() == "1" &&
                cyclePicker.selectedItem !is ApiSpending.Cycle &&
                !enabledCB.isChecked &&
                targetInput.text.isEmpty()
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
