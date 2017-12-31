package com.gb.canibuythat.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import com.gb.canibuythat.R
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.presenter.BasePresenter
import com.gb.canibuythat.presenter.MainPresenter
import com.gb.canibuythat.presenter.MonzoDispatchPresenter
import com.gb.canibuythat.screen.MainScreen
import com.gb.canibuythat.screen.Screen
import com.gb.canibuythat.util.DateUtils
import com.gb.canibuythat.util.PermissionVerifier
import com.gb.canibuythat.util.setTextWithLink
import com.gb.canibuythat.util.setTextWithLinks
import com.google.firebase.iid.FirebaseInstanceId
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
class MainActivity : BaseActivity(), MainScreen, SpendingListFragment.FragmentCallback, BalanceBreakdownDialog.Companion.Callback {

    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject internal lateinit var mainPresenter: MainPresenter
    @Inject internal lateinit var monzoDispatchPresenter: MonzoDispatchPresenter

    val projectionLbl: TextView? by lazy { findViewById(R.id.projection_lbl) as TextView? }
    val referenceLbl: TextView? by lazy { findViewById(R.id.reference_lbl) as TextView? }

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private var twoPane: Boolean = false

    private lateinit var permissionVerifier: PermissionVerifier

    private fun estimateDateUpdater() {
        val listener = { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val c = DateUtils.compose(year, month, dayOfMonth)
            val balanceReading = userPreferences.balanceReading

            if (balanceReading != null && balanceReading.`when`?.after(c.time) == true) {
                Toast.makeText(this@MainActivity,
                        "Please set a date after the last balance " + "reading! (" + balanceReading.`when` + ")", Toast.LENGTH_SHORT).show()
            } else {
                userPreferences.estimateDate = c.time
            }
        }
        val datePickerDialog = DateUtils.getDatePickerDialog(this@MainActivity, listener, userPreferences.estimateDate)
        datePickerDialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monzoDispatchPresenter.setScreen(this)
        setContentView(R.layout.activity_main)

        if (findViewById(R.id.spending_editor_container) != null) {
            twoPane = true
        }
        mainPresenter.handleDeepLink(intent)
        mainPresenter.calculateCurrentSavings()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mainPresenter.handleDeepLink(intent)
    }

    override fun inject(): BasePresenter<Screen> {
        Injector.INSTANCE.graph.inject(this)
        return mainPresenter as BasePresenter<Screen>
    }

    /**
     * Callback method from [SpendingListFragment.FragmentCallback] indicating
     * that the spending with the given database ID was selected.
     */
    override fun onSpendingSelected(id: Int) {
        mainPresenter.showEditorScreenForSpending(id)
    }

    override fun refresh() {
        mainPresenter.fetchMonzoData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> mainPresenter.showEditorScreen()
            R.id.menu_export -> {
                permissionVerifier = PermissionVerifier(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                if (permissionVerifier.verifyPermissions(true, REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT)) {
                    mainPresenter.exportDatabase()
                }
            }
            R.id.menu_import_all -> mainPresenter.onImportDatabase(MainScreen.SpendingsImportType.ALL)
            R.id.menu_import_monzo -> mainPresenter.onImportDatabase(MainScreen.SpendingsImportType.MONZO)
            R.id.menu_import_non_monzo -> mainPresenter.onImportDatabase(MainScreen.SpendingsImportType.NON_MONZO)
            R.id.menu_fcm -> {
                val token = FirebaseInstanceId.getInstance().token
                Log.d("MonzoDispatch", token)
                monzoDispatchPresenter.sendFCMTokenToServer(token!!)
            }
            R.id.menu_delete_spendings -> mainPresenter.deleteAllSpendings()
            R.id.menu_set_project_name -> mainPresenter.onSetProjectName()
            R.id.menu_hooks -> mainPresenter.logWebhooks()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val detailFragment = supportFragmentManager.findFragmentById(R.id.spending_editor_container) as SpendingEditorFragment?
        detailFragment?.saveAndRun(Runnable { super@MainActivity.onBackPressed() })
                ?: super@MainActivity.onBackPressed()
    }

    override fun showPickerForExport(suggestedPath: String) {
        val i = Intent(this, FileDialogActivity::class.java)
        i.putExtra(FileDialogActivity.EXTRA_START_PATH, suggestedPath)
        i.putExtra(FileDialogActivity.EXTRA_SELECTION_MODE, FileDialogActivity.SELECTION_MODE_CREATE)
        startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_EXPORT)
    }

    override fun showPickerForImport(directory: String, spendingsImportType: MainScreen.SpendingsImportType) {
        val i = Intent(this, FileDialogActivity::class.java)
        i.putExtra(FileDialogActivity.EXTRA_START_PATH, directory)
        i.putExtra(FileDialogActivity.EXTRA_SELECTION_MODE, FileDialogActivity.SELECTION_MODE_OPEN)
        when (spendingsImportType) {
            MainScreen.SpendingsImportType.ALL -> startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_ALL)
            MainScreen.SpendingsImportType.MONZO -> startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_MONZO)
            MainScreen.SpendingsImportType.NON_MONZO -> startActivityForResult(i, REQUEST_CODE_CHOOSE_FILE_NON_MONZO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_CODE_CHOOSE_FILE_ALL -> if (resultCode == Activity.RESULT_OK) {
                val path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH)
                mainPresenter.onImportSpendings(path, MainScreen.SpendingsImportType.ALL)
            }
            REQUEST_CODE_CHOOSE_FILE_MONZO -> if (resultCode == Activity.RESULT_OK) {
                val path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH)
                mainPresenter.onImportSpendings(path, MainScreen.SpendingsImportType.MONZO)
            }
            REQUEST_CODE_CHOOSE_FILE_NON_MONZO -> if (resultCode == Activity.RESULT_OK) {
                val path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH)
                mainPresenter.onImportSpendings(path, MainScreen.SpendingsImportType.NON_MONZO)
            }
            REQUEST_CODE_CHOOSE_FILE_EXPORT -> if (resultCode == Activity.RESULT_OK) {
                val path = data.getStringExtra(FileDialogActivity.EXTRA_RESULT_PATH)
                mainPresenter.onExportSpendings(path)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT) {
            if (permissionVerifier.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
                mainPresenter.exportDatabase()
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
            val spendingEditorFragment = supportFragmentManager.findFragmentById(R.id.spending_editor_container) as SpendingEditorFragment
            if (!spendingEditorFragment.isAdded) {
                val detailFragment = SpendingEditorFragment()

                if (spendingId != null) {
                    val arguments = Bundle()
                    arguments.putInt(SpendingEditorFragment.EXTRA_SPENDING_ID, spendingId)
                    detailFragment.arguments = arguments
                }
                supportFragmentManager.beginTransaction().replace(R.id.spending_editor_container, detailFragment).commit()
            } else {
                // if a detail fragment is already visible
                spendingEditorFragment.saveAndRun(Runnable { spendingEditorFragment.showSpending(spendingId) })
            }
        } else {
            if (spendingId != null) {
                startActivity(SpendingEditorActivity.getIntentForUpdate(this@MainActivity, spendingId))
            } else {
                startActivity(SpendingEditorActivity.getIntentForCreate(this@MainActivity))
            }
        }
    }

    override fun setBalanceInfo(balance: Balance?) {
        referenceLbl?.let {
            val text: String
            val balanceReading = userPreferences.balanceReading
            text = balanceReading?.`when`?.let {
                getString(R.string.reading, balanceReading.balance, DateUtils.formatDayMonthYearWithPrefix(balanceReading.`when`))
            } ?: getString(R.string.reading_none)
            it.setTextWithLink(text, text.substring(6), this::showBalanceUpdateDialog)
        }
        projectionLbl?.let {
            val estimateDate = userPreferences.estimateDate
            val estimateDateStr = if (DateUtils.isToday(estimateDate)) getString(R.string.today) else DateUtils.formatDayMonthYearWithPrefix(estimateDate)

            var defoMaybeStr = "?"
            var targetDefoMaybeStr = "?"
            if (balance != null) {
                defoMaybeStr = getString(R.string.definitely_maybe, balance.definitely, balance.maybeEvenThisMuch)
                targetDefoMaybeStr = getString(R.string.definitely_maybe, balance.targetDefinitely, balance.targetMaybeEvenThisMuch)
            }
            val estimateAtTime = getString(R.string.estimate_at_date, defoMaybeStr, estimateDateStr, targetDefoMaybeStr)
            it.setTextWithLinks(
                    estimateAtTime,
                    arrayOf(defoMaybeStr, targetDefoMaybeStr, "behave", estimateDateStr),
                    arrayOf(
                            mainPresenter::getBalanceBreakdown,
                            mainPresenter::getTargetBalanceBreakdown,
                            mainPresenter::getTargetSavingBreakdown,
                            this::estimateDateUpdater))
        }
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
            PromptDialog.bigMessageDialog(title, text).setPositiveButton(android.R.string.ok, null).show(supportFragmentManager, null)
        }
    }

    override fun setTitle(projectName: String?) {
        if (projectName == null) {
            super.setTitle(getString(R.string.app_name))
        } else {
            super.setTitle(getString(R.string.app_name_project, projectName))
        }
    }

    override fun setProjectName(currentName: String?) {
        val inputDialog = InputDialog.newInstance("Project name", currentName)
        inputDialog.setPositiveButton(R.string.save) { _ -> mainPresenter.setProjectName(inputDialog.getInput()) }.show(supportFragmentManager, null)
    }

    override fun onBalanceBreakdownItemClicked(category: ApiSpending.Category) {
        mainPresenter.onBalanceBreakdownItemClicked(category)
    }

    override fun setBalanceBreakdown(breakdown: Array<Pair<ApiSpending.Category, String>>) {
        val promptDialog = BalanceBreakdownDialog()
        val args = Bundle()
        args.putString(PromptDialog.EXTRA_TITLE, "Balance breakdown")
        args.putSerializable("breakdown", breakdown)
        promptDialog.arguments = args
        promptDialog.setPositiveButton(android.R.string.ok, null).show(supportFragmentManager, null)
    }

    companion object {
        private val REQUEST_CODE_CHOOSE_FILE_MONZO = 1
        private val REQUEST_CODE_CHOOSE_FILE_NON_MONZO = 2
        private val REQUEST_CODE_CHOOSE_FILE_ALL = 3
        private val REQUEST_CODE_PERMISSIONS_FOR_DB_EXPORT = 4
        private val REQUEST_CODE_CHOOSE_FILE_EXPORT = 5
    }
}

class BalanceBreakdownDialog : PromptDialog() {

    companion object {
        interface Callback {
            fun onBalanceBreakdownItemClicked(category: ApiSpending.Category)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val bigMessageTV = view.findViewById(R.id.big_message) as TextView

        val breakdown = arguments.getSerializable("breakdown") as Array<Pair<ApiSpending.Category, String>>
        val buffer = StringBuffer()
        breakdown.joinTo(buffer = buffer, separator = "\n", transform = {
            it.second
        })
        setBigMessage(buffer.toString())
        bigMessageTV.setTextWithLinks(
                buffer.toString(),
                breakdown.map { it.second }.toTypedArray(),
                breakdown.map { pair ->
                    {
                        (activity as Callback).onBalanceBreakdownItemClicked(pair.first)
                    }
                }.toTypedArray())
        return view
    }
}