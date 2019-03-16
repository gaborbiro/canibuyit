package com.gb.canibuyit.feature.project.model

import com.gb.canibuyit.feature.spending.persistence.Contract
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = Contract.Project.TABLE)
data class ApiProject(
    @DatabaseField(generatedId = true, columnName = Contract.Project._ID, canBeNull = true)
    var id: Int? = null,
    @DatabaseField(index = true, columnName = Contract.Project.NAME, unique = true, canBeNull = false)
    var name: String = "",
    @DatabaseField(index = true, columnName = Contract.Project.NAME_OVERRIDE, unique = true, canBeNull = false)
    var nameOverride: Boolean = false,
    @DatabaseField(index = true, columnName = Contract.Project.CATEGORY_OVERRIDE, unique = true, canBeNull = false)
    var categoryOverride: Boolean = false,
    @DatabaseField(index = true, columnName = Contract.Project.AVERAGE_OVERRIDE, unique = true, canBeNull = false)
    var averageOverride: Boolean = false,
    @DatabaseField(index = true, columnName = Contract.Project.CYCLE_OVERRIDE, unique = true, canBeNull = false)
    var cycleOverride: Boolean = false,
    @DatabaseField(index = true, columnName = Contract.Project.WHEN_OVERRIDE, unique = true, canBeNull = false)
    var whenOverride: Boolean = false
)