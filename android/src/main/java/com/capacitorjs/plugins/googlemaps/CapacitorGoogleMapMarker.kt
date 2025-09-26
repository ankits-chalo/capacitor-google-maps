package com.capacitorjs.plugins.googlemaps

import android.graphics.Color
import android.util.Size
import androidx.core.math.MathUtils
import BusesMarker
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.capacitorjs.plugins.googlemaps.R
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.ClusterItem
import org.json.JSONObject


class CapacitorGoogleMapMarker(val context: Context, fromJSONObject: JSONObject): ClusterItem {
    var coordinate: LatLng = LatLng(0.0, 0.0)
    var opacity: Float = 1.0f
    private var title: String
    private var snippet: String
    private var zIndex: Float = 1.0f
    var isFlat: Boolean = false
    var iconUrl: String? = null
    var iconSize: Size? = null
    var iconAnchor: CapacitorGoogleMapsPoint? = null
    var draggable: Boolean = false
    var isClustered: Boolean = true
    var googleMapMarker: Marker? = null
    var colorHue: Float? = null
    var markerOptions: MarkerOptions? = null
    var infoIcon: String? = null
    var infoData: JSONObject? = null
    var rotation: Int = 0
    var id: String? = null
    private var customAnchor: CapacitorGoogleMapsPoint = CapacitorGoogleMapsPoint(0.5F, 0.5F)

    init {
        if (!fromJSONObject.has("coordinate")) {
            throw InvalidArgumentsError("Marker object is missing the required 'coordinate' property")
        }

        val latLngObj = fromJSONObject.getJSONObject("coordinate")
        if (!latLngObj.has("lat") || !latLngObj.has("lng")) {
            throw InvalidArgumentsError("LatLng object is missing the required 'lat' and/or 'lng' property")
        }

        coordinate = LatLng(latLngObj.getDouble("lat"), latLngObj.getDouble("lng"))
        title = fromJSONObject.optString("title")
//        opacity = fromJSONObject.optDouble("opacity", 1.0).toFloat()
        snippet = fromJSONObject.optString("snippet")
        infoIcon = fromJSONObject.optString("infoIcon")
        infoData = fromJSONObject.optJSONObject("infoData")
        isFlat = fromJSONObject.optBoolean("isFlat", false)
        iconUrl = fromJSONObject.optString("iconUrl")
        if (fromJSONObject.has("iconSize")) {
            val iconSizeObject = fromJSONObject.getJSONObject("iconSize")
            iconSize = Size(iconSizeObject.optInt("width", 0), iconSizeObject.optInt("height", 0))
        }

        if (fromJSONObject.has("iconAnchor")) {
            val inputAnchorPoint = CapacitorGoogleMapsPoint(fromJSONObject.getJSONObject("iconAnchor"))
            iconAnchor = this.buildIconAnchorPoint(inputAnchorPoint)
        }

        if (fromJSONObject.has("anchor")) {
            customAnchor = CapacitorGoogleMapsPoint(fromJSONObject.getJSONObject("anchor"))
        }

        if (fromJSONObject.has("tintColor")) {
            val tintColorObject = fromJSONObject.getJSONObject("tintColor")

            val r = MathUtils.clamp(tintColorObject.optDouble("r", 0.00), 0.00, 255.0)
            val g = MathUtils.clamp(tintColorObject.optDouble("g", 0.00), 0.00, 255.0)
            val b = MathUtils.clamp(tintColorObject.optDouble("b", 0.00), 0.00, 255.0)

            val hsl = FloatArray(3)
            Color.RGBToHSV(r.toInt(), g.toInt(), b.toInt(), hsl)

            colorHue = hsl[0]
        }

        draggable = fromJSONObject.optBoolean("draggable", false)
        isClustered = fromJSONObject.optBoolean("isClustered", true)

        id = fromJSONObject.optString("id")
        zIndex = fromJSONObject.optDouble("zIndex", 1.0 ).toFloat()
        rotation = fromJSONObject.optInt("rotation")
    }

    override fun getPosition(): LatLng {
        return LatLng(coordinate.latitude, coordinate.longitude)
    }

    fun getMarkerId(): String? {
        return id
    }

    fun getRotation(): Int? {
        return rotation
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }

    override fun getZIndex(): Float {
        return zIndex
    }

    private fun buildIconAnchorPoint(iconAnchor: CapacitorGoogleMapsPoint): CapacitorGoogleMapsPoint? {
        iconSize ?: return null

        val u: Float = iconAnchor.x / iconSize!!.width
        val v: Float = iconAnchor.y / iconSize!!.height

        return CapacitorGoogleMapsPoint(u, v)
    }

    fun setPosition(latLong:LatLng) {
        googleMapMarker?.position =  latLong
    }

    fun setTitle(string: String) {
        googleMapMarker?.title =  string
    }

    fun getMarkerOptionsUpdated(): MarkerOptions {
        var markerOptions = MarkerOptions()
        markerOptions.position(coordinate)
        markerOptions.title(title)
        //Extra space is getting added if we add empty snippet
        if(!snippet.isNullOrEmpty()){
            markerOptions.snippet(snippet)
        }
        markerOptions.alpha(opacity)
        markerOptions.flat(isFlat)
        markerOptions.draggable(draggable)
        // Default value of anchor is 0.5, 0.5 and for placing the icon on bottom is 0.5, 1
        markerOptions.anchor(customAnchor.x, customAnchor.y)

        if (zIndex > 0) {
            markerOptions.zIndex(zIndex)
        }

        // Marker icon pickup and conversion to bitmap
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier(iconUrl, "drawable", context.packageName)
        if(iconUrl?.contains("arrow_marker") == true) {
            val arrowHeight = context.resources.getDimension(R.dimen.arrow_marker_height).toInt()
            val arrowWidth = context.resources.getDimension(R.dimen.arrow_marker_width).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, arrowWidth, arrowHeight, false)))
        }   else if(iconUrl?.contains("panic_marker") == true) {
            val arrowHeight = context.resources.getDimension(R.dimen.alert_marker_height).toInt()
            val arrowWidth = context.resources.getDimension(R.dimen.alert_marker_width).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, arrowWidth, arrowHeight, false)))
        }  else if(iconUrl?.contains("overspeed_marker") == true) {
            val arrowHeight = context.resources.getDimension(R.dimen.alert_marker_height).toInt()
            val arrowWidth = context.resources.getDimension(R.dimen.alert_marker_width).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, arrowWidth, arrowHeight, false)))
        }  else if(iconUrl?.contains("delayed_bus_marker") == true || iconUrl?.contains("active_bus_marker") == true || iconUrl?.contains("halt_bus_marker") == true || iconUrl?.contains("not_on_trip_bus_marker") == true) {
            val size = context.resources.getDimension(R.dimen.marker_size).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, size, size, false)))
        } else if(iconUrl?.contains("stop_marker") == true) {
            val size = context.resources.getDimension(R.dimen.stop_marker).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, size, size, false)))
        } else if(iconUrl?.contains("start_marker") == true || iconUrl?.contains("end_marker") == true) {
            val markerHeight = context.resources.getDimension(R.dimen.start_end_marker_height).toInt()
            val markerWidth = context.resources.getDimension(R.dimen.start_end_marker_width).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, markerWidth, markerHeight, false)))
        } else if(iconUrl?.contains("alert_bus") == true) {
            val markerHeight = context.resources.getDimension(R.dimen.alert_marker_height).toInt()
            val markerWidth = context.resources.getDimension(R.dimen.alert_marker_width).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            if(bitmap != null) {
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, markerWidth, markerHeight, false)))
            }
        } else if(iconUrl?.isEmpty() == true) {
            // When we need to show only the infoWindow

            // Hiding the default marker by making the alpha to 0
            markerOptions.alpha(0.0f)

            if(title.isNotEmpty()) {
                markerOptions.title(title)
            }
            if(snippet.isNotEmpty()) {
                markerOptions.snippet(snippet)
            }
        } else if(!iconUrl.isNullOrEmpty()) {
            if(resourceId != 0) {
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            }
        }
        return markerOptions
    }

    fun updateIcon(newIconName: String, title: String, snippet: String ) {
        iconUrl = newIconName
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier(iconUrl, "drawable", context.packageName)

        if(iconUrl?.contains("arrow_marker") == true) {
            val arrowHeight = context.resources.getDimension(R.dimen.arrow_marker_height).toInt()
            val arrowWidth = context.resources.getDimension(R.dimen.arrow_marker_width).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            googleMapMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, arrowWidth, arrowHeight, false)))
        } else if(iconUrl?.contains("bus_marker") == true) {
            val size = context.resources.getDimension(R.dimen.marker_size).toInt()
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            googleMapMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, size, size, false)))
        } else if(iconUrl?.contains("buses_custom_marker") == true) {
            val busesMarker = BusesMarker(context)
            googleMapMarker?.setIcon(busesMarker.getMarkerIcon(title, iconUrl!!))
        } else if(iconUrl?.contains("alert_custom_marker") == true) {
            val busesMarker = AlertBusMarker(context)
            googleMapMarker?.setIcon(busesMarker.getMarkerIcon(title, snippet, iconUrl!!))
        }  else if(iconUrl?.contains("alert_stop_custom_marker") == true) {
            val busesMarker = AlertStopCustomMarker(context)
            googleMapMarker?.setIcon(busesMarker.getMarkerIcon(title, snippet, iconUrl!!))
        } else if(iconUrl?.contains("overspeed_marker") == true) {
            val busesMarker = OverSpeedCustomMarker(context)
            val resId = context.resources.getIdentifier(iconUrl, "drawable", context.packageName)
            googleMapMarker?.setIcon(busesMarker.getMarkerIcon(title, snippet, resId))
        }

    }
}