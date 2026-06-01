import SwiftUI

enum DesignTokens {
    static let topAreaBackground = Color.accentColor.opacity(0.08)
    static let topAreaCornerRadius: CGFloat = 12
    static let cardCornerRadius: CGFloat = 12
    static let sectionPadding: CGFloat = 8
    static let cardVerticalPadding: CGFloat = 4
    static let iconSize: CGFloat = 18
    static let actionBarHeight: CGFloat = 28
    static let dotSpacing: CGFloat = 20
    static let dotRadius: CGFloat = 2
    static let dotOpacity: Double = 0.12
#if os(macOS)
    static let cardBackground = Color(nsColor: .windowBackgroundColor)
#else
    static let cardBackground = Color(uiColor: .systemBackground)
#endif
}

extension View {
    func dotGridBackground() -> some View {
        self.background(DotGridBackground())
    }
}

extension Image {
    func actionIcon() -> some View {
        self.resizable()
            .scaledToFit()
            .frame(width: DesignTokens.iconSize, height: DesignTokens.iconSize)
            .foregroundColor(.secondary)
            .padding((28 - DesignTokens.iconSize) / 2)
    }
}

struct DotGridBackground: View {
    var body: some View {
        Canvas { context, size in
            let spacing = DesignTokens.dotSpacing
            let radius = DesignTokens.dotRadius
            let cols = Int(size.width / spacing) + 2
            let rows = Int(size.height / spacing) + 2
            for col in 0...cols {
                for row in 0...rows {
                    let rect = CGRect(
                        x: CGFloat(col) * spacing - radius,
                        y: CGFloat(row) * spacing - radius,
                        width: radius * 2,
                        height: radius * 2
                    )
                    context.fill(
                        Path(ellipseIn: rect),
                        with: .color(.primary.opacity(DesignTokens.dotOpacity))
                    )
                }
            }
        }
    }
}
