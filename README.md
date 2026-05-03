# ⌨️ Keybind

A professional Minecraft keybind-to-command system that bridges the gap between keyboard shortcuts and server-side actions.

**[Client Mod (Fabric)](https://modrinth.com/plugin/keybind/versions?l=fabric)** • **[Server Plugin (Paper)](https://modrinth.com/plugin/keybind/versions?l=bukkit)**

---

## ✨ Features

- 🔗 **Server-Driven**: Keybinds are automatically synchronized from the server on join.
- 📁 **Per-Server Storage**: Your custom keybinds are saved separately for every server you visit.
- 🛠️ **Dynamic Registration**: New server actions appear in your **Controls** menu instantly—no restart required.
- 🏷️ **Custom Display Names**: Server owners can set friendly names (e.g., "Spawn" instead of `spawn_command`).
- ⏱️ **Robust Cooldowns**: Both global and per-action cooldowns to prevent command spam.
- 🔐 **Secure Execution**: Full permission support and optional console-side execution with `{player}` placeholder.
- 📡 **Dual-Mode Sync**: Uses fast Plugin Messaging with a seamless fallback to chat commands.

---

## 🚀 How It Works

1. **Handshake**: When you join a server, the plugin sends its configured action list and default keys.
2. **Registration**: The mod dynamically registers these as native Minecraft keybinds in your **Settings → Controls** menu.
3. **Trigger**: When you press a key, the mod sends a packet (or command fallback) to the server.
4. **Execution**: The server validates your permissions/cooldowns and executes the command.

---

## 🛠️ Installation

### Server Side
1. Place `Keybind-Plugin.jar` into your `plugins/` folder.
2. Restart the server to generate the default configuration.
3. Edit `plugins/Keybind/config.yml` to define your actions.

### Client Side
1. Place `Keybind-Mod.jar` into your `mods/` folder.
2. Requires **Fabric Loader** (0.18.4+) and **Fabric API**.
3. Launch the game and connect to any server running the plugin.

---

## ⚙️ Configuration

### Server `config.yml`
```yaml
# Global cooldown between any actions (in milliseconds)
global-cooldown: 500

actions:
  spawn:
    command: "spawn"           # Command to run (without /)
    display-name: "Teleport to Spawn" # Name shown in Controls menu
    default-key: "LEFT_BRACKET" # Suggested key for new players
    permission: "my.custom.perm" # Optional extra permission
    cooldown: 1000             # Per-action cooldown
    console: false             # Run as console? (default: player)
```

---

## ⌨️ Supported Keys & Mouse Buttons
The mod supports virtually every input on your keyboard and mouse.

### Keyboard
- **Alphanumeric**: `A`-`Z`, `0`-`9`
- **Function Keys**: `F1` through `F25`
- **Navigation**: `UP`, `DOWN`, `LEFT`, `RIGHT`, `PAGE_UP`, `PAGE_DOWN`, `HOME`, `END`
- **Special**: `SPACE`, `ENTER`, `TAB`, `BACKSPACE`, `INSERT`, `DELETE`, `ESCAPE`, `PAUSE`
- **Modifiers**: `LEFT_SHIFT`, `LEFT_CONTROL`, `LEFT_ALT`, `LEFT_SUPER`, `RIGHT_SHIFT`, etc.
- **Numpad**: `KP_0`-`KP_9`, `KP_ADD`, `KP_ENTER`, `KP_DECIMAL`, etc.
- **Symbols**: You can use the character directly (e.g., `\`, `/`, `[`, `]`, `,`, `.`, `;`, `'`, `` ` ``, `-`, `=`) or their names (`BACKSLASH`, `SLASH`, etc.).

### Mouse
Supports standard and multi-button gaming mice:
- `MOUSE_LEFT` (or `MOUSE_1`)
- `MOUSE_RIGHT` (or `MOUSE_2`)
- `MOUSE_MIDDLE` (or `MOUSE_3`)
- `MOUSE_4` through `MOUSE_8` (Gaming/Side buttons)

---

## 📜 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/kbind <action>` | Manually trigger an action | `keybind.use` |
| `/kbind reload` | Reload config & sync players | `keybind.admin` |

| Permission | Description | Default |
| :--- | :--- | :--- |
| `keybind.use` | Basic access to the system | `Everyone` |
| `keybind.admin` | Administrative control | `OP` |
| `keybind.bypass.cooldown` | Ignore all cooldowns | `OP` |

---

## 🏗️ Building from Source

```bash
./gradlew clean build
```
The artifacts will be generated in:
- `keybind-plugin/build/libs/KeybindPlugin-1.0.0.jar`
- `keybind-mod/build/libs/KeybindMod-1.0.0.jar`

---

## 🛡️ Security & UX
- **No Conflict**: The mod detects and cleans up obsolete keybinds from old server sessions.
- **Visual Feedback**: Real-time chat notifications when keybinds are synced.
- **Safety First**: Keybinds only trigger when no UI screens (chat, inventory) are open.
- **Persistence**: Your custom key assignments are never overwritten by server defaults on rejoin.
