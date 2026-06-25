package com.cortex.anticheat;

import com.cortex.anticheat.checks.ViolationService;
import com.cortex.anticheat.command.CortexCommand;
import com.cortex.anticheat.discord.DiscordWebhookService;
import com.cortex.anticheat.listener.ClientBrandListener;
import com.cortex.anticheat.listener.PacketEventListener;
import com.cortex.anticheat.punish.PunishmentService;
import com.cortex.anticheat.sync.SyncService;
import com.cortex.anticheat.task.AnnouncementJob;
import com.cortex.anticheat.task.AntiCheatJob;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CortexAntiCheatPlugin extends JavaPlugin {
    private PunishmentService punishmentService;
    private SyncService syncService;
    private ViolationService violationService;
    private ClientBrandListener clientBrandListener;
    private DiscordWebhookService discordWebhookService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.punishmentService = new PunishmentService(this);
        this.syncService = new SyncService(this);
        this.violationService = new ViolationService(this, punishmentService);
        this.clientBrandListener = new ClientBrandListener(this);
        this.discordWebhookService = new DiscordWebhookService(this);

        syncService.start();
        clientBrandListener.start();
        Bukkit.getPluginManager().registerEvents(new PacketEventListener(violationService, getConfig()), this);
        getCommand("cortex").setExecutor(new CortexCommand(this));

        long decayInterval = getConfig().getLong("job.decay-interval-ticks", 200L);
        Bukkit.getScheduler().runTaskTimer(this, new AntiCheatJob(violationService), decayInterval, decayInterval);
        if (getConfig().getBoolean("announcements.enabled", true)) {
            long interval = getConfig().getLong("announcements.interval-minutes", 60L) * 60L * 20L;
            Bukkit.getScheduler().runTaskTimer(this, new AnnouncementJob(this), interval, interval);
        }
        getLogger().info("Cortex anti-cheat enabled for " + serverRole() + " server " + serverId());
    }

    @Override
    public void onDisable() {
        if (syncService != null) syncService.stop();
        if (clientBrandListener != null) clientBrandListener.stop();
    }

    public PunishmentService punishmentService() { return punishmentService; }
    public SyncService syncService() { return syncService; }
    public DiscordWebhookService discordWebhookService() { return discordWebhookService; }
    public String serverId() { return getConfig().getString("server-id", "unknown"); }
    public String serverRole() { return getConfig().getString("server-role", "SMP"); }
}
