import Foundation
import Combine

@MainActor
final class LyricsViewModel: ObservableObject {
    @Published private(set) var snapshot: PlayerSnapshot?
    @Published private(set) var lines: [LyricLine] = []
    @Published private(set) var status: LyricsStatus = .waitingForSpotify
    @Published private(set) var playbackError: String?

    private let player: any PlaybackProvider
    private let lyricsClient = LRCLIBClient()
    private var refreshTask: Task<Void, Never>?
    private var lyricsTask: Task<Void, Never>?
    private var loadedTrackKey: String?

    init(player: any PlaybackProvider = SpotifyPlayer()) {
        self.player = player
    }

    var playerName: String { player.displayName }

    var emptyTitle: String {
        switch status {
        case .permissionRequired: "Allow \(playerName) access"
        case .playerUnavailable: "\(playerName) could not be read"
        default: "Start a song in \(playerName)"
        }
    }

    var currentLineIndex: Int? {
        guard let position = snapshot?.positionMilliseconds else { return nil }
        return lines.lastIndex { $0.startTimeMilliseconds <= position }
    }

    func start() {
        guard refreshTask == nil else { return }
        refreshTask = Task {
            while !Task.isCancelled {
                await refresh()
                try? await Task.sleep(for: .seconds(1))
            }
        }
    }

    func stop() {
        refreshTask?.cancel()
        refreshTask = nil
        lyricsTask?.cancel()
        lyricsTask = nil
        loadedTrackKey = nil
    }

    func refreshNow() {
        loadedTrackKey = nil
        Task { await refresh() }
    }

    func playPause() {
        performPlaybackAction { try await self.player.playPause() }
    }

    func previousTrack() {
        performPlaybackAction { try await self.player.previousTrack() }
    }

    func nextTrack() {
        performPlaybackAction { try await self.player.nextTrack() }
    }

    func openPlayer() {
        performPlaybackAction { try await self.player.openPlayer() }
    }

    private func performPlaybackAction(_ action: @escaping () async throws -> Void) {
        Task {
            do {
                try await action()
                playbackError = nil
                try? await Task.sleep(for: .milliseconds(250))
                await refresh()
            } catch {
                playbackError = error.localizedDescription
            }
        }
    }

    private func refresh() async {
        do {
            let nextSnapshot = try await player.currentTrack()
            snapshot = nextSnapshot
            playbackError = nil

            guard let nextSnapshot else {
                clearLyrics()
                status = .waitingForSpotify
                return
            }

            guard loadedTrackKey != nextSnapshot.track.key else { return }
            loadedTrackKey = nextSnapshot.track.key
            lyricsTask?.cancel()
            lines = []
            status = .loading(nextSnapshot.track.title)
            let track = nextSnapshot.track
            lyricsTask = Task {
                do {
                    let foundLines = try await lyricsClient.syncedLyrics(for: track)
                    guard !Task.isCancelled, loadedTrackKey == track.key else { return }
                    lines = foundLines
                    status = foundLines.isEmpty ? .noLyrics(track.title) : .showingLyrics
                } catch is CancellationError {
                    return
                } catch let error as LRCLIBError {
                    guard !Task.isCancelled, loadedTrackKey == track.key else { return }
                    status = error == .noSyncedLyrics ? .noLyrics(track.title) : .serviceUnavailable
                } catch {
                    guard !Task.isCancelled, loadedTrackKey == track.key else { return }
                    status = .serviceUnavailable
                }
            }
        } catch PlaybackProviderError.permissionRequired {
            snapshot = nil
            clearLyrics()
            status = .permissionRequired
        } catch {
            snapshot = nil
            clearLyrics()
            status = .playerUnavailable
        }
    }

    private func clearLyrics() {
        loadedTrackKey = nil
        lyricsTask?.cancel()
        lyricsTask = nil
        lines = []
    }
}

enum LyricsStatus: Equatable {
    case waitingForSpotify
    case permissionRequired
    case playerUnavailable
    case loading(String)
    case showingLyrics
    case noLyrics(String)
    case serviceUnavailable
}

extension LRCLIBError: Equatable {
    static func == (lhs: LRCLIBError, rhs: LRCLIBError) -> Bool {
        switch (lhs, rhs) {
        case (.unavailable, .unavailable), (.noSyncedLyrics, .noSyncedLyrics): true
        default: false
        }
    }
}
