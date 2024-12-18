package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.util.Locale

class HistoryReplayInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {


    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        val myMarker = marker.tag as? CapacitorGoogleMapMarker

        return myMarker?.let {
            if (it.infoIcon?.contains("replay_info_icon") == true) {
                val view = LayoutInflater.from(context).inflate(R.layout.replay_history_info_window, null)
                val lat_title = view.findViewById<TextView>(R.id.lat_title)
                val long_title = view.findViewById<TextView>(R.id.long_title)
                val time_title = view.findViewById<TextView>(R.id.time_title)
                val speed_title = view.findViewById<TextView>(R.id.speed_title)




                lat_title.text = it.infoData?.getString("latTitle")
                long_title.text = it.infoData?.getString("longTitle")
                time_title.text = it.infoData?.getString("timeTitle")
                speed_title.text = it.infoData?.getString("speedTitle")

                view
            } else {
                // Return null to let Google Maps show the default info window
                null
            }
        }
    }


}