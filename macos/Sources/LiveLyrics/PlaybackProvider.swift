import Foundation

@MainActor
protocol PlaybackProvider: Sendable {
    var displayName: String { get }

    func currentTrack() async throws -> PlayerSnapshot?
    func playPause() async throws
    func previousTrack() async throws
    func nextTrack() async throws
    func openPlayer() async throws
}

enum PlaybackProviderError: LocalizedError {
    case commandFailed
    case permissionRequired
    case unavailable

    var errorDescription: String? {
        switch self {
        case .commandFailed:
            "Playback control failed. Try again."
        case .permissionRequired:
            "Allow LiveLyrics to access the player in System Settings under Privacy & Security, then Automation."
        case .unavailable:
            "The selected music player is unavailable."
        }
    }
}
