package com.gb.canibuythat.screen;

import com.gb.canibuythat.model.BudgetItem;

import java.util.List;

public interface BudgetListScreen extends Screen {

    void setData(List<BudgetItem> budgetItems);
}
