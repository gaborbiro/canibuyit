package com.gb.canibuyit.feature.spending.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.gb.canibuyit.R

class ProgressRelativeLayout @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    internal var progress: Float = 0.0f
        set(value) {
            field = value
            mode = mode
            invalidate()
        }

    internal var mode: Mode = Mode.OFF
        set(value) {
            field = value
            when (value) {
                Mode.MIN_LIMIT -> {
                    when {
                        progress > 0.99 -> paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_high)
                        progress > 0.75 -> paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_medium)
                        else -> paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_low)
                    }
                }
                Mode.MAX_LIMIT -> {
                    when {
                        progress > 0.99 -> paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_low)
                        progress > 0.75 -> paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_medium)
                        else -> paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_high)
                    }
                }
                Mode.DEFAULT -> {
                    paint.color = ContextCompat.getColor(context, R.color.soba40)
                }
                Mode.OFF -> {
                    paint.color = ContextCompat.getColor(context, android.R.color.transparent)
                }
            }
            invalidate()
        }

    private val paint: Paint = Paint()
    private val path = Path()

    init {
        paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_low)
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(width.toFloat() * progress + 10, 0f)
        path.lineTo(width.toFloat() * progress - 10, height.toFloat())
        path.lineTo(0f, height.toFloat())
        canvas.drawPath(path, paint)
    }

    enum class Mode {
        MIN_LIMIT, MAX_LIMIT, DEFAULT, OFF
    }
}