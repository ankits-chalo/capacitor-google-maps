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
        AlertInactiveCard.layer.cornerRadius = 17
        AlertInactiveCard.layer.masksToBounds = false
        AlertInactiveCard.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        AlertInactiveCard.layer.shadowOpacity = 1
        AlertInactiveCard.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        AlertInactiveCard.layer.shadowRadius = 1.86
    }
    

    class func instanceFromNib() -> AlertBusMarkerInactive {
        return UINib(nibName: "AlertBusMarkerInactive", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! AlertBusMarkerInactive
    }
}
