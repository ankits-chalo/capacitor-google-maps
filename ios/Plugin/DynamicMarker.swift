//
//  DynamicMarker.swift
//  Plugin
//
//  Created by macbook on 27/01/26.
//  Copyright © 2026 Max Lynch. All rights reserved.
//

import Foundation
import UIKit
import GoogleMaps

final class DynamicMarkerGenerator {

    // MARK: - Constants

    private let circleSize: CGFloat = 100
    private let arrowWidth: CGFloat = 36
    private let arrowHeight: CGFloat = 32
    private let gap: CGFloat = 8
    private let shadowRadius: CGFloat = 8.909
    private let shadowColor = UIColor.black.withAlphaComponent(0.4)

    private var bitmapWidth: CGFloat {
        circleSize + 40
    }

    private var bitmapHeight: CGFloat {
        arrowHeight + gap + circleSize + 40
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

            let centerX = bitmapWidth / 2
            let centerY = 20 + arrowHeight + gap + circleSize / 2

            drawArrow(
                context: context,
                pivot: CGPoint(x: centerX, y: centerY),
                angle: angle,
                color: statusColor
            )

            drawCircle(
                context: context,
                center: CGPoint(x: centerX, y: centerY),
                color: statusColor
            )

            drawBusIcon(
                context: context,
                image: busImage,
                center: CGPoint(x: centerX, y: centerY)
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

        context.translateBy(x: pivot.x, y: pivot.y)
        context.rotate(by: angle * .pi / 180)
        context.translateBy(x: -pivot.x, y: -pivot.y)

        context.setShadow(
            offset: .zero,
            blur: shadowRadius,
            color: shadowColor.cgColor
        )

        let tip = CGPoint(
            x: pivot.x,
            y: pivot.y - circleSize / 2 - gap - arrowHeight
        )

        let left = CGPoint(
            x: pivot.x - arrowWidth / 2,
            y: pivot.y - circleSize / 2 - gap
        )

        let right = CGPoint(
            x: pivot.x + arrowWidth / 2,
            y: pivot.y - circleSize / 2 - gap
        )

        let path = UIBezierPath()
        path.move(to: tip)
        path.addLine(to: left)
        path.addLine(to: right)
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
        let scale: CGFloat = 0.65
        let size = min(image.size.width, image.size.height) * scale

        let rect = CGRect(
            x: center.x - size / 2,
            y: center.y - size / 2,
            width: size,
            height: size
        )

        image.draw(in: rect)
    }

    // MARK: - Anchor

    func anchor() -> CGPoint {
        let pivotY = 20 + arrowHeight + gap + circleSize / 2
        return CGPoint(x: 0.5, y: pivotY / bitmapHeight)
    }
}
