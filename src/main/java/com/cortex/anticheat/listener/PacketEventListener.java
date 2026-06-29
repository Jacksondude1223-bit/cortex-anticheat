package com.cortex.anticheat.listener;

import com.cortex.anticheat.checks.DetectionContext;
import com.cortex.anticheat.checks.PlayerProfile;
import com.cortex.anticheat.checks.ViolationService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class PacketEventListener implements Listener {
    private final ViolationService violations;
    private final double maxHorizontal;
    private final double maxVertical;
    private final int maxSwings;
    private final int maxInteracts;
    private final int maxCps;
    private final double maxReach;
    private final int maxHiddenAimSamples;
    private final double espMaxDistance;
    private final double espMinimumDot;
    private final double bowAimbotMinimumDot;
    private final double bowAimbotMaxDistance;
    private final double bowAimbotMinSnap;
    private final int bowAimbotMaxSamples;

    public PacketEventListener(ViolationService violations, org.bukkit.configuration.file.FileConfiguration config) {
        this.violations = violations;
        this.maxHorizontal = config.getDouble("checks.movement.max-horizontal-blocks-per-second", 12.5) / 20.0;
        this.maxVertical = config.getDouble("checks.movement.max-vertical-delta", 1.35);
        this.maxSwings = config.getInt("checks.packet.max-arm-swing-packets-per-second", 25);
        this.maxInteracts = config.getInt("checks.packet.max-interact-packets-per-second", 30);
        this.maxCps = config.getInt("checks.combat.max-cps", 18);
        this.maxReach = config.getDouble("checks.combat.max-reach", 3.45);
        this.maxHiddenAimSamples = config.getInt("checks.esp.max-hidden-aim-samples-per-second", 8);
        this.espMaxDistance = config.getDouble("checks.esp.max-distance", 32.0);
        double aimAngle = config.getDouble("checks.esp.aim-angle-degrees", 4.0);
        this.espMinimumDot = Math.cos(Math.toRadians(aimAngle));
        double bowAngle = config.getDouble("checks.bow-aimbot.max-angle-degrees", 1.5);
        this.bowAimbotMinimumDot = Math.cos(Math.toRadians(bowAngle));
        this.bowAimbotMaxDistance = config.getDouble("checks.bow-aimbot.max-distance", 48.0);
        this.bowAimbotMinSnap = config.getDouble("checks.bow-aimbot.min-snap-degrees", 25.0);
        this.bowAimbotMaxSamples = config.getInt("checks.bow-aimbot.max-samples-per-second", 2);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (exempt(player) || event.getFrom().getWorld() != event.getTo().getWorld()) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        violations.profile(player).recordRotation(to.getYaw(), to.getPitch());
        double horizontal = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
        double vertical = to.getY() - from.getY();
        if (horizontal > maxHorizontal && !isGliding(player) && !player.isInsideVehicle()) {
            violations.flag(new DetectionContext(player, "Movement.Speed", 1.2, "h=" + round(horizontal)));
        }
        if (vertical > maxVertical && !player.isFlying() && !isGliding(player)) {
            violations.flag(new DetectionContext(player, "Movement.Vertical", 1.5, "y=" + round(vertical)));
        }
        checkEspTrace(player, from, to);
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
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        PlayerProfile profile = violations.profile(player);
        int cps = profile.recordAttack();
        if (cps > maxCps) {
            violations.flag(new DetectionContext(player, "Combat.AutoClicker", 1.1, "cps=" + cps));
        }
        if (player.getLocation().distance(event.getEntity().getLocation()) > maxReach) {
            violations.flag(new DetectionContext(player, "Combat.Reach", 1.4, "d=" + round(player.getLocation().distance(event.getEntity().getLocation()))));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player)) return;
        Player player = (Player) shooter;
        if (exempt(player)) return;
        PlayerProfile profile = violations.profile(player);
        if (System.currentTimeMillis() - profile.lastRotationMillis() > 350L) return;
        if (profile.lastRotationDelta() < bowAimbotMinSnap) return;
        Player target = bowAimbotTarget(player, event.getEntity().getVelocity());
        if (target == null) return;
        int samples = profile.recordBowAimSample();
        if (samples > bowAimbotMaxSamples) {
            violations.flag(new DetectionContext(player, "Combat.BowAimbot", 1.3,
                    "target=" + target.getName() + " snap=" + round(profile.lastRotationDelta()) + " samples=" + samples));
        }
    }

    private Player bowAimbotTarget(Player player, Vector projectileVelocity) {
        if (projectileVelocity.lengthSquared() <= 0.0) return null;
        Vector projectileDirection = projectileVelocity.clone().normalize();
        Location eye = player.getEyeLocation();
        Player best = null;
        double bestDot = bowAimbotMinimumDot;
        for (Entity entity : player.getNearbyEntities(bowAimbotMaxDistance, bowAimbotMaxDistance, bowAimbotMaxDistance)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;
            if (target.equals(player) || exempt(target) || !player.hasLineOfSight(target)) continue;
            Vector offset = target.getEyeLocation().toVector().subtract(eye.toVector());
            double distance = offset.length();
            if (distance <= 0.01 || distance > bowAimbotMaxDistance) continue;
            double dot = projectileDirection.dot(offset.normalize());
            if (dot > bestDot) {
                bestDot = dot;
                best = target;
            }
        }
        return best;
    }

    private void checkEspTrace(Player player, Location from, Location to) {
        if (Math.abs(from.getYaw() - to.getYaw()) < 0.01 && Math.abs(from.getPitch() - to.getPitch()) < 0.01) return;
        Player target = hiddenAimedTarget(player);
        if (target == null) return;
        PlayerProfile profile = violations.profile(player);
        int samples = profile.recordHiddenAimSample();
        if (samples > maxHiddenAimSamples) {
            violations.flag(new DetectionContext(player, "Vision.ESP", 0.9, "target=" + target.getName() + " samples=" + samples));
        }
    }

    private Player hiddenAimedTarget(Player player) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location eye = player.getEyeLocation();
        Player best = null;
        double bestDot = espMinimumDot;
        for (Entity entity : player.getNearbyEntities(espMaxDistance, espMaxDistance, espMaxDistance)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;
            if (target.equals(player) || exempt(target) || player.hasLineOfSight(target)) continue;
            Vector offset = target.getEyeLocation().toVector().subtract(eye.toVector());
            double distance = offset.length();
            if (distance <= 0.01 || distance > espMaxDistance) continue;
            double dot = direction.dot(offset.normalize());
            if (dot > bestDot) {
                bestDot = dot;
                best = target;
            }
        }
        return best;
    }

    private boolean exempt(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || isSpectator(player) || player.getAllowFlight();
    }

    private boolean isSpectator(Player player) {
        return "SPECTATOR".equals(player.getGameMode().name());
    }

    private boolean isGliding(Player player) {
        try {
            Object result = player.getClass().getMethod("isGliding").invoke(player);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
