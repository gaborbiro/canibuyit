package com.gb.canibuyit.feature.spending.persistence.model

import com.gb.canibuyit.feature.spending.persistence.Contract
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.math.BigDecimal
import java.time.LocalDate

@DatabaseTable(tableName = Contract.Savings.TABLE)
class DBSaving(

    @DatabaseField(generatedId = true, columnName = Contract.Project._ID, canBeNull = false)
    var id: Int? = null,

    @DatabaseField(columnName = Contract.Savings.SPENDING, canBeNull = false, foreign = true)
    var spending: DBSpending? = null,

    @DatabaseField(columnName = Contract.Savings.AMOUNT, canBeNull = false)
    var amount: BigDecimal? = null,

    @DatabaseField(columnName = Contract.Savings.CREATED, canBeNull = false, dataType = DataType.SERIALIZABLE)
    var created: LocalDate? = null,

    @DatabaseField(columnName = Contract.Savings.TARGET, canBeNull = false)
    var target: Int? = null
)