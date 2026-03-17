package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors

class RouteNameMarker(private val context: Context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.route_name_info_window_marker, null)
    private val routeLabel = view.findViewById<TextView>(R.id.routeName)
    
    fun getMarkerIcon(text: String): BitmapDescriptor {
        routeLabel.text = text
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