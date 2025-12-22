package net.runelite.client.plugins.microbot.aoi.inheritance;

import java.util.Random;

/**
 * Configuration for personality inheritance from parent to child accounts.
 *
 * Inheritance Formula:
 * v_child = clamp(v_parent * inheritanceStrength + noise(μ=0, σ=mutation), min, max)
 *
 * Example:
 * Parent: Skilling=0.72, Combat=0.22, Quest=0.06
 * Child:  Skilling=0.25, Combat=0.08, Quest=0.04
 *
 * Critical Rules:
 * - Identity weights: Scaled + noisy
 * - Emotional associations: Low confidence (0.3x parent)
 * - Habits: Familiarity only, never preference
 * - Narrative events: NEVER copied
 * - Archetype: CANNOT inherit
 */
public class InheritanceConfig {

    // Identity inheritance
    private final float identityStrength;       // 0.25 - 0.40
    private final float identityMutationSigma;  // 0.05 - 0.15

    // Emotional association inheritance
    private final float emotionStrength;        // 0.20 - 0.35
    private final float emotionConfidenceFactor; // 0.3 (fixed low confidence)

    // Habit inheritance
    private final float habitStrength;          // 0.30 - 0.50

    // Random source for mutation
    private final Random random;

    public InheritanceConfig(
            float identityStrength,
            float identityMutationSigma,
            float emotionStrength,
            float emotionConfidenceFactor,
            float habitStrength,
            Random random
    ) {
        this.identityStrength = identityStrength;
        this.identityMutationSigma = identityMutationSigma;
        this.emotionStrength = emotionStrength;
        this.emotionConfidenceFactor = emotionConfidenceFactor;
        this.habitStrength = habitStrength;
        this.random = random;
    }

    /**
     * Create default inheritance config with reasonable values.
     *
     * @param seed Random seed for reproducibility
     * @return Default configuration
     */
    public static InheritanceConfig createDefault(long seed) {
        return new InheritanceConfig(
                0.30f,  // identityStrength
                0.10f,  // identityMutationSigma
                0.25f,  // emotionStrength
                0.30f,  // emotionConfidenceFactor
                0.40f,  // habitStrength
                new Random(seed)
        );
    }

    /**
     * Create config with custom strength values.
     *
     * @param strength Overall inheritance strength (0.0 - 1.0)
     * @param mutation Mutation variance (0.0 - 0.2)
     * @param seed Random seed
     * @return Custom configuration
     */
    public static InheritanceConfig createCustom(float strength, float mutation, long seed) {
        return new InheritanceConfig(
                strength * 0.5f,           // Identity at half strength
                mutation,
                strength * 0.4f,           // Emotions at 40% strength
                0.30f,                     // Fixed low confidence
                strength * 0.6f,           // Habits at 60% strength
                new Random(seed)
        );
    }

    // === Getters ===

    public float getIdentityStrength() {
        return identityStrength;
    }

    public float getIdentityMutationSigma() {
        return identityMutationSigma;
    }

    public float getEmotionStrength() {
        return emotionStrength;
    }

    public float getEmotionConfidenceFactor() {
        return emotionConfidenceFactor;
    }

    public float getHabitStrength() {
        return habitStrength;
    }

    public Random getRandom() {
        return random;
    }

    // === Inheritance Calculations ===

    /**
     * Apply inheritance formula to a value.
     *
     * @param parentValue Parent's value
     * @param strength Inheritance strength
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return Child's value
     */
    public float inheritValue(float parentValue, float strength, float min, float max) {
        float noise = (float) (random.nextGaussian() * identityMutationSigma);
        float childValue = parentValue * strength + noise;
        return clamp(childValue, min, max);
    }

    /**
     * Apply inheritance formula with default bounds.
     */
    public float inheritValue(float parentValue, float strength) {
        return inheritValue(parentValue, strength, 0.0f, 1.0f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return String.format(
                "InheritanceConfig{identity=%.2f±%.2f, emotion=%.2f@%.0f%%, habit=%.2f}",
                identityStrength, identityMutationSigma,
                emotionStrength, emotionConfidenceFactor * 100,
                habitStrength
        );
    }
}
