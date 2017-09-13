package com.gb.canibuythat.ui.model

import android.os.Parcel
import android.os.Parcelable
import com.gb.canibuythat.util.createParcel
import java.util.*

class BalanceReading(val `when`: Date?,
                     val balance: Float) : Parcelable {

    constructor(parcelIn: Parcel) : this(
            `when` = parcelIn.readSerializable() as Date,
            balance = parcelIn.readFloat()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(`when`)
        dest.writeFloat(balance)
    }

    companion object {
        val CREATOR = createParcel { BalanceReading(it) }
    }
}
