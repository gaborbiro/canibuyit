package com.gb.canibuythat.db.model

import com.gb.canibuythat.db.Contract
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = Contract.Savings.TABLE)
data class ApiSaving(
        @DatabaseField(generatedId = true, columnName = Contract.Project._ID, canBeNull = false)
        var id: Int? = null,
        @DatabaseField(columnName = Contract.Savings.SPENDING, canBeNull = false, foreign = true)
        var spending: ApiSpending? = null,
        @DatabaseField(columnName = Contract.Savings.AMOUNT, canBeNull = false)
        var amount: Double? = null,
        @DatabaseField(columnName = Contract.Savings.CREATED, canBeNull = false)
        var created: java.util.Date? = null,
        @DatabaseField(columnName = Contract.Savings.TARGET, canBeNull = false)
        var target: Double? = null
)