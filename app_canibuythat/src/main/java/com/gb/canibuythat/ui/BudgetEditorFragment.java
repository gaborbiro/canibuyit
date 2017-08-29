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
import com.gb.canibuythat.interactor.BudgetInteractor;
import com.gb.canibuythat.model.BudgetItem;
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
 * A fragment representing a single BudgetModifier detail screen. This fragment is either
 * contained in a {@link MainActivity} in two-pane mode (on tablets) or a
 * {@link BudgetEditorActivity} on handsets.
 */
public class BudgetEditorFragment extends BaseFragment {

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

    @Inject UserPreferences userPreferences;
    @Inject BudgetInteractor budgetInteractor;

    @BindView(R.id.name) EditText nameView;
    @BindView(R.id.amount) EditText amountView;
    @BindView(R.id.target) EditText targetView;
    @BindView(R.id.enabled) CheckBox enabledView;
    @BindView(R.id.category) Spinner categoryView;
    @BindView(R.id.first_occurence_start) Button firstOccurrenceStartView;
    @BindView(R.id.first_occurence_end) Button firstOccurrenceEndView;
    @BindView(R.id.occurence_count) EditText occurrenceLimitView;
    @BindView(R.id.period_multiplier) EditText periodMultiplierView;
    @BindView(R.id.period_type) Spinner periodTypeView;
    @BindView(R.id.notes) EditText notesView;
    @BindView(R.id.spending_events) TextView spendingEventsView;

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
        return inflater.inflate(R.layout.fragment_budget_editor, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = (ViewGroup) view;

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

        firstOccurrenceStartView.setOnClickListener(datePickerOnClickListener);
        firstOccurrenceStartView.setOnTouchListener(keyboardDismisser);
        firstOccurrenceEndView.setOnClickListener(datePickerOnClickListener);
        firstOccurrenceEndView.setOnTouchListener(keyboardDismisser);

        if (originalBudgetItem == null && getArguments() != null && getArguments().containsKey(EXTRA_ITEM_ID)) {
            showBudgetItem(getArguments().getInt(EXTRA_ITEM_ID), true);
        } else {
            showBudgetItem(null, true);
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
     * @param budgetItemId         can be null, in which case the content is cleared
     * @param showKeyboardWhenDone after data has been loaded from the database and
     *                             displayed, focus on the name
     *                             EditText and show the keyboard
     */
    public void showBudgetItem(Integer budgetItemId, final boolean showKeyboardWhenDone) {
        if (budgetItemId != null) {
            budgetInteractor.read(budgetItemId)
                    .subscribe(budgetItem -> onBudgetItemLoaded(budgetItem, showKeyboardWhenDone), this::onError);
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
                    budgetInteractor.delete(originalBudgetItem.getId())
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
            final BudgetItem newBudgetItem = getBudgetItemFromScreen();
            budgetInteractor.createOrUpdate(newBudgetItem)
                    .subscribe(createOrUpdateStatus -> {
                        BudgetEditorFragment.this.originalBudgetItem = newBudgetItem;
                        deleteButton.setVisible(true);
                        loadSpendingOccurrences(newBudgetItem);
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

    private void onBudgetItemLoaded(BudgetItem budgetItem, boolean showKeyboardWhenDone) {
        this.originalBudgetItem = budgetItem;
        firstOccurrenceEndPickerDialog = null;
        firstOccurrenceStartPickerDialog = null;
        applyBudgetItemToScreen(budgetItem);

        if (showKeyboardWhenDone && isAdded()) {
            ViewUtils.showKeyboard(nameView);
        }

        if (deleteButton != null) {
            deleteButton.setVisible(true);
        }
    }

    private void applyBudgetItemToScreen(final BudgetItem budgetItem) {
        nameView.setText(budgetItem.getName());
        if (budgetItem.getAmount() != null) {
            amountView.setText(getString(R.string.detail_amount, budgetItem.getAmount()));
        } else {
            amountView.setText(null);
        }
        if (budgetItem.getTarget() != null) {
            targetView.setText(getString(R.string.detail_amount, budgetItem.getTarget()));
        } else {
            targetView.setText(null);
        }
        enabledView.setChecked(budgetItem.getEnabled());
        if (budgetItem.getType() != null) {
            categoryView.setSelection(budgetItem.getType().ordinal() + 1);
        }

        Date firstOccurrenceStart = budgetItem.getFirstOccurrenceStart() != null
                ? budgetItem.getFirstOccurrenceStart()
                : DEFAULT_FIRST_OCCURRENCE_START;
        applyFirstOccurrenceStartToScreen(firstOccurrenceStart);

        Date firstOccurrenceEnd = budgetItem.getFirstOccurrenceEnd() != null ? budgetItem.getFirstOccurrenceEnd() : DEFAULT_FIRST_OCCURRENCE_END;
        applyFirstOccurrenceEndToScreen(firstOccurrenceEnd);

        if (budgetItem.getOccurrenceCount() != null) {
            occurrenceLimitView.setText(Integer.toString(budgetItem.getOccurrenceCount()));
        } else {
            occurrenceLimitView.setText(null);
        }

        if (budgetItem.getPeriodMultiplier() != null) {
            periodMultiplierView.setText(Integer.toString(budgetItem.getPeriodMultiplier()));
        } else {
            periodMultiplierView.setText(null);
        }

        if (budgetItem.getPeriodType() != null) {
            periodTypeView.setSelection(budgetItem.getPeriodType().ordinal() + 1);
        }
        notesView.setText(budgetItem.getNotes());
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
        BalanceReading balanceReading = userPreferences.getBalanceReading();
        BalanceCalculator.BalanceResult result = BalanceCalculator.get().getEstimatedBalance(budgetItem,
                balanceReading != null ? balanceReading.when : null,
                userPreferences.getEstimateDate());
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
        if (originalBudgetItem != null && originalBudgetItem.getFirstOccurrenceStart() != null) {
            c.setTime(this.originalBudgetItem.getFirstOccurrenceStart());
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
        if (originalBudgetItem != null && originalBudgetItem.getFirstOccurrenceEnd() != null) {
            c.setTime(this.originalBudgetItem.getFirstOccurrenceEnd());
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
            budgetItem.setName(nameView.getText().toString());
        }
        // amount
        if (!TextUtils.isEmpty(amountView.getText())) {
            budgetItem.setAmount(Double.valueOf(amountView.getText().toString()));
        }
        // enabled
        budgetItem.setEnabled(enabledView.isChecked());
        // type
        if (categoryView.getSelectedItem() instanceof BudgetItem.BudgetItemType) {
            budgetItem.setType((BudgetItem.BudgetItemType) categoryView.getSelectedItem());
        }
        // firstOccurrenceStart
        budgetItem.setFirstOccurrenceStart(getFirstOccurrenceStartFromScreen());
        // firstOccurrenceEnd
        budgetItem.setFirstOccurrenceEnd(getFirstOccurrenceEndFromScreen());
        // repetition
        if (!TextUtils.isEmpty(occurrenceLimitView.getText())) {
            budgetItem.setOccurrenceCount(Integer.valueOf(occurrenceLimitView.getText().toString()));
        }
        // periodMultiplier
        budgetItem.setPeriodMultiplier(getPeriodMultiplierFromScreen());
        // period
        if (periodTypeView.getSelectedItem() instanceof BudgetItem.PeriodType) {
            budgetItem.setPeriodType((BudgetItem.PeriodType) periodTypeView.getSelectedItem());
        }
        // notes
        if (!TextUtils.isEmpty(notesView.getText())) {
            budgetItem.setNotes(notesView.getText().toString());
        }
        // target
        if (!TextUtils.isEmpty(targetView.getText())) {
            budgetItem.setTarget(Double.valueOf(targetView.getText().toString()));
        }
        if (originalBudgetItem != null) {
            // this will make an already saved item ot be updated instead of a new
            // one being created
            budgetItem.setId(originalBudgetItem.getId());
            budgetItem.getSourceData().putAll(originalBudgetItem.getSourceData());
            budgetItem.setSpent(originalBudgetItem.getSpent());
        }
        return budgetItem;
    }

    private Date getFirstOccurrenceStartFromScreen() {
        Date firstOccurrenceDateStart;

        if (firstOccurrenceStartPickerDialog != null) {
            // user picked a date
            firstOccurrenceDateStart = DateUtils.getDayFromDatePicker(firstOccurrenceStartPickerDialog.getDatePicker());
        } else if (originalBudgetItem != null && originalBudgetItem.getFirstOccurrenceStart() != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateStart = originalBudgetItem.getFirstOccurrenceStart();
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
        } else if (originalBudgetItem != null && originalBudgetItem.getFirstOccurrenceEnd() != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateEnd = originalBudgetItem.getFirstOccurrenceEnd();
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
        } else if (originalBudgetItem != null && originalBudgetItem.getPeriodMultiplier() != null) {
            // user did not enter a value, but this is EDIT, not CREATE
            periodMultiplier = originalBudgetItem.getPeriodMultiplier();
        }
        return periodMultiplier;
    }

    /**
     * @return true if the content differs from the originally loaded BudgetItem, or if
     * this fragment contains unsaved user input
     */
    private boolean shouldSave() {
        BudgetItem newBudgetItem = getBudgetItemFromScreen();
        boolean isNew = originalBudgetItem == null
                && !new BudgetItem().compareForEditing(newBudgetItem, !(firstOccurrenceStartDateChanged || firstOccurrenceEndDateChanged));
        boolean changed = originalBudgetItem != null && !originalBudgetItem.compareForEditing(newBudgetItem, false);
        return isNew || changed;
    }
}
