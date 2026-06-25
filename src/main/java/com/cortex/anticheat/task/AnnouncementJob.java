package com.cortex.anticheat.task;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class AnnouncementJob implements Runnable {
    private final CortexAntiCheatPlugin plugin;

    public AnnouncementJob(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int count = plugin.punishmentService().bansLast24Hours();
        String template = plugin.getConfig().getString("announcements.template", "&cCortex &7has banned &e%count% &7players over the past 24 hours.");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', template.replace("%count%", String.valueOf(count))));
    }
}
