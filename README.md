# LazoBoombox

LazoBoombox is a portable and placeable music player addon for [LazoDiscs](https://modrinth.com/mod/lazodiscs), powered by [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice) positional audio.

Release in preparation: **0.1.2**. [Changelog](release-notes/0.1.2.md) · [Русский changelog](release-notes/0.1.2.ru.md) · [Release checklist](docs/RELEASING.md).

Take your music with you, play it from a handheld Boombox, or place it anywhere in the world and let the music play from its location.

## Features

* 🎵 Play music using [LazoDiscs](https://modrinth.com/mod/lazodiscs) music discs
* 📦 Place the Boombox anywhere in the world
* 💿 Insert, remove, and swap music discs
* 🔊 Positional audio powered by [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice)
* 📍 Configurable playback volume and radius
* 👤 Configurable Boombox ownership protection
* ⏱️ Optional playback progress HUD
* 💾 Preserves the inserted disc when picking up or breaking the Boombox
* ⚙️ Configurable through `config/lazoboombox/config.toml`

## How It Works

LazoBoombox uses music discs created by [LazoDiscs](https://modrinth.com/mod/lazodiscs).

A Boombox can be used in two ways:

### Handheld Boombox

* Hold a Boombox to play its stored music.
* Use **Shift + Right Click** to interact with the stored disc.

### Placed Boombox

* **Right Click + Music Disc** — insert a disc and start playback.
* **Right Click + Music Disc** — swap the currently inserted disc.
* **Right Click + Empty Hand** — remove the current disc.
* **Shift + Right Click** — pick up the Boombox.

When placed in the world, the Boombox emits positional audio from its location through [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice).

## Requirements

* Minecraft 1.21.1–1.21.11
* Fabric or NeoForge
* [LazoDiscs](https://modrinth.com/mod/lazodiscs) 1.0.5 or newer
* [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice) 2.1.8 or newer
* Java 21+

## Installation

Install LazoBoombox, LazoDiscs, and Plasmo Voice on both the server and every client, using builds for the same Minecraft version and loader. LazoBoombox adds a registered block, item, and client HUD. Fabric also requires Fabric API.

## Configuration

LazoBoombox provides configuration options for:

* Boombox volume
* Playback radius
* Volume-based radius scaling
* Maximum playback radius
* Maximum concurrent audio sources
* Boombox ownership protection
* Playback progress HUD position

Configuration file:

`config/lazoboombox/config.toml`

## Build Targets

**Fabric:** 1.21.1–1.21.11

**NeoForge:** 1.21.1–1.21.11

These ranges describe source ports. Runtime support also requires a matching Plasmo Voice build. Its official [2.1.17 release](https://github.com/plasmoapp/plasmo-voice/releases/tag/2.1.17) covers 1.21.1, 1.21.4, 1.21.6–1.21.8, and 1.21.11; the other ports need a compatible Plasmo Voice build before use.

## Building and verification

Use Java 21 and Node.js 22 or newer. Run from the repository root:

```sh
node tools/verify-projects.mjs
node tools/test-playback-regressions.mjs
node tools/test-creative-tabs.mjs
node tools/test-block-registration.mjs
node tools/test-release-tools.mjs
node tools/format-java.mjs --check
node tools/build-all.mjs
git diff --check
```

For one target, run `./gradlew clean build` inside its directory. Full builds return a nonzero exit code on failure and record results in `dist/build-results-all.json`. Avoid concurrent uncached builds for the same Minecraft version against a shared Loom cache.

Java uses AOSP formatting (four-space indentation). Apply it with `node tools/format-java.mjs --write`; the tool checks the downloaded formatter's pinned SHA-256. Behavioral tests compile production classes with a controlled Minecraft/PV environment, not a live game. Creative inventory regressions exercise each production registrar, including functional-tab ordering and search visibility; they do not render the actual game menu.

Prepare checked normal JARs, checksums and a manifest with `node tools/prepare-release.mjs`. This performs no upload. Follow the [release checklist](docs/RELEASING.md) before publishing to GitHub, Modrinth or CurseForge.

## Related Projects

* [LazoDiscs](https://modrinth.com/mod/lazodiscs) — custom music discs with positional audio
* [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice) — proximity voice chat and positional audio

## License

LazoBoombox is licensed under the GPL-3.0-only license. See [LICENSE](LICENSE) for the complete text.
