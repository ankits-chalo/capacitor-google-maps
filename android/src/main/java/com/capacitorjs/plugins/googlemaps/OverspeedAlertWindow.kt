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

    fun getMarkerIcon(title: String, snippet: String?, iconRes: Int? = null): BitmapDescriptor {
        
        alertTitle.text = title

        if (snippet.isNullOrEmpty()) {
            alertSnippet.text = "Loading..."
        } else {
            alertSnippet.text = snippet
        }

        alertIconImage.setImageResource(iconRes ?: R.drawable.alert_bus_inactive)

        return getMarkerIconFromView(view)
    }

    private fun getMarkerIconFromView(view: android.view.View): BitmapDescriptor {
    // 1. Measuring the view to get its original, desired dimensions in pixels (at mdpi).
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

    // 3. Creating the final bitmap with the original dimensions.
    val finalBitmap = Bitmap.createBitmap(
        originalWidth,
        originalHeight,
        Bitmap.Config.ARGB_8888
    )

    // 4. Creating a canvas for our final bitmap.
    val canvas = Canvas(finalBitmap)

    view.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(finalBitmap)
}
}
