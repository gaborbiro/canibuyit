package com.gb.canibuythat.ui;


import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BudgetModifier;
import com.gb.canibuythat.provider.BudgetModifierDbHelper;
import com.gb.canibuythat.provider.BudgetModifierProvider;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.util.DBUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.FileUtils;
import com.j256.ormlite.dao.Dao;


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

	private static final int	ACTION_CHOOSE_FILE	= 1;

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
	 */
	private boolean				twoPane;

	@InjectView(R.id.balance)
	TextView					balanceTV;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_budgetmodifier_list);
		ButterKnife.inject(this);

		if (findViewById(R.id.budgetmodifier_detail_container) != null) {
			twoPane = true;
			((BudgetModifierListFragment) getSupportFragmentManager().findFragmentById(R.id.budgetmodifier_list))
					.setActivateOnItemClick(true);
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
		case R.id.menu_export:
			DBUtils.exportDatabase(BudgetModifierDbHelper.DATABASE_NAME);
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
			final BudgetModifierDetailFragment fragment = (BudgetModifierDetailFragment) getFragmentManager()
					.findFragmentById(R.id.budgetmodifier_detail_container);

			if (fragment == null || !fragment.isAdded()) {
				BudgetModifierDetailFragment newFragment = new BudgetModifierDetailFragment();

				if (id != null) {
					Bundle arguments = new Bundle();
					arguments.putInt(BudgetModifierDetailFragment.EXTRA_ITEM_ID, id);
					newFragment.setArguments(arguments);
				}
				getFragmentManager().beginTransaction().replace(R.id.budgetmodifier_detail_container, newFragment)
						.commit();
			} else {
				if (fragment.isChanged()) {
					DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

						@Override
						public void run() {
							if (fragment.saveUserData()) {
								fragment.swapContent(id, false);
							}
						}
					}, new Runnable() {

						@Override
						public void run() {
							fragment.swapContent(id, false);
						}
					}).show();
				} else {
					fragment.swapContent(id, false);
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

	public class CalculateBalanceTask extends AsyncTask<Void, Void, Float> {

		@Override
		protected Float doInBackground(Void... params) {
			BudgetModifierDbHelper helper = BudgetModifierDbHelper.get();
			float balance = 0;

			try {
				Dao<BudgetModifier, Integer> dao = helper.getDao(BudgetModifier.class);

				for (BudgetModifier bm : dao) {
					balance += getEstimatedBalance(bm);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			return balance;
		}


		private float getEstimatedBalance(BudgetModifier budgetModifier) {
			return budgetModifier.amount;
		}


		@Override
		protected void onPostExecute(Float balance) {
			balanceTV.setText(Float.toString(balance));
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
					new BudgetModifierDbHelper(BudgetModifierListActivity.this));
			getContentResolver().notifyChange(BudgetModifierProvider.BUDGET_MODIFIERS_URI, null);
			return null;
		}
	}
}
