package com.keybind.plugin.gui;

import com.keybind.plugin.ConfigManager;
import com.keybind.plugin.KeyCaptureDraft;
import com.keybind.plugin.KeybindPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class KeybindInventoryMenu implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int[] ACTION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PAGE_SIZE = ACTION_SLOTS.length; // 28 actions per page

    private final KeybindPlugin plugin;

    public KeybindInventoryMenu(KeybindPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class MenuHolder implements InventoryHolder {
        private final int page;
        private final List<ConfigManager.ActionConfig> pageActions;

        public MenuHolder(int page, List<ConfigManager.ActionConfig> pageActions) {
            this.page = page;
            this.pageActions = pageActions;
        }

        public int getPage() {
            return page;
        }

        public List<ConfigManager.ActionConfig> getPageActions() {
            return pageActions;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static void open(Player player, KeybindPlugin plugin, int page) {
        ConfigManager config = plugin.getConfigManager();
        List<ConfigManager.ActionConfig> allActions = config.getActions().values().stream()
                .sorted(Comparator.comparing(action -> action.getName().toLowerCase(Locale.ROOT)))
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil(allActions.size() / (double) PAGE_SIZE));
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, allActions.size());
        List<ConfigManager.ActionConfig> pageActions = allActions.subList(from, to);

        MenuHolder holder = new MenuHolder(currentPage, pageActions);
        Component title = Component.text("⌨ Keybind Manager", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                .append(Component.text("  ·  Page ", NamedTextColor.DARK_GRAY))
                .append(Component.text(currentPage + 1, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("/" + totalPages, NamedTextColor.DARK_GRAY));

        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, title);

        // Header frame (Row 0: slots 0-8)
        ItemStack cyanFrame = createItem(Material.CYAN_STAINED_GLASS_PANE, Component.text(" "));
        ItemStack darkFrame = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, cyanFrame);
        }

        // Center info in header
        inv.setItem(4, createItem(
                Material.NETHER_STAR,
                Component.text("Keybind System", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(
                        Component.text("──────────────────────────", NamedTextColor.DARK_GRAY),
                        Component.text("• Configured Actions: ", NamedTextColor.GRAY)
                                .append(Component.text(allActions.size(), NamedTextColor.WHITE, TextDecoration.BOLD)),
                        Component.text("• Global Cooldown: ", NamedTextColor.GRAY)
                                .append(Component.text(config.getGlobalCooldown() + "ms", NamedTextColor.WHITE, TextDecoration.BOLD)),
                        Component.text("• Update Checker: ", NamedTextColor.GRAY)
                                .append(Component.text(config.isUpdateCheckerEnabled() ? "Enabled" : "Disabled", config.isUpdateCheckerEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)),
                        Component.text("• Anonymous Metrics: ", NamedTextColor.GRAY)
                                .append(Component.text(config.isMetricsEnabled() ? "Enabled" : "Disabled", config.isMetricsEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)),
                        Component.text("──────────────────────────", NamedTextColor.DARK_GRAY),
                        Component.text("Click any action below to customize its bind,", NamedTextColor.YELLOW),
                        Component.text("cooldown, console execution, or delete it.", NamedTextColor.YELLOW)
                )
        ));

        // Side frames for rows 1 to 4
        int[] sideSlots = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : sideSlots) {
            inv.setItem(slot, darkFrame);
        }

        // Populate action grid slots
        for (int i = 0; i < pageActions.size(); i++) {
            ConfigManager.ActionConfig action = pageActions.get(i);
            int slot = ACTION_SLOTS[i];
            inv.setItem(slot, createActionItem(action));
        }

        // Row 4 divider (slots 36-44)
        for (int i = 36; i <= 44; i++) {
            inv.setItem(i, darkFrame);
        }

        // Row 5 bottom toolbar (slots 45-53)
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, darkFrame);
        }

        // Slot 45: Previous page
        if (currentPage > 0) {
            inv.setItem(45, createItem(
                    Material.ARROW,
                    Component.text("◀ Previous Page", NamedTextColor.YELLOW, TextDecoration.BOLD),
                    List.of(Component.text("Go to page " + currentPage, NamedTextColor.GRAY))
            ));
        }

        // Slot 47: Add action
        inv.setItem(47, createItem(
                Material.EMERALD,
                Component.text("✚ Add New Keybind", NamedTextColor.GREEN, TextDecoration.BOLD),
                List.of(
                        Component.text("──────────────────────────", NamedTextColor.DARK_GRAY),
                        Component.text("Click to create a new action command.", NamedTextColor.GRAY),
                        Component.text("Pre-populates chat with: ", NamedTextColor.GRAY)
                                .append(Component.text("/kbind add", NamedTextColor.YELLOW))
                )
        ));

        // Slot 49: Settings & quick config
        inv.setItem(49, createItem(
                Material.COMPARATOR,
                Component.text("⚙ Global Settings", NamedTextColor.GOLD, TextDecoration.BOLD),
                List.of(
                        Component.text("──────────────────────────", NamedTextColor.DARK_GRAY),
                        Component.text("• Global Cooldown: ", NamedTextColor.GRAY)
                                .append(Component.text(config.getGlobalCooldown() + "ms", NamedTextColor.WHITE, TextDecoration.BOLD)),
                        Component.text("• Update Checker: ", NamedTextColor.GRAY)
                                .append(Component.text(config.isUpdateCheckerEnabled() ? "Enabled" : "Disabled", config.isUpdateCheckerEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)),
                        Component.text("• Metrics (bStats): ", NamedTextColor.GRAY)
                                .append(Component.text(config.isMetricsEnabled() ? "Enabled" : "Disabled", config.isMetricsEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)),
                        Component.text("──────────────────────────", NamedTextColor.DARK_GRAY),
                        Component.text("▶ Left-Click: ", NamedTextColor.YELLOW)
                                .append(Component.text("Cycle Global Cooldown", NamedTextColor.GRAY)),
                        Component.text("▶ Right-Click: ", NamedTextColor.GOLD)
                                .append(Component.text("Toggle Update Checker", NamedTextColor.GRAY)),
                        Component.text("▶ Shift-Click: ", NamedTextColor.AQUA)
                                .append(Component.text("Toggle Metrics", NamedTextColor.GRAY))
                )
        ));

        // Slot 51: Reload & Sync
        inv.setItem(51, createItem(
                Material.CLOCK,
                Component.text("⟳ Reload & Re-Sync", NamedTextColor.YELLOW, TextDecoration.BOLD),
                List.of(
                        Component.text("──────────────────────────", NamedTextColor.DARK_GRAY),
                        Component.text("Reloads config.yml and pushes all keybinds", NamedTextColor.GRAY),
                        Component.text("to all connected players instantly.", NamedTextColor.GRAY),
                        Component.text("──────────────────────────", NamedTextColor.DARK_GRAY),
                        Component.text("▶ Click to reload & broadcast", NamedTextColor.GREEN)
                )
        ));

        // Slot 53: Next page
        if (currentPage < totalPages - 1) {
            inv.setItem(53, createItem(
                    Material.ARROW,
                    Component.text("Next Page ▶", NamedTextColor.YELLOW, TextDecoration.BOLD),
                    List.of(Component.text("Go to page " + (currentPage + 2), NamedTextColor.GRAY))
            ));
        }

        player.openInventory(inv);
    }

    private static Material resolveActionIcon(ConfigManager.ActionConfig action) {
        String query = (action.getName() + " " + action.getCommand()).toLowerCase(Locale.ROOT);
        if (query.contains("spawn")) return Material.COMPASS;
        if (query.contains("home")) return Material.RED_BED;
        if (query.contains("warp")) return Material.ENDER_PEARL;
        if (query.contains("tp") || query.contains("teleport")) return Material.CHORUS_FRUIT;
        if (query.contains("back")) return Material.ENDER_EYE;
        if (query.contains("feed") || query.contains("eat") || query.contains("food")) return Material.COOKED_BEEF;
        if (query.contains("heal") || query.contains("health")) return Material.GOLDEN_APPLE;
        if (query.contains("fly") || query.contains("flight")) return Material.FEATHER;
        if (query.contains("kit")) return Material.CHEST;
        if (query.contains("craft") || query.contains("wb")) return Material.CRAFTING_TABLE;
        if (query.contains("ec") || query.contains("enderchest")) return Material.ENDER_CHEST;
        if (query.contains("trash") || query.contains("disposal") || query.contains("bin")) return Material.CAULDRON;
        if (query.contains("help") || query.contains("menu")) return Material.BOOK;
        if (query.contains("shop") || query.contains("store") || query.contains("market")) return Material.GOLD_INGOT;
        if (query.contains("discord") || query.contains("vote") || query.contains("link")) return Material.PAPER;
        if (query.contains("day") || query.contains("sun")) return Material.SUNFLOWER;
        if (query.contains("night")) return Material.PHANTOM_MEMBRANE;
        if (query.contains("repair") || query.contains("fix")) return Material.ANVIL;
        if (action.isConsole()) return Material.COMMAND_BLOCK;
        return Material.NAME_TAG;
    }

    private static ItemStack createActionItem(ConfigManager.ActionConfig action) {
        String key = (action.getDefaultKey() == null || action.getDefaultKey().isBlank()) ? "Unbound" : action.getDefaultKey();
        String perm = (action.getPermission() == null || action.getPermission().isBlank()) ? "keybind.use (default)" : action.getPermission();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("──────────────────────────", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("• Identifier: ", NamedTextColor.GRAY)
                .append(Component.text(action.getName(), NamedTextColor.WHITE)));
        lore.add(Component.text("• Command: ", NamedTextColor.GRAY)
                .append(Component.text("/" + action.getCommand(), NamedTextColor.GREEN, TextDecoration.BOLD)));
        lore.add(Component.text("• Default Key: ", NamedTextColor.GRAY)
                .append(Component.text("[" + key + "]", NamedTextColor.GOLD, TextDecoration.BOLD)));
        lore.add(Component.text("• Cooldown: ", NamedTextColor.GRAY)
                .append(Component.text(action.getCooldown() + "ms", NamedTextColor.AQUA)));
        lore.add(Component.text("• Execution: ", NamedTextColor.GRAY)
                .append(Component.text(action.isConsole() ? "Console Sender" : "Player Sender",
                        action.isConsole() ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.WHITE)));
        lore.add(Component.text("• Permission: ", NamedTextColor.GRAY)
                .append(Component.text(perm, NamedTextColor.YELLOW)));
        lore.add(Component.text("──────────────────────────", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("▶ Left-Click: ", NamedTextColor.YELLOW)
                .append(Component.text("Set Default Key (Keystroke Capture)", NamedTextColor.GRAY)));
        lore.add(Component.text("▶ Right-Click: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Toggle Player / Console", NamedTextColor.GRAY)));
        lore.add(Component.text("▶ Shift + Left-Click: ", NamedTextColor.GOLD)
                .append(Component.text("Cycle Cooldown (+250ms)", NamedTextColor.GRAY)));
        lore.add(Component.text("▶ Shift + Right-Click: ", NamedTextColor.RED)
                .append(Component.text("Delete Keybind", NamedTextColor.GRAY)));

        return createItem(
                resolveActionIcon(action),
                Component.text(action.getDisplayName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                        .append(Component.text(" [" + key + "]", NamedTextColor.DARK_AQUA)),
                lore
        );
    }

    private static ItemStack createItem(Material mat, Component name) {
        return createItem(mat, name, List.of());
    }

    private static ItemStack createItem(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            if (!lore.isEmpty()) {
                meta.lore(lore.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= INVENTORY_SIZE) {
            return;
        }

        // Check if an action slot was clicked
        for (int i = 0; i < ACTION_SLOTS.length; i++) {
            if (ACTION_SLOTS[i] == slot) {
                if (i < holder.getPageActions().size()) {
                    ConfigManager.ActionConfig action = holder.getPageActions().get(i);
                    handleActionClick(player, action, event.getClick(), holder.getPage());
                }
                return;
            }
        }

        // Control buttons
        switch (slot) {
            case 45 -> { // Previous page
                if (holder.getPage() > 0) {
                    playPageSound(player);
                    open(player, plugin, holder.getPage() - 1);
                }
            }
            case 47 -> { // Add action
                player.closeInventory();
                playClick(player);
                Component prompt = Component.text("[Keybind] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                        .append(Component.text("Click to create a new action: ", NamedTextColor.YELLOW))
                        .append(Component.text("/kbind add <name> <command>", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.suggestCommand("/kbind add "))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to paste /kbind add into chat", NamedTextColor.GRAY))));
                player.sendMessage(prompt);
            }
            case 49 -> { // Settings
                handleSettingsClick(player, event.getClick(), holder.getPage());
            }
            case 51 -> { // Reload & Sync
                playSuccessSound(player);
                plugin.getConfigManager().reload();
                plugin.getSyncSender().sendSyncToAll();
                player.sendMessage(Component.text("[Keybind] Config reloaded and synced to all players!", NamedTextColor.GREEN));
                open(player, plugin, holder.getPage());
            }
            case 53 -> { // Next page
                int totalPages = Math.max(1, (int) Math.ceil(plugin.getConfigManager().getActions().size() / (double) PAGE_SIZE));
                if (holder.getPage() < totalPages - 1) {
                    playPageSound(player);
                    open(player, plugin, holder.getPage() + 1);
                }
            }
            default -> { }
        }
    }

    private void handleSettingsClick(Player player, ClickType click, int page) {
        ConfigManager config = plugin.getConfigManager();
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            // Toggle metrics
            playChime(player);
            boolean newState = !config.isMetricsEnabled();
            config.setMetricsEnabled(newState);
            player.sendMessage(Component.text("[Keybind] Anonymous metrics (bStats) " + (newState ? "enabled." : "disabled."),
                    newState ? NamedTextColor.GREEN : NamedTextColor.RED));
            open(player, plugin, page);
        } else if (click.isRightClick()) {
            // Toggle update checker
            playChime(player);
            boolean newState = !config.isUpdateCheckerEnabled();
            config.setUpdateCheckerEnabled(newState);
            player.sendMessage(Component.text("[Keybind] Update checker " + (newState ? "enabled." : "disabled."),
                    newState ? NamedTextColor.GREEN : NamedTextColor.RED));
            open(player, plugin, page);
        } else if (click.isLeftClick()) {
            // Cycle global cooldown: 0 -> 250 -> 500 -> 1000 -> 2000 -> 0
            playChime(player);
            long current = config.getGlobalCooldown();
            long next;
            if (current < 250) next = 250;
            else if (current < 500) next = 500;
            else if (current < 1000) next = 1000;
            else if (current < 2000) next = 2000;
            else next = 0;

            config.setGlobalCooldown(next);
            player.sendMessage(Component.text("[Keybind] Global cooldown set to " + next + "ms.", NamedTextColor.GREEN));
            open(player, plugin, page);
        }
    }

    private void handleActionClick(Player player, ConfigManager.ActionConfig action, ClickType click, int page) {
        if (click == ClickType.SHIFT_RIGHT) {
            // Delete action
            playBreakSound(player);
            String error = plugin.getConfigManager().deleteAction(action.getName());
            if (error != null) {
                player.sendMessage(Component.text(error, NamedTextColor.RED));
                return;
            }
            plugin.getSyncSender().sendSyncToAll();
            player.sendMessage(Component.text("[Keybind] Deleted action: " + action.getName(), NamedTextColor.RED));
            open(player, plugin, page);
        } else if (click == ClickType.SHIFT_LEFT) {
            // Cycle per-action cooldown: 0 -> 250 -> 500 -> 1000 -> 2000 -> 5000 -> 0
            playChime(player);
            long current = action.getCooldown();
            long next;
            if (current < 250) next = 250;
            else if (current < 500) next = 500;
            else if (current < 1000) next = 1000;
            else if (current < 2000) next = 2000;
            else if (current < 5000) next = 5000;
            else next = 0;

            ConfigManager.ActionConfig updated = new ConfigManager.ActionConfig(
                    action.getName(), action.getDisplayName(), action.getCommand(),
                    action.getDefaultKey(), action.getPermission(), next, action.isConsole()
            );
            plugin.getConfigManager().saveAction(updated);
            plugin.getSyncSender().sendSyncToAll();
            player.sendMessage(Component.text("[Keybind] Set cooldown for " + action.getName() + " to " + next + "ms.", NamedTextColor.AQUA));
            open(player, plugin, page);
        } else if (click.isRightClick()) {
            // Toggle console
            playSuccessSound(player);
            ConfigManager.ActionConfig updated = new ConfigManager.ActionConfig(
                    action.getName(), action.getDisplayName(), action.getCommand(),
                    action.getDefaultKey(), action.getPermission(), action.getCooldown(),
                    !action.isConsole()
            );
            String error = plugin.getConfigManager().saveAction(updated);
            if (error != null) {
                player.sendMessage(Component.text(error, NamedTextColor.RED));
                return;
            }
            plugin.getSyncSender().sendSyncToAll();
            player.sendMessage(Component.text("[Keybind] Execution for " + action.getName() + " set to "
                    + (!action.isConsole() ? "Console." : "Player."), NamedTextColor.LIGHT_PURPLE));
            open(player, plugin, page);
        } else if (click.isLeftClick()) {
            // Trigger in-game capture for default key
            player.closeInventory();
            playClick(player);
            KeyCaptureDraft draft = new KeyCaptureDraft(
                    KeyCaptureDraft.Screen.VIEW,
                    action.getName(),
                    action.getDisplayName(),
                    action.getCommand(),
                    action.getDefaultKey(),
                    action.getPermission(),
                    String.valueOf(action.getCooldown()),
                    action.isConsole(),
                    page
            );
            plugin.getKeyCaptureService().request(player, draft);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private static void playClick(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } catch (Throwable ignored) {}
    }

    private static void playChime(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.2f);
        } catch (Throwable ignored) {}
    }

    private static void playSuccessSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.0f);
        } catch (Throwable ignored) {}
    }

    private static void playBreakSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 0.8f);
        } catch (Throwable ignored) {}
    }

    private static void playPageSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.0f);
        } catch (Throwable ignored) {}
    }
}
