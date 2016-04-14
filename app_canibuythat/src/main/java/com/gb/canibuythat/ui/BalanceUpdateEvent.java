package com.gb.canibuythat.ui;

import com.gb.canibuythat.provider.Contract;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * The real status of the user's budget (bank account balance for ex) at a certain
 * point in time
 */
@DatabaseTable(tableName = Contract.BalanceUpdateEvent.TABLE)
public class BalanceUpdateEvent {

    @DatabaseField(generatedId = true, columnName = Contract.BalanceUpdateEvent._ID)
    public Integer id;

    @DatabaseField(index = true, columnName = Contract.BalanceUpdateEvent.WHEN,
            unique = true, canBeNull = false) public Date when;

    @DatabaseField(columnName = Contract.BalanceUpdateEvent.VALUE, canBeNull = false)
    public Float value;
}
