import UIKit

class MultipleInfoWindowView: UIView, UIGestureRecognizerDelegate {
    var titleLabel: UILabel!
    var snippetLabel: UILabel!
    var containerView: UIView!
    var arrowView: UIView!
    var iconImageView: UIImageView!
    var contentStackView: UIStackView!
    var textStackView: UIStackView!
    var snippetStackView: UIStackView!
    
    var onClose: (() -> Void)?
    
    // Constants for padding
    private let horizontalPadding: CGFloat = 8
    private let verticalPadding: CGFloat = 4
    private let interItemSpacing: CGFloat = 4
    private let arrowHeight: CGFloat = 8
    private let maxWidth: CGFloat = 300
    private let minWidth: CGFloat = 150
    private let minHeight: CGFloat = 150
    private let iconSize: CGFloat = 16
    
    // Track which fields have content
    private var hasTitle: Bool = false
    private var hasSnippet: Bool = false
    private var currentIconUrl: String?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    class func instanceFromNib() -> MultipleInfoWindowView {
        return MultipleInfoWindowView(frame: .zero)
    }
    
    private func setupView() {
        self.backgroundColor = .clear
        
        // Container view with shadow
        containerView = UIView()
        containerView.backgroundColor = .white
        containerView.layer.cornerRadius = 4
        containerView.layer.shadowColor = UIColor.black.cgColor
        containerView.layer.shadowOpacity = 0.1
        containerView.layer.shadowRadius = 4
        containerView.layer.shadowOffset = CGSize(width: 1, height: 2)
        containerView.layer.borderColor = UIColor.lightGray.cgColor
        containerView.layer.zPosition = 0
        
        containerView.isUserInteractionEnabled = true
                
                // Add tap gesture with delegate
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
        tapGesture.numberOfTapsRequired = 1
        tapGesture.delegate = self  // Set delegate
        tapGesture.cancelsTouchesInView = false  // Important: don't cancel other touches
        self.addGestureRecognizer(tapGesture)
                
        // Arrow view (downward pointing triangle)
        arrowView = UIView()
        arrowView.backgroundColor = .clear
        arrowView.layer.zPosition = 1
        
        // Icon image view
        iconImageView = UIImageView()
        iconImageView.contentMode = .scaleAspectFit
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        iconImageView.widthAnchor.constraint(equalToConstant: iconSize).isActive = true
        iconImageView.heightAnchor.constraint(equalToConstant: iconSize).isActive = true
        
        // Title label
        titleLabel = UILabel()
        if let notoSansBold = UIFont(name: "NotoSans-Bold", size: 14) {
            titleLabel.font = notoSansBold
        } else {
            titleLabel.font = UIFont.boldSystemFont(ofSize: 14)
        }
        titleLabel.textColor = .black
        titleLabel.numberOfLines = 0
        titleLabel.lineBreakMode = .byWordWrapping
        titleLabel.setContentCompressionResistancePriority(.required, for: .vertical)
        
        // Snippet label
        snippetLabel = UILabel()
        if let notoSansBold = UIFont(name: "NotoSans-Bold", size: 12) {
            snippetLabel.font = notoSansBold
        } else {
            snippetLabel.font = UIFont.boldSystemFont(ofSize: 12)
        }
        snippetLabel.textColor = .darkGray
        snippetLabel.numberOfLines = 0
        snippetLabel.lineBreakMode = .byWordWrapping
        snippetLabel.setContentCompressionResistancePriority(.required, for: .vertical)
        
        // Snippet stack view (icon + snippet)
        snippetStackView = UIStackView()
        snippetStackView.axis = .horizontal
        snippetStackView.spacing = 6
        snippetStackView.alignment = .top
        snippetStackView.addArrangedSubview(snippetLabel)
        
        // Text stack view for title and snippet
        textStackView = UIStackView()
        textStackView.axis = .vertical
        textStackView.spacing = interItemSpacing
        textStackView.addArrangedSubview(titleLabel)
        textStackView.addArrangedSubview(snippetStackView)
        
        // Main content stack view
        contentStackView = UIStackView()
        contentStackView.axis = .vertical
        contentStackView.spacing = 0
        
        // Add subviews
        containerView.addSubview(contentStackView)
        self.addSubview(containerView)
        self.addSubview(arrowView)
        
        setupConstraints()
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
           // Allow this gesture to work simultaneously with map gestures
        NSLog("Should recognize simultaneously with: \(otherGestureRecognizer)")
        return true
    }
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        NSLog("Should receive touch: \(touch.location(in: self))")
            return true
    }
    @objc private func handleTap(_ gesture: UITapGestureRecognizer) {
        NSLog("Tap Called - SUCCESS!")
        NSLog("Tap location in view: \(gesture.location(in: self))")
        NSLog("Tap location in container: \(gesture.location(in: containerView))")
    }
    
    private func setupConstraints() {
        containerView.translatesAutoresizingMaskIntoConstraints = false
        contentStackView.translatesAutoresizingMaskIntoConstraints = false
        arrowView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // Container constraints - allow flexible width
            containerView.leadingAnchor.constraint(greaterThanOrEqualTo: self.leadingAnchor),
            containerView.trailingAnchor.constraint(lessThanOrEqualTo: self.trailingAnchor),
            containerView.topAnchor.constraint(equalTo: self.topAnchor),
            containerView.bottomAnchor.constraint(equalTo: arrowView.topAnchor),
            containerView.centerXAnchor.constraint(equalTo: self.centerXAnchor),
            
            // Content stack view constraints
            contentStackView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: horizontalPadding),
            contentStackView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -horizontalPadding),
            contentStackView.topAnchor.constraint(equalTo: containerView.topAnchor, constant: verticalPadding),
            contentStackView.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -verticalPadding),
            
            // Arrow constraints
            arrowView.topAnchor.constraint(equalTo: containerView.bottomAnchor),
            arrowView.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 0),
            arrowView.widthAnchor.constraint(equalToConstant: 12),
            arrowView.heightAnchor.constraint(equalToConstant: arrowHeight),
            arrowView.bottomAnchor.constraint(equalTo: self.bottomAnchor)
        ])
    }
    
    override func layoutSubviews() {
           super.layoutSubviews()
           
           // Calculate 40% position for arrow
           let arrowCenterX = self.bounds.width * 0.4
           let arrowLeading = arrowCenterX - 6
           
           // Update arrow frame
           arrowView.frame = CGRect(
               x: arrowLeading,
               y: containerView.frame.maxY,
               width: 12,
               height: arrowHeight
           )
           
           drawArrow()
       }
    
    private func drawArrow() {
        let arrowPath = UIBezierPath()
        arrowPath.move(to: CGPoint(x: 0, y: -1))
        arrowPath.addLine(to: CGPoint(x: arrowView.bounds.width, y: 0))
        arrowPath.addLine(to: CGPoint(x: arrowView.bounds.width / 2, y: arrowView.bounds.height))
        arrowPath.close()
        
        let arrowLayer = CAShapeLayer()
        arrowLayer.path = arrowPath.cgPath
        arrowLayer.fillColor = UIColor.white.cgColor
        arrowLayer.zPosition = 1
        
        arrowView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
        arrowView.layer.addSublayer(arrowLayer)
    }
    
    func configureWith(title: String?, snippet: String?, iconUrl: String?) {
        // Store which fields have content
        hasTitle = !(title?.isEmpty ?? true)
        hasSnippet = !(snippet?.isEmpty ?? true)
        currentIconUrl = iconUrl
        
        // Set text (use empty string instead of nil for consistent layout)
        titleLabel.text = title ?? ""
        snippetLabel.text = snippet ?? ""
        
        // Hide labels that don't have content
        titleLabel.isHidden = !hasTitle
        snippetLabel.isHidden = !hasSnippet
        
        // Configure appearance based on iconUrl
        configureAppearanceBasedOnIconUrl(iconUrl)
        
        // Update constraints based on content
        updateConstraintsBasedOnContent()
        
        // Calculate and set optimal size using auto layout for more accurate sizing
        let optimalSize = calculateOptimalSize()
        self.frame.size = optimalSize
        
        setNeedsLayout()
        layoutIfNeeded()
    }
    
    private func configureAppearanceBasedOnIconUrl(_ iconUrl: String?) {
        // Remove icon from snippet stack view first
        if snippetStackView.arrangedSubviews.contains(iconImageView) {
            iconImageView.removeFromSuperview()
        }
        
        // Configure based on the actual marker iconUrl patterns
        if let iconUrl = iconUrl, iconUrl.contains("halt") {
            iconImageView.image = UIImage(named: "alert_halted")
            snippetStackView.insertArrangedSubview(iconImageView, at: 0)
            snippetLabel.textColor = UIColor(red: 0.678, green: 0.455, blue: 0.0, alpha: 1.0)
            if let notoSansBold = UIFont(name: "NotoSans-Bold", size: 12) {
                snippetLabel.font = notoSansBold
            } else {
                snippetLabel.font = UIFont.boldSystemFont(ofSize: 12)
            }
            
        } else if let iconUrl = iconUrl, iconUrl.contains("inactive") {
            // Yellow theme for inactive GPS
            iconImageView.image = UIImage(named: "alert_inactive")
        
            snippetStackView.insertArrangedSubview(iconImageView, at: 0)
            snippetLabel.textColor = UIColor(red: 0.776, green: 0.157, blue: 0.157, alpha: 1.0)

            if let notoSansBold = UIFont(name: "NotoSans-Bold", size: 12) {
                snippetLabel.font = notoSansBold
            } else {
                snippetLabel.font = UIFont.boldSystemFont(ofSize: 12)
            }
            
        } else {
            // Default appearance - no icon
            snippetLabel.textColor = .darkGray
            if let notoSansBold = UIFont(name: "NotoSans-Bold", size: 12) {
                snippetLabel.font = notoSansBold
            } else {
                snippetLabel.font = UIFont.boldSystemFont(ofSize: 12)
            }
        }
        
        // Always add the text stack view to content
        if !contentStackView.arrangedSubviews.contains(textStackView) {
            contentStackView.addArrangedSubview(textStackView)
        }
    }
    
    private func updateConstraintsBasedOnContent() {
        // Remove existing constraints that we'll replace
        NSLayoutConstraint.deactivate([
            titleLabel.topAnchor.constraint(equalTo: textStackView.topAnchor),
            snippetStackView.bottomAnchor.constraint(equalTo: textStackView.bottomAnchor),
            titleLabel.bottomAnchor.constraint(equalTo: textStackView.bottomAnchor)
        ])
        
        var constraintsToActivate: [NSLayoutConstraint] = []
        
        if hasTitle && hasSnippet {
            // Both title and snippet - already handled by stack view
        } else if hasTitle {
            // Only title
            constraintsToActivate = [
                titleLabel.bottomAnchor.constraint(equalTo: textStackView.bottomAnchor)
            ]
        } else if hasSnippet {
            // Only snippet
            constraintsToActivate = [
                snippetStackView.topAnchor.constraint(equalTo: textStackView.topAnchor),
                snippetStackView.bottomAnchor.constraint(equalTo: textStackView.bottomAnchor)
            ]
        } else {
            // No content - minimal height
            constraintsToActivate = [
                titleLabel.bottomAnchor.constraint(equalTo: textStackView.bottomAnchor)
            ]
        }
        
        NSLayoutConstraint.activate(constraintsToActivate)
    }
    
    private func calculateOptimalSize() -> CGSize {
        // Use auto layout system for more accurate sizing
        let targetSize = CGSize(width: maxWidth, height: UIView.layoutFittingCompressedSize.height)
        
        // Calculate the container size
        let containerSize = containerView.systemLayoutSizeFitting(
            targetSize,
            withHorizontalFittingPriority: .fittingSizeLevel,
            verticalFittingPriority: .fittingSizeLevel
        )
        
        // Calculate actual required width based on content
        let actualWidth = calculateActualContentWidth()
        
        return CGSize(
            width: min(maxWidth, max(minWidth, actualWidth)),
            height: containerSize.height + arrowHeight
        )
    }
    
    private func calculateActualContentWidth() -> CGFloat {
        var maxContentWidth: CGFloat = 0
        
        // Calculate width for title if it exists
        if hasTitle, let titleText = titleLabel.text, !titleText.isEmpty {
            let titleSize = titleText.boundingRect(
                with: CGSize(width: maxWidth - (2 * horizontalPadding), height: .greatestFiniteMagnitude),
                options: [.usesLineFragmentOrigin, .usesFontLeading],
                attributes: [.font: titleLabel.font!],
                context: nil
            ).size
            maxContentWidth = max(maxContentWidth, titleSize.width)
        }
        
        // Calculate width for snippet if it exists
        if hasSnippet, let snippetText = snippetLabel.text, !snippetText.isEmpty {
            let hasIcon = currentIconUrl?.contains("halt") == true || currentIconUrl?.contains("inactive") == true
            let iconAndSpacingWidth: CGFloat = hasIcon ? (iconSize + 6) : 0
            
            let snippetSize = snippetText.boundingRect(
                with: CGSize(width: maxWidth - (2 * horizontalPadding) - iconAndSpacingWidth, height: .greatestFiniteMagnitude),
                options: [.usesLineFragmentOrigin, .usesFontLeading],
                attributes: [.font: snippetLabel.font!],
                context: nil
            ).size
            maxContentWidth = max(maxContentWidth, snippetSize.width + iconAndSpacingWidth)
        }
        
        // Add padding
        let totalWidth = maxContentWidth + (2 * horizontalPadding)
        
        // If no content, return min width
        if !hasTitle && !hasSnippet {
            return minWidth
        }
        
        return totalWidth
    }
    func getMinimumSize() -> CGSize {
           return CGSize(width: minWidth, height: minHeight)
       }
}
