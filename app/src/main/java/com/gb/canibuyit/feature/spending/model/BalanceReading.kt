package com.gb.canibuyit.feature.spending.model

import android.os.Parcel
import android.os.Parcelable
import com.gb.canibuyit.util.createParcel
import java.time.LocalDate

class BalanceReading(val date: LocalDate?,
                     val balance: Float) : Parcelable {

    constructor(parcelIn: Parcel) : this(
            date = parcelIn.readSerializable() as LocalDate,
            balance = parcelIn.readFloat()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(date)
        dest.writeFloat(balance)
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { BalanceReading(it) }
    }
}
