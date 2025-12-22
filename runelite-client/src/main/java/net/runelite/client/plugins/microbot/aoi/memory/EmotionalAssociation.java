package net.runelite.client.plugins.microbot.aoi.memory;

/**
 * Accumulated emotional bias toward a memory subject.
 *
 * This is now DERIVED from beliefs and recent events,
 * not stored as parallel state.
 *
 * Benefits of derivation:
 * - No parallel state (single source of truth)
 * - Consistent across simulation/runtime
 * - Better explainability
 * - Easier replay and debugging
 *
 * Formula: 70% from belief, 30% from recent events
 */
public class EmotionalAssociation {

    private final MemorySubject subject;
    private final float affinity;       // -1.0 (hate) to +1.0 (love)
    private final float frustration;    // 0.0 - 1.0 (recent failures)
    private final float fear;           // 0.0 - 1.0 (recent deaths/trauma)
    private final float familiarity;    // 0.0 - 1.0 (how well known)
    private final float confidence;     // 0.0 - 1.0 (how certain about feelings)

    private EmotionalAssociation(
            MemorySubject subject,
            float affinity,
            float frustration,
            float fear,
            float familiarity,
            float confidence
    ) {
        this.subject = subject;
        this.affinity = clamp(affinity, -1.0f, 1.0f);
        this.frustration = clamp(frustration, 0.0f, 1.0f);
        this.fear = clamp(fear, 0.0f, 1.0f);
        this.familiarity = clamp(familiarity, 0.0f, 1.0f);
        this.confidence = clamp(confidence, 0.0f, 1.0f);
    }

    // === Getters ===

    public MemorySubject getSubject() {
        return subject;
    }

    public float getAffinity() {
        return affinity;
    }

    public float getFrustration() {
        return frustration;
    }

    public float getFear() {
        return fear;
    }

    public float getFamiliarity() {
        return familiarity;
    }

    public float getConfidence() {
        return confidence;
    }

    // === Derived Values ===

    /**
     * Overall preference weight for activity selection.
     * Positive = prefer, negative = avoid
     */
    public float getPreferenceWeight() {
        // Base affinity, penalized by recent frustration and fear
        float weight = affinity;
        weight -= frustration * 0.3f;  // Recent failures reduce preference
        weight -= fear * 0.5f;          // Fear strongly reduces preference
        weight += familiarity * 0.1f;   // Slight comfort bonus for familiar
        return weight;
    }

    /**
     * Whether this subject feels "safe" based on emotions.
     */
    public boolean feelsSafe() {
        return fear < 0.3f && frustration < 0.5f;
    }

    /**
     * Whether this subject feels "comfortable" (familiar + positive).
     */
    public boolean feelsComfortable() {
        return familiarity > 0.5f && affinity > 0.0f;
    }

    /**
     * Whether to avoid this subject (high fear or frustration).
     */
    public boolean shouldAvoid() {
        return fear > 0.6f || frustration > 0.7f || affinity < -0.5f;
    }

    // === Builder for Derivation ===

    public static Builder builder(MemorySubject subject) {
        return new Builder(subject);
    }

    public static class Builder {
        private final MemorySubject subject;
        private float affinity = 0.0f;
        private float frustration = 0.0f;
        private float fear = 0.0f;
        private float familiarity = 0.0f;
        private float confidence = 0.0f;

        private Builder(MemorySubject subject) {
            this.subject = subject;
        }

        public Builder affinity(float value) {
            this.affinity = value;
            return this;
        }

        public Builder frustration(float value) {
            this.frustration = value;
            return this;
        }

        public Builder fear(float value) {
            this.fear = value;
            return this;
        }

        public Builder familiarity(float value) {
            this.familiarity = value;
            return this;
        }

        public Builder confidence(float value) {
            this.confidence = value;
            return this;
        }

        public EmotionalAssociation build() {
            return new EmotionalAssociation(subject, affinity, frustration, fear, familiarity, confidence);
        }
    }

    // === Utility ===

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return String.format(
                "EmotionalAssociation{%s: affinity=%.2f, frustration=%.2f, fear=%.2f, familiarity=%.2f}",
                subject, affinity, frustration, fear, familiarity
        );
    }
}
