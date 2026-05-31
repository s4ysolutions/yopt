import SwiftUI

struct DraggableSplitter: View {
    @Binding var fraction: Float
    let totalHeight: CGFloat
    let onFractionChanged: (Float) -> Void

    @State private var isDragging = false
    @State private var dragStartFraction: Float? = nil

    var body: some View {
        Rectangle()
            .fill(isDragging ? Color.accentColor.opacity(0.1) : Color.secondary.opacity(0.15))
            .frame(height: 12)
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
                DragGesture()
                    .onChanged { value in
                        isDragging = true
                        if dragStartFraction == nil { dragStartFraction = fraction }
                        guard totalHeight > 0 else { return }
                        let delta = Float(value.translation.height) / Float(totalHeight)
                        fraction = max(0.2, min(0.8, (dragStartFraction ?? fraction) + delta))
                    }
                    .onEnded { _ in
                        isDragging = false
                        dragStartFraction = nil
                        onFractionChanged(fraction)
                    }
            )
    }
}
