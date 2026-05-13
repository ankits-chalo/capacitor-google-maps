package com.capacitorjs.plugins.googlemaps

import BusesMarker
import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class CustomClusterManagerRenderer(
    private val context: Context,
    private val map: GoogleMap,
    clusterManager: ClusterManager<CapacitorGoogleMapMarker>
) : DefaultClusterRenderer<CapacitorGoogleMapMarker>(context, map, clusterManager) {

    private val clusterColor = Color.parseColor("#FE7C00")

    override fun onBeforeClusterItemRendered(item: CapacitorGoogleMapMarker, markerOptions: MarkerOptions) {
        val iconUrl = item.iconUrl

        when {
            iconUrl?.contains("buses_custom_marker") == true -> {
                val busesMarker = BusesMarker(context)
                markerOptions.icon(busesMarker.getMarkerIcon(item.getTitle(), iconUrl))
            }
            iconUrl?.contains("alert_custom_marker") == true -> {
                val alertMarker = AlertBusMarker(context)
                markerOptions.icon(alertMarker.getMarkerIcon(item.getTitle(), item.getSnippet(), iconUrl))
                markerOptions.title("")
            }
            iconUrl?.contains("alert_stop_custom_marker") == true -> {
                val alertStopMarker = AlertStopCustomMarker(context)
                markerOptions.icon(alertStopMarker.getMarkerIcon(item.getTitle(), item.getSnippet(), iconUrl))
                markerOptions.title("")
            }
            iconUrl?.contains("route_name") == true -> {
                val routeNameMarker = RouteNameMarker(context)
                markerOptions.icon(routeNameMarker.getMarkerIcon(item.getTitle()))
                markerOptions.anchor(0.5f, 1.0f)
                markerOptions.zIndex(1000f)
                markerOptions.title("")
            }
            iconUrl?.contains("overspeed_marker") == true -> {
                val overSpeedMarker = OverSpeedCustomMarker(context)
                val resId = context.resources.getIdentifier(
                    iconUrl,
                    "drawable",
                    context.packageName
                )
                markerOptions.icon(overSpeedMarker.getMarkerIcon(item.getTitle(), item.getSnippet(), resId))
                markerOptions.title("")
            }
            iconUrl?.contains("new_3d_marker") == true -> {
                val generator = DynamicMarkerGenerator(context)
                val safeColor = if (!item.markerBgColor.isNullOrEmpty()) {
                    try { Color.parseColor(item.markerBgColor) } catch (e: IllegalArgumentException) { Color.GRAY }
                } else {
                    Color.GRAY
                }
                val descriptor = generator.generateMarker(
                    busIconRes = R.drawable.ic_bus_white,
                    statusColor = safeColor,
                    angle = item.bearingAngle
                )
                markerOptions.icon(descriptor)
                markerOptions.anchor(
                    generator.getAnchor().first,
                    generator.getAnchor().second
                )
            }
            else -> {
                // Fallback: use the icon already set via getMarkerOptionsUpdated()
                item.markerOptions?.let {
                    if (it.icon != null) {
                        markerOptions.icon(it.icon)
                    }
                }
            }
        }

        // Set info window adapter based on infoIcon
        if (!item.infoIcon.isNullOrEmpty() && item.infoIcon != "not_show_info_window") {
            when {
                item.infoIcon == "buses_info_icon" -> {
                    map.setInfoWindowAdapter(BusesMarkerInfoWindow(context))
                }
                item.infoIcon?.contains("bus_alert_info") == true -> {
                    map.setInfoWindowAdapter(AlertMarkerInfoWindow(context))
                }
                item.infoIcon?.contains("last_updated_info") == true -> {
                    map.setInfoWindowAdapter(LastUpdatedInfoWindowAdapter(context))
                }
                item.infoIcon?.contains("stop_arrival_info") == true -> {
                    map.setInfoWindowAdapter(StopArrivalInfoWindowAdapter(context))
                }
                item.infoIcon?.contains("replay_info_icon") == true -> {
                    map.setInfoWindowAdapter(HistoryReplayInfoWindowAdapter(context))
                }
                else -> {
                    map.setInfoWindowAdapter(CustomInfoWindowAdapter(context))
                }
            }
        }
    }

    override fun onClusterItemUpdated(item: CapacitorGoogleMapMarker, marker: Marker) {
        marker.tag = item

        if (item.infoData?.optBoolean("showInfoIcon") == true) {
            marker.showInfoWindow()
        } else {
            marker.hideInfoWindow()
        }

        val iconUrl = item.iconUrl

        when {
            iconUrl?.contains("buses_custom_marker") == true -> {
                val busesMarker = BusesMarker(context)
                val newIcon = busesMarker.getMarkerIcon(item.getTitle(), iconUrl)
                marker.setIcon(newIcon)
            }
            iconUrl?.contains("alert_custom_marker") == true -> {
                val alertMarker = AlertBusMarker(context)
                val newIcon = alertMarker.getMarkerIcon(item.getTitle(), item.getSnippet(), iconUrl)
                marker.setIcon(newIcon)
            }
            iconUrl?.contains("alert_stop_custom_marker") == true -> {
                val alertStopMarker = AlertStopCustomMarker(context)
                val newIcon = alertStopMarker.getMarkerIcon(item.getTitle(), item.getSnippet(), iconUrl)
                marker.setIcon(newIcon)
            }
            iconUrl?.contains("route_name") == true -> {
                val routeNameMarker = RouteNameMarker(context)
                val newIcon = routeNameMarker.getMarkerIcon(item.getTitle())
                marker.setIcon(newIcon)
            }
            iconUrl?.contains("overspeed_marker") == true -> {
                val overSpeedMarker = OverSpeedCustomMarker(context)
                val resId = context.resources.getIdentifier(
                    iconUrl,
                    "drawable",
                    context.packageName
                )
                val newIcon = overSpeedMarker.getMarkerIcon(item.getTitle(), item.getSnippet(), resId)
                marker.setIcon(newIcon)
            }
            iconUrl?.contains("new_3d_marker") == true -> {
                val generator = DynamicMarkerGenerator(context)
                val safeColor = if (!item.markerBgColor.isNullOrEmpty()) {
                    try { Color.parseColor(item.markerBgColor) } catch (e: IllegalArgumentException) { Color.GRAY }
                } else {
                    Color.GRAY
                }
                val descriptor = generator.generateMarker(
                    busIconRes = R.drawable.ic_bus_white,
                    statusColor = safeColor,
                    angle = item.bearingAngle
                )
                marker.setIcon(descriptor)
            }
            else -> {
                // Fallback: use the icon from markerOptions if available
                item.markerOptions?.icon?.let {
                    marker.setIcon(it)
                }
            }
        }
    }

    override fun getColor(clusterSize: Int): Int {
        return clusterColor
    }
}
