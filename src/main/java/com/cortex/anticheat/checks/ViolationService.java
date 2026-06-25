package com.cortex.anticheat.checks;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import com.cortex.anticheat.punish.PunishmentService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ViolationService {
    private final CortexAntiCheatPlugin plugin;
    private final PunishmentService punishmentService;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public ViolationService(CortexAntiCheatPlugin plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
    }

    public PlayerProfile profile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), PlayerProfile::new);
    }

    public void flag(DetectionContext context) {
        PlayerProfile profile = profile(context.player());
        profile.addViolation(context.severity());
        String alert = ChatColor.RED + "[Cortex] " + ChatColor.YELLOW + context.player().getName()
                + ChatColor.GRAY + " failed " + ChatColor.WHITE + context.check()
                + ChatColor.GRAY + " (VL " + String.format("%.1f", profile.violations()) + ") "
                + ChatColor.DARK_GRAY + context.detail();
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("cortex.alerts"))
                .forEach(player -> player.sendMessage(alert));
        plugin.getLogger().warning(ChatColor.stripColor(alert));

        double threshold = plugin.getConfig().getDouble("punishments.violation-threshold", 12.0);
        if (profile.violations() >= threshold) {
            punishmentService.punish(context.player(), context.check());
            profile.decay(profile.violations());
        }
    }

    public void decayAll() {
        profiles.values().forEach(profile -> profile.decay(0.35));
    }
}
