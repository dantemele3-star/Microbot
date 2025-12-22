package net.runelite.client.plugins.microbot.aoi.decision;

import net.runelite.client.plugins.microbot.aoi.memory.MemorySubject;
import net.runelite.client.plugins.microbot.aoi.memory.SleepAware;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks regret for activities that led to bad outcomes.
 *
 * Unlike FailureMemory (short-term cooldowns), RegretMemory
 * stores longer-term avoidance that slowly reconciles.
 *
 * Regret accumulates from:
 * - Death during an activity
 * - Significant resource loss
 * - Wasted time (no progress over extended period)
 *
 * Regret slowly fades through reconciliation (successful completion).
 */
public class RegretMemory implements SleepAware {

    private static final float MAX_REGRET = 1.0f;
    private static final float RECONCILIATION_RATE = 0.1f;  // Per successful interaction
    private static final float DECAY_RATE = 0.02f;          // Per sleep cycle

    // Map: subject -> regret level (0.0 - 1.0)
    private final Map<MemorySubject, Float> regrets;

    // Map: subject -> reconciliation progress (0.0 - 1.0)
    private final Map<MemorySubject, Float> reconciliation;

    public RegretMemory() {
        this.regrets = new HashMap<>();
        this.reconciliation = new HashMap<>();
    }

    /**
     * Record regret for an activity.
     *
     * @param subject What we regret
     * @param intensity How much regret (0.0 - 1.0)
     */
    public void recordRegret(MemorySubject subject, float intensity) {
        float current = regrets.getOrDefault(subject, 0.0f);
        regrets.put(subject, Math.min(MAX_REGRET, current + intensity));

        // Reset reconciliation progress
        reconciliation.remove(subject);
    }

    /**
     * Record a positive experience with something we regretted.
     * This contributes to reconciliation.
     *
     * @param subject What we're reconciling with
     * @param intensity How positive the experience (0.0 - 1.0)
     */
    public void recordPositiveExperience(MemorySubject subject, float intensity) {
        if (!regrets.containsKey(subject)) {
            return;  // No regret to reconcile
        }

        float progress = reconciliation.getOrDefault(subject, 0.0f);
        progress += intensity * RECONCILIATION_RATE;
        reconciliation.put(subject, Math.min(1.0f, progress));

        // If reconciliation is complete, reduce regret
        if (progress >= 1.0f) {
            float regret = regrets.get(subject);
            regret = Math.max(0.0f, regret - 0.3f);

            if (regret <= 0.1f) {
                regrets.remove(subject);
                reconciliation.remove(subject);
            } else {
                regrets.put(subject, regret);
                reconciliation.put(subject, 0.0f);  // Reset for next cycle
            }
        }
    }

    /**
     * Get regret level for a subject.
     *
     * @param subject What to check
     * @return Regret level (0.0 - 1.0), 0 if no regret
     */
    public float getRegret(MemorySubject subject) {
        return regrets.getOrDefault(subject, 0.0f);
    }

    /**
     * Get reconciliation progress.
     *
     * @param subject What to check
     * @return Progress (0.0 - 1.0), 0 if no regret or not started
     */
    public float getReconciliationProgress(MemorySubject subject) {
        return reconciliation.getOrDefault(subject, 0.0f);
    }

    /**
     * Check if we have significant regret.
     *
     * @param subject What to check
     * @return true if regret > 0.3
     */
    public boolean hasSignificantRegret(MemorySubject subject) {
        return getRegret(subject) > 0.3f;
    }

    /**
     * Get weight modifier based on regret.
     * High regret = low weight.
     *
     * @param subject What to check
     * @return Weight multiplier (0.2 - 1.0)
     */
    public float getRegretWeight(MemorySubject subject) {
        float regret = getRegret(subject);
        if (regret <= 0.0f) {
            return 1.0f;
        }

        // Smooth reduction based on regret level
        return Math.max(0.2f, 1.0f - (regret * 0.8f));
    }

    // === SleepAware ===

    @Override
    public void onSleep(float strength) {
        // Regret slowly fades over time
        regrets.replaceAll((k, v) -> Math.max(0.0f, v - (DECAY_RATE * strength)));
        regrets.entrySet().removeIf(e -> e.getValue() <= 0.0f);

        // Clear reconciliation for removed regrets
        reconciliation.keySet().retainAll(regrets.keySet());
    }

    @Override
    public int getSleepPriority() {
        return 22;
    }

    @Override
    public String getSleepAwareName() {
        return "RegretMemory";
    }

    // === Query ===

    public int getRegretCount() {
        return regrets.size();
    }

    public void clear() {
        regrets.clear();
        reconciliation.clear();
    }
}
