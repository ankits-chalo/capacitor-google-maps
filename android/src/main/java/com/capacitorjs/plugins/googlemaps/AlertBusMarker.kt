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
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class AlertBusMarker(private val context: Context) {
    private val customMarkerView = LayoutInflater.from(context).inflate(R.layout.bus_alert_marker, null)
    private val textView: TextView = customMarkerView.findViewById(R.id.busNumberText)
    private val snippetTextView: TextView = customMarkerView.findViewById(R.id.alertSnippet)
    private val cardView: CardView = customMarkerView.findViewById(R.id.busAlertCardView)
    private val busAlertMarkerImage: ImageView = customMarkerView.findViewById(R.id.busAlertMarkerImage)
    private val ignitionImage: ImageView = customMarkerView.findViewById(R.id.ignitionImage)

    fun getMarkerIcon(text: String, snippet: String, iconUrl: String): BitmapDescriptor {
        textView.text = text
        snippetTextView.text = snippet
        if (!iconUrl.isNullOrEmpty()) {
            if (iconUrl.contains("red", ignoreCase = true)) {
                cardView.setCardBackgroundColor(Color.parseColor("#c62828"))
            }

            val resources: Resources = context.resources
            if(iconUrl.contains("halt", ignoreCase = true)) {
                val resourceId: Int = resources.getIdentifier("alert_halt_yellow", "drawable", context.packageName)
                busAlertMarkerImage.setImageDrawable(context.getDrawable(resourceId))
            }
            if(iconUrl.contains("inactive", ignoreCase = true)) {
                val resourceId: Int = resources.getIdentifier("alert_bus_inactive", "drawable", context.packageName)
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

    private fun getMarkerIconFromView(view: android.view.View): BitmapDescriptor {
        view.measure(android.view.View.MeasureSpec.UNSPECIFIED, android.view.View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.buildDrawingCache()
        val returnedBitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight,
                Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        view.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(returnedBitmap)
    }
}