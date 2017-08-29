package com.gb.canibuythat.screen;

import com.gb.canibuythat.model.Spending;

import java.util.List;

public interface SpendingListScreen extends Screen {

    void setData(List<Spending> spendings);
}
