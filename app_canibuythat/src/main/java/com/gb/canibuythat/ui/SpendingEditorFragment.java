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

    public static final String EXTRA_ITEM_ID = "spending_id";

    private static final Date DEFAULT_FIRST_OCCURRENCE_END;
    private static final Date DEFAULT_FIRST_OCCURRENCE_START;

    static {
        Calendar c = Calendar.getInstance();
        DateUtils.clearLowerBits(c);
        DEFAULT_FIRST_OCCURRENCE_END = c.getTime();
        DEFAULT_FIRST_OCCURRENCE_START = c.getTime();
    }

    @Inject UserPreferences userPreferences;
    @Inject SpendingInteractor spendingInteractor;

    @BindView(R.id.name) EditText nameView;
    @BindView(R.id.amount) EditText amountView;
    @BindView(R.id.target) EditText targetView;
    @BindView(R.id.enabled) CheckBox enabledView;
    @BindView(R.id.category) Spinner categoryView;
    @BindView(R.id.first_occurence_start) Button firstOccurrenceStartView;
    @BindView(R.id.first_occurence_end) Button firstOccurrenceEndView;
    @BindView(R.id.occurence_count) EditText occurrenceLimitView;
    @BindView(R.id.period_multiplier) EditText periodMultiplierView;
    @BindView(R.id.period_type) Spinner periodView;
    @BindView(R.id.notes) EditText notesView;
    @BindView(R.id.spending_events) TextView spendingEventsView;

    private Spending originalSpending;
    private boolean firstOccurrenceStartDateChanged;
    private boolean firstOccurrenceEndDateChanged;

    private DatePickerDialog firstOccurrenceStartPickerDialog;
    private DatePickerDialog firstOccurrenceEndPickerDialog;
    private MenuItem deleteButton;
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
                case R.id.first_occurence_start:
                    firstOccurrenceStartView.setText(DateUtils.FORMAT_MONTH_DAY.format(c.getTime()));
                    Date firstOccurrenceEnd = getFirstOccurrenceEndFromScreen();

                    if (firstOccurrenceEnd.getTime() < c.getTime().getTime()) {
                        firstOccurrenceEndPickerDialog = new DatePickerDialog(getActivity(),
                                dateSetListener, c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH));
                        applyFirstOccurrenceEndToScreen(c.getTime());
                    }
                    firstOccurrenceStartDateChanged = !c.getTime().equals(DEFAULT_FIRST_OCCURRENCE_START);
                    break;
                case R.id.first_occurence_end:
                    firstOccurrenceEndView.setText(DateUtils.FORMAT_MONTH_DAY.format(c.getTime()));
                    Date firstOccurrenceStart = getFirstOccurrenceStartFromScreen();

                    if (firstOccurrenceStart.getTime() > c.getTime().getTime()) {
                        firstOccurrenceStartPickerDialog = new DatePickerDialog(getActivity(),
                                dateSetListener, c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH));
                        applyFirstOccurrenceStartToScreen(c.getTime());
                    }
                    firstOccurrenceEndDateChanged = !c.getTime().equals(DEFAULT_FIRST_OCCURRENCE_END);
                    break;
            }
        }
    };
    private View.OnClickListener datePickerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.first_occurence_start:
                    getFirstOccurrenceStartPickerDialog().show();
                    firstOccurrenceStartView.setError(null);
                    break;
                case R.id.first_occurence_end:
                    getFirstOccurrenceEndPickerDialog().show();
                    firstOccurrenceEndView.setError(null);
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

        categoryView.setAdapter(new PlusOneAdapter(Spending.Category.values()));
        categoryView.setOnTouchListener(keyboardDismisser);

        periodView.setAdapter(new PlusOneAdapter(Spending.Period.values()));
        periodView.setOnTouchListener(keyboardDismisser);
        periodView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                firstOccurrenceEndView.setError(null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing to do
            }
        });

        firstOccurrenceStartView.setOnClickListener(datePickerOnClickListener);
        firstOccurrenceStartView.setOnTouchListener(keyboardDismisser);
        firstOccurrenceEndView.setOnClickListener(datePickerOnClickListener);
        firstOccurrenceEndView.setOnTouchListener(keyboardDismisser);

        if (originalSpending == null && getArguments() != null && getArguments().containsKey(EXTRA_ITEM_ID)) {
            showSpending(getArguments().getInt(EXTRA_ITEM_ID), true);
        } else {
            showSpending(null, true);
        }
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
            clearScreen();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit, menu);
        this.deleteButton = menu.findItem(R.id.menu_delete);

        if (getArguments() == null || !getArguments().containsKey(EXTRA_ITEM_ID)) {
            deleteButton.setVisible(false);
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
                            .subscribe(() -> deleteButton.setVisible(false), this::onError);
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
                        deleteButton.setVisible(true);
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
        firstOccurrenceEndPickerDialog = null;
        firstOccurrenceStartPickerDialog = null;
        applySpendingToScreen(spending);

        if (showKeyboardWhenDone && isAdded()) {
            ViewUtils.showKeyboard(nameView);
        }

        if (deleteButton != null) {
            deleteButton.setVisible(true);
        }
    }

    private void applySpendingToScreen(final Spending spending) {
        nameView.setText(spending.getName());
        if (spending.getAmount() != null) {
            amountView.setText(getString(R.string.detail_amount, spending.getAmount()));
        } else {
            amountView.setText(null);
        }
        if (spending.getTarget() != null) {
            targetView.setText(getString(R.string.detail_amount, spending.getTarget()));
        } else {
            targetView.setText(null);
        }
        enabledView.setChecked(spending.getEnabled());
        if (spending.getType() != null) {
            categoryView.setSelection(spending.getType().ordinal() + 1);
        }

        Date firstOccurrenceStart = spending.getFirstOccurrenceStart() != null
                ? spending.getFirstOccurrenceStart()
                : DEFAULT_FIRST_OCCURRENCE_START;
        applyFirstOccurrenceStartToScreen(firstOccurrenceStart);

        Date firstOccurrenceEnd = spending.getFirstOccurrenceEnd() != null ? spending.getFirstOccurrenceEnd() : DEFAULT_FIRST_OCCURRENCE_END;
        applyFirstOccurrenceEndToScreen(firstOccurrenceEnd);

        if (spending.getOccurrenceCount() != null) {
            occurrenceLimitView.setText(Integer.toString(spending.getOccurrenceCount()));
        } else {
            occurrenceLimitView.setText(null);
        }

        if (spending.getPeriodMultiplier() != null) {
            periodMultiplierView.setText(Integer.toString(spending.getPeriodMultiplier()));
        } else {
            periodMultiplierView.setText(null);
        }

        if (spending.getPeriod() != null) {
            periodView.setSelection(spending.getPeriod().ordinal() + 1);
        }
        notesView.setText(spending.getNotes());
        loadSpendingOccurrences(spending);
    }

    private void clearScreen() {
        nameView.setText(null);
        amountView.setText(null);
        enabledView.setChecked(true);
        categoryView.setSelection(0);
        applyFirstOccurrenceStartToScreen(DEFAULT_FIRST_OCCURRENCE_START);
        applyFirstOccurrenceEndToScreen(DEFAULT_FIRST_OCCURRENCE_END);
        occurrenceLimitView.setText(null);
        periodMultiplierView.setText(null);
        periodView.setSelection(0);
        notesView.setText(null);
        spendingEventsView.setText(null);

        originalSpending = null;
        firstOccurrenceEndPickerDialog = null;
        firstOccurrenceStartPickerDialog = null;
    }

    private void loadSpendingOccurrences(final Spending spending) {
        BalanceReading balanceReading = userPreferences.getBalanceReading();
        BalanceCalculator.BalanceResult result = BalanceCalculator.get().getEstimatedBalance(spending,
                balanceReading != null ? balanceReading.when : null,
                userPreferences.getEstimateDate());
        String spentStr = ArrayUtils.join("\n", result.spendingEvents, (index, item) -> getString(R.string.spending_occurrence, index + 1, DateUtils.FORMAT_MONTH_DAY.format(item)));
        spentStr = "Spent: " + result.bestCase + "/" + (result.worstCase) + "\n" + spentStr;
        spendingEventsView.setText(spentStr);
    }

    private void applyFirstOccurrenceStartToScreen(Date firstOccurrenceStart) {
        firstOccurrenceStartView.setText(DateUtils.FORMAT_MONTH_DAY.format(firstOccurrenceStart));
    }

    private void applyFirstOccurrenceEndToScreen(Date firstOccurrenceEnd) {
        firstOccurrenceEndView.setText(DateUtils.FORMAT_MONTH_DAY.format(firstOccurrenceEnd));
    }

    private DatePickerDialog getFirstOccurrenceStartPickerDialog() {
        Calendar c = Calendar.getInstance();
        if (originalSpending != null && originalSpending.getFirstOccurrenceStart() != null) {
            c.setTime(this.originalSpending.getFirstOccurrenceStart());
        }
        if (firstOccurrenceStartPickerDialog == null) {
            firstOccurrenceStartPickerDialog = new DatePickerDialog(getActivity(), dateSetListener,
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            firstOccurrenceStartPickerDialog.getDatePicker().setTag(R.id.first_occurence_start);
        }
        return firstOccurrenceStartPickerDialog;
    }

    private DatePickerDialog getFirstOccurrenceEndPickerDialog() {
        Calendar c = Calendar.getInstance();
        if (originalSpending != null && originalSpending.getFirstOccurrenceEnd() != null) {
            c.setTime(this.originalSpending.getFirstOccurrenceEnd());
        }
        if (firstOccurrenceEndPickerDialog == null) {
            firstOccurrenceEndPickerDialog = new DatePickerDialog(getActivity(), dateSetListener,
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            firstOccurrenceEndPickerDialog.getDatePicker().setTag(R.id.first_occurence_end);
        }
        return firstOccurrenceEndPickerDialog;
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
        if (TextUtils.isEmpty(nameView.getText())) {
            return new ValidationError(ValidationError.TYPE_INPUT_FIELD, nameView, "Please specify a name");
        }
        if (TextUtils.isEmpty(amountView.getText())) {
            return new ValidationError(ValidationError.TYPE_INPUT_FIELD, amountView, "Please specify an amount");
        }
        if (!(categoryView.getSelectedItem() instanceof Spending.Category)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a category");
        }
        if (!(periodView.getSelectedItem() instanceof Spending.Period)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a period");
        }
        if (TextUtils.isEmpty(periodMultiplierView.getText())) {
            return new ValidationError(ValidationError.TYPE_INPUT_FIELD, periodMultiplierView, "Please fill in");
        }
        Date firstOccurrenceStart = getFirstOccurrenceStartFromScreen();
        Date firstOccurrenceEnd = getFirstOccurrenceEndFromScreen();
        if (firstOccurrenceStart.after(firstOccurrenceEnd)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Start date must not be higher then end date");
        }

        Calendar c = Calendar.getInstance();
        c.setTime(firstOccurrenceStart);
        DateUtils.clearLowerBits(c);

        ((Spending.Period) periodView.getSelectedItem()).apply(c, getPeriodMultiplierFromScreen());
        c.add(Calendar.DAY_OF_MONTH, -1);

        if (firstOccurrenceEnd.after(c.getTime())) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, firstOccurrenceEndView,
                    "End date cannot be higher than " + DateUtils.FORMAT_MONTH_DAY.format(c.getTime()));
        }
        return null;
    }

    private Spending getSpendingFromScreen() {
        Spending spending = new Spending();
        // title
        if (!TextUtils.isEmpty(nameView.getText())) {
            spending.setName(nameView.getText().toString());
        }
        // amount
        if (!TextUtils.isEmpty(amountView.getText())) {
            spending.setAmount(Double.valueOf(amountView.getText().toString()));
        }
        // enabled
        spending.setEnabled(enabledView.isChecked());
        // type
        if (categoryView.getSelectedItem() instanceof Spending.Category) {
            spending.setType((Spending.Category) categoryView.getSelectedItem());
        }
        // firstOccurrenceStart
        spending.setFirstOccurrenceStart(getFirstOccurrenceStartFromScreen());
        // firstOccurrenceEnd
        spending.setFirstOccurrenceEnd(getFirstOccurrenceEndFromScreen());
        // repetition
        if (!TextUtils.isEmpty(occurrenceLimitView.getText())) {
            spending.setOccurrenceCount(Integer.valueOf(occurrenceLimitView.getText().toString()));
        }
        // periodMultiplier
        spending.setPeriodMultiplier(getPeriodMultiplierFromScreen());
        // period
        if (periodView.getSelectedItem() instanceof Spending.Period) {
            spending.setPeriod((Spending.Period) periodView.getSelectedItem());
        }
        // notes
        if (!TextUtils.isEmpty(notesView.getText())) {
            spending.setNotes(notesView.getText().toString());
        }
        // target
        if (!TextUtils.isEmpty(targetView.getText())) {
            spending.setTarget(Double.valueOf(targetView.getText().toString()));
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

    private Date getFirstOccurrenceStartFromScreen() {
        Date firstOccurrenceDateStart;

        if (firstOccurrenceStartPickerDialog != null) {
            // user picked a date
            firstOccurrenceDateStart = DateUtils.getDayFromDatePicker(firstOccurrenceStartPickerDialog.getDatePicker());
        } else if (originalSpending != null && originalSpending.getFirstOccurrenceStart() != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateStart = originalSpending.getFirstOccurrenceStart();
        } else {
            // user did not pick a date, and we are in CREATE mode
            firstOccurrenceDateStart = DEFAULT_FIRST_OCCURRENCE_START;
        }
        return firstOccurrenceDateStart;
    }

    private Date getFirstOccurrenceEndFromScreen() {
        Date firstOccurrenceDateEnd;
        if (firstOccurrenceEndPickerDialog != null) {
            // user picked a date
            firstOccurrenceDateEnd = DateUtils.getDayFromDatePicker(firstOccurrenceEndPickerDialog.getDatePicker());
        } else if (originalSpending != null && originalSpending.getFirstOccurrenceEnd() != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateEnd = originalSpending.getFirstOccurrenceEnd();
        } else {
            // user did not pick a date, and we are in CREATE mode
            firstOccurrenceDateEnd = DEFAULT_FIRST_OCCURRENCE_END;
        }
        return firstOccurrenceDateEnd;
    }

    private Integer getPeriodMultiplierFromScreen() {
        Integer periodMultiplier = null;
        if (!TextUtils.isEmpty(periodMultiplierView.getText())) {
            // user entered value
            periodMultiplier = Integer.valueOf(periodMultiplierView.getText().toString());
        } else if (originalSpending != null && originalSpending.getPeriodMultiplier() != null) {
            // user did not enter a value, but this is EDIT, not CREATE
            periodMultiplier = originalSpending.getPeriodMultiplier();
        }
        return periodMultiplier;
    }

    /**
     * @return true if the content differs from the originally loaded Spending, or if
     * this fragment contains unsaved user input
     */
    private boolean shouldSave() {
        Spending newSpending = getSpendingFromScreen();
        boolean isNew = originalSpending == null
                && !new Spending().compareForEditing(newSpending, !(firstOccurrenceStartDateChanged || firstOccurrenceEndDateChanged));
        boolean changed = originalSpending != null && !originalSpending.compareForEditing(newSpending, false);
        return isNew || changed;
    }
}
