package com.gb.canibuyit.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtils {

    /**
     * @param onSave   will be run if user selects Yes
     * @param onFinish will be run if user selects Yes and onSave returns true OR if
     *                 user selects Discard (in which case onSave is ignored)
     */
    public static AlertDialog getSaveOrDiscardDialog(Context context, final String message, final Executable onSave, final Runnable onFinish) {
        return getDialog(context, "Save changes?", message,"Save",
                new ClickRunner.TaskBuilder(onSave).onSuccess(onFinish).build(), "Discard",
                new ClickRunner.Builder().setOnDiscard(onFinish).build(), android.R.string.cancel, null);
    }


    public static AlertDialog getErrorDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Error").setMessage(message).setPositiveButton(android.R.string.ok, null);
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
    public static AlertDialog getDialog(Context context, Object title, Object message, Object positiveButton, ClickRunner onPositiveButtonClicked,
                                        Object negativeButton, ClickRunner onNegativeButtonClicked, Object fuckOffButton, ClickRunner onFuckOffButtonClicked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (title != null) {
            if (title instanceof CharSequence) {
                builder.setTitle((CharSequence) title);
            } else if (title instanceof Integer) {
                builder.setTitle((int) title);
            } else {
                throw new IllegalArgumentException("Wrong title type in DialogUtils.getDialog(...)");
            }
        }
        if (message != null) {
            if (message instanceof CharSequence) {
                builder.setMessage((CharSequence) message);
            } else if (title instanceof Integer) {
                builder.setMessage((int) message);
            } else {
                throw new IllegalArgumentException("Wrong title type in DialogUtils.getDialog(...)");
            }
        }
        if (positiveButton != null) {
            if (positiveButton instanceof CharSequence) {
                builder.setPositiveButton((CharSequence) positiveButton, onPositiveButtonClicked);
            } else if (positiveButton instanceof Integer) {
                builder.setPositiveButton((int) positiveButton, onPositiveButtonClicked);
            } else {
                throw new IllegalArgumentException("Wrong positive button text type in DialogUtils.getDialog(...)");
            }
        }
        if (negativeButton != null) {
            if (negativeButton instanceof CharSequence) {
                builder.setNegativeButton((CharSequence) negativeButton, onNegativeButtonClicked);
            } else if (negativeButton instanceof Integer) {
                builder.setNegativeButton((int) negativeButton, onNegativeButtonClicked);
            } else {
                throw new IllegalArgumentException("Wrong negative button text type in DialogUtils.getDialog(...)");
            }
        }
        if (fuckOffButton != null) {
            if (fuckOffButton instanceof CharSequence) {
                builder.setNeutralButton((CharSequence) fuckOffButton, onFuckOffButtonClicked);
            } else if (fuckOffButton instanceof Integer) {
                builder.setNeutralButton((int) fuckOffButton, onFuckOffButtonClicked);
            } else {
                throw new IllegalArgumentException("Wrong fuck off button text type in DialogUtils.getDialog(...)");
            }
        }
        return builder.create();
    }

    public static abstract class Executable {
        public abstract boolean run();
    }

    private static class ClickRunner implements DialogInterface.OnClickListener {

        private final Executable conditionalTask;
        private final Runnable[] onSuccess;
        private final Runnable[] onFail;

        ClickRunner(Builder builder) {
            conditionalTask = null;
            onSuccess = builder.onDiscard;
            onFail = null;
        }

        ClickRunner(TaskBuilder taskBuilder) {
            conditionalTask = taskBuilder.conditionalTask;
            onSuccess = taskBuilder.onSuccess;
            onFail = taskBuilder.onFail;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            boolean result = conditionalTask == null || conditionalTask.run();

            if (result && onSuccess != null) {
                for (Runnable r : onSuccess) {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        Logger.INSTANCE.d(ClickRunner.class.getName(), t);
                    }
                }
            } else if (onFail != null) {
                for (Runnable r : onFail) {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        Logger.INSTANCE.d(ClickRunner.class.getName(), t);
                    }
                }
            }
        }

        static class Builder {
            private Runnable[] onDiscard;

            Builder setOnDiscard(Runnable... onDiscard) {
                this.onDiscard = onDiscard;
                return this;
            }

            ClickRunner build() {
                return new ClickRunner(this);
            }
        }

        static class TaskBuilder {
            private Executable conditionalTask;
            private Runnable[] onSuccess;
            private Runnable[] onFail;

            TaskBuilder(Executable onClickTask) {
                conditionalTask = onClickTask;
            }

            TaskBuilder onSuccess(Runnable... onSuccess) {
                this.onSuccess = onSuccess;
                return this;
            }

            public TaskBuilder onFail(Runnable... onFail) {
                this.onFail = onFail;
                return this;
            }

            ClickRunner build() {
                return new ClickRunner(this);
            }
        }
    }
}
