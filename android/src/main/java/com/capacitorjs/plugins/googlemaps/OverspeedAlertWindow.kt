package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class OverSpeedCustomMarker(private val context: Context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.bus_alert_info_window, null)
    private val alertTitle = view.findViewById<TextView>(R.id.alertTitle)
    private val alertSnippet = view.findViewById<TextView>(R.id.alertSnippet)
    private val alertIconImage = view.findViewById<ImageView>(R.id.alertIconImage)

    /**
     * Build a custom marker for overspeed events.
     * @param title (e.g. "Started at 10:30 AM")
     * @param snippet (e.g. "MG Road, Bangalore")
     * @param iconRes (drawable for overspeed marker, fallback to bus alert inactive if null)
     */
    fun getMarkerIcon(title: String, snippet: String?, iconRes: Int? = null): BitmapDescriptor {
        // Set title always
        alertTitle.text = title

        // Handle snippet gracefully
        if (snippet.isNullOrEmpty()) {
            alertSnippet.text = "Loading..."
        } else {
            alertSnippet.text = snippet
        }

        // Handle icon with fallback
        alertIconImage.setImageResource(iconRes ?: R.drawable.alert_bus_inactive)

        return getMarkerIconFromView(view)
    }

    private fun getMarkerIconFromView(view: android.view.View): BitmapDescriptor {
    // 1. Measure the view to get its original, desired dimensions in pixels (at mdpi).
    view.measure(
        android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
        android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
    )

    val originalWidth = view.measuredWidth
    val originalHeight = view.measuredHeight

    // Ensure dimensions are valid before proceeding
    if (originalWidth <= 0 || originalHeight <= 0) {
        return BitmapDescriptorFactory.defaultMarker()
    }
    
    // 2. Layout the view to its measured dimensions.
    view.layout(0, 0, originalWidth, originalHeight)

    // 3. Create the final bitmap with the original dimensions.
    // This is the bitmap we will eventually give to Google Maps.
    val finalBitmap = Bitmap.createBitmap(
        originalWidth,
        originalHeight,
        Bitmap.Config.ARGB_8888
    )

    // 4. Create a canvas for our final bitmap.
    val canvas = Canvas(finalBitmap)

    // 5. Draw the view onto the canvas.
    // The view will draw itself sharply because the Android framework handles
    // scaling text, drawables, etc., appropriately for the canvas's density.
    view.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(finalBitmap)
}
}
