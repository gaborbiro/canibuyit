package com.gb.canibuythat.repository;

import com.gb.canibuythat.api.model.ApiAccount;
import com.gb.canibuythat.api.model.ApiAccountCollection;
import com.gb.canibuythat.api.model.ApiLogin;
import com.gb.canibuythat.api.model.ApiTransaction;
import com.gb.canibuythat.api.model.ApiTransactionCollection;
import com.gb.canibuythat.model.Account;
import com.gb.canibuythat.model.Login;
import com.gb.canibuythat.model.Transaction;

import org.apache.commons.lang3.text.WordUtils;
import org.threeten.bp.ZonedDateTime;

public class MonzoMapper {

    public Login map(ApiLogin apiLogin) {
        return Login.builder().accessToken(apiLogin.getAccessToken()).refreshToken(apiLogin.getRefreshToken()).build();
    }

    public Account[] map(ApiAccountCollection apiAccountCollection) {
        Account[] accounts = new Account[apiAccountCollection.getAccounts().length];

        for (int i = 0; i < apiAccountCollection.getAccounts().length; i++) {
            accounts[i] = map(apiAccountCollection.getAccounts()[i]);
        }
        return accounts;
    }

    public Account map(ApiAccount apiAccount) {
        return Account.builder().id(apiAccount.getId()).created(apiAccount.getCreated()).description(apiAccount.getDescription()).build();
    }

    public Transaction[] map(ApiTransactionCollection apiTransactionCollection) {
        Transaction[] transactions = new Transaction[apiTransactionCollection.getTransactions().length];


        return transactions;
    }

    public Transaction map(ApiTransaction apiTransaction) {
        return Transaction.builder()
                .amount(apiTransaction.getAmount() / 100.0f)
                .created(ZonedDateTime.parse(apiTransaction.getCreated()))
                .currency(apiTransaction.getCurrency())
                .description(apiTransaction.getDescription())
                .id(apiTransaction.getId())
                .merchant(apiTransaction.getMerchant())
                .notes(apiTransaction.getNotes())
                .isLoad(apiTransaction.isLoad())
                .settled(ZonedDateTime.parse(apiTransaction.getSettled()))
                .category(getModelCategory(apiTransaction.getCategory()))
                .build();
    }

    private static String getModelCategory(String apiCategory) {
        String noUnderscore = apiCategory.replaceAll("\\_", " ");
        return WordUtils.capitalizeFully(noUnderscore);
    }
}
