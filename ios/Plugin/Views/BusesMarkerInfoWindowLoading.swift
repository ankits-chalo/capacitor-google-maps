//
//  BusesMarkerInfoWindow.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class BusesMarkerInfoWindowLoading: UIView {
    @IBOutlet weak var busLoadingView: UIView!
    @IBOutlet weak var busCardName: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to infoCardView
        busLoadingView.layer.cornerRadius = 4
        busLoadingView.layer.shadowColor = UIColor.black.cgColor
        busLoadingView.layer.shadowOpacity = 0.5
        busLoadingView.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        busLoadingView.layer.shadowRadius = 2.0
        busLoadingView.layer.masksToBounds = false
        busLoadingView.backgroundColor = UIColor.white
//        titleLabel.textColor = UIColor.black
//        busesInfoCardView.textColor = UIColor.black

    }

    class func instanceFromNib() -> BusesMarkerInfoWindowLoading {
        return UINib(nibName: "BusesMarkerInfoWindowLoading", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! BusesMarkerInfoWindowLoading
    }
}
