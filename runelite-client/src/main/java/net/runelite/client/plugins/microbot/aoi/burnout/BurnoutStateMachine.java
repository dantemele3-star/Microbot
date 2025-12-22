package net.runelite.client.plugins.microbot.aoi.burnout;

import net.runelite.client.plugins.microbot.aoi.memory.SleepAware;

/**
 * State machine for burnout phase transitions.
 *
 * Burnout accumulation formula:
 * burnout += fatigueAvg * 0.4 + frustration * 0.5 - novelty * 0.3
 *
 * Recovery:
 * - Base recovery: 5% per sleep
 * - Longer breaks: 10% recovery
 * - Novelty events: 15% recovery
 *
 * Recovery is slow (weeks, not days) to feel realistic.
 *
 * Phase transitions:
 * ENGAGED → TIRED → DISENGAGED → RECOVERING → ENGAGED
 */
public class BurnoutStateMachine implements SleepAware {

    // Burnout accumulation weights
    private static final float FATIGUE_WEIGHT = 0.4f;
    private static final float FRUSTRATION_WEIGHT = 0.5f;
    private static final float NOVELTY_RECOVERY = 0.3f;

    // Recovery rates
    private static final float BASE_SLEEP_RECOVERY = 0.05f;
    private static final float EXTENDED_BREAK_RECOVERY = 0.10f;
    private static final float NOVELTY_BOOST_RECOVERY = 0.15f;

    // State
    private BurnoutPhase currentPhase;
    private float burnoutLevel;          // 0.0 - 1.0
    private float averageFatigue;        // Rolling average
    private float accumulatedFrustration;
    private float noveltyCounter;        // Counts new experiences
    private int daysInCurrentPhase;
    private int totalDays;

    // Tracking for recovery bonuses
    private int consecutiveRestDays;
    private boolean hadNoveltyToday;

    public BurnoutStateMachine() {
        this.currentPhase = BurnoutPhase.ENGAGED;
        this.burnoutLevel = 0.0f;
        this.averageFatigue = 0.0f;
        this.accumulatedFrustration = 0.0f;
        this.noveltyCounter = 0.0f;
        this.daysInCurrentPhase = 0;
        this.totalDays = 0;
        this.consecutiveRestDays = 0;
        this.hadNoveltyToday = false;
    }

    // === Daily Updates ===

    /**
     * Update burnout based on daily activity.
     * Called at end of each session.
     *
     * @param averageSessionFatigue Average fatigue level during session
     * @param frustrationEvents Number of frustrating events
     * @param noveltyEvents Number of novel experiences
     */
    public void updateDaily(float averageSessionFatigue, int frustrationEvents, int noveltyEvents) {
        // Update rolling fatigue average
        averageFatigue = (averageFatigue * 0.7f) + (averageSessionFatigue * 0.3f);

        // Accumulate frustration (decays over time)
        accumulatedFrustration = Math.min(1.0f,
                accumulatedFrustration * 0.8f + (frustrationEvents * 0.1f));

        // Track novelty
        if (noveltyEvents > 0) {
            hadNoveltyToday = true;
            noveltyCounter += noveltyEvents * 0.1f;
        }

        // Calculate burnout change
        float burnoutDelta = (averageFatigue * FATIGUE_WEIGHT) +
                (accumulatedFrustration * FRUSTRATION_WEIGHT) -
                (noveltyCounter * NOVELTY_RECOVERY);

        burnoutLevel = Math.max(0.0f, Math.min(1.0f, burnoutLevel + (burnoutDelta * 0.1f)));

        // Reset daily trackers
        noveltyCounter = Math.max(0.0f, noveltyCounter - 0.05f);
    }

    // === SleepAware ===

    @Override
    public void onSleep(float strength) {
        totalDays++;
        daysInCurrentPhase++;

        // Apply recovery
        float recovery = BASE_SLEEP_RECOVERY * strength;

        // Bonus for extended rest
        if (averageFatigue < 0.3f) {
            consecutiveRestDays++;
            if (consecutiveRestDays >= 3) {
                recovery = EXTENDED_BREAK_RECOVERY * strength;
            }
        } else {
            consecutiveRestDays = 0;
        }

        // Bonus for novelty
        if (hadNoveltyToday) {
            recovery += NOVELTY_BOOST_RECOVERY * strength;
        }

        // Apply recovery
        burnoutLevel = Math.max(0.0f, burnoutLevel - recovery);

        // Check phase transitions
        updatePhase();

        // Reset daily flags
        hadNoveltyToday = false;
    }

    @Override
    public int getSleepPriority() {
        return 25; // Mid-priority
    }

    @Override
    public String getSleepAwareName() {
        return "BurnoutStateMachine";
    }

    // === Phase Transitions ===

    private void updatePhase() {
        BurnoutPhase newPhase = currentPhase;

        switch (currentPhase) {
            case ENGAGED:
                if (burnoutLevel >= BurnoutPhase.TIRED.getEntryThreshold()) {
                    newPhase = BurnoutPhase.TIRED;
                }
                break;

            case TIRED:
                if (burnoutLevel >= BurnoutPhase.DISENGAGED.getEntryThreshold()) {
                    newPhase = BurnoutPhase.DISENGAGED;
                } else if (burnoutLevel < BurnoutPhase.TIRED.getEntryThreshold() - 0.1f) {
                    // Hysteresis: need to drop below threshold - 0.1 to go back
                    newPhase = BurnoutPhase.ENGAGED;
                }
                break;

            case DISENGAGED:
                if (burnoutLevel < 0.40f) {
                    // Exit disengaged into recovery
                    newPhase = BurnoutPhase.RECOVERING;
                }
                break;

            case RECOVERING:
                if (burnoutLevel >= BurnoutPhase.TIRED.getEntryThreshold()) {
                    // Can slip back into tired
                    newPhase = BurnoutPhase.TIRED;
                } else if (burnoutLevel < 0.15f && daysInCurrentPhase >= 7) {
                    // Fully recovered after at least a week
                    newPhase = BurnoutPhase.ENGAGED;
                }
                break;
        }

        if (newPhase != currentPhase) {
            transitionTo(newPhase);
        }
    }

    private void transitionTo(BurnoutPhase newPhase) {
        currentPhase = newPhase;
        daysInCurrentPhase = 0;
    }

    // === Getters ===

    public BurnoutPhase getCurrentPhase() {
        return currentPhase;
    }

    public float getBurnoutLevel() {
        return burnoutLevel;
    }

    public float getAverageFatigue() {
        return averageFatigue;
    }

    public float getAccumulatedFrustration() {
        return accumulatedFrustration;
    }

    public int getDaysInCurrentPhase() {
        return daysInCurrentPhase;
    }

    public int getTotalDays() {
        return totalDays;
    }

    // === Phase-Based Modifiers ===

    /**
     * Get session length modifier based on current phase.
     */
    public float getSessionLengthModifier() {
        return currentPhase.getSessionLengthMultiplier();
    }

    /**
     * Get idle activity weight modifier.
     */
    public float getIdleWeightModifier() {
        return currentPhase.getIdleWeightMultiplier();
    }

    /**
     * Get exploration rate for current phase.
     */
    public float getExplorationRate() {
        return currentPhase.getExplorationRate();
    }

    /**
     * Get goal horizon in days.
     */
    public int getGoalHorizonDays() {
        return currentPhase.getGoalHorizonDays();
    }

    /**
     * Get quest suppression factor.
     */
    public float getQuestSuppressionFactor() {
        return currentPhase.getQuestSuppressionFactor();
    }

    /**
     * Get diary tone for current phase.
     */
    public String getDiaryTone() {
        return currentPhase.getDiaryTone();
    }

    /**
     * Should goal-setting be suppressed?
     */
    public boolean shouldSuppressGoals() {
        return currentPhase.shouldSuppressGoals();
    }

    /**
     * Is the account currently in a low-motivation state?
     */
    public boolean isLowMotivation() {
        return currentPhase == BurnoutPhase.DISENGAGED || currentPhase == BurnoutPhase.TIRED;
    }

    // === Manual Controls (for testing/simulation) ===

    public void setBurnoutLevel(float level) {
        this.burnoutLevel = Math.max(0.0f, Math.min(1.0f, level));
        updatePhase();
    }

    public void forcePhase(BurnoutPhase phase) {
        transitionTo(phase);
    }

    @Override
    public String toString() {
        return String.format(
                "BurnoutStateMachine{phase=%s, level=%.2f, daysInPhase=%d, fatigue=%.2f, frustration=%.2f}",
                currentPhase.getDisplayName(), burnoutLevel, daysInCurrentPhase,
                averageFatigue, accumulatedFrustration
        );
    }
}
