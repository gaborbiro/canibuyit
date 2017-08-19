package com.gb.canibuythat.di;

import com.gb.canibuythat.App;
import com.gb.canibuythat.ui.MainActivity;

public interface CanIBuyThatGraph {
    void inject(App app);

    void inject(MainActivity mainActivity);
}
