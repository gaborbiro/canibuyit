package com.gb.canibuythat.model

import java.io.Serializable
import java.util.HashMap

class SerializableMap<K, V> : HashMap<K, V>(), Serializable
