package com.gb.canibuythat

import android.os.Environment

class AppConstants {
    companion object {
        const val DEFAULT_TIMEOUT_SECONDS: Long = 60

        val BACKUP_FOLDER
            get() = Environment.getExternalStorageDirectory().path + "/CanIBuyThat"
    }
}
