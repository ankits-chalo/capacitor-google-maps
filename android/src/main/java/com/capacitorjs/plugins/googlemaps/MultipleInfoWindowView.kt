package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.graphics.drawable.Drawable
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class MultipleInfoWindowView(private val context: Context) {

    fun createInfoWindowBitmap(marker: CapacitorGoogleMapMarker): Bitmap {
        // Inflate the XML layout
        val inflater = LayoutInflater.from(context)
        var container = inflater.inflate(R.layout.multiple_info_window, null) as LinearLayout
        if(marker.infoIcon?.contains("reverse") == true){
            container = inflater.inflate(R.layout.multiple_info_window_reverse, null) as LinearLayout
        }
        // Get views from layout
        val titleText = container.findViewById<TextView>(R.id.title_text)
        val snippetContainer = container.findViewById<LinearLayout>(R.id.snippet_container)
        val statusIcon = container.findViewById<ImageView>(R.id.status_icon)
        val snippetText = container.findViewById<TextView>(R.id.snippet_text)

        // Set title
        titleText.text = marker.title ?: ""

        // Handle snippet with status-based styling
        val hasSnippet = !marker.snippet.isNullOrEmpty()
        if (hasSnippet) {
            snippetContainer.visibility = View.VISIBLE
            snippetText.text = marker.snippet

            // Set status icon and text color based on infoIcon type
            when {
                marker.infoIcon?.contains("halt") == true -> {
                    val iconDrawable = getDrawableByName("alert_halted")
                    if (iconDrawable != null) {
                        statusIcon.setImageDrawable(iconDrawable)
                        statusIcon.visibility = View.VISIBLE
                    }
                    snippetText.setTextColor(Color.parseColor("#AD7400")) // Yellow
                }
                marker.infoIcon?.contains("inactiveGps") == true -> {
                    val iconDrawable = getDrawableByName("alert_inactive")
                    if (iconDrawable != null) {
                        statusIcon.setImageDrawable(iconDrawable)
                        statusIcon.visibility = View.VISIBLE
                    }
                    snippetText.setTextColor(Color.parseColor("#C62828"))
                }
                else -> {
                    statusIcon.visibility = View.GONE
                    snippetText.setTextColor(Color.DKGRAY) // Default color
                }
            }
        } else {
            snippetContainer.visibility = View.GONE
        }

        // Convert dp values to pixels for measurement
        val maxWidth = dpToPx(200) // 200dp
        val minWidth = dpToPx(80) // 80dp
        val minHeightSingleLine = dpToPx(40) // 40dp
        val minHeightMultiLine = dpToPx(60) // 60dp

        // First measure to get natural size
        container.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Get measured dimensions
        var measuredWidth = container.measuredWidth
        var measuredHeight = container.measuredHeight

        // Ensure minimum dimensions
        measuredWidth = measuredWidth.coerceAtLeast(minWidth)
        measuredWidth = measuredWidth.coerceAtMost(maxWidth)

        // Determine minimum height based on content
        val minHeight = if (!marker.snippet.isNullOrEmpty()) {
            minHeightMultiLine
        } else {
            minHeightSingleLine
        }
        measuredHeight = measuredHeight.coerceAtLeast(minHeight)

        // Re-measure with exact width to ensure proper text wrapping
        container.measure(
            View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Update height after exact width measurement
        measuredHeight = container.measuredHeight.coerceAtLeast(minHeight)

        // Final layout
        container.layout(0, 0, measuredWidth, measuredHeight)

        // Calculate shadow values based on a base padding
        val basePadding = dpToPx(8)
        val shadowPadding = basePadding
        val shadowRadius = basePadding * 0.75f
        val shadowOffsetY = basePadding * 0.5f

        val bitmapWidth = measuredWidth + shadowPadding * 2
        val bitmapHeight = measuredHeight + shadowPadding * 2

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val shadowHeightAdjustment = if (hasSnippet) {
            dpToPx(12)
        } else {
            dpToPx(6)
        }

        // Draw shadow with dynamic values
        drawShadow(canvas, measuredWidth, measuredHeight - shadowHeightAdjustment,
            shadowPadding, shadowRadius, shadowOffsetY)

        // Draw container on top of shadow (offset by shadow padding)
        canvas.save()
        canvas.translate(shadowPadding.toFloat(), shadowPadding.toFloat())
        container.draw(canvas)
        canvas.restore()

        return bitmap
    }

    private fun drawShadow(canvas: Canvas, width: Int, height: Int,
                           padding: Int, shadowRadius: Float, shadowOffsetY: Float) {

        // First shadow layer - subtle outer shadow
        val shadowPaint1 = Paint().apply {
            isAntiAlias = true
            color = Color.TRANSPARENT
            setShadowLayer(shadowRadius * 1.5f, 0f, shadowOffsetY * 0.5f, Color.argb(15, 0, 0, 0))
        }

        // Second shadow layer - main shadow
        val shadowPaint2 = Paint().apply {
            isAntiAlias = true
            color = Color.TRANSPARENT
            setShadowLayer(shadowRadius, 0f, shadowOffsetY, Color.argb(40, 0, 0, 0))
        }

        // Calculate corner radius based on padding
        val cornerRadius = padding * 1.25f

        // Create shadow rectangle with dynamic inset
        val shadowInset = (shadowRadius * 0.5f).toInt()
        val rect = RectF(
            (padding - shadowInset).toFloat(),
            (padding - shadowInset).toFloat(),
            (width + padding * 0.25 + shadowInset).toFloat(),
            (height ).toFloat()
        )

        // Draw shadow layers
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shadowPaint1)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shadowPaint2)
    }

    private fun getDrawableByName(drawableName: String): Drawable? {
        return try {
            val resourceId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
            if (resourceId != 0) {
                ContextCompat.getDrawable(context, resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}