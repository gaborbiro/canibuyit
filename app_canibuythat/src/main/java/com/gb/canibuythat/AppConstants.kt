package com.gb.canibuythat

import android.os.Environment

const val SPENDINGS_MONTH_SPAN = 3
const val DEFAULT_TIMEOUT_SECONDS: Long = 60
val BACKUP_FOLDER
    get() = Environment.getExternalStorageDirectory().path + "/CanIBuyThat"

