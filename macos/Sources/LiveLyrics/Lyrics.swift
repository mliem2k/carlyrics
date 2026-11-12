import Foundation

struct TrackInfo: Equatable, Sendable {
    let title: String
    let artist: String
    let album: String

    var key: String {
        "\(title.lowercased())|\(artist.lowercased())"
    }
}

struct LyricLine: Identifiable, Equatable, Sendable {
    let id: Int
    let startTimeMilliseconds: Int
    let text: String
}

struct PlayerSnapshot: Equatable, Sendable {
    let track: TrackInfo
    let positionMilliseconds: Int
    let isPlaying: Bool
}
