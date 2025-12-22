package net.runelite.client.plugins.microbot.aoi.social;

import net.runelite.client.plugins.microbot.aoi.memory.MemorySubject;

/**
 * Record of a relationship with another player or clan.
 *
 * Tracks:
 * - Affinity: How much we like them (-1 to +1)
 * - Trust: How reliable they are (0 to 1)
 * - Familiarity: How well we know them (0 to 1)
 * - Influence: How much they affect our decisions (1.0 = neutral)
 *
 * Social relationships grow through interaction and decay without it.
 */
public class SocialRecord {

    private final MemorySubject subject;
    private float affinity;         // -1.0 (dislike) to +1.0 (like)
    private float trust;            // 0.0 (untrusted) to 1.0 (fully trusted)
    private float familiarity;      // 0.0 (stranger) to 1.0 (well known)
    private int interactionCount;   // Total interactions
    private long lastInteractionTick;

    public SocialRecord(MemorySubject subject) {
        if (!subject.isSocial()) {
            throw new IllegalArgumentException("SocialRecord requires PLAYER or CLAN subject type");
        }
        this.subject = subject;
        this.affinity = 0.0f;
        this.trust = 0.3f;          // Start with slight trust
        this.familiarity = 0.0f;
        this.interactionCount = 0;
        this.lastInteractionTick = 0;
    }

    // === Getters ===

    public MemorySubject getSubject() {
        return subject;
    }

    public float getAffinity() {
        return affinity;
    }

    public float getTrust() {
        return trust;
    }

    public float getFamiliarity() {
        return familiarity;
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    public long getLastInteractionTick() {
        return lastInteractionTick;
    }

    /**
     * Calculate influence factor.
     * Base 1.0 + bonuses from affinity and familiarity.
     * Range: 0.85 (avoiding) to 1.25 (strongly influenced)
     */
    public float getInfluence() {
        // Positive affinity + familiarity = positive influence
        float influence = 1.0f + (affinity * 0.15f) + (familiarity * 0.1f);

        // Clamp to reasonable range
        return Math.max(0.85f, Math.min(1.25f, influence));
    }

    // === Interaction Recording ===

    /**
     * Record a positive interaction.
     *
     * @param intensity How positive (0.0 - 1.0)
     * @param currentTick Current game tick
     */
    public void recordPositiveInteraction(float intensity, long currentTick) {
        // Boost affinity and trust
        affinity = Math.min(1.0f, affinity + (intensity * 0.1f));
        trust = Math.min(1.0f, trust + (intensity * 0.05f));

        recordInteraction(currentTick);
    }

    /**
     * Record a negative interaction.
     *
     * @param intensity How negative (0.0 - 1.0)
     * @param currentTick Current game tick
     */
    public void recordNegativeInteraction(float intensity, long currentTick) {
        // Reduce affinity and trust
        affinity = Math.max(-1.0f, affinity - (intensity * 0.15f));
        trust = Math.max(0.0f, trust - (intensity * 0.1f));

        recordInteraction(currentTick);
    }

    /**
     * Record a neutral interaction (proximity, passive).
     *
     * @param currentTick Current game tick
     */
    public void recordNeutralInteraction(long currentTick) {
        recordInteraction(currentTick);
    }

    private void recordInteraction(long currentTick) {
        interactionCount++;
        lastInteractionTick = currentTick;

        // Familiarity grows with interactions (asymptotic)
        familiarity = Math.min(1.0f, familiarity + (0.05f * (1.0f - familiarity)));
    }

    // === Decay ===

    /**
     * Apply decay during sleep consolidation.
     * Weak relationships fade faster than strong ones.
     *
     * @param strength Sleep strength (0.0 - 1.0)
     */
    public void decay(float strength) {
        // Decay rates inversely proportional to relationship strength
        float relationshipStrength = (familiarity + trust + (affinity + 1) / 2) / 3;
        float decayRate = 0.02f * (1.0f - relationshipStrength * 0.5f);

        // Affinity drifts toward neutral
        if (affinity > 0) {
            affinity = Math.max(0.0f, affinity - (decayRate * strength));
        } else {
            affinity = Math.min(0.0f, affinity + (decayRate * strength));
        }

        // Trust and familiarity decay slowly
        trust = Math.max(0.3f, trust - (decayRate * 0.5f * strength));
        familiarity = Math.max(0.0f, familiarity - (decayRate * strength));
    }

    // === Analysis ===

    /**
     * Is this a friend (positive affinity + some familiarity)?
     */
    public boolean isFriend() {
        return affinity > 0.3f && familiarity > 0.3f;
    }

    /**
     * Is this relationship strong enough to influence behavior?
     */
    public boolean isInfluential() {
        return Math.abs(affinity) > 0.2f && familiarity > 0.2f;
    }

    /**
     * Should this relationship be forgotten (too weak/old)?
     */
    public boolean shouldForget(long currentTick) {
        // Forget if:
        // - Very weak relationship (low familiarity)
        // - Very old (no recent interactions)
        // - Few total interactions
        long ticksSinceInteraction = currentTick - lastInteractionTick;
        boolean isWeak = familiarity < 0.1f && interactionCount < 5;
        boolean isOld = ticksSinceInteraction > 600000; // ~100 hours of play

        return isWeak && isOld;
    }

    @Override
    public String toString() {
        return String.format(
                "SocialRecord{%s: affinity=%.2f, trust=%.2f, familiarity=%.2f, interactions=%d, influence=%.2f}",
                subject, affinity, trust, familiarity, interactionCount, getInfluence()
        );
    }
}
