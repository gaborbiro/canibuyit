package com.gb.canibuythat.ui;

import android.app.DatePickerDialog;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.interactor.SpendingInteractor;
import com.gb.canibuythat.model.Spending;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.ArrayUtils;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.TextChangeListener;
import com.gb.canibuythat.util.ViewUtils;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * A fragment representing a single Spending detail screen. This fragment is either
 * contained in a {@link MainActivity} in two-pane mode (on tablets) or a
 * {@link SpendingEditorActivity} on handsets.
 */
public class SpendingEditorFragment extends BaseFragment {

    public static final String EXTRA_SPENDING_ID = "spending_id";

    private static final Date DEFAULT_END_DATE;
    private static final Date DEFAULT_START_DATE;

    static {
        Calendar c = Calendar.getInstance();
        DateUtils.clearLowerBits(c);
        DEFAULT_END_DATE = c.getTime();
        DEFAULT_START_DATE = c.getTime();
    }

    @Inject UserPreferences userPreferences;
    @Inject SpendingInteractor spendingInteractor;

    @BindView(R.id.name) EditText nameInput;
    @BindView(R.id.amount) EditText averageInput;
    @BindView(R.id.target) EditText targetInput;
    @BindView(R.id.enabled) CheckBox enabledCB;
    @BindView(R.id.category) Spinner categoryPicker;
    @BindView(R.id.date_from) Button startDateBtn;
    @BindView(R.id.date_to) Button endDateBtn;
    @BindView(R.id.occurrence_count) EditText occurrenceInput;
    @BindView(R.id.cycle_multiplier) EditText cycleMultiplierInput;
    @BindView(R.id.cycle_picker) Spinner cyclePicker;
    @BindView(R.id.notes) EditText notesInput;
    @BindView(R.id.spending_events) TextView spendingEventsLayout;

    private Spending originalSpending;
    private boolean startDateChanged;
    private boolean endDateChanged;
    private boolean cycleMultiplierChanged;

    private DatePickerDialog startDatePickerDialog;
    private DatePickerDialog endDatePickerDialog;
    private MenuItem deleteBtn;
    private ViewGroup rootView;
    private View.OnTouchListener keyboardDismisser = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && rootView.getFocusedChild() != null) {
                ViewUtils.hideKeyboard(rootView.getFocusedChild());
            }
            return false;
        }
    };
    private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, monthOfYear);
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            DateUtils.clearLowerBits(c);

            switch ((int) view.getTag()) {
                case R.id.date_from:
                    startDateBtn.setText(DateUtils.FORMAT_MONTH_DAY.format(c.getTime()));
                    Date endDate = getEndDateFromScreen();

                    if (endDate.getTime() < c.getTime().getTime()) {
                        endDatePickerDialog = new DatePickerDialog(getActivity(),
                                dateSetListener, c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH));
                        applyEndDateToScreen(c.getTime());
                    }
                    startDateChanged = !c.getTime().equals(DEFAULT_START_DATE);
                    break;
                case R.id.date_to:
                    endDateBtn.setText(DateUtils.FORMAT_MONTH_DAY.format(c.getTime()));
                    Date startDate = getStartDateFromScreen();

                    if (startDate.getTime() > c.getTime().getTime()) {
                        startDatePickerDialog = new DatePickerDialog(getActivity(),
                                dateSetListener, c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH));
                        applyFirstFromDateToScreen(c.getTime());
                    }
                    endDateChanged = !c.getTime().equals(DEFAULT_END_DATE);
                    break;
            }
        }
    };
    private View.OnClickListener datePickerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.date_from:
                    getStartDatePickerDialog().show();
                    startDateBtn.setError(null);
                    break;
                case R.id.date_to:
                    getEndDatePickerDialog().show();
                    endDateBtn.setError(null);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_spending_editor, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = (ViewGroup) view;

        categoryPicker.setAdapter(new PlusOneAdapter(Spending.Category.values()));
        categoryPicker.setOnTouchListener(keyboardDismisser);

        cyclePicker.setAdapter(new PlusOneAdapter(Spending.Cycle.values()));
        cyclePicker.setOnTouchListener(keyboardDismisser);
        cyclePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                endDateBtn.setError(null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing to do
            }
        });

        startDateBtn.setOnClickListener(datePickerOnClickListener);
        startDateBtn.setOnTouchListener(keyboardDismisser);
        endDateBtn.setOnClickListener(datePickerOnClickListener);
        endDateBtn.setOnTouchListener(keyboardDismisser);

        if (originalSpending == null && getArguments() != null && getArguments().containsKey(EXTRA_SPENDING_ID)) {
            showSpending(getArguments().getInt(EXTRA_SPENDING_ID), true);
        } else {
            showSpending(null, true);
        }

        cycleMultiplierInput.addTextChangedListener(new TextChangeListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cycleMultiplierChanged = true;
            }
        });
    }

    @Override
    protected void inject() {
        Injector.INSTANCE.getGraph().inject(this);
    }

    private class PlusOneAdapter extends ArrayAdapter {

        PlusOneAdapter(Object[] items) {
            super(getActivity(), android.R.layout.simple_list_item_1, items);
        }

        @Override
        public int getCount() {
            return super.getCount() + 1;
        }

        @Override
        public Object getItem(int position) {
            return position == 0 ? "Select one" : super.getItem(position - 1);
        }

        @Override
        public long getItemId(int position) {
            return position == 0 ? -1 : super.getItemId(position - 1);
        }
    }

    /**
     * @param spendingId           can be null, in which case the content is cleared
     * @param showKeyboardWhenDone after data has been loaded from the database and
     *                             displayed, focus on the name
     *                             EditText and show the keyboard
     */
    public void showSpending(Integer spendingId, final boolean showKeyboardWhenDone) {
        if (spendingId != null) {
            spendingInteractor.read(spendingId)
                    .subscribe(spending -> onSpendingLoaded(spending, showKeyboardWhenDone), this::onError);
        } else {
            applyFirstFromDateToScreen(DEFAULT_START_DATE);
            applyEndDateToScreen(DEFAULT_END_DATE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit, menu);
        this.deleteBtn = menu.findItem(R.id.menu_delete);

        if (getArguments() == null || !getArguments().containsKey(EXTRA_SPENDING_ID)) {
            deleteBtn.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                saveUserInputOrShowError();
                break;
            case R.id.menu_delete:
                if (originalSpending != null && originalSpending.isPersisted()) {
                    spendingInteractor.delete(originalSpending.getId())
                            .subscribe(() -> deleteBtn.setVisible(false), this::onError);
                }
                break;
        }
        return false;
    }

    public void saveAndRun(Runnable onSaveOrDiscard) {
        if (shouldSave()) {
            DialogUtils.getSaveOrDiscardDialog(getActivity(), new DialogUtils.Executable() {
                @Override
                public boolean run() {
                    return saveUserInputOrShowError();
                }
            }, onSaveOrDiscard).show();
        } else {
            onSaveOrDiscard.run();
        }
    }

    /**
     * @return true if user data is valid
     */
    private synchronized boolean saveUserInputOrShowError() {
        ValidationError error = validateUserInput();
        if (error == null) {
            final Spending newSpending = getSpendingFromScreen();
            spendingInteractor.createOrUpdate(newSpending)
                    .subscribe(createOrUpdateStatus -> {
                        SpendingEditorFragment.this.originalSpending = newSpending;
                        deleteBtn.setVisible(true);
                        loadSpendingOccurrences(newSpending);
                    }, throwable -> {
                        onError(throwable);
                        do {
                            if (throwable.getCause() == null || throwable instanceof SQLiteConstraintException) {
                                break;
                            } else {
                                throwable = throwable.getCause();
                            }
                        } while (true);
                        onError(throwable);
                    });
            return true;
        } else {
            error.showError();
            return false;
        }
    }

    private void onSpendingLoaded(Spending spending, boolean showKeyboardWhenDone) {
        this.originalSpending = spending;
        endDatePickerDialog = null;
        startDatePickerDialog = null;
        applySpendingToScreen(spending);

        if (showKeyboardWhenDone && isAdded()) {
            ViewUtils.showKeyboard(nameInput);
        }

        if (deleteBtn != null) {
            deleteBtn.setVisible(true);
        }
    }

    private void applySpendingToScreen(final Spending spending) {
        nameInput.setText(spending.getName());
        if (spending.getAverage() != null) {
            averageInput.setText(getString(R.string.detail_amount, spending.getAverage()));
        } else {
            averageInput.setText(null);
        }
        if (spending.getTarget() != null) {
            targetInput.setText(getString(R.string.detail_amount, spending.getTarget()));
        } else {
            targetInput.setText(null);
        }
        enabledCB.setChecked(spending.getEnabled());
        if (spending.getType() != null) {
            categoryPicker.setSelection(spending.getType().ordinal() + 1);
        }

        Date firstOccurrenceStart = spending.getFromStartDate() != null
                ? spending.getFromStartDate()
                : DEFAULT_START_DATE;
        applyFirstFromDateToScreen(firstOccurrenceStart);

        Date firstOccurrenceEnd = spending.getFromEndDate() != null ? spending.getFromEndDate() : DEFAULT_END_DATE;
        applyEndDateToScreen(firstOccurrenceEnd);

        if (spending.getOccurrenceCount() != null) {
            occurrenceInput.setText(Integer.toString(spending.getOccurrenceCount()));
        } else {
            occurrenceInput.setText(null);
        }

        if (spending.getCycleMultiplier() != null) {
            cycleMultiplierInput.setText(Integer.toString(spending.getCycleMultiplier()));
        } else {
            cycleMultiplierInput.setText(null);
        }

        if (spending.getCycle() != null) {
            cyclePicker.setSelection(spending.getCycle().ordinal() + 1);
        }
        notesInput.setText(spending.getNotes());
        loadSpendingOccurrences(spending);
    }

    private void loadSpendingOccurrences(final Spending spending) {
        BalanceReading balanceReading = userPreferences.getBalanceReading();
        BalanceCalculator.BalanceResult result = BalanceCalculator.getEstimatedBalance(spending,
                balanceReading != null ? balanceReading.when : null,
                userPreferences.getEstimateDate());
        String spentStr = ArrayUtils.join("\n", result.spendingEvents, (index, item) -> getString(R.string.spending_occurrence, index + 1, DateUtils.FORMAT_MONTH_DAY.format(item)));
        spentStr = "Spent: " + result.bestCase + "/" + (result.worstCase) + "\n" + spentStr;
        spendingEventsLayout.setText(spentStr);
    }

    private void applyFirstFromDateToScreen(Date startDate) {
        startDateBtn.setText(DateUtils.FORMAT_MONTH_DAY.format(startDate));
    }

    private void applyEndDateToScreen(Date endDate) {
        endDateBtn.setText(DateUtils.FORMAT_MONTH_DAY.format(endDate));
    }

    private DatePickerDialog getStartDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        if (originalSpending != null && originalSpending.getFromStartDate() != null) {
            c.setTime(this.originalSpending.getFromStartDate());
        }
        if (startDatePickerDialog == null) {
            startDatePickerDialog = new DatePickerDialog(getActivity(), dateSetListener,
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            startDatePickerDialog.getDatePicker().setTag(R.id.date_from);
        }
        return startDatePickerDialog;
    }

    private DatePickerDialog getEndDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        if (originalSpending != null && originalSpending.getFromEndDate() != null) {
            c.setTime(this.originalSpending.getFromEndDate());
        }
        if (endDatePickerDialog == null) {
            endDatePickerDialog = new DatePickerDialog(getActivity(), dateSetListener,
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            endDatePickerDialog.getDatePicker().setTag(R.id.date_to);
        }
        return endDatePickerDialog;
    }

    /**
     * There are two kinds of validation errors: the ones that can be shown in an input
     * field (with {@link TextView#setError(CharSequence)} and the ones tha are shown
     * as a Toast or Dialog instead (DatePicker, etc...).
     */
    private class ValidationError {
        static final int TYPE_INPUT_FIELD = 1;
        static final int TYPE_NON_INPUT_FIELD = 2;

        final int type;
        final View target;
        final String errorMessage;

        ValidationError(int type, View target, String errorMessage) {
            if (type == TYPE_INPUT_FIELD) {
                if (target == null) {
                    throw new IllegalArgumentException("Please specify a target view for the input field error " + "message");
                }
                if (!(target instanceof TextView)) {
                    throw new IllegalArgumentException("Wrong view type in ValidationError. Cannot show error " + "message.");

                }
            } else if (type != TYPE_NON_INPUT_FIELD) {
                throw new IllegalArgumentException("\"type\" must be one of {TYPE_INPUT_FIELD, " + "TYPE_NON_INPUT_FIELD}");
            }
            this.type = type;
            this.target = target;
            this.errorMessage = errorMessage;
        }

        void showError() throws IllegalArgumentException {
            if (type == TYPE_INPUT_FIELD) {
                TextView textView = (TextView) target;
                textView.setError(errorMessage);
                textView.requestFocus();
            } else if (type == TYPE_NON_INPUT_FIELD) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                if (target != null) {
                    target.requestFocus();
                }
            }
        }
    }

    /**
     * Verify whether user input is valid and show appropriate error messages
     *
     * @return true if user input is valid
     */
    private ValidationError validateUserInput() {
        if (TextUtils.isEmpty(nameInput.getText())) {
            return new ValidationError(ValidationError.TYPE_INPUT_FIELD, nameInput, "Please specify a name");
        }
        if (TextUtils.isEmpty(averageInput.getText())) {
            return new ValidationError(ValidationError.TYPE_INPUT_FIELD, averageInput, "Please specify an average");
        }
        if (!(categoryPicker.getSelectedItem() instanceof Spending.Category)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a category");
        }
        if (!(cyclePicker.getSelectedItem() instanceof Spending.Cycle)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a cycle");
        }
        if (TextUtils.isEmpty(cycleMultiplierInput.getText())) {
            return new ValidationError(ValidationError.TYPE_INPUT_FIELD, cycleMultiplierInput, "Please fill in");
        }
        Date firstOccurrenceStart = getStartDateFromScreen();
        Date firstOccurrenceEnd = getEndDateFromScreen();
        if (firstOccurrenceStart.after(firstOccurrenceEnd)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Start date must not be higher then end date");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstOccurrenceStart);
        DateUtils.clearLowerBits(calendar);

        ((Spending.Cycle) cyclePicker.getSelectedItem()).apply(calendar, getCycleMultiplierFromScreen());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        if (firstOccurrenceEnd.after(calendar.getTime())) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, endDateBtn,
                    "End date cannot be higher than " + DateUtils.FORMAT_MONTH_DAY.format(calendar.getTime()));
        }
        return null;
    }

    private Spending getSpendingFromScreen() {
        Spending spending = new Spending();
        // title
        if (!TextUtils.isEmpty(nameInput.getText())) {
            spending.setName(nameInput.getText().toString());
        }
        // average
        if (!TextUtils.isEmpty(averageInput.getText())) {
            spending.setAverage(Double.valueOf(averageInput.getText().toString()));
        }
        // enabled
        spending.setEnabled(enabledCB.isChecked());
        // type
        if (categoryPicker.getSelectedItem() instanceof Spending.Category) {
            spending.setType((Spending.Category) categoryPicker.getSelectedItem());
        }
        // fromStartDate
        spending.setFromStartDate(getStartDateFromScreen());
        // fromEndDate
        spending.setFromEndDate(getEndDateFromScreen());
        // repetition
        if (!TextUtils.isEmpty(occurrenceInput.getText())) {
            spending.setOccurrenceCount(Integer.valueOf(occurrenceInput.getText().toString()));
        }
        // cycleMultiplier
        spending.setCycleMultiplier(getCycleMultiplierFromScreen());
        // cycle
        if (cyclePicker.getSelectedItem() instanceof Spending.Cycle) {
            spending.setCycle((Spending.Cycle) cyclePicker.getSelectedItem());
        }
        // notes
        if (!TextUtils.isEmpty(notesInput.getText())) {
            spending.setNotes(notesInput.getText().toString());
        }
        // target
        if (!TextUtils.isEmpty(targetInput.getText())) {
            spending.setTarget(Double.valueOf(targetInput.getText().toString()));
        }
        if (originalSpending != null) {
            // this will make an already saved item ot be updated instead of a new
            // one being created
            spending.setId(originalSpending.getId());
            spending.getSourceData().putAll(originalSpending.getSourceData());
            spending.setSpent(originalSpending.getSpent());
        }
        return spending;
    }

    private Date getStartDateFromScreen() {
        Date firstOccurrenceDateStart;

        if (startDatePickerDialog != null) {
            // user picked a date
            firstOccurrenceDateStart = DateUtils.getDayFromDatePicker(startDatePickerDialog.getDatePicker());
        } else if (originalSpending != null && originalSpending.getFromStartDate() != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateStart = originalSpending.getFromStartDate();
        } else {
            // user did not pick a date, and we are in CREATE mode
            firstOccurrenceDateStart = DEFAULT_START_DATE;
        }
        return firstOccurrenceDateStart;
    }

    private Date getEndDateFromScreen() {
        Date firstOccurrenceDateEnd;
        if (endDatePickerDialog != null) {
            // user picked a date
            firstOccurrenceDateEnd = DateUtils.getDayFromDatePicker(endDatePickerDialog.getDatePicker());
        } else if (originalSpending != null && originalSpending.getFromEndDate() != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateEnd = originalSpending.getFromEndDate();
        } else {
            // user did not pick a date, and we are in CREATE mode
            firstOccurrenceDateEnd = DEFAULT_END_DATE;
        }
        return firstOccurrenceDateEnd;
    }

    private Integer getCycleMultiplierFromScreen() {
        Integer cycleMultiplier = null;
        if (!TextUtils.isEmpty(cycleMultiplierInput.getText())) {
            // user entered value
            cycleMultiplier = Integer.valueOf(cycleMultiplierInput.getText().toString());
        } else if (originalSpending != null && originalSpending.getCycleMultiplier() != null) {
            // user did not enter a value, but this is EDIT, not CREATE
            cycleMultiplier = originalSpending.getCycleMultiplier();
        }
        return cycleMultiplier;
    }

    /**
     * @return true if the content differs from the originally loaded Spending, or if
     * this fragment contains unsaved user input
     */
    private boolean shouldSave() {
        Spending newSpending = getSpendingFromScreen();
        boolean isNew = originalSpending == null
                && !new Spending().compareForEditing(newSpending, !(startDateChanged || endDateChanged), !cycleMultiplierChanged);
        boolean changed = originalSpending != null && !originalSpending.compareForEditing(newSpending, false, false);
        return isNew || changed;
    }
}
