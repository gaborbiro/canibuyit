package com.gb.canibuythat.exception;

import android.content.Context;
import android.support.v4.app.FragmentManager;

public interface ContextSource {

    FragmentManager getSupportFragmentManager();

    Context getBaseContext();
}
