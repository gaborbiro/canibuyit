package com.gb.canibuythat.model

import org.threeten.bp.ZonedDateTime

class Transaction(val amount: Double,
                  val created: ZonedDateTime,
                  val currency: String,
                  val description: String?,
                  val id: String,
                  val merchant: String?,
                  val notes: String?,
                  val isLoad: Boolean?,
                  val settled: ZonedDateTime?,
                  val category: String)