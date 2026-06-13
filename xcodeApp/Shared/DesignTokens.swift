import SwiftUI

enum DesignTokens {
    // MARK: - Colors & Fills
    static let topAreaBackground = Color.secondary.opacity(0.06)
    static let cardBackground: Color = {
#if os(macOS)
        return Color(nsColor: .windowBackgroundColor)
#else
        return Color(uiColor: .systemBackground)
#endif
    }()

    // MARK: - Corner Radii
    static let cornerRadius2: CGFloat = 2
    static let cornerRadius4: CGFloat = 4
    static let cornerRadius6: CGFloat = 6
    static let cornerRadius8: CGFloat = 8
    static let cardCornerRadius: CGFloat = 8
    static let topAreaCornerRadius: CGFloat = 12

    // MARK: - Padding & Spacing
    static let padding2: CGFloat = 2
    static let padding4: CGFloat = 4
    static let padding6: CGFloat = 6
    static let padding8: CGFloat = 8
    static let padding10: CGFloat = 10
    static let padding12: CGFloat = 12
    static let padding16: CGFloat = 16

    static let sectionPadding: CGFloat = 8
    static let cardVerticalPadding: CGFloat = 4

    static let spacing2: CGFloat = 2
    static let spacing4: CGFloat = 4
    static let spacing6: CGFloat = 6
    static let spacing8: CGFloat = 8
    static let spacing12: CGFloat = 12
    static let spacing16: CGFloat = 16

    // MARK: - Icon & Control Sizes
    static let iconSize: CGFloat = 18
    static let actionBarHeight: CGFloat = 28
    static let dotRadius: CGFloat = 2
    static let circleSize4: CGFloat = 4

    // MARK: - Opacities
    static let opacity05: Double = 0.05
    static let opacity06: Double = 0.06
    static let opacity10: Double = 0.10
    static let opacity12: Double = 0.12
    static let opacity15: Double = 0.15
    static let opacity20: Double = 0.20
    static let opacity30: Double = 0.30
    static let opacity35: Double = 0.35
    static let opacity50: Double = 0.50

    // MARK: - Dot Grid
    static let dotSpacing: CGFloat = 20
    static let dotOpacity: Double = opacity12

    // MARK: - Divider
    static let dividerHeight: CGFloat = 12

    // MARK: - Heights
    static let textEditorMinHeight: CGFloat = 60
    static let chatSettingsInstructionsHeight: CGFloat = 100
    static let addTagDialogHeight: CGFloat = 180
    static let chatListMaxWidth: CGFloat = 360
    static let chatListMinWidth: CGFloat = 260
    static let chatSettingsMinWidth: CGFloat = 420
    static let chatSettingsIdealWidth: CGFloat = 460
    static let modelPickerMaxHeight: CGFloat = 380
    static let modelPickerMinHeight: CGFloat = 300
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
            .padding((DesignTokens.actionBarHeight - DesignTokens.iconSize) / 2)
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
