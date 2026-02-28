AIエージェント向けの説明

# 禁止事項
- git への commit,push

# Goal

Build a Kotlin/JVM CLI tool called `cursorGrid` that can:
1. Convert X cursor themes to grid images (`grid` command)
2. Decode XCursor files to PNG images with JSON metadata (`xcur2png` command)
3. Encode PNG images back to XCursor files using JSON metadata (`png2xcur` command)

# Relevant files / directories

```
app/src/main/kotlin/jp/juggler/cursorGrid/
├── CliArgs.kt              # CLI parser with subcommands
├── Main.kt                 # Entry point
├── IAction.kt              # Interface for actions
├── action/
│   ├── ActionGrid.kt       # Grid image generation
│   ├── ActionXCursorToPng.kt  # xcur2png command
│   └── ActionPngToXCursor.kt  # png2xcur command
├── data/
│   └── XCursorImageMeta.kt # JSON-serializable metadata class
└── encoder/
    ├── XcursorDecoder.kt   # XCursor decoding
    └── XcursorEncoder.kt   # XCursor encoding

gradle/libs.versions.toml   # Version catalog
app/build.gradle.kts        # App build config with plugins
README.md                   # Usage documentation
```

## Workflow Design
- `grid`: make grid image of Cursor theme, user can check it quickly.
- `xcur2png`: Decodes XCursor → multiple PNGs (one per size/animation frame) + JSON metadata
- `png2xcur`: Reads JSON metadata + PNGs → encodes back to XCursor
- Supports animation frames with delay parameter

# ビルド
- Kotlin(JVM)のコードをGradleでビルドするシンプルなプロジェクト

* Run `./gradlew run` to build and run the application.
* Run `./gradlew build` to only build the application.
* Run `./gradlew check` to run all checks, including tests.
* Run `./gradlew clean` to clean all build outputs.
* Run `./gradlew :app:copyFatJar` to generate `cursorGrid.jar` in project root

# 技術スタック
- 依存関係のバージョン定義: `gradle/libs.versions.toml`
- CLI引数のパース: `kotlinx-cli`
- JSON: `kotlinx-serialization-json`
- FatJarの作成: `GradleUp/shadow` https://github.com/GradleUp/shadow

# Design decisions

- Keep the project structure simple
- Use kotlinx-cli for CLI argument parsing with subcommands
- Use kotlinx-serialization-json for JSON handling
- Use Gradle with version catalog (libs.versions.toml)
- Generate fat JAR using Shadow plugin with `copyFatJar` task


# Discoveries

- XCursor files can contain multiple sizes and multiple animation frames per size
- `delay` parameter (milliseconds) is used for animation cursors
- `asIntBuffer().put()` on ByteBuffer doesn't update the ByteBuffer's position - must explicitly set position before `flip()`
- Animation frames need sequence numbers in filenames (e.g., `progress_24_000.png`)

# Accomplished

- ✅ Removed buildSrc, moved build logic to app/build.gradle.kts
- ✅ Added Shadow plugin for fat JAR generation
- ✅ Implemented XCursor decoder/encoder
- ✅ Implemented `grid` command with gray background, text labels, spacing
- ✅ Implemented `xcur2png` command with JSON metadata output
- ✅ Implemented `png2xcur` command
- ✅ Fixed ByteBuffer position bug in encoder
