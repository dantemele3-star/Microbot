package net.runelite.client.plugins.microbot.aoi.memory;

/**
 * A subjective interpretation of an experience.
 *
 * Key insight: "Not facts — interpretations. This stores HOW the
 * account experienced something, not WHAT happened objectively."
 *
 * NarrativeEvents are:
 * - Subjective (same objective event can create different narratives)
 * - Time-stamped (for decay and compression)
 * - Weighted by emotional impact and salience
 * - Bounded (old low-salience events are discarded)
 */
public class NarrativeEvent {

    private final long tick;                    // When this happened
    private final NarrativeType type;           // Type of interpretation
    private final MemorySubject subject;        // What/who this is about
    private final float emotionalImpact;        // -1.0 (very negative) to +1.0 (very positive)
    private final float salience;               // 0.0 (forgettable) to 1.0 (unforgettable)
    private final float confidence;             // 0.0 (uncertain) to 1.0 (certain)
    private final String narrative;             // The subjective story

    // Decay tracking
    private float currentSalience;              // Salience after decay
    private boolean compressed;                 // Has been compressed into belief

    public NarrativeEvent(
            long tick,
            NarrativeType type,
            MemorySubject subject,
            float emotionalImpact,
            float salience,
            float confidence,
            String narrative
    ) {
        this.tick = tick;
        this.type = type;
        this.subject = subject;
        this.emotionalImpact = clamp(emotionalImpact, -1.0f, 1.0f);
        this.salience = clamp(salience, 0.0f, 1.0f);
        this.currentSalience = this.salience;
        this.confidence = clamp(confidence, 0.0f, 1.0f);
        this.narrative = narrative;
        this.compressed = false;
    }

    // === Factory Methods ===

    public static NarrativeEvent achievement(long tick, MemorySubject subject, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.ACHIEVEMENT, subject, 0.6f, 0.7f, 0.9f, narrative);
    }

    public static NarrativeEvent enjoyment(long tick, MemorySubject subject, float intensity, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.ENJOYMENT, subject, intensity * 0.5f, 0.4f, 0.8f, narrative);
    }

    public static NarrativeEvent failure(long tick, MemorySubject subject, float severity, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.FAILURE, subject, -severity * 0.6f, 0.5f + severity * 0.3f, 0.9f, narrative);
    }

    public static NarrativeEvent death(long tick, MemorySubject subject, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.DEATH, subject, -0.8f, 0.9f, 1.0f, narrative);
    }

    public static NarrativeEvent frustration(long tick, MemorySubject subject, float intensity, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.FRUSTRATION, subject, -intensity * 0.4f, 0.4f, 0.7f, narrative);
    }

    public static NarrativeEvent boredom(long tick, MemorySubject subject, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.BOREDOM, subject, -0.2f, 0.3f, 0.6f, narrative);
    }

    public static NarrativeEvent exploration(long tick, MemorySubject subject, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.EXPLORATION, subject, 0.3f, 0.5f, 0.8f, narrative);
    }

    public static NarrativeEvent routine(long tick, MemorySubject subject, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.ROUTINE, subject, 0.0f, 0.2f, 0.9f, narrative);
    }

    public static NarrativeEvent socialPositive(long tick, MemorySubject subject, float intensity, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.SOCIAL_POSITIVE, subject, intensity * 0.5f, 0.5f, 0.7f, narrative);
    }

    public static NarrativeEvent socialNegative(long tick, MemorySubject subject, float intensity, String narrative) {
        return new NarrativeEvent(tick, NarrativeType.SOCIAL_NEGATIVE, subject, -intensity * 0.4f, 0.5f, 0.7f, narrative);
    }

    // === Getters ===

    public long getTick() {
        return tick;
    }

    public NarrativeType getType() {
        return type;
    }

    public MemorySubject getSubject() {
        return subject;
    }

    public float getEmotionalImpact() {
        return emotionalImpact;
    }

    public float getSalience() {
        return salience;
    }

    public float getCurrentSalience() {
        return currentSalience;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getNarrative() {
        return narrative;
    }

    public boolean isCompressed() {
        return compressed;
    }

    // === Decay ===

    /**
     * Apply decay to this event's salience.
     * Called during sleep consolidation.
     *
     * @param decayRate How much to reduce salience (0.0 - 1.0)
     */
    public void decay(float decayRate) {
        currentSalience = Math.max(0.0f, currentSalience - (currentSalience * decayRate));
    }

    /**
     * Check if this event is below the retention threshold.
     */
    public boolean shouldDiscard(float threshold) {
        return currentSalience < threshold && !isHighImpact();
    }

    /**
     * High-impact events resist discarding.
     */
    public boolean isHighImpact() {
        return Math.abs(emotionalImpact) > 0.6f || type == NarrativeType.DEATH || type == NarrativeType.ACHIEVEMENT;
    }

    /**
     * Mark as compressed into a belief.
     */
    public void markCompressed() {
        this.compressed = true;
    }

    // === Analysis ===

    /**
     * Is this a positive experience?
     */
    public boolean isPositive() {
        return emotionalImpact > 0.1f;
    }

    /**
     * Is this a negative experience?
     */
    public boolean isNegative() {
        return emotionalImpact < -0.1f;
    }

    /**
     * Get the weighted impact (impact × salience × confidence).
     */
    public float getWeightedImpact() {
        return emotionalImpact * currentSalience * confidence;
    }

    /**
     * Age of this event in ticks.
     */
    public long getAge(long currentTick) {
        return currentTick - tick;
    }

    // === Utility ===

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %.2f impact, %.2f salience - %s",
                type, subject, emotionalImpact, currentSalience, narrative);
    }
}
