package com.keybind.plugin.update;

import com.keybind.plugin.KeybindPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker implements Listener {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/iM5LB/Keybind/releases/latest";
    private static final String FALLBACK_URL = "https://github.com/iM5LB/Keybind/releases";
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final KeybindPlugin plugin;
    private final String currentVersion;
    private volatile String latestVersion = null;
    private volatile String downloadUrl = FALLBACK_URL;
    private final AtomicBoolean updateAvailable = new AtomicBoolean(false);

    public UpdateChecker(KeybindPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdates(boolean silentIfUpToDate) {
        if (!plugin.getConfigManager().isUpdateCheckerEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_API_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Keybind-Plugin/" + currentVersion);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    if (!silentIfUpToDate) {
                        plugin.getLogger().warning("Could not check for updates (HTTP " + conn.getResponseCode() + ").");
                    }
                    return;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                String json = response.toString();
                Matcher tagMatcher = TAG_PATTERN.matcher(json);
                if (tagMatcher.find()) {
                    String tag = tagMatcher.group(1).trim();
                    this.latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
                }

                Matcher urlMatcher = URL_PATTERN.matcher(json);
                if (urlMatcher.find()) {
                    this.downloadUrl = urlMatcher.group(1).trim();
                }

                if (latestVersion != null && isNewerVersion(latestVersion, currentVersion)) {
                    updateAvailable.set(true);
                    plugin.getLogger().info("A new version of Keybind is available: v" + latestVersion
                            + " (Current: v" + currentVersion + ")");
                    plugin.getLogger().info("Download it here: " + downloadUrl);
                } else {
                    updateAvailable.set(false);
                    if (!silentIfUpToDate) {
                        plugin.getLogger().info("Keybind is up to date (v" + currentVersion + ").");
                    }
                }
            } catch (Exception e) {
                if (!silentIfUpToDate) {
                    plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
                }
            }
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable.get();
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void notifyAdmin(Player player) {
        if (!updateAvailable.get() || !player.hasPermission("keybind.admin")) {
            return;
        }

        Component message = Component.text("[Keybind] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("A new update is available: ", NamedTextColor.YELLOW))
                .append(Component.text("v" + latestVersion, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" (Current: v" + currentVersion + ") ", NamedTextColor.GRAY))
                .append(Component.text("[Click to Download]", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(downloadUrl))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open releases in your browser", NamedTextColor.GRAY))));

        player.sendMessage(message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("keybind.admin") && updateAvailable.get()) {
            // Slight delay so chat loads properly on player join
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    notifyAdmin(player);
                }
            }, 40L);
        }
    }

    private static boolean isNewerVersion(String latest, String current) {
        String[] lParts = latest.split("[.-]");
        String[] cParts = current.split("[.-]");
        int len = Math.max(lParts.length, cParts.length);
        for (int i = 0; i < len; i++) {
            int lNum = i < lParts.length ? parseNumber(lParts[i]) : 0;
            int cNum = i < cParts.length ? parseNumber(cParts[i]) : 0;
            if (lNum > cNum) return true;
            if (lNum < cNum) return false;
        }
        return false;
    }

    private static int parseNumber(String part) {
        try {
            return Integer.parseInt(part.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
