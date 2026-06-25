package com.cortex.anticheat.proxy;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class CortexBungeePlugin extends Plugin implements Listener {
    private static final String CHANNEL = "cortex:sync";
    private final Deque<Long> bans = new ArrayDeque<Long>();

    @Override
    public void onEnable() {
        ProxyServer.getInstance().registerChannel(CHANNEL);
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
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
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
