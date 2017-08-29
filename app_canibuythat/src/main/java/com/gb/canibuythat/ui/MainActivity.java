package com.gb.canibuythat.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.model.Balance;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.presenter.MainPresenter;
import com.gb.canibuythat.screen.MainScreen;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.PermissionVerifier;
import com.gb.canibuythat.util.ViewUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * An activity representing a list of BudgetModifiers. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link BudgetEditorActivity} representing item details. On tablets, the activity
 * presents the list of items and item details side-by-side using two vertical panes. <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link BudgetListFragment} and the item details (if present) is a {@link BudgetEditorFragment}. <p/>
 * This activity also implements the required
 * {@link BudgetListFragment.FragmentCallback}
 * interface to listen for item selections.
 */
public class MainActivity extends BaseActivity implements MainScreen, BudgetListFragment.FragmentCallback, BalanceReadingInputDialog.BalanceReadingInputListener {

    private static final int REQUEST_CODE_CHOOSE_FILE = 1;
    private static final int REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT = 2;

    @Inject UserPreferences userPreferences;
    @Inject MainPresenter presenter;

    @Nullable @BindView(R.id.estimate_at_time) TextView estimateAtTimeView;
    @Nullable @BindView(R.id.reference) TextView referenceView;
    @Nullable @BindView(R.id.chart_button) ImageView chartButton;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean twoPane;

    private PermissionVerifier permissionVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter.setScreen(this);

        if (findViewById(R.id.budgetmodifier_detail_container) != null) {
            twoPane = true;
        }
        if (chartButton != null) {
            chartButton.setOnClickListener(v -> presenter.chartButtonClicked());
        }
        if (getIntent().getData() != null) {
            presenter.handleDeepLink(getIntent());
        }
    }

    @Override
    protected void inject() {
        Injector.INSTANCE.getGraph().inject(this);
    }

    /**
     * Callback method from {@link BudgetListFragment.FragmentCallback} indicating
     * that the budget item with the given database ID was selected.
     */
    @Override
    public void onBudgetItemSelected(int id) {
        presenter.showEditorScreenForBudgetItem(id);
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
                presenter.showEditorScreen();
                break;
            case R.id.menu_update_balance:
                presenter.updateBalance();
                break;
            case R.id.menu_export:
                permissionVerifier = new PermissionVerifier(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
                if (permissionVerifier.verifyPermissions(true, REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT)) {
                    presenter.exportDatabase();
                }
                break;
            case R.id.menu_import:
                presenter.onImportDatabase();
                break;
            case R.id.menu_monzo:
                presenter.loadMonzoData();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final BudgetEditorFragment detailFragment = (BudgetEditorFragment) getSupportFragmentManager().findFragmentById(R.id.budgetmodifier_detail_container);
        if (detailFragment != null) {
            detailFragment.saveAndRun(MainActivity.super::onBackPressed);
        } else {
            MainActivity.super.onBackPressed();
        }
    }

    /**
     * Callback from the {@link BalanceReadingInputDialog} notifying that the user has
     * added a new balance reading
     */
    @Override
    public void onBalanceReadingSet(BalanceReading balanceReading) {
        userPreferences.setBalanceReading(balanceReading);
        userPreferences.setEstimateDate(null);
        presenter.fetchBalance();
    }

    @Override
    public void showFilePickerActivity(String directory) {
        Intent i = new Intent(this, FileDialogActivity.class);
        i.putExtra(FileDialogActivity.EXTRA_START_PATH, directory);
        i.putExtra(FileDialogActivity.EXTRA_SELECTION_MODE, FileDialogActivity.SELECTION_MODE_OPEN);
        startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHOOSE_FILE:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH);
                    presenter.onDatabaseFileSelected(path);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT) {
            if (permissionVerifier.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
                presenter.exportDatabase();
            } else {
                Toast.makeText(this, "Missing permissions!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showBalanceUpdateDialog() {
        new BalanceReadingInputDialog().show(getSupportFragmentManager(), null);
    }

    public void showEditorScreen(final Integer budgetItemId) {
        if (twoPane) {
            final BudgetEditorFragment budgetEditorFragment =
                    (BudgetEditorFragment) getSupportFragmentManager().findFragmentById(R.id.budgetmodifier_detail_container);
            if (budgetEditorFragment == null || !budgetEditorFragment.isAdded()) {
                BudgetEditorFragment detailFragment = new BudgetEditorFragment();

                if (budgetItemId != null) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(BudgetEditorFragment.EXTRA_ITEM_ID, budgetItemId);
                    detailFragment.setArguments(arguments);
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.budgetmodifier_detail_container, detailFragment).commit();
            } else {
                // if a detail fragment is already visible
                budgetEditorFragment.saveAndRun(() -> budgetEditorFragment.showBudgetItem(budgetItemId, false));
            }
        } else {
            if (budgetItemId != null) {
                startActivity(BudgetEditorActivity.getIntentForUpdate(MainActivity.this, budgetItemId));
            } else {
                startActivity(BudgetEditorActivity.getIntentForCreate(MainActivity.this));
            }
        }
    }

    @Override
    public void setData(List<BudgetItem> budgetItems) {
        ((BudgetListFragment) getSupportFragmentManager().findFragmentById(R.id.budget_list)).setData(budgetItems);
    }

    @Override
    public void setBalanceInfo(Balance balance) {
        if (referenceView != null) {
            String text;
            if (balance.getBalanceReading() != null) {
                text = getString(R.string.reading, balance.getBalanceReading().balance, DateUtils.FORMAT_MONTH_DAY.format(balance.getBalanceReading().when));
            } else {
                text = getString(R.string.reading_none);
            }
            ViewUtils.setTextWithLink(referenceView, text, text, this::showBalanceUpdateDialog);
        }
        if (estimateAtTimeView != null) {
            final Date estimateDate = userPreferences.getEstimateDate();
            String estimateDateStr = estimateDate != null ? DateUtils.FORMAT_MONTH_DAY.format(estimateDate) : getString(R.string.today);
            String estimateAtTime = getString(R.string.estimate_at_time, balance.getBestCaseBalance(), balance.getWorstCaseBalance(), estimateDateStr);
            ViewUtils.setTextWithLink(estimateAtTimeView, estimateAtTime, estimateDateStr, estimateDateUpdater);
        }
    }

    private Runnable estimateDateUpdater = () -> {
        DatePickerDialog.OnDateSetListener listener = (view, year, monthOfYear, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();

            if (c.get(Calendar.YEAR) == year
                    && c.get(Calendar.MONTH) == monthOfYear
                    && c.get(Calendar.DAY_OF_MONTH) == dayOfMonth) {
                userPreferences.setEstimateDate(null);
            } else {
                c.set(year, monthOfYear, dayOfMonth);
                BalanceReading balanceReading = userPreferences.getBalanceReading();

                if (balanceReading == null || balanceReading.when.before(c.getTime())) {
                    DateUtils.clearLowerBits(c);
                    userPreferences.setEstimateDate(c.getTime());
                } else {
                    Toast.makeText(MainActivity.this,
                            "Please set a date after the last balance " + "reading! (" + balanceReading.when + ")", Toast.LENGTH_SHORT).show();
                }
            }
            presenter.fetchBalance();
        };

        DatePickerDialog datePickerDialog = DateUtils.getDatePickerDialog(MainActivity.this, listener, userPreferences.getEstimateDate());
        datePickerDialog.show();
    };

    @Override
    public void showLoginActivity() {
        LoginActivity.show(this);
    }

    @Override
    public void showChartScreen() {
        ChartActivity.show(MainActivity.this);
    }

    @Override
    public void showToast(@NotNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
