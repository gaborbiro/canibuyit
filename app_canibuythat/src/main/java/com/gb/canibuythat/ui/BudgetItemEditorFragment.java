package com.gb.canibuythat.ui;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Context;
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
import com.gb.canibuythat.model.BalanceUpdateEvent;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.ui.task.balance_update.LastBalanceUpdateLoaderTask;
import com.gb.canibuythat.ui.task.budget_item.BudgetItemCreateOrUpdateTaskBase;
import com.gb.canibuythat.ui.task.budget_item.BudgetItemDeleteTaskBase;
import com.gb.canibuythat.ui.task.budget_item.BudgetItemReadTaskBase;
import com.gb.canibuythat.ui.task.Callback;
import com.gb.canibuythat.util.ArrayUtils;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.ViewUtils;
import com.j256.ormlite.dao.Dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private static final int DEFAULT_PERIOD_MULTIPLIER = 1;

    static {
        Calendar c = Calendar.getInstance();
        DateUtils.clearLowerBits(c);
        DEFAULT_FIRST_OCCURRENCE_END = c.getTime();
        DEFAULT_FIRST_OCCURRENCE_START = c.getTime();
    }

    @InjectView(R.id.name) EditText mNameView;
    @InjectView(R.id.amount) EditText mAmountView;
    @InjectView(R.id.enabled) CheckBox mEnabledView;
    @InjectView(R.id.category) Spinner mCategoryView;
    @InjectView(R.id.first_occurence_start) Button mFirstOccurrenceStartView;
    @InjectView(R.id.first_occurence_end) Button mFirstOccurrenceEndView;
    @InjectView(R.id.occurence_count) EditText mOccurrenceLimitView;
    @InjectView(R.id.period_multiplier) EditText mPeriodMultiplierView;
    @InjectView(R.id.period_type) Spinner mPeriodTypeView;
    @InjectView(R.id.notes) EditText mNotesView;
    @InjectView(R.id.spending_events) TextView mSpendingEventsView;
    private BudgetItem mOriginalBudgetItem;
    private DatePickerDialog mFirstOccurrenceStartPickerDialog;
    private DatePickerDialog mFirstOccurrenceEndPickerDialog;
    private MenuItem mDeleteButton;
    private ViewGroup mRootView;
    private View.OnTouchListener nameFocuser = new View.OnTouchListener() {

        @Override public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN &&
                    mRootView.getFocusedChild() != null) {
                mRootView.getFocusedChild()
                        .clearFocus();
                // mNameView will automatically get the focus
            }
            return false;
        }
    };
    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear,
                        int dayOfMonth) {
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.YEAR, year);
                    c.set(Calendar.MONTH, monthOfYear);
                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    DateUtils.clearLowerBits(c);

                    switch ((int) view.getTag()) {
                        case R.id.first_occurence_start:
                            mFirstOccurrenceStartView.setText(
                                    DateUtils.DEFAULT_DATE_FORMAT.format(c.getTime()));
                            Date firstOccurrenceEnd = getFirstOccurrenceEndFromScreen();

                            if (firstOccurrenceEnd.getTime() < c.getTime()
                                    .getTime()) {
                                mFirstOccurrenceEndPickerDialog =
                                        new DatePickerDialog(getActivity(),
                                                mDateSetListener, c.get(Calendar.YEAR),
                                                c.get(Calendar.MONTH),
                                                c.get(Calendar.DAY_OF_MONTH));
                                applyFirstOccurrenceEndToScreen(c.getTime());
                            }
                            break;
                        case R.id.first_occurence_end:
                            mFirstOccurrenceEndView.setText(
                                    DateUtils.DEFAULT_DATE_FORMAT.format(c.getTime()));
                            Date firstOccurrenceStart =
                                    getFirstOccurrenceStartFromScreen();

                            if (firstOccurrenceStart.getTime() > c.getTime()
                                    .getTime()) {
                                mFirstOccurrenceStartPickerDialog =
                                        new DatePickerDialog(getActivity(),
                                                mDateSetListener, c.get(Calendar.YEAR),
                                                c.get(Calendar.MONTH),
                                                c.get(Calendar.DAY_OF_MONTH));
                                applyFirstOccurrenceStartToScreen(c.getTime());
                            }
                            break;
                    }
                }
            };
    private View.OnClickListener mDatePickerOnClickListener = new View.OnClickListener() {

        @Override public void onClick(View v) {
            switch (v.getId()) {
                case R.id.first_occurence_start:
                    getFirstOccurrenceStartPickerDialog().show();
                    mFirstOccurrenceStartView.setError(null);
                    break;
                case R.id.first_occurence_end:
                    getFirstOccurrenceEndPickerDialog().show();
                    mFirstOccurrenceEndView.setError(null);
                    break;
            }
        }
    };

    public BudgetItemEditorFragment() {
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_budget_item_detail,
                container, false);
        ButterKnife.inject(this, mRootView);

        mCategoryView.setAdapter(
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                        BudgetItem.BudgetItemType.values()));
        mCategoryView.setOnTouchListener(nameFocuser);

        mPeriodTypeView.setAdapter(
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                        BudgetItem.PeriodType.values()));
        mPeriodTypeView.setOnTouchListener(nameFocuser);
        mPeriodTypeView.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override public void onItemSelected(AdapterView<?> parent, View view,
                            int position, long id) {
                        mFirstOccurrenceEndView.setError(null);
                    }

                    @Override public void onNothingSelected(AdapterView<?> parent) {
                        // nothing to do
                    }
                });

        mFirstOccurrenceStartView.setOnClickListener(mDatePickerOnClickListener);
        mFirstOccurrenceEndView.setOnClickListener(mDatePickerOnClickListener);

        if (savedInstanceState != null) {
            mOriginalBudgetItem = savedInstanceState.getParcelable(EXTRA_ITEM);
        }
        if (mOriginalBudgetItem == null && getArguments() != null &&
                getArguments().containsKey(EXTRA_ITEM_ID)) {
            setContent(getArguments().getInt(EXTRA_ITEM_ID), true);
        } else {
            setContent(null, true);
        }
        return mRootView;
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // if (mOriginalBudgetItem != null) {
        // TODO
        // user data is automatically saved, but we need to transfer the
        // budgetModifier field as well, because it is the basis of comparison when
        // determining whether user data should be persisted or not.
        // outState.putParcelable(EXTRA_ITEM, mOriginalBudgetItem);
        // }
    }

    /**
     * @param budgetItemId         can be null, in which case the content is cleared
     * @param showKeyboardWhenDone after data has been loaded from the database and
     *                             displayed, focus on the name
     *                             EditText and show the keyboard
     */
    public void setContent(Integer budgetItemId, final boolean showKeyboardWhenDone) {
        if (budgetItemId != null) {
            new BudgetItemReadTaskBase(budgetItemId, new Callback<BudgetItem>() {

                @Override public void onSuccess(BudgetItem budgetItem) {
                    BudgetItemEditorFragment.this.mOriginalBudgetItem = budgetItem;
                    mFirstOccurrenceEndPickerDialog = null;
                    mFirstOccurrenceStartPickerDialog = null;
                    applyBudgetItemToScreen(budgetItem);

                    if (showKeyboardWhenDone && isAdded()) {
                        ViewUtils.showKeyboard(mNameView);
                    }

                    if (mDeleteButton != null) {
                        mDeleteButton.setVisible(true);
                    }
                    Toast.makeText(App.getAppContext(), "BudgetItem loaded",
                            Toast.LENGTH_SHORT)
                            .show();
                }

                @Override public void onFailure() {
                    Toast.makeText(App.getAppContext(), "BudgetItem was not found",
                            Toast.LENGTH_SHORT)
                            .show();
                }

                @Override public void onError(Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(App.getAppContext(),
                            "Error loading data. Check logs for more information.",
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }).execute();
        } else {
            mOriginalBudgetItem = null;
            mFirstOccurrenceEndPickerDialog = null;
            mFirstOccurrenceStartPickerDialog = null;
            clearScreen();
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit, menu);
        this.mDeleteButton = menu.findItem(R.id.menu_delete);

        if (getArguments() == null || !getArguments().containsKey(EXTRA_ITEM_ID)) {
            mDeleteButton.setVisible(false);
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                saveUserInputOrShowError();
                break;
            case R.id.menu_delete:
                if (mOriginalBudgetItem != null && mOriginalBudgetItem.isPersisted()) {
                    new BudgetItemDeleteTaskBase(new Callback<Boolean>() {

                        @Override public void onSuccess(Boolean result) {
                            mDeleteButton.setVisible(false);
                            Toast.makeText(App.getAppContext(), "BudgetItem deleted",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override public void onFailure() {
                            Toast.makeText(App.getAppContext(),
                                    "BudgetItem was not found", Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override public void onError(Throwable t) {
                            t.printStackTrace();
                            Toast.makeText(App.getAppContext(),
                                    "Error deleting data. Check logs for more " +
                                            "information.",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, mOriginalBudgetItem.mId).execute();
                }
                break;
        }
        return false;
    }

    public void saveAndRun(Runnable onSaveOrDiscard) {
        if (shouldSave()) {
            DialogUtils.getSaveOrDiscardDialog(getContext(), new DialogUtils.Executable() {

                @Override public boolean run() {
                    return saveUserInputOrShowError();
                }
            }, onSaveOrDiscard)
                    .show();
        } else {
            onSaveOrDiscard.run();
        }
    }

    /**
     * @return true if user data is valid
     */
    private synchronized boolean saveUserInputOrShowError() {
        ValidationResultImpl validationResult = validateUserInput();
        if (validationResult != null) {
            final BudgetItem newBudgetItem = getBudgetItemFromScreen();

            if (mOriginalBudgetItem != null) {
                // this will make an already saved item ot be updated instead of a new
                // one to be created
                newBudgetItem.mId = mOriginalBudgetItem.mId;
            }
            new BudgetItemCreateOrUpdateTaskBase(newBudgetItem,
                    new Callback<Dao.CreateOrUpdateStatus>() {

                        @Override public void onSuccess(Dao.CreateOrUpdateStatus result) {
                            BudgetItemEditorFragment.this.mOriginalBudgetItem =
                                    newBudgetItem;
                            mDeleteButton.setVisible(true);
                            loadSpendingOccurrences(newBudgetItem);
                            Toast.makeText(App.getAppContext(), "BudgetItem saved",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override public void onFailure() {
                        }

                        @Override public void onError(Throwable t) {
                            if (t.getCause() != null && t.getCause()
                                    .getCause() != null && t.getCause()
                                    .getCause() instanceof SQLiteConstraintException) {
                                t = t.getCause()
                                        .getCause();
                            }
                            t.printStackTrace();
                            Toast.makeText(App.getAppContext(),
                                    "Error saving data. Check logs for more information.",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }).execute();
            return true;
        } else {
            return false;
        }
    }

    private void applyBudgetItemToScreen(final BudgetItem budgetItem) {
        mNameView.setText(budgetItem.mName);

        if (budgetItem.mAmount != null) {
            mAmountView.setText(getString(R.string.detail_amount, budgetItem.mAmount));
        } else {
            mAmountView.setText(null);
        }
        mEnabledView.setChecked(budgetItem.mEnabled);
        if (budgetItem.mType != null) {
            mCategoryView.setSelection(budgetItem.mType.ordinal());
        }

        Date firstOccurrenceStart = budgetItem.mFirstOccurrenceStart != null
                                    ? budgetItem.mFirstOccurrenceStart
                                    : DEFAULT_FIRST_OCCURRENCE_START;
        applyFirstOccurrenceStartToScreen(firstOccurrenceStart);

        Date firstOccurrenceEnd =
                budgetItem.mFirstOccurrenceEnd != null ? budgetItem.mFirstOccurrenceEnd
                                                       : DEFAULT_FIRST_OCCURRENCE_END;
        applyFirstOccurrenceEndToScreen(firstOccurrenceEnd);

        if (budgetItem.mOccurrenceCount != null) {
            mOccurrenceLimitView.setText(Integer.toString(budgetItem.mOccurrenceCount));
        } else {
            mOccurrenceLimitView.setText(null);
        }

        if (budgetItem.mPeriodMultiplier != null) {
            mPeriodMultiplierView.setText(Integer.toString(budgetItem.mPeriodMultiplier));
        } else {
            mPeriodMultiplierView.setText(null);
        }

        if (budgetItem.mPeriodType != null) {
            mPeriodTypeView.setSelection(budgetItem.mPeriodType.ordinal());
        }
        mNotesView.setText(budgetItem.mNotes);
        loadSpendingOccurrences(budgetItem);
    }

    private void clearScreen() {
        mNameView.setText(null);
        mAmountView.setText(null);
        mEnabledView.setChecked(true);
        mCategoryView.setSelection(0);
        applyFirstOccurrenceStartToScreen(DEFAULT_FIRST_OCCURRENCE_START);
        applyFirstOccurrenceEndToScreen(DEFAULT_FIRST_OCCURRENCE_END);
        mOccurrenceLimitView.setText(null);
        mPeriodMultiplierView.setText(null);
        mPeriodTypeView.setSelection(0);
        mNotesView.setText(null);
        mSpendingEventsView.setText(null);
    }

    private void loadSpendingOccurrences(final BudgetItem budgetItem) {
        new LastBalanceUpdateLoaderTask() {

            @Override
            protected void onPostExecute(BalanceUpdateEvent balanceUpdateEvent) {
                BalanceCalculator.BalanceResult result = BalanceCalculator.get()
                        .getEstimatedBalance(budgetItem,
                                balanceUpdateEvent != null ? balanceUpdateEvent.when
                                                           : null, new Date());
                mSpendingEventsView.setText(ArrayUtils.join("\n", result.spendingEvents,
                        new ArrayUtils.Stringifier<Date>() {

                            @Override public String toString(int index, Date item) {
                                return getString(R.string.spending_occurrence, index + 1,
                                        DateUtils.DEFAULT_DATE_FORMAT.format(item));
                            }
                        }));
            }
        }.execute();
    }

    private void applyFirstOccurrenceStartToScreen(Date firstOccurrenceStart) {
        mFirstOccurrenceStartView.setText(
                DateUtils.DEFAULT_DATE_FORMAT.format(firstOccurrenceStart));
    }

    private void applyFirstOccurrenceEndToScreen(Date firstOccurrenceEnd) {
        mFirstOccurrenceEndView.setText(
                DateUtils.DEFAULT_DATE_FORMAT.format(firstOccurrenceEnd));
    }

    private DatePickerDialog getFirstOccurrenceStartPickerDialog() {
        Calendar c = Calendar.getInstance();
        if (mOriginalBudgetItem != null &&
                mOriginalBudgetItem.mFirstOccurrenceStart != null) {
            c.setTime(this.mOriginalBudgetItem.mFirstOccurrenceStart);
        }
        if (mFirstOccurrenceStartPickerDialog == null) {
            mFirstOccurrenceStartPickerDialog =
                    new DatePickerDialog(getActivity(), mDateSetListener,
                            c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH));
            mFirstOccurrenceStartPickerDialog.getDatePicker()
                    .setTag(R.id.first_occurence_start);
        }
        return mFirstOccurrenceStartPickerDialog;
    }

    private DatePickerDialog getFirstOccurrenceEndPickerDialog() {
        Calendar c = Calendar.getInstance();
        if (mOriginalBudgetItem != null &&
                mOriginalBudgetItem.mFirstOccurrenceEnd != null) {
            c.setTime(this.mOriginalBudgetItem.mFirstOccurrenceEnd);
        }
        if (mFirstOccurrenceEndPickerDialog == null) {
            mFirstOccurrenceEndPickerDialog =
                    new DatePickerDialog(getActivity(), mDateSetListener,
                            c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH));
            mFirstOccurrenceEndPickerDialog.getDatePicker()
                    .setTag(R.id.first_occurence_end);
        }
        return mFirstOccurrenceEndPickerDialog;
    }

    private static class ValidationResultImpl {
        final ValidationError[] errors;

        public ValidationResultImpl(ValidationError[] errors) {
            this.errors = errors;
        }
    }

    /**
     * There are two kinds of validation errors: the ones that can be shown in an input
     * field (with {@link TextView#setError(CharSequence)} and the ones tha are shown
     * as a Toast or Dialog instead (DatePicker, etc...).
     */
    public static class ValidationError {
        public static final int TYPE_INPUT_FIELD = 1;
        public static final int TYPE_NON_INPUT_FIELD = 2;

        public final int type;
        public final View target;
        public final String errorMessage;

        public ValidationError(int type, View target, String errorMessage) {
            if (type == TYPE_INPUT_FIELD) {
                if (target == null) {
                    throw new IllegalArgumentException(
                            "Please specify a target view for the input field error " +
                                    "message");
                }
                if (!(target instanceof TextView)) {
                    throw new IllegalArgumentException(
                            "Wrong view type in ValidationError. Cannot show error " +
                                    "message.");

                }
            } else if (type != TYPE_NON_INPUT_FIELD) {
                throw new IllegalArgumentException(
                        "\"type\" must be one of {TYPE_INPUT_FIELD, " +
                                "TYPE_NON_INPUT_FIELD}");
            }
            this.type = type;
            this.target = target;
            this.errorMessage = errorMessage;
        }

        public void showError(Context context) throws IllegalArgumentException {
            if (type == TYPE_INPUT_FIELD) {
                TextView textView = (TextView) target;
                textView.setError("Please specify a name");
                textView.requestFocus();
            } else if (type == TYPE_NON_INPUT_FIELD) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT)
                        .show();
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
    private ValidationResultImpl validateUserInput() {
        List<ValidationError> errors = new ArrayList<>();

        if (TextUtils.isEmpty(mNameView.getText())) {
            errors.add(new ValidationError(ValidationError.TYPE_INPUT_FIELD, mNameView,
                    "Please specify a name"));
        }
        if (TextUtils.isEmpty(mAmountView.getText())) {
            errors.add(new ValidationError(ValidationError.TYPE_INPUT_FIELD, mAmountView,
                    "Please specify an amount"));
        }
        Date firstOccurrenceStart = getFirstOccurrenceStartFromScreen();
        Date firstOccurrenceEnd = getFirstOccurrenceEndFromScreen();

        if (firstOccurrenceStart.after(firstOccurrenceEnd)) {
            errors.add(new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null,
                    "Start date must not be higher then end date"));
        }

        Calendar c = Calendar.getInstance();
        c.setTime(firstOccurrenceStart);
        DateUtils.clearLowerBits(c);

        ((BudgetItem.PeriodType) mPeriodTypeView.getSelectedItem()).apply(c,
                getPeriodMultiplierFromScreen());
        c.add(Calendar.DAY_OF_MONTH, -1);

        if (firstOccurrenceEnd.after(c.getTime())) {
            errors.add(new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD,
                    mFirstOccurrenceEndView, "End date cannot be higher than " +
                    DateUtils.DEFAULT_DATE_FORMAT.format(c.getTime())));
        }

        return new ValidationResultImpl(errors.isEmpty() ? null : errors.toArray(
                new ValidationError[errors.size()]));
    }

    private BudgetItem getBudgetItemFromScreen() {
        BudgetItem budgetItem = new BudgetItem();
        // title
        if (!TextUtils.isEmpty(mNameView.getText())) {
            budgetItem.mName = mNameView.getText()
                    .toString();
        }
        // amount
        if (!TextUtils.isEmpty(mAmountView.getText())) {
            budgetItem.mAmount = Float.valueOf(mAmountView.getText()
                    .toString());
        }
        // enabled
        budgetItem.mEnabled = mEnabledView.isChecked();
        // type
        budgetItem.mType = (BudgetItem.BudgetItemType) mCategoryView.getSelectedItem();
        // firstOccurrenceStart
        budgetItem.mFirstOccurrenceStart = getFirstOccurrenceStartFromScreen();
        // firstOccurrenceEnd
        budgetItem.mFirstOccurrenceEnd = getFirstOccurrenceEndFromScreen();
        // repetition
        if (!TextUtils.isEmpty(mOccurrenceLimitView.getText())) {
            budgetItem.mOccurrenceCount = Integer.valueOf(mOccurrenceLimitView.getText()
                    .toString());
        }
        // periodMultiplier
        budgetItem.mPeriodMultiplier = getPeriodMultiplierFromScreen();
        // period
        budgetItem.mPeriodType =
                (BudgetItem.PeriodType) mPeriodTypeView.getSelectedItem();
        // notes
        if (!TextUtils.isEmpty(mNotesView.getText())) {
            budgetItem.mNotes = mNotesView.getText()
                    .toString();
        }
        return budgetItem;
    }

    private Date getFirstOccurrenceStartFromScreen() {
        Date firstOccurrenceDateStart;

        if (mFirstOccurrenceStartPickerDialog != null) {
            // user picked a date
            firstOccurrenceDateStart = DateUtils.getDayFromDatePicker(
                    mFirstOccurrenceStartPickerDialog.getDatePicker());
        } else if (mOriginalBudgetItem != null &&
                mOriginalBudgetItem.mFirstOccurrenceStart != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateStart = mOriginalBudgetItem.mFirstOccurrenceStart;
        } else {
            // user did not pick a date, and we are in CREATE mode
            firstOccurrenceDateStart = DEFAULT_FIRST_OCCURRENCE_START;
        }
        return firstOccurrenceDateStart;
    }

    private Date getFirstOccurrenceEndFromScreen() {
        Date firstOccurrenceDateEnd;

        if (mFirstOccurrenceEndPickerDialog != null) {
            // user picked a date
            firstOccurrenceDateEnd = DateUtils.getDayFromDatePicker(
                    mFirstOccurrenceEndPickerDialog.getDatePicker());
        } else if (mOriginalBudgetItem != null &&
                mOriginalBudgetItem.mFirstOccurrenceEnd != null) {
            // user did not pick a date, but this is EDIT, not CREATE
            firstOccurrenceDateEnd = mOriginalBudgetItem.mFirstOccurrenceEnd;
        } else {
            // user did not pick a date, and we are in CREATE mode
            firstOccurrenceDateEnd = DEFAULT_FIRST_OCCURRENCE_END;
        }
        return firstOccurrenceDateEnd;
    }

    private int getPeriodMultiplierFromScreen() {
        int periodMultiplier;

        if (!TextUtils.isEmpty(mPeriodMultiplierView.getText())) {
            // user entered value
            periodMultiplier = Integer.valueOf(mPeriodMultiplierView.getText()
                    .toString());
        } else if (mOriginalBudgetItem != null &&
                mOriginalBudgetItem.mPeriodMultiplier != null) {
            // user did not enter a value, but this is EDIT, not CREATE
            periodMultiplier = mOriginalBudgetItem.mPeriodMultiplier;
        } else {
            // user did not enter a value, and we are in CREATE mode
            periodMultiplier = DEFAULT_PERIOD_MULTIPLIER;
        }
        return periodMultiplier;
    }

    /**
     * @return true if the content differs from the originally loaded BudgetItem, or if
     * this fragment contains unsaved user input
     */
    private boolean shouldSave() {
        BudgetItem userInput = getBudgetItemFromScreen();
        boolean isNew =
                mOriginalBudgetItem == null && !new BudgetItem().equals(userInput);
        boolean changed =
                mOriginalBudgetItem != null && !mOriginalBudgetItem.equals(userInput);
        return isNew || changed;
    }
}
