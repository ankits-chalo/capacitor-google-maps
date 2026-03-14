//
//  RouteNameMarker.swift
//  Plugin
//
//  Created by macbook on 14/03/26.
//  Copyright © 2026 Max Lynch. All rights reserved.
//

import UIKit

class RouteNameMarker: UIView {

    @IBOutlet weak var routeNameCardView: UIView!
    @IBOutlet weak var routeNameLabel: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to alertCardView
        routeNameCardView.layer.cornerRadius = 4
        routeNameCardView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        routeNameCardView.layer.shadowOpacity = 1
        routeNameCardView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        routeNameCardView.layer.shadowRadius = 1.86
        routeNameCardView.layer.masksToBounds = false
        routeNameCardView.backgroundColor = UIColor.white

        routeNameLabel.numberOfLines = 1 // Ensure single-line text
        routeNameLabel.lineBreakMode = .byClipping // Prevent wrapping or truncation
        routeNameLabel.setContentCompressionResistancePriority(.defaultLow, for: .horizontal) // Allow expansion
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        // Ensure the view adjusts its size dynamically
        self.layoutIfNeeded()
    }

    class func instanceFromNib() -> RouteNameMarker {
        return UINib(nibName: "RouteNameMarker", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! RouteNameMarker
    }

}
