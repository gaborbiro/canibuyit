package com.gb.canibuythat.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.App;
import com.gb.canibuythat.R;
import com.gb.canibuythat.model.BalanceUpdateEvent;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.ui.task.Callback;
import com.gb.canibuythat.ui.task.backup.DatabaseExportTask;
import com.gb.canibuythat.ui.task.backup.DatabaseImportTask;
import com.gb.canibuythat.ui.task.balance_update.BalanceUpdateWriterTask;
import com.gb.canibuythat.ui.task.balance_update.CalculateBalanceTask;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.PermissionVerifier;
import com.gb.canibuythat.util.ViewUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/**
 * An activity representing a list of BudgetModifiers. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link BudgetItemEditorActivity} representing item details. On tablets, the activity
 * presents the list of items and item details side-by-side using two vertical panes. <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link BudgetItemListFragment} and the item details (if present) is a {@link BudgetItemEditorFragment}. <p/>
 * This activity also implements the required
 * {@link BudgetItemListFragment.FragmentCallback}
 * interface to listen for item selections.
 */
public class BudgetItemListActivity extends ActionBarActivity
        implements BudgetItemListFragment.FragmentCallback,
        BalanceUpdateInputDialog.BalanceUpdateInputListener {

    private static final int REQUEST_CODE_CHOOSE_FILE = 1;
    private static final int REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT = 2;

    @Optional @InjectView(R.id.balance) TextView mBalanceView;
    @Optional @InjectView(R.id.reference) TextView mReferenceView;
    @Optional @InjectView(R.id.chart_button) ImageView mChartButton;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean twoPane;

    private PermissionVerifier mPermissionVerifier;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_item_list);
        ButterKnife.inject(this);
        EventBus.getDefault()
                .register(this);

        if (findViewById(R.id.budgetmodifier_detail_container) != null) {
            twoPane = true;
        }
        if (mChartButton != null) {
            ChartActivity.launchOnClick(this, mChartButton);
        }
        new CalculateBalanceTask(mBalanceCalculatorCallback).execute();
    }

    /**
     * Callback method from {@link BudgetItemListFragment.FragmentCallback} indicating
     * that the budget item with the given database ID was selected.
     */
    @Override public void onListItemClick(int id) {
        showEditorScreen(id);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                showEditorScreen();
                break;
            case R.id.menu_update_balance:
                showBalanceUpdateDialog();
                break;
            case R.id.menu_export:
                mPermissionVerifier = new PermissionVerifier(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
                if (mPermissionVerifier.verifyPermissions(true,
                        REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT)) {
                    new DatabaseExportTask(BudgetDbHelper.DATABASE_NAME) {
                        @Override protected void onPostExecute(String result) {
                            Toast.makeText(App.getAppContext(), result,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }.execute();
                }
                break;
            case R.id.menu_import:
                importDatabase();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        final BudgetItemEditorFragment detailFragment =
                (BudgetItemEditorFragment) getFragmentManager().findFragmentById(
                        R.id.budgetmodifier_detail_container);
        if (detailFragment != null) {
            detailFragment.saveAndRun(new Runnable() {
                @Override public void run() {
                    BudgetItemListActivity.super.onBackPressed();
                }
            });
        } else {
            BudgetItemListActivity.super.onBackPressed();
        }
    }

    private void importDatabase() {
        Intent i = new Intent(this, FileDialogActivity.class);
        i.putExtra(FileDialogActivity.EXTRA_START_PATH,
                Environment.getExternalStorageDirectory()
                        .getPath() + "/CanIBuyThat/");
        i.putExtra(FileDialogActivity.EXTRA_SELECTION_MODE,
                FileDialogActivity.SELECTION_MODE_OPEN);
        startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE);
    }

    @Subscribe public void onEvent(BudgetItemUpdatedEvent event) {
        new CalculateBalanceTask(mBalanceCalculatorCallback).execute();
    }

    /**
     * Callback from the {@link BalanceUpdateInputDialog} notifying that the user has
     * added a new balance reading
     */
    @Override public void onBalanceUpdateSet(BalanceUpdateEvent event) {
        new BalanceUpdateWriterTask() {
            @Override protected void onPostExecute(Void aVoid) {
                new CalculateBalanceTask(mBalanceCalculatorCallback).execute();
            }
        }.execute(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHOOSE_FILE:
                if (resultCode == RESULT_OK) {
                    String path =
                            data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH);
                    new DatabaseImportTask(path) {
                        @Override protected void onPostExecute(Void aVoid) {
                            new CalculateBalanceTask(
                                    mBalanceCalculatorCallback).execute();
                        }
                    }.execute();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT) {
            if (mPermissionVerifier.onRequestPermissionsResult(requestCode, permissions,
                    grantResults)) {
                new DatabaseExportTask(BudgetDbHelper.DATABASE_NAME) {
                    @Override protected void onPostExecute(String result) {
                        Toast.makeText(App.getAppContext(), result,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }.execute();
            } else {
                Toast.makeText(this, "Missing permissions!", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void showBalanceUpdateDialog() {
        new BalanceUpdateInputDialog().show(getSupportFragmentManager(), null);
    }

    /**
     * Display an empty budget item editor screen
     */
    private void showEditorScreen() {
        showEditorScreen(null);
    }

    /**
     * Open the specified budget item for editing
     */
    private void showEditorScreen(final Integer budgetItemId) {
        if (twoPane) {
            final BudgetItemEditorFragment budgetItemEditorFragment =
                    (BudgetItemEditorFragment) getFragmentManager().findFragmentById(
                            R.id.budgetmodifier_detail_container);

            if (budgetItemEditorFragment == null || !budgetItemEditorFragment.isAdded()) {
                BudgetItemEditorFragment detailFragment = new BudgetItemEditorFragment();

                if (budgetItemId != null) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(BudgetItemEditorFragment.EXTRA_ITEM_ID,
                            budgetItemId);
                    detailFragment.setArguments(arguments);
                }
                getFragmentManager().beginTransaction()
                        .replace(R.id.budgetmodifier_detail_container, detailFragment)
                        .commit();
            } else {
                // if a detail fragment is already visible
                budgetItemEditorFragment.saveAndRun(new Runnable() {

                    @Override public void run() {
                        budgetItemEditorFragment.setContent(budgetItemId, false);
                    }
                });
            }
        } else {
            if (budgetItemId != null) {
                startActivity(BudgetItemEditorActivity.getIntentForUpdate(budgetItemId));
            } else {
                startActivity(BudgetItemEditorActivity.getIntentForCreate());
            }
        }
    }

    private Callback<CalculateBalanceTask.BalanceResult> mBalanceCalculatorCallback =
            new Callback<CalculateBalanceTask.BalanceResult>() {

                @Override public void onSuccess(CalculateBalanceTask.BalanceResult data) {
                    setBalanceInfo(data.lastBalanceUpdateEvent, data.bestCaseBalance,
                            data.worstCaseBalance);
                }

                @Override public void onError(Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(App.getAppContext(),
                            "Error calculating balance. See log for more information.",
                            Toast.LENGTH_SHORT)
                            .show();
                }
            };

    private void setBalanceInfo(BalanceUpdateEvent balanceUpdateEvent,
            float bestCaseBalance, float worstCaseBalance) {
        if (mReferenceView != null) {
            String change = App.getAppContext()
                    .getString(R.string.change);
            String text;
            if (balanceUpdateEvent != null) {
                text = App.getAppContext()
                        .getString(R.string.reference_statistics,
                                balanceUpdateEvent.value,
                                DateUtils.DEFAULT_DATE_FORMAT.format(
                                        balanceUpdateEvent.when), change);
            } else {
                text = App.getAppContext()
                        .getString(R.string.reference_statistics_none, change);
            }
            ViewUtils.setTextWithLinkSegment(mReferenceView, text, change,
                    new Runnable() {
                        @Override public void run() {
                            showBalanceUpdateDialog();
                        }
                    });
        }
        if (mBalanceView != null) {
            mBalanceView.setText(App.getAppContext()
                    .getString(R.string.balance, bestCaseBalance, worstCaseBalance));
        }
    }
}
