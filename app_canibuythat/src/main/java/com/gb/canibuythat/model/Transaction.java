package com.gb.canibuythat.model;

import org.threeten.bp.ZonedDateTime;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Transaction {
    private float amount;
    private ZonedDateTime created;
    private String currency;
    private String description;
    private String id;
    private String merchant;
    private String notes;
    private boolean isLoad;
    private ZonedDateTime settled;
    private String category;
}
