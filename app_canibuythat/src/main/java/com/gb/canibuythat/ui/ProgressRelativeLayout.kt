package com.gb.canibuythat.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.gb.canibuythat.R

class ProgressRelativeLayout @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {
    var progress: Float = 0.0f
        set(value) {
            if (value > 1) {
                field = 1f
            } else {
                field = value
            }
            if (value > 0.9) {
                paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_high)
            } else if (value > 0.75) {
                paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_medium)
            } else {
                paint.color = ContextCompat.getColor(context, R.color.spending_list_item_spend_low)
            }
            invalidate()
        }

    val paint: Paint = Paint()
    val path = Path()

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
}