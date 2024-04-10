import Foundation
import Capacitor

public struct GoogleMapConfig: Codable {
    let width: Double
    let height: Double
    let x: Double
    let y: Double
    let center: LatLng
    let zoom: Double
    let styles: String?
    var mapId: String?
    let mapStyle: String
    let isCircleShow : Bool?

    init(fromJSObject: JSObject) throws {
        guard let width = fromJSObject["width"] as? Double else {
            throw GoogleMapErrors.invalidArguments("GoogleMapConfig object is missing the required 'width' property")
        }

        guard let height = fromJSObject["height"] as? Double else {
            throw GoogleMapErrors.invalidArguments("GoogleMapConfig object is missing the required 'height' property")
        }

        guard let x = fromJSObject["x"] as? Double else {
            throw GoogleMapErrors.invalidArguments("GoogleMapConfig object is missing the required 'x' property")
        }

        guard let y = fromJSObject["y"] as? Double else {
            throw GoogleMapErrors.invalidArguments("GoogleMapConfig object is missing the required 'y' property")
        }

        guard let zoom = fromJSObject["zoom"] as? Double else {
            throw GoogleMapErrors.invalidArguments("GoogleMapConfig object is missing the required 'zoom' property")
        }
        
         let isCircleShow = fromJSObject["isCircleShow"] as? Bool

        guard let latLngObj = fromJSObject["center"] as? JSObject else {
            throw GoogleMapErrors.invalidArguments("GoogleMapConfig object is missing the required 'center' property")
        }

        guard let lat = latLngObj["lat"] as? Double, let lng = latLngObj["lng"] as? Double else {
            throw GoogleMapErrors.invalidArguments("LatLng object is missing the required 'lat' and/or 'lng' property")
        }

         guard let mapStyle = fromJSObject["mapStyle"] as? String else {
            throw GoogleMapErrors.invalidArguments("mapStyle is missing")
        }

        self.width = round(width)
        self.height = round(height)
        self.x = x
        self.y = y
        self.mapStyle = mapStyle
        self.zoom = zoom
        self.center = LatLng(lat: lat, lng: lng)
        self.isCircleShow = isCircleShow
        if let stylesArray = fromJSObject["styles"] as? JSArray, let jsonData = try? JSONSerialization.data(withJSONObject: stylesArray, options: []) {
            self.styles = String(data: jsonData, encoding: .utf8)
        } else {
            self.styles = nil
        }

        self.mapId = fromJSObject["iOSMapId"] as? String
    }
}
