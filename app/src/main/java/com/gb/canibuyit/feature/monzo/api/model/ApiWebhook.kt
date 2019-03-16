package com.gb.canibuyit.feature.monzo.api.model

class ApiWebhooks(val webhooks: Array<ApiWebhook>)

class ApiWebhook(val id: String, val account_id: String, val url: String)