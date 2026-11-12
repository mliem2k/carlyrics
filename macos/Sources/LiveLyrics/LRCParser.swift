import Foundation

enum LRCParser {
    private static let timestampExpression = try! NSRegularExpression(
        pattern: #"\[(\d+):(\d+)(?:\.(\d+))?\]"#
    )

    static func parse(_ content: String) -> [LyricLine] {
        let parsed = content.components(separatedBy: .newlines).flatMap(parseLine)
        return parsed.sorted { $0.startTimeMilliseconds < $1.startTimeMilliseconds }
            .enumerated()
            .map { index, line in
                LyricLine(
                    id: index,
                    startTimeMilliseconds: line.startTimeMilliseconds,
                    text: line.text
                )
            }
    }

    private static func parseLine(_ line: String) -> [(startTimeMilliseconds: Int, text: String)] {
        let range = NSRange(line.startIndex..., in: line)
        let matches = timestampExpression.matches(in: line, range: range)
        guard let lastMatch = matches.last,
              let lastTimestampRange = Range(lastMatch.range, in: line)
        else {
            return []
        }

        let text = String(line[lastTimestampRange.upperBound...])
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return [] }

        return matches.compactMap { match in
            guard let minutes = integerCapture(1, in: line, match: match),
                  let seconds = integerCapture(2, in: line, match: match),
                  seconds < 60
            else {
                return nil
            }

            let fraction = stringCapture(3, in: line, match: match) ?? ""
            let milliseconds = Int((fraction + "000").prefix(3)) ?? 0
            let total = minutes.multipliedReportingOverflow(by: 60_000)
            guard !total.overflow else { return nil }
            let withSeconds = total.partialValue.addingReportingOverflow(seconds * 1_000)
            guard !withSeconds.overflow else { return nil }
            let withFraction = withSeconds.partialValue.addingReportingOverflow(milliseconds)
            guard !withFraction.overflow else { return nil }

            return (withFraction.partialValue, text)
        }
    }

    private static func integerCapture(_ index: Int, in text: String, match: NSTextCheckingResult) -> Int? {
        guard let value = stringCapture(index, in: text, match: match) else { return nil }
        return Int(value)
    }

    private static func stringCapture(_ index: Int, in text: String, match: NSTextCheckingResult) -> String? {
        guard index < match.numberOfRanges,
              let range = Range(match.range(at: index), in: text)
        else {
            return nil
        }
        return String(text[range])
    }
}
