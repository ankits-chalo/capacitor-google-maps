//
//  DynamicMarker.swift
//  Plugin
//
//  Created by macbook on 27/01/26.
//  Copyright © 2026 Max Lynch. All rights reserved.
//

import UIKit
import GoogleMaps

final class DynamicMarkerGenerator {

    // MARK: - Constants

    private let circleSize: CGFloat = 35
    private let arrowWidth: CGFloat = 19
    private let arrowHeight: CGFloat = 14
    private let gap: CGFloat = 6
    private let topPadding: CGFloat = 20

    private let shadowRadius: CGFloat = 8.909
    private let shadowColor = UIColor.black.withAlphaComponent(0.4)

    private var bitmapWidth: CGFloat {
        circleSize + 40
    }

    private var bitmapHeight: CGFloat {
        topPadding + arrowHeight + gap + circleSize + 40
    }

    // MARK: - Public

    func generateMarker(
        busImage: UIImage,
        statusColor: UIColor,
        angle: CGFloat
    ) -> UIImage {

        let renderer = UIGraphicsImageRenderer(
            size: CGSize(width: bitmapWidth, height: bitmapHeight)
        )

        return renderer.image { ctx in
            let context = ctx.cgContext
            context.setAllowsAntialiasing(true)
            context.setShouldAntialias(true)

            let pivot = CGPoint(
                x: bitmapWidth / 2,
                y: topPadding + arrowHeight + gap + circleSize / 2
            )

            drawArrow(
                context: context,
                pivot: pivot,
                angle: angle,
                color: statusColor
            )

            drawCircle(
                context: context,
                center: pivot,
                color: statusColor
            )

            drawBusIcon(
                context: context,
                image: busImage,
                center: pivot
            )
        }
    }

    // MARK: - Drawing

    private func drawArrow(
        context: CGContext,
        pivot: CGPoint,
        angle: CGFloat,
        color: UIColor
    ) {
        context.saveGState()

        // Rotate around circle center
        context.translateBy(x: pivot.x, y: pivot.y)
        context.rotate(by: angle * .pi / 180)
        context.translateBy(x: -pivot.x, y: -pivot.y)

        context.setShadow(
            offset: .zero,
            blur: shadowRadius,
            color: shadowColor.cgColor
        )

        let baseY = pivot.y - circleSize / 2 - gap
        let tipY = baseY - arrowHeight

        let leftX = pivot.x - arrowWidth / 2
        let rightX = pivot.x + arrowWidth / 2

        let r: CGFloat = 1.07   // 🔑 softness (1.5–2.5 is ideal)

        let path = UIBezierPath()

        // Start at left base (slightly inset)
        path.move(to: CGPoint(x: leftX + r, y: baseY))

        // Left edge → near tip
        path.addLine(to: CGPoint(x: pivot.x - r, y: tipY + r))

        // Tip corner (soft)
        path.addQuadCurve(
            to: CGPoint(x: pivot.x + r, y: tipY + r),
            controlPoint: CGPoint(x: pivot.x, y: tipY)
        )

        // Right edge → base
        path.addLine(to: CGPoint(x: rightX - r, y: baseY))

        // Bottom-right corner
        path.addQuadCurve(
            to: CGPoint(x: leftX + r, y: baseY),
            controlPoint: CGPoint(x: pivot.x, y: baseY + r)
        )

        path.close()

        color.setFill()
        path.fill()

        context.restoreGState()
    }

    private func drawCircle(
        context: CGContext,
        center: CGPoint,
        color: UIColor
    ) {
        context.setShadow(
            offset: .zero,
            blur: shadowRadius,
            color: shadowColor.cgColor
        )

        let rect = CGRect(
            x: center.x - circleSize / 2,
            y: center.y - circleSize / 2,
            width: circleSize,
            height: circleSize
        )

        color.setFill()
        context.fillEllipse(in: rect)
    }

    private func drawBusIcon(
        context: CGContext,
        image: UIImage,
        center: CGPoint
    ) {
        let scale: CGFloat = 0.55
        let targetSize = circleSize * scale

        // 🔑 Aspect-fit logic (prevents stretching)
        let imageAspect = image.size.width / image.size.height

        let drawSize: CGSize
        if imageAspect > 1 {
            drawSize = CGSize(
                width: targetSize,
                height: targetSize / imageAspect
            )
        } else {
            drawSize = CGSize(
                width: targetSize * imageAspect,
                height: targetSize
            )
        }

        let rect = CGRect(
            x: center.x - drawSize.width / 2,
            y: center.y - drawSize.height / 2,
            width: drawSize.width,
            height: drawSize.height
        )

        image.draw(in: rect)
    }

    // MARK: - Anchor

    func anchor() -> CGPoint {
        let pivotY = topPadding + arrowHeight + gap + circleSize / 2
        return CGPoint(x: 0.5, y: pivotY / bitmapHeight)
    }
}
