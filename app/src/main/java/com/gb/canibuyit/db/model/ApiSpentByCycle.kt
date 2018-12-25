package com.gb.canibuyit.db.model

import com.gb.canibuyit.db.Contract
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.math.BigDecimal
import java.time.LocalDate

@DatabaseTable(tableName = Contract.SpentByCycle.TABLE)
data class ApiSpentByCycle(
    @DatabaseField(generatedId = true, columnName = Contract.Project._ID, canBeNull = false)
    var id: Int? = null,

    @DatabaseField(columnName = Contract.SpentByCycle.SPENDING, canBeNull = false, foreign = true)
    var spending: ApiSpending? = null,

    @DatabaseField(columnName = Contract.SpentByCycle.FROM, canBeNull = false, dataType = DataType.SERIALIZABLE)
    val from: LocalDate? = null,

    @DatabaseField(columnName = Contract.SpentByCycle.TO, canBeNull = false, dataType = DataType.SERIALIZABLE)
    val to: LocalDate? = null,

    @DatabaseField(columnName = Contract.SpentByCycle.AMOUNT, canBeNull = true)
    val amount: BigDecimal? = null)