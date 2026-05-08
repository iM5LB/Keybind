# ⌨️ Keybind

Bind keys to server-side commands. Press a key, the server runs a command.

**[Client Mod (Fabric)](https://modrinth.com/plugin/keybind/versions?l=fabric)** • **[Server Plugin (Paper)](https://modrinth.com/plugin/keybind/versions?l=bukkit)**

| Component | Environment | Platform |
| :--- | :--- | :--- |
| `KeybindMod` | **Client only** | Fabric (MC 26.1.2) |
| `KeybindPlugin` | **Server only** | Paper 1.18.2+ |

---

## ✨ Features

- 🔗 **Server-Driven**: Actions are defined entirely on the server and synced to the client on join.
- 📁 **Per-Server Storage**: Key assignments are saved per server IP in `.minecraft/config/keybind-servers/` and never overwritten on rejoin.
- 🛠️ **Dynamic Registration**: New server actions appear in your **Settings → Controls** menu without restarting the game.
- 🏷️ **Custom Display Names**: Server owners set friendly names (e.g., `"Teleport to Spawn"` instead of `spawn`).
- ⏱️ **Dual Cooldowns**: Both a global cooldown (between any actions) and per-action cooldowns prevent spam.
- 🔐 **Permission Support**: Each action supports an optional extra permission node on top of `keybind.use`.
- 📡 **Plugin Messaging**: Uses `keybind:main` / `keybind:sync` plugin message channels. Falls back to `/kbind <action>` if the packet channel is unavailable.
- 🧹 **Stale Action Cleanup**: Obsolete actions from previous sessions are automatically removed from the Controls menu and saved config.

---

## 🚀 How It Works

1. **Sync**: 1 second after a player joins, the plugin sends all configured actions (name, display name, default key) over the `keybind:sync` channel.
2. **Registration**: The mod registers each action as a native Minecraft keybind under the **Keybind** category in **Settings → Controls**.
3. **Trigger**: When a key is pressed (and no screen is open), the mod sends the action name over the `keybind:main` channel.
4. **Execution**: The plugin validates the action name (alphanumeric + underscores only), checks permissions and cooldowns, then runs the command.

---

## 🛠️ Installation

### Server Side
1. Place `KeybindPlugin.jar` into your `plugins/` folder.
2. Restart the server — `plugins/Keybind/config.yml` is generated automatically.
3. Edit `config.yml` to define your actions, then run `/kbind reload`.

### Client Side
1. Place `KeybindMod.jar` into your `.minecraft/mods/` folder.
2. Requires **Fabric Loader 0.18.4+** and **Fabric API**.
3. Launch the game and connect to any server running the plugin.

> The mod works on any server. On servers without the plugin, no keybinds are registered.

---

## ⚙️ Configuration

### `plugins/Keybind/config.yml`
```yaml
# Global cooldown between any keybind actions (milliseconds)
global-cooldown: 500

actions:
  spawn:
    command: "spawn"              # Command to run (without /)
    display-name: "Spawn"        # Label shown in Controls menu
    default-key: "LEFT_BRACKET"  # Suggested key for first-time players
    permission: ""               # Optional extra permission node (leave empty for none)
    cooldown: 1000               # Per-action cooldown (ms); defaults to global-cooldown
    console: false               # true = run as console with {player} replaced by player name

  home:
    command: "home"
    display-name: "Home"
    default-key: "RIGHT_BRACKET"
    permission: ""
    cooldown: 1000
    console: false
```

**Notes:**
- Action names must be alphanumeric + underscores only (e.g. `my_action`).
- `console: true` runs the command as the console sender. Use `{player}` as a placeholder for the player's name (e.g. `command: "tp {player} spawn"`).
- `default-key` is only applied the first time a player connects. Their saved binding is used on subsequent joins.

---

## ⌨️ Supported Keys

### Keyboard
| Category | Values |
| :--- | :--- |
| Letters | `A`–`Z` |
| Numbers | `0`–`9` |
| Function | `F1`–`F25` |
| Arrows | `UP`, `DOWN`, `LEFT`, `RIGHT` |
| Navigation | `PAGE_UP`, `PAGE_DOWN`, `HOME`, `END`, `INSERT`, `DELETE` |
| Special | `SPACE`, `ENTER`, `TAB`, `BACKSPACE`, `ESCAPE`, `PAUSE` |
| Lock keys | `CAPS_LOCK`, `SCROLL_LOCK`, `NUM_LOCK` |
| Other | `PRINT_SCREEN`, `MENU` |
| Modifiers | `LEFT_SHIFT`, `LEFT_CONTROL` (or `LEFT_CTRL`), `LEFT_ALT`, `LEFT_SUPER`, `RIGHT_SHIFT`, `RIGHT_CONTROL` (or `RIGHT_CTRL`), `RIGHT_ALT`, `RIGHT_SUPER` |
| Numpad | `KP_0`–`KP_9`, `KP_ADD`, `KP_SUBTRACT`, `KP_MULTIPLY`, `KP_DIVIDE`, `KP_DECIMAL`, `KP_ENTER`, `KP_EQUAL` |
| Symbols | `LEFT_BRACKET`, `RIGHT_BRACKET`, `BACKSLASH`, `SEMICOLON`, `APOSTROPHE`, `COMMA`, `PERIOD`, `SLASH`, `GRAVE_ACCENT`, `MINUS`, `EQUAL` — or the character directly: `[`, `]`, `\`, `;`, `'`, `,`, `.`, `/`, `` ` ``, `-`, `=` |
| World keys | `WORLD_1`, `WORLD_2` (locale-specific keys on some keyboards) |

### Mouse
| Value | Aliases | Button |
| :--- | :--- | :--- |
| `MOUSE_LEFT` | `MOUSE_1` | Left click |
| `MOUSE_RIGHT` | `MOUSE_2` | Right click |
| `MOUSE_MIDDLE` | `MOUSE_3` | Middle click |
| `MOUSE_4`–`MOUSE_8` | — | Side/extra buttons |

---

## 📜 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/kbind <action>` | Manually trigger an action | `keybind.use` |
| `/kbind list` | List all configured action names | *(any)* |
| `/kbind reload` | Reload config and re-sync all online players | `keybind.admin` |

| Permission | Description | Default |
| :--- | :--- | :--- |
| `keybind.use` | Required to trigger any action | `true` (everyone) |
| `keybind.admin` | Access to `/kbind reload` | `op` |
| `keybind.bypass.cooldown` | Ignore all cooldowns | `op` |

---

## 🏗️ Building from Source

```bash
./gradlew clean build
```

Output jars:
- `keybind-plugin/build/libs/KeybindPlugin-1.0.0.jar`
- `keybind-mod/build/libs/KeybindMod-1.0.0.jar`

Requires Java 17+ for the plugin, Java 25+ for the mod.

---

## 🔒 Security

- Action names are validated server-side against `^[a-zA-Z0-9_]+$` — arbitrary input is rejected.
- Packet length is capped at 256 bytes.
- All command execution happens on the main server thread.
- Players without `keybind.use` cannot trigger any action, even via direct `/kbind` command.
