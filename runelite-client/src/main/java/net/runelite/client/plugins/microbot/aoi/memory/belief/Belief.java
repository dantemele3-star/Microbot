package net.runelite.client.plugins.microbot.aoi.memory.belief;

import net.runelite.client.plugins.microbot.aoi.memory.MemorySubject;

/**
 * A compressed representation of many narrative events.
 *
 * Problem: Memories accumulate forever, causing bloat and
 * overweighting ancient events.
 *
 * Solution: Periodically compress events into beliefs.
 *
 * Example:
 *   8 Fishing events (impacts: +0.3, +0.4, -0.2, +0.3, +0.5, +0.3)
 *   ↓
 *   Weighted sentiment: +0.28
 *   Certainty: 0.40 (8 events / 20 threshold)
 *   ↓
 *   Belief: "Fishing is enjoyable and rewarding"
 *
 * Benefits:
 * - Prevents memory bloat
 * - Stable long-term bias
 * - Explainability ("why does agent feel this way?")
 * - Old events can be safely discarded after compression
 */
public class Belief {

    private final MemorySubject subject;
    private float sentiment;      // -1.0 (hate) to +1.0 (love)
    private float certainty;      // 0.0 (unsure) to 1.0 (confident)
    private String summary;       // Human-readable explanation
    private int eventCount;       // Number of events compressed
    private long lastUpdatedTick;

    public Belief(MemorySubject subject, float sentiment, float certainty, String summary, int eventCount, long tick) {
        this.subject = subject;
        this.sentiment = clamp(sentiment, -1.0f, 1.0f);
        this.certainty = clamp(certainty, 0.0f, 1.0f);
        this.summary = summary;
        this.eventCount = eventCount;
        this.lastUpdatedTick = tick;
    }

    // === Getters ===

    public MemorySubject getSubject() {
        return subject;
    }

    public float getSentiment() {
        return sentiment;
    }

    public float getCertainty() {
        return certainty;
    }

    public String getSummary() {
        return summary;
    }

    public int getEventCount() {
        return eventCount;
    }

    public long getLastUpdatedTick() {
        return lastUpdatedTick;
    }

    // === Analysis ===

    /**
     * Is this a positive belief?
     */
    public boolean isPositive() {
        return sentiment > 0.15f;
    }

    /**
     * Is this a negative belief?
     */
    public boolean isNegative() {
        return sentiment < -0.15f;
    }

    /**
     * Is this a neutral belief?
     */
    public boolean isNeutral() {
        return sentiment >= -0.15f && sentiment <= 0.15f;
    }

    /**
     * Is this belief confident enough to influence decisions?
     */
    public boolean isConfident() {
        return certainty > 0.4f;
    }

    /**
     * Get the weighted sentiment (sentiment × certainty).
     * More certain beliefs have more influence.
     */
    public float getWeightedSentiment() {
        return sentiment * certainty;
    }

    // === Updates ===

    /**
     * Update this belief with new data from compression.
     *
     * Uses exponential moving average to blend old and new.
     * Older beliefs are more stable (require more evidence to change).
     */
    public void update(float newSentiment, float newCertainty, String newSummary, int additionalEvents, long tick) {
        // Weight based on how established the belief is
        float stability = Math.min(0.8f, eventCount / 50.0f);
        float newWeight = 1.0f - stability;

        this.sentiment = clamp(
                (this.sentiment * stability) + (newSentiment * newWeight),
                -1.0f, 1.0f
        );

        // Certainty can only increase with more evidence
        this.certainty = Math.min(1.0f, Math.max(this.certainty, newCertainty));

        this.summary = newSummary;
        this.eventCount += additionalEvents;
        this.lastUpdatedTick = tick;
    }

    /**
     * Apply decay during sleep consolidation.
     * Very gradual - beliefs are stable over months.
     */
    public void decay(float strength) {
        // Certainty decays slowly toward neutral if not reinforced
        certainty = Math.max(0.1f, certainty - (0.02f * strength));

        // Sentiment drifts toward neutral very slowly
        if (sentiment > 0) {
            sentiment = Math.max(0.0f, sentiment - (0.01f * strength));
        } else {
            sentiment = Math.min(0.0f, sentiment + (0.01f * strength));
        }
    }

    // === Utility ===

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return String.format(
                "Belief{%s: sentiment=%.2f, certainty=%.2f, events=%d, summary='%s'}",
                subject, sentiment, certainty, eventCount, summary
        );
    }
}
