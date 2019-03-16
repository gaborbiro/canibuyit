package com.gb.canibuyit.feature.monzo.model

class Webhooks(val webhooks: List<Webhook>)

class Webhook(val id: String, val url: String) {
    override fun toString(): String {
        return "Webhook(id='$id', url='$url')"
    }
}