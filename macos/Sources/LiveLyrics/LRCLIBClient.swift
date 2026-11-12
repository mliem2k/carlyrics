import Foundation

struct LRCLIBClient {
    private let session: URLSession
    private let baseURL = URL(string: "https://lrclib.net/api/search")!

    init(session: URLSession = .shared) {
        self.session = session
    }

    func syncedLyrics(for track: TrackInfo) async throws -> [LyricLine] {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "artist_name", value: track.artist),
            URLQueryItem(name: "track_name", value: track.title)
        ]

        let (data, response) = try await session.data(from: components.url!)
        guard let response = response as? HTTPURLResponse,
              (200..<300).contains(response.statusCode)
        else {
            throw LRCLIBError.unavailable
        }

        let results = try JSONDecoder().decode([SearchResult].self, from: data)
        let exactMatch = results.first {
            $0.trackName.localizedCaseInsensitiveCompare(track.title) == .orderedSame &&
                $0.artistName.localizedCaseInsensitiveCompare(track.artist) == .orderedSame
        }
        let result = exactMatch ?? results.first
        guard let lrc = result?.syncedLyrics, !lrc.isEmpty else {
            throw LRCLIBError.noSyncedLyrics
        }

        return LRCParser.parse(lrc)
    }
}

private struct SearchResult: Decodable {
    let trackName: String
    let artistName: String
    let syncedLyrics: String?
}

enum LRCLIBError: LocalizedError {
    case unavailable
    case noSyncedLyrics

    var errorDescription: String? {
        switch self {
        case .unavailable:
            return "Lyrics service is unavailable. Try again in a moment."
        case .noSyncedLyrics:
            return "No synced lyrics found for this track."
        }
    }
}
