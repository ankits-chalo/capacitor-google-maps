package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chalo.operatorapp.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    override fun getInfoContents(marker: Marker): View? {
        val myMarker = marker.tag as? CapacitorGoogleMapMarker

        return myMarker?.let {
            val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
            val imageView = view.findViewById<ImageView>(R.id.info_window_image)
            val titleTextView = view.findViewById<TextView>(R.id.info_window_title)
            val snippetTextView = view.findViewById<TextView>(R.id.info_window_snippet)

            titleTextView.text = it.title
            snippetTextView.text = it.snippet

            // Get the image from resources
            val resources: Resources = context.resources
            val resourceId: Int = resources.getIdentifier(it.infoIcon, "drawable", context.packageName)
            imageView.setImageDrawable(context.getDrawable(resourceId))

            view
        }
    }

}
