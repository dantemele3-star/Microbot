package net.runelite.client.plugins.microbot.aoi.memory;

/**
 * Types of narrative events that can be recorded.
 * These represent subjective interpretations, not objective facts.
 *
 * "Not facts — interpretations. This stores HOW the account
 * experienced something, not WHAT happened objectively."
 */
public enum NarrativeType {

    // === Positive Experiences ===

    /**
     * Accomplished a goal or milestone
     */
    ACHIEVEMENT,

    /**
     * Activity was enjoyable or satisfying
     */
    ENJOYMENT,

    /**
     * Made progress toward something
     */
    PROGRESS,

    /**
     * Unexpected positive outcome
     */
    SURPRISE_POSITIVE,

    /**
     * Successful social interaction
     */
    SOCIAL_POSITIVE,

    // === Negative Experiences ===

    /**
     * Failed at something
     */
    FAILURE,

    /**
     * Activity was frustrating
     */
    FRUSTRATION,

    /**
     * Felt bored or disengaged
     */
    BOREDOM,

    /**
     * Died or lost significant resources
     */
    DEATH,

    /**
     * Unexpected negative outcome
     */
    SURPRISE_NEGATIVE,

    /**
     * Negative social interaction
     */
    SOCIAL_NEGATIVE,

    // === Neutral/Observational ===

    /**
     * Explored a new area or activity
     */
    EXPLORATION,

    /**
     * Routine activity, neither good nor bad
     */
    ROUTINE,

    /**
     * Took a break or rested
     */
    REST,

    /**
     * Was interrupted (random event, etc.)
     */
    INTERRUPTION,

    // === Metacognitive ===

    /**
     * Regretted a previous decision
     */
    REGRET,

    /**
     * Felt confident about an activity
     */
    CONFIDENCE,

    /**
     * Felt anxious or stressed
     */
    ANXIETY,

    /**
     * Activity became a habit
     */
    HABIT_FORMATION
}
