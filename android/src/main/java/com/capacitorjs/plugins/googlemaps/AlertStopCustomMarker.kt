package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class AlertStopCustomMarker(private val context: Context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.bus_alert_info_window, null)
    private val alertTitle = view.findViewById<TextView>(R.id.alertTitle)
    private val alertSnippet = view.findViewById<TextView>(R.id.alertSnippet)

    fun getMarkerIcon(text: String, snippet: String, iconUrl: String): BitmapDescriptor {
        alertTitle.text = text
        alertSnippet.text = snippet
            // Set loading text initially
        if(snippet.isNullOrEmpty() && iconUrl.contains("address")){
            alertSnippet.text = snippet ?: "Loading..."
        }

        return getMarkerIconFromView(view)
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