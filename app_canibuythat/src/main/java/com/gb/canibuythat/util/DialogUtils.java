package com.gb.canibuythat.util;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;


/**
 * Created by GABOR on 2015-jan.-25.
 */
public class DialogUtils {

	public static AlertDialog getSaveOrDiscardDialog(Context context, final Runnable onSave, final Runnable onDiscard) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Save changes?")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						onSave.run();
					}
				}).setNegativeButton("Discard", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						onDiscard.run();
					}
				}).setNeutralButton("Cancel", null);
		return builder.create();
	}


	public static AlertDialog getErrorDialog(Context context, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Error").setMessage(message).setPositiveButton(android.R.string.ok, null);
		return builder.create();
	}
}
