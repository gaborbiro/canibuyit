package com.gb.canibuythat.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.gb.canibuythat.R
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.di.Injector
import com.gb.canibuythat.ui.model.BalanceReading
import com.gb.canibuythat.util.DateUtils
import com.gb.canibuythat.util.clearLowerBits
import com.gb.canibuythat.util.showKeyboard
import java.util.*
import javax.inject.Inject

class BalanceReadingInputDialog : DialogFragment(), DialogInterface.OnClickListener {

    @Inject lateinit var userPreferences: UserPreferences
    private var lastUpdate: BalanceReading? = null

    lateinit var body: LinearLayout
    val amountInput: EditText by lazy { body.findViewById(R.id.amount_input) as EditText }
    val whenBtn: DatePickerButton by lazy { body.findViewById(R.id.when_btn) as DatePickerButton }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.INSTANCE.graph.inject(this)
        lastUpdate = userPreferences.balanceReading
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        body = LayoutInflater.from(activity).inflate(R.layout.dialog_balance_reading, null) as LinearLayout

        lastUpdate?.let {
            amountInput.setText(java.lang.Float.toString(lastUpdate!!.balance))
            whenBtn.text = DateUtils.formatDayMonthYear(it.`when`!!)
            whenBtn.setDate(it.`when`)
        } ?: let {
            val today = Date()
            whenBtn.text = DateUtils.formatDayMonthYearWithPrefix(today)
            whenBtn.setDate(today)
        }

        return AlertDialog.Builder(activity).setTitle("Set starting balance")
                .setPositiveButton(getText(android.R.string.ok), this)
                .setNegativeButton(getText(android.R.string.cancel), null)
                .setView(body)
                .create()
    }

    override fun onStart() {
        super.onStart()
        amountInput.showKeyboard()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (validate()) {
            val selectedDate = whenBtn.selectedDate
            userPreferences.balanceReading = BalanceReading(selectedDate.clearLowerBits(),
                    amountInput.text.toString().toFloat())
            dismiss()
        }
    }

    private fun validate(): Boolean {
        if (TextUtils.isEmpty(amountInput.text)) {
            amountInput.error = "Please specify an amount!"
            amountInput.requestFocus()
            return false
        }

        val selectedDate = whenBtn.selectedDate
        if (!selectedDate.before(Date())) {
            Toast.makeText(activity, "Please select a non-future date!", Toast.LENGTH_SHORT).show()
            return false
        }

        val estimateDate = userPreferences.estimateDate
        if (selectedDate.after(estimateDate)) {
            Toast.makeText(activity, "Please select a date before the target date!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}
