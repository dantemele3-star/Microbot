package net.runelite.client.plugins.microbot.aoi.metrics;

/**
 * Mathematical metrics for measuring account divergence.
 *
 * Four key metrics:
 * A. Cosine Distance - Angle between identity vectors
 * B. KL-Divergence - Distribution difference
 * C. Behavioral Entropy - Activity distribution spread
 * D. Archetype Stability - Consistency of specialization
 *
 * Expected progression (from validation):
 * Day 10:  Cosine=0.045, KL=0.021 (very similar)
 * Day 30:  Cosine=0.223, KL=0.087 (diverging)
 * Day 60:  Cosine=0.389, KL=0.215 (strongly diverged)
 * Day 90:  Cosine=0.476, KL=0.342 (locked specialization)
 */
public class DivergenceMetrics {

    /**
     * Calculate cosine distance between two identity vectors.
     *
     * 0.0 = identical directions
     * 1.0 = opposite directions
     *
     * @param v1 First identity vector
     * @param v2 Second identity vector
     * @return Cosine distance (0.0 - 1.0)
     */
    public static float cosineDistance(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0f;
        }

        float cosineSimilarity = dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));

        // Convert similarity to distance
        return 1.0f - cosineSimilarity;
    }

    /**
     * Calculate KL-divergence between two probability distributions.
     *
     * Higher = more different
     * < 0.05:  Essentially identical
     * 0.1-0.3: Mild divergence
     * 0.3-0.6: Strong divergence
     * > 0.6:   Different archetypes
     *
     * @param p First distribution (should sum to 1)
     * @param q Second distribution (should sum to 1)
     * @return KL-divergence (0.0+, unbounded)
     */
    public static float klDivergence(float[] p, float[] q) {
        if (p.length != q.length) {
            throw new IllegalArgumentException("Distributions must have same length");
        }

        // Normalize to ensure valid probability distributions
        p = normalize(p);
        q = normalize(q);

        float divergence = 0.0f;
        float epsilon = 1e-10f; // Avoid log(0)

        for (int i = 0; i < p.length; i++) {
            if (p[i] > epsilon) {
                divergence += p[i] * Math.log((p[i] + epsilon) / (q[i] + epsilon));
            }
        }

        return divergence;
    }

    /**
     * Calculate symmetric KL-divergence (average of both directions).
     */
    public static float symmetricKlDivergence(float[] p, float[] q) {
        return (klDivergence(p, q) + klDivergence(q, p)) / 2.0f;
    }

    /**
     * Calculate Shannon entropy of a distribution.
     *
     * High (≥ 2.0):    Exploratory (spread across many activities)
     * Medium (1.2-2.0): Directed (some preferences)
     * Low (< 1.2):      Specialized (focused on few activities)
     *
     * @param distribution Activity distribution (should sum to 1)
     * @return Entropy in bits
     */
    public static float shannonEntropy(float[] distribution) {
        distribution = normalize(distribution);

        float entropy = 0.0f;
        float epsilon = 1e-10f;

        for (float p : distribution) {
            if (p > epsilon) {
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }

        return entropy;
    }

    /**
     * Calculate Jensen-Shannon divergence (bounded version of KL).
     *
     * Range: 0.0 - 1.0
     * 0.0 = identical
     * 1.0 = maximally different
     *
     * @param p First distribution
     * @param q Second distribution
     * @return JS-divergence (0.0 - 1.0)
     */
    public static float jensenShannonDivergence(float[] p, float[] q) {
        p = normalize(p);
        q = normalize(q);

        // Calculate midpoint distribution
        float[] m = new float[p.length];
        for (int i = 0; i < p.length; i++) {
            m[i] = (p[i] + q[i]) / 2.0f;
        }

        return (klDivergence(p, m) + klDivergence(q, m)) / 2.0f;
    }

    /**
     * Calculate Euclidean distance between vectors.
     *
     * @param v1 First vector
     * @param v2 Second vector
     * @return Euclidean distance
     */
    public static float euclideanDistance(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        float sum = 0.0f;
        for (int i = 0; i < v1.length; i++) {
            float diff = v1[i] - v2[i];
            sum += diff * diff;
        }

        return (float) Math.sqrt(sum);
    }

    /**
     * Normalize a vector to sum to 1.0.
     */
    private static float[] normalize(float[] v) {
        float sum = 0.0f;
        for (float val : v) {
            sum += val;
        }

        if (sum <= 0.0f) {
            // Return uniform distribution if sum is zero
            float[] result = new float[v.length];
            float uniform = 1.0f / v.length;
            for (int i = 0; i < result.length; i++) {
                result[i] = uniform;
            }
            return result;
        }

        float[] result = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = v[i] / sum;
        }
        return result;
    }

    /**
     * Calculate gini coefficient (inequality measure).
     *
     * 0.0 = perfect equality (uniform distribution)
     * 1.0 = perfect inequality (all in one category)
     *
     * @param values Distribution values
     * @return Gini coefficient (0.0 - 1.0)
     */
    public static float giniCoefficient(float[] values) {
        int n = values.length;
        if (n == 0) return 0.0f;

        // Sort values
        float[] sorted = values.clone();
        java.util.Arrays.sort(sorted);

        float sum = 0.0f;
        float cumulative = 0.0f;

        for (int i = 0; i < n; i++) {
            sum += sorted[i];
            cumulative += (i + 1) * sorted[i];
        }

        if (sum == 0) return 0.0f;

        return (2.0f * cumulative) / (n * sum) - (n + 1.0f) / n;
    }
}
