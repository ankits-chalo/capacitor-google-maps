package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject
import java.util.Locale

class StopArrivalInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        val myMarker = marker.tag as? CapacitorGoogleMapMarker

        return myMarker?.let {
            if (it.infoIcon?.contains("stop_arrival_info") == true) {
                val view = LayoutInflater.from(context).inflate(R.layout.stop_arrival_info_window, null)

                // Find all the views from the updated XML layout
                val infoTitle = view.findViewById<TextView>(R.id.infoTitle)
                val infoSnippetArr = view.findViewById<TextView>(R.id.infoSnippetArr)
                val infoSnippetDep = view.findViewById<TextView>(R.id.infoSnippetDep)
                val arrivalContainer = view.findViewById<LinearLayout>(R.id.arrival_container)
                val departureContainer = view.findViewById<LinearLayout>(R.id.departure_container)
                val infoWindowImage = view.findViewById<ImageView>(R.id.info_window_image)

                // Set the title
                infoTitle.text = it.title ?: "NA"

                val arrivalTime = it.infoData?.getString("arrival_time")?.trim()
                val departureTime = it.infoData?.getString("departure_time")?.trim()

                // Set arrival and departure times, show 'NA' if empty or null
                infoSnippetArr.text = arrivalTime?.takeIf { time -> time.isNotEmpty() } ?: "NA"
                infoSnippetDep.text = departureTime?.takeIf { time -> time.isNotEmpty() } ?: "NA"


                arrivalContainer.visibility = View.VISIBLE
                departureContainer.visibility = View.VISIBLE

                view
            } else {
                // Return null to let Google Maps show the default info window
                null
            }
        }
    }
}