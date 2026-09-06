import AppKit

// 실제 Flutter 캡처가 화면의 주인공이 되도록 최소한의 App Store 헤더만 합성한다.
// V3는 운영 서버에서 받은 `해` 입·혀 가이드를 포함한 실제 GuideSheet 캡처를 사용한다.

let canvasSize = NSSize(width: 1320, height: 2868)
let projectRoot = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let assetRoot = projectRoot.appendingPathComponent("app/store-assets/ios/en-US/6.9-inch")
let rawRoot = assetRoot.appendingPathComponent("raw")
let outputRoot = assetRoot.appendingPathComponent("v3")

func color(_ hex: UInt32, alpha: CGFloat = 1) -> NSColor {
    NSColor(
        red: CGFloat((hex >> 16) & 0xff) / 255,
        green: CGFloat((hex >> 8) & 0xff) / 255,
        blue: CGFloat(hex & 0xff) / 255,
        alpha: alpha
    )
}

let ink = color(0x17324A)
let primary = color(0x2F73B9)
let softBlue = color(0xEDF6FD)
let border = color(0xDCE7EF)
let secondary = color(0x5C7386)
let white = NSColor.white

func text(
    _ value: String,
    in rect: NSRect,
    size: CGFloat,
    weight: NSFont.Weight,
    color: NSColor,
    lineHeight: CGFloat
) {
    let style = NSMutableParagraphStyle()
    style.lineBreakMode = .byWordWrapping
    style.minimumLineHeight = lineHeight
    style.maximumLineHeight = lineHeight
    let attributes: [NSAttributedString.Key: Any] = [
        .font: NSFont.systemFont(ofSize: size, weight: weight),
        .foregroundColor: color,
        .paragraphStyle: style,
        .kern: size >= 50 ? -1.2 : 0
    ]
    (value as NSString).draw(
        with: rect,
        options: [.usesLineFragmentOrigin, .usesFontLeading],
        attributes: attributes
    )
}

func roundedRect(_ rect: NSRect, radius: CGFloat, fill: NSColor, stroke: NSColor? = nil) {
    let path = NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
    fill.setFill()
    path.fill()
    if let stroke {
        stroke.setStroke()
        path.lineWidth = 2
        path.stroke()
    }
}

func load(_ name: String) -> NSImage {
    let url = rawRoot.appendingPathComponent(name)
    guard let value = NSImage(contentsOf: url) else {
        fatalError("Missing raw Flutter capture: \(url.path)")
    }
    return value
}

func drawTopCrop(
    _ image: NSImage,
    topOffset: CGFloat,
    sourceHeight: CGFloat,
    in rect: NSRect,
    radius: CGFloat
) {
    let sourceY = image.size.height - topOffset - sourceHeight
    let source = NSRect(
        x: 0,
        y: max(0, sourceY),
        width: image.size.width,
        height: min(sourceHeight, image.size.height - topOffset)
    )
    NSGraphicsContext.saveGraphicsState()
    let shadow = NSShadow()
    shadow.shadowColor = color(0x17324A, alpha: 0.16)
    shadow.shadowBlurRadius = 30
    shadow.shadowOffset = NSSize(width: 0, height: -12)
    shadow.set()
    roundedRect(rect, radius: radius, fill: white)
    NSGraphicsContext.restoreGraphicsState()

    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).addClip()
    image.draw(
        in: rect,
        from: source,
        operation: .sourceOver,
        fraction: 1,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )
    NSGraphicsContext.restoreGraphicsState()
    roundedRect(rect, radius: radius, fill: NSColor.clear, stroke: border)
}

func header(title: String, detail: String, detailHeight: CGFloat = 120) {
    softBlue.setFill()
    NSRect(x: 0, y: 0, width: canvasSize.width, height: 640).fill()
    roundedRect(NSRect(x: 56, y: 52, width: 230, height: 58), radius: 29, fill: white, stroke: border)
    roundedRect(NSRect(x: 76, y: 71, width: 20, height: 20), radius: 10, fill: primary)
    text("LingKo", in: NSRect(x: 112, y: 64, width: 150, height: 34), size: 24, weight: .bold, color: ink, lineHeight: 28)
    text(title, in: NSRect(x: 56, y: 155, width: 1208, height: 190), size: 80, weight: .heavy, color: ink, lineHeight: 86)
    text(detail, in: NSRect(x: 60, y: 365, width: 1196, height: detailHeight), size: 31, weight: .medium, color: secondary, lineHeight: 42)
    primary.setFill()
    NSRect(x: 56, y: 565, width: 92, height: 8).fill()
}

func export(_ name: String, draw: () -> Void) {
    let canvas = NSImage(size: canvasSize)
    canvas.lockFocusFlipped(true)
    white.setFill()
    NSRect(origin: .zero, size: canvasSize).fill()
    draw()
    canvas.unlockFocus()

    guard let tiff = canvas.tiffRepresentation,
          let bitmap = NSBitmapImageRep(data: tiff),
          let png = bitmap.representation(using: .png, properties: [:]) else {
        fatalError("Failed to encode \(name)")
    }
    let output = outputRoot.appendingPathComponent(name)
    let normalized = outputRoot.appendingPathComponent(name.replacingOccurrences(of: ".png", with: ".normalizing.png"))
    try! png.write(to: output)

    // Retina backing scale와 alpha channel을 App Store 업로드 규격으로 고정한다.
    let process = Process()
    process.executableURL = URL(fileURLWithPath: "/opt/homebrew/bin/ffmpeg")
    process.arguments = [
        "-y", "-v", "error", "-i", output.path,
        "-vf", "scale=1320:2868", "-pix_fmt", "rgb24", normalized.path
    ]
    try! process.run()
    process.waitUntilExit()
    guard process.terminationStatus == 0 else {
        fatalError("Failed to normalize \(name)")
    }
    try! FileManager.default.removeItem(at: output)
    try! FileManager.default.moveItem(at: normalized, to: output)
}

let home = load("01-home.png")
let practice = load("02-practice.png")
let recording = load("03-recording.png")
let result = load("04-result-current.png")
let serverGuide = load("05-guide-server.png")

export("01-practice-what-matters.png") {
    header(
        title: "Practice what matters.",
        detail: "See your weakest sounds, then move straight into an everyday Korean sentence."
    )
    drawTopCrop(home, topOffset: 0, sourceHeight: 760, in: NSRect(x: 30, y: 620, width: 1260, height: 2176), radius: 42)
}

export("02-hear-the-exact-pronunciation.png") {
    header(
        title: "Hear the exact pronunciation.",
        detail: "Compare your sentence, the standard Korean sound, and romanization before recording."
    )
    drawTopCrop(practice, topOffset: 0, sourceHeight: 760, in: NSRect(x: 30, y: 620, width: 1260, height: 2176), radius: 42)
}

export("03-record-with-confidence.png") {
    header(
        title: "Record with confidence.",
        detail: "A 10-second timer and live waveform keep one focused take on track."
    )
    drawTopCrop(recording, topOffset: 0, sourceHeight: 770, in: NSRect(x: 30, y: 620, width: 1260, height: 2205), radius: 42)
}

export("04-know-what-every-score-means.png") {
    header(
        title: "Know what every score means.",
        detail: "Accuracy = sound precision  ·  Fluency = rhythm and pace\nFull sentence = words completed  ·  Word score = what to retry",
        detailHeight: 130
    )
    drawTopCrop(result, topOffset: 0, sourceHeight: 770, in: NSRect(x: 30, y: 620, width: 1260, height: 2205), radius: 42)
}

export("05-use-the-real-mouth-and-tongue-guide.png") {
    header(
        title: "See how each syllable is made.",
        detail: "For 해 (hae), compare lip opening from the front with tongue position and contact from the side.",
        detailHeight: 135
    )
    drawTopCrop(serverGuide, topOffset: 270, sourceHeight: 686, in: NSRect(x: 0, y: 620, width: 1320, height: 2058), radius: 0)
}

print("Generated V3 App Store screenshots in \(outputRoot.path)")
