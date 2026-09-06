import AppKit

// App Store 이미지의 문구와 실제 앱 화면을 결정적으로 합성한다.
// 생성형 이미지에는 글자를 맡기지 않아 스토어 문구와 기능 설명의 정확성을 지킨다.

let canvasSize = NSSize(width: 1320, height: 2868)
let projectRoot = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let assetRoot = projectRoot.appendingPathComponent("app/store-assets/ios/en-US/6.9-inch")
let rawRoot = assetRoot.appendingPathComponent("raw")
let sourceRoot = assetRoot.appendingPathComponent("source")
let outputRoot = assetRoot.appendingPathComponent("v2")

func color(_ hex: UInt32, alpha: CGFloat = 1) -> NSColor {
    NSColor(
        red: CGFloat((hex >> 16) & 0xff) / 255,
        green: CGFloat((hex >> 8) & 0xff) / 255,
        blue: CGFloat(hex & 0xff) / 255,
        alpha: alpha
    )
}

let ink = color(0x102C46)
let blue = color(0x2F73B9)
let brightBlue = color(0x1769E0)
let mint = color(0x00CFA5)
let coral = color(0xF05B58)
let amber = color(0xD88921)
let white = NSColor.white

func roundedRect(_ rect: NSRect, radius: CGFloat, fill: NSColor, stroke: NSColor? = nil, lineWidth: CGFloat = 1) {
    let path = NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
    fill.setFill()
    path.fill()
    if let stroke {
        stroke.setStroke()
        path.lineWidth = lineWidth
        path.stroke()
    }
}

func text(
    _ value: String,
    in rect: NSRect,
    size: CGFloat,
    weight: NSFont.Weight = .regular,
    color: NSColor = ink,
    lineHeight: CGFloat? = nil,
    alignment: NSTextAlignment = .left
) {
    let style = NSMutableParagraphStyle()
    style.alignment = alignment
    style.lineBreakMode = .byWordWrapping
    style.minimumLineHeight = lineHeight ?? size * 1.12
    style.maximumLineHeight = lineHeight ?? size * 1.12
    let attributes: [NSAttributedString.Key: Any] = [
        .font: NSFont.systemFont(ofSize: size, weight: weight),
        .foregroundColor: color,
        .paragraphStyle: style,
        .kern: size >= 40 ? -1.8 : 0
    ]
    (value as NSString).draw(with: rect, options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: attributes)
}

func shadowedCard(_ rect: NSRect, radius: CGFloat, fill: NSColor = white, stroke: NSColor? = nil) {
    NSGraphicsContext.saveGraphicsState()
    let shadow = NSShadow()
    shadow.shadowColor = color(0x071E33, alpha: 0.18)
    shadow.shadowBlurRadius = 36
    shadow.shadowOffset = NSSize(width: 0, height: -16)
    shadow.set()
    roundedRect(rect, radius: radius, fill: fill)
    NSGraphicsContext.restoreGraphicsState()
    if let stroke {
        roundedRect(rect, radius: radius, fill: NSColor.clear, stroke: stroke, lineWidth: 2)
    }
}

func drawCover(_ image: NSImage, in rect: NSRect, radius: CGFloat) {
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).addClip()
    let imageRatio = image.size.width / image.size.height
    let rectRatio = rect.width / rect.height
    var destination = rect
    if imageRatio > rectRatio {
        let width = rect.height * imageRatio
        destination.origin.x -= (width - rect.width) / 2
        destination.size.width = width
    } else {
        let height = rect.width / imageRatio
        destination.origin.y -= (height - rect.height) / 2
        destination.size.height = height
    }
    image.draw(
        in: destination,
        from: .zero,
        operation: .sourceOver,
        fraction: 1,
        respectFlipped: true,
        hints: nil
    )
    NSGraphicsContext.restoreGraphicsState()
}

func drawTopCrop(_ image: NSImage, in rect: NSRect, sourceHeight: CGFloat, radius: CGFloat) {
    let safeHeight = min(sourceHeight, image.size.height)
    // NSImage source 좌표는 아래에서 시작하므로 원본 상단 구간을 명시적으로 선택한다.
    let source = NSRect(x: 0, y: image.size.height - safeHeight, width: image.size.width, height: safeHeight)
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).addClip()
    image.draw(
        in: rect,
        from: source,
        operation: .sourceOver,
        fraction: 1,
        respectFlipped: true,
        hints: nil
    )
    NSGraphicsContext.restoreGraphicsState()
}

func image(named name: String, in root: URL) -> NSImage {
    let url = root.appendingPathComponent(name)
    guard let value = NSImage(contentsOf: url) else {
        fatalError("Missing image: \(url.path)")
    }
    return value
}

func brand(light: Bool) {
    let foreground = light ? white : ink
    roundedRect(NSRect(x: 70, y: 78, width: 300, height: 70), radius: 35, fill: light ? color(0xFFFFFF, alpha: 0.14) : color(0xFFFFFF, alpha: 0.72))
    roundedRect(NSRect(x: 92, y: 98, width: 30, height: 30), radius: 15, fill: light ? mint : brightBlue)
    text("LingKo", in: NSRect(x: 138, y: 88, width: 200, height: 50), size: 29, weight: .bold, color: foreground, lineHeight: 34)
}

func featurePill(_ label: String, x: CGFloat, y: CGFloat, width: CGFloat, dark: Bool = false) {
    roundedRect(
        NSRect(x: x, y: y, width: width, height: 62),
        radius: 31,
        fill: dark ? color(0xFFFFFF, alpha: 0.12) : color(0xFFFFFF, alpha: 0.92),
        stroke: dark ? color(0xFFFFFF, alpha: 0.2) : color(0xC9DEF4),
        lineWidth: 1.5
    )
    text(label, in: NSRect(x: x + 18, y: y + 15, width: width - 36, height: 34), size: 21, weight: .semibold, color: dark ? white : ink, lineHeight: 26, alignment: .center)
}

func export(name: String, draw: () -> Void) {
    let canvas = NSImage(size: canvasSize)
    canvas.lockFocusFlipped(true)
    draw()
    canvas.unlockFocus()
    guard let tiff = canvas.tiffRepresentation,
          let bitmap = NSBitmapImageRep(data: tiff) else {
        fatalError("Failed to rasterize \(name)")
    }
    guard let data = bitmap.representation(using: .png, properties: [:]) else {
        fatalError("Failed to encode \(name)")
    }
    let output = outputRoot.appendingPathComponent(name)
    let normalized = outputRoot.appendingPathComponent(name.replacingOccurrences(of: ".png", with: ".normalizing.png"))
    try! data.write(to: output)

    // Retina backing scale와 alpha channel을 App Store 업로드 규격으로 정규화한다.
    let ffmpeg = URL(fileURLWithPath: "/opt/homebrew/bin/ffmpeg")
    guard FileManager.default.fileExists(atPath: ffmpeg.path) else {
        fatalError("ffmpeg is required to normalize App Store screenshots")
    }
    let process = Process()
    process.executableURL = ffmpeg
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

let home = image(named: "01-home.png", in: rawRoot)
let practice = image(named: "02-practice.png", in: rawRoot)
let recording = image(named: "03-recording.png", in: rawRoot)
let result = image(named: "04-result.png", in: rawRoot)
let lips = image(named: "hae-lips-front.png", in: sourceRoot)
let tongue = image(named: "hae-tongue-side.png", in: sourceRoot)

export(name: "01-speak-korean-know-what-to-fix.png") {
    color(0x1266E3).setFill()
    NSRect(origin: .zero, size: canvasSize).fill()
    brand(light: true)
    text("Speak Korean.\nKnow what to fix.", in: NSRect(x: 70, y: 225, width: 1160, height: 360), size: 112, weight: .heavy, color: white, lineHeight: 116)
    text("Practice useful sentences and start with the sounds\nthat need your attention most.", in: NSRect(x: 76, y: 610, width: 1120, height: 150), size: 37, weight: .medium, color: color(0xE9F3FF), lineHeight: 50)
    shadowedCard(NSRect(x: 84, y: 840, width: 1152, height: 1540), radius: 64)
    drawTopCrop(home, in: NSRect(x: 112, y: 870, width: 1096, height: 1480), sourceHeight: 595, radius: 46)
    featurePill("CHOOSE A SENTENCE", x: 84, y: 2510, width: 350, dark: true)
    featurePill("SPEAK", x: 474, y: 2510, width: 200, dark: true)
    featurePill("IMPROVE", x: 714, y: 2510, width: 250, dark: true)
}

export(name: "02-hear-before-you-speak.png") {
    color(0xEDF6FF).setFill()
    NSRect(origin: .zero, size: canvasSize).fill()
    brand(light: false)
    text("Hear the sound\nbefore you speak.", in: NSRect(x: 70, y: 225, width: 1160, height: 350), size: 108, weight: .heavy, color: ink, lineHeight: 114)
    text("Compare Hangul, standard pronunciation, and romanization.\nThen listen at normal or slow speed.", in: NSRect(x: 76, y: 600, width: 1160, height: 150), size: 34, weight: .medium, color: color(0x49677F), lineHeight: 47)
    featurePill("1  YOUR SENTENCE", x: 76, y: 820, width: 350)
    featurePill("2  STANDARD SOUND", x: 450, y: 820, width: 380)
    featurePill("3  ROMANIZATION", x: 854, y: 820, width: 360)
    shadowedCard(NSRect(x: 100, y: 970, width: 1120, height: 1530), radius: 64, stroke: color(0xCFE2F5))
    drawTopCrop(practice, in: NSRect(x: 128, y: 998, width: 1064, height: 1474), sourceHeight: 610, radius: 46)
    roundedRect(NSRect(x: 332, y: 2600, width: 656, height: 92), radius: 46, fill: brightBlue)
    text("NORMAL  •  SLOW", in: NSRect(x: 360, y: 2623, width: 600, height: 44), size: 28, weight: .bold, color: white, lineHeight: 34, alignment: .center)
}

export(name: "03-ten-second-focused-take.png") {
    color(0x00CFA5).setFill()
    NSRect(origin: .zero, size: canvasSize).fill()
    brand(light: false)
    text("Ten seconds.\nOne focused take.", in: NSRect(x: 70, y: 225, width: 1160, height: 350), size: 110, weight: .heavy, color: ink, lineHeight: 116)
    text("Follow the sentence and romanization while the timer\nand live waveform keep your recording on track.", in: NSRect(x: 76, y: 605, width: 1160, height: 150), size: 35, weight: .medium, color: color(0x123F48), lineHeight: 48)
    shadowedCard(NSRect(x: 130, y: 850, width: 1060, height: 1730), radius: 76)
    drawTopCrop(recording, in: NSRect(x: 158, y: 878, width: 1004, height: 1674), sourceHeight: 735, radius: 56)
    roundedRect(NSRect(x: 442, y: 2650, width: 436, height: 90), radius: 45, fill: ink)
    text("RECORD  •  REVIEW", in: NSRect(x: 464, y: 2672, width: 392, height: 42), size: 27, weight: .bold, color: white, lineHeight: 34, alignment: .center)
}

func scoreDefinition(y: CGFloat, label: String, score: String, description: String, accent: NSColor) {
    roundedRect(NSRect(x: 70, y: y, width: 1180, height: 144), radius: 34, fill: color(0xFFFFFF, alpha: 0.10), stroke: color(0xFFFFFF, alpha: 0.18), lineWidth: 1.5)
    roundedRect(NSRect(x: 94, y: y + 31, width: 82, height: 82), radius: 24, fill: accent)
    text(score, in: NSRect(x: 98, y: y + 52, width: 74, height: 38), size: 29, weight: .heavy, color: white, lineHeight: 34, alignment: .center)
    text(label, in: NSRect(x: 208, y: y + 22, width: 300, height: 44), size: 28, weight: .bold, color: white, lineHeight: 34)
    text(description, in: NSRect(x: 208, y: y + 68, width: 980, height: 54), size: 23, weight: .medium, color: color(0xCFE3F7), lineHeight: 29)
}

export(name: "04-understand-your-score.png") {
    color(0x0D2943).setFill()
    NSRect(origin: .zero, size: canvasSize).fill()
    brand(light: true)
    text("Your 86,\nexplained.", in: NSRect(x: 70, y: 215, width: 1160, height: 350), size: 118, weight: .heavy, color: white, lineHeight: 120)
    text("One overall score, three clear signals, and word-level\nfeedback that points to your next practice target.", in: NSRect(x: 76, y: 595, width: 1160, height: 145), size: 34, weight: .medium, color: color(0xCFE3F7), lineHeight: 47)
    scoreDefinition(y: 790, label: "Accuracy", score: "84", description: "How precisely your sounds match the target pronunciation.", accent: brightBlue)
    scoreDefinition(y: 952, label: "Fluency", score: "91", description: "How smoothly your rhythm and pacing flow through the sentence.", accent: mint)
    scoreDefinition(y: 1114, label: "Full sentence", score: "88", description: "How much of the sentence was spoken without missing words.", accent: coral)
    shadowedCard(NSRect(x: 70, y: 1320, width: 1180, height: 1460), radius: 58)
    drawTopCrop(result, in: NSRect(x: 96, y: 1346, width: 1128, height: 1408), sourceHeight: 550, radius: 42)
}

func guideHeader(number: String, label: String, detail: String, y: CGFloat, accent: NSColor) {
    roundedRect(NSRect(x: 120, y: y, width: 54, height: 54), radius: 18, fill: accent)
    text(number, in: NSRect(x: 122, y: y + 12, width: 50, height: 30), size: 23, weight: .heavy, color: white, lineHeight: 28, alignment: .center)
    text(label, in: NSRect(x: 194, y: y - 2, width: 420, height: 38), size: 25, weight: .bold, color: ink, lineHeight: 31)
    text(detail, in: NSRect(x: 194, y: y + 38, width: 950, height: 50), size: 22, weight: .medium, color: color(0x547083), lineHeight: 28)
}

export(name: "05-see-how-hae-is-made.png") {
    color(0xFFF4E4).setFill()
    NSRect(origin: .zero, size: canvasSize).fill()
    brand(light: false)
    text("See how 해\nis made.", in: NSRect(x: 70, y: 220, width: 1160, height: 350), size: 118, weight: .heavy, color: ink, lineHeight: 122)
    text("Open 공부해요, tap 해 (hae), then match both views.\nSyllables guide the fix; scores stay at word level.", in: NSRect(x: 76, y: 600, width: 1160, height: 145), size: 34, weight: .medium, color: color(0x4C687C), lineHeight: 47)

    shadowedCard(NSRect(x: 70, y: 790, width: 1180, height: 1980), radius: 56, stroke: color(0xE8D6BC))
    guideHeader(number: "1", label: "CHOOSE THE SYLLABLE", detail: "공부해요  ·  gong-bu-hae-yo  ·  word score 88", y: 850, accent: brightBlue)
    roundedRect(NSRect(x: 120, y: 955, width: 132, height: 96), radius: 24, fill: color(0xEDF6FD), stroke: color(0x88B9E8), lineWidth: 2)
    text("해", in: NSRect(x: 132, y: 967, width: 108, height: 43), size: 36, weight: .heavy, color: brightBlue, lineHeight: 40, alignment: .center)
    text("hae", in: NSRect(x: 132, y: 1011, width: 108, height: 25), size: 17, weight: .semibold, color: color(0x547083), lineHeight: 20, alignment: .center)

    guideHeader(number: "2", label: "LIPS · FRONT VIEW", detail: "Relax the jaw; keep the lips gently open and slightly spread.", y: 1115, accent: coral)
    drawCover(lips, in: NSRect(x: 120, y: 1225, width: 1080, height: 610), radius: 34)

    guideHeader(number: "3", label: "TONGUE · SIDE VIEW", detail: "Keep the tongue body low and forward, clear of the palate.", y: 1905, accent: mint)
    drawCover(tongue, in: NSRect(x: 120, y: 2015, width: 1080, height: 610), radius: 34)
}

print("Generated 5 App Store screenshots in \(outputRoot.path)")
