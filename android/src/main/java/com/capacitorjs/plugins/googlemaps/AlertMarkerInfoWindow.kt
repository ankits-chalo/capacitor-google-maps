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

class AlertMarkerInfoWindow(private val context: Context) : GoogleMap.InfoWindowAdapter {
    private val geocoder = Geocoder(context, Locale.getDefault())

    override fun getInfoContents(marker: Marker): View? {
        TODO("Not yet implemented")
    }

    override fun getInfoWindow(marker: Marker): View? {
        val myMarker = marker.tag as? CapacitorGoogleMapMarker

        return myMarker?.let {
            val view = LayoutInflater.from(context).inflate(R.layout.bus_alert_info_window, null)
            val alertTitle = view.findViewById<TextView>(R.id.alertTitle)
            val alertSnippet = view.findViewById<TextView>(R.id.alertSnippet)
            val alertIconImage = view.findViewById<ImageView>(R.id.alertIconImage)
            alertIconImage.visibility = View.GONE

            alertTitle.text = it.title
            alertSnippet.text = it.snippet
            // Set loading text initially
            if(!marker.snippet.isNullOrEmpty() && it.infoIcon?.contains("address") == true){
                alertSnippet.text = marker.snippet ?: "Loading..."
            }

            view
        }
    }


}