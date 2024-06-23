//
//  BusesMarkers.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class BusesMarkers: UIView {

    
    @IBOutlet weak var BusMarkerCard: UIView!
    @IBOutlet weak var BusNumberMarkerText: UILabel!
    @IBOutlet weak var BusMarkerImage: UIImageView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to infoCardView
        BusMarkerCard.layer.cornerRadius = 15
        BusMarkerCard.layer.shadowColor = UIColor.black.cgColor
        BusMarkerCard.layer.shadowOpacity = 0.5
        BusMarkerCard.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        BusMarkerCard.layer.shadowRadius = 2.0
        BusMarkerCard.layer.masksToBounds = false

    }
    
    func updateCardColorBasedOnIconUrl(iconUrl: String?) {
        if let iconUrl = iconUrl {
            
            // Check if the iconUrl string contains "selected" and apply a thick white border
            if iconUrl.contains("selected") {
                if iconUrl.contains("grey") {
                    BusMarkerCard.backgroundColor = UIColor(hexString: "#808080")
                    BusMarkerImage.image = UIImage(named: "not_live_status")
                } else{
                    BusMarkerCard.backgroundColor = UIColor(hexString: "#2196f3")
                    BusMarkerImage.image = UIImage(named: "live_status")
                }
                BusMarkerCard.layer.borderWidth = 3.0 // Set the border width
                BusMarkerCard.layer.borderColor = UIColor.white.cgColor // Set the border color
                BusMarkerCard.layer.masksToBounds = true // Ensure the border is not clipped
                BusMarkerCard.layer.shadowColor = UIColor.black.cgColor
                BusMarkerCard.layer.shadowOpacity = 0.5
                BusMarkerCard.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
                BusMarkerCard.layer.shadowRadius = 2.0
            }
            else {
                if iconUrl.contains("grey") {
                    BusMarkerCard.backgroundColor = UIColor(hexString: "#808080")
                    BusMarkerImage.image = UIImage(named: "not_live_status")
                } else{
                    BusMarkerCard.backgroundColor = UIColor(hexString: "#2196f3")
                    BusMarkerImage.image = UIImage(named: "live_status")
                }
                // Remove the border or set it to default
                BusMarkerCard.layer.borderWidth = 0
                BusMarkerCard.layer.borderColor = UIColor.clear.cgColor // Clear the border color
            }
        }
       }

    class func instanceFromNib() -> BusesMarkers {
        return UINib(nibName: "BusesMarkers", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! BusesMarkers
    }
}
