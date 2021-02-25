package com.kpstv.imageloaderview

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.animation.doOnEnd
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

    init {
        alpha = 50
    }

    fun updateColor(color: Int) {
        ripplePaint.color = ColorUtils.setAlphaComponent(color, alpha)

        cancel()
        updateAnimator()
    }

    fun onLayout(width: Int, height: Int) {
        maxDimension = max(width, height)
        updateAnimator()
    }

    fun start(pointX: Float, pointY: Float) {
        if (callback == null) return

        cancel()
        this.pointX = pointX
        this.pointY = pointY

        valueAnimator?.start()
    }

    fun cancel() {
        valueAnimator?.cancel()
        ripplePath.reset()
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
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
            addUpdateListener {
                val rippleRadius = it.animatedValue as Float
                ripplePath.addCircle(pointX, pointY, rippleRadius, Path.Direction.CW)
                invalidateSelf()
            }
            doOnEnd {
                ripplePath.reset()
                invalidateSelf()
            }
            duration = 240
            start()
        }
    }
}