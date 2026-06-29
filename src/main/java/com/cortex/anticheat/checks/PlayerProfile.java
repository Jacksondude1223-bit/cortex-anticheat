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
    private final Deque<Long> bowAimSamples = new ArrayDeque<>();
    private final Deque<Long> mlSignals = new ArrayDeque<>();
    private float lastYaw;
    private float lastPitch;
    private double lastRotationDelta;
    private long lastRotationMillis;
    private boolean hasRotation;

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
    public int recordBowAimSample() { return record(bowAimSamples); }
    public int recordMlSignal() { return record(mlSignals, 10_000L); }
    public double lastRotationDelta() { return lastRotationDelta; }
    public long lastRotationMillis() { return lastRotationMillis; }

    public void recordRotation(float yaw, float pitch) {
        if (hasRotation) {
            double yawDelta = Math.abs(wrapAngle(yaw - lastYaw));
            double pitchDelta = Math.abs(pitch - lastPitch);
            lastRotationDelta = Math.hypot(yawDelta, pitchDelta);
            lastRotationMillis = System.currentTimeMillis();
        }
        lastYaw = yaw;
        lastPitch = pitch;
        hasRotation = true;
    }

    private float wrapAngle(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    private int record(Deque<Long> samples) {
        long now = Instant.now().toEpochMilli();
        samples.addLast(now);
        while (!samples.isEmpty() && now - samples.peekFirst() > 1000) {
            samples.removeFirst();
        }
        return samples.size();
    }

    private int record(Deque<Long> samples, long windowMillis) {
        long now = Instant.now().toEpochMilli();
        samples.addLast(now);
        while (!samples.isEmpty() && now - samples.peekFirst() > windowMillis) {
            samples.removeFirst();
        }
        return samples.size();
    }
}
