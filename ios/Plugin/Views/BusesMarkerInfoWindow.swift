//
//  BusesMarkerInfoWindow.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class BusesMarkerInfoWindow: UIView {

    
    @IBOutlet weak var busesInfoCardView: UIView!
    @IBOutlet weak var busCardName: UILabel!
    @IBOutlet weak var busTime: UILabel!
    @IBOutlet weak var busFromTo: UILabel!
    @IBOutlet weak var collectionSoFar: UILabel!
    @IBOutlet weak var occupancyLevelImage: UIImageView!
    @IBOutlet weak var currentOccupancy: UILabel!
    @IBOutlet weak var ticketUpdatingText: UILabel!
    @IBOutlet weak var ticketStatusImage: UIImageView!
    @IBOutlet weak var collectionText: UILabel!
    @IBOutlet weak var occupancyText: UILabel!
    @IBOutlet weak var viewDetailsText: UILabel!
    
    
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

    class func instanceFromNib() -> BusesMarkerInfoWindow {
        return UINib(nibName: "BusesMarkerInfoWindow", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! BusesMarkerInfoWindow
    }
}
