// swiftlint:disable file_length
import Foundation
import Capacitor
import GoogleMaps
import GoogleMapsUtils

extension GMSMapViewType {
    static func fromString(mapType: String) -> GMSMapViewType {
        switch mapType {
        case "Normal":
            return .normal
        case "Hybrid":
            return .hybrid
        case "Satellite":
            return .satellite
        case "Terrain":
            return .terrain
        case "None":
            return .none
        default:
            print("CapacitorGoogleMaps Warning: unknown mapView type '\(mapType)'.  Defaulting to normal.")
            return .normal
        }
    }
    static func toString(mapType: GMSMapViewType) -> String {
        switch mapType {
        case .normal:
            return "Normal"
        case .hybrid:
            return "Hybrid"
        case .satellite:
            return "Satellite"
        case .terrain:
            return "Terrain"
        case .none:
            return "None"
        default:
            return "Normal"
        }
    }
}

extension CGRect {
    static func fromJSObject(_ jsObject: JSObject) throws -> CGRect {
        guard let width = jsObject["width"] as? Double else {
            throw GoogleMapErrors.invalidArguments("bounds object is missing the required 'width' property")
        }

        guard let height = jsObject["height"] as? Double else {
            throw GoogleMapErrors.invalidArguments("bounds object is missing the required 'height' property")
        }

        guard let x = jsObject["x"] as? Double else {
            throw GoogleMapErrors.invalidArguments("bounds object is missing the required 'x' property")
        }

        guard let y = jsObject["y"] as? Double else {
            throw GoogleMapErrors.invalidArguments("bounds object is missing the required 'y' property")
        }

        return CGRect(x: x, y: y, width: width, height: height)
    }
}

// swiftlint:disable type_body_length
@objc(CapacitorGoogleMapsPlugin)
public class CapacitorGoogleMapsPlugin: CAPPlugin, GMSMapViewDelegate {
    private var maps = [String: Map]()
    private var isInitialized = false

    func checkLocationPermission() -> String {
        let locationState: String

        switch CLLocationManager.authorizationStatus() {
        case .notDetermined:
            locationState = "prompt"
        case .restricted, .denied:
            locationState = "denied"
        case .authorizedAlways, .authorizedWhenInUse:
            locationState = "granted"
        @unknown default:
            locationState = "prompt"
        }

        return locationState
    }

    @objc func create(_ call: CAPPluginCall) {
        do {
            if !isInitialized {
                guard let apiKey = call.getString("apiKey") else {
                    throw GoogleMapErrors.invalidAPIKey
                }

                GMSServices.provideAPIKey(apiKey)
                isInitialized = true
            }

            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let configObj = call.getObject("config") else {
                throw GoogleMapErrors.invalidArguments("config object is missing")
            }

            let forceCreate = call.getBool("forceCreate", false)

            let config = try GoogleMapConfig(fromJSObject: configObj)

            if self.maps[id] != nil {
                if !forceCreate {
                    call.resolve()
                    return
                }

                let removedMap = self.maps.removeValue(forKey: id)
                removedMap?.destroy()
            }

            DispatchQueue.main.sync {
                let newMap = Map(id: id, config: config, delegate: self)
                self.maps[id] = newMap
            }

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func destroy(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let removedMap = self.maps.removeValue(forKey: id) else {
                throw GoogleMapErrors.mapNotFound
            }

            removedMap.destroy()
            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func enableTouch(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            map.enableTouch()

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func disableTouch(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            map.disableTouch()

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func addMarker(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let markerObj = call.getObject("marker") else {
                throw GoogleMapErrors.invalidArguments("marker object is missing")
            }

            let marker = try Marker(fromJSObject: markerObj)

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            let markerId = try map.addMarker(marker: marker)

            call.resolve(["id": String(markerId)])

        } catch {
            handleError(call, error: error)
        }
    }

    @objc func addMarkers(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let markerObjs = call.getArray("markers") as? [JSObject] else {
                throw GoogleMapErrors.invalidArguments("markers array is missing")
            }

            if markerObjs.isEmpty {
                throw GoogleMapErrors.invalidArguments("markers requires at least one marker")
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            var markers: [Marker] = []

            try markerObjs.forEach { marker in
                let marker = try Marker(fromJSObject: marker)
                markers.append(marker)
            }

            let ids = try map.addMarkers(markers: markers)

            call.resolve(["ids": ids.map({ id in
                return String(id)
            })])

        } catch {
            handleError(call, error: error)
        }
    }

    @objc func removeMarkers(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let markerIdStrings = call.getArray("markerIds") as? [String] else {
                throw GoogleMapErrors.invalidArguments("markerIds are invalid or missing")
            }

            if markerIdStrings.isEmpty {
                throw GoogleMapErrors.invalidArguments("markerIds requires at least one marker id")
            }

            let ids: [Int] = try markerIdStrings.map { idString in
                guard let markerId = Int(idString) else {
                    throw GoogleMapErrors.invalidArguments("markerIds are invalid or missing")
                }

                return markerId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            try map.removeMarkers(ids: ids)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func removeMarker(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let markerIdString = call.getString("markerId") else {
                throw GoogleMapErrors.invalidArguments("markerId is invalid or missing")
            }

            guard let markerId = Int(markerIdString) else {
                throw GoogleMapErrors.invalidArguments("markerId is invalid or missing")
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            try map.removeMarker(id: markerId)
            

            call.resolve()

        } catch {
            handleError(call, error: error)
        }
    }

@objc func addPolygons(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let shapeObjs = call.getArray("polygons") as? [JSObject] else {
                throw GoogleMapErrors.invalidArguments("polygons array is missing")
            }

            if shapeObjs.isEmpty {
                throw GoogleMapErrors.invalidArguments("polygons requires at least one shape")
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            var shapes: [Polygon] = []

            try shapeObjs.forEach { shapeObj in
                let polygon = try Polygon(fromJSObject: shapeObj)
                shapes.append(polygon)
            }

            let ids = try map.addPolygons(polygons: shapes)

            call.resolve(["ids": ids.map({ id in
                return String(id)
            })])
        } catch {
            handleError(call, error: error)
        }
    }

    // @objc func addPolylines(_ call: CAPPluginCall) {
    //     do {
    //         guard let id = call.getString("id") else {
    //             throw GoogleMapErrors.invalidMapId
    //         }

    //         guard let lineObjs = call.getArray("polylines") as? [JSObject] else {
    //             throw GoogleMapErrors.invalidArguments("polylines array is missing")
    //         }

    //         if lineObjs.isEmpty {
    //             throw GoogleMapErrors.invalidArguments("polylines requires at least one line")
    //         }

    //         guard let map = self.maps[id] else {
    //             throw GoogleMapErrors.mapNotFound
    //         }

    //         var lines: [Polyline] = []

    //         try lineObjs.forEach { lineObj in
    //             let line = try Polyline(fromJSObject: lineObj)
    //             lines.append(line)
    //         }

    //         let ids = try map.addPolylines(lines: lines)

    //         call.resolve(["ids": ids.map({ id in
    //             return String(id)
    //         })])
    //     } catch {
    //         handleError(call, error: error)
    //     }
    // }

    @objc func removePolygons(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let polygonIdsStrings = call.getArray("polygonIds") as? [String] else {
                throw GoogleMapErrors.invalidArguments("polygonIds are invalid or missing")
            }

            if polygonIdsStrings.isEmpty {
                throw GoogleMapErrors.invalidArguments("polygonIds requires at least one polygon id")
            }

            let ids: [Int] = try polygonIdsStrings.map { idString in
                guard let polygonId = Int(idString) else {
                    throw GoogleMapErrors.invalidArguments("polygonIds are invalid or missing")
                }

                return polygonId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            try map.removePolygons(ids: ids)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func addCircles(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let circleObjs = call.getArray("circles") as? [JSObject] else {
                throw GoogleMapErrors.invalidArguments("circles array is missing")
            }

            if circleObjs.isEmpty {
                throw GoogleMapErrors.invalidArguments("circles requires at least one circle")
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            var circles: [Circle] = []

            try circleObjs.forEach { circleObj in
                let circle = try Circle(from: circleObj)
                circles.append(circle)
            }

            let ids = try map.addCircles(circles: circles)

            call.resolve(["ids": ids.map({ id in
                return String(id)
            })])
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func removeCircles(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let circleIdsStrings = call.getArray("circleIds") as? [String] else {
                throw GoogleMapErrors.invalidArguments("circleIds are invalid or missing")
            }

            if circleIdsStrings.isEmpty {
                throw GoogleMapErrors.invalidArguments("circleIds requires at least one cicle id")
            }

            let ids: [Int] = try circleIdsStrings.map { idString in
                guard let circleId = Int(idString) else {
                    throw GoogleMapErrors.invalidArguments("circleIds are invalid or missing")
                }

                return circleId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            try map.removeCircles(ids: ids)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func removePolylines(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let polylineIdsStrings = call.getArray("polylineIds") as? [String] else {
                throw GoogleMapErrors.invalidArguments("polylineIds are invalid or missing")
            }

            if polylineIdsStrings.isEmpty {
                throw GoogleMapErrors.invalidArguments("polylineIds requires at least one polyline id")
            }

            let ids: [Int] = try polylineIdsStrings.map { idString in
                guard let polylineId = Int(idString) else {
                    throw GoogleMapErrors.invalidArguments("polylineIds are invalid or missing")
                }

                return polylineId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            try map.removePolylines(ids: ids)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func setCamera(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let configObj = call.getObject("config") else {
                throw GoogleMapErrors.invalidArguments("config object is missing")
            }

            let config = try GoogleMapCameraConfig(fromJSObject: configObj)

            try map.setCamera(config: config)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func getMapType(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            let mapType = GMSMapViewType.toString(mapType: map.getMapType())

            call.resolve([
                "type": mapType
            ])
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func setMapType(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let mapTypeString = call.getString("mapType") else {
                throw GoogleMapErrors.invalidArguments("mapType is missing")
            }

            let mapType = GMSMapViewType.fromString(mapType: mapTypeString)

            try map.setMapType(mapType: mapType)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }
    
    @objc func enableIndoorMaps(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let enabled = call.getBool("enabled") else {
                throw GoogleMapErrors.invalidArguments("enabled is missing")
            }

            try map.enableIndoorMaps(enabled: enabled)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func enableTrafficLayer(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let enabled = call.getBool("enabled") else {
                throw GoogleMapErrors.invalidArguments("enabled is missing")
            }

            try map.enableTrafficLayer(enabled: enabled)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func enableAccessibilityElements(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let enabled = call.getBool("enabled") else {
                throw GoogleMapErrors.invalidArguments("enabled is missing")
            }

            try map.enableAccessibilityElements(enabled: enabled)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func setPadding(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let configObj = call.getObject("padding") else {
                throw GoogleMapErrors.invalidArguments("padding is missing")
            }

            let padding = try GoogleMapPadding.init(fromJSObject: configObj)

            try map.setPadding(padding: padding)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func enableCurrentLocation(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let enabled = call.getBool("enabled") else {
                throw GoogleMapErrors.invalidArguments("enabled is missing")
            }

            let locationStatus = checkLocationPermission()

            if enabled &&  !(locationStatus == "granted" || locationStatus == "prompt") {
                throw GoogleMapErrors.permissionsDeniedLocation
            }

// Uncomment when user loaction is required
//            try map.enableCurrentLocation(enabled: enabled)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func enableClustering(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            map.enableClustering()
            call.resolve()

        } catch {
            handleError(call, error: error)
        }
    }

    @objc func disableClustering(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            map.disableClustering()
            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func onScroll(_ call: CAPPluginCall) {
        call.unavailable("not supported on iOS")
    }

    @objc func onResize(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let mapBoundsObj = call.getObject("mapBounds") else {
                throw GoogleMapErrors.invalidArguments("map bounds not set")
            }

            let mapBounds = try CGRect.fromJSObject(mapBoundsObj)

            // map.updateRender(mapBounds: mapBounds)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func onDisplay(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let mapBoundsObj = call.getObject("mapBounds") else {
                throw GoogleMapErrors.invalidArguments("map bounds not set")
            }

            let mapBounds = try CGRect.fromJSObject(mapBoundsObj)

            map.rebindTargetContainer(mapBounds: mapBounds)

            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func getMapBounds(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            try DispatchQueue.main.sync {
                guard let bounds = map.getMapLatLngBounds() else {
                    throw GoogleMapErrors.unhandledError("Google Map Bounds could not be found.")
                }

                call.resolve(
                    formatMapBoundsForResponse(
                        bounds: bounds,
                        cameraPosition: map.mapViewController.GMapView.camera
                    )
                )
            }
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func mapBoundsContains(_ call: CAPPluginCall) {
        do {
            guard let boundsObject = call.getObject("bounds") else {
                throw GoogleMapErrors.invalidArguments("Invalid bounds provided")
            }

            guard let pointObject = call.getObject("point") else {
                throw GoogleMapErrors.invalidArguments("Invalid point provided")
            }

            let bounds = try getGMSCoordinateBounds(boundsObject)
            let point = try getCLLocationCoordinate(pointObject)

            call.resolve([
                "contains": bounds.contains(point)
            ])
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func fitBounds(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            guard let boundsObject = call.getObject("bounds") else {
                throw GoogleMapErrors.invalidArguments("Invalid bounds provided")
            }

            let bounds = try getGMSCoordinateBounds(boundsObject)
            let padding = CGFloat(call.getInt("padding", 0))

            map.fitBounds(bounds: bounds, padding: padding)
            call.resolve()
        } catch {
            handleError(call, error: error)
        }
    }

    @objc func mapBoundsExtend(_ call: CAPPluginCall) {
        do {
            guard let boundsObject = call.getObject("bounds") else {
                throw GoogleMapErrors.invalidArguments("Invalid bounds provided")
            }

            guard let pointObject = call.getObject("point") else {
                throw GoogleMapErrors.invalidArguments("Invalid point provided")
            }

            let bounds = try getGMSCoordinateBounds(boundsObject)
            let point = try getCLLocationCoordinate(pointObject)

            DispatchQueue.main.sync {
                let newBounds = bounds.includingCoordinate(point)
                call.resolve([
                    "bounds": formatMapBoundsForResponse(newBounds)
                ])
            }
        } catch {
            handleError(call, error: error)
        }
    }

    private func getGMSCoordinateBounds(_ bounds: JSObject) throws -> GMSCoordinateBounds {
        guard let southwest = bounds["southwest"] as? JSObject else {
            throw GoogleMapErrors.unhandledError("Bounds southwest property not formatted properly.")
        }

        guard let northeast = bounds["northeast"] as? JSObject else {
            throw GoogleMapErrors.unhandledError("Bounds northeast property not formatted properly.")
        }

        return GMSCoordinateBounds(
            coordinate: try getCLLocationCoordinate(southwest),
            coordinate: try getCLLocationCoordinate(northeast)
        )
    }

    private func getCLLocationCoordinate(_ point: JSObject) throws -> CLLocationCoordinate2D {
        guard let lat = point["lat"] as? Double else {
            throw GoogleMapErrors.unhandledError("Point lat property not formatted properly.")
        }

        guard let lng = point["lng"] as? Double else {
            throw GoogleMapErrors.unhandledError("Point lng property not formatted properly.")
        }

        return CLLocationCoordinate2D(latitude: lat, longitude: lng)
    }

    private func formatMapBoundsForResponse(bounds: GMSCoordinateBounds?, cameraPosition: GMSCameraPosition) -> PluginCallResultData {
        return [
            "southwest": [
                "lat": bounds?.southWest.latitude,
                "lng": bounds?.southWest.longitude
            ],
            "center": [
                "lat": cameraPosition.target.latitude,
                "lng": cameraPosition.target.longitude
            ],
            "northeast": [
                "lat": bounds?.northEast.latitude,
                "lng": bounds?.northEast.longitude
            ]
        ]
    }

    private func formatMapBoundsForResponse(_ bounds: GMSCoordinateBounds) -> PluginCallResultData {
        let centerLatitude = (bounds.southWest.latitude + bounds.northEast.latitude) / 2.0
        let centerLongitude = (bounds.southWest.longitude + bounds.northEast.longitude) / 2.0

        return [
            "southwest": [
                "lat": bounds.southWest.latitude,
                "lng": bounds.southWest.longitude
            ],
            "center": [
                "lat": centerLatitude,
                "lng": centerLongitude
            ],
            "northeast": [
                "lat": bounds.northEast.latitude,
                "lng": bounds.northEast.longitude
            ]
        ]
    }

    private func handleError(_ call: CAPPluginCall, error: Error) {
        let errObject = getErrorObject(error)
        call.reject(errObject.message, "\(errObject.code)", error, [:])
    }

    private func findMapIdByMapView(_ mapView: GMSMapView) -> String {
        for (mapId, map) in self.maps {
            if map.mapViewController.GMapView === mapView {
                return mapId
            }
        }
        return ""
    }


    @objc func toogleScrollGesture(_ call: CAPPluginCall){
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            let isEnabled = call.getBool("enabled") ?? false

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

             try map.toogleScrollGesture(enabled:isEnabled)

            call.resolve(["id": String(id)])

        } catch {
            handleError(call, error: error)
        }
    }
    
    @objc func drawCircle(_ call: CAPPluginCall){
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let circleObj = call.getObject("circleProps") else {
                throw GoogleMapErrors.invalidArguments("circle object is missing")
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            try map.drawCircle(circleProps:circleObj,id:id)

            call.resolve(["id": String(id)])

        } catch {
            handleError(call, error: error)
        }
    }

    @objc func setMarkerPosition(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let markerObj = call.getObject("marker") else {
                throw GoogleMapErrors.invalidArguments("marker object is missing")
            }
            let marker = try Marker(fromJSObject: markerObj)

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            let markerId = try map.setMarkerPosition(marker: marker)

            call.resolve(["id": String(markerId)])

        } catch {
            handleError(call, error: error)
        }
    }
    
    @objc func fitBound(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }
            
            guard let cordsObjectsArray = call.getArray("cords") as? [JSObject] else {
                throw GoogleMapErrors.invalidArguments("Coordinate array is missing for fitBound")
            }
            
            if cordsObjectsArray.isEmpty {
                throw GoogleMapErrors.invalidArguments("FitBound requires at least one coordinate")
            }
            
            var cords: [LatLng] = []
            
            try cordsObjectsArray.forEach { cord in
            guard let lat = cord["lat"] as? Double, let lng = cord["lng"] as? Double else {
                throw GoogleMapErrors.invalidArguments("LatLng object is missing the required 'lat' and/or 'lng' property")
            }
                let cord = LatLng(lat: lat, lng: lng)
                cords.append(cord)
            }
            
            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }
            
            map.fitBound(cords: cords, padding: 100)
            call.resolve()
            
        } catch {
            handleError(call, error: error)
        }
    }
    
    @objc func updateInfoWindow(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let markerObj = call.getObject("marker") else {
                throw GoogleMapErrors.invalidArguments("marker object is missing")
            }
            let marker = try Marker(fromJSObject: markerObj)
            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            let markerId = try map.updateInfoWindow(marker: marker)

            call.resolve(["id": String(markerId)])

        } catch {
            handleError(call, error: error)
        }
    }

    @objc func addPolyline(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let cordsObjs = call.getArray("cords") as? [JSObject] else {
                throw GoogleMapErrors.invalidArguments("cordinate array is missing")
            }

             guard let polylineProps = call.getObject("polylineProps") else {
                throw GoogleMapErrors.invalidArguments("polylineProps is missing")
            }

            guard let strokeWidth = polylineProps["strokeWidth"] as? Double else {
                throw GoogleMapErrors.invalidArguments("strokeWidth is missing")
            }

            guard let strokeColor = polylineProps["strokeColor"] as? String else {
                throw GoogleMapErrors.invalidArguments("strokeColor is missing")
            }

            guard let strokeOpacity = polylineProps["strokeOpacity"] as? Double else {
                throw GoogleMapErrors.invalidArguments("strokeOpacity is missing")
            }

            if cordsObjs.isEmpty {
                throw GoogleMapErrors.invalidArguments("cordinates requires at least one cordinate")
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }

            var cords: [LatLng] = []

            try cordsObjs.forEach { cord in
            guard let lat = cord["lat"] as? Double, let lng = cord["lng"] as? Double else {
                throw GoogleMapErrors.invalidArguments("LatLng object is missing the required 'lat' and/or 'lng' property")
            }
                let cord = LatLng(lat: lat, lng: lng)
                cords.append(cord)
            }

            let ids = try map.addPolyline(cords: cords, strokeWidth:strokeWidth, strokeColor:strokeColor, strokeOpacity:strokeOpacity)

            call.resolve(["ids": ids.map({ id in
                return String(id)
            })])

        } catch {
            handleError(call, error: error)
        }
    }

    @objc func addPolylines(_ call: CAPPluginCall) {
        do {
            guard let id = call.getString("id") else {
                throw GoogleMapErrors.invalidMapId
            }

            guard let polylineObjs = call.getArray("cords") as? [JSObject] else {
                throw GoogleMapErrors.invalidArguments("cordinate array is missing")
            }
            
            guard let polylineProps = call.getObject("polylineProps") else {
               throw GoogleMapErrors.invalidArguments("polylineProps is missing")
           }

            guard let strokeWidth = polylineProps["strokeWidth"] as? Double else {
                throw GoogleMapErrors.invalidArguments("strokeWidth is missing")
            }

            guard let strokeColor = polylineProps["strokeColor"] as? String else {
                throw GoogleMapErrors.invalidArguments("strokeColor is missing")
            }

            guard let strokeOpacity = polylineProps["strokeOpacity"] as? Double else {
                throw GoogleMapErrors.invalidArguments("strokeOpacity is missing")
            }
            
            guard let zIndex = polylineProps["zIndex"] as? Double else {
                throw GoogleMapErrors.invalidArguments("zIndex is missing")
            }

            if polylineObjs.isEmpty {
                throw GoogleMapErrors.invalidArguments("cordinates requires at least one cordinate")
            }

            guard let map = self.maps[id] else {
                throw GoogleMapErrors.mapNotFound
            }
            var polylines: [[LatLng]] = []
            var strokeColors: [String] = []
            var strokeWidths: [Double] = []
            var zIndexs: [Double] = []
            var strokeOpacities: [Double] = []
            
            try polylineObjs.forEach { polylineObject in
                guard let cordsObjs = polylineObject["polylinePath"] as? [JSObject] else {
                    throw GoogleMapErrors.invalidArguments("polyline path is missing")
                }
                var cords: [LatLng] = []

                try cordsObjs.forEach { cord in
                guard let lat = cord["lat"] as? Double, let lng = cord["lng"] as? Double else {
                    throw GoogleMapErrors.invalidArguments("LatLng object is missing the required 'lat' and/or 'lng' property")
                }
                    let cord = LatLng(lat: lat, lng: lng)
                    cords.append(cord)
                }
                
                var objStokeColor: String
                if let polylineStrokeColor = polylineObject["polylineStrokeColor"] as? String {
                    objStokeColor = polylineStrokeColor
                } else {
                    // If the current object doesn't have stroke color, set the default color
                    objStokeColor = strokeColor as String
                }

                var objStrokeWidth: Double
                if let polylineStrokeWidth = polylineObject["polylineStrokeWidth"] as? Double {
                    objStrokeWidth = polylineStrokeWidth
                } else {
                    // If the current object doesn't have stroke width, set the default width
                    objStrokeWidth = strokeWidth as Double
                }

                var objZIndex: Double
                if let polylineZIndex = polylineObject["polylineZIndex"] as? Double {
                    objZIndex = polylineZIndex
                } else {
                    // If the current object doesn't have a Z index, set the default Z index
                    objZIndex = zIndex as Double
                }

                var objStrokeOpacities: Double
                if let polylineOpacity = polylineObject["polylineOpacity"] as? Double {
                    objStrokeOpacities = polylineOpacity
                } else {
                    // If the current object doesn't have stroke opacity, set the default opacity
                    objStrokeOpacities = strokeOpacity as Double
                }
                
                strokeColors.append(objStokeColor)
                strokeWidths.append(objStrokeWidth)
                zIndexs.append(objZIndex)
                strokeOpacities.append(objStrokeOpacities)
                polylines.append(cords)
            }
      
            let ids = try map.addPolylines(polylines:polylines, strokeColors:strokeColors, strokeWidths:strokeWidths, zIndexs:zIndexs, strokeOpacities:strokeOpacities)

            call.resolve(["ids": ids.map({ id in
                return String(id)
            })])

        } catch {
            handleError(call, error: error)
        }
    }

    // --- EVENT LISTENERS ---

    // onCameraIdle
    public func mapView(_ mapView: GMSMapView, idleAt cameraPosition: GMSCameraPosition) {
        let mapId = self.findMapIdByMapView(mapView)
        let map = self.maps[mapId]
        let bounds = map?.getMapLatLngBounds()

        let data: PluginCallResultData = [
            "mapId": mapId,
            "bounds": formatMapBoundsForResponse(
                bounds: bounds,
                cameraPosition: cameraPosition
            ),
            "bearing": cameraPosition.bearing,
            "latitude": cameraPosition.target.latitude,
            "longitude": cameraPosition.target.longitude,
            "tilt": cameraPosition.viewingAngle,
            "zoom": cameraPosition.zoom
        ]

        self.notifyListeners("onBoundsChanged", data: data)
        self.notifyListeners("onCameraIdle", data: data)
    }

    // onCameraMoveStarted
    // public func mapView(_ mapView: GMSMapView, willMove gesture: Bool) {
    //     self.notifyListeners("onCameraMoveStarted", data: [
    //         "mapId": self.findMapIdByMapView(mapView),
    //         "isGesture": gesture
    //     ])
    // }

    // onMapClick
    public func mapView(_ mapView: GMSMapView, didTapAt coordinate: CLLocationCoordinate2D) {
        self.notifyListeners("onMapClick", data: [
            "mapId": self.findMapIdByMapView(mapView),
            "latitude": coordinate.latitude,
            "longitude": coordinate.longitude
        ])
    }

    // onPolygonClick, onPolylineClick, onCircleClick
    public func mapView(_ mapView: GMSMapView, didTap overlay: GMSOverlay) {
        if let polygon = overlay as? GMSPolygon {
            self.notifyListeners("onPolygonClick", data: [
                "mapId": self.findMapIdByMapView(mapView),
                "polygonId": String(overlay.hash.hashValue),
                "tag": polygon.userData as? String
            ])
        }

        if let circle = overlay as? GMSCircle {
            self.notifyListeners("onCircleClick", data: [
                "mapId": self.findMapIdByMapView(mapView),
                "circleId": String(overlay.hash.hashValue),
                "tag": circle.userData as? String,
                "latitude": circle.position.latitude,
                "longitude": circle.position.longitude,
                "radius": circle.radius
            ])
        }

        if let polyline = overlay as? GMSPolyline {
            self.notifyListeners("onPolylineClick", data: [
                "mapId": self.findMapIdByMapView(mapView),
                "polylineId": String(overlay.hash.hashValue),
                "tag": polyline.userData as? String
            ])
        }
    }

    // onClusterClick, onMarkerClick
    public func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
        if let userData = marker.userData as? [String: Any],
              userData["type"] as? String == "infoWindow",
              let originalMarkerId = userData["originalMarkerId"] as? Int,
              let markerData = userData["markerData"] as? Marker {
               
               self.notifyListeners("onInfoWindowClick", data: [
                   "mapId": self.findMapIdByMapView(mapView),
                   "markerId": String(originalMarkerId),
                   "latitude": marker.position.latitude,
                   "longitude": marker.position.longitude,
                   "title": markerData.title ?? "",
                   "snippet": markerData.snippet ?? "",
                   "isCustomInfoWindow": true
               ])
               return true
           }
        if let cluster = marker.userData as? GMUCluster {
            var items: [[String: Any?]] = []

            var bounds = GMSCoordinateBounds()
            
            for item in cluster.items {
                items.append([
                    "markerId": String(item.hash.hashValue),
                    "latitude": item.position.latitude,
                    "longitude": item.position.longitude,
                    "title": item.title ?? "",
                    "snippet": item.snippet ?? ""
                ])
                
                let coordinate = CLLocationCoordinate2D(latitude: item.position.latitude, longitude: item.position.longitude)
                bounds = bounds.includingCoordinate(coordinate)
            }
            
            let update = GMSCameraUpdate.fit(bounds, withPadding: 100) // Adjust padding as needed
            mapView.animate(with: update)

            self.notifyListeners("onClusterClick", data: [
                "mapId": self.findMapIdByMapView(mapView),
                "latitude": cluster.position.latitude,
                "longitude": cluster.position.longitude,
                "size": cluster.count,
                "items": items
            ])
        } else {
            let userInfo = marker.userData as! Marker
            var title = marker.title
            if((title?.isEmpty) != nil) {
                title = userInfo.title
            }
            
            self.notifyListeners("onMarkerClick", data: [
                "mapId": self.findMapIdByMapView(mapView),
                "markerId": String(marker.hash.hashValue),
                "latitude": marker.position.latitude,
                "longitude": marker.position.longitude,
                "title": title ?? "",
                "snippet": marker.snippet ?? ""
            ])
            
        }
        return false
    }

    // onMarkerDragStart
    public func mapView(_ mapView: GMSMapView, didBeginDragging marker: GMSMarker) {
        self.notifyListeners("onMarkerDragStart", data: [
            "mapId": self.findMapIdByMapView(mapView),
            "markerId": String(marker.hash.hashValue),
            "latitude": marker.position.latitude,
            "longitude": marker.position.longitude,
            "title": marker.title ?? "",
            "snippet": marker.snippet ?? ""
        ])
    }

    // onMarkerDrag
    public func mapView(_ mapView: GMSMapView, didDrag marker: GMSMarker) {
        self.notifyListeners("onMarkerDrag", data: [
            "mapId": self.findMapIdByMapView(mapView),
            "markerId": String(marker.hash.hashValue),
            "latitude": marker.position.latitude,
            "longitude": marker.position.longitude,
            "title": marker.title ?? "",
            "snippet": marker.snippet ?? ""
        ])
    }

    // onMarkerDragEnd
    public func mapView(_ mapView: GMSMapView, didEndDragging marker: GMSMarker) {
        self.notifyListeners("onMarkerDragEnd", data: [
            "mapId": self.findMapIdByMapView(mapView),
            "markerId": String(marker.hash.hashValue),
            "latitude": marker.position.latitude,
            "longitude": marker.position.longitude,
            "title": marker.title ?? "",
            "snippet": marker.snippet ?? ""
        ])
    }
    
    public func mapView(_ mapView: GMSMapView, markerInfoWindow marker: GMSMarker) -> UIView? {
            guard let userData = marker.userData as? Marker,
                  let imageUrl = userData.infoIcon else {
                   return nil
            }
            if(imageUrl == "buses_info_icon") {
                let busesMarkerInfo = BusesMarkerInfoWindow.instanceFromNib()
                let busesMarkerLoading = BusesMarkerInfoWindowLoading.instanceFromNib()
                let busesTripNotRun = BusesTripNotRun.instanceFromNib()
                
                busesMarkerInfo.busCardName.text = marker.title
                busesMarkerLoading.busCardName.text = marker.title
                busesTripNotRun.busCardName.text = marker.title
                
                let totalCollection = userData.infoData?["totalCollctn"] as? String ?? "0"
                let tripStartTime = userData.infoData?["tripStartTime"] as? String ?? "trip time"
                let currentPassengerCount = userData.infoData?["currPsgCount"] as? Int ?? 0
                let occupancyLevel = userData.infoData?["occupancyLevel"] as? String ?? ""
                let routeName = userData.infoData?["routeName"] as? String ?? "route"
                let tripNotRunning = userData.infoData?["tripNotRunning"] as? Bool ?? false
                let apiFail = userData.infoData?["apiFail"] as? Bool ?? false
                let ticketStatus = userData.infoData?["ticketStatus"] as? String ?? ""
                let collectionName = userData.infoData?["collectionName"] as? String ?? ""
                let occupancyName = userData.infoData?["occupancyName"] as? String ?? ""
                let viewDetailsName = userData.infoData?["viewDetailsName"] as? String ?? ""
                let tripNotRunName = userData.infoData?["tripNotRunName"] as? String ?? ""
                let loadingName = userData.infoData?["loadingName"] as? String ?? ""
                let errorName = userData.infoData?["errorName"] as? String ?? ""
                let luggageTicketCount = userData.infoData?["luggageTicketCount"] as? Int ?? 0
                
                busesMarkerInfo.collectionText.text = collectionName
                busesMarkerInfo.occupancyText.text = occupancyName
                busesMarkerInfo.viewDetailsText.text = viewDetailsName
                busesTripNotRun.tripNotRunningText.text = tripNotRunName
                busesMarkerLoading.loadingText.text = loadingName

                if let loading = userData.infoData?["loading"] as? Bool, loading {
                    return busesMarkerLoading
                } else if (apiFail == true) {
                    busesMarkerLoading.loadingText.text = "Failed to load, try again later"
                    busesMarkerLoading.loadingText.textColor = UIColor(hexString: "#c62828")
                    return busesMarkerLoading
                } else if (tripNotRunning == true) {
                    return busesTripNotRun
                } else {
                    busesMarkerInfo.collectionSoFar.text = String(totalCollection)
                    busesMarkerInfo.busTime.text = tripStartTime
                    busesMarkerInfo.currentOccupancy.text = String(currentPassengerCount)
                    busesMarkerInfo.occupancyLevelImage.image = UIImage(named: occupancyLevel )
                    busesMarkerInfo.luggageCount.text = String(luggageTicketCount)
                    busesMarkerInfo.busFromTo.text = routeName
                    if(!ticketStatus.isEmpty && ticketStatus != "") {
                        busesMarkerInfo.ticketUpdatingText.text = ticketStatus
                    } else {
                        busesMarkerInfo.ticketUpdatingText.isHidden = true
                        busesMarkerInfo.ticketStatusImage.isHidden = true
                    }
                    
                    return busesMarkerInfo
                }
                    
            } else if(imageUrl.contains("bus_alert_info")) {
                if userData.iconUrl?.contains("alert_bus_bunching") ?? false {
                    let alertMarkerInfo = AlertSingleLineInfoWindow.instanceFromNib()
                    alertMarkerInfo.alertTitle.text = marker.title
                    alertMarkerInfo.alertSnippet.text = marker.snippet ?? "Loading..."
                    return alertMarkerInfo
                }
                let alertMarkerInfo = AlertMarkerInfoWindow.instanceFromNib()
                alertMarkerInfo.alertTitle.text = marker.title
                alertMarkerInfo.alertSnippet.text = marker.snippet ?? "Loading..."
                return alertMarkerInfo
            } else if(imageUrl.contains("last_updated_info")) {
                let lastUpdateInfo = LastUpdatedInfoWindow.instanceFromNib()
                lastUpdateInfo.infoTitle.text = marker.title
                lastUpdateInfo.infoSnippet.text = marker.snippet
                return lastUpdateInfo
            } else if(imageUrl.contains("not_show_info_window")) {
                return nil
                
            } else if(imageUrl.contains("multiple_info_window")) {
                return nil
            
            } else {
                let infoWindow = InfoWindowWithImage.instanceFromNib()
                infoWindow.titleLabel.text = marker.title
                infoWindow.snippetLabel.text = marker.snippet
                infoWindow.infoIcon.image = UIImage(named: imageUrl)
                return infoWindow
            }
            
            return nil
        }

    // onClusterInfoWindowClick, onInfoWindowClick
    public func mapView(_ mapView: GMSMapView, didTapInfoWindowOf marker: GMSMarker) {
        if let cluster = marker.userData as? GMUCluster {
            var items: [[String: Any?]] = []

            for item in cluster.items {
                items.append([
                    "markerId": String(item.hash.hashValue),
                    "latitude": item.position.latitude,
                    "longitude": item.position.longitude,
                    "title": item.title ?? "",
                    "snippet": item.snippet ?? ""
                ])
            }

            self.notifyListeners("onClusterInfoWindowClick", data: [
                "mapId": self.findMapIdByMapView(mapView),
                "latitude": cluster.position.latitude,
                "longitude": cluster.position.longitude,
                "size": cluster.count,
                "items": items
            ])
        } else {
            self.notifyListeners("onInfoWindowClick", data: [
                "mapId": self.findMapIdByMapView(mapView),
                "markerId": String(marker.hash.hashValue),
                "latitude": marker.position.latitude,
                "longitude": marker.position.longitude,
                "title": marker.title ?? "",
                "snippet": marker.snippet ?? ""
            ])
        }
    }

    // onMyLocationButtonClick
    public func didTapMyLocationButtonForMapView(for mapView: GMSMapView) -> Bool {
        self.notifyListeners("onMyLocationButtonClick", data: [
            "mapId": self.findMapIdByMapView(mapView)
        ])
        return false
    }

    // onMyLocationClick
    public func mapView(_ mapView: GMSMapView, didTapMyLocation location: CLLocationCoordinate2D) {
        self.notifyListeners("onMyLocationButtonClick", data: [
            "mapId": self.findMapIdByMapView(mapView),
            "latitude": location.latitude,
            "longitude": location.longitude
        ])
    }
    public func mapView(_ mapView: GMSMapView, didChange position: GMSCameraPosition) {
        for (mapId, map) in self.maps {
            if map.mapViewController.GMapView === mapView {
                map.onCameraMove()
                if(map.mapViewController.isCircleShow == true){
                    let height = map.mapViewController.circleView.frame.height
                    let xCenter = map.mapViewController.circleView.frame.origin.x
                    let yCenter = map.mapViewController.circleView.frame.origin.y
                    let region = mapView.projection.visibleRegion()
                    let distance=region.nearLeft.distance(to: region.farLeft)
                    let x = mapView.projection.point(for: region.farLeft)
                    let y = mapView.projection.point(for: region.nearLeft)
                    let center=mapView.projection.point(for: position.target)
                    let left=y.y-x.y
                    let radius=(left*2)*((500)/distance)
                    let height1 = radius * 2 - height
                    
                    map.mapViewController.circleView.frame = CGRect(x: center.x-radius, y: center.y-radius, width: map.mapViewController.circleView.frame.width + height1, height: map.mapViewController.circleView.frame.height + height1)
                    map.mapViewController.circleView.layoutIfNeeded()
                    map.mapViewController.circleView.layer.cornerRadius = radius
                }
            }
        }
        self.notifyListeners("onCameraMoveStarted", data: [
            "mapId": self.findMapIdByMapView(mapView),
            "latitude": position.target.latitude,
            "longitude": position.target.longitude,
        ])
     }
}

extension UIColor {
    public convenience init?(hex: String) {
        let r, g, b, a: CGFloat

        if hex.hasPrefix("#") {
            let start = hex.index(hex.startIndex, offsetBy: 1)
            let hexColor = String(hex[start...])

            let scanner = Scanner(string: hexColor)
            var hexNumber: UInt64 = 0
            if hexColor.count == 8 {
                if scanner.scanHexInt64(&hexNumber) {
                    r = CGFloat((hexNumber & 0xff000000) >> 24) / 255
                    g = CGFloat((hexNumber & 0x00ff0000) >> 16) / 255
                    b = CGFloat((hexNumber & 0x0000ff00) >> 8) / 255
                    a = CGFloat(hexNumber & 0x000000ff) / 255

                    self.init(red: r, green: g, blue: b, alpha: a)
                    return
                }
            } else {
                if scanner.scanHexInt64(&hexNumber) {
                    r = CGFloat((hexNumber & 0xff0000) >> 16) / 255
                    g = CGFloat((hexNumber & 0x00ff00) >> 8) / 255
                    b = CGFloat((hexNumber & 0x0000ff) >> 0) / 255

                    self.init(red: r, green: g, blue: b, alpha: 1)
                    return
                }
            }
        }

        return nil
    }
}
