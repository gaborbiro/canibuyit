package com.gb.canibuythat.util;


import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.gb.canibuythat.App;


/**
 * Created by GABOR on 2015-jan.-24.
 */
public class ViewUtils {

	public static void showKeyboard(final View view) {
		view.post(new Runnable() {

			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) App.getAppContext().getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
				view.requestFocus();
			}
		});
	}
}
