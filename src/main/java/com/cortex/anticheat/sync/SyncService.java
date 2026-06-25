package com.cortex.anticheat.sync;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public final class SyncService implements PluginMessageListener {
    private final CortexAntiCheatPlugin plugin;
    private final String channel;

    public SyncService(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.channel = plugin.getConfig().getString("sync.channel", "cortex:sync");
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("sync.enabled", true)) return;
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
    }

    public void stop() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
    }

    public void publishBan(UUID uuid, String name, String ipAddress, String check) {
        if (!plugin.getConfig().getBoolean("sync.enabled", true)) return;
        Player carrier = Bukkit.getOnlinePlayers().stream().findAny().orElse(null);
        if (carrier == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("BAN");
        out.writeUTF(plugin.serverId());
        out.writeUTF(uuid.toString());
        out.writeUTF(name);
        out.writeUTF(ipAddress);
        out.writeUTF(check);
        carrier.sendPluginMessage(plugin, channel, out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!this.channel.equals(channel)) return;
        ByteArrayDataInput input = ByteStreams.newDataInput(message);
        if (!"BAN".equals(input.readUTF())) return;
        String sourceServer = input.readUTF();
        input.readUTF();
        String name = input.readUTF();
        String ipAddress = input.readUTF();
        String check = input.readUTF();
        if (plugin.serverId().equals(sourceServer)) return;
        plugin.punishmentService().recordRemoteBan(name, ipAddress, check);
        plugin.getLogger().info("Received synced Cortex ban for " + name + " from " + sourceServer + " (" + check + ")");
        plugin.discordWebhookService().logSync(name, sourceServer, check);
    }
}
