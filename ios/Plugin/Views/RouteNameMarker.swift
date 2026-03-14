class RouteNameMarker: UIView {
    @IBOutlet weak var containerView: UIView!
    
    @IBOutlet weak var routeLabel: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        setupView()
    }
    
    private func setupView() {
        // Configure label for dynamic sizing
        routeLabel.numberOfLines = 1
        routeLabel.lineBreakMode = .tailTruncation
        routeLabel.setContentHuggingPriority(.required, for: .horizontal)
        routeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        
        // Configure container
        containerView.backgroundColor = .systemBackground
        containerView.layer.cornerRadius = 8
    }
    
    func configure(with routeName: String) {
        routeLabel.text = "Route : \(routeName)"
        // Force layout update
        setNeedsLayout()
        layoutIfNeeded()
    }
       
    override var intrinsicContentSize: CGSize {
        let labelSize = routeLabel.intrinsicContentSize
        let padding: CGFloat = 16 // 8 points on each side
        let arrowWidth: CGFloat = 24
        let totalHeight: CGFloat = 52 // As per your current design
        
        return CGSize(
            width: labelSize.width + arrowWidth + padding,
            height: totalHeight
        )
    }

    class func instanceFromNib() -> RouteNameMarker {
        return UINib(nibName: "RouteNameMarker", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! RouteNameMarker
    }
}
