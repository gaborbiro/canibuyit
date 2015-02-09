package com.gb.canibuythat.ui;


import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.gb.canibuythat.App;
import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BalanceCalculator;
import com.gb.canibuythat.model.BudgetModifier;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.ViewUtils;
import com.j256.ormlite.dao.Dao;


/**
 * A fragment representing a single BudgetModifier detail screen. This fragment is either contained in a
 * {@link BudgetModifierListActivity} in two-pane mode (on tablets) or a {@link BudgetModifierDetailActivity} on
 * handsets.
 */
public class BudgetModifierDetailFragment extends Fragment implements View.OnClickListener,
		DatePickerDialog.OnDateSetListener {

	private static final SimpleDateFormat	SPINNER_DATE_FORMAT			= new SimpleDateFormat("MMM. dd");

	public static final String				EXTRA_ITEM_ID				= "item_id";
	private static final String				EXTRA_ITEM					= "item";

	private static final Date				DEFAULT_UPPER_DATE;
	private static final Date				DEFAULT_LOWER_DATE;
	private static final int				DEFAULT_PERIOD_MULTIPLIER	= 1;

	static {
		Calendar c = DateUtils.clearLowerBits(Calendar.getInstance());
		DEFAULT_UPPER_DATE = c.getTime();
		DEFAULT_LOWER_DATE = c.getTime();
	}

	private BudgetModifier					budgetModifier;
	private DatePickerDialog				lowerDatePickerDialog;
	private DatePickerDialog				upperDatePickerDialog;

	@InjectView(R.id.title)
	EditText								titleET;

	@InjectView(R.id.amount)
	EditText								amountET;

	@InjectView(R.id.type)
	Spinner									typeSpinner;

	@InjectView(R.id.lower_date)
	EditText								lowerDateET;

	@InjectView(R.id.upper_date)
	EditText								upperDateET;

	@InjectView(R.id.repetition_count)
	EditText								repetitionCountET;

	@InjectView(R.id.period_multiplier)
	EditText								periodMultiplierET;

	@InjectView(R.id.period_type)
	Spinner									periodTypeSpinner;

	@InjectView(R.id.notes)
	EditText								notesET;

	private MenuItem						deleteMenuItem;
	private ViewGroup						rootView;


	public BudgetModifierDetailFragment() {
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = (ViewGroup) inflater.inflate(R.layout.fragment_budgetmodifier_detail, container, false);
		ButterKnife.inject(this, rootView);

		typeSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
				BudgetModifier.BudgetModifierType.values()));
		typeSpinner.setOnTouchListener(titleFocuser);

		periodTypeSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
				BudgetModifier.PeriodType.values()));
		periodTypeSpinner.setOnTouchListener(titleFocuser);
		periodTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				upperDateET.setError(null);
			}


			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// nothing to do
			}
		});

		lowerDateET.setOnClickListener(this);
		upperDateET.setOnClickListener(this);

		if (savedInstanceState != null) {
			budgetModifier = savedInstanceState.getParcelable(EXTRA_ITEM);
		}
		if (budgetModifier == null && getArguments() != null && getArguments().containsKey(EXTRA_ITEM_ID)) {
			setContent(getArguments().getInt(EXTRA_ITEM_ID), true);
		} else {
			ViewUtils.showKeyboard(titleET);
		}
		return rootView;
	}

	private View.OnTouchListener	titleFocuser	= new View.OnTouchListener() {

														@Override
														public boolean onTouch(View v, MotionEvent event) {
															if (event.getAction() == MotionEvent.ACTION_DOWN
																	&& rootView.getFocusedChild() != null) {
																rootView.getFocusedChild().clearFocus();
																// titleET will automatically get the focus
															}
															return false;
														}
													};


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (budgetModifier != null) {
			// user data is automatically saved, but we need to transfer the budgetModifier field as well, 
			// because it is the basis of comparison when determining whether user data should be persisted or not 
			outState.putParcelable(EXTRA_ITEM, budgetModifier);
		}
	}


	/**
	 * 
	 * @param id
	 *            can be null, in which case the content is cleared
	 * @param showKeyboardWhenDone
	 *            after data has been loaded from the database and displayed, focus on the title EditText and show the
	 *            keyboard
	 */
	public void setContent(Integer id, final boolean showKeyboardWhenDone) {
		if (id != null) {
			new BudgetModifierLoaderTask(new BudgetModifierLoaderTask.Listener() {

				@Override
				public void onDataReady(BudgetModifier budgetModifier) {
					BudgetModifierDetailFragment.this.budgetModifier = budgetModifier;
					upperDatePickerDialog = null;
					lowerDatePickerDialog = null;
					displayBudgetModifier(budgetModifier);

					if (showKeyboardWhenDone && isAdded()) {
						ViewUtils.showKeyboard(titleET);
					}

					if (deleteMenuItem != null) {
						deleteMenuItem.setVisible(true);
					}
				}
			}, id).execute();
		} else {
			budgetModifier = null;
			upperDatePickerDialog = null;
			lowerDatePickerDialog = null;
			displayBudgetModifier(null);
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit, menu);
		this.deleteMenuItem = menu.findItem(R.id.menu_delete);

		if (getArguments() == null || !getArguments().containsKey(EXTRA_ITEM_ID)) {
			deleteMenuItem.setVisible(false);
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_save:
			saveUserData();
			break;
		case R.id.menu_delete:
			if (budgetModifier != null && budgetModifier.id != null) {
				new BudgetModifierDeleteTask(getActivity(), new BudgetModifierDeleteTask.Listener() {

					@Override
					public void onDataDeleted() {
						deleteMenuItem.setVisible(false);
					}


					@Override
					public void onNothingDeleted() {
						Toast.makeText(App.getAppContext(), "BudgetModifier was not found", Toast.LENGTH_SHORT).show();
					}
				}, budgetModifier.id).execute();
			}
			break;
		}
		return false;
	}


	/**
	 * @return true data can be saved, false if data is invalid
	 */
	public synchronized boolean saveUserData() {
		if (validate()) {
			final BudgetModifier newBudgetModifier = gatherData();

			if (budgetModifier != null) {
				newBudgetModifier.id = budgetModifier.id;
			}
			new BudgetModifierUpdateTask(getActivity(), newBudgetModifier, new BudgetModifierUpdateTask.Listener() {

				@Override
				public void onSuccess() {
					BudgetModifierDetailFragment.this.budgetModifier = newBudgetModifier;
				}
			}).execute();
			deleteMenuItem.setVisible(true);
			return true;
		} else {
			return false;
		}
	}


	private void displayBudgetModifier(BudgetModifier budgetModifier) {
		if (budgetModifier != null) {
			titleET.setText(budgetModifier.title);

			if (budgetModifier.amount != null) {
				amountET.setText(Float.toString(budgetModifier.amount));
			} else {
				amountET.setText(null);
			}

			if (budgetModifier.type != null) {
				typeSpinner.setSelection(budgetModifier.type.ordinal());
			}

			Date lowerDate = budgetModifier.lowerDate != null ? budgetModifier.lowerDate : DEFAULT_LOWER_DATE;
			lowerDateET.setText(SPINNER_DATE_FORMAT.format(lowerDate));

			Date upperDate = budgetModifier.upperDate != null ? budgetModifier.upperDate : DEFAULT_UPPER_DATE;
			upperDateET.setText(SPINNER_DATE_FORMAT.format(upperDate));

			if (budgetModifier.repetitionCount != null) {
				repetitionCountET.setText(Integer.toString(budgetModifier.repetitionCount));
			} else {
				repetitionCountET.setText(null);
			}

			if (budgetModifier.periodMultiplier != null) {
				periodMultiplierET.setText(Integer.toString(budgetModifier.periodMultiplier));
			} else {
				periodMultiplierET.setText(null);
			}

			if (budgetModifier.periodType != null) {
				periodTypeSpinner.setSelection(budgetModifier.periodType.ordinal());
			}
			notesET.setText(budgetModifier.notes);
		} else {
			titleET.setText(null);
			amountET.setText(null);
			typeSpinner.setSelection(0);
			lowerDateET.setText(SPINNER_DATE_FORMAT.format(DEFAULT_LOWER_DATE));
			upperDateET.setText(SPINNER_DATE_FORMAT.format(DEFAULT_UPPER_DATE));
			repetitionCountET.setText(null);
			periodMultiplierET.setText(null);
			periodTypeSpinner.setSelection(0);
			notesET.setText(null);
		}
	}


	private DatePickerDialog getLowerDatePickerDialog() {
		Calendar c = Calendar.getInstance();
		if (budgetModifier != null && budgetModifier.lowerDate != null) {
			c.setTime(this.budgetModifier.lowerDate);
		}
		if (lowerDatePickerDialog == null) {
			lowerDatePickerDialog = new DatePickerDialog(getActivity(), this, c.get(Calendar.YEAR),
					c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
			lowerDatePickerDialog.getDatePicker().setTag(R.id.lower_date);
		}
		return lowerDatePickerDialog;
	}


	private DatePickerDialog getUpperDatePickerDialog() {
		Calendar c = Calendar.getInstance();
		if (budgetModifier != null && budgetModifier.upperDate != null) {
			c.setTime(this.budgetModifier.upperDate);
		}
		if (upperDatePickerDialog == null) {
			upperDatePickerDialog = new DatePickerDialog(getActivity(), this, c.get(Calendar.YEAR),
					c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
			upperDatePickerDialog.getDatePicker().setTag(R.id.upper_date);
		}
		return upperDatePickerDialog;
	}


	private boolean validate() {
		if (TextUtils.isEmpty(titleET.getText())) {
			titleET.setError("Please specify a title");
			titleET.requestFocus();
			return false;
		}
		if (TextUtils.isEmpty(amountET.getText())) {
			amountET.setError("Please specify an amount");
			amountET.requestFocus();
			return false;
		}
		Date lowerDate = getLowerDate();
		Date upperDate = getUpperDate();

		if (lowerDate.after(upperDate)) {
			upperDateET.setError("First date must not be after the second");
			upperDateET.requestFocus();
			return false;
		}

		Calendar c = Calendar.getInstance();
		c.setTime(lowerDate);
		DateUtils.clearLowerBits(c);

		BalanceCalculator.increaseDateWithPeriod(c, (BudgetModifier.PeriodType) periodTypeSpinner.getSelectedItem(),
				getPeriodMultiplier());
		c.add(Calendar.DAY_OF_MONTH, -1);

		if (upperDate.after(c.getTime())) {
			upperDateET.setError("Second date cannot be higher than " + SPINNER_DATE_FORMAT.format(c.getTime()));
			upperDateET.requestFocus();
			return false;
		}

		return true;
	}


	private BudgetModifier gatherData() {
		BudgetModifier budgetModifier = new BudgetModifier();
		// title
		if (!TextUtils.isEmpty(titleET.getText())) {
			budgetModifier.title = titleET.getText().toString();
		}
		// amount
		if (!TextUtils.isEmpty(amountET.getText())) {
			budgetModifier.amount = Float.valueOf(amountET.getText().toString());
		}
		// type
		budgetModifier.type = (BudgetModifier.BudgetModifierType) typeSpinner.getSelectedItem();
		// lowerDate
		budgetModifier.lowerDate = getLowerDate();
		// upperDate
		budgetModifier.upperDate = getUpperDate();
		// repetition
		if (!TextUtils.isEmpty(repetitionCountET.getText())) {
			budgetModifier.repetitionCount = Integer.valueOf(repetitionCountET.getText().toString());
		}
		// periodMultiplier
		budgetModifier.periodMultiplier = getPeriodMultiplier();
		// period
		budgetModifier.periodType = (BudgetModifier.PeriodType) periodTypeSpinner.getSelectedItem();
		// notes
		if (!TextUtils.isEmpty(notesET.getText())) {
			budgetModifier.notes = notesET.getText().toString();
		}
		return budgetModifier;
	}


	private Date getLowerDate() {
		Date lowerDate;

		if (lowerDatePickerDialog != null) {
			// user picked a date
			lowerDate = getDayFromDatePicker(lowerDatePickerDialog.getDatePicker());
		} else if (budgetModifier != null && budgetModifier.lowerDate != null) {
			// user did not pick a date, but this is EDIT, not CREATE
			lowerDate = budgetModifier.lowerDate;
		} else {
			// user did not pick a date, and we are in CREATE mode
			lowerDate = DEFAULT_LOWER_DATE;
		}
		return lowerDate;
	}


	private Date getUpperDate() {
		Date upperDate;

		if (upperDatePickerDialog != null) {
			// user picked a date
			upperDate = getDayFromDatePicker(upperDatePickerDialog.getDatePicker());
		} else if (budgetModifier != null && budgetModifier.upperDate != null) {
			// user did not pick a date, but this is EDIT, not CREATE
			upperDate = budgetModifier.upperDate;
		} else {
			// user did not pick a date, and we are in CREATE mode
			upperDate = DEFAULT_UPPER_DATE;
		}
		return upperDate;
	}


	private int getPeriodMultiplier() {
		int periodMultiplier;

		if (!TextUtils.isEmpty(periodMultiplierET.getText())) {
			// user entered value
			periodMultiplier = Integer.valueOf(periodMultiplierET.getText().toString());
		} else if (budgetModifier != null && budgetModifier.periodMultiplier != null) {
			// user did not enter a value, but this is EDIT, not CREATE
			periodMultiplier = budgetModifier.periodMultiplier;
		} else {
			// user did not enter a value, and we are in CREATE mode
			periodMultiplier = DEFAULT_PERIOD_MULTIPLIER;
		}
		return periodMultiplier;
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.lower_date:
			getLowerDatePickerDialog().show();
			lowerDateET.setError(null);
			break;
		case R.id.upper_date:
			getUpperDatePickerDialog().show();
			upperDateET.setError(null);
			break;
		}
	}


	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		Calendar c = DateUtils.clearLowerBits(Calendar.getInstance());
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, monthOfYear);
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

		switch ((int) view.getTag()) {
		case R.id.lower_date:
			lowerDateET.setText(SPINNER_DATE_FORMAT.format(c.getTime()));
			break;
		case R.id.upper_date:
			upperDateET.setText(SPINNER_DATE_FORMAT.format(c.getTime()));
			break;
		}
	}


	private static Date getDayFromDatePicker(DatePicker datePicker) {
		return DateUtils.getDay(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
	}


	/**
	 * @return true if the content differs relative to the original BudgetModifier that was loaded, or the content
	 *         hasn't been yet saved
	 */
	public boolean isChanged() {
		return (budgetModifier == null && !new BudgetModifier().equals(gatherData()))
				|| (budgetModifier != null && !budgetModifier.equals(gatherData()));
	}

	private static class BudgetModifierLoaderTask extends AsyncTask<Void, Void, BudgetModifier> {

		public interface Listener {

			public void onDataReady(BudgetModifier budgetModifier);
		}

		private Listener	listener;
		private int			id;


		private BudgetModifierLoaderTask(Listener listener, int id) {
			this.listener = listener;
			this.id = id;
		}


		@Override
		protected BudgetModifier doInBackground(Void... params) {
			BudgetDbHelper helper = BudgetDbHelper.get();
			try {
				Dao<BudgetModifier, Integer> dao = helper.getDao(BudgetModifier.class);
				return dao.queryForId(id);
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}


		@Override
		protected void onPostExecute(BudgetModifier budgetModifier) {
			listener.onDataReady(budgetModifier);
		}
	}

	private static class BudgetModifierUpdateTask extends AsyncTask<Void, Void, Void> {

		public interface Listener {

			public void onSuccess();
		}

		private Context						context;
		private BudgetModifier				budgetModifier;
		private Dao.CreateOrUpdateStatus	result;
		private Listener					listener;

		private SQLiteConstraintException	e;


		private BudgetModifierUpdateTask(Context context, BudgetModifier budgetModifier, Listener listener) {
			this.context = context;
			this.budgetModifier = budgetModifier;
			this.listener = listener;
		}


		@Override
		protected Void doInBackground(Void... params) {
			BudgetDbHelper helper = BudgetDbHelper.get();
			try {
				Dao<BudgetModifier, Integer> dao = helper.getDao(BudgetModifier.class);
				result = dao.createOrUpdate(budgetModifier);
				context.getContentResolver().notifyChange(BudgetProvider.BUDGET_MODIFIERS_URI, null);
			} catch (SQLException e) {
				e.printStackTrace();
				if (e.getCause() != null && e.getCause().getCause() != null
						&& e.getCause().getCause() instanceof SQLiteConstraintException) {
					this.e = (SQLiteConstraintException) e.getCause().getCause();
				} else {
					throw new RuntimeException(e);
				}
			}
			return null;
		}


		@Override
		protected void onPostExecute(Void aVoid) {
			if (e == null) {
				Toast.makeText(context, result.isCreated() ? "BudgetModifier created" : "BudgetModifier updated",
						Toast.LENGTH_SHORT).show();
				listener.onSuccess();
			} else {
				DialogUtils.getErrorDialog(context, e.getMessage()).show();
			}
		}
	}

	private static class BudgetModifierDeleteTask extends AsyncTask<Void, Void, Boolean> {

		public interface Listener {

			public void onDataDeleted();


			public void onNothingDeleted();
		}

		private Context		context;
		private Listener	listener;
		private int			id;


		private BudgetModifierDeleteTask(Context context, Listener listener, int id) {
			this.context = context;
			this.listener = listener;
			this.id = id;
		}


		@Override
		protected Boolean doInBackground(Void... params) {
			BudgetDbHelper helper = BudgetDbHelper.get();
			boolean deleted;
			try {
				Dao<BudgetModifier, Integer> dao = helper.getDao(BudgetModifier.class);
				deleted = dao.deleteById(id) > 0;
				context.getContentResolver().notifyChange(BudgetProvider.BUDGET_MODIFIERS_URI, null);
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			return deleted;
		}


		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				listener.onDataDeleted();
			} else {
				listener.onNothingDeleted();
			}
		}
	}
}
