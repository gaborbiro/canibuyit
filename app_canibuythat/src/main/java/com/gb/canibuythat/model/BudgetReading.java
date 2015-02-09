package com.gb.canibuythat.model;


import java.util.Date;

import com.gb.canibuythat.provider.Contract;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


/**
 * The real status of the user's budget (bank account balance for ex) at a certain point in time
 */
@DatabaseTable(tableName = Contract.BudgetReading.TABLE)
public class BudgetReading {

	@DatabaseField(generatedId = true, columnName = Contract.BudgetReading._ID)
	public Integer	id;

	@DatabaseField(index = true, columnName = Contract.BudgetReading.DATE, unique = true, canBeNull = false)
	public Date		dateOfReading;

	@DatabaseField(columnName = Contract.BudgetReading.VALUE, canBeNull = false)
	public Float	value;
}
