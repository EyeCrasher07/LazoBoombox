# LazoBoombox

LazoBoombox is a portable and placeable music player addon for [LazoDiscs](https://modrinth.com/mod/lazodiscs), powered by [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice) positional audio.

Take your music with you, play it from a handheld Boombox, or place it anywhere in the world and let the music play from its location.

## Features

* 🎵 Play music using [LazoDiscs](https://modrinth.com/mod/lazodiscs) music discs
* 📦 Place the Boombox anywhere in the world
* 💿 Insert, remove, and swap music discs
* 🔊 Positional audio powered by [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice)
* 📍 Configurable playback volume and radius
* 👤 Configurable Boombox ownership protection
* ⏱️ Optional playback progress HUD
* 🎚️ Configurable maximum number of concurrent audio sources
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
* [LazoDiscs](https://modrinth.com/mod/lazodiscs)
* [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice)
* Java 21+

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

## Supported Versions

**Fabric:** 1.21.1–1.21.11

**NeoForge:** 1.21.1–1.21.11

## Related Projects

* [LazoDiscs](https://modrinth.com/mod/lazodiscs) — custom music discs with positional audio
* [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice) — proximity voice chat and positional audio

## License

LazoBoombox is licensed under the GPL-3.0-only license.
