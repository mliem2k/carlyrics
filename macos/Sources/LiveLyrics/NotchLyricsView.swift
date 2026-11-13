import SwiftUI

/// What the app delegate drives: the menu bar icon expands the bar, and the
/// pointer passing over the click-through bar fades it.
@MainActor
final class NotchState: ObservableObject {
    @Published var isExpanded = false
    @Published var isPointerOver = false
}

/// A black bar hanging from the notch with the previous, current and next line.
/// The menu bar icon opens the full popup in place, with the playback controls.
struct NotchLyricsView: View {
    static let collapsedSize = NSSize(width: 440, height: 74)
    static let expandedSize = NSSize(width: 440, height: 280)

    @ObservedObject var model: LyricsViewModel
    @ObservedObject var state: NotchState
    var onCollapse: @MainActor () -> Void
    var onTogglePlacement: @MainActor () -> Void
    @State private var collapseTask: Task<Void, Never>?

    var body: some View {
        Group {
            if state.isExpanded {
                LyricsPopoverView(model: model, isInNotch: true, onTogglePlacement: onTogglePlacement)
                    .onHover { hovering in
                        collapseTask?.cancel()
                        guard !hovering else { return }
                        collapseTask = Task {
                            if await waitToHide(after: .seconds(0.6)) { onCollapse() }
                        }
                    }
            } else {
                collapsedLines
                    .padding(.horizontal, 18)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(.black)
                    // Nearly clear while the pointer is over it, so what is underneath shows.
                    .opacity(state.isPointerOver ? 0.12 : 1)
                    .animation(.easeOut(duration: 0.15), value: state.isPointerOver)
            }
        }
        // Round only the bottom corners: the top edge meets the menu bar under the notch.
        .padding(.top, 18)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .padding(.top, -18)
        .ignoresSafeArea()
        .preferredColorScheme(.dark)
    }

    @ViewBuilder
    private var collapsedLines: some View {
        if let snapshot = model.snapshot {
            // Before the first line, the title stands in for the current one.
            let current = model.currentLineIndex
            VStack(spacing: 3) {
                sideLine(current.map { $0 - 1 })
                Text(current.map { model.lines[$0].text } ?? "\(snapshot.track.title) · \(snapshot.track.artist)")
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                sideLine((current ?? -1) + 1)
            }
        } else {
            Text(model.emptyTitle)
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .foregroundStyle(.white.opacity(0.56))
                .lineLimit(1)
        }
    }

    /// A dimmed neighbouring line, blank when there is none, so the current line stays centred.
    private func sideLine(_ index: Int?) -> some View {
        Text(index.flatMap { model.lines.indices.contains($0) ? model.lines[$0].text : nil } ?? " ")
            .font(.system(size: 12, design: .rounded))
            .foregroundStyle(.white.opacity(0.45))
            .lineLimit(1)
    }
}
