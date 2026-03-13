//
//  ViewController.swift
//  Plugin
//
//  Created by macbook on 13/03/26.
//  Copyright © 2026 Max Lynch. All rights reserved.
//

import UIKit

class RouteNameInfoWindow: UIView {

    @IBOutlet weak var routeNameCardView: UIView!
    
    @IBOutlet weak var infoTitle: UILabel!
    

    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to lastUpdatedCardView
        routeNameCardView.layer.cornerRadius = 4
        routeNameCardView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        routeNameCardView.layer.shadowOpacity = 1
        routeNameCardView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        routeNameCardView.layer.shadowRadius = 1.86
        routeNameCardView.layer.masksToBounds = false
        routeNameCardView.backgroundColor = UIColor.white
        
    }
    
    class func instanceFromNib() -> RouteNameInfoWindow {
        return UINib(nibName: "RouteNameInfoWindow", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! RouteNameInfoWindow
    }
    
}
