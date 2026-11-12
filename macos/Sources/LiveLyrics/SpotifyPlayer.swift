import AppKit
import Foundation

struct SpotifyPlayer: PlaybackProvider {
    nonisolated private static let bundleIdentifier = "com.spotify.client"

    var displayName: String { "Spotify" }

    func currentTrack() async throws -> PlayerSnapshot? {
        try await Task.detached(priority: .utility) {
            try Self.readCurrentTrack()
        }.value
    }

    func playPause() async throws {
        try await runCommand("playpause")
    }

    func previousTrack() async throws {
        try await runCommand("previous track")
    }

    func nextTrack() async throws {
        try await runCommand("next track")
    }

    func openPlayer() async throws {
        guard let appURL = NSWorkspace.shared.urlForApplication(withBundleIdentifier: Self.bundleIdentifier) else {
            throw PlaybackProviderError.unavailable
        }
        do {
            _ = try await NSWorkspace.shared.openApplication(
                at: appURL,
                configuration: NSWorkspace.OpenConfiguration()
            )
        } catch {
            throw PlaybackProviderError.unavailable
        }
    }

    private func runCommand(_ command: String) async throws {
        try await Task.detached(priority: .userInitiated) {
            guard Self.isRunning else { throw PlaybackProviderError.unavailable }
            _ = try Self.execute("tell application id \"\(Self.bundleIdentifier)\" to \(command)")
        }.value
    }

    nonisolated private static var isRunning: Bool {
        !NSRunningApplication.runningApplications(withBundleIdentifier: bundleIdentifier).isEmpty
    }

    nonisolated private static func readCurrentTrack() throws -> PlayerSnapshot? {
        guard isRunning else { return nil }

        let result = try execute("""
        tell application id "\(bundleIdentifier)"
            if player state is stopped then return missing value
            set currentItem to current track
            return {name of currentItem, artist of currentItem, album of currentItem, player position, player state as text}
        end tell
        """)

        guard result.descriptorType != typeNull,
              let title = result.atIndex(1)?.stringValue,
              let artist = result.atIndex(2)?.stringValue,
              let album = result.atIndex(3)?.stringValue,
              let positionString = result.atIndex(4)?.stringValue,
              let positionSeconds = Double(positionString),
              let state = result.atIndex(5)?.stringValue,
              !title.isEmpty,
              !artist.isEmpty
        else {
            return nil
        }

        return PlayerSnapshot(
            track: TrackInfo(title: title, artist: artist, album: album),
            positionMilliseconds: max(0, Int(positionSeconds * 1_000)),
            isPlaying: state == "playing"
        )
    }

    nonisolated private static func execute(_ source: String) throws -> NSAppleEventDescriptor {
        guard let script = NSAppleScript(source: source) else {
            throw PlaybackProviderError.unavailable
        }

        var errorInfo: NSDictionary?
        let result = script.executeAndReturnError(&errorInfo)
        if let errorInfo {
            let errorNumber = (errorInfo[NSAppleScript.errorNumber] as? NSNumber)?.intValue
            if errorNumber == -1743 || errorNumber == -1744 {
                throw PlaybackProviderError.permissionRequired
            }
            throw PlaybackProviderError.commandFailed
        }
        return result
    }
}

