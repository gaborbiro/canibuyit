package com.gb.canibuyit.base.model

import java.io.Serializable
import java.util.HashMap

class SerializableMap<K, V> : HashMap<K, V>(), Serializable {
    companion object {
        private val serialVersionUid: Long = 3061564712667237848
    }
}
