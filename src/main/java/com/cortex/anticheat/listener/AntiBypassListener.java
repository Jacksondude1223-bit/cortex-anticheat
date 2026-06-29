package com.cortex.anticheat.listener;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import com.cortex.anticheat.checks.DetectionContext;
import com.cortex.anticheat.checks.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class AntiBypassListener implements PluginMessageListener {
    private static final String[] CHANNELS = new String[] {
            "REGISTER", "minecraft:register", "FML|HS", "FML", "fabric:brand"
    };

    private final CortexAntiCheatPlugin plugin;
    private final ViolationService violations;
    private final Set<UUID> recentlyHandled = new HashSet<UUID>();

    public AntiBypassListener(CortexAntiCheatPlugin plugin, ViolationService violations) {
        this.plugin = plugin;
        this.violations = violations;
    }

    public void start() {
        for (String channel : CHANNELS) {
            register(channel);
        }
    }

    public void stop() {
        for (String channel : CHANNELS) {
            unregister(channel);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!plugin.getConfig().getBoolean("anti-bypass.enabled", true)) return;
        String payload = readPayload(message);
        String matchedToken = matchedToken(channel + " " + payload);
        if (matchedToken == null) return;
        if (!recentlyHandled.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                recentlyHandled.remove(player.getUniqueId());
            }
        }, 100L);

        violations.flag(new DetectionContext(player, "Bypass.ModMenu", 2.0,
                "token=" + matchedToken + " channel=" + channel));
        if (plugin.getConfig().getBoolean("anti-bypass.kick-on-detection", true)) {
            String kickMessage = plugin.getConfig().getString(
                            "anti-bypass.kick-message",
                            "&cCortex AntiCheat\n&7A mod-menu anti-cheat bypass module was detected and is not allowed.")
                    .replace("%token%", matchedToken);
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
                }
            });
            plugin.discordWebhookService().logClientKick(player.getName(), "anti-bypass:" + matchedToken);
        }
    }

    private void register(String channel) {
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        } catch (RuntimeException ignored) {
            plugin.getLogger().fine("Anti-bypass channel is not supported on this server version: " + channel);
        }
    }

    private void unregister(String channel) {
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
        } catch (RuntimeException ignored) {
            plugin.getLogger().fine("Anti-bypass channel was not registered on this server version: " + channel);
        }
    }

    private String matchedToken(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        List<String> tokens = plugin.getConfig().getStringList("anti-bypass.suspicious-tokens");
        for (String token : tokens) {
            if (!token.isEmpty() && normalized.contains(token.toLowerCase(Locale.ROOT))) {
                return token;
            }
        }
        return null;
    }

    private String readPayload(byte[] message) {
        if (message.length == 0) return "";
        String payload = new String(message, StandardCharsets.UTF_8);
        return payload.replace('\u0000', ' ').trim();
    }
}
