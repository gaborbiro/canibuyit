package com.gb.canibuythat.ui;

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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.gb.canibuythat.util.ValidationError;
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

    @Inject UserPreferences userPreferences;
    @Inject SpendingInteractor spendingInteractor;

    @BindView(R.id.name) EditText nameInput;
    @BindView(R.id.amount) EditText averageInput;
    @BindView(R.id.target) EditText targetInput;
    @BindView(R.id.enabled) CheckBox enabledCB;
    @BindView(R.id.category) Spinner categoryPicker;
    @BindView(R.id.occurrence_count) EditText occurrenceInput;
    @BindView(R.id.cycle_multiplier) EditText cycleMultiplierInput;
    @BindView(R.id.cycle_picker) Spinner cyclePicker;
    @BindView(R.id.from_date_picker) DateRangePicker fromDatePicker;
    @BindView(R.id.notes) EditText notesInput;
    @BindView(R.id.spending_events) TextView spendingEventsLayout;

    private Spending originalSpending;
    private boolean cycleMultiplierChanged;
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
        fromDatePicker.setTouchInterceptor(new DateRangePicker.TouchInterceptor() {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                keyboardDismisser.onTouch(fromDatePicker, MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0));
                return false;
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
            error.showError(getActivity());
            return false;
        }
    }

    private void onSpendingLoaded(Spending spending, boolean showKeyboardWhenDone) {
        this.originalSpending = spending;
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
        fromDatePicker.setStartDate(spending.getFromStartDate());
        fromDatePicker.setEndDate(spending.getFromEndDate());

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
        Date firstOccurrenceStart = fromDatePicker.getStartDate();
        Date firstOccurrenceEnd = fromDatePicker.getEndDate();
        if (firstOccurrenceStart.after(firstOccurrenceEnd)) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null, "Start date must not be higher then end date");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstOccurrenceStart);
        DateUtils.clearLowerBits(calendar);

        ((Spending.Cycle) cyclePicker.getSelectedItem()).apply(calendar, getCycleMultiplierFromScreen());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        if (firstOccurrenceEnd.after(calendar.getTime())) {
            return new ValidationError(ValidationError.TYPE_NON_INPUT_FIELD, null,
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
        spending.setFromStartDate(fromDatePicker.getStartDate());
        // fromEndDate
        spending.setFromEndDate(fromDatePicker.getEndDate());
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
                && !new Spending().compareForEditing(newSpending, !(fromDatePicker.isStartDateChanged() || fromDatePicker.isEndDateChanged()), !cycleMultiplierChanged);
        boolean changed = originalSpending != null && !originalSpending.compareForEditing(newSpending, false, false);
        return isNew || changed;
    }
}
