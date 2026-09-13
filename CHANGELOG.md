# Changelog

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
