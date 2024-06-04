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
    }
    

    class func instanceFromNib() -> AlertBusMarkerInactive {
        return UINib(nibName: "AlertBusMarkerInactive", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! AlertBusMarkerInactive
    }
}
