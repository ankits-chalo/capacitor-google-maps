//
//  AlertMarkerInfoWindow.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class AlertMarkerInfoWindow: UIView {
    
    @IBOutlet weak var alertTitle: UILabel!
    @IBOutlet weak var alertSnippet: UILabel!
    @IBOutlet weak var alertCardView: UIView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to alertCardView
        alertCardView.layer.cornerRadius = 4
        alertCardView.layer.shadowColor = UIColor.black.cgColor
        alertCardView.layer.shadowOpacity = 0.5
        alertCardView.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        alertCardView.layer.shadowRadius = 2.0
        alertCardView.layer.masksToBounds = false
        alertCardView.backgroundColor = UIColor.white

    }

    class func instanceFromNib() -> AlertMarkerInfoWindow {
        return UINib(nibName: "AlertMarkerInfoWindow", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! AlertMarkerInfoWindow
    }
}
