package com.kpstv.imageloaderview

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
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
     * Set the per cycle animation duration for overlay tint animation.
     */
    public var overlayTintAnimDuration: Long = 700

    /**
     * Configure ripple color to be shown. Shown only when [selectable] is true.
     */
    public var rippleColor: Int = Color.WHITE
        set(value) {
            field = value
            rippleDrawable.updateColor(field)
        }

    /**
     * Setting this to true will not draw ripple event after it is [selectable].
     */
    public var disableRipple: Boolean = false

    /**
     * Makes the view clickable to accept touch inputs.
     */
    public var selectable: Boolean = false

    /**
     * Sets whether the view should shimmer.
     */
    public var isShimmering: Boolean = false
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
    public var isOverlayAlphaTinting: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) startOverlayTintAnimation() else cancelOverlayTintAnimation()
            }
        }

    private var viewBackgroundColor: Int = Color.GRAY

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
    private val rippleDrawable = RippleDrawable()
    @AnimationType
    private var animationType: Int = NONE

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                performClick()
                return isClickable
            }
            override fun onLongPress(e: MotionEvent?) {
                performLongClick()
                return super.onLongPress(e)
            }
        })

    init {
        shimmerDrawable.setShimmer(Shimmer.AlphaHighlightBuilder().build())
        shimmerDrawable.callback = this
        rippleDrawable.callback = this

        context.withStyledAttributes(attrs, R.styleable.ImageLoaderView, defStyleAttr) {
            animDuration = getInteger(R.styleable.ImageLoaderView_anim_duration, animDuration.toInt()).toLong()
            viewBackgroundColor = getColor(R.styleable.ImageLoaderView_backgroundColor, viewBackgroundColor)
            overlayDrawable = getDrawable(R.styleable.ImageLoaderView_overlay_drawable)?.mutate()
            if (hasValue(R.styleable.ImageLoaderView_ripple_color)) {
                val color = getColor(R.styleable.ImageLoaderView_ripple_color, rippleColor)
                rippleColor = color
            }
            overlayDrawableTint = getColor(R.styleable.ImageLoaderView_overlay_drawable_tint, overlayDrawableTint)
            overlayDrawableSecondaryTint =
                getColor(R.styleable.ImageLoaderView_overlay_drawable_secondary_tint, overlayDrawableSecondaryTint)
            overlayDrawablePadding =
                getDimension(R.styleable.ImageLoaderView_overlay_drawable_padding, overlayDrawablePadding)
            overlayTintAnimDuration =
                getInteger(R.styleable.ImageLoaderView_overlay_tinting_duration, overlayTintAnimDuration.toInt()).toLong()
            selectable = getBoolean(R.styleable.ImageLoaderView_selectable, selectable)
            disableRipple = getBoolean(R.styleable.ImageLoaderView_disable_ripple, disableRipple)

            val cornerRadius = getDimension(R.styleable.ImageLoaderView_corner_radius, 0f)
            for (i in cornerArray.indices) {
                cornerArray[i] = cornerRadius
            }

            isShimmering = getBoolean(R.styleable.ImageLoaderView_shimmering, false)
            isOverlayAlphaTinting = getBoolean(R.styleable.ImageLoaderView_overlay_tinting, false)
        }

        rippleDrawable.updateColor(rippleColor)
    }

    /**
     * Stops all the side effects going on.
     *
     * You need to call this manually to draw the image to the screen when
     * you don't call [setImageDrawable] with [animate] property to true.
     */
    public fun stopAllSideEffects() {
        isShimmering = false
        isOverlayAlphaTinting = false
    }

    /**
     * Directly set a overlay from drawable resource id.
     */
    public fun setOverlayResource(@DrawableRes resId: Int) {
        overlayDrawable = ContextCompat.getDrawable(context, resId)
    }

    /**
     * After animation is complete it will automatically stop any running side effects eg: Shimmer.
     */
    public fun setImageDrawable(
        drawable: Drawable?,
        @AnimationType animationType: Int,
        doAfterEnd: () -> Unit = {}
    ) {
        setImageDrawable(drawable)
        // Fake checking this implementation will be changed when there will be more
        // effects available.
        if (animationType > 0) {
            startAnimation(doAfterEnd)
        }
    }

    /**
     * After animation is complete it will automatically stop any running side effects eg: Shimmer.
     */
    public fun setImageBitmap(
        bm: Bitmap?,
        @AnimationType animationType: Int,
        doAfterEnd: () -> Unit = {}
    ) {
        setImageBitmap(bm)
        if (animationType > 0) {
            startAnimation(doAfterEnd)
        }
    }

    /**
     * After animation is complete it will automatically stop any running side effects eg: Shimmer.
     */
    public fun setImageResource(
        resId: Int,
        @AnimationType animationType: Int,
        doAfterEnd: () -> Unit = {}
    ) {
        setImageResource(resId)
        if (animationType > 0) {
            startAnimation(doAfterEnd)
        }
    }

    override fun getBackground(): Drawable = ColorDrawable(viewBackgroundColor)

    override fun setBackground(background: Drawable?) {
        if (background is ColorDrawable) {
            viewBackgroundColor = background.color
            invalidate()
        }
    }

    override fun setBackgroundColor(color: Int) {
        viewBackgroundColor = color
        invalidate()
    }


    override fun isClickable(): Boolean =
        selectable && !isLoading && !isShimmering && !isOverlayAlphaTinting

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        maxDimension = max(width, height)
        shimmerDrawable.setBounds(left, top, right, bottom)
        rippleDrawable.onLayout(width, height)
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
            } else if (drawable == null) {
                drawEmptyView(canvas)
            }
            if (isShimmering) {
                shimmerDrawable.draw(canvas)
                invalidate()
            }
        }

        if (rippleDrawable.isRippleDrawn) {
            rippleDrawable.draw(canvas)
            invalidate()
        }

        canvas.restoreToCount(cornerRestore)
    }

    private fun isAnyEffectOnGoing(): Boolean = isShimmering || isOverlayAlphaTinting

    private var pointX = 0f
    private var pointY = 0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && isClickable) {
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointX = event.x
                    pointY = event.y
                    if (!disableRipple) {
                        rippleDrawable.start(event.x, event.y)
                        invalidate()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.x - pointX) > ViewConfiguration.getTouchSlop()
                        || abs(event.y - pointY) > ViewConfiguration.getTouchSlop()
                    ) {
                        if (!disableRipple) {
                            rippleDrawable.cancel()
                        }
                    }
                }
            }
            return isClickable
        }
        return false
    }

    private fun drawEmptyView(canvas: Canvas) {
        canvas.drawColor(viewBackgroundColor)
        val overlay = overlayDrawable ?: return
        if (overlayDrawableTint != -1) Compat.setTint(overlay, overlayDrawableTint)
        overlay.draw(canvas)
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
        val drawableHeight =
            (overlayDrawable?.intrinsicHeight ?: 0) - overlayDrawablePadding.toInt()
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
                stopAllSideEffects()
                doAfterEnd()
            }
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

        overlayTintAnimator =
            ValueAnimator.ofInt(overlayDrawableTint, overlayDrawableSecondaryTint).apply {
                setEvaluator(ArgbEvaluator())
                addUpdateListener {
                    if (isLaidOut) {
                        val color = it.animatedValue as Int
                        Compat.setTint(drawable, color)
                        invalidate()
                    }
                }
                duration = overlayTintAnimDuration
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
    }

    public companion object {
        public const val NONE: Int = 0
        public const val CIRCLE_IN: Int = 1
    }
}