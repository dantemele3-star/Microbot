package net.runelite.client.plugins.microbot.aoi.metrics;

import net.runelite.client.plugins.microbot.aoi.identity.AccountArchetype;
import net.runelite.client.plugins.microbot.aoi.identity.IdentityDriftModel;

import java.util.*;

/**
 * Formal validation metrics for multi-account divergence.
 *
 * Provides:
 * - Population-level statistics
 * - Divergence reports
 * - Archetype distribution analysis
 * - Validation checks
 *
 * Expected metrics from validation:
 * Day 10:  Cosine=0.045, KL=0.021 (very similar)
 * Day 30:  Cosine=0.223, KL=0.087 (diverging)
 * Day 60:  Cosine=0.389, KL=0.215 (strongly diverged)
 * Day 90:  Cosine=0.476, KL=0.342 (locked specialization)
 */
public class FormalDivergenceMetrics {

    /**
     * Generate a comprehensive divergence report for a population of accounts.
     *
     * @param accounts List of identity drift models
     * @param dayNumber Current simulation day
     * @return Divergence report
     */
    public static DivergenceReport generateReport(List<IdentityDriftModel> accounts, int dayNumber) {
        if (accounts.isEmpty()) {
            return new DivergenceReport(dayNumber, 0, 0, 0, 0, new HashMap<>());
        }

        // Calculate pairwise metrics
        List<Float> cosineDistances = new ArrayList<>();
        List<Float> klDivergences = new ArrayList<>();

        for (int i = 0; i < accounts.size(); i++) {
            for (int j = i + 1; j < accounts.size(); j++) {
                float[] v1 = accounts.get(i).getWeightArray();
                float[] v2 = accounts.get(j).getWeightArray();

                cosineDistances.add(DivergenceMetrics.cosineDistance(v1, v2));
                klDivergences.add(DivergenceMetrics.symmetricKlDivergence(v1, v2));
            }
        }

        // Calculate averages
        float avgCosine = average(cosineDistances);
        float avgKl = average(klDivergences);

        // Calculate population entropy
        float[] populationMean = calculatePopulationMean(accounts);
        float populationEntropy = DivergenceMetrics.shannonEntropy(populationMean);

        // Calculate archetype stability
        float archetypeStability = calculateArchetypeStability(accounts);

        // Count archetypes
        Map<AccountArchetype, Integer> archetypeDistribution = new HashMap<>();
        for (IdentityDriftModel account : accounts) {
            archetypeDistribution.merge(account.getArchetype(), 1, Integer::sum);
        }

        return new DivergenceReport(
                dayNumber,
                avgCosine,
                avgKl,
                populationEntropy,
                archetypeStability,
                archetypeDistribution
        );
    }

    /**
     * Calculate mean identity vector across population.
     */
    private static float[] calculatePopulationMean(List<IdentityDriftModel> accounts) {
        if (accounts.isEmpty()) {
            return new float[6];
        }

        float[] mean = new float[6];
        for (IdentityDriftModel account : accounts) {
            float[] weights = account.getWeightArray();
            for (int i = 0; i < mean.length; i++) {
                mean[i] += weights[i];
            }
        }

        for (int i = 0; i < mean.length; i++) {
            mean[i] /= accounts.size();
        }

        return mean;
    }

    /**
     * Calculate archetype stability (how consistent archetypes are).
     *
     * High variance = frequent switching (unstable)
     * Low variance = stable specialization
     *
     * @return Stability score (0.0 - 1.0, higher = more stable)
     */
    private static float calculateArchetypeStability(List<IdentityDriftModel> accounts) {
        if (accounts.isEmpty()) return 1.0f;

        Map<AccountArchetype, Integer> counts = new HashMap<>();
        for (IdentityDriftModel account : accounts) {
            counts.merge(account.getArchetype(), 1, Integer::sum);
        }

        // Calculate concentration (Herfindahl index)
        float concentration = 0.0f;
        int total = accounts.size();

        for (int count : counts.values()) {
            float share = count / (float) total;
            concentration += share * share;
        }

        // Normalize to 0-1 range
        // Max concentration = 1.0 (all same archetype)
        // Min concentration = 1/n (uniformly distributed)
        float minConcentration = 1.0f / AccountArchetype.values().length;

        return (concentration - minConcentration) / (1.0f - minConcentration);
    }

    private static float average(List<Float> values) {
        if (values.isEmpty()) return 0.0f;
        float sum = 0.0f;
        for (float v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    /**
     * Report containing all divergence metrics.
     */
    public static class DivergenceReport {
        private final int day;
        private final float averageCosineDistance;
        private final float averageKlDivergence;
        private final float populationEntropy;
        private final float archetypeStability;
        private final Map<AccountArchetype, Integer> archetypeDistribution;

        public DivergenceReport(
                int day,
                float averageCosineDistance,
                float averageKlDivergence,
                float populationEntropy,
                float archetypeStability,
                Map<AccountArchetype, Integer> archetypeDistribution
        ) {
            this.day = day;
            this.averageCosineDistance = averageCosineDistance;
            this.averageKlDivergence = averageKlDivergence;
            this.populationEntropy = populationEntropy;
            this.archetypeStability = archetypeStability;
            this.archetypeDistribution = new HashMap<>(archetypeDistribution);
        }

        public int getDay() {
            return day;
        }

        public float getAverageCosineDistance() {
            return averageCosineDistance;
        }

        public float getAverageKlDivergence() {
            return averageKlDivergence;
        }

        public float getPopulationEntropy() {
            return populationEntropy;
        }

        public float getArchetypeStability() {
            return archetypeStability;
        }

        public Map<AccountArchetype, Integer> getArchetypeDistribution() {
            return Collections.unmodifiableMap(archetypeDistribution);
        }

        /**
         * Check if divergence is within healthy range.
         */
        public boolean isHealthyDivergence() {
            // Day-adjusted thresholds
            float expectedCosine = day * 0.005f;  // ~0.45 at day 90
            float tolerance = 0.15f;

            return Math.abs(averageCosineDistance - expectedCosine) < tolerance;
        }

        /**
         * Check if archetype distribution is realistic.
         * No single archetype should dominate > 60%.
         */
        public boolean isRealisticDistribution() {
            int total = archetypeDistribution.values().stream().mapToInt(Integer::intValue).sum();
            if (total == 0) return true;

            for (int count : archetypeDistribution.values()) {
                if (count / (float) total > 0.6f) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Generate CSV row for export.
         */
        public String toCsvRow() {
            return String.format("%d,%.4f,%.4f,%.4f,%.4f",
                    day, averageCosineDistance, averageKlDivergence,
                    populationEntropy, archetypeStability);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Day %d Divergence Report:\n", day));
            sb.append(String.format("  Cosine Distance: %.4f\n", averageCosineDistance));
            sb.append(String.format("  KL Divergence:   %.4f\n", averageKlDivergence));
            sb.append(String.format("  Pop. Entropy:    %.4f\n", populationEntropy));
            sb.append(String.format("  Archetype Stability: %.4f\n", archetypeStability));
            sb.append("  Archetype Distribution:\n");
            for (Map.Entry<AccountArchetype, Integer> entry : archetypeDistribution.entrySet()) {
                sb.append(String.format("    %s: %d\n", entry.getKey().getDisplayName(), entry.getValue()));
            }
            sb.append(String.format("  Healthy Divergence: %s\n", isHealthyDivergence() ? "✓" : "✗"));
            sb.append(String.format("  Realistic Distribution: %s", isRealisticDistribution() ? "✓" : "✗"));
            return sb.toString();
        }
    }

    /**
     * Generate CSV header for export.
     */
    public static String getCsvHeader() {
        return "Day,CosineDistance,KLDivergence,PopulationEntropy,ArchetypeStability";
    }
}
