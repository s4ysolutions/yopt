import SwiftUI

/// A vertically split view with a draggable divider.
/// Fraction persisted via @AppStorage (UserDefaults) — synchronous read means
/// the correct position is applied on the very first frame, with no flick.
/// Drag uses the cursor's absolute Y in a fixed coordinate space so there
/// is no moving reference and no feedback wobble.
struct SplitView<Top: View, Bottom: View>: View {
    @AppStorage("mainSplitFraction") private var fraction: Double = 0.4

    private let minTop: CGFloat = 120
    private let minBottom: CGFloat = 80
    private let dividerH: CGFloat = 12

    private let top: () -> Top
    private let bottom: () -> Bottom

    init(@ViewBuilder top: @escaping () -> Top,
         @ViewBuilder bottom: @escaping () -> Bottom) {
        self.top = top
        self.bottom = bottom
    }

    var body: some View {
        GeometryReader { geo in
            let available = geo.size.height
            let topHeight = max(minTop,
                                min(available * CGFloat(fraction),
                                    available - minBottom - dividerH))
            VStack(spacing: 0) {
                top()
                    .frame(height: topHeight)
                    .zIndex(1)  // top pane dropdowns render above handle + bottom
                handle(available: available)
                bottom()
                    .frame(maxHeight: .infinity)
            }
            .coordinateSpace(name: "split")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func handle(available: CGFloat) -> some View {
        Rectangle()
            .fill(Color.secondary.opacity(0.15))
            .frame(height: dividerH)
            .overlay(
                HStack(spacing: 4) {
                    ForEach(0..<3, id: \.self) { _ in
                        Circle()
                            .fill(Color.secondary.opacity(0.3))
                            .frame(width: 4, height: 4)
                    }
                }
            )
            .gesture(
                DragGesture(minimumDistance: 0, coordinateSpace: .named("split"))
                    .onChanged { v in
                        guard available > 0 else { return }
                        // v.location.y is the cursor's absolute Y in the split coordinate space —
                        // i.e. the desired top-pane height. No moving reference, no wobble.
                        fraction = Double(max(0.2, min(0.8, v.location.y / available)))
                    }
            )
            #if os(macOS)
            .onHover { hovering in
                if hovering { NSCursor.resizeUpDown.push() } else { NSCursor.pop() }
            }
            #endif
    }
}
