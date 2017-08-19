package com.gb.canibuythat.model;

import com.gb.canibuythat.ui.model.BalanceReading;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Balance {
    private BalanceReading balanceReading;
    private float bestCaseBalance;
    private float worstCaseBalance;
}
