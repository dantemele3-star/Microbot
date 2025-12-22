package net.runelite.client.plugins.microbot.aoi.burnout;

/**
 * Burnout phase states for mode-based behavior switching.
 *
 * Problem: Burnout as weight penalties felt mechanical, not human.
 * Solution: Mode switching with distinct behavioral characteristics.
 *
 * Phase Characteristics:
 * Phase        | Goal Horizon | Exploration | Session Length
 * ENGAGED      | 7 days       | 30%         | 100%
 * TIRED        | 3 days       | 10%         | 70%
 * DISENGAGED   | 0 days       | 0%          | 40%
 * RECOVERING   | 5 days       | 20%         | 80%
 *
 * Diary tones change with phase:
 * - ENGAGED: "productive and engaged"
 * - TIRED: "a bit tired, taking it easy"
 * - DISENGAGED: "didn't feel like much today"
 * - RECOVERING: "starting to feel better"
 */
public enum BurnoutPhase {

    /**
     * Normal engaged state.
     * Full motivation, long-term goals, active exploration.
     */
    ENGAGED(
            "Engaged",
            "productive and engaged",
            7,      // Goal horizon (days)
            0.30f,  // Exploration rate
            1.0f,   // Session length multiplier
            1.0f,   // Idle weight multiplier
            0.0f    // Entry threshold (burnout level to enter)
    ),

    /**
     * Beginning to feel tired.
     * Reduced motivation, shorter goals, prefer AFK.
     */
    TIRED(
            "Tired",
            "a bit tired, taking it easy",
            3,
            0.10f,
            0.70f,
            1.5f,   // Prefer idle activities
            0.30f   // Enter when burnout >= 0.30
    ),

    /**
     * Fully burned out.
     * No long-term goals, minimal activity, heavy idle.
     */
    DISENGAGED(
            "Disengaged",
            "didn't feel like much today",
            0,      // No future planning
            0.0f,   // No exploration
            0.40f,  // Very short sessions
            2.5f,   // Strongly prefer idle
            0.60f   // Enter when burnout >= 0.60
    ),

    /**
     * Recovering from burnout.
     * Gradually returning to normal, mid-range settings.
     */
    RECOVERING(
            "Recovering",
            "starting to feel better",
            5,
            0.20f,
            0.80f,
            1.2f,
            -1.0f   // Special: entered when leaving DISENGAGED with burnout < 0.40
    );

    private final String displayName;
    private final String diaryTone;
    private final int goalHorizonDays;
    private final float explorationRate;
    private final float sessionLengthMultiplier;
    private final float idleWeightMultiplier;
    private final float entryThreshold;

    BurnoutPhase(
            String displayName,
            String diaryTone,
            int goalHorizonDays,
            float explorationRate,
            float sessionLengthMultiplier,
            float idleWeightMultiplier,
            float entryThreshold
    ) {
        this.displayName = displayName;
        this.diaryTone = diaryTone;
        this.goalHorizonDays = goalHorizonDays;
        this.explorationRate = explorationRate;
        this.sessionLengthMultiplier = sessionLengthMultiplier;
        this.idleWeightMultiplier = idleWeightMultiplier;
        this.entryThreshold = entryThreshold;
    }

    // === Getters ===

    public String getDisplayName() {
        return displayName;
    }

    public String getDiaryTone() {
        return diaryTone;
    }

    public int getGoalHorizonDays() {
        return goalHorizonDays;
    }

    public float getExplorationRate() {
        return explorationRate;
    }

    public float getSessionLengthMultiplier() {
        return sessionLengthMultiplier;
    }

    public float getIdleWeightMultiplier() {
        return idleWeightMultiplier;
    }

    public float getEntryThreshold() {
        return entryThreshold;
    }

    // === Phase Logic ===

    /**
     * Check if this phase should suppress goal-setting.
     */
    public boolean shouldSuppressGoals() {
        return goalHorizonDays == 0;
    }

    /**
     * Check if this phase prefers AFK activities.
     */
    public boolean prefersAfk() {
        return idleWeightMultiplier > 1.2f;
    }

    /**
     * Get quest suppression factor (how much to reduce quest weight).
     * DISENGAGED = 0.2 (80% suppression), ENGAGED = 1.0 (no suppression)
     */
    public float getQuestSuppressionFactor() {
        switch (this) {
            case DISENGAGED: return 0.2f;
            case TIRED: return 0.5f;
            case RECOVERING: return 0.7f;
            case ENGAGED:
            default: return 1.0f;
        }
    }
}
