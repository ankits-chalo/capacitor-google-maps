class RouteNameMarker: UIView {
    @IBOutlet weak var containerView: UIView!
    
    @IBOutlet weak var routeLabel: UILabel!
    
    @IBOutlet weak var arrowTail: UIImageView!
    override func awakeFromNib() {
        super.awakeFromNib()
        setupView()
    }
    
    private func setupView() {
        // Configure label for dynamic sizing
        routeLabel.numberOfLines = 1
//        routeLabel.lineBreakMode = .tailTruncation
        routeLabel.setContentHuggingPriority(.required, for: .horizontal)
        routeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        
        // Configure container
        containerView.backgroundColor = .systemBackground
        containerView.layer.cornerRadius = 8
    }
    
    func configure(with routeName: String) {
        routeLabel.text = routeName
        routeLabel.sizeToFit()
        // Force layout update
        setNeedsLayout()
        layoutIfNeeded()
        let paddingX: CGFloat = 12
            let paddingY: CGFloat = 8
        routeLabel.frame = CGRect(
               x: paddingX,
               y: paddingY,
               width: routeLabel.frame.width,
               height: routeLabel.frame.height
           )
           
           // ✅ Set container size based on label
           containerView.frame = CGRect(
               x: 0,
               y: 1,
               width: routeLabel.frame.width + (paddingX * 2),
               height: routeLabel.frame.height + (paddingY * 2) + 50
           )
        
        containerView.addSubview(routeLabel)
        containerView.addSubview(arrowTail)
    }
       
//    override var intrinsicContentSize: CGSize {
//        let labelSize = routeLabel.intrinsicContentSize
//        let padding: CGFloat = 16 // 8 points on each side
//        let arrowWidth: CGFloat = 24
//        let totalHeight: CGFloat = 52 // As per your current design
//
//        return CGSize(
//            width: labelSize.width + arrowWidth + padding,
//            height: totalHeight
//        )
//    }

    class func instanceFromNib() -> RouteNameMarker {
        return UINib(nibName: "RouteNameMarker", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! RouteNameMarker
    }
}
