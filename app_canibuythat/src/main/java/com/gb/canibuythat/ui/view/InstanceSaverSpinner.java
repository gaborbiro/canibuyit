package com.gb.canibuythat.ui.view;


import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.Spinner;


/**
 * Created by GABOR on 2015-febr.-07.
 */
public class InstanceSaverSpinner extends Spinner {

	public InstanceSaverSpinner(Context context) {
		super(context);
	}


	public InstanceSaverSpinner(Context context, int mode) {
		super(context, mode);
	}


	public InstanceSaverSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

    @NonNull
    @Override
    public Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }
}
