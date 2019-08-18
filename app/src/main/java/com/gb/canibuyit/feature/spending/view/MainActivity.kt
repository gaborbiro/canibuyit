package com.gb.canibuyit.feature.spending.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.gb.canibuyit.R
import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.base.view.BaseActivity
import com.gb.canibuyit.base.view.PromptDialog
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.feature.chart.view.ChartActivity
import com.gb.canibuyit.feature.dispatch.view.DispatchPresenter
import com.gb.canibuyit.feature.dispatch.view.DispatchScreen
import com.gb.canibuyit.feature.monzo.view.LoginActivity
import com.gb.canibuyit.feature.spending.data.Balance
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.feature.spending.ui.BalanceBreakdown
import com.gb.canibuyit.feature.spending.ui.BalanceBreakdownDialog
import com.gb.canibuyit.feature.spending.ui.BalanceBreakdownDialogCallback
import com.gb.canibuyit.feature.spending.ui.BalanceReadingInputDialog
import com.gb.canibuyit.feature.spending.ui.EXTRA_RESULT_PATH
import com.gb.canibuyit.feature.spending.ui.EXTRA_SELECTION_MODE
import com.gb.canibuyit.feature.spending.ui.EXTRA_START_PATH
import com.gb.canibuyit.feature.spending.ui.FileDialogActivity
import com.gb.canibuyit.feature.spending.ui.InputDialog
import com.gb.canibuyit.feature.spending.ui.SELECTION_MODE_CREATE
import com.gb.canibuyit.feature.spending.ui.SELECTION_MODE_OPEN
import com.gb.canibuyit.util.PermissionVerifier
import com.gb.canibuyit.util.createDatePickerDialog
import com.gb.canibuyit.util.formatDayMonthYearWithPrefix
import com.gb.canibuyit.util.isToday
import com.gb.canibuyit.util.setSubtextWithLink
import com.gb.canibuyit.util.setSubtextWithLinks
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.statistics.*
import javax.inject.Inject

/**
 * An activity representing a list of Spendings. This activity has different presentations for handset
 * and tablet-size devices. On handsets, the activity presents a list of items, which when touched,
 * lead to a [SpendingEditorActivity] representing item details. On tablets, the activity
 * presents the list of items and item details side-by-side using two vertical panes.
 *
 *
 * The activity makes heavy use of fragments. The list of items is a [SpendingListFragment]
 * and the item details (if present) is a [SpendingEditorFragment].
 *
 *
 * This activity also implements the required [SpendingListFragment.FragmentCallback]
 * interface to listen for item selections.
 */
class MainActivity : BaseActivity(), MainScreen, DispatchScreen,
    SpendingListFragment.FragmentCallback, BalanceBreakdownDialogCallback {

    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var presenter: MainPresenter
    @Inject internal lateinit var dispatchPresenter: DispatchPresenter

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private var twoPane: Boolean = false

    private lateinit var permissionVerifier: PermissionVerifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.screenReference = this
        dispatchPresenter.screenReference = this
        setContentView(R.layout.activity_main)

        if (findViewById<View>(R.id.spending_editor_container) != null) {
            twoPane = true
        }
        presenter.handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        presenter.handleDeepLink(intent)
    }

    override fun inject() {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun close() {
        finish()
    }

    /**
     * Callback method from [SpendingListFragment.FragmentCallback] indicating
     * that the spending with the given database ID was selected.
     */
    override fun onSpendingSelected(spendingId: Int) {
        presenter.showEditorScreenForSpending(spendingId)
    }

    override fun refresh() {
        presenter.fetchMonzoData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> presenter.showEditorScreen()
            R.id.menu_export -> {
                permissionVerifier =
                    PermissionVerifier(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                if (permissionVerifier.verifyPermissions(true,
                        REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT)) {
                    presenter.exportDatabase()
                }
            }
            R.id.menu_import_all -> presenter.onImportDatabase(
                MainScreen.SpendingsImportType.ALL)
            R.id.menu_import_monzo -> presenter.onImportDatabase(
                MainScreen.SpendingsImportType.MONZO)
            R.id.menu_import_non_monzo -> presenter.onImportDatabase(
                MainScreen.SpendingsImportType.NON_MONZO)
            R.id.menu_fcm -> sendFCMTokenToServer()
            R.id.menu_delete_spendings -> {
                AlertDialog.Builder(this).apply {
                    setMessage("Are you sure you want to delete everything?\nNote: You can do backups")
                    setPositiveButton("Permanently Delete All") { _, _ ->
                        presenter.deleteAllSpendings()
                    }
                    setNeutralButton(android.R.string.cancel, null)
                    show()
                }
            }
            R.id.menu_set_project_name -> presenter.onSetProjectName()
            R.id.menu_hooks -> presenter.logWebhooks()
            R.id.menu_overview_chart -> ChartActivity.launch(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val detailFragment = supportFragmentManager.findFragmentById(
            R.id.spending_editor_container) as SpendingEditorFragment?
        detailFragment?.onFragmentClose { super@MainActivity.onBackPressed() }
            ?: super@MainActivity.onBackPressed()
    }

    override fun showPickerForExport(suggestedPath: String) {
        val i = Intent(this, FileDialogActivity::class.java)
        i.putExtra(EXTRA_START_PATH, suggestedPath)
        i.putExtra(EXTRA_SELECTION_MODE,
            SELECTION_MODE_CREATE)
        startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_EXPORT)
    }

    override fun showPickerForImport(directory: String,
                                     spendingsImportType: MainScreen.SpendingsImportType) {
        val i = Intent(this, FileDialogActivity::class.java)
        i.putExtra(EXTRA_START_PATH, directory)
        i.putExtra(EXTRA_SELECTION_MODE, SELECTION_MODE_OPEN)
        when (spendingsImportType) {
            MainScreen.SpendingsImportType.ALL -> startActivityForResult(i,
                REQUEST_CODE_CHOOSE_FILE_ALL)
            MainScreen.SpendingsImportType.MONZO -> startActivityForResult(i,
                REQUEST_CODE_CHOOSE_FILE_MONZO)
            MainScreen.SpendingsImportType.NON_MONZO -> startActivityForResult(i,
                REQUEST_CODE_CHOOSE_FILE_NON_MONZO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            when (requestCode) {
                REQUEST_CODE_CHOOSE_FILE_ALL -> if (resultCode == Activity.RESULT_OK) {
                    val path = data.getStringExtra(EXTRA_RESULT_PATH)
                    presenter.onImportSpendings(path, MainScreen.SpendingsImportType.ALL)
                }
                REQUEST_CODE_CHOOSE_FILE_MONZO -> if (resultCode == Activity.RESULT_OK) {
                    val path = data.getStringExtra(EXTRA_RESULT_PATH)
                    presenter.onImportSpendings(path, MainScreen.SpendingsImportType.MONZO)
                }
                REQUEST_CODE_CHOOSE_FILE_NON_MONZO -> if (resultCode == Activity.RESULT_OK) {
                    val path = data.getStringExtra(EXTRA_RESULT_PATH)
                    presenter.onImportSpendings(path, MainScreen.SpendingsImportType.NON_MONZO)
                }
                REQUEST_CODE_CHOOSE_FILE_EXPORT -> if (resultCode == Activity.RESULT_OK) {
                    val path = data.getStringExtra(EXTRA_RESULT_PATH)
                    presenter.onExportSpendings(path)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT) {
            if (permissionVerifier.onRequestPermissionsResult(requestCode, permissions,
                    grantResults)) {
                presenter.exportDatabase()
            } else {
                Toast.makeText(this, "Missing permissions!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun showBalanceUpdateDialog() {
        BalanceReadingInputDialog().show(supportFragmentManager, null)
    }

    override fun showEditorScreen(spendingId: Int?) {
        if (twoPane) {
            val spendingEditorFragment = supportFragmentManager.findFragmentById(
                R.id.spending_editor_container) as SpendingEditorFragment
            if (!spendingEditorFragment.isAdded) {
                val detailFragment = SpendingEditorFragment()

                if (spendingId != null) {
                    val arguments = Bundle()
                    arguments.putInt(EXTRA_SPENDING_ID, spendingId)
                    detailFragment.arguments = arguments
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.spending_editor_container, detailFragment).commit()
            } else {
                // if a detail fragment is already visible
                spendingEditorFragment.onFragmentClose {
                    spendingEditorFragment.showSpending(spendingId)
                }
            }
        } else {
            if (spendingId != null) {
                startActivity(
                    SpendingEditorActivity.getIntentForUpdate(this@MainActivity, spendingId))
            } else {
                startActivity(SpendingEditorActivity.getIntentForCreate(this@MainActivity))
            }
        }
    }

    override fun setBalanceInfo(balance: Balance?) {
        val text: String
        val balanceReading = userPreferences.balanceReading
        text = balanceReading?.date?.let {
            getString(R.string.reading, balanceReading.balance,
                balanceReading.date.formatDayMonthYearWithPrefix())
        } ?: getString(R.string.reading_none)
        reference_lbl.setSubtextWithLink(text, text.substring(6), this::showBalanceUpdateDialog)
        val estimateDate = userPreferences.estimateDate
        val estimateDateStr = if (estimateDate.isToday()) getString(
            R.string.today) else estimateDate.formatDayMonthYearWithPrefix()

        var defoMaybeStr = "?"
        var targetDefoMaybeStr = "?"
        if (balance != null) {
            defoMaybeStr = getString(R.string.amount_formatted, balance.amount)
            targetDefoMaybeStr = getString(R.string.amount_formatted, balance.target)
        }
        val estimateAtTime =
            getString(R.string.estimate_at_date, defoMaybeStr, estimateDateStr, targetDefoMaybeStr)
        projection_lbl.setSubtextWithLinks(
            estimateAtTime,
            arrayOf(defoMaybeStr, "behave", estimateDateStr),
            arrayOf(
                presenter::showBalanceBreakdown,
                presenter::showTargetSavingBreakdown,
                this::showEstimateDateUpdater))

    }

    private fun showEstimateDateUpdater() {
        createDatePickerDialog(this@MainActivity, userPreferences.estimateDate) { _, date ->
            val balanceReading = userPreferences.balanceReading

            if (balanceReading != null && balanceReading.date?.isAfter(date) == true) {
                Toast.makeText(this@MainActivity,
                    "Please set a date after the last balance " + "reading! (" + balanceReading.date + ")",
                    Toast.LENGTH_SHORT)
                    .show()
            } else {
                userPreferences.estimateDate = date
            }
        }.show()
    }

    override fun showLoginActivity() {
        LoginActivity.show(this)
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showDialog(title: String, text: String) {
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this@MainActivity, "Add some spendings", Toast.LENGTH_SHORT).show()
        } else {
            PromptDialog.bigMessageDialog(title, text).setPositiveButton(android.R.string.ok, null)
                .show(supportFragmentManager, null)
        }
    }

    override fun setTitle(projectName: String?) {
        if (projectName.isNullOrBlank()) {
            super.setTitle(getString(R.string.app_name))
        } else {
            super.setTitle(getString(R.string.app_name_project, projectName))
        }
    }

    override fun setProjectName(currentName: String?) {
        val inputDialog = InputDialog.newInstance("Project name", currentName)
        inputDialog.setPositiveButton(R.string.save) { _ ->
            presenter.setProjectName(inputDialog.input)
        }
            .show(supportFragmentManager, null)
    }

    override fun onBalanceBreakdownItemClicked(category: ApiSpending.Category) {
        presenter.onBalanceBreakdownItemClicked(category)
    }

    override fun showBalanceBreakdown(breakdown: BalanceBreakdown) {
        BalanceBreakdownDialog.show(breakdown, supportFragmentManager)
    }

    override fun sendFCMTokenToServer() {
        val token = FirebaseInstanceId.getInstance().token
        Log.d("MonzoDispatch", token)
        dispatchPresenter.sendFCMTokenToServer(token!!)
    }

    override fun setLastUpdate(lastUpdate: String) {
        last_update?.text = "Last update: $lastUpdate"
    }
}

private const val REQUEST_CODE_CHOOSE_FILE_MONZO = 1
private const val REQUEST_CODE_CHOOSE_FILE_NON_MONZO = 2
private const val REQUEST_CODE_CHOOSE_FILE_ALL = 3
private const val REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT = 4
private const val REQUEST_CODE_CHOOSE_FILE_EXPORT = 5
