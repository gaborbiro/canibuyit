package com.gb.canibuyit

import android.os.Environment

const val DEFAULT_TIMEOUT_SECONDS: Long = 60
val BACKUP_FOLDER
    get() = Environment.getExternalStorageDirectory().path + "/CanIBuyIt"

