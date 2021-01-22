package com.gb.canibuyit.util

import java.math.BigDecimal
import kotlin.math.absoluteValue

fun Float.roundToDecimals(decimals: Int = 2) = "%.${decimals}f".format(this)
fun Double.roundToDecimals(decimals: Int = 2) = "%.${decimals}f".format(this)

fun BigDecimal.reverseSign() = (if (this > BigDecimal.ZERO) "+" else "") + abs().toString()
fun Int.reverseSign() = (if (this > 0) "+" else "") + absoluteValue.toString()
fun Float.reverseSign() = (if (this > 0) "+" else "") + absoluteValue.roundToDecimals()
fun String.readReverseSign() = toFloat().absoluteValue * (if (startsWith("+")) 1 else -1)