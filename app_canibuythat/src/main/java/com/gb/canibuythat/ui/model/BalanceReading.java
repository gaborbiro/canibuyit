package com.gb.canibuythat.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class BalanceReading implements Parcelable {

    public final Date when;
    public final float balance;

    public BalanceReading(Date when, float balance) {
        this.when = when;
        this.balance = balance;
    }

    public BalanceReading(Parcel in) {
        when = (Date) in.readSerializable();
        balance = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(when);
        dest.writeFloat(balance);
    }

    public static final Parcelable.Creator<BalanceReading> CREATOR =
            new Parcelable.Creator<BalanceReading>() {
                @Override
                public BalanceReading createFromParcel(Parcel in) {
                    return new BalanceReading(in);
                }

                @Override
                public BalanceReading[] newArray(int size) {
                    return new BalanceReading[size];
                }
            };
}
