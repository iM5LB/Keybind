# Changelog

## 1.0.4

### Fixes & Reliability
- **Controls Menu Duplication**: Resolved issue where keybinds appeared twice in **Settings → Controls → Key Binds** and were duplicated in `options.txt`. Replaced redundant static registration with name-based deduplication and in-place mapping updates in `KeybindManager`.
- **Automatic Settings Deduplication**: Client options mappings are automatically scanned and pruned of duplicate entries on initialization.
- **Forge Event Listener Cleanup**: Removed redundant `RegisterKeyMappingsEvent` registration in Forge 26 to rely on unified dynamic injection without duplicates.
- **Chat Formatting & Encoding**: Replaced legacy `\u00a7` formatting codes in chat sync and key capture overlay messages with native `ChatFormatting` text components, preventing `A a[Keybind]` Mojibake on Windows systems.
- **Forge 26.1 Runtime Bootstrap**: Resolved launcher issue on Forge 26.1 where vanilla unpatched jar was selected instead of patched client binaries.

### NeoForge (Released)
- **NeoForge 1.21.11**: Official support added targeting NeoForge 21.11.48 (Minecraft 1.21.11).
- **NeoForge 26.1 / 26.2**: Full support for Minecraft 26.x with custom Category sorting, payload networking, and client tick handling.
- **NeoForge 1.21**: Stable build targeting NeoForge 21.0.167.

### Plugin
- **Chest GUI Fallback**: Added interactive 54-slot Chest GUI with pagination and click sounds for servers without Paper Dialog API.
- **Update Checker**: Automatic GitHub release check that notifies administrators when joining.
- **Metrics**: Anonymous telemetry via bStats.

## 1.0.3

### Plugin
- Minecraft dialog manager on Paper 1.21.7+ via `/kbind manage`
- Add, edit, delete keybinds and change the global cooldown in-game
- **Press a key…** captures the next key from the updated client mod
- Admin commands: `/kbind add`, `/kbind set`, `/kbind remove` (works on older Paper too)

### Fabric / Forge / NeoForge
- New `keybind:capture` channel so the client can send a pressed key back to the plugin
- Action bar prompt while capturing; ESC cancels

### Forge (released)
- Forge client builds for Minecraft 1.21, 1.21.11, and 26.1.
- Forge 1.21.11 build targets Forge 61.2.1 and accepts Forge 61.1.5+.
- Forge 26 build targets Minecraft 26.1 and Forge 62.0.9.
- Fix Forge mod entrypoint construction and cross-version event-bus registration.
- Fix Minecraft 1.21 resource-pack metadata and Forge loader version metadata.

## 1.0.2

### Fabric (ready with plugin)
- Single jars: `Fabric-Keybind-1.21` and `Fabric-Keybind-26`
- Fix missing common classes in the 26.x jar
- Support 1.21.9+ `KeyMapping.Category` (intermediary-safe)
- Fix Controls UI: inject into `keyMappings`, not `debugKeys`
- Register custom category so binds show under **Keybind Actions**

### NeoForge
- In progress.
