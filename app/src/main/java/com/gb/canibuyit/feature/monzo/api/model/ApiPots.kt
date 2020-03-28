package com.gb.canibuyit.feature.monzo.api.model

class ApiPots(
    val pots: List<ApiPot>
)

class ApiPot(
    val id: String,
    val name: String,
    val style: String,
    val balance: Long,
    val currency: String,
    val created: String,
    val updated: String,
    val deleted: Boolean
) {
    override fun toString(): String {
        return "ApiPot(name='$name', style='$style', balance=$balance, id='$id', currency='$currency', created='$created', updated='$updated', deleted=$deleted)"
    }
}