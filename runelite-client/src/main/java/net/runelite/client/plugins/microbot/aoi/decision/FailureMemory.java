package net.runelite.client.plugins.microbot.aoi.decision;

import net.runelite.client.plugins.microbot.aoi.memory.SleepAware;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks recent failures to avoid retry death spirals.
 *
 * When an activity fails, it goes on cooldown for 15-30 minutes.
 * This prevents the bot from immediately retrying and failing again.
 *
 * Cooldowns decay over time and reset on sleep.
 */
public class FailureMemory implements SleepAware {

    private static final int BASE_COOLDOWN_TICKS = 1500;  // 15 minutes
    private static final int MAX_COOLDOWN_TICKS = 3000;   // 30 minutes
    private static final float CONSECUTIVE_FAILURE_MULTIPLIER = 1.5f;

    // Map: scriptId -> tick when cooldown expires
    private final Map<String, Long> cooldowns;

    // Map: scriptId -> consecutive failure count
    private final Map<String, Integer> consecutiveFailures;

    private long currentTick;

    public FailureMemory() {
        this.cooldowns = new HashMap<>();
        this.consecutiveFailures = new HashMap<>();
        this.currentTick = 0;
    }

    /**
     * Record a failure for an activity.
     *
     * @param scriptId The script that failed
     */
    public void recordFailure(String scriptId) {
        // Increment consecutive failures
        int failures = consecutiveFailures.getOrDefault(scriptId, 0) + 1;
        consecutiveFailures.put(scriptId, failures);

        // Calculate cooldown with consecutive failure multiplier
        int cooldown = (int) (BASE_COOLDOWN_TICKS * Math.pow(CONSECUTIVE_FAILURE_MULTIPLIER, failures - 1));
        cooldown = Math.min(cooldown, MAX_COOLDOWN_TICKS);

        // Set cooldown expiry
        cooldowns.put(scriptId, currentTick + cooldown);
    }

    /**
     * Record a success (resets consecutive failure count).
     *
     * @param scriptId The script that succeeded
     */
    public void recordSuccess(String scriptId) {
        consecutiveFailures.remove(scriptId);
    }

    /**
     * Check if an activity is on cooldown.
     *
     * @param scriptId The script to check
     * @return true if on cooldown
     */
    public boolean isOnCooldown(String scriptId) {
        Long expiry = cooldowns.get(scriptId);
        return expiry != null && currentTick < expiry;
    }

    /**
     * Get remaining cooldown time in ticks.
     *
     * @param scriptId The script to check
     * @return Remaining ticks, or 0 if not on cooldown
     */
    public long getRemainingCooldown(String scriptId) {
        Long expiry = cooldowns.get(scriptId);
        if (expiry == null || currentTick >= expiry) {
            return 0;
        }
        return expiry - currentTick;
    }

    /**
     * Get failure weight multiplier for activity selection.
     * Activities on cooldown get very low weight.
     *
     * @param scriptId The script to check
     * @return Weight multiplier (0.0 - 1.0)
     */
    public float getFailureWeight(String scriptId) {
        if (!isOnCooldown(scriptId)) {
            return 1.0f;
        }

        // Weight decreases based on remaining cooldown
        long remaining = getRemainingCooldown(scriptId);
        return Math.max(0.1f, 1.0f - (remaining / (float) MAX_COOLDOWN_TICKS));
    }

    /**
     * Get consecutive failure count.
     *
     * @param scriptId The script to check
     * @return Number of consecutive failures
     */
    public int getConsecutiveFailures(String scriptId) {
        return consecutiveFailures.getOrDefault(scriptId, 0);
    }

    // === SleepAware ===

    @Override
    public void onSleep(float strength) {
        // Clear all cooldowns on sleep
        cooldowns.clear();

        // Reduce consecutive failure counts
        consecutiveFailures.replaceAll((k, v) -> Math.max(0, v - 1));
        consecutiveFailures.entrySet().removeIf(e -> e.getValue() <= 0);
    }

    @Override
    public int getSleepPriority() {
        return 20;
    }

    @Override
    public String getSleepAwareName() {
        return "FailureMemory";
    }

    // === State Management ===

    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    public void clear() {
        cooldowns.clear();
        consecutiveFailures.clear();
    }
}
