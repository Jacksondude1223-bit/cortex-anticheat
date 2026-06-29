package com.cortex.anticheat.discord;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhookService {
    private final CortexAntiCheatPlugin plugin;

    public DiscordWebhookService(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void logViolation(String playerName, String check, double violations, String detail) {
        send("Violation", "Player `" + playerName + "` failed `" + check + "` (VL " + round(violations) + "). " + detail, 16753920);
    }

    public void logPunishment(String playerName, String check, String ipAddress) {
        String ipText = ipAddress == null || ipAddress.isEmpty() ? "not available" : ipAddress;
        send("Punishment", "Player `" + playerName + "` was punished for `" + check + "`. IP: `" + ipText + "`", 15158332);
    }

    public void logClientKick(String playerName, String clientBrand) {
        send("Client blocked", "Player `" + playerName + "` was kicked for using blocked client `" + clientBrand + "`.", 15105570);
    }

    public void logSync(String playerName, String sourceServer, String check) {
        send("Synced ban", "Received synced ban for `" + playerName + "` from `" + sourceServer + "` (`" + check + "`).", 3447003);
    }

    private void send(String title, String description, int color) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty() || webhookUrl.contains("paste-webhook")) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                post(webhookUrl, payload(title, description, color));
            }
        });
    }

    private void post(String webhookUrl, String payload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(webhookUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning("Discord webhook returned HTTP " + responseCode);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to send Discord webhook: " + exception.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String payload(String title, String description, int color) {
        String username = plugin.getConfig().getString("discord.username", "Cortex AntiCheat");
        return "{"
                + "\"username\":\"" + escape(username) + "\"," 
                + "\"embeds\":[{"
                + "\"title\":\"" + escape(title) + "\"," 
                + "\"description\":\"" + escape(description) + "\"," 
                + "\"color\":" + color
                + "}]}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String round(double value) {
        return String.valueOf(Math.round(value * 10.0) / 10.0);
    }
}
