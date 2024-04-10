//
//  BusesAroundYou.swift
//  App
//
//  Created by mac157 on 12/07/22.
//

import UIKit
import GoogleMaps

class BusesAroundYou: GMSMarker {
    init(title:String,coordinate:LatLng){
        super.init()
        position = CLLocationCoordinate2D(latitude: coordinate.lat, longitude: coordinate.lng)
           let view = Bundle.main.loadNibNamed("BusesAround", owner: self, options: nil)![0] as! BusesAround
        let label =  UILabel(frame: CGRect(x: 50, y: 0, width: 1000, height: 600))
            label.numberOfLines = 0
            label.text = title
        var maximumLabelSize: CGSize = CGSize(width: 280, height: 9999)
        var expectedLabelSize: CGSize = label.sizeThatFits(maximumLabelSize);
            var calculatedWidth=expectedLabelSize.width
            if(expectedLabelSize.width<20){
                calculatedWidth+=5
            }else if(expectedLabelSize.width>40){
                calculatedWidth-=5
            }
            let frame = CGRect(x: 0, y: 0, width:calculatedWidth+10 , height: 20)
        view.frame = frame
        view.BusInfo.frame = frame
        view.BusInfo.text = title
        let imageName = "busAroundIcon"
        let image = UIImage(named: imageName)
        let imageView = UIImageView(image: image!)
        imageView.frame = CGRect(x: 3, y: 5, width: 10, height: 10)
        view.addSubview(imageView)
        //Imageview on Top of View
        view.bringSubviewToFront(imageView)
        view.layer.masksToBounds =  true
        view.layer.cornerRadius = 3;
        view.BusInfo.textColor = UIColor.white
        view.layer.borderColor = UIColor.white.cgColor
        view.layer.borderWidth = 1
        view.layer.backgroundColor = UIColor(red: 30/255, green: 144/255, blue: 227/255, alpha: 1).cgColor
        iconView = view;
    }
}

