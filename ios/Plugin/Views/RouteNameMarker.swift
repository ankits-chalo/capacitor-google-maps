final class RouteNameMarker: UIView {

    
    
    @IBOutlet weak var contentView: UIView!
    @IBOutlet weak var labelView: UIView!
    @IBOutlet weak var routeLabel: UILabel!
    @IBOutlet weak var arrowTail: UIImageView!
    
    override func awakeFromNib() {
            super.awakeFromNib()
            setupView()
        }

        private func setupView() {
            backgroundColor = .clear
            contentView.backgroundColor = .clear

            routeLabel.numberOfLines = 1
            routeLabel.lineBreakMode = .byTruncatingTail
            routeLabel.textAlignment = .center
            routeLabel.font = .systemFont(ofSize: 15, weight: .semibold)
            routeLabel.textColor = .label
            routeLabel.backgroundColor = .clear
            routeLabel.setContentHuggingPriority(.required, for: .horizontal)
            routeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

            labelView.backgroundColor = .systemBackground
            labelView.layer.cornerRadius = 4
            labelView.layer.masksToBounds = true

            arrowTail.contentMode = .scaleAspectFit
            arrowTail.clipsToBounds = true
            arrowTail.backgroundColor = .clear
            
        }

        func configure(with routeName: String) {
            routeLabel.text = routeName

            let paddingX: CGFloat = 12
            let paddingY: CGFloat = 8
            let tailHeight: CGFloat = 10
            let tailWidth: CGFloat = 20
            let minLabelHeight: CGFloat = 20

            routeLabel.sizeToFit()

            let labelWidth = ceil(routeLabel.frame.width)
            let labelHeight = max(ceil(routeLabel.frame.height), minLabelHeight)

            // label inside bubble
            routeLabel.frame = CGRect(
                x: paddingX,
                y: paddingY,
                width: labelWidth,
                height: labelHeight
            )

            let bubbleWidth = labelWidth + (paddingX * 2)
            let bubbleHeight = labelHeight + (paddingY * 2)

            // rounded bubble
            labelView.frame = CGRect(
                x: 0,
                y: 0,
                width: bubbleWidth,
                height: bubbleHeight
            )

            // tail below bubble
            arrowTail.frame = CGRect(
                x: (bubbleWidth - tailWidth) / 2,
                y: labelView.frame.maxY,
                width: tailWidth,
                height: tailHeight
            )

            // full marker content
            contentView.frame = CGRect(
                x: 0,
                y: 0,
                width: bubbleWidth,
                height: bubbleHeight + tailHeight
            )

            // if subviews are not already placed correctly in xib, ensure hierarchy
            if routeLabel.superview != labelView {
                routeLabel.removeFromSuperview()
                labelView.addSubview(routeLabel)
            }

            if labelView.superview != contentView {
                labelView.removeFromSuperview()
                contentView.addSubview(labelView)
            }

            if arrowTail.superview != contentView {
                arrowTail.removeFromSuperview()
                contentView.addSubview(arrowTail)
            }

            frame = contentView.frame
            setNeedsLayout()
            layoutIfNeeded()
        }

        override var intrinsicContentSize: CGSize {
            let paddingX: CGFloat = 12
            let paddingY: CGFloat = 8
            let tailHeight: CGFloat = 10
            let minLabelHeight: CGFloat = 20

            let labelSize = routeLabel.intrinsicContentSize
            let width = ceil(labelSize.width) + (paddingX * 2)
            let height = max(ceil(labelSize.height), minLabelHeight) + (paddingY * 2) + tailHeight

            return CGSize(width: width, height: height)
        }

        class func instanceFromNib() -> RouteNameMarker {
            return UINib(nibName: "RouteNameMarker", bundle: nil)
                .instantiate(withOwner: nil, options: nil)[0] as! RouteNameMarker
        }
}
