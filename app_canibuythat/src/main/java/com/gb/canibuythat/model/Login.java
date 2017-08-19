package com.gb.canibuythat.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Login {
    private String accessToken;
    private String refreshToken;
}
