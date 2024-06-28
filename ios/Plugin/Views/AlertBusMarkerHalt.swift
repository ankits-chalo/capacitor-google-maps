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
        AlertHaltCard.layer.cornerRadius = 17
        AlertHaltCard.layer.masksToBounds = false
        AlertHaltCard.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        AlertHaltCard.layer.shadowOpacity = 1
        AlertHaltCard.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        AlertHaltCard.layer.shadowRadius = 1.86
    }
    

    class func instanceFromNib() -> AlertBusMarkerHalt {
        return UINib(nibName: "AlertBusMarkerHalt", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! AlertBusMarkerHalt
    }
}
