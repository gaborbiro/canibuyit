package com.gb.canibuyit.error;

import android.content.Context;
import android.support.v4.app.FragmentManager;

public interface ContextSource {

    FragmentManager getSupportFragmentManager();

    Context getBaseContext();
}
