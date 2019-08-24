package com.gb.canibuyit.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import com.gb.canibuyit.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.info_marker.view.*

@SuppressLint("ViewConstructor")
abstract class ClickableMarkerView(context: Context) : MarkerView(context, R.layout.info_marker) {

    var realLeft: Float = 0f
        private set

    var realTop: Float = 0f
        private set

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        content.text = text(e)
        super.refreshContent(e, highlight)
    }

    abstract fun text(e: Entry?): String

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }

    override fun draw(canvas: Canvas, posX: Float, posY: Float) {
        val offset = getOffsetForDrawingAtPoint(posX, posY)
        realLeft = posX + offset.x
        realTop = posY + offset.y
        super.draw(canvas, posX, posY)
    }
}