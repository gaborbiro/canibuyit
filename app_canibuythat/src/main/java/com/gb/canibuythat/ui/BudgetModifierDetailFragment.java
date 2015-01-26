package com.gb.canibuythat.ui;


import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.gb.canibuythat.App;
import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BudgetModifier;
import com.gb.canibuythat.provider.BudgetModifierDbHelper;
import com.gb.canibuythat.provider.BudgetModifierProvider;
import com.gb.canibuythat.util.ViewUtils;
import com.j256.ormlite.dao.Dao;


/**
 * A fragment representing a single BudgetModifier detail screen. This fragment is either contained in a
 * {@link BudgetModifierListActivity} in two-pane mode (on tablets) or a {@link BudgetModifierDetailActivity} on
 * handsets.
 */
public class BudgetModifierDetailFragment extends Fragment implements View.OnClickListener,
		DatePickerDialog.OnDateSetListener {

	private static final SimpleDateFormat	SPINNER_DATE_FORMAT	= new SimpleDateFormat("MMM. dd");

	public static final String				EXTRA_ITEM_ID		= "item_id";

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
	private View							rootView;


	public BudgetModifierDetailFragment() {
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_budgetmodifier_detail, container, false);
		ButterKnife.inject(this, rootView);

		typeSpinner.setAdapter(new ArrayAdapter<BudgetModifier.BudgetModifierType>(getActivity(),
				android.R.layout.simple_list_item_1, BudgetModifier.BudgetModifierType.values()) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);

				if (position == BudgetModifier.BudgetModifierType.UNKNOWN.ordinal()) {
					((TextView) view).setText("Choose type");
				}
				return view;
			}
		});
		periodTypeSpinner.setAdapter(new ArrayAdapter<BudgetModifier.PeriodType>(getActivity(),
				android.R.layout.simple_list_item_1, BudgetModifier.PeriodType.values()) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);

				if (position == BudgetModifier.PeriodType.UNKNOWN.ordinal()) {
					((TextView) view).setText("Choose repetition period");
				}
				return view;
			}
		});
		lowerDateET.setOnClickListener(this);
		upperDateET.setOnClickListener(this);

		if (savedInstanceState == null && getArguments() != null && getArguments().containsKey(EXTRA_ITEM_ID)) {
			swapContent(getArguments().getInt(EXTRA_ITEM_ID), true);
		} else {
			ViewUtils.showKeyboard(titleET);
		}
		return rootView;
	}


	public void swapContent(Integer id, final boolean showKeyboardWhenDone) {
		if (id != null) {
			new BudgetModifierLoaderTask(getActivity(), new BudgetModifierLoaderTask.Listener() {

				@Override
				public void onDataReady(BudgetModifier budgetModifier) {
					BudgetModifierDetailFragment.this.budgetModifier = budgetModifier;
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
	 * @return true if saving the BudgetModifier was a success, false otherwise
	 */
	public boolean saveUserData() {
		if (validate()) {
			BudgetModifier newBudgetModifier = gatherData();

			if (budgetModifier != null) {
				newBudgetModifier.id = budgetModifier.id;
			}
			new BudgetModifierUpdateTask(getActivity(), budgetModifier).execute();
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
			typeSpinner.setSelection(budgetModifier.type.ordinal());

			if (budgetModifier.lowerDate != null) {
				lowerDateET.setText(SPINNER_DATE_FORMAT.format(budgetModifier.lowerDate));
			} else {
				lowerDateET.setText(null);
			}

			if (budgetModifier.upperDate != null) {
				upperDateET.setText(SPINNER_DATE_FORMAT.format(budgetModifier.upperDate));
			} else {
				upperDateET.setText(null);
			}

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
			periodTypeSpinner.setSelection(budgetModifier.periodType.ordinal());
			notesET.setText(budgetModifier.notes);
		} else {
			titleET.setText(null);
			amountET.setText(null);
			typeSpinner.setSelection(0);
			lowerDateET.setText(SPINNER_DATE_FORMAT.format(new Date()));
			upperDateET.setText(SPINNER_DATE_FORMAT.format(new Date()));
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

		Calendar c = Calendar.getInstance();

		if (lowerDatePickerDialog != null) {
			DatePicker datePicker = lowerDatePickerDialog.getDatePicker();
			c.set(Calendar.YEAR, datePicker.getYear());
			c.set(Calendar.MONTH, datePicker.getMonth());
			c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
		} else if (budgetModifier == null || budgetModifier.lowerDate == null) {
			lowerDateET.setError("Select a date");
			lowerDateET.requestFocus();
			return false;
		}
		Date lowerDate = c.getTime();
		// upperDate
		c = Calendar.getInstance();

		if (upperDatePickerDialog != null) {
			DatePicker datePicker = upperDatePickerDialog.getDatePicker();
			c.set(Calendar.YEAR, datePicker.getYear());
			c.set(Calendar.MONTH, datePicker.getMonth());
			c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
		} else if (budgetModifier == null || budgetModifier.upperDate == null) {
			upperDateET.setError("Select a date");
			upperDateET.requestFocus();
			return false;
		}

		Date upperDate = c.getTime();

		if (lowerDate.after(upperDate)) {
			upperDateET.setError("First date must not be after the second");
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
		if (typeSpinner.getSelectedItem() != BudgetModifier.BudgetModifierType.UNKNOWN) {
			budgetModifier.type = (BudgetModifier.BudgetModifierType) typeSpinner.getSelectedItem();
		}
		// lowerDate
		if (lowerDatePickerDialog != null) {
			DatePicker datePicker = lowerDatePickerDialog.getDatePicker();
			Calendar c = Calendar.getInstance();
			c.set(Calendar.YEAR, datePicker.getYear());
			c.set(Calendar.MONTH, datePicker.getMonth());
			c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
			budgetModifier.lowerDate = c.getTime();
		} else if (this.budgetModifier != null) {
			budgetModifier.lowerDate = this.budgetModifier.lowerDate;
		}
		// upperDate
		if (upperDatePickerDialog != null) {
			DatePicker datePicker = upperDatePickerDialog.getDatePicker();
			Calendar c = Calendar.getInstance();
			c.set(Calendar.YEAR, datePicker.getYear());
			c.set(Calendar.MONTH, datePicker.getMonth());
			c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
			budgetModifier.upperDate = c.getTime();
		} else if (this.budgetModifier != null) {
			budgetModifier.upperDate = this.budgetModifier.upperDate;
		}
		// repetition
		if (!TextUtils.isEmpty(repetitionCountET.getText())) {
			budgetModifier.repetitionCount = Integer.valueOf(repetitionCountET.getText().toString());
		}
		// periodMultiplier
		if (!TextUtils.isEmpty(periodMultiplierET.getText())) {
			budgetModifier.periodMultiplier = Integer.valueOf(periodMultiplierET.getText().toString());
		} else {
			budgetModifier.periodMultiplier = 1;
		}
		// period
		if (periodTypeSpinner.getSelectedItem() != BudgetModifier.PeriodType.UNKNOWN) {
			budgetModifier.periodType = (BudgetModifier.PeriodType) periodTypeSpinner.getSelectedItem();
		}
		// notes
		if (!TextUtils.isEmpty(notesET.getText())) {
			budgetModifier.notes = notesET.getText().toString();
		}
		return budgetModifier;
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.lower_date:
			getLowerDatePickerDialog().show();
			upperDateET.setError(null);
			break;
		case R.id.upper_date:
			getUpperDatePickerDialog().show();
			upperDateET.setError(null);
			break;
		}
	}


	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		Calendar c = Calendar.getInstance();
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

	private static class BudgetModifierLoaderTask extends AsyncTask<Void, Void, BudgetModifier> {

		public interface Listener {

			public void onDataReady(BudgetModifier budgetModifier);
		}

		private Context		context;
		private Listener	listener;
		private int			id;


		private BudgetModifierLoaderTask(Context context, Listener listener, int id) {
			this.context = context;
			this.listener = listener;
			this.id = id;
		}


		@Override
		protected BudgetModifier doInBackground(Void... params) {
			BudgetModifierDbHelper helper = new BudgetModifierDbHelper(context);
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

		private Context						context;
		private BudgetModifier				budgetModifier;
		private Dao.CreateOrUpdateStatus	result;


		private BudgetModifierUpdateTask(Context context, BudgetModifier budgetModifier) {
			this.context = context;
			this.budgetModifier = budgetModifier;
		}


		@Override
		protected Void doInBackground(Void... params) {
			BudgetModifierDbHelper helper = new BudgetModifierDbHelper(context);
			try {
				Dao<BudgetModifier, Integer> dao = helper.getDao(BudgetModifier.class);
				result = dao.createOrUpdate(budgetModifier);
				context.getContentResolver().notifyChange(BudgetModifierProvider.BUDGET_MODIFIERS_URI, null);
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			return null;
		}


		@Override
		protected void onPostExecute(Void aVoid) {
			Toast.makeText(context, result.isCreated() ? "BudgetModifier created" : "BudgetModifier updated",
					Toast.LENGTH_SHORT).show();
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
			BudgetModifierDbHelper helper = new BudgetModifierDbHelper(context);
			boolean deleted;
			try {
				Dao<BudgetModifier, Integer> dao = helper.getDao(BudgetModifier.class);
				deleted = dao.deleteById(id) > 0;
				context.getContentResolver().notifyChange(BudgetModifierProvider.BUDGET_MODIFIERS_URI, null);
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


	/**
	 * 
	 * @return true if the content differs relative to the original BudgetModifier that was loaded, or the content
	 *         hasn't been yet saved
	 */
	public boolean isChanged() {
		return (budgetModifier == null && !new BudgetModifier().equals(gatherData()))
				|| (budgetModifier != null && !budgetModifier.equals(gatherData()));
	}
}
