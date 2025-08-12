//
//  HistoryReplayInfoWindow.swift
//  App
//
//  Created by Saksham.
//

import UIKit

class HistoryReplayInfoWindow: UIView {

    
    @IBOutlet weak var historyReplayCardView: UIView!
    @IBOutlet weak var longTitle: UILabel!
    @IBOutlet weak var speedTitle: UILabel!
    @IBOutlet weak var timeTitle: UILabel!
    @IBOutlet weak var latTitle: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        // Applying card-like background to lastUpdatedCardView
        historyReplayCardView.layer.cornerRadius = 4
        historyReplayCardView.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.08).cgColor
        historyReplayCardView.layer.shadowOpacity = 1
        historyReplayCardView.layer.shadowOffset = CGSize(width: 0, height: 0.62)
        historyReplayCardView.layer.shadowRadius = 1.86
        historyReplayCardView.layer.masksToBounds = false
        historyReplayCardView.backgroundColor = UIColor.white
        
    }

    class func instanceFromNib() -> HistoryReplayInfoWindow {
        return UINib(nibName: "HistoryReplayInfoWindow", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! HistoryReplayInfoWindow
    }
}
