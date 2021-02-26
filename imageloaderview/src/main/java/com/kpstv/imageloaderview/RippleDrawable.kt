 package com.kpstv.imageloaderview

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.graphics.ColorUtils
import kotlin.math.max

// Backward compatible to API 19
internal class RippleDrawable : Drawable() {
    private var ripplePath = Path()
    private var ripplePaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private var valueAnimator: ValueAnimator? = null
    private var pointX: Float = 0f
    private var pointY: Float = 0f
    private var maxDimension: Int = 0
    var isRippleDrawn = false

    private var alphaTint = 50

    fun updateColor(color: Int) {
        ripplePaint.color = ColorUtils.setAlphaComponent(color, alphaTint)
    }

    fun onLayout(width: Int, height: Int) {
        maxDimension = max(width, height)
        updateAnimator()
    }

    fun start(pointX: Float, pointY: Float) {
        if (callback == null && isRippleDrawn) return

        cancel()
        this.pointX = pointX
        this.pointY = pointY

        valueAnimator?.start()
    }

    fun cancel() {
        valueAnimator?.cancel()
        ripplePath.reset()
    }

    override fun setAlpha(alpha: Int) {
        alphaTint = alpha
        updateColor(ripplePaint.color)
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // No-op
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun draw(canvas: Canvas) {
        canvas.drawPath(ripplePath, ripplePaint)
    }

    private fun updateAnimator() {
        valueAnimator = ValueAnimator.ofFloat(maxDimension / 3f, maxDimension * 1.5f).apply {
            doOnStart {
                isRippleDrawn = true
                invalidateSelf()
            }
            addUpdateListener {
                val rippleRadius = it.animatedValue as Float
                ripplePath.addCircle(pointX, pointY, rippleRadius, Path.Direction.CW)
                invalidateSelf()
            }
            doOnEnd {
                isRippleDrawn = false
                ripplePath.reset()
                invalidateSelf()
            }
            duration = 240
        }
    }
}