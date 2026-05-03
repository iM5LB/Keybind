# Keybind

A Minecraft keybind-to-command system: bind keyboard keys to server actions.

**Client Mod** (Fabric 1.20.4) detects key presses and sends them to the server.
**Server Plugin** (Paper 1.20.4) receives triggers and executes configured commands.

## Architecture

```
[Player presses key]
        ↓
[Fabric Mod detects key]
        ↓
  Packet (keybind:main) or /kbind command
        ↓
[Paper Plugin receives]
        ↓
[Executes command from config]
```

## Building

```bash
# Build both projects
./gradlew build

# Plugin JAR → keybind-plugin/build/libs/KeybindPlugin-1.0.0.jar
# Mod JAR    → keybind-mod/build/libs/KeybindMod-1.0.0.jar
```

**Requirements:** Java 17+, Gradle 8.5+

## Installation

1. Place `KeybindPlugin-1.0.0.jar` in your Paper server's `plugins/` folder
2. Place `KeybindMod-1.0.0.jar` in your client's `mods/` folder (requires Fabric Loader + Fabric API)
3. Restart server and client

## Server Plugin Configuration

**`plugins/Keybind/config.yml`**

```yaml
global-cooldown: 500    # Default cooldown in ms
channel: "keybind:main"  # Plugin messaging channel

actions:
  spawn:
    command: "spawn"      # Command to run (without /)
    permission: ""        # Extra permission (optional)
    cooldown: 1000        # Per-action cooldown in ms
    console: false        # Run as console? (default: player)
  home:
    command: "home"
    permission: ""
    cooldown: 1000
    console: false
```

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/kbind <action>` | Execute a keybind action | `keybind.use` |
| `/kbind list` | List available actions | `keybind.use` |
| `/kbind reload` | Reload config | `keybind.admin` |

### Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `keybind.use` | Use keybind actions | Everyone |
| `keybind.admin` | Reload config | OP |
| `keybind.bypass.cooldown` | Skip cooldowns | OP |
| `keybind.action.<name>` | Per-action permission | Not set (allowed if `keybind.use` is granted) |

## Client Mod Configuration

**`config/keybind-mod.json`**

```json
{
  "usePackets": true,
  "bindings": {
    "K": "spawn",
    "L": "home"
  }
}
```

- **`usePackets`**: `true` = use plugin messaging (preferred), `false` = fallback to `/kbind` chat commands
- **`bindings`**: Map of key names to action names

### Supported Keys

Letters (`A`-`Z`), numbers (`0`-`9`), function keys (`F1`-`F12`), modifiers (`LEFT_SHIFT`, `LEFT_CTRL`, `LEFT_ALT`), and special keys (`SPACE`, `ENTER`, `TAB`, `ESCAPE`, `HOME`, `END`, `PAGE_UP`, `PAGE_DOWN`, arrow keys, punctuation).

Keybinds also appear in Minecraft's Controls settings under the **Keybind Actions** category, so players can rebind them in-game.

## Communication

The mod supports two communication modes:

1. **Packet mode** (default, recommended): Uses the `keybind:main` plugin messaging channel. No chat spam, faster, more secure.
2. **Command mode** (fallback): Sends `/kbind <action>` as a chat command. Works even if the server doesn't register the channel.

The mod automatically falls back to command mode if the server doesn't support the packet channel.

## Security

- Permission checks on every action
- Per-action cooldowns prevent spam
- Packet validation (length limits, alphanumeric-only action names)
- Actions execute on the main server thread
- Console commands support `{player}` placeholder for the triggering player's name

## Project Structure

```
Keybind/
├── keybind-plugin/          # Paper server plugin
│   └── src/main/java/com/keybind/plugin/
│       ├── KeybindPlugin.java      # Main plugin class
│       ├── ConfigManager.java      # Config loading & action registry
│       ├── ActionExecutor.java     # Command execution & cooldowns
│       ├── KeybindCommand.java     # /kbind command handler
│       └── PacketListener.java     # Plugin messaging listener
├── keybind-mod/             # Fabric client mod
│   ├── src/main/java/com/keybind/mod/
│   │   └── KeybindMod.java         # Mod entrypoint & constants
│   └── src/client/java/com/keybind/mod/client/
│       ├── KeybindModClient.java    # Client entrypoint
│       ├── KeybindConfigManager.java # JSON config loader
│       └── KeybindManager.java      # Keybind registration & tick handler
├── settings.gradle
├── build.gradle
└── gradle.properties
```

## Roadmap

- [ ] In-game GUI for keybind management
- [ ] Per-server config profiles
- [ ] Action arguments support
- [ ] Key combos (e.g. CTRL+K)
