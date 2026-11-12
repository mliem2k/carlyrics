import AppKit
import SwiftUI

@main
struct LiveLyricsApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        Settings { EmptyView() }
    }
}

/// Owns the menu bar icon and the corner panel. The panel also opens on launch
/// and on relaunch, so the app stays reachable when macOS hides the icon.
@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate, NSWindowDelegate {
    private let model = LyricsViewModel()
    private var statusItem: NSStatusItem?
    private var panel: NSPanel?
    private var controlsShown = false
    /// How far the panel moved up to fit the controls above the Dock.
    private var controlsShift: CGFloat = 0
    private static let frameName = "LyricsPanel"
    private static let minimumSize = NSSize(width: 200, height: 80)

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApplication.shared.setActivationPolicy(.accessory)

        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        // The Android app's themed icon, as a template so it follows the menu bar's colour.
        let icon = NSImage(named: "MenuBarIcon")
        icon?.isTemplate = true
        icon?.accessibilityDescription = "LiveLyrics"
        item.button?.image = icon
        item.button?.target = self
        item.button?.action = #selector(togglePanel)
        statusItem = item

        showPanel()
    }

    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        showPanel()
        return false
    }

    func windowWillClose(_ notification: Notification) {
        setControlsShown(false)
        model.stop()
    }

    func applicationWillTerminate(_ notification: Notification) {
        // Collapse first so the autosaved frame is the lyrics-only size.
        setControlsShown(false)
    }

    /// Grows the panel downward by the controls' height, so the size the user
    /// picks is for the lyrics alone and the controls never squeeze them.
    private func setControlsShown(_ shown: Bool) {
        guard let panel, shown != controlsShown else { return }
        controlsShown = shown
        let height = LyricsPopoverView.controlsHeight
        var frame = panel.frame
        var minimum = Self.minimumSize
        if shown {
            minimum.height += height
            frame.origin.y -= height
            frame.size.height += height
            let bottom = panel.screen?.visibleFrame.minY ?? frame.minY
            controlsShift = max(0, bottom - frame.minY)
            frame.origin.y += controlsShift
        } else {
            frame.origin.y += height - controlsShift
            frame.size.height -= height
            controlsShift = 0
        }
        panel.contentMinSize = minimum
        panel.setFrame(frame, display: true)
    }

    @objc private func togglePanel() {
        if panel?.isVisible == true {
            panel?.close()
        } else {
            showPanel()
        }
    }

    private func showPanel() {
        let panel = self.panel ?? makePanel()
        self.panel = panel
        model.start()
        panel.orderFrontRegardless()
        panel.makeKey()
    }

    private func makePanel() -> NSPanel {
        let panel = NSPanel(
            // First launch: the title row and about three lyric lines.
            contentRect: NSRect(x: 0, y: 0, width: 360, height: 180),
            styleMask: [.titled, .resizable, .fullSizeContentView, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        panel.titleVisibility = .hidden
        panel.titlebarAppearsTransparent = true
        for button in [NSWindow.ButtonType.closeButton, .miniaturizeButton, .zoomButton] {
            panel.standardWindowButton(button)?.isHidden = true
        }
        panel.contentMinSize = Self.minimumSize
        panel.isMovableByWindowBackground = true
        panel.isReleasedWhenClosed = false
        panel.hidesOnDeactivate = false
        panel.allowsToolTipsWhenApplicationIsInactive = true
        panel.level = .floating
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        let hostingView = NSHostingView(rootView: LyricsPopoverView(model: model) { [weak self] shown in
            self?.setControlsShown(shown)
        })
        // The panel's size comes from the user and the controls, never from the SwiftUI content.
        hostingView.sizingOptions = []
        panel.contentView = hostingView
        panel.delegate = self
        if !panel.setFrameUsingName(Self.frameName) {
            positionInTopRightCorner(panel)
        }
        panel.setFrameAutosaveName(Self.frameName)
        return panel
    }

    private func positionInTopRightCorner(_ panel: NSPanel) {
        guard let visible = (NSScreen.main ?? NSScreen.screens.first)?.visibleFrame else { return }
        let margin: CGFloat = 12
        panel.setFrameOrigin(NSPoint(
            x: visible.maxX - panel.frame.width - margin,
            y: visible.maxY - panel.frame.height - margin
        ))
    }
}
