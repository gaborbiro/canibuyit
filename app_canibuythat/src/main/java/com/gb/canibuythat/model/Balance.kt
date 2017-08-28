package com.gb.canibuythat.model

import com.gb.canibuythat.ui.model.BalanceReading

class Balance(val balanceReading: BalanceReading? = null, val bestCaseBalance: Float = 0.toFloat(), val worstCaseBalance: Float = 0.toFloat())
