//
//  CustomMarker.swift
//  App
//
//  Created by mac157 on 21/06/22.
//

import UIKit
import GoogleMaps

class CustomMarker: GMSMarker {
    init(title:String,coordinate:LatLng){
        super.init()
        position = CLLocationCoordinate2D(latitude: coordinate.lat, longitude: coordinate.lng)
        let htmlData = NSString(string: title).data(using: String.Encoding.utf8.rawValue)
           let options = [NSAttributedString.DocumentReadingOptionKey.documentType: NSAttributedString.DocumentType.html]
            let attributedString = try! NSMutableAttributedString(data: htmlData!, options: options, documentAttributes:nil)
            let textRangeForFont : NSRange = NSMakeRange(0, attributedString.length)

            attributedString.addAttributes([NSAttributedString.Key.font : UIFont(name: "Arial", size:14)!], range: textRangeForFont)
           let view = Bundle.main.loadNibNamed("infoWindowNew", owner: self, options: nil)![0] as! infoWindowNew
        let viewBackground = Bundle.main.loadNibNamed("infoWindowBackground", owner: self, options: nil)![0] as! infoWindowBackground
        
        let label =  UILabel(frame: CGRect(x: 0, y: 0, width: 1000, height: 600))
            label.numberOfLines = 0
            label.attributedText = attributedString
            label.sizeToFit()
        let frame = CGRect(x: 10, y: 0, width: label.frame.width, height: label.frame.height)
        let arrowFrame = CGRect(x: (label.frame.width - 90)/2 , y: label.frame.height - 1, width: 20, height: 20)
        view.frame = frame
        view.view.frame = arrowFrame
        view.view.transform = CGAffineTransform(rotationAngle: .pi/4)
       view.info.attributedText = attributedString
        viewBackground.childView.addSubview(view)
        viewBackground.frame = CGRect(x: 0, y: 0, width: label.frame.width - 50, height: label.frame.height + 25)
        viewBackground.childView.layer.shadowOffset = CGSize(width: 3, height: 3);
        viewBackground.childView.layer.shadowColor = UIColor.black.cgColor;
        viewBackground.childView.layer.shadowOpacity = 0.7;
        viewBackground.childView.frame = CGRect(x: 0, y: 0, width: label.frame.width, height: label.frame.height + 13)
        iconView = viewBackground;
        groundAnchor = CGPoint(x: 0.5, y: 1.3)
    }
}
