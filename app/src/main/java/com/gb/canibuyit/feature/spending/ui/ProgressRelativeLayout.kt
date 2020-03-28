package com.gb.canibuyit.feature.spending.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.gb.canibuyit.R
import kotlin.math.min

class ProgressRelativeLayout @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyleAttr: Int = 0) :
    RelativeLayout(context, attrs, defStyleAttr) {

    internal var progress: Float = 0.0f
        set(value) {
            field = value
            paints[0].color = getPaintColor()
            invalidate()
        }

    internal var mode: Mode = Mode.OFF
        set(value) {
            field = value
            paints[0].color = getPaintColor()
            invalidate()
        }

    private val paints: Array<Paint> = arrayOf(Paint(),
        Paint().apply { color = ContextCompat.getColor(context, R.color.beige_50) },
        Paint().apply { color = ContextCompat.getColor(context, R.color.apricot_50) },
        Paint().apply { color = ContextCompat.getColor(context, R.color.lavender_50) }
    )

    private val path = Path()

    init {
        paints[0].color = ContextCompat.getColor(context, R.color.spending_list_item_spend_low)
        paints[0].style = Paint.Style.FILL
    }

    private fun getPaintColor(): Int {
        return when (mode) {
            Mode.MIN_LIMIT -> {
                when {
                    progress > 0.99 ->
                        ContextCompat.getColor(context, R.color.spending_list_item_spend_high)
                    progress > 0.75 ->
                        ContextCompat.getColor(context, R.color.spending_list_item_spend_medium)
                    else ->
                        ContextCompat.getColor(context, R.color.spending_list_item_spend_low)
                }
            }
            Mode.MAX_LIMIT -> {
                when {
                    progress > 0.99 ->
                        ContextCompat.getColor(context, R.color.spending_list_item_spend_low)
                    progress > 0.75 ->
                        ContextCompat.getColor(context, R.color.spending_list_item_spend_medium)
                    else ->
                        ContextCompat.getColor(context, R.color.spending_list_item_spend_high)
                }
            }
            Mode.DEFAULT -> {
                ContextCompat.getColor(context, R.color.soba80_50)
            }
            Mode.OFF -> {
                ContextCompat.getColor(context, android.R.color.transparent)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i: Int in 0 until progress.toInt()) {
            path.reset()
            path.moveTo(0f, 0f)
            path.lineTo(width.toFloat(), 0f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            canvas.drawPath(path, paints[min(i, 3)])
        }
        val leftover = progress - progress.toInt()
        if (leftover > 0) {
            path.reset()
            path.moveTo(0f, 0f)
            path.lineTo(width.toFloat() * leftover + 10, 0f)
            path.lineTo(width.toFloat() * leftover - 10, height.toFloat())
            path.lineTo(0f, height.toFloat())
            canvas.drawPath(path, paints[min(progress.toInt(), 3)])
        }
    }

    enum class Mode {
        MIN_LIMIT, MAX_LIMIT, DEFAULT, OFF
    }
}