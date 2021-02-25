package com.kpstv.imageloaderview

import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.DrawableCompat

internal class Compat {
    companion object {
        fun setTint(drawable: Drawable, color: Int) {
            if (Build.VERSION.SDK_INT >= 21) {
                DrawableCompat.setTint(drawable, color)
            } else {
                drawable.mutate().colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        color,
                        BlendModeCompat.SRC_IN
                    )
            }
        }
    }
}