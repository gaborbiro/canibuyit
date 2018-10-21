package com.gb.canibuyit.model

import java.io.Serializable
import java.util.*


class SerializableMap<K, V> : HashMap<K, V>(), Serializable {
    companion object {
        private val serialVersionUid: Long = 3061564712667237848
    }
}
