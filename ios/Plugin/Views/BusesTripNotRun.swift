//
//  BusesMarkerInfoWindow.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class BusesTripNotRun: UIView {
    @IBOutlet weak var busesInfoCardView: UIView!
    @IBOutlet weak var busCardName: UILabel!
    
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to infoCardView
        busesInfoCardView.layer.cornerRadius = 4
        busesInfoCardView.layer.shadowColor = UIColor.black.cgColor
        busesInfoCardView.layer.shadowOpacity = 0.5
        busesInfoCardView.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        busesInfoCardView.layer.shadowRadius = 2.0
        busesInfoCardView.layer.masksToBounds = false
        busesInfoCardView.backgroundColor = UIColor.white
//        titleLabel.textColor = UIColor.black
//        busesInfoCardView.textColor = UIColor.black

    }

    class func instanceFromNib() -> BusesTripNotRun {
        return UINib(nibName: "BusesTripNotRun", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! BusesTripNotRun
    }
}
