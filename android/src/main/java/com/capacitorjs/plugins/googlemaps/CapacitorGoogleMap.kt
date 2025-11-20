package com.capacitorjs.plugins.googlemaps

import BusesMarker
import BusesMarkerRenderer
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.getcapacitor.Bridge
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Locale

class CapacitorGoogleMap(
    val id: String,
    val config: GoogleMapConfig,
    val delegate: CapacitorGoogleMapsPlugin
) :
    OnCameraIdleListener,
    OnCameraMoveStartedListener,
    OnCameraMoveListener,
    OnMyLocationButtonClickListener,
    OnMyLocationClickListener,
    OnMapReadyCallback,
    OnMapClickListener,
    OnMarkerClickListener,
    OnMarkerDragListener,
    OnInfoWindowClickListener,
    OnInfoWindowCloseListener,
    OnCircleClickListener,
    OnPolylineClickListener,
    OnPolygonClickListener
{
    private var mapView: MapView
    private var googleMap: GoogleMap? = null
    private val markers = HashMap<String, CapacitorGoogleMapMarker>()
    private val polygons = HashMap<String, CapacitorGoogleMapsPolygon>()
    private val circles = HashMap<String, CapacitorGoogleMapsCircle>()
    private val polylines = HashMap<String, CapacitorGoogleMapPolyline>()
    private val markerIcons = HashMap<String, Bitmap>()
    private val infoWindowMarkers = HashMap<String, Marker>()
    private var multipleInfoWindowZoomLevel: Float = 13.5f
    private lateinit var multipleInfoWindowView: MultipleInfoWindowView
    private var clusterManager: ClusterManager<CapacitorGoogleMapMarker>? = null
    private var markerIdOnWeb = ArrayList<String>()
    private var markerIdNotOnCluster = ArrayList<String>()
    private var animator: Animator? = null
    private var polylineCords: MutableList<LatLng> = mutableListOf()

    private val isReadyChannel = Channel<Boolean>()
    private var debounceJob: Job? = null

    init {
        val bridge = delegate.bridge

        mapView = MapView(bridge.context, config.googleMapOptions)
        multipleInfoWindowView = MultipleInfoWindowView(bridge.context)
        initMap()
        setListeners()
    }

    private fun initMap() {
        runBlocking {
            val job =
                CoroutineScope(Dispatchers.Main).launch {
                    mapView.onCreate(null)
                    mapView.onStart()
                    mapView.getMapAsync(this@CapacitorGoogleMap)
                    mapView.setWillNotDraw(false)
                    isReadyChannel.receive()

                    render()
                }

            job.join()
        }
    }

    private fun render() {
        runBlocking {
            CoroutineScope(Dispatchers.Main).launch {
                val bridge = delegate.bridge
                val mapViewParent = FrameLayout(bridge.context)
                mapViewParent.minimumHeight = bridge.webView.height
                mapViewParent.minimumWidth = bridge.webView.width

                val layoutParams =
                    FrameLayout.LayoutParams(
                        getScaledPixels(bridge, config.width),
                        getScaledPixels(bridge, config.height),
                    )
                layoutParams.leftMargin = getScaledPixels(bridge, config.x)
                layoutParams.topMargin = getScaledPixels(bridge, config.y)

                mapViewParent.tag = id

                val borderRadius = config.borderRadius ?: GoogleMapConfig.BorderRadius()
                mapView.background = createRoundedBackground(
                    topLeftRadius = getScaledPixelsF(bridge, borderRadius.topLeft),
                    topRightRadius = getScaledPixelsF(bridge, borderRadius.topRight),
                    bottomLeftRadius = getScaledPixelsF(bridge, borderRadius.bottomLeft),
                    bottomRightRadius = getScaledPixelsF(bridge, borderRadius.bottomRight)
                )
                mapView.clipToOutline = true

                mapView.layoutParams = layoutParams
                mapViewParent.addView(mapView)

                ((bridge.webView.parent) as ViewGroup).addView(mapViewParent)

                bridge.webView.bringToFront()
                bridge.webView.setBackgroundColor(Color.TRANSPARENT)
                if (config.styles != null) {
                    googleMap?.setMapStyle(MapStyleOptions(config.styles!!))
                }
            }
        }
    }

    fun updateRender(updatedBounds: RectF) {
        this.config.x = updatedBounds.left.toInt()
        this.config.y = updatedBounds.top.toInt()
        this.config.width = updatedBounds.width().toInt()
        this.config.height = updatedBounds.height().toInt()

        runBlocking {
            CoroutineScope(Dispatchers.Main).launch {
                val bridge = delegate.bridge
                val mapRect = getScaledRect(bridge, updatedBounds)
                val mapView = this@CapacitorGoogleMap.mapView;
                mapView.x = mapRect.left
                mapView.y = mapRect.top
                if (mapView.layoutParams.width != config.width || mapView.layoutParams.height != config.height) {
                    mapView.layoutParams.width = getScaledPixels(bridge, config.width)
                    mapView.layoutParams.height = getScaledPixels(bridge, config.height)
                    mapView.requestLayout()
                }
            }
        }
    }

    fun dispatchTouchEvent(event: MotionEvent) {
        CoroutineScope(Dispatchers.Main).launch {
            val offsetViewBounds = getMapBounds()

            val relativeTop = offsetViewBounds.top
            val relativeLeft = offsetViewBounds.left

            event.setLocation(event.x - relativeLeft, event.y - relativeTop)
            mapView.dispatchTouchEvent(event)
        }
    }

    fun bringToFront() {
        CoroutineScope(Dispatchers.Main).launch {
            val mapViewParent =
                ((delegate.bridge.webView.parent) as ViewGroup).findViewWithTag<ViewGroup>(
                    this@CapacitorGoogleMap.id
                )
            mapViewParent.bringToFront()
        }
    }

    fun destroy() {
        runBlocking {
            val job =
                CoroutineScope(Dispatchers.Main).launch {
                    val bridge = delegate.bridge

                    val viewToRemove: View? =
                        ((bridge.webView.parent) as ViewGroup).findViewWithTag(id)
                    if (null != viewToRemove) {
                        ((bridge.webView.parent) as ViewGroup).removeView(viewToRemove)
                    }
                    mapView.onDestroy()
                    googleMap = null
                    clusterManager = null
                }

            job.join()
        }
    }

    private fun createRoundedBackground(topLeftRadius: Float, topRightRadius: Float,bottomLeftRadius: Float,bottomRightRadius: Float): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        // Set different radii for each corner: top-left, top-right, bottom-right, bottom-left
        drawable.cornerRadii = floatArrayOf(
            topLeftRadius, topLeftRadius,
            topRightRadius, topRightRadius,
            bottomLeftRadius, bottomLeftRadius,
            bottomRightRadius, bottomRightRadius
        )
        drawable.setColor(Color.TRANSPARENT)
        return drawable
    }

    fun addMarkers(
        newMarkers: List<CapacitorGoogleMapMarker>,
        callback: (ids: Result<List<String>>) -> Unit
    ) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            val markerIds: MutableList<String> = mutableListOf()

            CoroutineScope(Dispatchers.Main).launch {


                // Collect IDs of new markers for comparison
                val newMarkerIds = newMarkers.mapNotNull { it.getMarkerId() }.toSet()

                // Find and remove markers that are not in the newMarkers list
                val markersToRemove = markerIdOnWeb.filter { it !in newMarkerIds }
                markersToRemove.forEach { markerId ->
                    // Remove the marker from the map
                    markers.entries.find { it.value.getMarkerId() == markerId }?.let { entry ->
                        entry.value.googleMapMarker?.remove()
                        markers.remove(entry.key)
                    }
                    // Remove the marker ID from the markerIdOnWeb list
                    markerIdOnWeb.remove(markerId)
                }

                newMarkers.forEach {
                    if (markerIdOnWeb.contains(it.getMarkerId())) {
                        for ((key, value) in markers) {
                            if(value.id == it.getMarkerId()) {
                                if (clusterManager != null) {
                                    it.googleMapMarker?.remove()
                                }
                                setMultipleMarkerPosition(it)
                                markerIds.add(key)

                                break
                            }
                        }
                    } else {
                        // Make a record of react code marker IDs that we are adding on map
                        it.getMarkerId()?.let { marker -> markerIdOnWeb.add(marker) }

                        val markerOptions = it.getMarkerOptionsUpdated()

                        if (it.iconUrl.equals("buses_custom_marker")) {
                            val bridge = delegate.bridge
                            val busesMarker = BusesMarker(bridge.context)
                            markerOptions.icon(it.iconUrl?.let { it1 -> busesMarker.getMarkerIcon(it.title, it1) })
                        }

                        val googleMapMarker = googleMap?.addMarker(markerOptions)

                        if (!it.infoIcon.isNullOrEmpty()) {
                            if(it.infoIcon.equals("buses_info_icon")) {
                                val bridge = delegate.bridge
                                googleMapMarker?.tag = it
                                googleMap?.setInfoWindowAdapter(BusesMarkerInfoWindow(bridge.context))
                            } else {
                                val bridge = delegate.bridge
                                googleMapMarker?.tag = it
                                googleMap?.setInfoWindowAdapter(CustomInfoWindowAdapter(bridge.context))
                            }
                        }
                        it.googleMapMarker = googleMapMarker

                        if (clusterManager != null) {
                            googleMapMarker?.remove()
                        }

                        markers[googleMapMarker!!.id] = it
                        markerIds.add(googleMapMarker.id)
                    }

                    if (clusterManager != null) {
                        clusterManager?.addItems(newMarkers)
                        clusterManager?.cluster()
                    }
                }

                callback(Result.success(markerIds))
            }
        } catch (e: GoogleMapsError) {
            callback(Result.failure(e))
        }
    }

    fun distance(origin: LatLng, dest: LatLng): Double {
        val a = Location("")
        a.latitude = origin.latitude
        a.longitude = origin.longitude
        val b = Location("")
        b.latitude = dest.latitude
        b.longitude = dest.longitude
        return a.distanceTo(b).toDouble()
    }

    private fun angleFromCoordinate(mlat1: Double, mlong1: Double, mlat2: Double,
                                    mlong2: Double): Float {
        val lat1 = Math.toRadians(mlat1)
        val long1 = Math.toRadians(mlong1)
        val lat2 = Math.toRadians(mlat2)
        val long2 = Math.toRadians(mlong2)
        val dLon = long2 - long1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - (Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon))
        var brng = Math.atan2(y, x)
        brng = Math.toDegrees(brng)
        brng = (brng + 360) % 360
        // brng = 360 - brng;  count degrees counter-clockwise - remove to make clockwise
        return brng.toFloat()
    }

    private fun getAngle(coordinate: LatLng): Float {
        if(polylineCords.size == 0) {
            return Integer(0).toFloat();
        }

        var reqLatLng: LatLng? = null
        var minDistance = Int.MAX_VALUE.toDouble()
        var point: Int = polylineCords.size - 1

        for (i in 0 until polylineCords.size) {
            val dis: Double = Math.abs(distance(coordinate, polylineCords[i]))
            if (dis < minDistance) {
                point = i
                minDistance = dis
                reqLatLng = polylineCords[i]
            }
        }
        if (point < polylineCords.size - 1) {
            reqLatLng = polylineCords[point + 1]
        }

        if (reqLatLng != null) {
            return angleFromCoordinate(coordinate.latitude, coordinate.longitude, reqLatLng.latitude, reqLatLng.longitude)
        } else {
            return Integer(0).toFloat();
        }
    }

    private fun fetchAddressForMarker(marker: Marker, context: Context) {
        val position = marker.position
        CoroutineScope(Dispatchers.IO).launch {
            val address = getAddressFromLatLng(position, context)
            withContext(Dispatchers.Main) {
                marker.snippet = address
                marker.showInfoWindow() // Refresh the info window
            }
        }
    }

    private fun getAddressFromLatLng(latLng: LatLng, context: Context): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                "Address not found"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "Address not found"
        }
    }

    fun addMarker(marker: CapacitorGoogleMapMarker, callback: (result: Result<String>) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            var markerId: String = ""

            CoroutineScope(Dispatchers.Main).launch {
                // If the marker is already added on map(i.e. markerIdOnWeb has the ID which we are using
                // on react code) then instead of adding a new marker(with new google marker ID)
                // we are returning ID that is already present.
                if(markerIdOnWeb.contains(marker.getMarkerId())) {
                    for ((key, value) in markers) {

                        if(value.id == marker.getMarkerId()) {
                            markerId = key
                            break
                        }
                    }
                } else {
                    // Make a record of react code marker IDs that we are adding on map
                    marker.getMarkerId()?.let { markerIdOnWeb.add(it) }

                    val markerOptions = marker.getMarkerOptionsUpdated()
                    val googleMapMarker = googleMap?.addMarker(markerOptions)
                    if(marker.rotation == 1) {
                        if (marker.angleDiff > 0) {
                            markerOptions.rotation(marker.angleDiff)
                        } else {
                            markerOptions.rotation(getAngle(marker.coordinate))
                        }
                    }
                    if(marker.iconUrl?.contains("buses_custom_marker") == true) {
                        val bridge = delegate.bridge
                        val busesMarker = BusesMarker(bridge.context)
                        markerOptions.icon(busesMarker.getMarkerIcon(marker.title, marker.iconUrl!!))
                    }

                    if(marker.iconUrl?.contains("alert_custom_marker") == true) {
                        val bridge = delegate.bridge
                        val busesMarker = AlertBusMarker(bridge.context)
                        markerOptions.icon(busesMarker.getMarkerIcon(marker.title, marker.snippet, marker.iconUrl!!))
//                        To remove info window set title as empty string
                        markerOptions.title("")
                    }

                    if(marker.iconUrl?.contains("alert_stop_custom_marker") == true) {
                        val bridge = delegate.bridge
                        val busesMarker = AlertStopCustomMarker(bridge.context)
                        markerOptions.icon(busesMarker.getMarkerIcon(marker.title, marker.snippet, marker.iconUrl!!))
//                        To remove info window set title as empty string
                        markerOptions.title("")
                    }

                    if(marker.iconUrl?.contains("overspeed_marker") == true) {
                        val bridge = delegate.bridge
                        val overSpeedMarker = OverSpeedCustomMarker(bridge.context)

                        val resId = bridge.context.resources.getIdentifier(
                        marker.iconUrl,
                         "drawable",
                         bridge.context.packageName
                        )

                        markerOptions.icon(overSpeedMarker.getMarkerIcon(marker.title, marker.snippet, resId))
                        markerOptions.title("")
                    }

                    if(marker.infoIcon.equals("not_show_info_window")) {
//                        To remove info window set title as empty string
//                        markerOptions.title("")
//                        markerOptions.snippet("")
                    }

//                    val googleMapMarker = googleMap?.addMarker(markerOptions)

                    googleMapMarker?.tag = marker


                    if (!marker.infoIcon.isNullOrEmpty()  && (!marker.iconUrl!!.contains("new_3d_marker") || marker.infoIcon!!.contains("last_updated_info")) && (!marker.infoIcon.equals("not_show_info_window")) ) {
                        if(marker.infoIcon.equals("buses_info_icon")) {
                            val bridge = delegate.bridge
                            googleMapMarker?.tag = marker
                            googleMap?.setInfoWindowAdapter(BusesMarkerInfoWindow(bridge.context))
                        }else if(marker.infoIcon!!.contains("bus_alert_info")) {
                            val bridge = delegate.bridge
                            googleMapMarker?.tag = marker
                            googleMap?.setInfoWindowAdapter(AlertMarkerInfoWindow(bridge.context))
                            googleMapMarker?.showInfoWindow()
                            // Fetch the address and update the info window
                            if (googleMapMarker != null && marker.infoIcon!!.contains("address")) {
                                fetchAddressForMarker(googleMapMarker, bridge.context)
                            }
                        } else if(marker.infoIcon!!.contains("last_updated_info")) {
                            val bridge = delegate.bridge
                            googleMapMarker?.tag = marker
                            if(marker.infoIcon!!.contains("reverse") == true){
                                googleMapMarker?.setInfoWindowAnchor(0.5f,1.8f)
                            }
                            else{
                                googleMapMarker?.setInfoWindowAnchor(0.5f,0.2f)
                            }
                            googleMap?.setInfoWindowAdapter(LastUpdatedInfoWindowAdapter(bridge.context))
                            googleMapMarker?.showInfoWindow()
                        }
                        else if(marker.infoIcon!!.contains("stop_arrival_info")) {
                            val bridge = delegate.bridge
                            googleMapMarker?.tag = marker
                            googleMap?.setInfoWindowAdapter(StopArrivalInfoWindowAdapter(bridge.context))
                        }
                        else if(marker.infoIcon!!.contains("replay_info_icon")) {
                            val bridge = delegate.bridge
                            googleMapMarker?.tag = marker
                            googleMap?.setInfoWindowAdapter(HistoryReplayInfoWindowAdapter(bridge.context))
                        } else {
                            val bridge = delegate.bridge
                            googleMapMarker?.tag = marker
                            googleMap?.setInfoWindowAdapter(CustomInfoWindowAdapter(bridge.context))
                        }
                    }

                    if(marker.iconUrl?.isEmpty() == true){
                        // If the marker is only been used to show info window
                        googleMapMarker?.showInfoWindow()
                        googleMapMarker?.alpha = 0.0f
                        if(marker.title.isNotEmpty()) {
                            googleMapMarker?.title = marker.title
                        }
                        if(marker.snippet.isNotEmpty()) {
                            googleMapMarker?.snippet = marker.snippet
                        }
                    }

                    if (clusterManager == null || !marker.isClustered) {
                        marker.googleMapMarker = googleMapMarker
                        if(!marker.isClustered) {
                            markerIdNotOnCluster.add(googleMapMarker!!.id)
                        }
                    } else {
                        if (!marker.infoIcon.isNullOrEmpty() && (!marker.infoIcon.equals("not_show_info_window")) ) {
                            if(marker.infoIcon.equals("buses_info_icon")) {
                                val bridge = delegate.bridge
                                googleMap?.setInfoWindowAdapter(BusesMarkerInfoWindow(bridge.context))
                            }
                        }
                        googleMapMarker?.remove()
                        clusterManager?.addItem(marker)
                        clusterManager?.cluster()
                    }

                    markers[googleMapMarker!!.id] = marker

                    markerId = googleMapMarker.id
                }
                callback(Result.success(markerId))
            }
        } catch (e: GoogleMapsError) {
            callback(Result.failure(e))
        }
    }

    private fun createInfoWindowAsMarker(originalMarker: CapacitorGoogleMapMarker, googleMapMarker: Marker) {
        CoroutineScope(Dispatchers.Main).launch {
            if (infoWindowMarkers.containsKey(googleMapMarker.id)) {
                return@launch
            }

            val infoWindowPosition = calculateInfoWindowPosition(googleMapMarker.position)
            // Create info window marker options
            val infoWindowMarkerOptions = MarkerOptions()
                .position(infoWindowPosition)
                .flat(true)
                .zIndex(googleMapMarker.zIndex + 1.0f)

            if (originalMarker.infoIcon?.contains("reverse") == true) {
                infoWindowMarkerOptions.anchor(0.4f, -0.15f)
            } else {
                infoWindowMarkerOptions.anchor(0.4f, 1.0f)
            }

            // Create bitmap in background thread to avoid UI blocking
            val infoWindowBitmap = withContext(Dispatchers.Default) {
                multipleInfoWindowView.createInfoWindowBitmap(originalMarker)
            }

            infoWindowMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(infoWindowBitmap))

            val infoWindowMarker = googleMap?.addMarker(infoWindowMarkerOptions)
            infoWindowMarker?.tag = hashMapOf(
                "type" to "infoWindow",
                "originalMarkerId" to googleMapMarker.id,
                "markerData" to originalMarker
            )

            infoWindowMarker?.let { marker ->
                infoWindowMarkers[googleMapMarker.id] = marker
            }
        }
    }
    private fun calculateInfoWindowPosition(originalPosition: LatLng): LatLng {

        return LatLng(originalPosition.latitude , originalPosition.longitude)
    }

    private fun createInfoWindowView(marker: CapacitorGoogleMapMarker): Bitmap {
        return multipleInfoWindowView.createInfoWindowBitmap(marker)
    }

    private fun removeInfoWindowMarker(originalMarkerId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val infoWindowMarker = infoWindowMarkers[originalMarkerId]
            if (infoWindowMarker != null) {
                infoWindowMarker.remove() // PROPERLY REMOVE FROM MAP
                infoWindowMarkers.remove(originalMarkerId)
            }
        }
    }

    fun addPolygons(newPolygons: List<CapacitorGoogleMapsPolygon>, callback: (ids: Result<List<String>>) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            val shapeIds: MutableList<String> = mutableListOf()

            CoroutineScope(Dispatchers.Main).launch {
                newPolygons.forEach {
                    val polygonOptions: Deferred<PolygonOptions> = CoroutineScope(Dispatchers.IO).async {
                        this@CapacitorGoogleMap.buildPolygon(it)
                    }

                    val googleMapsPolygon = googleMap?.addPolygon(polygonOptions.await())
                    googleMapsPolygon?.tag = it.tag

                    it.googleMapsPolygon = googleMapsPolygon

                    polygons[googleMapsPolygon!!.id] = it
                    shapeIds.add(googleMapsPolygon.id)
                }

                callback(Result.success(shapeIds))
            }
        } catch (e: GoogleMapsError) {
            callback(Result.failure(e))
        }
    }

    fun addCircles(newCircles: List<CapacitorGoogleMapsCircle>,callback: (ids: Result<List<String>>) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            val circleIds: MutableList<String> = mutableListOf()

            CoroutineScope(Dispatchers.Main).launch {
                newCircles.forEach {
                    var circleOptions: Deferred<CircleOptions> = CoroutineScope(Dispatchers.IO).async {
                        this@CapacitorGoogleMap.buildCircle(it)
                    }

                    val googleMapsCircle = googleMap?.addCircle(circleOptions.await())
                    googleMapsCircle?.tag = it.tag

                    it.googleMapsCircle = googleMapsCircle

                    circles[googleMapsCircle!!.id] = it
                    circleIds.add(googleMapsCircle.id)
                }

                callback(Result.success(circleIds))
            }
        } catch (e: GoogleMapsError) {
            callback(Result.failure(e))
        }
    }

    private fun animateMarker(marker: Marker?, finalPosition: LatLng, duration: Long = 2000) {
        // Return early if the marker is null
        if (marker == null) return

        val startPosition = marker.position // The initial position of the marker
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration // 2 seconds by default

        animator.interpolator = LinearInterpolator()
        val infoWindowMarker = infoWindowMarkers[marker.id]

        animator.addUpdateListener { valueAnimator ->
            val v = valueAnimator.animatedFraction
            val latitude =
                startPosition.latitude + (finalPosition.latitude - startPosition.latitude) * v
            val longitude =
                startPosition.longitude + (finalPosition.longitude - startPosition.longitude) * v

            val newPosition = LatLng(latitude, longitude)
            marker.position = newPosition
            infoWindowMarker?.position = calculateInfoWindowPosition(newPosition)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Ensure final positions are set correctly
                infoWindowMarker?.position = calculateInfoWindowPosition(finalPosition)
            }
        })
        animator.start()
    }

    private fun animateMarkerInsideCluster(clusterItem: CapacitorGoogleMapMarker, newPosition: LatLng) {
        val oldPosition = clusterItem.coordinate
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 2000 // duration of the animation in milliseconds

        val marker = (clusterManager?.renderer as? BusesMarkerRenderer)?.getMarker(clusterItem)
        valueAnimator.addUpdateListener { animator ->
            val v = valueAnimator.animatedFraction

            val latitude =
                oldPosition.latitude + (newPosition.latitude - oldPosition.latitude) * v
            val longitude =
                oldPosition.longitude + (newPosition.longitude - oldPosition.longitude) * v

            val newLocation = LatLng(latitude, longitude)

            // Update the position of the cluster item
            clusterItem.coordinate = newLocation
            // Update the position of the marker if it's not null and not clustered
            marker?.position = newLocation
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // At the end of the animation, re-cluster if necessary
                clusterManager?.cluster()
            }
        })
        valueAnimator.start()
    }

    fun setMarkerPosition(marker: CapacitorGoogleMapMarker, callback: (result: Result<String>) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            var markerId: String
            CoroutineScope(Dispatchers.Main).launch {
                if (markers.contains(marker.id)) {
                    val oldMarker = markers[marker.id]
                    val currentZoom = googleMap?.cameraPosition?.zoom ?: 0f
                    val shouldShowInfoWindow = currentZoom >= multipleInfoWindowZoomLevel

                    if (shouldShowInfoWindow && marker.infoIcon?.contains("multiple_info_window") == true) {
                        val infoWindowMarker = infoWindowMarkers[marker.id]
                        val existingInfoWindow = infoWindowMarkers[marker.id]
                        val infoWindowTypeChanged = oldMarker?.infoIcon != marker.infoIcon
                        if (infoWindowMarker != null) {
                            //  UPDATE EXISTING INFO WINDOW POSITION (DON'T CREATE NEW ONE)
                            val newInfoWindowPosition = calculateInfoWindowPosition(marker.coordinate)
                            infoWindowMarker.position = newInfoWindowPosition
                            infoWindowMarker.zIndex = oldMarker?.googleMapMarker?.zIndex?.plus(10.0f) ?: 1000.0f
                            if (existingInfoWindow != null && infoWindowTypeChanged) {
                                val newInfoWindowBitmap = withContext(Dispatchers.Default) {
                                    multipleInfoWindowView.createInfoWindowBitmap(marker)
                                }
                                existingInfoWindow.setIcon(BitmapDescriptorFactory.fromBitmap(newInfoWindowBitmap))

                                // Update anchor
                                if (marker.infoIcon?.contains("reverse") == true) {
                                    existingInfoWindow.setAnchor(0.4f, -0.15f)
                                } else {
                                    existingInfoWindow.setAnchor(0.4f, 1.0f)
                                }
                            }
                        } else {
                            // Create new info window only if it doesn't exist
                            oldMarker?.googleMapMarker?.let { googleMapMarker ->
                                createInfoWindowAsMarker(marker, googleMapMarker)
                            }
                        }
                    } else {
                        // Remove info window if zoom level is too low
                        marker.id?.let { removeInfoWindowMarker(it) }
                    }
                    if(markerIdNotOnCluster.contains(marker.id) && marker.isClustered) {
                        // If previously the marker was not added to cluster but in new request
                        // this marker needs to be added to the cluster

                        oldMarker?.googleMapMarker?.remove()
                        clusterManager?.addItem(oldMarker)
                        clusterManager?.cluster()
                        markerIdNotOnCluster.remove(marker.id)

                    } else if(!markerIdNotOnCluster.contains(marker.id) && !marker.isClustered) {
                        // If previously the marker was added to cluster but in new request
                        // this marker wants to the be remove from cluster

                        clusterManager?.removeItem(oldMarker)
                        clusterManager?.cluster()
                        val googleMapMarker = googleMap?.addMarker(oldMarker!!.getMarkerOptionsUpdated())
                        oldMarker?.googleMapMarker = googleMapMarker
                        marker.id?.let { markerIdNotOnCluster.add(it) }
                    }
                    if (clusterManager == null || !marker.isClustered) {
                        // Below line animate the marker
                        if (oldMarker!!.position.latitude != marker!!.coordinate.latitude
                            || oldMarker!!.position.longitude != marker!!.coordinate.longitude) {
                            marker.infoData?.getLong("animationDuration")?.takeIf { it >= 0 }?.let { duration ->
                                animateMarker(oldMarker?.googleMapMarker, marker!!.coordinate, duration)
                            } ?: animateMarker(oldMarker?.googleMapMarker, marker!!.coordinate)
                            val infoWindowMarker = infoWindowMarkers[marker.id]
                            if (infoWindowMarker != null) {
                                val newInfoWindowPosition = calculateInfoWindowPosition(marker.coordinate)
                                infoWindowMarker.position = newInfoWindowPosition
                                val existingInfoWindow = infoWindowMarkers[marker.id]
                                val infoWindowTypeChanged = oldMarker?.infoIcon != marker.infoIcon
                                if (existingInfoWindow != null && infoWindowTypeChanged && shouldShowInfoWindow && marker.infoIcon?.contains("multiple_info_window") == true) {
                                    val newInfoWindowBitmap = withContext(Dispatchers.Default) {
                                        multipleInfoWindowView.createInfoWindowBitmap(marker)
                                    }
                                    existingInfoWindow.setIcon(BitmapDescriptorFactory.fromBitmap(newInfoWindowBitmap))

                                    // Update anchor if needed based on the new type
                                    if (marker.infoIcon?.contains("reverse") == true) {
                                        existingInfoWindow.setAnchor(0.4f, -0.15f)
                                    } else {
                                        existingInfoWindow.setAnchor(0.4f, 1.0f)
                                    }
                                }
                            }
                        }
                        // Set the camera position of map to the centre of the marker
                        //                    googleMap?.animateCamera(CameraUpdateFactory.newLatLng(marker!!.coordinate), 5000, null)

                        if (marker.rotation == 1) {
                            if(marker.angleDiff != 0.0f){
                                oldMarker?.googleMapMarker?.rotation = marker.angleDiff
                            }
                            else{
                                oldMarker?.googleMapMarker?.rotation = getAngle(marker!!.coordinate)
                            }
                        } else {
                            oldMarker?.googleMapMarker?.rotation = 0.0f
                        }

                        // In case marker is only for showing info window
                        if (marker.iconUrl?.isEmpty() == true) {
                            oldMarker?.googleMapMarker?.showInfoWindow()
                            oldMarker?.googleMapMarker?.alpha = 0.0f
                            if (marker.title.isNotEmpty()) {
                                oldMarker?.googleMapMarker?.title = marker.title
                            }
                            if (marker.snippet.isNotEmpty()) {
                                oldMarker?.googleMapMarker?.snippet = marker.snippet
                            }
                        } else {
                            // Setting the new icon if the icon is modified
                            marker?.iconUrl?.let { oldMarker?.updateIcon(it, marker.title, marker.snippet) }
                        }

                        if (!marker.infoIcon.isNullOrEmpty() && (!marker.infoIcon.equals("not_show_info_window"))) {
                            if (marker.infoIcon.equals("buses_info_icon")) {
                                if (marker.infoData?.getBoolean("showInfoIcon") == true) {
                                    val bridge = delegate.bridge
                                    oldMarker?.googleMapMarker?.tag = marker
                                    oldMarker?.googleMapMarker?.showInfoWindow()
                                } else {
                                    oldMarker?.googleMapMarker?.hideInfoWindow()
                                }
                            } else if (marker.infoIcon!!.contains("bus_alert_info")) {
                                val bridge = delegate.bridge
                                oldMarker?.googleMapMarker?.tag = marker
                                oldMarker?.googleMapMarker?.showInfoWindow()
                                // Fetch the address and update the info window
                                if (oldMarker?.googleMapMarker != null && marker.infoIcon!!.contains("address")) {
                                    fetchAddressForMarker(oldMarker?.googleMapMarker!!, bridge.context)
                                }
                            } else if(marker.infoIcon!!.contains("last_updated_info")) {
                                oldMarker?.googleMapMarker?.tag = marker
                                if(marker.infoIcon!!.contains("reverse") == true){
                                    oldMarker?.googleMapMarker?.setInfoWindowAnchor(0.5f, 1.8f)
                                }
                                else{
                                    oldMarker?.googleMapMarker?.setInfoWindowAnchor(0.5f, 0.4f)
                                }
                                oldMarker?.googleMapMarker?.showInfoWindow()
                            } else if(marker.infoIcon!!.contains("replay_info_icon")) {
                                if (marker.infoData?.getBoolean("showInfoIcon") == true) {
                                    val bridge = delegate.bridge
                                    oldMarker?.googleMapMarker?.tag = marker
                                    oldMarker?.googleMapMarker?.showInfoWindow()
                                } else {
                                    oldMarker?.googleMapMarker?.hideInfoWindow()
                                }
                            }
                        }


                    } else {
                        oldMarker?.let {

                            val renderer = clusterManager?.renderer as? BusesMarkerRenderer
                            val isClustered = renderer?.getClusterItem(oldMarker.googleMapMarker) != null
                            if (!isClustered
                                && (oldMarker.position.latitude != marker.coordinate.latitude
                                        || oldMarker.position.longitude != marker.coordinate.longitude)) {
//                                  Only animate if the marker is not currently clustered
                                animateMarkerInsideCluster(it, marker.coordinate)
                            }
                            it.iconUrl = marker.iconUrl
                            it.coordinate = marker.coordinate
                            it.infoData = marker.infoData
                            it.title = marker.title


//                              Remove and re-add the marker to the ClusterManager.
                            clusterManager?.removeItem(it)
                            clusterManager?.addItem(it)
                            clusterManager?.cluster()


                        }
                    }

                    markerId = marker?.id.toString()
                    callback(Result.success(markerId))

                } else {
                    throw Exception("Marker for setMarkerPosition is not Found.")
                }
            }
        } catch (e: GoogleMapsError) {
            callback(Result.failure(e))
        }

    }

    fun fitBound(cords: List<LatLng>, padding: Int,  callback: (error: GoogleMapsError?) -> Unit) {
        try {

            googleMap ?: throw GoogleMapNotAvailable()

            CoroutineScope(Dispatchers.Main).launch {
                // Initialize the LatLngBounds.Builder
                val builder = LatLngBounds.Builder()
                val width = mapView.width
                val height = mapView.height

                // Loop through the list of coordinates
                for (cord in cords) {
                    builder.include(cord)
                }

                // Build the bounds
                val bounds = builder.build()
                if(width > 0 && height > 0) {
                    // Create a camera update with the bounds and the specified padding
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                }
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }


    private fun setMultipleMarkerPosition(marker: CapacitorGoogleMapMarker) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            var markerId: String

            val existingMarkerEntry = markers.entries.find { it.value.id == marker.id }

            if (existingMarkerEntry != null) {
                val oldMarker = existingMarkerEntry.value
                // Below line animate the marker
//                animateMarkerToICS(oldMarker?.googleMapMarker, marker!!.coordinate, LatLngInterpolator.Spherical())
                val infoWindowMarker = infoWindowMarkers[marker.id]
                if (infoWindowMarker != null) {
                    val newInfoWindowPosition = calculateInfoWindowPosition(marker.coordinate)
                    infoWindowMarker.position = newInfoWindowPosition
                }

                if(marker.rotation == 1){
                    if(marker.angleDiff>0){
                        oldMarker?.googleMapMarker?.rotation = marker.angleDiff
                    }
                    else {
                        oldMarker?.googleMapMarker?.rotation = getAngle(marker!!.coordinate)
                    }
                }else{
                    oldMarker?.googleMapMarker?.rotation =  0.0f
                }

                // In case marker is only for showing info window
                if(marker.iconUrl?.isEmpty() == true){
                    oldMarker?.googleMapMarker?.alpha = 0.0f
                    if(marker.title.isNotEmpty()) {
                        oldMarker?.googleMapMarker?.title = marker.title
                    }
                    if(marker.snippet.isNotEmpty()) {
                        oldMarker?.googleMapMarker?.snippet = marker.snippet
                    }
                } else {
                    // Setting the new icon if the icon is modified
                    marker?.iconUrl?.let { oldMarker?.updateIcon(it, marker.title, marker.snippet) }
                }

                markerId = marker?.id.toString()


            } else {
                throw Exception("Marker for setMultipleMarkerPosition is not Found.")
            }
        } catch (e: GoogleMapsError) {
            throw Exception("Error in setMultipleMarkerPosition in the ID: ${marker.id}" )
        }

    }

    fun addPolylines(polylines: MutableList<MutableList<LatLng>>,
                     strokeColors: MutableList<String>,
                     strokeWidths: MutableList<Int>,
                     zIndexs: MutableList<Int>,
                     strokeOpacities: MutableList<Int>,
                     callback: (result: Result<String>) -> Unit
    ) {
        // addPolylines is our custom implementation. If we want to use both add and remove polylines we can copy the plugins code here addPolylines
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            val polylineIds: MutableList<String> = mutableListOf()

            CoroutineScope(Dispatchers.Main).launch {
                polylines.forEachIndexed { index, polyline->
                    polylineCords = polyline
                    val polylineOptions = PolylineOptions()
                    polylineOptions.addAll(polyline)
                    polylineOptions.width(strokeWidths[index].toFloat())
                    polylineOptions.color(Color.parseColor(strokeColors[index]))
                    polylineOptions.zIndex(zIndexs[index].toFloat())
                    val googleMapPolyline = googleMap?.addPolyline(polylineOptions)

                    googleMapPolyline?.id?.let { it1 -> polylineIds.add(it1) }
                }

                callback(Result.success(polylineIds[0]))
            }
        } catch (e: GoogleMapsError) {
            callback(Result.failure(e))
        }
    }

    private fun setClusterManagerRenderer(minClusterSize: Int?) {
        clusterManager?.renderer = CapacitorClusterManagerRenderer(
            delegate.bridge.context,
            googleMap,
            clusterManager,
            minClusterSize
        )
    }

    @SuppressLint("PotentialBehaviorOverride")
    fun enableClustering(callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            if (clusterManager != null) {
                callback(null)
                return
            }

            CoroutineScope(Dispatchers.Main).launch {
                val bridge = delegate.bridge
                clusterManager = ClusterManager(bridge.context, googleMap)
                clusterManager!!.renderer = BusesMarkerRenderer(bridge.context, googleMap!!, clusterManager!!)
                // Only for buses page marker info window. If new cluster marker info window is made then update
                // the below line with condition
                googleMap?.setInfoWindowAdapter(BusesMarkerInfoWindow(bridge.context))



                googleMap?.setOnCameraIdleListener(clusterManager)
//                googleMap?.setOnMarkerClickListener(clusterManager)
                googleMap?.setOnInfoWindowClickListener(clusterManager)


                setClusterListeners()

                // add existing markers to the cluster
                if (markers.isNotEmpty()) {
                    clusterManager?.clearItems() // Clear existing items in the cluster manager

                    val filteredMarkers = markers.values.filter { it.isClustered }

                    // Collect all the keys whose markers.values.isClustered is false
                    markerIdNotOnCluster = markers.filter { !it.value.isClustered }.keys.toCollection(ArrayList())

                    for (marker in filteredMarkers) {
                        marker.googleMapMarker?.remove()
                        // marker.googleMapMarker = null
                    }

                    clusterManager?.addItems(filteredMarkers)
                    clusterManager?.cluster()
                }


                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    fun disableClustering(callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            CoroutineScope(Dispatchers.Main).launch {
                clusterManager?.clearItems()
                clusterManager?.cluster()
                clusterManager = null

                googleMap?.setOnMarkerClickListener(this@CapacitorGoogleMap)

                // add existing markers back to the map
                if (markers.isNotEmpty()) {
                    for ((_, marker) in markers) {
                        val googleMapMarker = googleMap?.addMarker(marker.getMarkerOptionsUpdated())
                        marker.googleMapMarker = googleMapMarker
                    }
                }

                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun removePolygons(ids: List<String>, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            CoroutineScope(Dispatchers.Main).launch {
                ids.forEach {
                    val polygon = polygons[it]
                    if (polygon != null) {
                        polygon.googleMapsPolygon?.remove()
                        polygons.remove(it)
                    }
                }

                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }
    fun setMultipleInfoWindowZoomLevel(zoomLevel: Float, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                multipleInfoWindowZoomLevel = zoomLevel
                updateInfoWindowsForCurrentZoom()
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun removeMarker(id: String, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            val marker = markers[id]
            if(markerIdOnWeb.contains(markers[id]?.id)) {
                val markerIdToRemove = markers[id]?.id
                markerIdOnWeb.remove(markerIdToRemove)
            }
            marker ?: throw MarkerNotFoundError()

            CoroutineScope(Dispatchers.Main).launch {
                removeInfoWindowMarker(id)
                if (clusterManager != null) {
                    clusterManager?.removeItem(marker)
                    clusterManager?.cluster()
                }

                marker.googleMapMarker?.remove()
                markers.remove(id)

                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun removeMarkers(ids: List<String>, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            CoroutineScope(Dispatchers.Main).launch {
                val deletedMarkers: MutableList<CapacitorGoogleMapMarker> = mutableListOf()

                ids.forEach {
                    val marker = markers[it]
                    if (marker != null) {
                        marker.googleMapMarker?.remove()
                        markers.remove(it)

                        deletedMarkers.add(marker)
                    }
                }

                if (clusterManager != null) {
                    clusterManager?.removeItems(deletedMarkers)
                    clusterManager?.cluster()
                }

                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun removeCircles(ids: List<String>, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            CoroutineScope(Dispatchers.Main).launch {
                ids.forEach {
                    val circle = circles[it]
                    if (circle != null) {
                        circle.googleMapsCircle?.remove()
                        markers.remove(it)
                    }
                }

                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun removePolylines(ids: List<String>, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            CoroutineScope(Dispatchers.Main).launch {
                ids.forEach {
                    val polyline = polylines[it]
                    if (polyline != null) {
                        polyline.googleMapsPolyline?.remove()
                        polylines.remove(it)
                    }
                }

                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun setCamera(config: GoogleMapCameraConfig, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            CoroutineScope(Dispatchers.Main).launch {
                val currentPosition = googleMap!!.cameraPosition

                var updatedTarget = config.coordinate
                if (updatedTarget == null) {
                    updatedTarget = currentPosition.target
                }

                var zoom = config.zoom
                if (zoom == null) {
                    zoom = currentPosition.zoom.toDouble()
                }

                var bearing = config.bearing
                if (bearing == null) {
                    bearing = currentPosition.bearing.toDouble()
                }

                var angle = config.angle
                if (angle == null) {
                    angle = currentPosition.tilt.toDouble()
                }

                var animate = config.animate
                if (animate == null) {
                    animate = false
                }

                val updatedPosition =
                    CameraPosition.Builder()
                        .target(updatedTarget)
                        .zoom(zoom.toFloat())
                        .bearing(bearing.toFloat())
                        .tilt(angle.toFloat())
                        .build()

                if (animate) {
                    googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(updatedPosition))
                } else {
                    googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(updatedPosition))
                }
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun getMapType(callback: (type: String, error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            CoroutineScope(Dispatchers.Main).launch {
                val mapType: String = when (googleMap?.mapType) {
                    MAP_TYPE_NORMAL -> "Normal"
                    MAP_TYPE_HYBRID -> "Hybrid"
                    MAP_TYPE_SATELLITE -> "Satellite"
                    MAP_TYPE_TERRAIN -> "Terrain"
                    MAP_TYPE_NONE -> "None"
                    else -> {
                        "Normal"
                    }
                }
                callback(mapType, null);
            }
        }  catch (e: GoogleMapsError) {
            callback("", e)
        }
    }

    fun setMapType(mapType: String, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            CoroutineScope(Dispatchers.Main).launch {
                val mapTypeInt: Int =
                    when (mapType) {
                        "Normal" -> MAP_TYPE_NORMAL
                        "Hybrid" -> MAP_TYPE_HYBRID
                        "Satellite" -> MAP_TYPE_SATELLITE
                        "Terrain" -> MAP_TYPE_TERRAIN
                        "None" -> MAP_TYPE_NONE
                        else -> {
                            Log.w(
                                "CapacitorGoogleMaps",
                                "unknown mapView type '$mapType'  Defaulting to normal."
                            )
                            MAP_TYPE_NORMAL
                        }
                    }

                googleMap?.mapType = mapTypeInt
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun enableIndoorMaps(enabled: Boolean, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            CoroutineScope(Dispatchers.Main).launch {
                googleMap?.isIndoorEnabled = enabled
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun enableTrafficLayer(enabled: Boolean, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            CoroutineScope(Dispatchers.Main).launch {
                googleMap?.isTrafficEnabled = enabled
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun enableCurrentLocation(enabled: Boolean, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            CoroutineScope(Dispatchers.Main).launch {
                googleMap?.isMyLocationEnabled = enabled
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun setPadding(padding: GoogleMapPadding, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()
            CoroutineScope(Dispatchers.Main).launch {
                googleMap?.setPadding(padding.left, padding.top, padding.right, padding.bottom)
                callback(null)
            }
        } catch (e: GoogleMapsError) {
            callback(e)
        }
    }

    fun getMapBounds(): Rect {
        return Rect(
            getScaledPixels(delegate.bridge, config.x),
            getScaledPixels(delegate.bridge, config.y),
            getScaledPixels(delegate.bridge, config.x + config.width),
            getScaledPixels(delegate.bridge, config.y + config.height)
        )
    }

    fun getLatLngBounds(): LatLngBounds {
        return googleMap?.projection?.visibleRegion?.latLngBounds ?: throw BoundsNotFoundError()
    }

    fun fitBounds(bounds: LatLngBounds, padding: Int) {
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        googleMap?.animateCamera(cameraUpdate)
    }

    private fun getScaledPixels(bridge: Bridge, pixels: Int): Int {
        // Get the screen's density scale
        val scale = bridge.activity.resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        return (pixels * scale + 0.5f).toInt()
    }

    private fun getScaledPixelsF(bridge: Bridge, pixels: Float): Float {
        // Get the screen's density scale
        val scale = bridge.activity.resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        return (pixels * scale + 0.5f)
    }

    private fun getScaledRect(bridge: Bridge, rectF: RectF): RectF {
        return RectF(
            getScaledPixelsF(bridge, rectF.left),
            getScaledPixelsF(bridge, rectF.top),
            getScaledPixelsF(bridge, rectF.right),
            getScaledPixelsF(bridge, rectF.bottom)
        )
    }

    private fun buildCircle(circle: CapacitorGoogleMapsCircle): CircleOptions {
        val circleOptions = CircleOptions()
        circleOptions.fillColor(circle.fillColor)
        circleOptions.strokeColor(circle.strokeColor)
        circleOptions.strokeWidth(circle.strokeWidth)
        circleOptions.zIndex(circle.zIndex)
        circleOptions.clickable(circle.clickable)
        circleOptions.radius(circle.radius.toDouble())
        circleOptions.center(circle.center)

        return circleOptions
    }

    private fun buildPolygon(polygon: CapacitorGoogleMapsPolygon): PolygonOptions {
        val polygonOptions = PolygonOptions()
        polygonOptions.fillColor(polygon.fillColor)
        polygonOptions.strokeColor(polygon.strokeColor)
        polygonOptions.strokeWidth(polygon.strokeWidth)
        polygonOptions.zIndex(polygon.zIndex)
        polygonOptions.geodesic(polygon.geodesic)
        polygonOptions.clickable(polygon.clickable)

        var shapeCounter = 0
        polygon.shapes.forEach {
            if (shapeCounter == 0) {
                // outer shape
                it.forEach {
                    polygonOptions.add(it)
                }
            } else {
                polygonOptions.addHole(it)
            }

            shapeCounter += 1
        }

        return polygonOptions
    }

    private fun buildPolyline(line: CapacitorGoogleMapPolyline): PolylineOptions {
        val polylineOptions = PolylineOptions()
        polylineOptions.width(line.strokeWidth * this.config.devicePixelRatio)
        polylineOptions.color(line.strokeColor)
        polylineOptions.clickable(line.clickable)
        polylineOptions.zIndex(line.zIndex)
        polylineOptions.geodesic(line.geodesic)

        line.path.forEach {
            polylineOptions.add(it)
        }

        line.styleSpans.forEach {
            if (it.segments != null) {
                polylineOptions.addSpan(StyleSpan(it.color, it.segments))
            } else {
                polylineOptions.addSpan(StyleSpan(it.color))
            }
        }

        return polylineOptions
    }

    private fun buildMarker(marker: CapacitorGoogleMapMarker): MarkerOptions {
        val markerOptions = MarkerOptions()
        markerOptions.position(marker.coordinate)
        markerOptions.title(marker.title)
        markerOptions.snippet(marker.snippet)
        markerOptions.alpha(marker.opacity)
        markerOptions.flat(marker.isFlat)
        markerOptions.draggable(marker.draggable)
        markerOptions.zIndex(marker.zIndex)
        if (marker.iconAnchor != null) {
            markerOptions.anchor(marker.iconAnchor!!.x, marker.iconAnchor!!.y)
        }


        if (!marker.iconUrl.isNullOrEmpty()) {
            if (this.markerIcons.contains(marker.iconUrl)) {
                val cachedBitmap = this.markerIcons[marker.iconUrl]
                markerOptions.icon(getResizedIcon(cachedBitmap!!, marker))
            } else {
                try {
                    var stream: InputStream? = null
                    if (marker.iconUrl!!.startsWith("https:")) {
                        stream = URL(marker.iconUrl).openConnection().getInputStream()
                    } else {
                        stream = this.delegate.context.assets.open("public/${marker.iconUrl}")
                    }
                    var bitmap = BitmapFactory.decodeStream(stream)
                    this.markerIcons[marker.iconUrl!!] = bitmap
                    markerOptions.icon(getResizedIcon(bitmap, marker))
                } catch (e: Exception) {
                    var detailedMessage = "${e.javaClass} - ${e.localizedMessage}"
                    if (marker.iconUrl!!.endsWith(".svg")) {
                        detailedMessage = "SVG not supported"
                    }

                    Log.w(
                        "CapacitorGoogleMaps",
                        "Could not load image '${marker.iconUrl}': ${detailedMessage}. Using default marker icon."
                    )
                }
            }
        } else {
            if (marker.colorHue != null) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(marker.colorHue!!))
            }
        }

        marker.markerOptions = markerOptions

        return markerOptions
    }

    private fun getResizedIcon(
        _bitmap: Bitmap,
        marker: CapacitorGoogleMapMarker
    ): BitmapDescriptor {
        var bitmap = _bitmap
        if (marker.iconSize != null) {
            bitmap =
                Bitmap.createScaledBitmap(
                    bitmap,
                    (marker.iconSize!!.width * this.config.devicePixelRatio).toInt(),
                    (marker.iconSize!!.height * this.config.devicePixelRatio).toInt(),
                    false
                )
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun onStart() {
        mapView.onStart()
    }

    fun onResume() {
        mapView.onResume()
    }

    fun onStop() {
        mapView.onStop()
    }

    fun onPause() {
        mapView.onPause()
    }

    fun onDestroy() {
        mapView.onDestroy()
    }

    override fun onMapReady(map: GoogleMap) {
        runBlocking {
            googleMap = map

            // Disable the 2 button that shows on bottom right after the click on marker
            googleMap?.uiSettings?.isMapToolbarEnabled = false;

            googleMap?.uiSettings?.isRotateGesturesEnabled = false
            val data = JSObject()
            data.put("mapId", this@CapacitorGoogleMap.id)
            delegate.notify("onMapReady", data)

            isReadyChannel.send(true)
            isReadyChannel.close()
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    fun setListeners() {
        CoroutineScope(Dispatchers.Main).launch {
            this@CapacitorGoogleMap.googleMap?.setOnCameraIdleListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnCameraMoveStartedListener(
                this@CapacitorGoogleMap
            )
            this@CapacitorGoogleMap.googleMap?.setOnCameraMoveListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnMarkerClickListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnPolygonClickListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnCircleClickListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnMarkerDragListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnMapClickListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnMyLocationButtonClickListener(
                this@CapacitorGoogleMap
            )
            this@CapacitorGoogleMap.googleMap?.setOnMyLocationClickListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnInfoWindowClickListener(this@CapacitorGoogleMap)
            this@CapacitorGoogleMap.googleMap?.setOnPolylineClickListener(this@CapacitorGoogleMap)
//            this@CapacitorGoogleMap.googleMap?.setOnInfoWindowCloseListener(this@CapacitorGoogleMap)
        }
    }

    fun setClusterListeners() {
        CoroutineScope(Dispatchers.Main).launch {
            clusterManager?.setOnClusterItemClickListener {
                if (null == it.googleMapMarker) false
                else this@CapacitorGoogleMap.onMarkerClick(it.googleMapMarker!!)
            }

            clusterManager?.setOnClusterItemInfoWindowClickListener {
                if (null != it.googleMapMarker) {
                    this@CapacitorGoogleMap.onInfoWindowClick(it.googleMapMarker!!)
                }
            }

            clusterManager?.setOnClusterInfoWindowClickListener {
                val data = this@CapacitorGoogleMap.getClusterData(it)
                delegate.notify("onClusterInfoWindowClick", data)
            }

            clusterManager?.setOnClusterClickListener {
                val data = this@CapacitorGoogleMap.getClusterData(it)
                val builder = LatLngBounds.Builder()
                for (item in it.items) {
                    builder.include(item.position)
                }
                val bounds = builder.build()
                val padding = 200 // adjust padding as needed
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                googleMap?.animateCamera(cameraUpdate)


                delegate.notify("onClusterClick", data)
//              If false is returned then the above changes to zoom in inside the cluster will not work
                true
            }

            googleMap?.setOnMarkerClickListener(this@CapacitorGoogleMap)
        }
    }

    private fun getClusterData(it: Cluster<CapacitorGoogleMapMarker>): JSObject {
        val data = JSObject()
        data.put("mapId", this.id)
        data.put("latitude", it.position.latitude)
        data.put("longitude", it.position.longitude)
        data.put("size", it.size)

        val items = JSArray()
        for (item in it.items) {
            val marker = item.googleMapMarker

            if (marker != null) {
                val jsItem = JSObject()
                jsItem.put("markerId", marker.id)
                jsItem.put("latitude", marker.position.latitude)
                jsItem.put("longitude", marker.position.longitude)
                jsItem.put("title", marker.title)
                jsItem.put("snippet", marker.snippet)

                items.put(jsItem)
            }
        }

        data.put("items", items)

        return data
    }

    override fun onMapClick(point: LatLng) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("latitude", point.latitude)
        data.put("longitude", point.longitude)
        delegate.notify("onMapClick", data)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val data = JSObject()
        // For multi_info_window
        val markerTag = marker.tag
        if (markerTag is HashMap<*, *> && markerTag["type"] == "infoWindow") {
            // This is an info window marker click
            val originalMarkerId = markerTag["originalMarkerId"] as? String
            val markerData = markerTag["markerData"] as? CapacitorGoogleMapMarker

            if (originalMarkerId != null && markerData != null) {
                data.put("mapId", this@CapacitorGoogleMap.id)
                data.put("markerId", originalMarkerId)
                data.put("isInfoWindow", true)
                data.put("latitude", marker.position.latitude)
                data.put("longitude", marker.position.longitude)
                data.put("title", markerData.title)
                data.put("snippet", markerData.snippet)

                // Add info data if available
                markerData.infoData?.let { infoData ->
                    val infoJsObject = JSObject()
                    infoData.keys().forEach { key ->
                        when (val value = infoData.get(key)) {
                            is String -> infoJsObject.put(key, value)
                            is Int -> infoJsObject.put(key, value)
                            is Boolean -> infoJsObject.put(key, value)
                            is Double -> infoJsObject.put(key, value)
                            // Add other types as needed
                        }
                    }
                    data.put("infoData", infoJsObject)
                }

                delegate.notify("onMarkerClick", data)
                return true // Consume the event
            }
        }

        val infoData = (marker?.tag as? CapacitorGoogleMapMarker)?.infoData
        var title = marker.title
        if(marker.title.isNullOrEmpty() && infoData is JSONObject && infoData.has("title")){
            // Also validate if infoData has title as there may be chances
            // That title is removed in markerOption to hide info window
            title = infoData.getString("title")
        }

        // Is this Marker is added inside the cluster
        val isClusterItemMarker = clusterManager?.markerCollection?.markers?.contains(marker) ?: false

        if (title.isNullOrEmpty() && (clusterManager != null || isClusterItemMarker)) {
            // For the cluster marker when clicked should zoom into the
            // So when below method is called clusterManager?.setOnClusterClickListener is called
            // and marker gets zoomed in
            return clusterManager?.onMarkerClick(marker) ?: false
        }

        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("markerId", marker.id)
        data.put("latitude", marker.position.latitude)
        data.put("longitude", marker.position.longitude)
        data.put("title", title)
        data.put("snippet", marker.snippet)
        if(marker.snippet.isNullOrEmpty() && infoData is JSONObject && infoData.has("snippet")){
            val snippet = infoData.getString("snippet")
            data.put("snippet", snippet)
        }
        delegate.notify("onMarkerClick", data)
        return false
    }

    override fun onPolylineClick(polyline: Polyline) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("polylineId", polyline.id)
        data.put("tag", polyline.tag)
        delegate.notify("onPolylineClick", data)
    }

    override fun onMarkerDrag(marker: Marker) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("markerId", marker.id)
        data.put("latitude", marker.position.latitude)
        data.put("longitude", marker.position.longitude)
        data.put("title", marker.title)
        data.put("snippet", marker.snippet)
        delegate.notify("onMarkerDrag", data)
    }

    override fun onMarkerDragStart(marker: Marker) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("markerId", marker.id)
        data.put("latitude", marker.position.latitude)
        data.put("longitude", marker.position.longitude)
        data.put("title", marker.title)
        data.put("snippet", marker.snippet)
        delegate.notify("onMarkerDragStart", data)
    }

    override fun onMarkerDragEnd(marker: Marker) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("markerId", marker.id)
        data.put("latitude", marker.position.latitude)
        data.put("longitude", marker.position.longitude)
        data.put("title", marker.title)
        data.put("snippet", marker.snippet)
        delegate.notify("onMarkerDragEnd", data)
    }

    override fun onMyLocationButtonClick(): Boolean {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        delegate.notify("onMyLocationButtonClick", data)
        return false
    }

    override fun onMyLocationClick(location: Location) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("latitude", location.latitude)
        data.put("longitude", location.longitude)
        delegate.notify("onMyLocationClick", data)
    }

    override fun onCameraIdle() {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("bounds", getLatLngBoundsJSObject(getLatLngBounds()))
        data.put("bearing", this@CapacitorGoogleMap.googleMap?.cameraPosition?.bearing)
        data.put("latitude", this@CapacitorGoogleMap.googleMap?.cameraPosition?.target?.latitude)
        data.put("longitude", this@CapacitorGoogleMap.googleMap?.cameraPosition?.target?.longitude)
        data.put("tilt", this@CapacitorGoogleMap.googleMap?.cameraPosition?.tilt)
        data.put("zoom", this@CapacitorGoogleMap.googleMap?.cameraPosition?.zoom)
        delegate.notify("onCameraIdle", data)
        delegate.notify("onBoundsChanged", data)
    }

    override fun onCameraMoveStarted(reason: Int) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("isGesture", reason == 1)
        delegate.notify("onCameraMoveStarted", data)
    }

    override fun onInfoWindowClick(marker: Marker) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("markerId", marker.id)
        data.put("latitude", marker.position.latitude)
        data.put("longitude", marker.position.longitude)
        data.put("title", marker.title)
        data.put("snippet", marker.snippet)
        delegate.notify("onInfoWindowClick", data)
    }

    override fun onCameraMove() {
        debounceJob?.cancel()
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(150)
            clusterManager?.cluster()
            updateInfoWindowsForCurrentZoom()
        }
    }
    private fun updateInfoWindowsForCurrentZoom() {
        CoroutineScope(Dispatchers.Main).launch {
            val currentZoom = googleMap?.cameraPosition?.zoom ?: 0f
            val shouldShowInfoWindows = currentZoom >= multipleInfoWindowZoomLevel

            // Immediate update without delay
            if (shouldShowInfoWindows) {
                showAllMultipleInfoWindows()
            } else {
                hideAllMultipleInfoWindows()
            }

            cleanupStaleInfoWindows();
        }
    }

    private fun cleanupStaleInfoWindows() {
        CoroutineScope(Dispatchers.Main).launch {
            val currentZoom = googleMap?.cameraPosition?.zoom ?: 0f
            val shouldShowInfoWindows = currentZoom >= multipleInfoWindowZoomLevel

            val markersToRemove = mutableListOf<String>()

            // Clean up info windows that don't have corresponding markers anymore
            infoWindowMarkers.forEach { (originalMarkerId, infoWindowMarker) ->
                val markerExists = markers.containsKey(originalMarkerId) &&
                        markers[originalMarkerId]?.googleMapMarker != null

                if (!markerExists) {
                    // Remove if original marker doesn't exist
                    markersToRemove.add(originalMarkerId)
                    infoWindowMarker.remove()
                } else if (!shouldShowInfoWindows) {
                    // Remove if zoomed out beyond the threshold
                    markersToRemove.add(originalMarkerId)
                    infoWindowMarker.remove()
                }
            }

            // Remove from our tracking map
            markersToRemove.forEach { infoWindowMarkers.remove(it) }
        }
    }

    private fun showAllMultipleInfoWindows() {
        // Find all markers that should have multiple info windows and create them
        markers.values.forEach { marker ->
            if (marker.infoIcon?.contains("multiple_info_window") == true &&
                infoWindowMarkers[marker.id] == null &&
                marker.googleMapMarker != null) {
                createInfoWindowAsMarker(marker, marker.googleMapMarker!!)
            }
        }
    }

    private fun hideAllMultipleInfoWindows() {
        // Remove all multiple info window markers
        infoWindowMarkers.values.forEach { it.remove() }
        infoWindowMarkers.clear()
    }

    override fun onPolygonClick(polygon: Polygon) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("polygonId", polygon.id)
        data.put("tag", polygon.tag)
        delegate.notify("onPolygonClick", data)
    }

    override fun onCircleClick(circle: Circle) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("circleId", circle.id)
        data.put("tag", circle.tag)
        data.put("latitude", circle.center.latitude)
        data.put("longitude", circle.center.longitude)
        data.put("radius", circle.radius)

        delegate.notify("onCircleClick", data)
    }
    override fun onInfoWindowClose(marker: Marker) {
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("markerId", marker.id)
        data.put("latitude", marker.position.latitude)
        data.put("longitude", marker.position.longitude)
        data.put("title", marker.title)
        data.put("snippet", marker.snippet)
        delegate.notify("onInfoWindowClose", data)
    }
}

fun getLatLngBoundsJSObject(bounds: LatLngBounds): JSObject {
    val data = JSObject()

    val southwestJS = JSObject()
    val centerJS = JSObject()
    val northeastJS = JSObject()

    southwestJS.put("lat", bounds.southwest.latitude)
    southwestJS.put("lng", bounds.southwest.longitude)
    centerJS.put("lat", bounds.center.latitude)
    centerJS.put("lng", bounds.center.longitude)
    northeastJS.put("lat", bounds.northeast.latitude)
    northeastJS.put("lng", bounds.northeast.longitude)

    data.put("southwest", southwestJS)
    data.put("center", centerJS)
    data.put("northeast", northeastJS)

    return data
}
