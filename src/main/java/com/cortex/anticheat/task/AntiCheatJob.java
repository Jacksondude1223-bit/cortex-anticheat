package com.cortex.anticheat.task;

import com.cortex.anticheat.checks.ViolationService;

public final class AntiCheatJob implements Runnable {
    private final ViolationService violations;

    public AntiCheatJob(ViolationService violations) {
        this.violations = violations;
    }

    @Override
    public void run() {
        violations.decayAll();
    }
}
