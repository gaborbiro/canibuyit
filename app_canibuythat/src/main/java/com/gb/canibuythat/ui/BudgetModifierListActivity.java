package com.gb.canibuythat.ui;


import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BalanceCalculator;
import com.gb.canibuythat.model.BudgetModifier;
import com.gb.canibuythat.model.BudgetReading;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.util.DBUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.FileUtils;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;


/**
 * An activity representing a list of BudgetModifiers. This activity has different presentations for handset and
 * tablet-size devices. On handsets, the activity presents a list of items, which when touched, lead to a
 * {@link BudgetModifierDetailActivity} representing item details. On tablets, the activity presents the list of items
 * and item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a {@link BudgetModifierListFragment} and the item
 * details (if present) is a {@link BudgetModifierDetailFragment}.
 * <p/>
 * This activity also implements the required {@link BudgetModifierListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class BudgetModifierListActivity extends ActionBarActivity implements BudgetModifierListFragment.Callbacks {

	private static final SimpleDateFormat	READING_DATE_FORMAT	= new SimpleDateFormat("yyyy/MM/dd");
	private static final int				ACTION_CHOOSE_FILE	= 1;

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
	 */
	private boolean							twoPane;

	@InjectView(R.id.balance)
	TextView								balanceTV;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_budgetmodifier_list);
		ButterKnife.inject(this);

		if (findViewById(R.id.budgetmodifier_detail_container) != null) {
			twoPane = true;
			BudgetModifierListFragment budgetModifierListFragment = ((BudgetModifierListFragment) getSupportFragmentManager()
					.findFragmentById(R.id.budgetmodifier_list));
			budgetModifierListFragment.setActivateOnItemClick(true);
		}
		new CalculateBalanceTask().execute();
		// TODO: If exposing deep links into your app, handle intents here.
	}


	/**
	 * Callback method from {@link BudgetModifierListFragment.Callbacks} indicating that the item with the given ID was
	 * selected.
	 */
	@Override
	public void onItemSelected(int id) {
		showDetailScreen(id);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add:
			showDetailScreen(null);
			break;
		case R.id.menu_update_balance:
			new LastBudgetReadingLoaderTask().execute();
			break;
		case R.id.menu_export:
			DBUtils.exportDatabase(BudgetDbHelper.DATABASE_NAME);
			break;
		case R.id.menu_import:
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.setType("*/*");
			i.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(i, ACTION_CHOOSE_FILE);
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	private void showBudgetReadingDialog(final BudgetReading lastReading) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LinearLayout body = (LinearLayout) LayoutInflater.from(this)
				.inflate(R.layout.budget_reading_input_layout, null);
		TextView lastReadingTV = (TextView) body.findViewById(R.id.last_reading);
		final EditText valueET = (EditText) body.findViewById(R.id.value);

		if (lastReading != null) {
			lastReadingTV.setText(lastReading.value + " " + READING_DATE_FORMAT.format(lastReading.dateOfReading));
		} else {
			lastReadingTV.setText("None");
		}

		builder.setTitle("Enter current budget balance").setView(body)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (!TextUtils.isEmpty(valueET.getText())) {
							BudgetReading budgetReading = new BudgetReading();
							budgetReading.dateOfReading = new Date();
							budgetReading.value = Float.valueOf(valueET.getText().toString());
							new BudgetReadingWriterTask().execute(budgetReading);
						} else {
							Toast.makeText(BudgetModifierListActivity.this, "You didn't enter a value!",
									Toast.LENGTH_SHORT).show();
						}
					}
				}).setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_CHOOSE_FILE:
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				try {
					String path = FileUtils.getPath(uri);
					new DatabaseImportTask(path).execute();
				} catch (URISyntaxException e) {
					e.printStackTrace();
					Toast.makeText(this, "Error importing database", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	private void showDetailScreen(final Integer id) {
		if (twoPane) {
			final BudgetModifierDetailFragment budgetModifierDetailFragment = (BudgetModifierDetailFragment) getFragmentManager()
					.findFragmentById(R.id.budgetmodifier_detail_container);

			if (budgetModifierDetailFragment == null || !budgetModifierDetailFragment.isAdded()) {
				BudgetModifierDetailFragment newFragment = new BudgetModifierDetailFragment();

				if (id != null) {
					Bundle arguments = new Bundle();
					arguments.putInt(BudgetModifierDetailFragment.EXTRA_ITEM_ID, id);
					newFragment.setArguments(arguments);
				}
				getFragmentManager().beginTransaction().replace(R.id.budgetmodifier_detail_container, newFragment)
						.commit();
			} else {
				if (budgetModifierDetailFragment.isChanged()) {
					DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

						@Override
						public void run() {
							if (budgetModifierDetailFragment.saveUserData()) {
								budgetModifierDetailFragment.setContent(id, false);
							}
						}
					}, new Runnable() {

						@Override
						public void run() {
							budgetModifierDetailFragment.setContent(id, false);
						}
					}).show();
				} else {
					budgetModifierDetailFragment.setContent(id, false);
				}
			}
		} else {
			if (id != null) {
				startActivity(BudgetModifierDetailActivity.getIntentForUpdate(this, id));
			} else {
				startActivity(BudgetModifierDetailActivity.getIntentForCreate(this));
			}
		}
	}


	@Override
	public void onBackPressed() {
		final BudgetModifierDetailFragment detailFragment = (BudgetModifierDetailFragment) getFragmentManager()
				.findFragmentById(R.id.budgetmodifier_detail_container);
		if (detailFragment != null && detailFragment.isChanged()) {
			DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

				@Override
				public void run() {
					if (detailFragment.saveUserData()) {
						finish();
					}
				}
			}, new Runnable() {

				@Override
				public void run() {
					finish();
				}
			}).show();
		} else {
			super.onBackPressed();
		}
	}

	public class CalculateBalanceTask extends AsyncTask<Void, Void, Float[]> {

		@Override
		protected Float[] doInBackground(Void... params) {
			BudgetDbHelper helper = BudgetDbHelper.get();
			float minimum = 0;
			float maximum = 0;

			try {
				Dao<BudgetReading, Integer> budgetReadingDao = helper.getDao(BudgetReading.class);
				QueryBuilder<BudgetReading, Integer> budgetReadingQBuilder = budgetReadingDao.queryBuilder();
				budgetReadingQBuilder.orderBy(Contract.BudgetReading.DATE, false); // false for descending order
				budgetReadingQBuilder.limit(1L);
				List<BudgetReading> listOfOne = budgetReadingQBuilder.query();
				BudgetReading lastBudgetReading = null;

				if (listOfOne != null && !listOfOne.isEmpty()) {
					lastBudgetReading = listOfOne.get(0);
				}

				Dao<BudgetModifier, Integer> budgetModifierDao = helper.getDao(BudgetModifier.class);

				for (BudgetModifier bm : budgetModifierDao) {
					float[] temp = new BalanceCalculator(bm).getEstimatedBalance(bm,
							lastBudgetReading != null ? lastBudgetReading.dateOfReading : null);
					minimum += temp[0];
					maximum += temp[1];
				}
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			return new Float[] {
					minimum, maximum
			};
		}


		@Override
		protected void onPostExecute(Float[] balance) {
			balanceTV.setText(Float.toString(balance[0]) + "/" + Float.toString(balance[1]));
		}
	}

	public class DatabaseImportTask extends AsyncTask<Void, Void, Void> {

		private String	path;


		public DatabaseImportTask(String path) {
			this.path = path;
		}


		@Override
		protected Void doInBackground(Void... params) {
			DBUtils.importDatabase(new File(path), Contract.BudgetModifier.TABLE, Contract.BudgetModifier.COLUMNS,
					new BudgetDbHelper(BudgetModifierListActivity.this));
			getContentResolver().notifyChange(BudgetProvider.BUDGET_MODIFIERS_URI, null);
			new CalculateBalanceTask().execute();
			return null;
		}
	}

	private class LastBudgetReadingLoaderTask extends AsyncTask<Void, Void, BudgetReading> {

		@Override
		protected BudgetReading doInBackground(Void... params) {
			BudgetDbHelper helper = BudgetDbHelper.get();
			try {
				Dao<BudgetReading, Integer> dao = helper.getDao(BudgetReading.class);
				QueryBuilder<BudgetReading, Integer> qBuilder = dao.queryBuilder();
				qBuilder.orderBy(Contract.BudgetReading.DATE, false); // false for descending order
				qBuilder.limit(1L);
				List<BudgetReading> listOfOne = qBuilder.query();

				if (listOfOne != null && !listOfOne.isEmpty()) {
					return listOfOne.get(0);
				} else {
					return null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		}


		@Override
		protected void onPostExecute(BudgetReading budgetReading) {
			showBudgetReadingDialog(budgetReading);
		}
	}

	public class BudgetReadingWriterTask extends AsyncTask<BudgetReading, Void, Void> {

		@Override
		protected Void doInBackground(BudgetReading... params) {
			BudgetDbHelper helper = BudgetDbHelper.get();

			try {
				Dao<BudgetReading, Integer> dao = helper.getDao(BudgetReading.class);

				for (BudgetReading budgetReading : params) {
					dao.create(budgetReading);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}
