package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.capacitorjs.plugins.googlemaps.CapacitorGoogleMapMarker
import com.capacitorjs.plugins.googlemaps.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject

class BusesMarkerInfoWindow(private val context: Context) : GoogleMap.InfoWindowAdapter {
    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        val myMarker = marker.tag as? CapacitorGoogleMapMarker

        return myMarker?.let {
            val view = LayoutInflater.from(context).inflate(R.layout.buses_marker_info_window, null)
            val collectionOccupancyLayout = view.findViewById<LinearLayout>(R.id.collectionOccupancyLayout)
            val ticketStatusLayout = view.findViewById<LinearLayout>(R.id.ticketStatusLayout)
            val lineView = view.findViewById<View>(R.id.lineView)
            val loading = it.infoData?.getBoolean("loading")
            val apiFail = it.infoData?.getBoolean("apiFail")
            val tripNotRunning = it.infoData?.getBoolean("tripNotRunning")
            val routeName = it.infoData?.getString("routeName")
            val totalCollctn = it.infoData?.getString("totalCollctn")
            val tripStartTime = it.infoData?.getString("tripStartTime")
            val currPsgCount = it.infoData?.getLong("currPsgCount")
            val luggageTicketCount = it.infoData?.getLong("luggageTicketCount")
            val occupancyLevel = it.infoData?.getString("occupancyLevel")
            val ticketStatus = it.infoData?.getString("ticketStatus")
            val collectionName = it.infoData?.getString("collectionName")
            val occupancyName = it.infoData?.getString("occupancyName")
            val viewDetailsName = it.infoData?.getString("viewDetailsName")
            val tripNotRunName = it.infoData?.getString("tripNotRunName")
            val loadingName = it.infoData?.getString("loadingName")


            val occupancyLevelImage = view.findViewById<ImageView>(R.id.occupancyLevelImage)
            val busName = view.findViewById<TextView>(R.id.busCardName)
            val busRouteName = view.findViewById<TextView>(R.id.busRouteName)
            val busTime = view.findViewById<TextView>(R.id.busTime)
            val busCollectionSoFar = view.findViewById<TextView>(R.id.collectionSoFar)
            val busCurrentOccupancy = view.findViewById<TextView>(R.id.currentOccupancy)
            val viewDetailsText = view.findViewById<TextView>(R.id.viewDetailsText)
            val loadingText = view.findViewById<TextView>(R.id.loadingText)
            val ticketStatusAlert = view.findViewById<TextView>(R.id.ticketStatusAlert)
            val collectionText = view.findViewById<TextView>(R.id.collectionText)
            val occupancyText = view.findViewById<TextView>(R.id.occupancyText)
            val tripNotRunningText = view.findViewById<TextView>(R.id.tripNotRunningText)
            val luggageCount = view.findViewById<TextView>(R.id.luggageCount)

            busName.text = it.title
            collectionText.text = collectionName
            occupancyText.text = occupancyName
            viewDetailsText.text = viewDetailsName
            tripNotRunningText.text = tripNotRunName
            loadingText.text = loadingName

            if(loading == true) {
                collectionOccupancyLayout.visibility = View.GONE;
                busTime.visibility = View.GONE;
                busRouteName.visibility = View.GONE;
                viewDetailsText.visibility = View.GONE;
                ticketStatusLayout.visibility = View.GONE;
                lineView.visibility = View.GONE;
                tripNotRunningText.visibility = View.GONE;
            } else if( apiFail == true) {
                collectionOccupancyLayout.visibility = View.GONE;
                busTime.visibility = View.GONE;
                busRouteName.visibility = View.GONE;
                viewDetailsText.visibility = View.GONE;
                ticketStatusLayout.visibility = View.GONE;
                lineView.visibility = View.GONE;
                tripNotRunningText.visibility = View.GONE;
                loadingText.text = "Failed to load, try again later"
                loadingText.setTextColor(Color.parseColor("#C62828"))
            } else if( tripNotRunning == true) {
                loadingText.visibility = View.GONE;
                collectionOccupancyLayout.visibility = View.GONE;
                busTime.visibility = View.GONE;
                busRouteName.visibility = View.GONE;
                viewDetailsText.visibility = View.GONE;
                ticketStatusLayout.visibility = View.GONE;
            } else {
                loadingText.visibility = View.GONE;
                tripNotRunningText.visibility = View.GONE;
                busRouteName.text = routeName
                busTime.text = tripStartTime
                busCollectionSoFar.text = totalCollctn
                luggageCount.text = luggageTicketCount.toString()
                busCurrentOccupancy.text = currPsgCount.toString()
                val resources: Resources = context.resources
                if(!occupancyLevel.isNullOrEmpty()) {
                    val resourceId: Int = resources.getIdentifier(occupancyLevel, "drawable", context.packageName)
                    occupancyLevelImage.setImageDrawable(context.getDrawable(resourceId))
                }
                if(ticketStatus.isNullOrEmpty()) {
                    ticketStatusLayout.visibility = View.GONE;
                } else {
                    ticketStatusAlert.text = ticketStatus
                }
            }



            view
        }
    }
}