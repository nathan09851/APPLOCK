package com.nathan.applock.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun Drawable.toImageBitmapSafe(): ImageBitmap? {
    return try {
        val bitmap = if (this is BitmapDrawable && this.bitmap != null) {
            this.bitmap
        } else {
            val width = if (intrinsicWidth > 0) intrinsicWidth else 128
            val height = if (intrinsicHeight > 0) intrinsicHeight else 128
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bmp
        }
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
