package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors

class RouteNameMarker(private val context: Context) : GoogleMap.InfoWindowAdapter {
    private val geocoder = Geocoder(context, Locale.getDefault())

    override fun getInfoContents(marker: Marker): View? {
        TODO("Not yet implemented")
    }

    override fun getInfoWindow(marker: Marker): View? {
        val myMarker = marker.tag as? CapacitorGoogleMapMarker

        return myMarker?.let {
            val view = LayoutInflater.from(context).inflate(R.layout.route_name_info_window_marker, null)
            val routeName = view.findViewById<TextView>(R.id.routeName)

            routeName.text = "Route : " + it.title

            view
        }
    }


}