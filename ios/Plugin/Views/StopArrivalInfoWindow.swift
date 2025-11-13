//
//  StopArrivalInfoWindow.swift
//  Plugin
//
//  Created by Abhay Agrahary on 08/11/25.
//  Copyright © 2025 Max Lynch. All rights reserved.
//

import UIKit

class StopArrivalInfoWindow: UIView {
    
    @IBOutlet weak var arrivalView: UIView!
    @IBOutlet weak var busTitle: UILabel!
    @IBOutlet weak var arrivalTime: UILabel!
    @IBOutlet weak var departureTime: UILabel!
    
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to arrivalView
        arrivalView.layer.cornerRadius = 4
        arrivalView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        arrivalView.layer.shadowOpacity = 1
        arrivalView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        arrivalView.layer.shadowRadius = 1.86
        arrivalView.layer.masksToBounds = false
        arrivalView.backgroundColor = UIColor.white

    }

    class func instanceFromNib() -> StopArrivalInfoWindow {
        return UINib(nibName: "StopArrivalInfoWindow", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! StopArrivalInfoWindow
    }
}
