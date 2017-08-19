package com.gb.canibuythat.repository;

import com.gb.canibuythat.api.model.ApiLogin;
import com.gb.canibuythat.model.Login;

public class LoginMapper {

    public Login map(ApiLogin apiLogin) {
        return Login.builder().accessToken(apiLogin.getAccessToken()).refreshToken(apiLogin.getRefreshToken()).build();
    }
}
