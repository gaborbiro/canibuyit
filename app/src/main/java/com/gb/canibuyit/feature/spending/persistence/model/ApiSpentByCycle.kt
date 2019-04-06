package com.gb.canibuyit.feature.spending.persistence.model

import com.gb.canibuyit.feature.spending.persistence.Contract
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

    @DatabaseField(columnName = Contract.SpentByCycle.FROM, canBeNull = false,
            dataType = DataType.SERIALIZABLE)
    var from: LocalDate? = null,

    @DatabaseField(columnName = Contract.SpentByCycle.TO, canBeNull = false,
            dataType = DataType.SERIALIZABLE)
    var to: LocalDate? = null,

    @DatabaseField(columnName = Contract.SpentByCycle.AMOUNT, canBeNull = false)
    var amount: BigDecimal? = null,

    @DatabaseField(columnName = Contract.SpentByCycle.COUNT, canBeNull = false)
    var count: Int? = null
) {

    override fun toString(): String {
        return "ApiSpentByCycle(id=$id, from=$from, to=$to, amount=$amount, count=$count)"
    }
}