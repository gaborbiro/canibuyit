package com.gb.canibuythat.ui;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
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
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.util.ArrayUtils;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.ViewUtils;
import com.j256.ormlite.dao.Dao;

import org.greenrobot.eventbus.EventBus;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A fragment representing a single BudgetModifier detail screen. This fragment is either
 * contained in a {@link BudgetItemListActivity} in two-pane mode (on tablets) or a
 * {@link BudgetItemDetailActivity} on handsets.
 */
public class BudgetItemDetailFragment extends Fragment {

    public static final String EXTRA_ITEM_ID = "budget_item_id";

    private static final String EXTRA_ITEM = "budget_item";

    private static final Date DEFAULT_FIRST_OCCURRENCE_END;
    private static final Date DEFAULT_FIRST_OCCURRENCE_START;
    private static final int DEFAULT_PERIOD_MULTIPLIER = 1;

    static {
        Calendar c = DateUtils.clearLowerBits(Calendar.getInstance());
        DEFAULT_FIRST_OCCURRENCE_END = c.getTime();
        DEFAULT_FIRST_OCCURRENCE_START = c.getTime();
    }

    @InjectView(R.id.name) EditText mNameView;
    @InjectView(R.id.amount) EditText mAmountView;
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

        @Override
        public boolean onTouch(View v, MotionEvent event) {
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
                    Calendar c = DateUtils.clearLowerBits(Calendar.getInstance());
                    c.set(Calendar.YEAR, year);
                    c.set(Calendar.MONTH, monthOfYear);
                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

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

        @Override
        public void onClick(View v) {
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

    public BudgetItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                            int position, long id) {
                        mFirstOccurrenceEndView.setError(null);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mOriginalBudgetItem != null) {
            // TODO
            // user data is automatically saved, but we need to transfer the
            // budgetModifier field as well, because it is the basis of comparison when
            // determining whether user data should be persisted or not.
            // outState.putParcelable(EXTRA_ITEM, mOriginalBudgetItem);
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
            new BudgetItemLoaderTask(budgetItemId, new BudgetItemLoaderTask.Listener() {

                @Override
                public void onDataReady(BudgetItem budgetItem) {
                    BudgetItemDetailFragment.this.mOriginalBudgetItem = budgetItem;
                    mFirstOccurrenceEndPickerDialog = null;
                    mFirstOccurrenceStartPickerDialog = null;
                    applyBudgetItemToScreen(budgetItem);

                    if (showKeyboardWhenDone && isAdded()) {
                        ViewUtils.showKeyboard(mNameView);
                    }

                    if (mDeleteButton != null) {
                        mDeleteButton.setVisible(true);
                    }
                }
            }).execute();
        } else {
            mOriginalBudgetItem = null;
            mFirstOccurrenceEndPickerDialog = null;
            mFirstOccurrenceStartPickerDialog = null;
            clearScreen();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit, menu);
        this.mDeleteButton = menu.findItem(R.id.menu_delete);

        if (getArguments() == null || !getArguments().containsKey(EXTRA_ITEM_ID)) {
            mDeleteButton.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                saveUserDataOrShowError();
                break;
            case R.id.menu_delete:
                if (mOriginalBudgetItem != null && mOriginalBudgetItem.isPersisted()) {
                    new BudgetItemDeleteTask(getActivity(),
                            new BudgetItemDeleteTask.Listener() {

                                @Override
                                public void onSuccess() {
                                    mDeleteButton.setVisible(false);
                                    Toast.makeText(App.getAppContext(),
                                            "BudgetItem deleted", Toast.LENGTH_SHORT)
                                            .show();
                                }

                                @Override
                                public void onFailure() {
                                    Toast.makeText(App.getAppContext(),
                                            "BudgetItem was not found",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }, mOriginalBudgetItem.mId).execute();
                }
                break;
        }
        return false;
    }

    /**
     * @return true if user data is valid
     */
    public synchronized boolean saveUserDataOrShowError() {
        if (validateUserInput()) {
            final BudgetItem newBudgetItem = getBudgetItemFromScreen();

            if (mOriginalBudgetItem != null) {
                // this will make an already saved item ot be updated instead of a new
                // one to be created
                newBudgetItem.mId = mOriginalBudgetItem.mId;
            }
            new BudgetItemUpdateTask(getActivity(), newBudgetItem,
                    new BudgetItemUpdateTask.Listener() {

                        @Override
                        public void onSuccess() {
                            BudgetItemDetailFragment.this.mOriginalBudgetItem =
                                    newBudgetItem;
                            mDeleteButton.setVisible(true);
                            loadSpendingOccurrences(newBudgetItem);
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

        if (budgetItem.mOccurenceCount != null) {
            mOccurrenceLimitView.setText(Integer.toString(budgetItem.mOccurenceCount));
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
                BalanceCalculator.BalanceResult result =
                        BalanceCalculator.get().getEstimatedBalance(budgetItem,
                                balanceUpdateEvent.when, new Date());
                mSpendingEventsView.setText(ArrayUtils.join("\n", result.spendingEvents,
                        new ArrayUtils.Stringifier<Date>() {

                            @Override
                            public String toString(int index, Date item) {
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

    /**
     * Verify whether user input is valid and show appropriate error messages
     *
     * @return true if user input is valid
     */
    private boolean validateUserInput() {
        if (TextUtils.isEmpty(mNameView.getText())) {
            mNameView.setError("Please specify a name");
            mNameView.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(mAmountView.getText())) {
            mAmountView.setError("Please specify an amount");
            mAmountView.requestFocus();
            return false;
        }
        Date firstOccurrenceStart = getFirstOccurrenceStartFromScreen();
        Date firstOccurrenceEnd = getFirstOccurrenceEndFromScreen();

        if (firstOccurrenceStart.after(firstOccurrenceEnd)) {
            Toast.makeText(getActivity(), "Start date must not be higher then end date",
                    Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(firstOccurrenceStart);
        DateUtils.clearLowerBits(c);

        ((BudgetItem.PeriodType) mPeriodTypeView.getSelectedItem()).apply(c,
                getPeriodMultiplierFromScreen());
        c.add(Calendar.DAY_OF_MONTH, -1);

        if (firstOccurrenceEnd.after(c.getTime())) {
            Toast.makeText(getActivity(), "End date cannot be higher than " +
                    DateUtils.DEFAULT_DATE_FORMAT.format(c.getTime()), Toast.LENGTH_SHORT)
                    .show();
            mFirstOccurrenceEndView.requestFocus();
            return false;
        }

        return true;
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
        // type
        budgetItem.mType = (BudgetItem.BudgetItemType) mCategoryView.getSelectedItem();
        // firstOccurrenceStart
        budgetItem.mFirstOccurrenceStart = getFirstOccurrenceStartFromScreen();
        // firstOccurrenceEnd
        budgetItem.mFirstOccurrenceEnd = getFirstOccurrenceEndFromScreen();
        // repetition
        if (!TextUtils.isEmpty(mOccurrenceLimitView.getText())) {
            budgetItem.mOccurenceCount = Integer.valueOf(mOccurrenceLimitView.getText()
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
     * @return true if the content differs relative to the original BudgetItem that was
     * loaded,
     * or the content hasn't been yet saved
     */
    public boolean isChanged() {
        return (mOriginalBudgetItem == null &&
                !new BudgetItem().equals(getBudgetItemFromScreen())) ||
                (mOriginalBudgetItem != null &&
                        !mOriginalBudgetItem.equals(getBudgetItemFromScreen()));
    }

    private static class BudgetItemLoaderTask extends AsyncTask<Void, Void, BudgetItem> {

        private int mBudgetItemId;
        private Listener mListener;

        private BudgetItemLoaderTask(int budgetItemId, Listener listener) {
            this.mBudgetItemId = budgetItemId;
            this.mListener = listener;
        }

        @Override
        protected BudgetItem doInBackground(Void... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();
            try {
                Dao<BudgetItem, Integer> dao = helper.getDao(BudgetItem.class);
                return dao.queryForId(mBudgetItemId);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(BudgetItem budgetItem) {
            mListener.onDataReady(budgetItem);
        }

        public interface Listener {

            void onDataReady(BudgetItem budgetItem);
        }
    }

    private static class BudgetItemUpdateTask extends AsyncTask<Void, Void, Void> {

        private Context mContext;
        private BudgetItem mBudgetItem;
        private Listener mListener;
        private Dao.CreateOrUpdateStatus mResult;
        private SQLiteConstraintException e;

        private BudgetItemUpdateTask(Context context, BudgetItem budgetItem,
                Listener listener) {
            this.mContext = context;
            this.mBudgetItem = budgetItem;
            this.mListener = listener;
        }

        @Override
        protected Void doInBackground(Void... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();
            try {
                Dao<BudgetItem, Integer> dao = helper.getDao(BudgetItem.class);
                mResult = dao.createOrUpdate(mBudgetItem);
                mContext.getContentResolver()
                        .notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
                EventBus.getDefault()
                        .post(new BudgetItemUpdatedEvent());
            } catch (SQLException e) {
                e.printStackTrace();
                if (e.getCause() != null && e.getCause()
                        .getCause() != null && e.getCause()
                        .getCause() instanceof SQLiteConstraintException) {
                    this.e = (SQLiteConstraintException) e.getCause()
                            .getCause();
                } else {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (e == null) {
                Toast.makeText(mContext,
                        mResult.isCreated() ? "BudgetItem created" : "BudgetItem updated",
                        Toast.LENGTH_SHORT)
                        .show();
                mListener.onSuccess();
            } else {
                DialogUtils.getErrorDialog(mContext, e.getMessage())
                        .show();
            }
        }

        public interface Listener {

            void onSuccess();
        }
    }

    private static class BudgetItemDeleteTask extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;
        private Listener mListener;
        private int mId;

        private BudgetItemDeleteTask(Context context, Listener listener, int id) {
            this.mContext = context;
            this.mListener = listener;
            this.mId = id;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();
            try {
                Dao<BudgetItem, Integer> dao = helper.getDao(BudgetItem.class);
                boolean success = dao.deleteById(mId) > 0;
                mContext.getContentResolver()
                        .notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
                return success;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mListener.onSuccess();
            } else {
                mListener.onFailure();
            }
        }

        public interface Listener {

            void onSuccess();

            void onFailure();
        }
    }
}
