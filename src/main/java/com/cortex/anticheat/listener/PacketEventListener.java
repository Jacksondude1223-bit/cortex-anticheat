package com.cortex.anticheat.listener;

import com.cortex.anticheat.checks.DetectionContext;
import com.cortex.anticheat.checks.PlayerProfile;
import com.cortex.anticheat.checks.ViolationService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PacketEventListener implements Listener {
    private final ViolationService violations;
    private final double maxHorizontal;
    private final double maxVertical;
    private final int maxSwings;
    private final int maxInteracts;
    private final int maxCps;
    private final double maxReach;

    public PacketEventListener(ViolationService violations, org.bukkit.configuration.file.FileConfiguration config) {
        this.violations = violations;
        this.maxHorizontal = config.getDouble("checks.movement.max-horizontal-blocks-per-second", 12.5) / 20.0;
        this.maxVertical = config.getDouble("checks.movement.max-vertical-delta", 1.35);
        this.maxSwings = config.getInt("checks.packet.max-arm-swing-packets-per-second", 25);
        this.maxInteracts = config.getInt("checks.packet.max-interact-packets-per-second", 30);
        this.maxCps = config.getInt("checks.combat.max-cps", 18);
        this.maxReach = config.getDouble("checks.combat.max-reach", 3.45);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (exempt(player) || event.getFrom().getWorld() != event.getTo().getWorld()) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        double horizontal = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
        double vertical = to.getY() - from.getY();
        if (horizontal > maxHorizontal && !player.isGliding() && !player.isInsideVehicle()) {
            violations.flag(new DetectionContext(player, "Movement.Speed", 1.2, "h=" + round(horizontal)));
        }
        if (vertical > maxVertical && !player.isFlying() && !player.isGliding()) {
            violations.flag(new DetectionContext(player, "Movement.Vertical", 1.5, "y=" + round(vertical)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimation(PlayerAnimationEvent event) {
        PlayerProfile profile = violations.profile(event.getPlayer());
        int swings = profile.recordSwing();
        if (swings > maxSwings) {
            violations.flag(new DetectionContext(event.getPlayer(), "Packet.SwingRate", 0.8, "pps=" + swings));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        PlayerProfile profile = violations.profile(event.getPlayer());
        int interacts = profile.recordInteract();
        if (interacts > maxInteracts) {
            violations.flag(new DetectionContext(event.getPlayer(), "Packet.InteractRate", 0.8, "pps=" + interacts));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        PlayerProfile profile = violations.profile(player);
        int cps = profile.recordAttack();
        if (cps > maxCps) {
            violations.flag(new DetectionContext(player, "Combat.AutoClicker", 1.1, "cps=" + cps));
        }
        if (player.getLocation().distance(event.getEntity().getLocation()) > maxReach) {
            violations.flag(new DetectionContext(player, "Combat.Reach", 1.4, "d=" + round(player.getLocation().distance(event.getEntity().getLocation()))));
        }
    }

    private boolean exempt(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.getAllowFlight();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
