package com.gb.canibuythat.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Account {
    private String id;
    private String description;
    private String created;
}
