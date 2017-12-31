package com.gb.canibuythat.repository

import com.gb.canibuythat.db.model.ApiSaving
import com.gb.canibuythat.exception.MapperException
import com.gb.canibuythat.model.Saving
import javax.inject.Inject

class SavingMapper @Inject constructor() {

    fun mapApiSaving(apiSaving: ApiSaving): Saving {
        return Saving(
                id = apiSaving.id?: throw MapperException("Missing saving id"),
                spendingId = apiSaving.spending?.id ?: throw MapperException("Missing spending id"),
                amount = apiSaving.amount ?: throw MapperException("Missing amount"),
                created = apiSaving.created ?: throw MapperException("Missing created"),
                target = apiSaving.target ?: throw MapperException("Missing target"))
    }
}