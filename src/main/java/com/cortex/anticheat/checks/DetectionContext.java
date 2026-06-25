package com.cortex.anticheat.checks;

import org.bukkit.entity.Player;

public final class DetectionContext {
    private final Player player;
    private final String check;
    private final double severity;
    private final String detail;

    public DetectionContext(Player player, String check, double severity, String detail) {
        this.player = player;
        this.check = check;
        this.severity = severity;
        this.detail = detail;
    }

    public Player player() { return player; }
    public String check() { return check; }
    public double severity() { return severity; }
    public String detail() { return detail; }
}
