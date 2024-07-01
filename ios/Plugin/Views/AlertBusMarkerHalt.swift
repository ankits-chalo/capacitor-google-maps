//
//  AlertBusMarkerHalt.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class AlertBusMarkerHalt: UIView {
    
    @IBOutlet weak var BusNumberText: UILabel!
    @IBOutlet weak var AlertSnippet: UILabel!
    @IBOutlet weak var IgnitionImage: UIImageView!
    @IBOutlet weak var AlertHaltCard: UIView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        AlertHaltCard.layer.cornerRadius = 15
        AlertHaltCard.layer.shadowColor = UIColor.black.cgColor
        AlertHaltCard.layer.shadowOpacity = 0.5
        AlertHaltCard.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        AlertHaltCard.layer.shadowRadius = 2.0
        AlertHaltCard.layer.masksToBounds = false

    }
    

    class func instanceFromNib() -> AlertBusMarkerHalt {
        return UINib(nibName: "AlertBusMarkerHalt", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! AlertBusMarkerHalt
    }
}
