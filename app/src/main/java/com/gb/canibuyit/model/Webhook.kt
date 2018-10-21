package com.gb.canibuyit.model

class Webhook(val id: String, val url: String) {
    override fun toString(): String {
        return "Webhook(id='$id', url='$url')"
    }
}