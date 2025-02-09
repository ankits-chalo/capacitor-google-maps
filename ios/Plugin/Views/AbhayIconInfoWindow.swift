//
//  AlertMarkerInfoWindow.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class AbhayIconInfoWindow: UIView {
    
    @IBOutlet weak var Views: UIView!
    @IBOutlet weak var name: UILabel!
    
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to alertCardView
        Views.layer.cornerRadius = 4
        Views.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        Views.layer.shadowOpacity = 1
        Views.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        Views.layer.shadowRadius = 1.86
        Views.layer.masksToBounds = false
        Views.backgroundColor = UIColor.white

    }

    class func instanceFromNib() -> AbhayIconInfoWindow {
        return UINib(nibName: "AbhayIconInfoWindow", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! AbhayIconInfoWindow
    }
}
