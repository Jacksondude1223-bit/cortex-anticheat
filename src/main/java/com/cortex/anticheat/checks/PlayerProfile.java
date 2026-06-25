package com.cortex.anticheat.checks;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uuid;
    private double violations;
    private long lastDecayMillis;
    private final Deque<Long> swings = new ArrayDeque<>();
    private final Deque<Long> interacts = new ArrayDeque<>();
    private final Deque<Long> attacks = new ArrayDeque<>();
    private final Deque<Long> hiddenAimSamples = new ArrayDeque<>();

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.lastDecayMillis = System.currentTimeMillis();
    }

    public UUID uuid() { return uuid; }
    public double violations() { return violations; }
    public void addViolation(double amount) { violations += amount; }
    public void decay(double amount) { violations = Math.max(0, violations - amount); lastDecayMillis = System.currentTimeMillis(); }
    public long lastDecayMillis() { return lastDecayMillis; }

    public int recordSwing() { return record(swings); }
    public int recordInteract() { return record(interacts); }
    public int recordAttack() { return record(attacks); }
    public int recordHiddenAimSample() { return record(hiddenAimSamples); }

    private int record(Deque<Long> samples) {
        long now = Instant.now().toEpochMilli();
        samples.addLast(now);
        while (!samples.isEmpty() && now - samples.peekFirst() > 1000) {
            samples.removeFirst();
        }
        return samples.size();
    }
}
