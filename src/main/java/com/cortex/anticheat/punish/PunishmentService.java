package com.cortex.anticheat.punish;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

public final class PunishmentService {
    private final CortexAntiCheatPlugin plugin;
    private final Deque<Long> bans = new ArrayDeque<>();

    public PunishmentService(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void punish(Player player, String check) {
        bans.addLast(Instant.now().toEpochMilli());
        plugin.syncService().publishBan(player.getUniqueId(), player.getName(), check);
        String command = plugin.getConfig().getString("punishments.command", "ban %player% Cortex AntiCheat: %check%")
                .replace("%player%", player.getName())
                .replace("%check%", check);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    public void recordRemoteBan() {
        bans.addLast(Instant.now().toEpochMilli());
    }

    public int bansLast24Hours() {
        long cutoff = Instant.now().minusSeconds(86_400).toEpochMilli();
        while (!bans.isEmpty() && bans.peekFirst() < cutoff) {
            bans.removeFirst();
        }
        return bans.size();
    }
}
