package com.kpstv.imageloaderview

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withSave
import androidx.core.graphics.withScale
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import kotlin.math.abs
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate")
public class ImageLoaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /**
     * Set the overlay drawable which will be visible during effect
     */
    public var overlayDrawable: Drawable? = null
        set(value) {
            field = value
            requestLayout()
        }
    public var overlayDrawablePadding: Float = 0f
    public var overlayDrawableTint: Int = -1
    public var overlayDrawableSecondaryTint: Int = -1

    /**
     * Set the animation duration used by the setImage..(animate) methods.
     */
    public var animDuration: Long = 1200

    /**
     * Configure ripple color to be shown. Shown only when [selectable] is true.
     */
    public var rippleColor: Int = ColorUtils.setAlphaComponent(Color.WHITE, 50)
        set(value) {
            field = ColorUtils.setAlphaComponent(value, 50)
        }

    /**
     * Makes the view clickable to accept touch inputs.
     */
    public var selectable: Boolean = false

    /**
     * Sets whether the view should shimmer.
     */
    public var isShimmering : Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) shimmerDrawable.startShimmer() else shimmerDrawable.stopShimmer()
                invalidate()
            }
        }

    /**
     * Sets whether the overlay icon should start tinting animation.
     */
    public var isOverlayAlphaTinting : Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) startOverlayTintAnimation() else cancelOverlayTintAnimation()
            }
        }

    private var viewBackgroundColor: Int = Color.GRAY

    private val ripplePaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private var isLoading = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var clipScale = 0f
    private val overlayDrawableRect = Rect()

    private val clipPath = Path()
    private val shimmerDrawable = ShimmerDrawable()
    private var maxDimension: Int = 0

    private var cornerArray = FloatArray(8)
    private val cornerRectF = RectF() // to support API 19
    private val cornerPath = Path()
    private val ripplePath = Path()
    private var isRippleDrawn: Boolean = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            performClick()
            return true
        }
        override fun onLongPress(e: MotionEvent?) {
            performLongClick()
            return super.onLongPress(e)
        }
    })

    init {
        shimmerDrawable.setShimmer(Shimmer.AlphaHighlightBuilder().build())
        shimmerDrawable.callback = this

        context.withStyledAttributes(attrs, R.styleable.ImageLoaderView, defStyleAttr) {
            animDuration = getInteger(R.styleable.ImageLoaderView_anim_duration, 1200).toLong()
            viewBackgroundColor = getColor(R.styleable.ImageLoaderView_backgroundColor, Color.GRAY)
            overlayDrawable = getDrawable(R.styleable.ImageLoaderView_overlay_drawable)
            if (hasValue(R.styleable.ImageLoaderView_ripple_color)) {
                val color = getColor(R.styleable.ImageLoaderView_ripple_color, Color.WHITE)
                rippleColor = color
            }
            overlayDrawableTint = getColor(R.styleable.ImageLoaderView_overlay_drawable_tint, -1)
            overlayDrawableSecondaryTint = getColor(R.styleable.ImageLoaderView_overlay_drawable_secondary_tint, -1)
            overlayDrawablePadding = getDimension(R.styleable.ImageLoaderView_overlay_drawable_padding, 0f)
            selectable = getBoolean(R.styleable.ImageLoaderView_selectable, false)

            val cornerRadius = getDimension(R.styleable.ImageLoaderView_corner_radius, 0f)
            for (i in cornerArray.indices) {
                cornerArray[i] = cornerRadius
            }

            isShimmering = getBoolean(R.styleable.ImageLoaderView_shimmering, false)
            isOverlayAlphaTinting = getBoolean(R.styleable.ImageLoaderView_overlay_tinting, false)
        }

        ripplePaint.color = rippleColor
    }

    public fun setOverlayResource(@DrawableRes resId: Int) {
        overlayDrawable = ContextCompat.getDrawable(context, resId)
    }

    public fun setImageDrawable(
        drawable: Drawable?,
        animate: Boolean,
        doAfterEnd: () -> Unit = {}
    ) {
        setImageDrawable(drawable)
        if (animate) {
            startAnimation(doAfterEnd)
        }
    }

    public fun setImageBitmap(bm: Bitmap?, animate: Boolean, doAfterEnd: () -> Unit = {}) {
        setImageBitmap(bm)
        if (animate) {
            startAnimation(doAfterEnd)
        }
    }

    public fun setImageResource(resId: Int, animate: Boolean, doAfterEnd: () -> Unit = {}) {
        setImageResource(resId)
        if (animate) {
            startAnimation(doAfterEnd)
        }
    }

    override fun setBackgroundColor(color: Int) {
        viewBackgroundColor = color
    }


    override fun isClickable(): Boolean = selectable && !isLoading && !isShimmering && !isOverlayAlphaTinting

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        maxDimension = max(width, height)
        shimmerDrawable.setBounds(left, top, right, bottom)
        if (overlayDrawableTint != -1 && overlayDrawable != null)
            Compat.setTint(overlayDrawable!!, overlayDrawableTint)
        cornerRectF.set(0f, 0f, width.toFloat(), height.toFloat())
        cornerPath.addRoundRect(cornerRectF, cornerArray, Path.Direction.CW)
        calculateDrawableBounds()
    }

    override fun onDraw(canvas: Canvas) {
        val cornerRestore = canvas.save()
        if (cornerArray[0] != 0f) {
            canvas.clipPath(cornerPath)
        }
        canvas.withScale(scaleX + clipScale, scaleY + clipScale, width / 2f, height / 2f) {
            super.onDraw(canvas)
        }
        canvas.withSave {
            if (isLoading) {
                drawClipPath(this)
            }
            if (isAnyEffectOnGoing()) {
                canvas.drawColor(viewBackgroundColor)
                if (overlayDrawable != null) {
                    val count = canvas.save()
                    if (isLoading) {
                        canvas.scale(clipScale, clipScale, width / 2f, height / 2f)
                    }
                    overlayDrawable?.draw(canvas)
                    canvas.restoreToCount(count)
                }
            }
            if (isShimmering) {
                shimmerDrawable.draw(canvas)
                invalidate()
            }
        }
        if (isRippleDrawn) {
            canvas.drawPath(ripplePath, ripplePaint)
        }

        canvas.restoreToCount(cornerRestore)
    }

    private fun isAnyEffectOnGoing() : Boolean = isShimmering || isOverlayAlphaTinting

    private var pointX: Float = 0f
    private var pointY: Float = 0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && isClickable) {
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointX = event.x
                    pointY = event.y
                    startClickAnimation()
                }
                MotionEvent.ACTION_MOVE -> {
                   if (abs(event.x - pointX) > ViewConfiguration.getTouchSlop()
                       || abs(event.y - pointY) > ViewConfiguration.getTouchSlop()) {
                       isRippleDrawn = false
                       invalidate()
                   }
                }
            }
            return isClickable
        }
        return false
    }

    private fun drawClipPath(canvas: Canvas) {
        if (clipScale != 1f) {
            clipPath.reset()
            clipPath.addCircle(width / 2f, height / 2f, maxDimension * clipScale, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }
    }

    private fun calculateDrawableBounds() {
        val drawableWidth = (overlayDrawable?.intrinsicWidth ?: 0) - overlayDrawablePadding.toInt()
        val drawableHeight = (overlayDrawable?.intrinsicHeight ?: 0) - overlayDrawablePadding.toInt()
        overlayDrawableRect.set(
            width / 2 - drawableWidth,
            height / 2 - drawableHeight,
            width / 2 + drawableWidth,
            height / 2 + drawableHeight
        )
        overlayDrawable?.bounds = overlayDrawableRect
    }

    private fun startAnimation(doAfterEnd: () -> Unit) {
        isLoading = true
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = animDuration
            addUpdateListener {
                clipScale = it.animatedValue as Float
                invalidate()
            }
            doOnEnd {
                isLoading = false
                isShimmering = false
                isOverlayAlphaTinting = false
                doAfterEnd()
            }
            start()
        }
    }

    private fun startClickAnimation() {
        isRippleDrawn = true
        ValueAnimator.ofFloat(maxDimension / 3f, maxDimension * 1.5f).apply {
            addUpdateListener {
                val rippleRadius = it.animatedValue as Float
                ripplePath.addCircle(pointX, pointY, rippleRadius, Path.Direction.CW)
                invalidate()
            }
            doOnEnd {
                ripplePath.reset()
                isRippleDrawn = false
                invalidate()
            }
            duration = 260
            start()
        }
    }

    private var overlayTintAnimator: ValueAnimator? = null
    private fun cancelOverlayTintAnimation() {
        overlayTintAnimator?.cancel()
        invalidate()
    }
    private fun startOverlayTintAnimation() {
        cancelOverlayTintAnimation()

        val drawable = overlayDrawable ?: return
        if (overlayDrawableTint == -1 || overlayDrawableSecondaryTint == -1) return

        overlayTintAnimator = ValueAnimator.ofInt(overlayDrawableTint, overlayDrawableSecondaryTint).apply {
            setEvaluator(ArgbEvaluator())
            addUpdateListener {
               if (isLaidOut) {
                   val color = it.animatedValue as Int
                   Compat.setTint(drawable, color)
                   invalidate()
               }
            }
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }
}