package com.cortex.anticheat.checks;

import org.bukkit.entity.Player;

public record DetectionContext(Player player, String check, double severity, String detail) {
}
