package com.gb.canibuyit.error;

import android.content.Context;

import androidx.fragment.app.FragmentManager;

public interface ContextSource {

    FragmentManager getSupportFragmentManager();

    Context getBaseContext();
}
