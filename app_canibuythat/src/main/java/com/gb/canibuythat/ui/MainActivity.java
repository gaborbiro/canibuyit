package com.gb.canibuythat.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.CredentialsProvider;
import com.gb.canibuythat.MonzoConstants;
import com.gb.canibuythat.R;
import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.interactor.BudgetInteractor;
import com.gb.canibuythat.interactor.MonzoInteractor;
import com.gb.canibuythat.model.Balance;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.PermissionVerifier;
import com.gb.canibuythat.util.ViewUtils;

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
public class MainActivity extends BaseActivity implements BudgetListFragment.FragmentCallback, BalanceReadingInputDialog.BalanceReadingInputListener {

    private static final int REQUEST_CODE_CHOOSE_FILE = 1;
    private static final int REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT = 2;

    @Inject MonzoInteractor monzoInteractor;
    @Inject BudgetInteractor budgetInteractor;
    @Inject CredentialsProvider credentialsProvider;
    @Inject UserPreferences userPreferences;

    @BindView(R.id.estimate_at_time) TextView estimateAtTimeView;
    @BindView(R.id.reference) TextView referenceView;
    @BindView(R.id.chart_button) ImageView chartButton;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean twoPane;

    private PermissionVerifier permissionVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.budgetmodifier_detail_container) != null) {
            twoPane = true;
        }
        if (chartButton != null) {
            ChartActivity.launchOnClick(this, chartButton);
        }
        budgetInteractor.calculateBalance().subscribe(this::setBalanceInfo, this::onError);

        if (getIntent().getData() != null) {
            Uri data = getIntent().getData();
            if (data.getAuthority().equals(MonzoConstants.MONZO_AUTH_AUTHORITY)) {
                List<String> pathSegments = data.getPathSegments();

                if (pathSegments.get(0).equals(MonzoConstants.MONZO_AUTH_PATH_BASE)) {
                    if (pathSegments.get(1).equals(MonzoConstants.MONZO_AUTH_PATH_CALLBACK)) {
                        // finished email authentication -> exchange code for auth token
                        String authorizationCode = data.getQueryParameter(MonzoConstants.MONZO_OAUTH_PARAM_AUTHORIZATION_CODE);
                        login(authorizationCode);
                    }
                }
            }
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
    public void onListItemClick(int id) {
        showEditorScreen(id);
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
                showEditorScreen();
                break;
            case R.id.menu_update_balance:
                showBalanceUpdateDialog();
                break;
            case R.id.menu_export:
                permissionVerifier = new PermissionVerifier(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
                if (permissionVerifier.verifyPermissions(true, REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT)) {
                    budgetInteractor.exportDatabase().subscribe(() -> {
                    }, this::onError);
                }
                break;
            case R.id.menu_import:
                importDatabase();
                break;
            case R.id.menu_monzo:
                if (TextUtils.isEmpty(credentialsProvider.getAccessToken())) {
                    LoginActivity.show(this);
                } else {
                    monzoInteractor.accounts()
                            .subscribe(accounts -> Toast.makeText(this, accounts[0].getDescription(), Toast.LENGTH_SHORT).show(), this::onError);
                }
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

    private void importDatabase() {
        Intent i = new Intent(this, FileDialogActivity.class);
        i.putExtra(FileDialogActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath() + "/CanIBuyThat/");
        i.putExtra(FileDialogActivity.EXTRA_SELECTION_MODE, FileDialogActivity.SELECTION_MODE_OPEN);
        startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE);
    }

//    @Subscribe public void onEvent(BudgetItemUpdatedEvent event) {
//        new CalculateBalanceTask(balanceCalculatorCallback).execute();
//    }

    /**
     * Callback from the {@link BalanceReadingInputDialog} notifying that the user has
     * added a new balance reading
     */
    @Override
    public void onBalanceReadingSet(BalanceReading balanceReading) {
        userPreferences.setBalanceReading(balanceReading);
        userPreferences.setEstimateDate(null);
        budgetInteractor.calculateBalance().subscribe(this::setBalanceInfo, this::onError);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHOOSE_FILE:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH);
                    budgetInteractor.importBudgetDatabaseFromFile(path)
                            .subscribe(() -> budgetInteractor.calculateBalance().subscribe(this::setBalanceInfo, this::onError), this::onError);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT) {
            if (permissionVerifier.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
                budgetInteractor.exportDatabase().subscribe(() -> {
                }, this::onError);
            } else {
                Toast.makeText(this, "Missing permissions!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBalanceUpdateDialog() {
        new BalanceReadingInputDialog().show(getSupportFragmentManager(), null);
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
                budgetEditorFragment.saveAndRun(() -> budgetEditorFragment.setContent(budgetItemId, false));
            }
        } else {
            if (budgetItemId != null) {
                startActivity(BudgetEditorActivity.getIntentForUpdate(MainActivity.this, budgetItemId));
            } else {
                startActivity(BudgetEditorActivity.getIntentForCreate(MainActivity.this));
            }
        }
    }

    private void setBalanceInfo(Balance balance) {
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
            budgetInteractor.calculateBalance().subscribe(this::setBalanceInfo, this::onError);
        };

        DatePickerDialog datePickerDialog = DateUtils.getDatePickerDialog(MainActivity.this, listener, userPreferences.getEstimateDate());
        datePickerDialog.show();
    };

    private void login(String authorizationCode) {
        monzoInteractor.login(authorizationCode).subscribe(login -> {
            credentialsProvider.setAccessToken(login.getAccessToken());
            credentialsProvider.setRefreshToken(login.getRefreshToken());
            Toast.makeText(this, "AccessToken: " + credentialsProvider.getAccessToken(), Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "RefreshToken: " + credentialsProvider.getRefreshToken(), Toast.LENGTH_SHORT).show();
        }, this::onError);
    }
}
