package com.gb.canibuythat.ui.task;

import android.os.AsyncTask;

public abstract class TaskBase<R> extends AsyncTask<Void, Void, R> {

    private Callback<R> mCallback;
    private Throwable mError;

    public TaskBase(Callback<R> callback) {
        mCallback = callback;
    }

    @Override protected R doInBackground(Void... params) {
        try {
            return doWork();
        } catch (Throwable e) {
            e.printStackTrace();
            mError = e;
        }
        return null;
    }

    protected abstract R doWork() throws Exception;

    @Override protected void onPostExecute(R result) {
        if (mCallback != null) {
            if (mError != null) {
                mCallback.onError(mError);
            } else {
                if (result != null) {
                    mCallback.onSuccess(result);
                } else {
                    mCallback.onFailure();
                }
            }
        }
    }
}
