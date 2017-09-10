package com.gb.canibuythat.model

import com.gb.canibuythat.db.Contract
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = Contract.Project.TABLE)
class Project(
        @DatabaseField(generatedId = true, columnName = Contract.Project._ID, canBeNull = true)
        var id: Int? = null,
        @DatabaseField(index = true, columnName = Contract.Project.NAME, unique = true, canBeNull = false)
        var name: String? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Project

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }
}