# LazoBoombox Changelog


## [0.1.2] - Unreleased

### Fixed

- Make the boombox available in creative inventory search as well as the Functional Blocks tab on every NeoForge target.
- Use Fabric's block-entity builder on Minecraft 1.21.2–1.21.11 to avoid the erroneous vanilla data-fixer lookup without changing registry IDs or saved boombox data.
- Keep replacement playback sessions safe from late callbacks belonging to a previous track.
- Make audio-source startup, stop and cleanup atomic; drain buffered frames before track completion.
- Preserve the inserted disc and owner through ordinary block loot, including non-player destruction paths; respect creative mode and doTileDrops.
- Assign an owner when placing an unowned boombox with ownership enabled.
- Stop playback when its player disconnects/dies, its chunk unloads or its block entity is removed.
- Respect the placed_boombox switch on placement and playback.
- Support Shift+right-click pickup with occupied hands on Fabric and older NeoForge targets.
- Parse quoted HUD positions, inline TOML comments and booleans correctly; reject invalid/non-finite numeric settings.
- Play a valid offhand boombox when the main-hand boombox is empty, invalid or unusable, and use the same selection for the HUD.
- Reset stale HUD state after disconnect and honor hidden GUI mode.
- Send HUD payloads only after the client has negotiated the channel.
- Retry failed tracks with a five-second backoff and notify the player once per failed disc, without delaying normal track looping.
- Retry failed position updates and correct Sable projection/far-coordinate handling.
- Copy incoming disc stacks rather than retaining mutable references.
- Fix boombox crafting recipes on Minecraft 1.21.2–1.21.3 (Fabric/NeoForge) to use ingredient ID strings instead of legacy `{"item": ...}` objects.

### Improved

- Require LazoDiscs 1.0.5 or newer and align compile-only LavaPlayer with its 2.2.7 runtime. Do not bundle duplicate LazoDiscs/LavaPlayer classes.
- Correct Minecraft/NeoForge dependency bounds and resource/data pack metadata.
- Standardize Java formatting, clarify compatibility comments and reduce routine log noise.
- Update the build toolchain, include the existing GPL-3.0-only license text, and make archive settings reproducible.
- Add playback/config/HUD, creative inventory and block-registration regressions, project/resource checks, full-matrix CI and checked release packaging.

### Compatibility

Requires LazoDiscs 1.0.5+, Plasmo Voice 2.1.8+ and Java 21. Install all three mods on both server and client; Fabric additionally requires Fabric API.

Source ports exist for Minecraft 1.21.1–1.21.11 on Fabric and NeoForge. Official Plasmo Voice 2.1.17 metadata covers 1.21.1, 1.21.4, 1.21.6–1.21.8 and 1.21.11. Other ports require a separately compatible Plasmo Voice build; compilation alone does not establish runtime support.


## 0.1.1 — 2026-09-12

### Fixed
- **Disc serialization (root cause of disc-loss on Sable assembly/disassembly):**
  `BoomboxBlockEntity.saveAdditional` was calling the two-argument `disc.save(registries, discTag)`
  and ignoring its return value. In MC 1.21.1 this form may not populate the CompoundTag
  argument; the stored tag was empty, so every serialize/deserialize cycle silently lost the
  disc. Changed to the single-argument `disc.save(registries)`, matching what vanilla
  `JukeboxBlockEntity` does.

- **Sable platform assembly — orphaned block entities:** a previous workaround skipped
  `super.onRemove()` on the server to keep the block entity alive after the block was set
  to AIR. This left orphaned block entities in the chunk map that were written to disk; on
  the next world load Minecraft threw `IllegalStateException: Invalid block entity … got
  Block{minecraft:air}` for each one. Now that the serialization bug is fixed, `onRemove`
  always calls `super.onRemove()`.

- **Sable platform assembly — playback position projection:** `BoomboxAudioEngine.start`
  used `BoomboxSableCompat.project()`, which short-circuits for coordinates within
  ±1 000 000 blocks and never calls the Sable API in that range. Replaced with
  `BoomboxSableCompat.projectBoomboxCenter()`, which always calls
  `Sable.HELPER.projectOutOfSubLevel()` — same as LazoDiscs jukeboxes.

- **Sable platform assembly — immediate playback restart (NeoForge):** added `onLoad()`
  override so the boombox restarts playback in the same tick Sable places the block entity
  at its new position, instead of waiting up to 20 ticks for `serverTick`.

- **`onRemove` client-side ClassCastException (NeoForge 1.21.1–1.21.4):** replaced the
  unsafe `(ServerLevel) level` cast with `if (level instanceof ServerLevel sl)`.

### Added
- **Crafting recipe:**
  ```
  [stone_button] [glass_pane]  [stone_button]
  [iron_ingot]   [jukebox]     [iron_ingot]
  [gold_nugget]  [redstone]    [gold_nugget]
  ```
  Also fixed the data-pack path from `data/<mod>/recipes/` to `data/<mod>/recipe/`
  (singular — MC 1.21 pack format change). Affected versions: 1.21.1–1.21.3.

---

## 0.1.0 — initial public release
