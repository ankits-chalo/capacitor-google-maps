package com.capacitorjs.plugins.googlemaps

import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

class GoogleMapConfig(fromJSONObject: JSONObject) {
    var width: Int = 0
    var height: Int = 0
    var x: Int = 0
    var y: Int = 0
    var center: LatLng = LatLng(0.0, 0.0)
    var googleMapOptions: GoogleMapOptions? = null
    var zoom: Int = 0
    var liteMode: Boolean = false
    var devicePixelRatio: Float = 1.00f
    var styles: String? = null
    var mapId: String? = null
    var borderRadius: BorderRadius? = null

    data class BorderRadius(
        val topLeft: Float = 0f,
        val topRight: Float = 0f,
        val bottomLeft: Float = 0f,
        val bottomRight: Float = 0f
    )

    init {
        if (!fromJSONObject.has("width")) {
            throw InvalidArgumentsError(
                    "GoogleMapConfig object is missing the required 'width' property"
            )
        }

        if (!fromJSONObject.has("height")) {
            throw InvalidArgumentsError(
                    "GoogleMapConfig object is missing the required 'height' property"
            )
        }

        if (!fromJSONObject.has("x")) {
            throw InvalidArgumentsError(
                    "GoogleMapConfig object is missing the required 'x' property"
            )
        }

        if (!fromJSONObject.has("y")) {
            throw InvalidArgumentsError(
                    "GoogleMapConfig object is missing the required 'y' property"
            )
        }

        if (!fromJSONObject.has("zoom")) {
            throw InvalidArgumentsError(
                    "GoogleMapConfig object is missing the required 'zoom' property"
            )
        }

        if (fromJSONObject.has("devicePixelRatio")) {
            devicePixelRatio = fromJSONObject.getDouble("devicePixelRatio").toFloat()
        }

        if (!fromJSONObject.has("center")) {
            throw InvalidArgumentsError(
                    "GoogleMapConfig object is missing the required 'center' property"
            )
        }

        val centerJSONObject = fromJSONObject.getJSONObject("center")

        if (!centerJSONObject.has("lat") || !centerJSONObject.has("lng")) {
            throw InvalidArgumentsError(
                    "LatLng object is missing the required 'lat' and/or 'lng' property"
            )
        }

        liteMode =
                fromJSONObject.has("androidLiteMode") &&
                        fromJSONObject.getBoolean("androidLiteMode")

        width = fromJSONObject.getInt("width")
        height = fromJSONObject.getInt("height")
        x = fromJSONObject.getInt("x")
        y = fromJSONObject.getInt("y")
        zoom = fromJSONObject.getInt("zoom")

        val lat = centerJSONObject.getDouble("lat")
        val lng = centerJSONObject.getDouble("lng")
        center = LatLng(lat, lng)

        if (fromJSONObject.has("borderRadius")) {
            val borderRadiusObj = fromJSONObject.getJSONObject("borderRadius")
            borderRadius = BorderRadius(
                topLeft = borderRadiusObj.optDouble("topLeft", 0.0).toFloat(),
                topRight = borderRadiusObj.optDouble("topRight", 0.0).toFloat(),
                bottomLeft = borderRadiusObj.optDouble("bottomLeft", 0.0).toFloat(),
                bottomRight = borderRadiusObj.optDouble("bottomRight", 0.0).toFloat()
            )
        }

        val cameraPosition = CameraPosition(center, zoom.toFloat(), 0.0F, 0.0F)

        styles = fromJSONObject.getString("styles")

        mapId = fromJSONObject.getString("androidMapId")

        googleMapOptions = GoogleMapOptions().camera(cameraPosition).liteMode(liteMode)
        if (mapId != null) {
            googleMapOptions?.mapId(mapId!!)
        }
    }
}
