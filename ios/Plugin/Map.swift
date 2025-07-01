import Foundation
import GoogleMaps
import Capacitor
import GoogleMapsUtils

public struct LatLng: Codable {
    let lat: Double
    let lng: Double
}

class GMViewController: UIViewController {
    var mapViewBounds: [String: Double]!
    var GMapView: GMSMapView!
    var cameraPosition: [String: Double]!
    var minimumClusterSize: Int?
    var mapId: String?
    var isCircleShow: Bool = false
    var circleView: UIView!
    var circle: GMSCircle!

    private var clusterManager: GMUClusterManager?

    var clusteringEnabled: Bool {
        return clusterManager != nil
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        let camera = GMSCameraPosition.camera(withLatitude: cameraPosition["latitude"] ?? 0, longitude: cameraPosition["longitude"] ?? 0, zoom: Float(cameraPosition["zoom"] ?? 12))
        let frame = CGRect(x: mapViewBounds["x"] ?? 0, y: mapViewBounds["y"] ?? 0, width: mapViewBounds["width"] ?? 0, height: mapViewBounds["height"] ?? 0)
        if let id = mapId {
            let gmsId = GMSMapID(identifier: id)
            self.GMapView = GMSMapView(frame: frame, mapID: gmsId, camera: camera)
        } else {
            self.GMapView = GMSMapView(frame: frame, camera: camera)
        }

        self.view = GMapView
    }

    func initClusterManager() {
        var customColor = UIColor(hexString: "#FE7C00")
        let iconGenerator = GMUDefaultClusterIconGenerator()
        if let customColor = UIColor(hexString: "#FE7C00") {
            iconGenerator = GMUDefaultClusterIconGenerator(buckets: [99999], backgroundColors: [customColor])
        }
        let algorithm = GMUNonHierarchicalDistanceBasedAlgorithm()
        let renderer = GMUDefaultClusterRenderer(mapView: self.GMapView, clusterIconGenerator: iconGenerator)

        self.clusterManager = GMUClusterManager(map: self.GMapView, algorithm: algorithm, renderer: renderer)
    }

    func destroyClusterManager() {
        self.clusterManager = nil
    }
    
    func clusterMarker() {
        if let clusterManager = clusterManager {
            clusterManager.cluster()
        }
    }
    
    func updateMarkerPosition(marker: GMSMarker, newPosition: CLLocationCoordinate2D) {
            guard let clusterManager = clusterManager else { return }
                
                // Check if the marker is visible (not clustered)
            if marker.map != nil &&
                (marker.position.latitude != newPosition.latitude ||
                marker.position.longitude != newPosition.longitude) {
                    // The marker is visible on the map
                    clusterManager.remove(marker)
                    
                    // Update the marker's position
                    marker.position = newPosition
                    
                    clusterManager.add(marker)
                    
                    // Since the marker's position has changed, it may need to be re-clustered
                    clusterManager.cluster()
                } else {
                    print("Not inside the map", marker.title)
                    // The marker is not visible (it's inside a cluster)
                    // Do not update the position
                }

            }

    func addMarkersToCluster(markers: [GMSMarker]) {
        if let clusterManager = clusterManager {
            clusterManager.add(markers)
            clusterManager.cluster()
        }
    }

    func removeMarkersFromCluster(markers: [GMSMarker]) {
        if let clusterManager = clusterManager {
            markers.forEach { marker in
                clusterManager.remove(marker)
            }
            clusterManager.cluster()
        }
    }
}

 class removeArrayClass {
     let id: String
     var keyValue: Int
     init(id: String, keyValue: Int) {
         self.id = id
         self.keyValue = keyValue
     }
 }

public class Map {
    var id: String
    var config: GoogleMapConfig
    var mapViewController: GMViewController
    var targetViewController: UIView?
    var markers = [Int: GMSMarker]()
    var polygons = [Int: GMSPolygon]()
    var circles = [Int: GMSCircle]()
    var polylines = [Int: GMSPolyline]()
    var markerIcons = [String: UIImage]()
    var removeMarkersArray = [removeArrayClass]()
    var polylineCords = [LatLng]()
    var timer : Timer?
    // swiftlint:disable identifier_name
    public static let MAP_TAG = 99999
    // swiftlint:enable identifier_name

    // swiftlint:disable weak_delegate
    private var delegate: CapacitorGoogleMapsPlugin
    var markerIdOnWeb = [String : Int]()

    init(id: String, config: GoogleMapConfig, delegate: CapacitorGoogleMapsPlugin) {
        self.id = id
        self.config = config
        self.delegate = delegate
        self.mapViewController = GMViewController()
        self.mapViewController.mapId = config.mapId
        self.timer = Timer.scheduledTimer(timeInterval: 1, target: self, selector: #selector(self.render), userInfo: nil, repeats: true)
        self.render()
    }

    @objc func render() {
        DispatchQueue.main.async {
            self.mapViewController.mapViewBounds = [
                "width": self.config.width,
                "height": self.config.height,
                "x": self.config.x,
                "y": self.config.y
            ]

            self.mapViewController.cameraPosition = [
                "latitude": self.config.center.lat,
                "longitude": self.config.center.lng,
                "zoom": self.config.zoom
            ]

            self.targetViewController = self.getTargetContainer(refWidth: self.config.width, refHeight: self.config.height)
            self.mapViewController.isCircleShow = self.config.isCircleShow ?? false
            
            if let bridge = self.delegate.bridge {

                for item in bridge.webView!.getAllSubViews() {
                    if let typeClass = NSClassFromString("WKChildScrollView"), item.isKind(of: typeClass) {
                        (item as? UIScrollView)?.isScrollEnabled = true
                        if item.bounds.width == self.config.width && item.bounds.height == self.config.height && (item as? UIView)?.tag == 0 {
                            self.timer?.invalidate()
                            self.targetViewController = item
                            break
                        }
                    }
                }

                if let target = self.targetViewController {
                    target.tag = 1
                    target.removeAllSubview()
                    self.mapViewController.view.frame = target.bounds
                    target.addSubview(self.mapViewController.view)
                    self.mapViewController.GMapView.delegate = self.delegate
                    
                    if let styles = self.config.styles {
                        do {
                            self.mapViewController.GMapView.mapStyle = try GMSMapStyle(jsonString: styles)
                        } catch {
                            CAPLog.print("Invalid Google Maps styles")
                        }           
                    }

                    self.delegate.notifyListeners("onMapReady", data: [
                        "mapId": self.id
                    ])
                }
            }
        }
    }

    func updateRender(frame: CGRect, mapBounds: CGRect) {
        DispatchQueue.main.async {
            self.mapViewController.view.layer.mask = nil

            var updatedFrame = self.mapViewController.view.frame
            updatedFrame.origin.x = mapBounds.origin.x
            updatedFrame.origin.y = mapBounds.origin.y

            self.mapViewController.view.frame = updatedFrame

            var maskBounds: [CGRect] = []

            if !frame.contains(mapBounds) {
                maskBounds.append(contentsOf: self.getFrameOverflowBounds(frame: frame, mapBounds: mapBounds))
            }

            if maskBounds.count > 0 {
                let maskLayer = CAShapeLayer()
                let path = CGMutablePath()

                path.addRect(self.mapViewController.view.bounds)
                maskBounds.forEach { b in
                    path.addRect(b)
                }

                maskLayer.path = path
                maskLayer.fillRule = .evenOdd

                self.mapViewController.view.layer.mask = maskLayer

            }

            self.mapViewController.view.layoutIfNeeded()
        }

    }

    func rebindTargetContainer(mapBounds: CGRect) {
        DispatchQueue.main.sync {
            if let target = self.getTargetContainer(refWidth: round(Double(mapBounds.width)), refHeight: round(Double(mapBounds.height))) {
                self.targetViewController = target
                target.tag = Map.MAP_TAG
                target.removeAllSubview()
                CATransaction.begin()
                CATransaction.setDisableActions(true)
                self.mapViewController.view.frame.size.width = mapBounds.width
                self.mapViewController.view.frame.size.height = mapBounds.height
                CATransaction.commit()
                target.addSubview(self.mapViewController.view)
            }
        }
    }

    private func getTargetContainer(refWidth: Double, refHeight: Double) -> UIView? {
        if let bridge = self.delegate.bridge {
            for item in bridge.webView!.getAllSubViews() {
                let isScrollView = item.isKind(of: NSClassFromString("WKChildScrollView")!) || item.isKind(of: NSClassFromString("WKScrollView")!)
                let isBridgeScrollView = item.isEqual(bridge.webView?.scrollView)

                if isScrollView && !isBridgeScrollView {
                    (item as? UIScrollView)?.isScrollEnabled = true

                    let height = Double((item as? UIScrollView)?.contentSize.height ?? 0)
                    let width = Double((item as? UIScrollView)?.contentSize.width ?? 0)
                    let actualHeight = round(height / 2)

                    let isWidthEqual = width == self.config.width
                    let isHeightEqual = actualHeight == self.config.height

                    if isWidthEqual && isHeightEqual && item.tag < self.targetViewController?.tag ?? Map.MAP_TAG {
                        return item
                    }
                }
            }
        }

        return nil
    }

    func destroy() {
        DispatchQueue.main.async {
            self.targetViewController?.tag = 0
            self.mapViewController.view = nil

        }
    }

    func enableTouch() {
        DispatchQueue.main.async {
            if let target = self.targetViewController, let itemIndex = WKWebView.disabledTargets.firstIndex(of: target) {
                WKWebView.disabledTargets.remove(at: itemIndex)
            }
        }
    }

    func disableTouch() {
        DispatchQueue.main.async {
            if let target = self.targetViewController, !WKWebView.disabledTargets.contains(target) {
                WKWebView.disabledTargets.append(target)
            }
        }
    }

    func addMarker(marker: Marker) throws -> Int {
        var markerHash = 0

        DispatchQueue.main.sync {
            if(self.markerIdOnWeb[marker.id!] != nil ){
                markerHash = self.markerIdOnWeb[marker.id!]!
            }
            else {
                var newMarker: GMSMarker

                newMarker = GMSMarker();
                newMarker.position = CLLocationCoordinate2D(latitude: marker.coordinate.lat, longitude: marker.coordinate.lng)
                newMarker.groundAnchor = CGPoint(x: 0.5, y: 0.5)
                if !(marker.title ?? "").isEmpty {
                    newMarker.title = marker.title
                }
                if !(marker.snippet ?? "").isEmpty {
                    newMarker.snippet = marker.snippet
                }
                if let infoIcon = marker.infoIcon, let mapView = self.mapViewController.GMapView {
                    newMarker.userData = marker
                    newMarker.map = mapView
                    
//                    To show the info window
//                    mapView.selectedMarker = newMarker
                }
                
                newMarker.isFlat = marker.isFlat ?? false
                newMarker.opacity = marker.opacity ?? 1
                newMarker.isDraggable = marker.draggable ?? false
                if (marker.iconUrl ?? "").isEmpty {
                    //                Hiding the default marker whose iconUrl is not present
                    newMarker.opacity = 0.0
                    //                Show the infowindow which is present
                    newMarker.map = self.mapViewController.GMapView
                    self.mapViewController.GMapView.selectedMarker = newMarker
                } else {
                    if ((marker.iconUrl?.contains("buses_custom_marker")) != nil) {
                        let busesMarker = BusesMarker.instanceFromNib()
                        busesMarker.BusNumberMarkerText.text = marker.title
                        busesMarker.updateCardColorBasedOnIconUrl(iconUrl: marker.iconUrl)
                        newMarker.iconView = busesMarker
                    } else {
                        // If it is present in assets folder then the icon is picked from it
                        newMarker.icon = UIImage(named: marker.iconUrl ?? "")
                    }
                }
                do {
                    if((marker.rotation) == 1){
                        newMarker.rotation =  try getAngle(marker: marker)
                    }else{
                        newMarker.rotation =  0
                    }
                } catch {
                    NSLog("Error in angle. \(error)")
                }
                if (marker.zIndex != nil) {
                    newMarker.zIndex = marker.zIndex ?? 1
                }
                
                if self.mapViewController.clusteringEnabled {
                    self.mapViewController.addMarkersToCluster(markers: [newMarker])
                } else {
                    newMarker.map = self.mapViewController.GMapView
                }
                
                self.markers[newMarker.hash.hashValue] = newMarker
                self.markerIdOnWeb[marker.id!] = newMarker.hash.hashValue
                markerHash = newMarker.hash.hashValue
            }
        }
        return markerHash
        
    }
    
    func setMarkerPosition(marker: Marker) throws  -> String  {
        if let oldMarker = self.markers[Int(marker.id!)!] {
            DispatchQueue.main.sync {
                if !self.mapViewController.clusteringEnabled {
                    CATransaction.begin()
                    CATransaction.setAnimationDuration(2.0)
                    oldMarker.position = CLLocationCoordinate2D(latitude: marker.coordinate.lat, longitude: marker.coordinate.lng)
                    oldMarker.title = marker.title
                    
                    let newCamera = GMSCameraPosition(latitude: marker.coordinate.lat, longitude: marker.coordinate.lng, zoom: 15)
                    
                    //                CATransaction.beg`in()
                    //                CATransaction.setAnimationDuration(5.0)
                    //                self.mapViewController.GMapView.animate(to: newCamera)
                    //                CATransaction.commit()
                    //                if marker.title?.range(of:"showInfoWindowTrue") != nil {
                    //                    let customMarker = CustomMarker(title: marker.title!,coordinate: marker.coordinate)
                    //                    oldMarker.iconView = customMarker.iconView
                    //                }
                    //               else if marker.title?.range(of:"busesAroundYou") != nil {
                    //                   let customMarker = BusesAroundYou(title: marker.title!,coordinate: marker.coordinate)
                    //                   oldMarker.iconView = customMarker.iconView
                    //               }
                    //                else{
                    //                    oldMarker.title = marker.title
                    //                }
                    if((marker.iconUrl ) != nil){
                        if ((marker.iconUrl?.contains("buses_custom_marker")) != nil) {
                            if let busesMarker = oldMarker.iconView as? BusesMarker {
                                busesMarker.BusNumberMarkerText.text = marker.title
                                busesMarker.updateCardColorBasedOnIconUrl(iconUrl: marker.iconUrl)
                            } else if let iconUrl = marker.iconUrl, iconUrl.contains("buses_custom_marker") {
                                // Create new BusesMarker if necessary
                                let busesMarker = BusesMarker.instanceFromNib()
                                busesMarker.BusNumberMarkerText.text = marker.title
                                busesMarker.updateCardColorBasedOnIconUrl(iconUrl: iconUrl)
                                oldMarker.iconView = busesMarker
                            }
                        } else {
                            // If it is present in assets folder then the icon is picked from it
                            oldMarker.icon =  UIImage(named: marker.iconUrl ?? "")
                        }
                        
                    } else {
                        //                Hiding the default marker whose iconUrl is not present by setting the opacity
                        oldMarker.opacity = 0.0
                        //                Show the infowindow which is present
                        oldMarker.map = self.mapViewController.GMapView
                        self.mapViewController.GMapView.selectedMarker = oldMarker
                    }
                    
                    if let infoIcon = marker.infoIcon, let infoData = marker.infoData, let mapView = self.mapViewController.GMapView{
                        oldMarker.userData = marker
                        oldMarker.map = mapView
                        let showInfoIcon = infoData["showInfoIcon"] as? Bool ?? false
                        //                    To show the info window
                        if(showInfoIcon) {
                            mapView.selectedMarker = nil
                            mapView.selectedMarker = oldMarker
                        }
                    }
                    
                    do {
                        // Set the map style by passing the URL of the local file.
                        if((marker.rotation) == 1){
                            oldMarker.rotation =  try self.getAngle(marker: marker)
                        }else{
                            oldMarker.rotation =  0
                        }
                    } catch {
                        NSLog("Error in angle. \(error)")
                    }
                    CATransaction.commit()
                } else {  
                    oldMarker.userData = marker
                    
                    self.mapViewController.updateMarkerPosition(marker: oldMarker, newPosition: CLLocationCoordinate2D(latitude: marker.coordinate.lat, longitude: marker.coordinate.lng))
                    
                    if((marker.iconUrl ) != nil){
                        if ((marker.iconUrl?.contains("buses_custom_marker")) != nil) {
                            if let busesMarker = oldMarker.iconView as? BusesMarker {
                                busesMarker.BusNumberMarkerText.text = marker.title
                                busesMarker.updateCardColorBasedOnIconUrl(iconUrl: marker.iconUrl)
                            } else if let iconUrl = marker.iconUrl, iconUrl.contains("buses_custom_marker") {
                                // Create new BusesMarker if necessary
                                let busesMarker = BusesMarker.instanceFromNib()
                                busesMarker.BusNumberMarkerText.text = marker.title
                                busesMarker.updateCardColorBasedOnIconUrl(iconUrl: iconUrl)
                                oldMarker.iconView = busesMarker
                            }
                        } else {
                            // If it is present in assets folder then the icon is picked from it
                            oldMarker.icon =  UIImage(named: marker.iconUrl ?? "")
                        }
                        
                    }
                    
                    // If the marker is the selected marker, refresh the info window
                    let showInfoIcon = marker.infoData?["showInfoIcon"] as? Bool ?? false && oldMarker.map != nil
                    if showInfoIcon {
//                            self.mapViewController.GMapView.selectedMarker = nil
                        oldMarker.map = self.mapViewController.GMapView
                        self.mapViewController.GMapView.selectedMarker = oldMarker
                    } else {
                        self.mapViewController.GMapView.selectedMarker = nil
                    }
                    
                }
            }
        } else {
            throw GoogleMapErrors.markerNotFound
        }
        return marker.id!
    }
    
    func fitBound(cords: [LatLng], padding: CGFloat) {
        DispatchQueue.main.sync {
            // Initialize the GMSCoordinateBounds
            var bounds = GMSCoordinateBounds()

            // Loop through the list of coordinates
            for cord in cords {
                let coordinate = CLLocationCoordinate2D(latitude: cord.lat, longitude: cord.lng)
                bounds = bounds.includingCoordinate(coordinate)
            }
            let cameraUpdate = GMSCameraUpdate.fit(bounds, withPadding: padding)
            if let mapView = self.mapViewController.GMapView {
                mapView.animate(with: cameraUpdate)
            }
        }
    }
    
     func setMarkerPositionNew(marker: Marker) throws  -> String  {
             let element = self.removeMarkersArray.first(where:{$0.id == marker.id})
            
             if let oldMarker = self.markers[element?.keyValue ?? 0]  {
                 DispatchQueue.main.sync {
                     CATransaction.begin()
                     CATransaction.setAnimationDuration(2.0)
                     oldMarker.position = CLLocationCoordinate2D(latitude: marker.coordinate.lat, longitude: marker.coordinate.lng)
                     oldMarker.title = marker.title
                     if marker.title?.range(of:"showInfoWindowTrue") != nil {
                         let customMarker = CustomMarker(title: marker.title!,coordinate: marker.coordinate)
                     oldMarker.iconView = customMarker.iconView
                 }
     //               else if marker.title?.range(of:"busesAroundYou") != nil {
     //                   let customMarker = BusesAroundYou(title: marker.title!,coordinate: marker.coordinate)
     //                   oldMarker.iconView = customMarker.iconView
     //               }
                 else{
                     oldMarker.title = marker.title
                 }
                 if((marker.iconUrl ) != nil){
                     oldMarker.icon =  UIImage(named: marker.iconUrl ?? "")
                 }
                 do {
                     // Set the map style by passing the URL of the local file.
                     if((marker.rotation) == 1){
                             oldMarker.rotation =  try self.getAngle(marker: marker)
                     }else{
                         oldMarker.rotation =  0
                     }
                     } catch {
                     NSLog("Error in angle. \(error)")
                 }
                 CATransaction.commit()
             }
         } else {
             throw GoogleMapErrors.markerNotFound
         }
         return marker.id!
     }
    
    func updateInfoWindow(marker: Marker) throws  -> String  {
        if let oldMarker = self.markers[Int(marker.id!)!] {
            if(oldMarker.snippet == self.mapViewController.GMapView.selectedMarker?.snippet){
                DispatchQueue.main.sync {
                    oldMarker.title = marker.title
                    self.mapViewController.GMapView.selectedMarker = oldMarker
                }
            }else{
                DispatchQueue.main.sync {
                    oldMarker.title = marker.title
                }
            }
        } else {
            throw GoogleMapErrors.markerNotFound
        }
        return marker.id!
    }
    
    func drawCircle(circleProps: JSObject,id:String) throws  -> String  {
        let center = circleProps["center"] as? JSObject
        let _lat = center?["lat"] as? Double
        let _lng = center?["lng"] as? Double
        let circleCenter = CLLocationCoordinate2D(latitude: _lat!, longitude: _lng!)
        let circle = GMSCircle(position: circleCenter, radius:circleProps["radius"] as! CLLocationDistance)
        circle.fillColor = UIColor(red: 0, green: 0.35, blue: 0, alpha: 0.1)
        circle.strokeColor = .green
        circle.strokeWidth = 1
        circle.map = self.mapViewController.GMapView
        self.mapViewController.circle = circle;
//        self.mapViewController.isCircleShow=circleProps["isCircleShow"] as? Bool ?? false
        return id
    }
    
    func getAngle(marker:Marker) throws -> Double {
        var angle : Double = 0
        var minDistance : Double = Double.greatestFiniteMagnitude
        var index : Int = 0
        var nearestPointIndex : Int = 0
        let markerLat = marker.coordinate.lat
        let markerLng = marker.coordinate.lng
        self.polylineCords.forEach { cord in
          let _markerLocation1 = CLLocation(latitude: markerLat, longitude: markerLng)
            let _polyLinePointLocation = CLLocation(latitude: cord.lat, longitude: cord.lng)
            let distance = _markerLocation1.distance(from: _polyLinePointLocation)
            index+=1
            if (distance < minDistance) {
                   minDistance = distance;
                   nearestPointIndex = index;
                 }
        }
        nearestPointIndex = nearestPointIndex < self.polylineCords.count - 1 ? nearestPointIndex + 1 : self.polylineCords.count - 1;
        if (nearestPointIndex >= 0 && nearestPointIndex <= self.polylineCords.count - 1) {
            let _markerLocation2 = CLLocationCoordinate2D(latitude: markerLat, longitude: markerLng)
            let _polyLinePointLocation2 = CLLocationCoordinate2D(latitude: self.polylineCords[nearestPointIndex].lat, longitude: self.polylineCords[nearestPointIndex].lng)
            angle = GMSGeometryHeading(_markerLocation2, _polyLinePointLocation2)
        }
        return angle
    }

    func addMarkers(markers: [Marker]) throws -> [Int] {
         var markerHashes: [Int] = []
        var flag = true
        var markersNew = [removeArrayClass]()
       try self.removeMarkersArray.forEach { marker in
             if((markers.first{$0.id == marker.id}) != nil){
                 let element = markers.first{$0.id == marker.id}
                 var data = try setMarkerPositionNew(marker: element!)
                 markersNew.insert(marker, at: 0)
                 self.removeMarkersArray.removeAll{$0.id == marker.id}
             }
        }
        
        if(flag){
               if(!self.removeMarkersArray.isEmpty){
                  flag = false
                  try removeMarkers(ids: self.removeMarkersArray.map{$0.keyValue})
                  self.removeMarkersArray = []
               }
          }
        
        if(!markersNew.isEmpty){
            markersNew.forEach{marker in
                self.removeMarkersArray.insert(marker, at: 0)
            }
        }
        
         DispatchQueue.main.sync {
             var googleMapsMarkers: [GMSMarker] = []
             markers.forEach { marker in
                 if(marker.title != "" || (marker.skipTitle == true)){
                 if((markersNew.first{$0.id == marker.id}) == nil ){
                 var newMarker = GMSMarker()
                      if marker.secondaryImageUrl == nil {
                          newMarker = BusesAroundYou(title: marker.title!,coordinate: marker.coordinate)
                     }
                     else{
                         newMarker = GMSMarker();
                         newMarker.position = CLLocationCoordinate2D(latitude: marker.coordinate.lat, longitude: marker.coordinate.lng)
                         if(marker.skipTitle != true){
                             newMarker.title = marker.title
                         }
                         newMarker.groundAnchor = CGPoint(x: 0.5, y: 0.5)
                     }
                 newMarker.snippet = marker.snippet
                 newMarker.isFlat = marker.isFlat ?? false
                 newMarker.opacity = marker.opacity ?? 1
                 newMarker.isDraggable = marker.draggable ?? false
                 newMarker.icon = UIImage(named: marker.iconUrl ?? "")

                 if self.mapViewController.clusteringEnabled {
                     googleMapsMarkers.append(newMarker)
                 } else {
                     newMarker.map = self.mapViewController.GMapView
                 }

                 self.markers[newMarker.hash.hashValue] = newMarker
                 markerHashes.append(newMarker.hash.hashValue)
                     self.removeMarkersArray.append(removeArrayClass(id: marker.id!, keyValue: newMarker.hash.hashValue))
             }
                 }
             }

             if self.mapViewController.clusteringEnabled {
                 self.mapViewController.addMarkersToCluster(markers: googleMapsMarkers)
             }
         }
        return markerHashes
    }

    func addPolygons(polygons: [Polygon]) throws -> [Int] {
        var polygonHashes: [Int] = []

        DispatchQueue.main.sync {
            polygons.forEach { polygon in
                let newPolygon = self.buildPolygon(polygon: polygon)
                newPolygon.map = self.mapViewController.GMapView

                self.polygons[newPolygon.hash.hashValue] = newPolygon

                polygonHashes.append(newPolygon.hash.hashValue)
            }
        }

        return polygonHashes
    }

    func addCircles(circles: [Circle]) throws -> [Int] {
        var circleHashes: [Int] = []

        DispatchQueue.main.sync {
            circles.forEach { circle in
                let newCircle = self.buildCircle(circle: circle)
                newCircle.map = self.mapViewController.GMapView

                self.circles[newCircle.hash.hashValue] = newCircle

                circleHashes.append(newCircle.hash.hashValue)
            }
        }

        return circleHashes
    }  

    // func addPolylines(lines: [Polyline]) throws -> [Int] {
    //     var polylineHashes: [Int] = []

    //     DispatchQueue.main.sync {
    //         lines.forEach { line in
    //             let newLine = self.buildPolyline(line: line)
    //             newLine.map = self.mapViewController.GMapView

    //             self.polylines[newLine.hash.hashValue] = newLine

    //             polylineHashes.append(newLine.hash.hashValue)
    //         }
    //     }

    //     return polylineHashes
    // }

    func addPolyline(cords: [LatLng], strokeWidth: Double, strokeColor: String, strokeOpacity:Double ) throws -> [Int] {
        var polylineHashes: [Int] = []

        DispatchQueue.main.sync {
            var googleMapsMarkers: [GMSPolyline] = []

            let path = GMSMutablePath()
            self.polylineCords = cords
            cords.forEach { cord in
                path.addLatitude(cord.lat, longitude: cord.lng)
            }
                let newPolyline = GMSPolyline(path: path)
                newPolyline.geodesic = true
                newPolyline.map = self.mapViewController.GMapView
                newPolyline.strokeWidth = strokeWidth
                newPolyline.strokeColor = .black

                polylineHashes.append(newPolyline.hash.hashValue)
            let startCord = CLLocation(latitude: cords[0].lat, longitude: cords[0].lng)
            var farestLocation: CLLocation?
            var longestDistance: CLLocationDistance?

            cords.forEach { cord in
                let location = CLLocation(latitude: cord.lat, longitude: cord.lng)
                let distance = startCord.distance(from: location)
                if longestDistance == nil || distance > longestDistance! {
                  farestLocation = location
                  longestDistance = distance
              }
            }
            let bounds = GMSCoordinateBounds(coordinate:CLLocationCoordinate2D(latitude: cords[0].lat, longitude: cords[0].lng),coordinate:CLLocationCoordinate2D(latitude: farestLocation?.coordinate.latitude ?? cords[cords.count - 1].lat, longitude: farestLocation?.coordinate.longitude ?? cords[cords.count - 1].lng))
            let update = GMSCameraUpdate.fit(bounds)
//            let newCamera = self.mapViewController.GMapView.camera(for: bounds, insets: UIEdgeInsets())!
//            let newCamera = GMSCameraPosition(latitude: lat, longitude: lng, zoom: nil, bearing: bearing, viewingAngle: angle)

            CATransaction.begin()
            CATransaction.setAnimationDuration(1.0)
            self.mapViewController.GMapView.animate(with: update)
            CATransaction.commit()

            }
        return polylineHashes
    }

    func addPolylines(polylines:[[LatLng]],strokeColors: [String], strokeWidths: [Double], zIndexs: [Double], strokeOpacities:[Double] ) throws -> [Int] {
        var polylineHashes: [Int] = []
       
        DispatchQueue.main.sync {
            let startCord = CLLocation(latitude: polylines[0][0].lat, longitude: polylines[0][0].lng)
            
            polylines.enumerated().forEach { index, polyline in
               let path = GMSMutablePath()
               self.polylineCords = polyline
                polyline.forEach { cord in
                   path.addLatitude(cord.lat, longitude: cord.lng)
                   
               }
                   let newPolyline = GMSPolyline(path: path)
                   newPolyline.geodesic = true
                   newPolyline.map = self.mapViewController.GMapView
                   newPolyline.strokeWidth = strokeWidths[index]
                   newPolyline.zIndex = Int32(zIndexs[index])
                   if let strokeColor = UIColor(hexString: strokeColors[index]) {
                        newPolyline.strokeColor = strokeColor
                   }
                   newPolyline.zIndex = Int32(zIndexs[index])
                   polylineHashes.append(newPolyline.hash.hashValue)
            }
            
//            let lastArray = polylines[polylines.count - 1]
//            let lastElement = lastArray[lastArray.count - 1]
//            let bounds = GMSCoordinateBounds(coordinate:CLLocationCoordinate2D(latitude: polylines[0][0].lat, longitude: polylines[0][0].lng),coordinate:CLLocationCoordinate2D(latitude: farestLocation?.coordinate.latitude ?? lastElement.lat, longitude: farestLocation?.coordinate.longitude ?? lastElement.lng))
//            let update = GMSCameraUpdate.fit(bounds)
//
//            CATransaction.begin()
//            CATransaction.setAnimationDuration(1.0)
//            self.mapViewController.GMapView.animate(with: update)
//            CATransaction.commit()
        }
        return polylineHashes
    }

    func enableClustering() {
        if !self.mapViewController.clusteringEnabled {
            DispatchQueue.main.sync {
                self.mapViewController.initClusterManager()

                // add existing markers to the cluster
                if !self.markers.isEmpty {
                    var existingMarkers: [GMSMarker] = []
                    for (_, marker) in self.markers {
                        marker.map = nil
                        existingMarkers.append(marker)
                    }

                    self.mapViewController.addMarkersToCluster(markers: existingMarkers)
                }
            }
        }
    }

    func disableClustering() {
        DispatchQueue.main.sync {
            self.mapViewController.destroyClusterManager()

            // add existing markers back to the map
            if !self.markers.isEmpty {
                for (_, marker) in self.markers {
                    marker.map = self.mapViewController.GMapView
                }
            }
        }
    }

    func removeMarker(id: Int) throws {
        if (id != 0) {
            if let marker = self.markers[id] {
                DispatchQueue.main.async {
                    if self.mapViewController.clusteringEnabled {
                        self.mapViewController.removeMarkersFromCluster(markers: [marker])
                    }

                marker.map = nil
                self.markers.removeValue(forKey: id)
            }
        } else {
            throw GoogleMapErrors.markerNotFound
        }
        }
    }

    func removePolygons(ids: [Int]) throws {
        DispatchQueue.main.sync {
            ids.forEach { id in
                if let polygon = self.polygons[id] {
                    polygon.map = nil
                    self.polygons.removeValue(forKey: id)
                }
            }
        }
    }

    func removeCircles(ids: [Int]) throws {
        DispatchQueue.main.sync {
            ids.forEach { id in
                if let circle = self.circles[id] {
                    circle.map = nil
                    self.circles.removeValue(forKey: id)
                }
            }
        }
    }

    func removePolylines(ids: [Int]) throws {
        DispatchQueue.main.sync {
            ids.forEach { id in
                if let line = self.polylines[id] {
                    line.map = nil
                    self.polylines.removeValue(forKey: id)
                }
            }
        }
    }

    func setCamera(config: GoogleMapCameraConfig) throws {
        let currentCamera = self.mapViewController.GMapView.camera

        let lat = config.coordinate?.lat ?? currentCamera.target.latitude
        let lng = config.coordinate?.lng ?? currentCamera.target.longitude

        let zoom = config.zoom ?? currentCamera.zoom
        let bearing = config.bearing ?? Double(currentCamera.bearing)
        let angle = config.angle ?? currentCamera.viewingAngle

        let animate = config.animate ?? false

        DispatchQueue.main.sync {
            let newCamera = GMSCameraPosition(latitude: lat, longitude: lng, zoom: zoom, bearing: bearing, viewingAngle: angle)

            if animate {
                self.mapViewController.GMapView.animate(to: newCamera)
            } else {
                self.mapViewController.GMapView.camera = newCamera
            }
        }

    }

    func getMapType() -> GMSMapViewType {
        return self.mapViewController.GMapView.mapType
    }

    func setMapType(mapType: GMSMapViewType) throws {
        DispatchQueue.main.sync {
            self.mapViewController.GMapView.mapType = mapType
        }
    }

    func enableIndoorMaps(enabled: Bool) throws {
        DispatchQueue.main.sync {
            self.mapViewController.GMapView.isIndoorEnabled = enabled
        }
    }

    func enableTrafficLayer(enabled: Bool) throws {
        DispatchQueue.main.sync {
            self.mapViewController.GMapView.isTrafficEnabled = enabled
        }
    }

    func enableAccessibilityElements(enabled: Bool) throws {
        DispatchQueue.main.sync {
            self.mapViewController.GMapView.accessibilityElementsHidden = enabled
        }
    }

    func enableCurrentLocation(enabled: Bool) throws {
        DispatchQueue.main.sync {
            self.mapViewController.GMapView.isMyLocationEnabled = enabled
        }
    }

// Uncomment when user loaction is required
    // func enableCurrentLocation(enabled: Bool) throws {
    //     DispatchQueue.main.sync {
    //         self.mapViewController.GMapView.isMyLocationEnabled = enabled
    //     self.mapViewController.GMapView.settings.myLocationButton = enabled
    //     self.mapViewController.GMapView.padding = UIEdgeInsets(top: 0, left: 0, bottom: 34, right: 0)

    //     }
    // }
    
    func toogleScrollGesture(enabled: Bool) throws {
        DispatchQueue.main.sync {
            if(self.mapViewController.GMapView != nil){
                self.mapViewController.GMapView.settings.scrollGestures = enabled
                self.mapViewController.GMapView.settings.zoomGestures = enabled
                self.mapViewController.GMapView.settings.rotateGestures = enabled
                let view = self.mapViewController.GMapView.viewWithTag(5)
                if(enabled == false){
                    if((view) == nil){
                    let btnFloor = UIView()
                    btnFloor.frame = CGRect(x: 0, y: 0, width: self.mapViewController.GMapView.frame.width, height: self.mapViewController.GMapView.frame.height)
                    btnFloor.contentMode = UIView.ContentMode.scaleToFill
                    btnFloor.tag = 5
                    self.mapViewController.view.addSubview(btnFloor)
                    }
                }else{
                    if((view) != nil){
                        view?.isHidden = true
                        self.mapViewController.GMapView.bringSubviewToFront(view!)
                        view?.removeFromSuperview()
                    }
                }

            }
        }
    }

    func setPadding(padding: GoogleMapPadding) throws {
        DispatchQueue.main.sync {
            let mapInsets = UIEdgeInsets(top: CGFloat(padding.top), left: CGFloat(padding.left), bottom: CGFloat(padding.bottom), right: CGFloat(padding.right))
            self.mapViewController.GMapView.padding = mapInsets
        }
    }

    func removeMarkers(ids: [Int]) throws {
        DispatchQueue.main.sync {
            var markers: [GMSMarker] = []
            ids.forEach { id in
                if let marker = self.markers[id] {
                    marker.map = nil
                    self.markers.removeValue(forKey: id)
                    markers.append(marker)
                }
            }

            if self.mapViewController.clusteringEnabled {
                self.mapViewController.removeMarkersFromCluster(markers: markers)
            }
        }
    }

    func getMapLatLngBounds() -> GMSCoordinateBounds? {
        return GMSCoordinateBounds(region: self.mapViewController.GMapView.projection.visibleRegion())
    }

    func fitBounds(bounds: GMSCoordinateBounds, padding: CGFloat) {
        DispatchQueue.main.sync {
            let cameraUpdate = GMSCameraUpdate.fit(bounds, withPadding: padding)
            self.mapViewController.GMapView.animate(with: cameraUpdate)
        }
    }

    private func getFrameOverflowBounds(frame: CGRect, mapBounds: CGRect) -> [CGRect] {
        var intersections: [CGRect] = []

        // get top overflow
        if mapBounds.origin.y < frame.origin.y {
            let height = frame.origin.y - mapBounds.origin.y
            let width = mapBounds.width
            intersections.append(CGRect(x: 0, y: 0, width: width, height: height))
        }

        // get bottom overflow
        if (mapBounds.origin.y + mapBounds.height) > (frame.origin.y + frame.height) {
            let height = (mapBounds.origin.y + mapBounds.height) - (frame.origin.y + frame.height)
            let width = mapBounds.width
            intersections.append(CGRect(x: 0, y: mapBounds.height, width: width, height: height))
        }

        return intersections
    }

    private func buildCircle(circle: Circle) -> GMSCircle {
        let newCircle = GMSCircle()
        newCircle.title = circle.title
        newCircle.strokeColor = circle.strokeColor
        newCircle.strokeWidth = circle.strokeWidth
        newCircle.fillColor = circle.fillColor
        newCircle.position = CLLocationCoordinate2D(latitude: circle.center.lat, longitude: circle.center.lng)
        newCircle.radius = CLLocationDistance(circle.radius)
        newCircle.isTappable = circle.tappable ?? false
        newCircle.zIndex = circle.zIndex
        newCircle.userData = circle.tag

        return newCircle
    }

    private func buildPolygon(polygon: Polygon) -> GMSPolygon {
        let newPolygon = GMSPolygon()
        newPolygon.title = polygon.title
        newPolygon.strokeColor = polygon.strokeColor
        newPolygon.strokeWidth = polygon.strokeWidth
        newPolygon.fillColor = polygon.fillColor
        newPolygon.isTappable = polygon.tappable ?? false
        newPolygon.geodesic = polygon.geodesic ?? false
        newPolygon.zIndex = polygon.zIndex
        newPolygon.userData = polygon.tag

        var shapeIndex = 0
        let outerShape = GMSMutablePath()
        var holes: [GMSMutablePath] = []

        polygon.shapes.forEach { shape in
            if shapeIndex == 0 {
                shape.forEach { coord in
                    outerShape.add(CLLocationCoordinate2D(latitude: coord.lat, longitude: coord.lng))
                }
            } else {
                let holeShape = GMSMutablePath()
                shape.forEach { coord in
                    holeShape.add(CLLocationCoordinate2D(latitude: coord.lat, longitude: coord.lng))
                }

                holes.append(holeShape)
            }

            shapeIndex += 1
        }

        newPolygon.path = outerShape
        newPolygon.holes = holes

        return newPolygon
    }

    private func buildPolyline(line: Polyline) -> GMSPolyline {
        let newPolyline = GMSPolyline()
        newPolyline.title = line.title
        newPolyline.strokeColor = line.strokeColor
        newPolyline.strokeWidth = line.strokeWidth
        newPolyline.isTappable = line.tappable ?? false
        newPolyline.geodesic = line.geodesic ?? false
        newPolyline.zIndex = line.zIndex
        newPolyline.userData = line.tag

        let path = GMSMutablePath()
        line.path.forEach { coord in
            path.add(CLLocationCoordinate2D(latitude: coord.lat, longitude: coord.lng))
        }

        newPolyline.path = path

        if line.styleSpans.count > 0 {
            var spans: [GMSStyleSpan] = []

            line.styleSpans.forEach { span in
                if let segments = span.segments {
                    spans.append(GMSStyleSpan(color: span.color, segments: segments))
                } else {
                    spans.append(GMSStyleSpan(color: span.color))
                }
            }

            newPolyline.spans = spans
        }

        return newPolyline
    }

    private func buildMarker(marker: Marker) -> GMSMarker {
        let newMarker = GMSMarker()
        newMarker.position = CLLocationCoordinate2D(latitude: marker.coordinate.lat, longitude: marker.coordinate.lng)
        newMarker.title = marker.title
        newMarker.snippet = marker.snippet
        newMarker.isFlat = marker.isFlat ?? false
        newMarker.opacity = marker.opacity ?? 1
        newMarker.isDraggable = marker.draggable ?? false
        newMarker.zIndex = marker.zIndex ?? 0
        if let iconAnchor = marker.iconAnchor {
            newMarker.groundAnchor = iconAnchor
        }

        // cache and reuse marker icon uiimages
        if let iconUrl = marker.iconUrl {
            if let iconImage = self.markerIcons[iconUrl] {
                newMarker.icon = getResizedIcon(iconImage, marker)
            } else {
                if iconUrl.starts(with: "https:") {
                    if let url = URL(string: iconUrl) {
                        URLSession.shared.dataTask(with: url) { (data, _, _) in
                            DispatchQueue.main.async {
                                if let data = data, let iconImage = UIImage(data: data) {
                                    self.markerIcons[iconUrl] = iconImage
                                    newMarker.icon = getResizedIcon(iconImage, marker)
                                }
                            }
                        }.resume()
                    }
                } else if let iconImage = UIImage(named: "public/\(iconUrl)") {
                    self.markerIcons[iconUrl] = iconImage
                    newMarker.icon = getResizedIcon(iconImage, marker)
                } else {
                    var detailedMessage = ""

                    if iconUrl.hasSuffix(".svg") {
                        detailedMessage = "SVG not supported."
                    }

                    print("CapacitorGoogleMaps Warning: could not load image '\(iconUrl)'. \(detailedMessage)  Using default marker icon.")
                }
            }
        } else {
            if let color = marker.color {
                newMarker.icon = GMSMarker.markerImage(with: color)
            }
        }

        return newMarker
    }
}

private func getResizedIcon(_ iconImage: UIImage, _ marker: Marker) -> UIImage? {
    if let iconSize = marker.iconSize {
        return iconImage.resizeImageTo(size: iconSize)
    } else {
        return iconImage
    }
}

extension WKWebView {
    static var disabledTargets: [UIView] = []

    override open func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        var hitView = super.hitTest(point, with: event)

        if let tempHitView = hitView, WKWebView.disabledTargets.contains(tempHitView) {
            return nil
        }

        if let typeClass = NSClassFromString("WKChildScrollView"), let tempHitView = hitView, tempHitView.isKind(of: typeClass) {
            for item in tempHitView.subviews.reversed() {
                let convertPoint = item.convert(point, from: self)
                if let hitTestView = item.hitTest(convertPoint, with: event) {
                    hitView = hitTestView
                    break
                }
            }
        }

        return hitView
    }
}

extension UIView {
    private static var allSubviews: [UIView] = []

    private func viewArray(root: UIView) -> [UIView] {
        var index = root.tag
        for view in root.subviews {
            if view.tag == Map.MAP_TAG {
                // view already in use as in map
                continue
            }

            // tag the index depth of the uiview
            view.tag = index

            if view.isKind(of: UIView.self) {
                UIView.allSubviews.append(view)
            }
            _ = viewArray(root: view)

            index += 1
        }
        return UIView.allSubviews
    }

    fileprivate func getAllSubViews() -> [UIView] {
        UIView.allSubviews = []
        return viewArray(root: self).reversed()
    }

    fileprivate func removeAllSubview() {
        subviews.forEach {
            $0.removeFromSuperview()
        }
    }
}

extension UIImage {
    func resizeImageTo(size: CGSize) -> UIImage? {
        UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
        self.draw(in: CGRect(origin: CGPoint.zero, size: size))
        let resizedImage = UIGraphicsGetImageFromCurrentImageContext()!
        UIGraphicsEndImageContext()
        return resizedImage
    }
}
