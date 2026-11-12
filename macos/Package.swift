// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "LiveLyrics",
    platforms: [.macOS(.v13)],
    products: [
        .executable(name: "LiveLyrics", targets: ["LiveLyrics"])
    ],
    targets: [
        .executableTarget(name: "LiveLyrics")
    ]
)
