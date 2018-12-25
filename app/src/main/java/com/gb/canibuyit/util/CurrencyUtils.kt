package com.gb.canibuyit.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

class CurrencyUtils @Inject constructor() {

    fun formatDecimal(amount: BigDecimal, maxDecimals: Int = -1): String {
        val moneyFormatter = NumberFormat.getInstance()

        if (maxDecimals >= 0) {
            moneyFormatter.maximumFractionDigits = maxDecimals
        }
        return moneyFormatter.format(amount)
    }

    fun formatCurrency(amount: Float, currencyCode: String?, currencySymbol: String? = null, maxDecimals: Int = -1): String {
        val moneyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        currencyCode?.let { moneyFormatter.currency = Currency.getInstance(it) }
        if (maxDecimals >= 0) {
            moneyFormatter.maximumFractionDigits = maxDecimals
        }

        getSymbol(currencyCode, currencySymbol)?.let {
            val decimalFormatSymbols = (moneyFormatter as java.text.DecimalFormat).decimalFormatSymbols
            decimalFormatSymbols.currencySymbol = it
            moneyFormatter.decimalFormatSymbols = decimalFormatSymbols
        }

        return moneyFormatter.format(amount)
    }

    fun getSymbol(currencyCode: String?, currencySymbol: String?): String? {
        if (currencyCode == null) {
            return null
        }
        val symbolFromCode = Currency.getInstance(currencyCode).symbol
        return if (symbolFromCode != null && symbolFromCode != currencyCode) symbolFromCode else currencySymbol
    }
}
