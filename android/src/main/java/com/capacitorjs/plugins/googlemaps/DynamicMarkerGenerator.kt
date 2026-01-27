package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class DynamicMarkerGenerator(private val context: Context) {

    companion object {
        private const val CIRCLE_SIZE = 95       // Circle diameter
        private const val ARROW_WIDTH = 34        // Arrow width
        private const val ARROW_HEIGHT = 30       // Arrow height
        private const val GAP = 8                 // Gap between arrow and circle
        private const val BITMAP_WIDTH = 210      // Total bitmap width
        private const val BITMAP_HEIGHT = CIRCLE_SIZE + ARROW_HEIGHT * 2 + GAP * 2 + 46// Total height
        private val SHADOW_COLOR = Color.argb((0.4f * 255).toInt(), 0, 0, 0)
        private const val SHADOW_RADIUS = 8.909f
        private const val SHADOW_DX = 0f
        private const val SHADOW_DY = 0f
        private const val BUS_ICON_SCALE = 0.5f
    }

    /**
     * Generates a marker bitmap with an arrow pointing to the bus heading.
     * Arrow rotates around the bus circle center.
     *
     * @param busIconRes Drawable resource for bus icon (keeps original pixels)
     * @param statusColor Color for circle and arrow
     * @param angle Rotation angle for arrow (0-360°)
     */
    fun generateMarker(
        @DrawableRes busIconRes: Int,
        @ColorInt statusColor: Int,
        angle: Float
    ): BitmapDescriptor {

        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(true)
        val canvas = Canvas(bitmap)

        // ---------------- Draw rotated arrow ----------------
        drawArrow(canvas, statusColor, angle)

        // ---------------- Draw bus circle ----------------
        drawCircle(canvas, statusColor)

        // ---------------- Draw bus icon ----------------
        drawBusIcon(canvas, busIconRes)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // -------------------- PRIVATE HELPERS -------------------- //

    private fun drawArrow(canvas: Canvas, @ColorInt statusColor: Int, angle: Float) {
//        val shadowColor = Color.argb((0.4f * 255).toInt(), 0, 0, 0)
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            style = Paint.Style.FILL
            setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR)
        }

        // Bus circle center (pivot for rotation)
        val pivotX = BITMAP_WIDTH / 2f
        val pivotY = ARROW_HEIGHT + GAP + CIRCLE_SIZE / 2f

        // Arrow coordinates relative to pivot
        val arrowTipX = pivotX
        val arrowTipY = pivotY - CIRCLE_SIZE / 2f - GAP - ARROW_HEIGHT
        val arrowBaseY = pivotY - CIRCLE_SIZE / 2f - GAP
        val arrowLeft = pivotX - ARROW_WIDTH / 2f
        val arrowRight = pivotX + ARROW_WIDTH / 2f

        // Rotate canvas around bus center
        canvas.save()
        canvas.rotate(angle, pivotX, pivotY)

        val arrowPath = Path().apply {
            moveTo(arrowTipX, arrowTipY)      // tip
            lineTo(arrowLeft, arrowBaseY)     // left base
            lineTo(arrowRight, arrowBaseY)    // right base
            close()
        }

        canvas.drawPath(arrowPath, arrowPaint)
        canvas.restore()
    }

    private fun drawCircle(canvas: Canvas, @ColorInt statusColor: Int) {
        val circleCenterX = BITMAP_WIDTH / 2f
        val circleCenterY = ARROW_HEIGHT + GAP + CIRCLE_SIZE / 2f
        val radius = CIRCLE_SIZE / 2f
//        val shadowColor = Color.argb((0.4f * 255).toInt(), 0, 0, 0)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            style = Paint.Style.FILL
            setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR)
        }



        canvas.drawCircle(circleCenterX, circleCenterY, radius, circlePaint)
    }

    private fun drawBusIcon(canvas: Canvas, @DrawableRes busIconRes: Int) {
        val drawable = ContextCompat.getDrawable(context, busIconRes) ?: return
        val originalBitmap = drawableToBitmap(drawable)
        val circleCenterX = BITMAP_WIDTH / 2f
        val circleCenterY = ARROW_HEIGHT + GAP + CIRCLE_SIZE / 2f
        val targetSize = (CIRCLE_SIZE * BUS_ICON_SCALE).toInt()
        val bitmap = scaleBitmapPreserveRatio(originalBitmap, targetSize)
        val iconWidth = bitmap.width
        val iconHeight = bitmap.height

        val left = (circleCenterX - iconWidth / 2f).toInt()
        val top = (circleCenterY - iconHeight / 2f).toInt()
        val right = left + iconWidth
        val bottom = top + iconHeight

        drawable.setBounds(left, top, right, bottom)
        drawable.draw(canvas)
    }

    /**
     * Returns the anchor (u,v) for Google Maps marker.
     * Ensures marker aligns with bus circle center.
     */
    fun getAnchor(): Pair<Float, Float> {
        val pivotY = ARROW_HEIGHT + GAP + CIRCLE_SIZE / 2f
        val anchorX = 0.5f
        val anchorY = pivotY / BITMAP_HEIGHT.toFloat()
        return Pair(anchorX, anchorY)
    }
    private fun drawableToBitmap(
        drawable: Drawable
    ): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun scaleBitmapPreserveRatio(
        src: Bitmap,
        targetSize: Int
    ): Bitmap {
        val scale = minOf(
            targetSize.toFloat() / src.width,
            targetSize.toFloat() / src.height
        )

        val width = (src.width * scale).toInt()
        val height = (src.height * scale).toInt()

        return Bitmap.createScaledBitmap(src, width, height, true)
    }

}