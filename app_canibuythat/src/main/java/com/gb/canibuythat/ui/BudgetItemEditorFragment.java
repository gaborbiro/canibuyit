package com.gb.canibuythat.ui;

import android.app.DatePickerDialog;
import android.app.Fragment;
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

import com.gb.canibuythat.App;
import com.gb.canibuythat.R;
import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.ui.task.Callback;
import com.gb.canibuythat.ui.task.budget_item.BudgetItemCreateOrUpdateTask;
import com.gb.canibuythat.ui.task.budget_item.BudgetItemDeleteTask;
import com.gb.canibuythat.ui.task.budget_item.BudgetItemReadTask;
import com.gb.canibuythat.util.ArrayUtils;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.ViewUtils;
import com.j256.ormlite.dao.Dao;

import java.util.Calendar;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A fragment representing a single BudgetModifier detail screen. This fragment is either
 * contained in a {@link BudgetItemListActivity} in two-pane mode (on tablets) or a
 * {@link BudgetItemEditorActivity} on handsets.
 */
public class BudgetItemEditorFragment extends Fragment {

    public static final String EXTRA_ITEM_ID = "budget_item_id";

    private static final String EXTRA_ITEM = "budget_item";

    private static final Date DEFAULT_FIRST_OCCURRENCE_END;
    private static final Date DEFAULT_FIRST_OCCURRENCE_START;

    static {
        Calendar c = Calendar.getInstance();
        DateUtils.clearLowerBits(c);
        DEFAULT_FIRST_OCCURRENCE_END = c.getTime();
        DEFAULT_FIRST_OCCURRENCE_START = c.getTime();
    }

    @InjectView(R.id.name) EditText nameView;
    @InjectView(R.id.amount) EditText amountView;
    @InjectView(R.id.enabled) CheckBox enabledView;
    @InjectView(R.id.category) Spinner categoryView;
    @InjectView(R.id.first_occurence_start) Button firstOccurrenceStartView;
    @InjectView(R.id.first_occurence_end) Button firstOccurrenceEndView;
    @InjectView(R.id.occurence_count) EditText occurrenceLimitView;
    @InjectView(R.id.period_multiplier) EditText periodMultiplierView;
    @InjectView(R.id.period_type) Spinner periodTypeView;
    @InjectView(R.id.notes) EditText notesView;
    @InjectView(R.id.spending_events) TextView spendingEventsView;

    private BudgetItem originalBudgetItem;
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
    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

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
                                mDateSetListener, c.get(Calendar.YEAR),
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
                                mDateSetListener, c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH));
                        applyFirstOccurrenceStartToScreen(c.getTime());
                    }
                    firstOccurrenceEndDateChanged = !c.getTime().equals(DEFAULT_FIRST_OCCURRENCE_END);
                    break;
            }
        }
    };
    private View.OnClickListener mDatePickerOnClickListener = new View.OnClickListener() {
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
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_budget_item_editor, container, false);
        ButterKnife.inject(this, rootView);

        categoryView.setAdapter(new PlusOneAdapter(BudgetItem.BudgetItemType.values()));
        categoryView.setOnTouchListener(keyboardDismisser);

        periodTypeView.setAdapter(new PlusOneAdapter(BudgetItem.PeriodType.values()));
        periodTypeView.setOnTouchListener(keyboardDismisser);
        periodTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                firstOccurrenceEndView.setError(null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing to do
            }
        });

        firstOccurrenceStartView.setOnClickListener(mDatePickerOnClickListener);
        firstOccurrenceStartView.setOnTouchListener(keyboardDismisser);
        firstOccurrenceEndView.setOnClickListener(mDatePickerOnClickListener);
        firstOccurrenceEndView.setOnTouchListener(keyboardDismisser);

        if (savedInstanceState != null) {
            originalBudgetItem = savedInstanceState.getParcelable(EXTRA_ITEM);
        }
        if (originalBudgetItem == null && getArguments() != null && getArguments().containsKey(EXTRA_ITEM_ID)) {
            setContent(getArguments().getInt(EXTRA_ITEM_ID), true);
        } else {
            setContent(null, true);
        }
        return rootView;
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
     * @param budgetItemId         can be null, in which case the content is cleared
     * @param showKeyboardWhenDone after data has been loaded from the database and
     *                             displayed, focus on the name
     *                             EditText and show the keyboard
     */
    public void setContent(Integer budgetItemId, final boolean showKeyboardWhenDone) {
        if (budgetItemId != null) {
            new BudgetItemReadTask(budgetItemId, new Callback<BudgetItem>() {
                @Override
                public void onSuccess(BudgetItem budgetItem) {
                    BudgetItemEditorFragment.this.originalBudgetItem = budgetItem;
                    firstOccurrenceEndPickerDialog = null;
                    firstOccurrenceStartPickerDialog = null;
                    applyBudgetItemToScreen(budgetItem);

                    if (showKeyboardWhenDone && isAdded()) {
                        ViewUtils.showKeyboard(nameView);
                    }

                    if (deleteButton != null) {
                        deleteButton.setVisible(true);
                    }
                    Toast.makeText(App.getAppContext(), "BudgetItem loaded", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure() {
                    Toast.makeText(App.getAppContext(), "BudgetItem was not found", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(App.getAppContext(), "Error loading data. Check logs for more information.", Toast.LENGTH_SHORT).show();
                }
            }).execute();
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
                if (originalBudgetItem != null && originalBudgetItem.isPersisted()) {
                    new BudgetItemDeleteTask(new Callback<Boolean>() {

                        @Override
                        public void onSuccess(Boolean result) {
                            deleteButton.setVisible(false);
                            Toast.makeText(App.getAppContext(), "BudgetItem deleted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure() {
                            Toast.makeText(App.getAppContext(), "BudgetItem was not found", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Throwable t) {
                            t.printStackTrace();
                            Toast.makeText(App.getAppContext(), "Error deleting data. Check logs for more information.", Toast.LENGTH_SHORT).show();
                        }
                    }, originalBudgetItem.id).execute();
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
            final BudgetItem newBudgetItem = getBudgetItemFromScreen();

            if (originalBudgetItem != null) {
                // this will make an already saved item ot be updated instead of a new
                // one being created
                newBudgetItem.id = originalBudgetItem.id;
                newBudgetItem.ordering = originalBudgetItem.ordering;
            }
            new BudgetItemCreateOrUpdateTask(newBudgetItem, new Callback<Dao.CreateOrUpdateStatus>() {

                @Override
                public void onSuccess(Dao.CreateOrUpdateStatus result) {
                    BudgetItemEditorFragment.this.originalBudgetItem = newBudgetItem;
                    deleteButton.setVisible(true);
                    loadSpendingOccurrences(newBudgetItem);
                    Toast.makeText(App.getAppContext(), "BudgetItem saved", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Throwable t) {
                    if (t.getCause() != null
                            && t.getCause().getCause() != null
                            && t.getCause().getCause() instanceof SQLiteConstraintException) {
                        t = t.getCause().getCause();
                    }
                    t.printStackTrace();
                    Toast.makeText(App.getAppContext(), "Error saving data. Check logs for more information.", Toast.LENGTH_SHORT).show();
                }
            }).execute();
            return true;
        } else {
            error.showError();
            return false;
        }
    }

    private void applyBudgetItemToScreen(final BudgetItem budgetItem) {
        nameView.setText(budgetItem.name);
        if (budgetItem.amount != null) {
            amountView.setText(getString(R.string.detail_amount, budgetItem.amount));
        } else {
            amountView.setText(null);
        }
        enabledView.setChecked(budgetItem.enabled);
        if (budgetItem.type != null) {
            categoryView.setSelection(budgetItem.type.ordinal() + 1);
        }

        Date firstOccurrenceStart = budgetItem.firstOccurrenceStart != null
                ? budgetItem.firstOccurrenceStart
                : DEFAULT_FIRST_OCCURRENCE_START;
        applyFirstOccurrenceStartToScreen(firstOccurrenceStart);

        Date firstOccurrenceEnd = budgetItem.firstOccurrenceEnd != null ? budgetItem.firstOccurrenceEnd : DEFAULT_FIRST_OCCURRENCE_END;
        applyFirstOccurrenceEndToScreen(firstOccurrenceEnd);

        if (budgetItem.occurrenceCount != null) {
            occurrenceLimitView.setText(Integer.toString(budgetItem.occurrenceCount));
        } else {
            occurrenceLimitView.setText(null);
        }

        if (budgetItem.periodMultiplier != null) {
            periodMultiplierView.setText(Integer.toString(budgetItem.periodMultiplier));
        } else {
            periodMultiplierView.setText(null);
        }

        if (budgetItem.periodType != null) {
            periodTypeView.setSelection(budgetItem.periodType.ordinal() + 1);
        }
        notesView.setText(budgetItem.notes);
        loadSpendingOccurrences(budgetItem);
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
        periodTypeView.setSelection(0);
        notesView.setText(null);
        spendingEventsView.setText(null);

        originalBudgetItem = null;
        firstOccurrenceEndPickerDialog = null;
        firstOccurrenceStartPickerDialog = null;
    }

    private void loadSpendingOccurrences(final BudgetItem budgetItem) {
        BalanceReading balanceReading = UserPreferences.getBalanceReading();
        BalanceCalculator.BalanceResult result = BalanceCalculator.get().getEstimatedBalance(budgetItem,
                balanceReading != null ? balanceReading.when : null,
                UserPreferences.getEstimateDate());
        String spending = ArrayUtils.join("\n", result.spendingEvents, (index, item) -> getString(R.string.spending_occurrence, index + 1, DateUtils.FORMAT_MONTH_DAY.format(item)));
        spending = "Spent: " + result.bestCase + "/" + (result.worstCase) + "\n" + spending;
        spendingEventsView.setText(spending);
    }

    private void applyFirstOccurrenceStartToScreen(Date firstOccurrenceStart) {
        firstOccurrenceStartView.setText(DateUtils.FORMAT_MONTH_DAY.format(firstOccurrenceStart));
    }

    private void applyFirstOccurrenceEndToScreen(Date firstOccurrenceEnd) {
        firstOccurrenceEndView.setText(DateUtils.FORMAT_MONTH_DAY.format(firstOccurrenceEnd));
    }

    private DatePickerDialog getFirstOccurrenceStartPickerDialog() {
        Calendar c = Calendar.getInstance();
        if (originalBudgetItem != null && originalBudgetItem.firstOccurrenceStart != null) {
            c.setTime(this.originalBudgetItem.firstOccurrenceStart);
        }
        if (firstOccurrenceStartPickerDialog == null) {
            firstOccurrenceStartPickerDialog = new DatePickerDialog(getActivity(), mDateSetListener,
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            firstOccurrenceStartPickerDialog.getDatePicker().setTag(R.id.first_occurence_start);
        }
        return firstOccurrenceStartPickerDialog;
    }

    private DatePickerDialog getFirstOccurrenceEndPickerDialog() {
        Calendar c = Calendar.getInstance();
        if (originalBudgetItem != null && originalBudgetItem.firstOccurrenceEnd != null) {
            c.setTime(this.originalBudgetItem.firstOccurrenceEnd);
        }
        if (firstOccurrenceEndPickerDialog == null) {
            firstOccurrenceEndPickerDialog = new DatePickerDialog(getActivity(), mDateSetListener,
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
    private static class ValidationError {
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
                Toast.makeText(App.getAppContext(), errorMessage, Toast.LENGTH_SHORT).show();
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
        if (!(categoryView.getSelectedItem() instanceof BudgetItem.BudgetItemType)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Please select a category");
        }
        if (!(periodTypeView.getSelectedItem() instanceof BudgetItem.PeriodType)) {
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

        ((BudgetItem.PeriodType) periodTypeView.getSelectedItem()).apply(c, getPeriodMultiplierFromScreen());
        c.add(Calendar.DAY_OF_MONTH, -1);

        if (firstOccurrenceEnd.after(c.getTime())) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, firstOccurrenceEndView,
                    "End date cannot be higher than " + DateUtils.FORMAT_MONTH_DAY.format(c.getTime()));
        }
        return null;
    }

    private BudgetItem getBudgetItemFromScreen() {
        BudgetItem budgetItem = new BudgetItem();
        // title
        if (!TextUtils.isEmpty(nameView.getText())) {
            budgetItem.name = nameView.getText().toString();
        }
        // amount
        if (!TextUtils.isEmpty(amountView.getText())) {
            budgetItem.amount = Float.valueOf(amountView.getText().toString());
        }
        // enabled
        budgetItem.enabled = enabledView.isChecked();
        // type
        if (categoryView.getSelectedItem() instanceof BudgetItem.BudgetItemType) {
            budgetItem.type = (BudgetItem.BudgetItemType) categoryView.getSelectedItem();
        }
        // firstOccurrenceStart
        budgetItem.firstOccurrenceStart = getFirstOccurrenceStartFromScreen();
        // firstOccurrenceEnd
        budgetItem.firstOccurrenceEnd = getFirstOccurrenceEndFromScreen();
        // repetition
        if (!TextUtils.isEmpty(occurrenceLimitView.getText())) {
            budgetItem.occurrenceCount = Integer.valueOf(occurrenceLimitView.getText().toString());
        }
        // periodMultiplier
        budgetItem.periodMultiplier = getPeriodMultiplierFromScreen();
        // period
        if (periodTypeView.getSelectedItem() instanceof BudgetItem.PeriodType) {
            budgetItem.periodType = (BudgetItem.PeriodType) periodTypeView.getSelectedItem();
        }
        // notes
        if (!TextUtils.isEmpty(notesView.getText())) {
            budgetItem.notes = notesView.getText().toString();
        }
        return budgetItem;
    }

    private Date getFirstOccurrenceStartFromScreen() {
        Date firstOccurrenceDateStart;

        if (firstOccurrenceStartPickerDialog != null) {
            // user picked a date
            firstOccurrenceDateStart = DateUtils.getDayFromDatePicker(firstOccurrenceStartPickerDialog.getDatePicker());
        } else if (originalBudgetItem != null && originalBudgetItem.firstOccurrenceStart != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateStart = originalBudgetItem.firstOccurrenceStart;
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
        } else if (originalBudgetItem != null && originalBudgetItem.firstOccurrenceEnd != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateEnd = originalBudgetItem.firstOccurrenceEnd;
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
        } else if (originalBudgetItem != null && originalBudgetItem.periodMultiplier != null) {
            // user did not enter a value, but this is EDIT, not CREATE
            periodMultiplier = originalBudgetItem.periodMultiplier;
        }
        return periodMultiplier;
    }

    /**
     * @return true if the content differs from the originally loaded BudgetItem, or if
     * this fragment contains unsaved user input
     */
    private boolean shouldSave() {
        BudgetItem userInput = getBudgetItemFromScreen();
        boolean isNew = originalBudgetItem == null
                && !new BudgetItem().compareForEditing(userInput, !(firstOccurrenceStartDateChanged || firstOccurrenceEndDateChanged));
        boolean changed = originalBudgetItem != null && !originalBudgetItem.compareForEditing(userInput, false);
        return isNew || changed;
    }
}
