package com.gb.canibuythat.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BalanceUpdateEvent;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.util.DBUtils;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.FileUtils;
import com.gb.canibuythat.util.ViewUtils;
import com.j256.ormlite.dao.Dao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * An activity representing a list of BudgetModifiers. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link BudgetItemDetailActivity} representing item details. On tablets, the activity
 * presents the list of items and item details side-by-side using two vertical panes. <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link BudgetItemListFragment} and the item details (if present) is a {@link BudgetItemDetailFragment}. <p/>
 * This activity also implements the required {@link BudgetItemListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class BudgetItemListActivity extends ActionBarActivity
        implements BudgetItemListFragment.Callbacks,
        BalanceUpdateInputDialog.BalanceUpdateInputListener {

    private static final int ACTION_CHOOSE_FILE = 1;

    @InjectView(R.id.balance) TextView mBalanceView;
    @InjectView(R.id.reference) TextView mReferenceView;
    @InjectView(R.id.chart_button) ImageView mChartButton;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean twoPane;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_item_list);
        ButterKnife.inject(this);
        EventBus.getDefault()
                .register(this);

        if (findViewById(R.id.budgetmodifier_detail_container) != null) {
            twoPane = true;
            BudgetItemListFragment budgetItemListFragment =
                    ((BudgetItemListFragment) getSupportFragmentManager()
                            .findFragmentById(
                            R.id.budgetmodifier_list));
            budgetItemListFragment.setActivateOnItemClick(true);
        }
        ChartActivity.launchOnClick(this, mChartButton);
        //        BalanceUpdateInputDialog.launchOnClick(getSupportFragmentManager(),
        //                mReferenceView);
        new CalculateBalanceTask().execute();
    }

    /**
     * Callback method from {@link BudgetItemListFragment.Callbacks} indicating that the
     * item with the given ID was
     * selected.
     */
    @Override public void onItemSelected(int id) {
        showDetailScreen(id);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                showDetailScreen(null);
                break;
            case R.id.menu_update_balance:
                showBalanceUpdateDialog();
                break;
            case R.id.menu_export:
                DBUtils.exportDatabase(BudgetDbHelper.DATABASE_NAME);
                break;
            case R.id.menu_import:
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                        .getPath() + "/CanIBuyThat/");
                i.setDataAndType(uri, "*/sqlite");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(i, "Select file"),
                        ACTION_CHOOSE_FILE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe public void onEvent(BudgetItemUpdatedEvent event) {
        new CalculateBalanceTask().execute();
    }

    private void showBalanceUpdateDialog() {
        new BalanceUpdateInputDialog().show(getSupportFragmentManager(), null);
    }

    @Override public void onBalanceUpdateSet(BalanceUpdateEvent event) {
        new BalanceUpdateWriterTask().execute(event);
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
                        Toast.makeText(this, "Error importing database",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDetailScreen(final Integer budgetItemId) {
        if (twoPane) {
            final BudgetItemDetailFragment budgetItemDetailFragment =
                    (BudgetItemDetailFragment) getFragmentManager().findFragmentById(
                            R.id.budgetmodifier_detail_container);

            if (budgetItemDetailFragment == null || !budgetItemDetailFragment.isAdded()) {
                BudgetItemDetailFragment detailFragment = new BudgetItemDetailFragment();

                if (budgetItemId != null) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(BudgetItemDetailFragment.EXTRA_ITEM_ID,
                            budgetItemId);
                    detailFragment.setArguments(arguments);
                }
                getFragmentManager().beginTransaction()
                        .replace(R.id.budgetmodifier_detail_container, detailFragment)
                        .commit();
            } else {
                if (budgetItemDetailFragment.isChanged()) {
                    DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

                        @Override public void run() {
                            if (budgetItemDetailFragment.saveUserDataOrShowError()) {
                                budgetItemDetailFragment.setContent(budgetItemId, false);
                            }
                        }
                    }, new Runnable() {

                        @Override public void run() {
                            budgetItemDetailFragment.setContent(budgetItemId, false);
                        }
                    })
                            .show();
                } else {
                    budgetItemDetailFragment.setContent(budgetItemId, false);
                }
            }
        } else {
            if (budgetItemId != null) {
                startActivity(
                        BudgetItemDetailActivity.getIntentForUpdate(this, budgetItemId));
            } else {
                startActivity(BudgetItemDetailActivity.getIntentForCreate(this));
            }
        }
    }

    @Override public void onBackPressed() {
        final BudgetItemDetailFragment detailFragment =
                (BudgetItemDetailFragment) getFragmentManager().findFragmentById(
                        R.id.budgetmodifier_detail_container);
        if (detailFragment != null && detailFragment.isChanged()) {
            DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

                @Override public void run() {
                    if (detailFragment.saveUserDataOrShowError()) {
                        finish();
                    }
                }
            }, new Runnable() {

                @Override public void run() {
                    finish();
                }
            })
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private class CalculateBalanceTask extends AsyncTask<Void, Void, Float[]> {

        private LastBalanceUpdateLoaderTask mLastBalanceUpdateLoaderTask;

        @Override protected void onPreExecute() {
            mLastBalanceUpdateLoaderTask = new LastBalanceUpdateLoaderTask();
            mLastBalanceUpdateLoaderTask.execute();
        }

        @Override protected Float[] doInBackground(Void... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();
            float bestCase = 0;
            float worstCase = 0;

            try {
                BalanceUpdateEvent balanceUpdateEvent =
                        mLastBalanceUpdateLoaderTask.get();
                Dao<com.gb.canibuythat.model.BudgetItem, Integer> budgetItemDao =
                        helper.getDao(com.gb.canibuythat.model.BudgetItem.class);

                for (com.gb.canibuythat.model.BudgetItem item : budgetItemDao) {
                    if (item.mEnabled) {
                        BalanceCalculator.BalanceResult result = BalanceCalculator.get()
                                .getEstimatedBalance(item,
                                        balanceUpdateEvent != null ? balanceUpdateEvent.when
                                                                   : null, new Date());
                        bestCase += result.bestCase;
                        worstCase += result.worstCase;
                    }
                }

                if (balanceUpdateEvent != null) {
                    bestCase += balanceUpdateEvent.value;
                    worstCase += balanceUpdateEvent.value;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(BudgetItemListActivity.this,
                        "Error calculating balance. Check stack trace.",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            return new Float[]{bestCase, worstCase};
        }

        @Override protected void onPostExecute(Float[] balance) {
            BalanceUpdateEvent balanceUpdateEvent = null;
            try {
                balanceUpdateEvent = mLastBalanceUpdateLoaderTask.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            String change = getString(R.string.change);
            String text;
            if (balanceUpdateEvent != null) {
                text = getString(R.string.reference_statistics, balanceUpdateEvent.value,
                        DateUtils.DEFAULT_DATE_FORMAT.format(
                                balanceUpdateEvent.when), change);
            } else {
                text = getString(R.string.reference_statistics_none, change);
            }
            ViewUtils.setTextWithLinkSegment(mReferenceView, text, change, new Runnable()
            {
                @Override public void run() {
                    showBalanceUpdateDialog();
                }
            });

            mBalanceView.setText(getString(R.string.balance, balance[0], balance[1]));
        }
    }

    public class DatabaseImportTask extends AsyncTask<Void, Void, Void> {

        private String path;

        public DatabaseImportTask(String path) {
            this.path = path;
        }

        @Override protected Void doInBackground(Void... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();
            DBUtils.importDatabase(new File(path), Contract.BudgetItem.TABLE,
                    Contract.BudgetItem.COLUMNS, helper);
            getContentResolver().notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
            DBUtils.importDatabase(new File(path), Contract.BalanceUpdateEvent.TABLE,
                    Contract.BalanceUpdateEvent.COLUMNS, helper);
            getContentResolver().notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
            return null;
        }

        @Override protected void onPostExecute(Void aVoid) {
            new CalculateBalanceTask().execute();
        }
    }

    public class BalanceUpdateWriterTask
            extends AsyncTask<BalanceUpdateEvent, Void, Void> {

        @Override protected Void doInBackground(BalanceUpdateEvent... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();

            try {
                Dao<BalanceUpdateEvent, Integer> dao =
                        helper.getDao(BalanceUpdateEvent.class);

                for (BalanceUpdateEvent balanceUpdateEvent : params) {
                    dao.create(balanceUpdateEvent);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override protected void onPostExecute(Void aVoid) {
            new CalculateBalanceTask().execute();
        }
    }
}
