## New in this release

This is a bug-fix release focused on plant growth and the greenhouse GUI.

* 🔡 **GUI colour bleed fixed** — the Nether recipe entry used a red colour code that was never reset, turning the rest of the recipe panel text red. Tall/double plants (sunflowers, lilacs, rose bushes, etc.) now also correctly place their upper half instead of just the bottom block. [[PR #130](https://github.com/BentoBoxWorld/Greenhouses/pull/130)]
* **Glow Lichen now grows on land** — it was treated as an underwater-only plant, so it would never appear on exposed blocks such as stone. It now attaches to land blocks as configured (e.g. `GLOW_LICHEN: 10:STONE`). [[PR #131](https://github.com/BentoBoxWorld/Greenhouses/pull/131)] Fixes [#127](https://github.com/BentoBoxWorld/Greenhouses/issues/127)
* **`maxmobs` limit is now enforced** — mob spawning only checked the absolute `maxmobs` cap once before spawning, so greenhouses could overshoot the configured maximum. The cap is now honoured on every spawn. [[PR #131](https://github.com/BentoBoxWorld/Greenhouses/pull/131)] Fixes [#127](https://github.com/BentoBoxWorld/Greenhouses/issues/127)

## Compatibility

✔️ BentoBox API 2.7.1
✔️ Paper Minecraft 1.21.5 - 26.1.x
✔️ Java 21

## Upgrading

1. Take backups of your server, for safety.
2. Download this jar and put it in your addons folder — delete the old one.
3. Restart the server.
4. You should be good to go!

> 🔡 **Locale note:** the English (`en-US.yml`) locale strings for the recipe panel were updated. If you maintain customised locale files, re-check the affected keys.

### Legend

- 🔡 locale files may need to be regenerated or updated.

## What's Changed

* 🔡 Fix color bleed and double-plant bounding box (Stage 1) by @tastybento in https://github.com/BentoBoxWorld/Greenhouses/pull/130
* Fix glow lichen growth and mob limit overshoot (#127) by @tastybento in https://github.com/BentoBoxWorld/Greenhouses/pull/131

**Full Changelog**: https://github.com/BentoBoxWorld/Greenhouses/compare/1.9.4...1.9.5
