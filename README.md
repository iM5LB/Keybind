# Keybind

A Minecraft keybind-to-command system: bind keyboard keys to server actions.

**Client Mod** (Fabric, Minecraft 26.1) detects key presses and sends them to the server.
**Server Plugin** (Paper, Minecraft 1.21+) receives triggers and executes configured commands.

## How It Works

```
[Player joins server]
        |
[Server sends action list + default keys via keybind:sync]
        |
[Client activates keybinds for this server]
        |
[Player presses key]
        |
[Client sends action via keybind:main packet or /kbind command]
        |
[Server executes configured command]
```

**Per-server keybinds:** Each server defines its own actions and default keys. The client stores customized bindings per server address in `config/keybind-servers/`. When you switch servers, keybinds update automatically.

## Building

```bash
./gradlew build

# Plugin JAR -> keybind-plugin/build/libs/KeybindPlugin-1.0.0.jar
# Mod JAR    -> keybind-mod/build/libs/KeybindMod-1.0.0.jar
```

Gradle auto-downloads the correct JDKs via toolchains (Java 21 for plugin, Java 25 for mod).

## Installation

1. Place `KeybindPlugin-1.0.0.jar` in your Paper server's `plugins/` folder
2. Place `KeybindMod-1.0.0.jar` in your client's `mods/` folder (requires Fabric Loader 0.18.4+ and Fabric API)
3. Restart server and client

## Server Plugin Configuration

**`plugins/Keybind/config.yml`**

```yaml
global-cooldown: 500         # Default cooldown in ms
channel: "keybind:main"      # Action messaging channel
sync-channel: "keybind:sync" # Sync channel (server -> client)

actions:
  spawn:
    command: "spawn"      # Command to run (without /)
    default-key: "K"      # Suggested key for clients
    permission: ""        # Extra permission (optional)
    cooldown: 1000        # Per-action cooldown in ms
    console: false        # Run as console? (default: player)
  home:
    command: "home"
    default-key: "L"
    permission: ""
    cooldown: 1000
    console: false
```

When you add/remove actions and run `/kbind reload`, all connected players receive the updated action list automatically.

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/kbind <action>` | Execute a keybind action | `keybind.use` |
| `/kbind list` | List available actions | `keybind.use` |
| `/kbind reload` | Reload config & sync all players | `keybind.admin` |

### Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `keybind.use` | Use keybind actions | Everyone |
| `keybind.admin` | Reload config | OP |
| `keybind.bypass.cooldown` | Skip cooldowns | OP |
| `keybind.action.<name>` | Per-action permission | Not set (allowed if `keybind.use` is granted) |

## Client Mod

The client mod is fully server-driven. No manual configuration needed.

When you join a server with the Keybind plugin, the server sends its action list and default key assignments. The mod stores your bindings per server in `config/keybind-servers/<server>.json`.

Per-server config example (`config/keybind-servers/play_example_com_25565.json`):
```json
{
  "serverAddress": "play.example.com:25565",
  "bindings": {
    "spawn": "K",
    "home": "L"
  }
}
```

Edit this file to customize keys for a specific server. Changes take effect next time you join.

### Supported Keys

Letters (`A`-`Z`), numbers (`0`-`9`), function keys (`F1`-`F12`), modifiers (`LEFT_SHIFT`, `LEFT_CTRL`, `LEFT_ALT`), and special keys (`SPACE`, `ENTER`, `TAB`, `ESCAPE`, `HOME`, `END`, `PAGE_UP`, `PAGE_DOWN`, arrow keys, punctuation).

## Communication

The mod supports two communication modes:

1. **Packet mode** (preferred): Uses the `keybind:main` plugin messaging channel via `CustomPacketPayload`. No chat spam, faster, more secure.
2. **Command mode** (fallback): Sends `/kbind <action>` as a chat command. Works even if the server doesn't support the packet channel.

The mod automatically falls back to command mode if packet sending isn't available.

## Security

- Permission checks on every action
- Per-action cooldowns prevent spam
- Packet validation (length limits, alphanumeric-only action names)
- Actions execute on the main server thread
- Console commands support `{player}` placeholder for the triggering player's name
- Keybinds only fire when no screen is open (chat, inventory, etc.)

## Project Structure

```
Keybind/
├── keybind-plugin/          # Paper server plugin
│   └── src/main/java/com/keybind/plugin/
│       ├── KeybindPlugin.java      # Main plugin class
│       ├── ConfigManager.java      # Config loading & action registry
│       ├── ActionExecutor.java     # Command execution & cooldowns
│       ├── KeybindCommand.java     # /kbind command handler
│       ├── PacketListener.java     # Plugin messaging listener
│       └── SyncSender.java         # Sends action list to clients
├── keybind-mod/             # Fabric client mod
│   ├── src/main/java/com/keybind/mod/
│   │   ├── KeybindMod.java         # Mod entrypoint & constants
│   │   └── network/
│   │       ├── KeybindActionPayload.java  # Client -> server action
│   │       └── KeybindSyncPayload.java    # Server -> client sync
│   └── src/client/java/com/keybind/mod/client/
│       ├── KeybindModClient.java    # Client entrypoint & receivers
│       ├── KeybindManager.java      # Key polling & action dispatch
│       └── ServerKeybindStorage.java # Per-server config persistence
├── settings.gradle
├── build.gradle
└── gradle.properties
```

## Roadmap

- [ ] Multi-version support (Stonecutter)
- [ ] In-game GUI for keybind management
- [ ] Action arguments support
- [ ] Key combos (e.g. CTRL+K)
