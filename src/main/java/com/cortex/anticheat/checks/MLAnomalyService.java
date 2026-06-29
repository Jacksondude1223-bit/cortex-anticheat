package com.cortex.anticheat.checks;

import com.cortex.anticheat.CortexAntiCheatPlugin;

public final class MLAnomalyService {
    private final CortexAntiCheatPlugin plugin;

    public MLAnomalyService(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public MLResult evaluate(DetectionContext context, PlayerProfile profile) {
        if (!plugin.getConfig().getBoolean("ml.enabled", true)) {
            return MLResult.clean();
        }
        int recentSignals = profile.recordMlSignal();
        double score = baseScore(context, profile, recentSignals);
        double threshold = plugin.getConfig().getDouble("ml.anomaly-threshold", 0.82);
        if (score < threshold || context.check().startsWith("ML.")) {
            return MLResult.clean();
        }
        double bonus = plugin.getConfig().getDouble("ml.bonus-violation", 1.0);
        String detail = "score=" + round(score) + " signals10s=" + recentSignals + " source=" + context.check();
        return new MLResult(true, score, bonus, detail);
    }

    private double baseScore(DetectionContext context, PlayerProfile profile, int recentSignals) {
        double severityWeight = plugin.getConfig().getDouble("ml.weights.severity", 0.28);
        double vlWeight = plugin.getConfig().getDouble("ml.weights.current-vl", 0.22);
        double burstWeight = plugin.getConfig().getDouble("ml.weights.burst", 0.25);
        double checkWeight = plugin.getConfig().getDouble("ml.weights.check-risk", 0.25);
        double severityFeature = clamp(context.severity() / 2.0);
        double vlFeature = clamp(profile.violations() / plugin.getConfig().getDouble("punishments.violation-threshold", 12.0));
        double burstFeature = clamp(recentSignals / 8.0);
        double checkFeature = checkRisk(context.check());
        double linear = severityWeight * severityFeature
                + vlWeight * vlFeature
                + burstWeight * burstFeature
                + checkWeight * checkFeature;
        return sigmoid((linear - 0.5) * 5.0);
    }

    private double checkRisk(String check) {
        if (check.contains("BowAimbot") || check.contains("ESP")) return 1.0;
        if (check.contains("Reach") || check.contains("AutoClicker")) return 0.85;
        if (check.contains("Packet")) return 0.7;
        if (check.contains("Movement")) return 0.6;
        return 0.5;
    }

    private double sigmoid(double value) {
        return 1.0 / (1.0 + Math.exp(-value));
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static final class MLResult {
        private final boolean anomalous;
        private final double score;
        private final double bonusViolation;
        private final String detail;

        private MLResult(boolean anomalous, double score, double bonusViolation, String detail) {
            this.anomalous = anomalous;
            this.score = score;
            this.bonusViolation = bonusViolation;
            this.detail = detail;
        }

        public static MLResult clean() {
            return new MLResult(false, 0.0, 0.0, "");
        }

        public boolean anomalous() { return anomalous; }
        public double score() { return score; }
        public double bonusViolation() { return bonusViolation; }
        public String detail() { return detail; }
    }
}
