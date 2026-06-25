package com.cortex.anticheat.listener;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public final class ClientBrandListener implements PluginMessageListener {
    private final CortexAntiCheatPlugin plugin;

    public ClientBrandListener(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        register("MC|Brand");
        register("minecraft:brand");
    }

    public void stop() {
        unregister("MC|Brand");
        unregister("minecraft:brand");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!plugin.getConfig().getBoolean("client-detection.enabled", true)) return;
        String brand = readBrand(message);
        String detectedClient = detectedClient(brand);
        if (detectedClient == null) return;
        String kickMessage = plugin.getConfig().getString(
                        "client-detection.kick-message",
                        "&cCortex AntiCheat\n&7The client &e%client% &7is not allowed on this server because it is a known hacked client.")
                .replace("%client%", detectedClient);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
            }
        });
        plugin.getLogger().warning("Kicked " + player.getName() + " for disallowed client brand: " + brand);
    }

    private void register(String channel) {
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        } catch (RuntimeException ignored) {
            plugin.getLogger().fine("Client brand channel is not supported on this server version: " + channel);
        }
    }

    private void unregister(String channel) {
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
        } catch (RuntimeException ignored) {
            plugin.getLogger().fine("Client brand channel was not registered on this server version: " + channel);
        }
    }

    private String detectedClient(String brand) {
        String normalized = brand.toLowerCase(Locale.ROOT);
        List<String> blockedClients = plugin.getConfig().getStringList("client-detection.blocked-brands");
        for (String blockedClient : blockedClients) {
            if (!blockedClient.isEmpty() && normalized.contains(blockedClient.toLowerCase(Locale.ROOT))) {
                return blockedClient;
            }
        }
        return null;
    }

    private String readBrand(byte[] message) {
        if (message.length == 0) return "unknown";
        int first = message[0] & 0xFF;
        if (first > 0 && first < message.length && first < 128) {
            return new String(message, 1, Math.min(first, message.length - 1), StandardCharsets.UTF_8).trim();
        }
        return new String(message, StandardCharsets.UTF_8).replace("\u0000", "").trim();
    }
}
