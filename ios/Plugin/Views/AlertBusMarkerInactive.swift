//
//  AlertBusMarkerHalt.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class AlertBusMarkerInactive: UIView {
    @IBOutlet weak var AlertInactiveCard: UIView!
    @IBOutlet weak var AlertSnippet: UILabel!
    @IBOutlet weak var BusNumberText: UILabel!
    override func awakeFromNib() {
        super.awakeFromNib()
        AlertInactiveCard.layer.cornerRadius = 15
        AlertInactiveCard.layer.masksToBounds = false
        BusMarkerCard.layer.shadowColor = UIColor.black.cgColor
        BusMarkerCard.layer.shadowOpacity = 0.5
        BusMarkerCard.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        BusMarkerCard.layer.shadowRadius = 2.0
    }
    

    class func instanceFromNib() -> AlertBusMarkerInactive {
        return UINib(nibName: "AlertBusMarkerInactive", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! AlertBusMarkerInactive
    }
}
