import SwiftUI

/// A black bar hanging from the notch with the current line. Hovering it opens
/// the full popup in place, with the playback controls.
struct NotchLyricsView: View {
    static let collapsedSize = NSSize(width: 440, height: 30)
    static let expandedSize = NSSize(width: 440, height: 280)

    @ObservedObject var model: LyricsViewModel
    /// Asks the window to take `expandedSize` or `collapsedSize` before the content changes.
    var onExpandedChange: @MainActor (Bool) -> Void
    var onTogglePlacement: @MainActor () -> Void
    @State private var isExpanded = false
    @State private var collapseTask: Task<Void, Never>?

    var body: some View {
        Group {
            if isExpanded {
                LyricsPopoverView(model: model, isInNotch: true, onTogglePlacement: onTogglePlacement)
            } else {
                Text(currentLine)
                    .font(.system(size: 13, weight: .semibold, design: .rounded))
                    .foregroundStyle(.white.opacity(model.snapshot == nil ? 0.56 : 1))
                    .lineLimit(1)
                    .padding(.horizontal, 18)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(.black)
            }
        }
        // Round only the bottom corners: the top edge meets the menu bar under the notch.
        .padding(.top, 18)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .padding(.top, -18)
        .ignoresSafeArea()
        .preferredColorScheme(.dark)
        .onHover { hovering in
            collapseTask?.cancel()
            if hovering {
                setExpanded(true)
            } else {
                collapseTask = Task {
                    if await waitToHide(after: .seconds(0.6)) { setExpanded(false) }
                }
            }
        }
    }

    private var currentLine: String {
        guard let snapshot = model.snapshot else { return model.emptyTitle }
        if let index = model.currentLineIndex, model.lines.indices.contains(index) {
            return model.lines[index].text
        }
        return "\(snapshot.track.title) · \(snapshot.track.artist)"
    }

    private func setExpanded(_ expanded: Bool) {
        guard expanded != isExpanded else { return }
        onExpandedChange(expanded)
        isExpanded = expanded
    }
}
