import SwiftUI

struct SplitView<Top: View, Bottom: View>: View {
    @AppStorage("mainSplitFraction") private var fraction: Double = 0.4

    private let dividerH: CGFloat = 12

    @State private var cursorPushed = false
    @State private var minTopHeight: CGFloat = 0

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
            let minFraction = (minTopHeight + dividerH) / available
            let desiredTopHeight = available * CGFloat(fraction) - dividerH

            let topHeight = if desiredTopHeight >= minTopHeight {
                desiredTopHeight
            } else {
                minTopHeight
            }

            VStack(spacing: 0) {
                top()
                    .frame(height: topHeight)
                handle(available: available, minFraction: minFraction)
                bottom()
                    .frame(maxHeight: .infinity)
            }
            .coordinateSpace(name: "split")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .onPreferenceChange(ChatTopPanelMinHeight.self) { newMin in
                let minGrew = newMin > minTopHeight
                minTopHeight = newMin

                if minGrew {
                    let newMinFraction = (newMin + dividerH) / available
                    let currentTopHeight = available * CGFloat(fraction) - dividerH

                    if currentTopHeight < newMin {
                        fraction = Double(newMinFraction)
                    }
                }
            }
        }
    }

    private func handle(available: CGFloat, minFraction: Double) -> some View {
        Rectangle()
            .fill(Color.secondary.opacity(0.15))
            .frame(height: dividerH)
            .overlay(
                HStack(spacing: 4) {
                    ForEach(0 ..< 3, id: \.self) { _ in
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
                        let raw = Double(v.location.y / available)
                        fraction = min(max(raw, minFraction), 1.0)
                    }
            )
        #if os(macOS)
            .onHover { hovering in
                if hovering {
                    if !cursorPushed { NSCursor.resizeUpDown.push(); cursorPushed = true }
                } else {
                    if cursorPushed { NSCursor.pop(); cursorPushed = false }
                }
            }
            .onDisappear {
                if cursorPushed { NSCursor.pop(); cursorPushed = false }
            }
        #endif
    }
}
