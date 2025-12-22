package net.runelite.client.plugins.microbot.aoi.memory;

import net.runelite.client.plugins.microbot.aoi.memory.belief.Belief;
import net.runelite.client.plugins.microbot.aoi.memory.belief.BeliefSystem;

import java.util.List;

/**
 * Derives EmotionalAssociation from beliefs and recent events.
 *
 * Formula: 70% from belief, 30% from recent events
 *
 * This replaces the old parallel-state EmotionalAssociation storage.
 * Now emotions are computed on-demand from the source of truth.
 *
 * Benefits:
 * - No inconsistency between beliefs and emotions
 * - Emotions automatically update when beliefs change
 * - Recent events add volatility (short-term mood)
 * - Long-term beliefs provide stability
 */
public class DerivedEmotions {

    private static final float BELIEF_WEIGHT = 0.7f;
    private static final float RECENT_WEIGHT = 0.3f;
    private static final int RECENT_TICKS = 6000; // ~1 hour
    private static final float FAMILIARITY_THRESHOLD = 20; // Events to max familiarity

    private final BeliefSystem beliefSystem;
    private final NarrativeStore narrativeStore;

    public DerivedEmotions(BeliefSystem beliefSystem, NarrativeStore narrativeStore) {
        this.beliefSystem = beliefSystem;
        this.narrativeStore = narrativeStore;
    }

    /**
     * Derive the emotional association for a subject.
     *
     * @param subject The memory subject to get emotions for
     * @return Derived emotional association
     */
    public EmotionalAssociation derive(MemorySubject subject) {
        // Get belief (long-term)
        Belief belief = beliefSystem.getBelief(subject);

        // Get recent events (short-term)
        List<NarrativeEvent> recentEvents = narrativeStore.getRecentEvents(RECENT_TICKS).stream()
                .filter(e -> e.getSubject().equals(subject))
                .toList();

        // Calculate recent impact
        float recentImpact = calculateRecentImpact(recentEvents);
        float recentFrustration = calculateRecentFrustration(recentEvents);
        float recentFear = calculateRecentFear(recentEvents);

        // Get total event count for familiarity
        int eventCount = narrativeStore.getEventCount(subject);
        float familiarity = Math.min(1.0f, eventCount / FAMILIARITY_THRESHOLD);

        // Derive affinity (70% belief + 30% recent)
        float beliefSentiment = belief != null ? belief.getSentiment() : 0.0f;
        float affinity = (beliefSentiment * BELIEF_WEIGHT) + (recentImpact * RECENT_WEIGHT);

        // Confidence comes from belief certainty and event count
        float confidence = calculateConfidence(belief, eventCount);

        return EmotionalAssociation.builder(subject)
                .affinity(affinity)
                .frustration(recentFrustration)  // Recent failures only
                .fear(recentFear)                 // Recent deaths only
                .familiarity(familiarity)
                .confidence(confidence)
                .build();
    }

    /**
     * Check if there's any emotional data for a subject.
     */
    public boolean hasEmotions(MemorySubject subject) {
        return beliefSystem.getBelief(subject) != null ||
                narrativeStore.getEventCount(subject) > 0;
    }

    // === Internal Calculations ===

    private float calculateRecentImpact(List<NarrativeEvent> events) {
        if (events.isEmpty()) return 0.0f;

        double totalImpact = events.stream()
                .mapToDouble(NarrativeEvent::getWeightedImpact)
                .sum();

        // Average and normalize
        return (float) Math.max(-1.0, Math.min(1.0, totalImpact / events.size()));
    }

    private float calculateRecentFrustration(List<NarrativeEvent> events) {
        // Count failure events
        long failures = events.stream()
                .filter(e -> e.getType() == NarrativeType.FAILURE ||
                        e.getType() == NarrativeType.FRUSTRATION)
                .count();

        // Scale: 3+ failures = max frustration
        return Math.min(1.0f, failures / 3.0f);
    }

    private float calculateRecentFear(List<NarrativeEvent> events) {
        // Count death/trauma events
        long deaths = events.stream()
                .filter(e -> e.getType() == NarrativeType.DEATH ||
                        e.getType() == NarrativeType.ANXIETY)
                .count();

        // Any recent death = high fear
        return Math.min(1.0f, deaths * 0.7f);
    }

    private float calculateConfidence(Belief belief, int eventCount) {
        float beliefConfidence = belief != null ? belief.getCertainty() : 0.0f;
        float experienceConfidence = Math.min(1.0f, eventCount / 10.0f);

        // More weight to direct experience
        return (beliefConfidence * 0.4f) + (experienceConfidence * 0.6f);
    }
}
