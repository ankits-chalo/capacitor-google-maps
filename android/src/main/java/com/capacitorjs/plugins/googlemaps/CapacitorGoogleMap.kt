package com.capacitorjs.plugins.googlemaps

import BusesMarker
import BusesMarkerRenderer
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.location.Location
import android.os.Build
import android.util.Log
import android.util.Property
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
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
import java.io.InputStream
import java.net.URL


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
    private var clusterManager: ClusterManager<CapacitorGoogleMapMarker>? = null
    private var markerIdOnWeb = ArrayList<String>()
    private var animator: Animator? = null
    private var polylineCords: MutableList<LatLng> = mutableListOf()

    private val isReadyChannel = Channel<Boolean>()
    private var debounceJob: Job? = null

    init {
        val bridge = delegate.bridge

        mapView = MapView(bridge.context, config.googleMapOptions)
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

                        val markerOptions = it.getMarkerOptions()

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

                    val markerOptions = marker.getMarkerOptions()
                    if(marker.rotation == 1) {
                        markerOptions.rotation(getAngle(marker.coordinate))
                    }

                    if(marker.iconUrl?.contains("buses_custom_marker") == true) {
                        val bridge = delegate.bridge
                        val busesMarker = BusesMarker(bridge.context)
                        markerOptions.icon(busesMarker.getMarkerIcon(marker.title, marker.iconUrl!!))
                    }


                    val googleMapMarker = googleMap?.addMarker(markerOptions)
                    googleMapMarker?.tag = marker

                    if (!marker.infoIcon.isNullOrEmpty()) {
                        if(marker.infoIcon.equals("buses_info_icon")) {
                            val bridge = delegate.bridge
                            googleMapMarker?.tag = marker
                            googleMap?.setInfoWindowAdapter(BusesMarkerInfoWindow(bridge.context))
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

                    if (clusterManager == null) {
                        marker.googleMapMarker = googleMapMarker
                    } else {
                        if (!marker.infoIcon.isNullOrEmpty()) {
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

    private fun animateMarker(marker: Marker?, finalPosition: LatLng) {
        // Return early if the marker is null
        if (marker == null) return

        val startPosition = marker.position // The initial position of the marker
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 2000 // 2 seconds

        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val v = valueAnimator.animatedFraction
            val interpolatedLat = (1 - v) * startPosition.latitude + v * finalPosition.latitude
            val interpolatedLng = (1 - v) * startPosition.longitude + v * finalPosition.longitude
            val currentPosition = LatLng(interpolatedLat, interpolatedLng)
            marker.position = currentPosition
        }
        animator.start()
    }

    private fun animateMarkerInsideCluster(clusterItem: CapacitorGoogleMapMarker, newPosition: LatLng) {
        val oldPosition = clusterItem.coordinate
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 800 // duration of the animation in milliseconds

        val marker = (clusterManager?.renderer as? BusesMarkerRenderer)?.getMarker(clusterItem)
        valueAnimator.addUpdateListener { animator ->
            val v = animator.animatedFraction
            val lng = v * newPosition.longitude + (1 - v) * oldPosition.longitude
            val lat = v * newPosition.latitude + (1 - v) * oldPosition.latitude
            val newLocation = LatLng(lat, lng)

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
                        if (clusterManager == null) {
                            // Below line animate the marker
                            animateMarker(oldMarker?.googleMapMarker, marker!!.coordinate)
                            // Set the camera position of map to the centre of the marker
    //                    googleMap?.animateCamera(CameraUpdateFactory.newLatLng(marker!!.coordinate), 5000, null)

                            if (marker.rotation == 1) {
                                oldMarker?.googleMapMarker?.rotation = getAngle(marker!!.coordinate)
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
                                marker?.iconUrl?.let { oldMarker?.updateIcon(it, marker.title) }
                            }

                            if (!marker.infoIcon.isNullOrEmpty()) {
                                if (marker.infoIcon.equals("buses_info_icon") && marker.infoData?.getBoolean("showInfoIcon") == true) {
                                    val bridge = delegate.bridge
                                    oldMarker?.googleMapMarker?.tag = marker
                                    oldMarker?.googleMapMarker?.showInfoWindow()
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

                // Loop through the list of coordinates
                for (cord in cords) {
                    builder.include(cord)
                }

                // Build the bounds
                val bounds = builder.build()

                // Create a camera update with the bounds and the specified padding
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))

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


                if(marker.rotation == 1){
                    oldMarker?.googleMapMarker?.rotation = getAngle(marker!!.coordinate)
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
                    marker?.iconUrl?.let { oldMarker?.updateIcon(it, marker.title) }
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
                googleMap?.setOnMarkerClickListener(clusterManager)
                googleMap?.setOnInfoWindowClickListener(clusterManager)


                setClusterListeners()

                // add existing markers to the cluster
                if (markers.isNotEmpty()) {
                    clusterManager?.clearItems() // Clear existing items in the cluster manager
                    for ((_, marker) in markers) {
                        marker.googleMapMarker?.remove()
                        // marker.googleMapMarker = null
                    }
                    clusterManager?.addItems(markers.values)
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
                        val googleMapMarker = googleMap?.addMarker(marker.getMarkerOptions())
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

    fun removeMarker(id: String, callback: (error: GoogleMapsError?) -> Unit) {
        try {
            googleMap ?: throw GoogleMapNotAvailable()

            val marker = markers[id]
            marker ?: throw MarkerNotFoundError()

            CoroutineScope(Dispatchers.Main).launch {
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
        // Disable the 2 button that shows on bottom right after the click on marker
        googleMap?.uiSettings?.isMapToolbarEnabled = false;
        val data = JSObject()
        data.put("mapId", this@CapacitorGoogleMap.id)
        data.put("markerId", marker.id)
        data.put("latitude", marker.position.latitude)
        data.put("longitude", marker.position.longitude)
        data.put("title", marker.title)
        data.put("snippet", marker.snippet)
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
            delay(100)
            clusterManager?.cluster()
        }
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
