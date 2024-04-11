//
//  InfoWindowWithImage.swift
//  App
//
//  Created by Ankit Saini on 25/06/23.
//

import UIKit

class InfoWindowWithImage: UIView {

    @IBOutlet weak var busInfoTime: UILabel!
    @IBOutlet weak var infoIcon: UIImageView!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var snippetLabel: UILabel!
    @IBOutlet weak var infoCardView: UIView!
    @IBOutlet weak var arrowImageView: UIImageView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to infoCardView
        infoCardView.layer.cornerRadius = 4
        infoCardView.layer.shadowColor = UIColor.black.cgColor
        infoCardView.layer.shadowOpacity = 0.5
        infoCardView.layer.shadowOffset = CGSize(width: 0.0, height: 1.0)
        infoCardView.layer.shadowRadius = 2.0
        infoCardView.layer.masksToBounds = false
        infoCardView.backgroundColor = UIColor.white
        titleLabel.textColor = UIColor.black
        snippetLabel.textColor = UIColor.black

        // Setting arrow image to arrowImageView
        arrowImageView.image = UIImage(named: "ArrowPolygon")
    }

    class func instanceFromNib() -> InfoWindowWithImage {
        return UINib(nibName: "InfoWindowWithImage", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! InfoWindowWithImage
    }
}
