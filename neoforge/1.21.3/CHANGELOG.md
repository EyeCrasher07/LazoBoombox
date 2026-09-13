# LazoBoombox Changelog

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

