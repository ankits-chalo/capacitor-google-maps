package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class AlertBusMarker(private val context: Context) {
    private val customMarkerView = LayoutInflater.from(context).inflate(R.layout.bus_alert_marker, null)
    private val textView: TextView = customMarkerView.findViewById(R.id.busNumberText)
    private val snippetTextView: TextView = customMarkerView.findViewById(R.id.alertSnippet)
    private val cardView: CardView = customMarkerView.findViewById(R.id.busAlertCardView)
    private val busAlertParent: LinearLayout = customMarkerView.findViewById(R.id.busAlertParent)
    private val busAlertMarkerImage: ImageView = customMarkerView.findViewById(R.id.busAlertMarkerImage)
    private val ignitionImage: ImageView = customMarkerView.findViewById(R.id.ignitionImage)

    fun getMarkerIcon(text: String, snippet: String, iconUrl: String): BitmapDescriptor {
        textView.text = text
        snippetTextView.text = snippet

        if (!iconUrl.isNullOrEmpty()) {
            if (iconUrl.contains("red", ignoreCase = true)) {
                cardView.setCardBackgroundColor(Color.parseColor("#c62828"))
            } else { }

            val resources: Resources = context.resources
            if(iconUrl.contains("halt", ignoreCase = true)) {
                val resourceId: Int = resources.getIdentifier("alert_halt_yellow", "drawable", context.packageName)
                busAlertMarkerImage.setImageDrawable(context.getDrawable(resourceId))
            }
            if(iconUrl.contains("inactive", ignoreCase = true)) {
                val resourceId: Int = resources.getIdentifier("alert_bus_inactive_red", "drawable", context.packageName)
                busAlertMarkerImage.setImageDrawable(context.getDrawable(resourceId))
            }

            if(iconUrl.contains("ignition_off", ignoreCase = true)) {
                val resourceId: Int = resources.getIdentifier("alert_ignition_off", "drawable", context.packageName)
                ignitionImage.setImageDrawable(context.getDrawable(resourceId))
            } else if(iconUrl.contains("ignition_on", ignoreCase = true)) {
                val resourceId: Int = resources.getIdentifier("alert_ignition_on", "drawable", context.packageName)
                ignitionImage.setImageDrawable(context.getDrawable(resourceId))
            } else {
                ignitionImage.visibility = View.GONE
            }

        }

        return getMarkerIconFromView(customMarkerView)
    }

    private fun getMarkerIconFromView(view: View): BitmapDescriptor {
        // Shadow padding for extra space around the view
        val shadowPadding = 8 // Total padding (adjust if needed)

        // Measure and layout the view
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Create a bitmap large enough for the view and shadow
        val bitmapWidth = view.measuredWidth + shadowPadding * 2
        val bitmapHeight = view.measuredHeight + shadowPadding * 2
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a subtle shadow behind the view
        val paint = android.graphics.Paint().apply {
            color = Color.BLACK
            alpha = 80 // Reduced shadow opacity for subtler effect (range: 0-255)
            setShadowLayer(6f, 0f, 1f, Color.BLACK) // Reduced radius and offset for lighter shadow
            isAntiAlias = true
        }

        // Adjust the shadow rectangle to align properly with the view
        val cornerRadius = 75f // Corner radius for the shadow
        canvas.drawRoundRect(
            shadowPadding.toFloat(), // Start x
            shadowPadding.toFloat(), // Start y
            (bitmapWidth - shadowPadding).toFloat(), // End x
            (bitmapHeight - shadowPadding).toFloat(), // End y
            cornerRadius,
            cornerRadius,
            paint
        )

        // Draw the view on top of the shadow
        canvas.translate(shadowPadding.toFloat(), shadowPadding.toFloat())
        view.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}