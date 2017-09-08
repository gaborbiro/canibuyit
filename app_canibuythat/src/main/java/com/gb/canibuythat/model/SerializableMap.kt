package com.gb.canibuythat.model

import java.io.Serializable
import java.util.*


class SerializableMap<K, V> : HashMap<K, V>(), Serializable {
    companion object {
        private val serialVersionUid: Long = 23876324978632L
    }
}
