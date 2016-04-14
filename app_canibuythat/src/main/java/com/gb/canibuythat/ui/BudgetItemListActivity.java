package com.gb.canibuythat.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.util.DBUtils;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.DialogUtils;
import com.gb.canibuythat.util.FileUtils;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
        implements BudgetItemListFragment.Callbacks {

    private static final int ACTION_CHOOSE_FILE = 1;
    @InjectView(R.id.balance) TextView balanceTV;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean twoPane;
    private DatePickerDialog mBalanceUpdateWhenPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_item_list);
        ButterKnife.inject(this);

        if (findViewById(R.id.budgetmodifier_detail_container) != null) {
            twoPane = true;
            BudgetItemListFragment budgetItemListFragment =
                    ((BudgetItemListFragment) getSupportFragmentManager()
                            .findFragmentById(
                            R.id.budgetmodifier_list));
            budgetItemListFragment.setActivateOnItemClick(true);
        }
        new CalculateBalanceTask().execute();
        // TODO: If exposing deep links into your app, handle intents here.
    }

    /**
     * Callback method from {@link BudgetItemListFragment.Callbacks} indicating that the
     * item with the given ID was
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

    private void showBalanceUpdateDialog(final BalanceUpdateEvent lastUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout body = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.balance_update_input_layout, null);
        TextView lastUpdateView = (TextView) body.findViewById(R.id.last_update);
        final EditText valueView = (EditText) body.findViewById(R.id.value);
        final Button whenButton = (Button) body.findViewById(R.id.when);

        whenButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getDatePickerDialog(whenButton).show();
            }
        });

        if (lastUpdate != null) {
            lastUpdateView.setText(lastUpdate.value + " @" +
                    DateUtils.SHORT_DATE_FORMAT.format(lastUpdate.when));
        } else {
            lastUpdateView.setText("None");
        }
        whenButton.setText(DateUtils.SHORT_DATE_FORMAT.format(new Date()));
        builder.setTitle("Set starting balance")
                .setView(body)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!TextUtils.isEmpty(valueView.getText())) {
                                    BalanceUpdateEvent balanceUpdateEvent =
                                            new BalanceUpdateEvent();

                                    if (mBalanceUpdateWhenPicker != null) {
                                        balanceUpdateEvent.when =
                                                DateUtils.getDayFromDatePicker(
                                                        mBalanceUpdateWhenPicker
                                                                .getDatePicker());
                                    } else {
                                        balanceUpdateEvent.when = new Date();
                                    }
                                    balanceUpdateEvent.value = Float.valueOf(
                                            valueView.getText()
                                                    .toString());
                                    new BudgetUpdateWriterTask().execute(
                                            balanceUpdateEvent);
                                } else {
                                    Toast.makeText(BudgetItemListActivity.this,
                                            "You didn't enter a value!",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null);
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
                BudgetItemDetailFragment newFragment = new BudgetItemDetailFragment();

                if (budgetItemId != null) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(BudgetItemDetailFragment.EXTRA_ITEM_ID,
                            budgetItemId);
                    newFragment.setArguments(arguments);
                }
                getFragmentManager().beginTransaction()
                        .replace(R.id.budgetmodifier_detail_container, newFragment)
                        .commit();
            } else {
                if (budgetItemDetailFragment.isChanged()) {
                    DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

                        @Override
                        public void run() {
                            if (budgetItemDetailFragment.saveUserData()) {
                                budgetItemDetailFragment.setContent(budgetItemId, false);
                            }
                        }
                    }, new Runnable() {

                        @Override
                        public void run() {
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

    @Override
    public void onBackPressed() {
        final BudgetItemDetailFragment detailFragment =
                (BudgetItemDetailFragment) getFragmentManager().findFragmentById(
                        R.id.budgetmodifier_detail_container);
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
            })
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private DatePickerDialog getDatePickerDialog(TextView targetView) {
        if (mBalanceUpdateWhenPicker == null) {
            Calendar c = Calendar.getInstance();
            mBalanceUpdateWhenPicker =
                    new DatePickerDialog(this, new DatePickerListener(targetView),
                            c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH));
            mBalanceUpdateWhenPicker.getDatePicker()
                    .setTag(R.id.first_occurence_end);
        }
        return mBalanceUpdateWhenPicker;
    }

    private class DatePickerListener implements DatePickerDialog.OnDateSetListener {

        private TextView targetView;

        public DatePickerListener(TextView targetView) {
            this.targetView = targetView;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                int dayOfMonth) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, monthOfYear);
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            targetView.setText(DateUtils.SHORT_DATE_FORMAT.format(c.getTime()));
        }
    }

    ;

    public class CalculateBalanceTask extends AsyncTask<Void, Void, Float[]> {

        @Override
        protected Float[] doInBackground(Void... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();
            float minimum = 0;
            float maximum = 0;

            try {
                Dao<BalanceUpdateEvent, Integer> balanceUpdateDao =
                        helper.getDao(BalanceUpdateEvent.class);
                QueryBuilder<BalanceUpdateEvent, Integer> balanceUpdateQBuilder =
                        balanceUpdateDao.queryBuilder();
                balanceUpdateQBuilder.orderBy(Contract.BalanceUpdateEvent.WHEN,
                        false); // false for descending order
                balanceUpdateQBuilder.limit(1L);
                List<BalanceUpdateEvent> listOfOne = balanceUpdateQBuilder.query();
                BalanceUpdateEvent lastBalanceUpdateEvent = null;

                if (listOfOne != null && !listOfOne.isEmpty()) {
                    lastBalanceUpdateEvent = listOfOne.get(0);
                }

                Dao<com.gb.canibuythat.model.BudgetItem, Integer> budgetItemDao =
                        helper.getDao(com.gb.canibuythat.model.BudgetItem.class);

                for (com.gb.canibuythat.model.BudgetItem bi : budgetItemDao) {
                    float[] temp = new BalanceCalculator().getEstimatedBalance(bi,
                            lastBalanceUpdateEvent != null ? lastBalanceUpdateEvent.when
                                                           : null);
                    minimum += temp[0];
                    maximum += temp[1];
                }

                if (lastBalanceUpdateEvent != null) {
                    minimum += lastBalanceUpdateEvent.value;
                    maximum += lastBalanceUpdateEvent.value;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return new Float[]{minimum, maximum};
        }

        @Override
        protected void onPostExecute(Float[] balance) {
            balanceTV.setText(
                    Float.toString(balance[0]) + "/" + Float.toString(balance[1]));
        }
    }

    public class DatabaseImportTask extends AsyncTask<Void, Void, Void> {

        private String path;

        public DatabaseImportTask(String path) {
            this.path = path;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DBUtils.importDatabase(new File(path), Contract.BudgetItem.TABLE,
                    Contract.BudgetItem.COLUMNS,
                    new BudgetDbHelper(BudgetItemListActivity.this));
            getContentResolver().notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new CalculateBalanceTask().execute();
        }
    }

    private class LastBudgetReadingLoaderTask
            extends AsyncTask<Void, Void, BalanceUpdateEvent> {

        @Override
        protected BalanceUpdateEvent doInBackground(Void... params) {
            BudgetDbHelper helper = BudgetDbHelper.get();
            try {
                Dao<BalanceUpdateEvent, Integer> dao =
                        helper.getDao(BalanceUpdateEvent.class);
                QueryBuilder<BalanceUpdateEvent, Integer> qBuilder = dao.queryBuilder();
                qBuilder.orderBy(Contract.BalanceUpdateEvent.WHEN,
                        false); // false for descending order
                qBuilder.limit(1L);
                List<BalanceUpdateEvent> listOfOne = qBuilder.query();

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
        protected void onPostExecute(BalanceUpdateEvent balanceUpdateEvent) {
            showBalanceUpdateDialog(balanceUpdateEvent);
        }
    }

    public class BudgetUpdateWriterTask
            extends AsyncTask<BalanceUpdateEvent, Void, Void> {

        @Override
        protected Void doInBackground(BalanceUpdateEvent... params) {
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

        @Override
        protected void onPostExecute(Void aVoid) {
            new CalculateBalanceTask().execute();
        }
    }
}
