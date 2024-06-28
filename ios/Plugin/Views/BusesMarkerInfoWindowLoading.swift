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
    @IBOutlet weak var loadingText: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to infoCardView
        busLoadingView.layer.cornerRadius = 4
        busLoadingView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        busLoadingView.layer.shadowOpacity = 1
        busLoadingView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        busLoadingView.layer.shadowRadius = 1.86
        busLoadingView.layer.masksToBounds = false
        busLoadingView.backgroundColor = UIColor.white
//        titleLabel.textColor = UIColor.black
//        busesInfoCardView.textColor = UIColor.black

    }

    class func instanceFromNib() -> BusesMarkerInfoWindowLoading {
        return UINib(nibName: "BusesMarkerInfoWindowLoading", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! BusesMarkerInfoWindowLoading
    }
}
