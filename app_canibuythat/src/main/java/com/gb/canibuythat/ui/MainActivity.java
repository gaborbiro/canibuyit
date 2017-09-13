package com.gb.canibuythat.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.model.Balance;
import com.gb.canibuythat.presenter.BasePresenter;
import com.gb.canibuythat.presenter.MainPresenter;
import com.gb.canibuythat.screen.MainScreen;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.DateUtils;
import com.gb.canibuythat.util.PermissionVerifier;
import com.gb.canibuythat.util.ViewUtils;
import com.google.firebase.iid.FirebaseInstanceId;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * An activity representing a list of Spendings. This activity has different presentations for handset
 * and tablet-size devices. On handsets, the activity presents a list of items, which when touched,
 * lead to a {@link SpendingEditorActivity} representing item details. On tablets, the activity
 * presents the list of items and item details side-by-side using two vertical panes. <p/>
 * The activity makes heavy use of fragments. The list of items is a {@link SpendingListFragment}
 * and the item details (if present) is a {@link SpendingEditorFragment}. <p/>
 * This activity also implements the required {@link SpendingListFragment.FragmentCallback}
 * interface to listen for item selections.
 */
public class MainActivity extends BaseActivity implements MainScreen, SpendingListFragment.FragmentCallback {

    private static final int REQUEST_CODE_CHOOSE_FILE_MONZO = 1;
    private static final int REQUEST_CODE_CHOOSE_FILE_NON_MONZO = 2;
    private static final int REQUEST_CODE_CHOOSE_FILE_ALL = 3;
    private static final int REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT = 4;

    @Inject UserPreferences userPreferences;
    @Inject MainPresenter presenter;

    @Nullable
    @BindView(R.id.estimate_at_time)
    TextView estimateAtTimeView;
    @Nullable
    @BindView(R.id.reference)
    TextView referenceView;
    @Nullable
    @BindView(R.id.chart_button)
    ImageView chartButton;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean twoPane;

    private PermissionVerifier permissionVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.spending_editor_container) != null) {
            twoPane = true;
        }
        if (chartButton != null) {
            chartButton.setOnClickListener(v -> presenter.chartButtonClicked());
        }
        presenter.handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        presenter.handleDeepLink(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onPresenterDestroyed();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected BasePresenter inject() {
        Injector.INSTANCE.getGraph().inject(this);
        return presenter;
    }

    /**
     * Callback method from {@link SpendingListFragment.FragmentCallback} indicating
     * that the spending with the given database ID was selected.
     */
    @Override
    public void onSpendingSelected(int id) {
        presenter.showEditorScreenForSpending(id);
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
            case R.id.menu_import_all:
                presenter.onImportDatabase(SpendingsImportType.ALL);
                break;
            case R.id.menu_import_monzo:
                presenter.onImportDatabase(SpendingsImportType.MONZO);
                break;
            case R.id.menu_import_non_monzo:
                presenter.onImportDatabase(SpendingsImportType.NON_MONZO);
                break;
            case R.id.menu_fcm:
                String token = FirebaseInstanceId.getInstance().getToken();
                Log.d("MonzoDispatch", token);
                presenter.sendFCMTokenToServer(token);
                break;
            case R.id.menu_monzo:
                presenter.fetchMonzoData();
                break;
            case R.id.menu_delete_spendings:
                presenter.deleteAllSpendings();
                break;
            case R.id.menu_set_project_name:
                presenter.deleteAllSpendings();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final SpendingEditorFragment detailFragment = (SpendingEditorFragment) getSupportFragmentManager().findFragmentById(R.id.spending_editor_container);
        if (detailFragment != null) {
            detailFragment.saveAndRun(MainActivity.super::onBackPressed);
        } else {
            MainActivity.super.onBackPressed();
        }
    }

    @Override
    public void showFilePickerActivity(@NonNull String directory, @NonNull SpendingsImportType spendingsImportType) {
        Intent i = new Intent(this, FileDialogActivity.class);
        i.putExtra(FileDialogActivity.EXTRA_START_PATH, directory);
        i.putExtra(FileDialogActivity.EXTRA_SELECTION_MODE, FileDialogActivity.SELECTION_MODE_OPEN);
        switch (spendingsImportType) {
            case ALL:
                startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_ALL);
                break;
            case MONZO:
                startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_MONZO);
                break;
            case NON_MONZO:
                startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_NON_MONZO);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHOOSE_FILE_ALL:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH);
                    presenter.onImportSpendings(path, SpendingsImportType.ALL);
                }
                break;
            case REQUEST_CODE_CHOOSE_FILE_MONZO:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH);
                    presenter.onImportSpendings(path, SpendingsImportType.MONZO);
                }
                break;
            case REQUEST_CODE_CHOOSE_FILE_NON_MONZO:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH);
                    presenter.onImportSpendings(path, SpendingsImportType.NON_MONZO);
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

    public void showEditorScreen(final Integer spendingId) {
        if (twoPane) {
            final SpendingEditorFragment spendingEditorFragment =
                    (SpendingEditorFragment) getSupportFragmentManager().findFragmentById(R.id.spending_editor_container);
            if (spendingEditorFragment == null || !spendingEditorFragment.isAdded()) {
                SpendingEditorFragment detailFragment = new SpendingEditorFragment();

                if (spendingId != null) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(SpendingEditorFragment.EXTRA_SPENDING_ID, spendingId);
                    detailFragment.setArguments(arguments);
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.spending_editor_container, detailFragment).commit();
            } else {
                // if a detail fragment is already visible
                spendingEditorFragment.saveAndRun(() -> spendingEditorFragment.showSpending(spendingId));
            }
        } else {
            if (spendingId != null) {
                startActivity(SpendingEditorActivity.getIntentForUpdate(MainActivity.this, spendingId));
            } else {
                startActivity(SpendingEditorActivity.getIntentForCreate(MainActivity.this));
            }
        }
    }

    @Override
    public void setBalanceInfo(@NonNull Balance balance) {
        if (referenceView != null) {
            String text;
            BalanceReading balanceReading = userPreferences.getBalanceReading();
            if (balanceReading != null) {
                text = getString(R.string.reading, balanceReading.getBalance(), DateUtils.getFORMAT_MONTH_DAY_YR().format(balanceReading.getWhen()));
            } else {
                text = getString(R.string.reading_none);
            }
            ViewUtils.setTextWithLink(referenceView, text, text, this::showBalanceUpdateDialog);
        }
        if (estimateAtTimeView != null) {
            final Date estimateDate = userPreferences.getEstimateDate();
            String estimateDateStr = DateUtils.isToday(estimateDate) ? getString(R.string.today) : DateUtils.getFORMAT_MONTH_DAY_YR().format(estimateDate);

            String defoMaybeStr = "?";
            if (balance.getDefinitely() != null || balance.getMaybeEvenThisMuch() != null) {
                defoMaybeStr = getString(R.string.definitely_maybe, balance.getDefinitely(), balance.getMaybeEvenThisMuch());
            }
            String estimateAtTime = getString(R.string.estimate_at_date, defoMaybeStr, estimateDateStr);
            ViewUtils.setTextWithLinks(estimateAtTimeView, estimateAtTime, new String[]{defoMaybeStr, estimateDateStr}, new Runnable[]{defoMaybeClickListener, estimateDateUpdater});
        }
    }

    private Runnable estimateDateUpdater = () -> {
        DatePickerDialog.OnDateSetListener listener = (view, year, month, dayOfMonth) -> {
            Calendar c = DateUtils.compose(year, month, dayOfMonth);
            BalanceReading balanceReading = userPreferences.getBalanceReading();

            if (balanceReading != null && balanceReading.getWhen().after(c.getTime())) {
                Toast.makeText(MainActivity.this,
                        "Please set a date after the last balance " + "reading! (" + balanceReading.getWhen() + ")", Toast.LENGTH_SHORT).show();
            } else {
                userPreferences.setEstimateDate(c.getTime());
            }
        };

        DatePickerDialog datePickerDialog = DateUtils.getDatePickerDialog(MainActivity.this, listener, userPreferences.getEstimateDate());
        datePickerDialog.show();
    };

    private Runnable defoMaybeClickListener = () -> presenter.fetchCategoryBalance();

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

    @Override
    public void showCategoryBalance(@NotNull String text) {
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(MainActivity.this, "Add some spendings", Toast.LENGTH_SHORT).show();
        } else {
            PromptDialog.newInstance("Category balance", text).setPositiveButton(android.R.string.ok, null).show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void setProjectName(String name) {
        if (name == null) {
            setTitle(getString(R.string.app_name));
        } else {
            setTitle(getString(R.string.app_name_project, name));
        }
    }
}
