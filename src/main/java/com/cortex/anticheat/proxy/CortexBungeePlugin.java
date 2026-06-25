package com.cortex.anticheat.proxy;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class CortexBungeePlugin extends Plugin implements Listener {
    private static final String CHANNEL = "cortex:sync";
    private static final String LEGACY_BRAND_CHANNEL = "MC|Brand";
    private static final String MODERN_BRAND_CHANNEL = "minecraft:brand";
    private static final List<String> BLOCKED_CLIENTS = Arrays.asList(
            "wurst", "meteor", "impact", "aristois", "liquidbounce", "sigma",
            "future", "rusherhack", "bleachhack", "inertia", "kami", "salhack", "pyro", "konas");
    private final Deque<Long> bans = new ArrayDeque<Long>();

    @Override
    public void onEnable() {
        ProxyServer.getInstance().registerChannel(CHANNEL);
        ProxyServer.getInstance().registerChannel(LEGACY_BRAND_CHANNEL);
        ProxyServer.getInstance().registerChannel(MODERN_BRAND_CHANNEL);
        ProxyServer.getInstance().getPluginManager().registerListener(this, this);
        ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                announceBans();
            }
        }, 1, 1, TimeUnit.HOURS);
        getLogger().info("Cortex Bungee sync bridge enabled.");
    }

    @Override
    public void onDisable() {
        ProxyServer.getInstance().unregisterChannel(CHANNEL);
        ProxyServer.getInstance().unregisterChannel(LEGACY_BRAND_CHANNEL);
        ProxyServer.getInstance().unregisterChannel(MODERN_BRAND_CHANNEL);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (LEGACY_BRAND_CHANNEL.equals(event.getTag()) || MODERN_BRAND_CHANNEL.equals(event.getTag())) {
            handleBrand(event);
            return;
        }
        if (!CHANNEL.equals(event.getTag())) return;
        ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
        if (!"BAN".equals(input.readUTF())) return;
        String sourceServer = input.readUTF();
        String uuid = input.readUTF();
        String name = input.readUTF();
        String ipAddress = input.readUTF();
        String check = input.readUTF();
        bans.addLast(Instant.now().toEpochMilli());
        forwardBan(sourceServer, uuid, name, ipAddress, check);
    }

    private void handleBrand(PluginMessageEvent event) {
        ProxiedPlayer player = playerConnection(event);
        if (player == null) return;
        String brand = readBrand(event.getData());
        String detectedClient = detectedClient(brand);
        if (detectedClient == null) return;
        player.disconnect(ChatColor.RED + "Cortex AntiCheat\n" + ChatColor.GRAY + "The client "
                + ChatColor.YELLOW + detectedClient + ChatColor.GRAY
                + " is not allowed on this server because it is a known hacked client.");
        getLogger().warning("Kicked " + player.getName() + " for disallowed client brand: " + brand);
    }

    private ProxiedPlayer playerConnection(PluginMessageEvent event) {
        Connection sender = event.getSender();
        if (sender instanceof ProxiedPlayer) {
            return (ProxiedPlayer) sender;
        }
        Connection receiver = event.getReceiver();
        if (receiver instanceof ProxiedPlayer) {
            return (ProxiedPlayer) receiver;
        }
        return null;
    }

    private String detectedClient(String brand) {
        String normalized = brand.toLowerCase(Locale.ROOT);
        for (String blockedClient : BLOCKED_CLIENTS) {
            if (normalized.contains(blockedClient.toLowerCase(Locale.ROOT))) {
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

    private void forwardBan(String sourceServer, String uuid, String name, String ipAddress, String check) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("BAN");
        output.writeUTF(sourceServer);
        output.writeUTF(uuid);
        output.writeUTF(name);
        output.writeUTF(ipAddress);
        output.writeUTF(check);
        byte[] payload = output.toByteArray();
        for (Map.Entry<String, ServerInfo> entry : ProxyServer.getInstance().getServers().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(sourceServer)) continue;
            entry.getValue().sendData(CHANNEL, payload, true);
        }
    }

    private void announceBans() {
        int count = bansLast24Hours();
        String message = ChatColor.RED + "Cortex " + ChatColor.GRAY + "has banned "
                + ChatColor.YELLOW + count + ChatColor.GRAY + " players over the past 24 hours.";
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            player.sendMessage(message);
        }
    }

    private int bansLast24Hours() {
        long cutoff = Instant.now().minusSeconds(86_400).toEpochMilli();
        while (!bans.isEmpty() && bans.peekFirst() < cutoff) {
            bans.removeFirst();
        }
        return bans.size();
    }
}
