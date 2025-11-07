//
//  StopArrivalInfoWindow.swift
//  Pods
//
//  Created by Abhay Agrahary on 06/11/25.
//


import UIKit

class StopArrivalInfoWindow: UIView {
    
    @IBOutlet weak var busTitle: UILabel!
    @IBOutlet weak var arrivalTime: UILabel!
    @IBOutlet weak var departureTime: UILabel!
    @IBOutlet weak var StopArrivalCardView: UIView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to StopArrivalCardView
        StopArrivalCardView.layer.cornerRadius = 4
        StopArrivalCardView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        StopArrivalCardView.layer.shadowOpacity = 1
        StopArrivalCardView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        StopArrivalCardView.layer.shadowRadius = 1.86
        StopArrivalCardView.layer.masksToBounds = false
        StopArrivalCardView
    }

    class func instanceFromNib() -> StopArrivalInfoWindow {
        return UINib(nibName: "StopArrivalInfoWindow", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! StopArrivalInfoWindow
    }
}
