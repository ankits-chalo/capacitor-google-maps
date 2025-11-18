//
//  LastUpdatedInfoWindowReversed.swift
//  App
//
//  Created by Saksham.
//

import UIKit

class LastUpdatedInfoWindowReversed: UIView {

    @IBOutlet weak var lastUpdatedCardView: UIView!
    @IBOutlet weak var infoTitle: UILabel!
    @IBOutlet weak var infoSnippet: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to lastUpdatedCardView
        lastUpdatedCardView.layer.cornerRadius = 4
        lastUpdatedCardView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        lastUpdatedCardView.layer.shadowOpacity = 1
        lastUpdatedCardView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        lastUpdatedCardView.layer.shadowRadius = 1.86
        lastUpdatedCardView.layer.masksToBounds = false
        lastUpdatedCardView.backgroundColor = UIColor.white
        
    }

    class func instanceFromNib() -> LastUpdatedInfoWindowReversed {
        return UINib(nibName: "LastUpdatedInfoWindowReversed", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! LastUpdatedInfoWindowReversed
    }
}
