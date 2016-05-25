package com.gb.canibuythat.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtils {

    /**
     * @param onSave   will be run if user selects Yes
     * @param onFinish will be run if user selects Yes and onSave returns true OR if
     *                 user selects Discard (in which case onSave is ignored)
     */
    public static AlertDialog getSaveOrDiscardDialog(Context context,
            final Executable onSave, final Runnable onFinish) {
        return getDialog(context, "Save changes?", "Save",
                new ClickRunner.TaskBuilder(onSave).onSuccess(onFinish)
                        .build(), "Discard",
                new ClickRunner.Builder().setOnDiscard(onFinish)
                        .build(), android.R.string.cancel, null);
    }


    public static AlertDialog getErrorDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null);
        return builder.create();
    }

    /**
     * @param title                   CharSequence or resource id
     * @param positiveButton          CharSequence or resource id
     * @param onPositiveButtonClicked
     * @param negativeButton          CharSequence or resource id
     * @param onNegativeButtonClicked
     * @param fuckOffButton           CharSequence or resource id
     * @param onFuckOffButtonClicked
     */
    public static AlertDialog getDialog(Context context, Object title,
            Object positiveButton, ClickRunner onPositiveButtonClicked,
            Object negativeButton, ClickRunner onNegativeButtonClicked,
            Object fuckOffButton, ClickRunner onFuckOffButtonClicked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (title != null) {
            if (title instanceof CharSequence) {
                builder.setTitle((CharSequence) title);
            } else if (title instanceof Integer) {
                builder.setTitle((int) title);
            } else {
                throw new IllegalArgumentException(
                        "Wrong title type in DialogUtils.getDialog(...)");
            }
        }
        if (positiveButton != null) {
            if (positiveButton instanceof CharSequence) {
                builder.setPositiveButton((CharSequence) positiveButton,
                        onPositiveButtonClicked);
            } else if (positiveButton instanceof Integer) {
                builder.setPositiveButton((int) positiveButton, onPositiveButtonClicked);
            } else {
                throw new IllegalArgumentException(
                        "Wrong positive button text type in DialogUtils.getDialog(...)");
            }
        }
        if (negativeButton != null) {
            if (negativeButton instanceof CharSequence) {
                builder.setNegativeButton((CharSequence) negativeButton,
                        onNegativeButtonClicked);
            } else if (negativeButton instanceof Integer) {
                builder.setNegativeButton((int) negativeButton, onNegativeButtonClicked);
            } else {
                throw new IllegalArgumentException(
                        "Wrong negative button text type in DialogUtils.getDialog(...)");
            }
        }
        if (fuckOffButton != null) {
            if (fuckOffButton instanceof CharSequence) {
                builder.setNeutralButton((CharSequence) fuckOffButton,
                        onFuckOffButtonClicked);
            } else if (fuckOffButton instanceof Integer) {
                builder.setNeutralButton((int) fuckOffButton, onFuckOffButtonClicked);
            } else {
                throw new IllegalArgumentException(
                        "Wrong fuck off button text type in DialogUtils.getDialog(...)");
            }
        }
        return builder.create();
    }

    public static abstract class Executable {
        public abstract boolean run();
    }

    private static class ClickRunner implements DialogInterface.OnClickListener {

        private final Executable mConditionalTask;
        private final Runnable[] mOnSuccess;
        private final Runnable[] mOnFail;

        public ClickRunner(Builder builder) {
            mConditionalTask = null;
            mOnSuccess = builder.mOnDiscard;
            mOnFail = null;
        }

        public ClickRunner(TaskBuilder taskBuilder) {
            mConditionalTask = taskBuilder.mConditionalTask;
            mOnSuccess = taskBuilder.mOnSuccess;
            mOnFail = taskBuilder.mOnFail;
        }

        @Override public void onClick(DialogInterface dialog, int which) {
            boolean result = mConditionalTask == null || mConditionalTask.run();

            if (result && mOnSuccess != null) {
                for (Runnable r : mOnSuccess) {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        Logger.d(ClickRunner.class.getName(), t);
                    }
                }
            } else if (mOnFail != null) {
                for (Runnable r : mOnFail) {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        Logger.d(ClickRunner.class.getName(), t);
                    }
                }
            }
        }

        public static class Builder {
            private Runnable[] mOnDiscard;

            public Builder setOnDiscard(Runnable... onDiscard) {
                mOnDiscard = onDiscard;
                return this;
            }

            public ClickRunner build() {
                return new ClickRunner(this);
            }
        }

        public static class TaskBuilder {
            private Executable mConditionalTask;
            private Runnable[] mOnSuccess;
            private Runnable[] mOnFail;

            public TaskBuilder(Executable onClickTask) {
                mConditionalTask = onClickTask;
            }

            public TaskBuilder onSuccess(Runnable... onSuccess) {
                mOnSuccess = onSuccess;
                return this;
            }

            public TaskBuilder onFail(Runnable... onFail) {
                mOnFail = onFail;
                return this;
            }

            public ClickRunner build() {
                return new ClickRunner(this);
            }
        }
    }
}
