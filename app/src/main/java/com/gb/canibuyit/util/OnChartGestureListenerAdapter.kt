package com.gb.canibuyit.util

import android.view.MotionEvent
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener

abstract class OnChartGestureListenerAdapter : OnChartGestureListener {
    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {}

    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {}

    override fun onChartLongPressed(me: MotionEvent) {}

    override fun onChartDoubleTapped(me: MotionEvent) {}

    override fun onChartSingleTapped(me: MotionEvent) {}

    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {}

    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}

    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {}
}