package com.gb.canibuythat.repository;

import com.gb.canibuythat.api.model.ApiAccount;
import com.gb.canibuythat.api.model.ApiAccountsResponse;
import com.gb.canibuythat.api.model.ApiLogin;
import com.gb.canibuythat.model.Account;
import com.gb.canibuythat.model.Login;

public class MonzoMapper {

    public Login map(ApiLogin apiLogin) {
        return Login.builder().accessToken(apiLogin.getAccessToken()).refreshToken(apiLogin.getRefreshToken()).build();
    }

    public Account[] map(ApiAccountsResponse apiAccountsResponse) {
        Account[] result = new Account[apiAccountsResponse.getAccounts().length];

        for (int i = 0; i < apiAccountsResponse.getAccounts().length; i++) {
            ApiAccount apiAccount = apiAccountsResponse.getAccounts()[i];
            result[i] = Account.builder().id(apiAccount.getId()).created(apiAccount.getCreated()).description(apiAccount.getDescription()).build();
        }
        return result;
    }
}
