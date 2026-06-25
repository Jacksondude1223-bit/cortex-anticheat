package com.cortex.anticheat.punish;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class PunishmentService {
    private final CortexAntiCheatPlugin plugin;
    private final Deque<Long> bans = new ArrayDeque<>();

    public PunishmentService(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void punish(Player player, String check) {
        bans.addLast(Instant.now().toEpochMilli());
        Optional<String> address = playerAddress(player);
        plugin.syncService().publishBan(player.getUniqueId(), player.getName(), address.orElse(""), check);
        dispatchConfiguredCommand("punishments.command", player.getName(), address.orElse(""), check);
        if (plugin.getConfig().getBoolean("punishments.ip-ban.enabled", true)) {
            if (address.isPresent()) {
                dispatchConfiguredCommand("punishments.ip-ban.command", player.getName(), address.get(), check);
            } else {
                plugin.getLogger().warning("Cannot IP-ban " + player.getName() + " because no socket address is available.");
            }
        }
    }

    public void recordRemoteBan(String playerName, String ipAddress, String check) {
        bans.addLast(Instant.now().toEpochMilli());
        if (ipAddress == null || ipAddress.isBlank()) return;
        if (!plugin.getConfig().getBoolean("punishments.ip-ban.execute-on-remote-sync", true)) return;
        dispatchConfiguredCommand("punishments.ip-ban.command", playerName, ipAddress, check);
    }

    public int bansLast24Hours() {
        long cutoff = Instant.now().minusSeconds(86_400).toEpochMilli();
        while (!bans.isEmpty() && bans.peekFirst() < cutoff) {
            bans.removeFirst();
        }
        return bans.size();
    }

    private Optional<String> playerAddress(Player player) {
        InetSocketAddress socketAddress = player.getAddress();
        if (socketAddress == null || socketAddress.getAddress() == null) {
            return Optional.empty();
        }
        return Optional.of(socketAddress.getAddress().getHostAddress());
    }

    private void dispatchConfiguredCommand(String path, String playerName, String ipAddress, String check) {
        String command = plugin.getConfig().getString(path, "")
                .replace("%player%", playerName)
                .replace("%ip%", ipAddress)
                .replace("%check%", check);
        if (command.isBlank()) return;
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
}
