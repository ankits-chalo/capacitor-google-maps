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
    @IBOutlet weak var tripNotRunningText: UILabel!
    
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to infoCardView
        busesInfoCardView.layer.cornerRadius = 4
        busesInfoCardView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        busesInfoCardView.layer.shadowOpacity = 1
        busesInfoCardView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        busesInfoCardView.layer.shadowRadius = 1.86
        busesInfoCardView.layer.masksToBounds = false
        busesInfoCardView.backgroundColor = UIColor.white
//        titleLabel.textColor = UIColor.black
//        busesInfoCardView.textColor = UIColor.black

    }

    class func instanceFromNib() -> BusesTripNotRun {
        return UINib(nibName: "BusesTripNotRun", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! BusesTripNotRun
    }
}
