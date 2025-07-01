package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.util.Locale

class LastUpdatedInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {
    private val geocoder = Geocoder(context, Locale.getDefault())

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        val myMarker = marker.tag as? CapacitorGoogleMapMarker

        return myMarker?.let {
            if (it.infoIcon?.contains("last_updated_info") == true) {
                val view = LayoutInflater.from(context).inflate(R.layout.last_updated_info, null)
                val infoTitle = view.findViewById<TextView>(R.id.infoTitle)
                val infoSnippet = view.findViewById<TextView>(R.id.infoSnippet)

                infoTitle.text = it.title
                infoSnippet.text = it.snippet

                view
            } else {
                // Return null to let Google Maps show the default info window
                null
            }
        }
    }


}