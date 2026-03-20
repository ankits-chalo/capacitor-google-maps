//class RouteNameMarker: UIView {
//
//    
//    @IBOutlet weak var contentView: UIView!
//
//    @IBOutlet weak var labelView: UIView!
//    
//    @IBOutlet weak var routeLabel: UILabel!
//    @IBOutlet weak var arrowTail: UIImageView!
//    override func awakeFromNib() {
//        super.awakeFromNib()
//        setupView()
//    }
//    
//    private func setupView() {
//        // Configure label for dynamic sizing
//        routeLabel.numberOfLines = 1
////        routeLabel.lineBreakMode = .tailTruncation
//        routeLabel.setContentHuggingPriority(.required, for: .horizontal)
//        routeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
//        
//        // Configure container
//        containerView.backgroundColor = .systemBackground
//        containerView.layer.cornerRadius = 8
//    }
//    
//    func configure(with routeName: String) {
//        routeLabel.text = routeName
//        routeLabel.sizeToFit()
//        // Force layout update
//        setNeedsLayout()
//        layoutIfNeeded()
//        
//        let paddingX: CGFloat = 12
//        let paddingY: CGFloat = 8
//        let tailHeight: CGFloat = 10 // adjust as per your arrow
//        let tailWidth: CGFloat = 20
//
//        // 1. Label sizing
//        routeLabel.sizeToFit()
//
//        routeLabel.frame = CGRect(
//            x: paddingX,
//            y: paddingY,
//            width: routeLabel.frame.width,
//            height: routeLabel.frame.height
//        )
//
//        // 2. Tail view (arrow)
////        let tailView = UIImageView(image: UIImage(named: "marker_tail"))
//
//        arrowTail.frame = CGRect(
//            x: (routeLabel.frame.width + paddingX * 2 - tailWidth) / 2, // center horizontally
//            y: routeLabel.frame.maxY + paddingY, // place BELOW label
//            width: tailWidth,
//            height: tailHeight
//        )
//
//        // 3. Container size (label + tail)
//        containerView.frame = CGRect(
//            x: 0,
//            y: 0,
//            width: routeLabel.frame.width + (paddingX * 2),
//            height: routeLabel.frame.height + (paddingY * 2) + tailHeight
//        )
//
//        // 4. Add subviews
//        containerView.addSubview(routeLabel)
//        containerView.addSubview(arrowTail)
////        let paddingX: CGFloat = 12
////            let paddingY: CGFloat = 8
////        routeLabel.frame = CGRect(
////               x: paddingX,
////               y: paddingY,
////               width: routeLabel.frame.width,
////               height: routeLabel.frame.height
////           )
////           
////           // ✅ Set container size based on label
////           containerView.frame = CGRect(
////               x: 0,
////               y: 1,
////               width: routeLabel.frame.width + (paddingX * 2),
////               height: routeLabel.frame.height + (paddingY * 2) + 50
////           )
////        
////        containerView.addSubview(routeLabel)
////        containerView.addSubview(arrowTail)
//    }
//       
////    override var intrinsicContentSize: CGSize {
////        let labelSize = routeLabel.intrinsicContentSize
////        let padding: CGFloat = 16 // 8 points on each side
////        let arrowWidth: CGFloat = 24
////        let totalHeight: CGFloat = 52 // As per your current design
////
////        return CGSize(
////            width: labelSize.width + arrowWidth + padding,
////            height: totalHeight
////        )
////    }
//
//    class func instanceFromNib() -> RouteNameMarker {
//        return UINib(nibName: "RouteNameMarker", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! RouteNameMarker
//    }
//}


@import UIKit

final class RouteNameMarker: UIView {

    @IBOutlet weak var contentView: UIView!
    
    @IBOutlet weak var arrowTail: UIImageView!
    
    @IBOutlet weak var labelView: UIView!
    @IBOutlet weak var routeLabel: UILabel!
    
    
    private var didSetupUI = false

    // MARK: - Init

    override init(frame: CGRect) {
        super.init(frame: frame)
        loadFromNib()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        loadFromNib()
    }

    class func instanceFromNib() -> RouteNameMarker {
        return RouteNameMarker(frame: .zero)
    }

    // MARK: - Nib Load

    private func loadFromNib() {
        Bundle.main.loadNibNamed("RouteNameMarker", owner: self, options: nil)

        guard let contentView = contentView else {
            assertionFailure("contentView outlet is not connected in RouteNameMarker.xib")
            return
        }

        addSubview(contentView)
        contentView.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            contentView.topAnchor.constraint(equalTo: topAnchor),
            contentView.leadingAnchor.constraint(equalTo: leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        setupUIIfNeeded()
    }

    private func setupUIIfNeeded() {
        guard !didSetupUI else { return }
        didSetupUI = true

        setupViews()
        setupConstraints()
    }

    // MARK: - View Setup

    private func setupViews() {
        backgroundColor = .clear
        isOpaque = false

        contentView.backgroundColor = .clear
        contentView.translatesAutoresizingMaskIntoConstraints = false

        bubbleView.backgroundColor = .systemBackground
        bubbleView.layer.cornerRadius = 8
        bubbleView.layer.masksToBounds = true
        bubbleView.translatesAutoresizingMaskIntoConstraints = false

        routeLabel.backgroundColor = .clear
        routeLabel.textColor = .label
        routeLabel.font = .systemFont(ofSize: 15, weight: .semibold)
        routeLabel.textAlignment = .center
        routeLabel.numberOfLines = 1
        routeLabel.lineBreakMode = .byTruncatingTail
        routeLabel.translatesAutoresizingMaskIntoConstraints = false
        routeLabel.setContentHuggingPriority(.required, for: .horizontal)
        routeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

        arrowTail.backgroundColor = .clear
        arrowTail.contentMode = .scaleAspectFit
        arrowTail.translatesAutoresizingMaskIntoConstraints = false

        // Put your tail image here
        arrowTail.image = UIImage(named: "marker_tail")
    }

    private func setupConstraints() {
        NSLayoutConstraint.activate([
            // bubbleView inside contentView
            bubbleView.topAnchor.constraint(equalTo: contentView.topAnchor),
            bubbleView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            bubbleView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),

            // routeLabel inside bubbleView
            routeLabel.topAnchor.constraint(equalTo: bubbleView.topAnchor, constant: 8),
            routeLabel.leadingAnchor.constraint(equalTo: bubbleView.leadingAnchor, constant: 12),
            routeLabel.trailingAnchor.constraint(equalTo: bubbleView.trailingAnchor, constant: -12),
            routeLabel.bottomAnchor.constraint(equalTo: bubbleView.bottomAnchor, constant: -8),
            routeLabel.heightAnchor.constraint(greaterThanOrEqualToConstant: 20),

            // arrowTail below bubbleView
            arrowTail.topAnchor.constraint(equalTo: bubbleView.bottomAnchor),
            arrowTail.centerXAnchor.constraint(equalTo: bubbleView.centerXAnchor),
            arrowTail.widthAnchor.constraint(equalToConstant: 20),
            arrowTail.heightAnchor.constraint(equalToConstant: 10),
            arrowTail.bottomAnchor.constraint(equalTo: contentView.bottomAnchor)
        ])
    }

    // MARK: - Configure

    func configure(with routeName: String) {
        routeLabel.text = routeName
        invalidateIntrinsicContentSize()
        setNeedsLayout()
        layoutIfNeeded()
    }

    // MARK: - Size

    override var intrinsicContentSize: CGSize {
        return contentView.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize)
    }
}
