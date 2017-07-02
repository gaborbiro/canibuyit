package com.gb.canibuythat.ui.task;

import android.os.AsyncTask;

public abstract class TaskBase<R> extends AsyncTask<Void, Void, R> {
    private Callback<R> callback;
    private Throwable error;

    public TaskBase(Callback<R> callback) {
        this.callback = callback;
    }

    @Override
    protected R doInBackground(Void... params) {
        try {
            return doWork();
        } catch (Throwable e) {
            e.printStackTrace();
            error = e;
        }
        return null;
    }

    protected abstract R doWork() throws Exception;

    @Override
    protected void onPostExecute(R result) {
        if (callback != null) {
            if (error != null) {
                callback.onError(error);
            } else {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onFailure();
                }
            }
        }
    }
}
