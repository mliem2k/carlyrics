import AppKit
import SwiftUI

struct LyricsPopoverView: View {
    /// Height the playback controls add below the lyrics, divider included.
    static let controlsHeight: CGFloat = 77

    @ObservedObject var model: LyricsViewModel
    /// Asks the window to grow or shrink by `controlsHeight` before the controls show or hide.
    var onControlsVisibilityChange: @MainActor (Bool) -> Void = { _ in }
    @State private var isHovering = false
    @State private var followsPlayback = true
    @State private var scrollMonitor: Any?
    @State private var hideControlsTask: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 0) {
            if let snapshot = model.snapshot {
                trackHeader(snapshot)
                Divider().overlay(Color.white.opacity(0.08))
                lyricsBody
                if isHovering {
                    Divider().overlay(Color.white.opacity(0.08))
                    playbackControls(snapshot)
                }
            } else {
                emptyState
                    .overlay(alignment: .topTrailing) { quitButton.padding(10) }
                Spacer(minLength: 0)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 0.075, green: 0.09, blue: 0.085))
        .ignoresSafeArea()
        .preferredColorScheme(.dark)
        .onHover { hovering in
            hideControlsTask?.cancel()
            if hovering {
                setControlsVisible(model.snapshot != nil)
            } else {
                hideControlsTask = Task { await hideControlsAfterDelay() }
            }
        }
    }

    private func setControlsVisible(_ visible: Bool) {
        onControlsVisibilityChange(visible)
        isHovering = visible
    }

    /// Leaving the popup hides the controls only after a pause, and never while a
    /// mouse button is down, so dragging an edge to resize does not collapse them.
    private func hideControlsAfterDelay() async {
        repeat {
            try? await Task.sleep(for: .seconds(1.5))
            if Task.isCancelled { return }
        } while NSEvent.pressedMouseButtons != 0
        setControlsVisible(false)
    }

    private var quitButton: some View {
        Button(action: quit) {
            Image(systemName: "power")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(.white.opacity(0.45))
                .frame(width: 22, height: 22)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .help("Quit LiveLyrics")
        .accessibilityLabel("Quit LiveLyrics")
    }

    private func trackHeader(_ snapshot: PlayerSnapshot) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(snapshot.track.title)
                .font(.system(size: 16, weight: .semibold, design: .rounded))
                .foregroundStyle(.white)
                .lineLimit(1)
                .accessibilityAddTraits(.isHeader)

            Text(snapshot.track.artist)
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.58))
                .lineLimit(1)

            Spacer(minLength: 0)
            quitButton
        }
        // The title and artist truncate in a narrow popup; hovering shows them in full.
        .help("\(snapshot.track.title), \(snapshot.track.artist)")
        .padding(.leading, 20)
        .padding(.trailing, 12)
        .padding(.vertical, 10)
    }

    @ViewBuilder
    private var lyricsBody: some View {
        switch model.status {
        case .loading:
            statusMessage("Looking up synced lyrics…", symbol: "text.quote")
        case .noLyrics:
            statusMessage("No synced lyrics found for this track.", symbol: "text.quote")
        case .serviceUnavailable:
            VStack(spacing: 12) {
                statusMessage("Lyrics are unavailable right now.", symbol: "wifi.slash")
                Button("Try again", action: model.refreshNow)
                    .buttonStyle(.bordered)
                    .tint(Color(red: 0.47, green: 0.91, blue: 0.63))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .showingLyrics:
            lyricLines
        case .waitingForSpotify, .permissionRequired, .playerUnavailable:
            EmptyView()
        }
    }

    private var lyricLines: some View {
        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) {
                LazyVStack(alignment: .leading, spacing: 17) {
                    ForEach(Array(model.lines.enumerated()), id: \.element.id) { index, line in
                        Text(line.text)
                            .font(.system(size: index == model.currentLineIndex ? 20 : 17,
                                          weight: index == model.currentLineIndex ? .semibold : .regular,
                                          design: .rounded))
                            .foregroundStyle(index == model.currentLineIndex
                                             ? Color.white
                                             : Color.white.opacity(index < (model.currentLineIndex ?? -1) ? 0.38 : 0.68))
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .id(line.id)
                    }
                }
                .padding(.horizontal, 22)
                .padding(.vertical, 24)
            }
            .onChange(of: model.currentLineIndex) { _ in
                if followsPlayback { scrollToCurrentLine(proxy) }
            }
            .onChange(of: model.lines) { _ in followsPlayback = true }
            .onAppear {
                let follows = $followsPlayback
                scrollMonitor = NSEvent.addLocalMonitorForEvents(matching: .scrollWheel) { event in
                    if event.scrollingDeltaY != 0 { follows.wrappedValue = false }
                    return event
                }
            }
            .onDisappear {
                if let scrollMonitor { NSEvent.removeMonitor(scrollMonitor) }
                scrollMonitor = nil
            }
            .overlay(alignment: .bottomTrailing) {
                if !followsPlayback {
                    Button {
                        followsPlayback = true
                        scrollToCurrentLine(proxy)
                    } label: {
                        Label("Sync", systemImage: "arrow.triangle.2.circlepath")
                            .font(.system(size: 12, weight: .semibold, design: .rounded))
                            .foregroundStyle(Color(red: 0.075, green: 0.09, blue: 0.085))
                            .padding(.horizontal, 11)
                            .padding(.vertical, 6)
                            .background(Color(red: 0.47, green: 0.91, blue: 0.63), in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .help("Jump back to the current line")
                    .padding(12)
                }
            }
            .overlay(alignment: .top) {
                LinearGradient(
                    colors: [Color(red: 0.075, green: 0.09, blue: 0.085), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 14)
                .allowsHitTesting(false)
            }
            .overlay(alignment: .bottom) {
                LinearGradient(
                    colors: [.clear, Color(red: 0.075, green: 0.09, blue: 0.085)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 14)
                .allowsHitTesting(false)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func scrollToCurrentLine(_ proxy: ScrollViewProxy) {
        guard let index = model.currentLineIndex, model.lines.indices.contains(index) else { return }
        withAnimation(.easeOut(duration: 0.25)) {
            proxy.scrollTo(model.lines[index].id, anchor: .center)
        }
    }

    private func playbackControls(_ snapshot: PlayerSnapshot) -> some View {
        HStack(spacing: 30) {
            transportButton("backward.end.fill", label: "Previous track", action: model.previousTrack)

            Button(action: model.playPause) {
                Image(systemName: snapshot.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Color(red: 0.075, green: 0.09, blue: 0.085))
                    .frame(width: 46, height: 46)
                    .background(Color(red: 0.47, green: 0.91, blue: 0.63), in: Circle())
            }
            .buttonStyle(.plain)
            .help(snapshot.isPlaying ? "Pause" : "Play")
            .accessibilityLabel(snapshot.isPlaying ? "Pause" : "Play")

            transportButton("forward.end.fill", label: "Next track", action: model.nextTrack)
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.controlsHeight - 1)
        .background(Color.black.opacity(0.12))
        .overlay(alignment: .top) {
            if let playbackError = model.playbackError {
                Text(playbackError)
                    .font(.system(size: 11))
                    .foregroundStyle(.red.opacity(0.9))
                    .lineLimit(2)
                    .padding(.horizontal, 14)
                    .padding(.top, 3)
            }
        }
    }

    private func transportButton(_ symbol: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.white.opacity(0.76))
                .frame(width: 38, height: 38)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .help(label)
        .accessibilityLabel(label)
    }

    private var emptyState: some View {
        VStack(spacing: 13) {
            Image(systemName: model.status == .permissionRequired ? "lock.fill" : "music.note")
                .font(.system(size: 25, weight: .regular))
                .foregroundStyle(Color(red: 0.47, green: 0.91, blue: 0.63))
                .frame(width: 60, height: 60)
                .background(Color.white.opacity(0.05), in: Circle())

            Text(emptyTitle)
                .font(.system(size: 16, weight: .semibold, design: .rounded))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)

            Text(emptyDetail)
                .font(.system(size: 12))
                .foregroundStyle(.white.opacity(0.56))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: 270)

            if model.status == .permissionRequired {
                Button("Open Privacy Settings", action: openAutomationSettings)
                    .buttonStyle(.borderedProminent)
                    .tint(Color(red: 0.47, green: 0.91, blue: 0.63))
                    .foregroundStyle(Color(red: 0.075, green: 0.09, blue: 0.085))
                    .padding(.top, 3)
            } else if model.status == .playerUnavailable || model.status == .waitingForSpotify {
                Button("Open \(model.playerName)", action: model.openPlayer)
                    .buttonStyle(.borderedProminent)
                    .tint(Color(red: 0.47, green: 0.91, blue: 0.63))
                    .foregroundStyle(Color(red: 0.075, green: 0.09, blue: 0.085))
                    .padding(.top, 3)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 24)
        .padding(.top, 44)
        .accessibilityElement(children: .contain)
    }

    private var emptyTitle: String {
        switch model.status {
        case .permissionRequired: "Allow \(model.playerName) access"
        case .playerUnavailable: "\(model.playerName) could not be read"
        default: "Start a song in \(model.playerName)"
        }
    }

    private var emptyDetail: String {
        switch model.status {
        case .permissionRequired:
            "LiveLyrics needs permission to read the current track and control playback."
        case .playerUnavailable:
            "Check that \(model.playerName) is installed, then try again."
        default:
            "Your synced lyrics will appear here as the song plays."
        }
    }

    private func statusMessage(_ message: String, symbol: String) -> some View {
        VStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 20))
                .foregroundStyle(.white.opacity(0.4))
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.58))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
    }

    private func quit() {
        NSApplication.shared.terminate(nil)
    }

    private func openAutomationSettings() {
        guard let url = URL(string: "x-apple.systempreferences:com.apple.settings.PrivacySecurity.extension?Privacy_Automation") else {
            return
        }
        NSWorkspace.shared.open(url)
    }
}
