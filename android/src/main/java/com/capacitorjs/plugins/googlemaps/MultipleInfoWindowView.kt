package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable

class MultipleInfoWindowView(private val context: Context) {

    fun createInfoWindowBitmap(marker: CapacitorGoogleMapMarker): Bitmap {
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER_VERTICAL
        container.setBackgroundColor(Color.WHITE)

        // Styling without border but with shadow
        val borderRadius = dpToPx(4)

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.WHITE)
        gradientDrawable.cornerRadius = borderRadius.toFloat()
        container.background = gradientDrawable

        // Calculate padding based on content
        val horizontalPadding = dpToPx(12)
        val verticalPadding = dpToPx(8)
        container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        // Create title with proper layout
        val titleText = TextView(context)
        titleText.text = marker.title ?: ""
        titleText.setTextColor(Color.BLACK)
        titleText.textSize = 14f
        titleText.setTypeface(null, Typeface.BOLD)
        titleText.maxLines = 2
        titleText.ellipsize = TextUtils.TruncateAt.END

        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, // Changed to MATCH_PARENT
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleParams.gravity = Gravity.CENTER_VERTICAL
        container.addView(titleText, titleParams)

        // Add snippet if available with status-based styling
        if (!marker.snippet.isNullOrEmpty()) {
            val snippetContainer = LinearLayout(context)
            snippetContainer.orientation = LinearLayout.HORIZONTAL
            snippetContainer.gravity = Gravity.CENTER_VERTICAL

            val snippetParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            snippetParams.topMargin = dpToPx(4)

            // Add status icon based on infoIcon type
            if (marker.infoIcon?.contains("halt") == true ||
                marker.infoIcon?.contains("inactiveGps") == true) {

                val statusIcon = ImageView(context)
                val iconSize = dpToPx(16)
                val iconParams = LinearLayout.LayoutParams(iconSize, iconSize)
                iconParams.marginEnd = dpToPx(6)
                iconParams.gravity = Gravity.TOP // Align icon to top for multi-line text

                // Set icon based on status using drawable resources
                val iconDrawable = when {
                    marker.infoIcon!!.contains("halt") ->
                        getDrawableByName("halt_bus_marker")
                    marker.infoIcon!!.contains("inactiveGps") ->
                        getDrawableByName("alert_bus_inactive")
                    else -> null
                }

                if (iconDrawable != null) {
                    statusIcon.setImageDrawable(iconDrawable)
                    snippetContainer.addView(statusIcon, iconParams)
                }
            }

            val snippetText = TextView(context)
            snippetText.text = marker.snippet
            snippetText.textSize = 12f
            snippetText.setTypeface(null, Typeface.BOLD)
            snippetText.maxLines = 3
            snippetText.ellipsize = TextUtils.TruncateAt.END

            // Set text color based on status
            when {
                marker.infoIcon?.contains("halt") == true -> {
                    snippetText.setTextColor(Color.parseColor("#FFA500")) // Orange/Yellow
                }
                marker.infoIcon?.contains("inactiveGps") == true -> {
                    snippetText.setTextColor(Color.RED)
                }
                else -> {
                    snippetText.setTextColor(Color.DKGRAY) // Default color
                }
            }

            val textParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, // Changed to MATCH_PARENT
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Remove weight since we're using MATCH_PARENT
            snippetContainer.addView(snippetText, textParams)
            container.addView(snippetContainer, snippetParams)
        }

        // Measure with proper constraints - use larger max width
        val maxWidth = dpToPx(400)

        // First measure to get natural size
        container.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Get measured dimensions
        var measuredWidth = container.measuredWidth
        var measuredHeight = container.measuredHeight

        // Ensure minimum dimensions but don't restrict maximum too much
        measuredWidth = measuredWidth.coerceAtLeast(dpToPx(80))
        measuredWidth = measuredWidth.coerceAtMost(maxWidth) // But don't exceed max

        // Re-measure with exact width to ensure proper text wrapping
        container.measure(
            View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Update height after exact width measurement
        measuredHeight = container.measuredHeight

        // Ensure minimum height
        val minHeight = if (!marker.snippet.isNullOrEmpty()) {
            dpToPx(60) // Increased minimum height for multi-line text
        } else {
            dpToPx(40)
        }
        measuredHeight = measuredHeight.coerceAtLeast(minHeight)

        // Final layout
        container.layout(0, 0, measuredWidth, measuredHeight)

        // Create bitmap with extra space for arrow and shadow
        val arrowHeight = dpToPx(8) // Height of the arrow
        val shadowPadding = dpToPx(4) // Shadow padding on all sides
        val totalWidth = measuredWidth + shadowPadding * 2
        val totalHeight = measuredHeight + arrowHeight + shadowPadding

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw shadow first
        drawShadow(canvas, measuredWidth, measuredHeight, totalWidth, totalHeight, shadowPadding, arrowHeight)

        // Draw container background (offset by shadow padding)
        canvas.save()
        canvas.translate(shadowPadding.toFloat(), shadowPadding.toFloat())
        container.draw(canvas)
        canvas.restore()

        // Draw arrow at the bottom, 40% from left (considering shadow offset)
        drawArrow(canvas, measuredWidth, measuredHeight, totalHeight, arrowHeight, shadowPadding)

        return bitmap
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

    private fun drawShadow(
        canvas: Canvas,
        width: Int,
        height: Int,
        totalWidth: Int,
        totalHeight: Int,
        shadowPadding: Int,
        arrowHeight: Int
    ) {
        val shadowRadius = dpToPx(4).toFloat()
        val borderRadius = dpToPx(4).toFloat()

        // Create shadow for main container
        val containerRect = RectF(
            shadowPadding.toFloat(),
            shadowPadding.toFloat(),
            shadowPadding + width.toFloat(),
            shadowPadding + height.toFloat()
        )

        val shadowPaint = Paint()
        shadowPaint.isAntiAlias = true
        shadowPaint.color = Color.TRANSPARENT
        shadowPaint.setShadowLayer(shadowRadius, 0f, 2f, Color.argb(100, 0, 0, 0))

        // Draw container shadow
        canvas.drawRoundRect(containerRect, borderRadius, borderRadius, shadowPaint)

        // Create shadow for arrow
        val arrowWidth = dpToPx(16)
        val arrowCenterX = width * 0.4f + shadowPadding
        val arrowLeft = arrowCenterX - arrowWidth / 2
        val arrowRight = arrowLeft + arrowWidth
        val arrowTop = shadowPadding + height.toFloat()
        val arrowBottom = totalHeight.toFloat()

        val arrowPath = Path()
        arrowPath.moveTo(arrowLeft, arrowTop)
        arrowPath.lineTo(arrowRight, arrowTop)
        arrowPath.lineTo(arrowCenterX, arrowBottom)
        arrowPath.lineTo(arrowLeft, arrowTop)
        arrowPath.close()

        // Draw arrow shadow
        canvas.drawPath(arrowPath, shadowPaint)
    }

    private fun drawArrow(
        canvas: Canvas,
        width: Int,
        containerHeight: Int,
        totalHeight: Int,
        arrowHeight: Int,
        shadowPadding: Int
    ) {
        val arrowWidth = dpToPx(16) // Width of the arrow

        // Calculate arrow position - 40% from left (considering shadow offset)
        val arrowCenterX = width * 0.4f + shadowPadding
        val arrowLeft = arrowCenterX - arrowWidth / 2
        val arrowRight = arrowLeft + arrowWidth
        val arrowTop = containerHeight.toFloat() + shadowPadding
        val arrowBottom = totalHeight.toFloat()

        val path = Path()
        path.moveTo(arrowLeft, arrowTop)
        path.lineTo(arrowRight, arrowTop)
        path.lineTo(arrowCenterX, arrowBottom)
        path.lineTo(arrowLeft, arrowTop)
        path.close()

        val paint = Paint()

        // Fill arrow with white
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        canvas.drawPath(path, paint)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}