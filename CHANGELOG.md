# Changelog

Nightly builds are published as pre-releases on GitHub. The newest entry is first.

## 2026-10-30 (builds 3 to 8)

### Added
1. Android Auto lyrics screen, drawn on a navigation surface so the media card stays visible. Song and artist on top, the current line large, controls that appear on touch, a backdrop in the cover colors.
2. Home screen with player controls, a seek bar, a blurred artwork backdrop drawn edge to edge, and a queue sheet.
3. Lyrics sync to the real Spotify playback position through the media session.
4. In app updater that installs new nightlies from GitHub releases.
5. New app icon, a speech bubble with a music note.
6. Restyled Settings and Library, with a song sheet to share a .lrc file or delete.

### Changed
1. The lyrics engine now runs in the notification listener and the foreground service is gone, so nothing runs while idle.
2. The position timer sleeps while nothing plays, and only music apps are followed.
3. The companion lyrics view is merged into the home screen.
4. The landing page is rebuilt around real screenshots.
5. Dependencies updated, compiled against SDK 37 with a Gradle toolchain.

### Fixed
1. Android Auto now opens: the minimum car app API level is declared on the application.
2. Spotify titles, artist credits and ad breaks are cleaned before lyrics lookup, and lyrics are cached under the player's names.
3. The play state and lyric line are correct when lyrics load while paused.
4. The updater tells nightly builds from the same day apart.
5. The Android Automotive launcher is enabled at runtime.

### Removed
1. The "Synced lyrics" status text from the top bar, the Library list and the song sheet. The highlighted line shows it.
2. The leftover Flutter project, its backups and its build scripts.

## 2026-09-14

### Fixed
1. Data corruption, background leaks and race conditions found in an audit.
2. Signing keystores and hardcoded passwords removed from the repository.

## 2026-06-30 to 2026-07-02

### Added
1. First release: enhanced lyrics matching, phone companion view, home screen widget, Android Auto car UI, Spotify integration, LRCLIB synced lyrics, Lyrics Library, live notification.
2. Nightly release script and the Astro landing page.

### Changed
1. Package renamed to com.mliem.carlyrics, app name to CarLyrics.
2. Permissions and broadcast surface reduced to avoid Play Protect flags.
