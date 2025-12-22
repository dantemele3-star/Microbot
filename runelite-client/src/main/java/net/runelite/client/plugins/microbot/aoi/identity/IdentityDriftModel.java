package net.runelite.client.plugins.microbot.aoi.identity;

import net.runelite.client.plugins.microbot.aoi.memory.SleepAware;

import java.util.EnumMap;
import java.util.Map;

/**
 * Long-term specialization tracking model.
 *
 * Identity doesn't emerge from random numbers — it emerges from
 * memory accumulation, belief formation, and reinforcement over time.
 *
 * Behavior:
 * - Days 1-30: EXPLORATION (entropy 2.1, trying everything)
 * - Days 30-60: PREFERENCE FORMATION (entropy 1.68, loops emerge)
 * - Days 60-90: SPECIALIZATION LOCK-IN (entropy 0.95, identity locked)
 *
 * Identity weights represent the relative preference for each category.
 * They range from 0.0 to 1.0 and should sum to approximately 1.0.
 */
public class IdentityDriftModel implements SleepAware {

    // Learning rates - how fast identity shifts
    private static final float ACTIVITY_LEARNING_RATE = 0.01f;  // Per activity
    private static final float SLEEP_DECAY_RATE = 0.005f;       // Drift toward neutral
    private static final float LOCK_IN_THRESHOLD = 0.6f;        // When a category becomes dominant

    // Category weights (sum to ~1.0)
    private final Map<SkillCategory, Float> weights;

    // Tracking
    private final Map<SkillCategory, Integer> activityCounts;
    private int totalActivities;
    private long dayCount;
    private AccountArchetype currentArchetype;

    // Lock-in state
    private boolean identityLocked;
    private SkillCategory lockedCategory;

    public IdentityDriftModel() {
        this.weights = new EnumMap<>(SkillCategory.class);
        this.activityCounts = new EnumMap<>(SkillCategory.class);

        // Initialize with equal weights
        float initialWeight = 1.0f / (SkillCategory.values().length - 1); // Exclude IDLE
        for (SkillCategory category : SkillCategory.values()) {
            if (category != SkillCategory.IDLE) {
                weights.put(category, initialWeight);
                activityCounts.put(category, 0);
            }
        }

        this.totalActivities = 0;
        this.dayCount = 0;
        this.currentArchetype = AccountArchetype.BALANCED;
        this.identityLocked = false;
        this.lockedCategory = null;
    }

    // === Activity Recording ===

    /**
     * Record an activity in a category.
     * This gradually shifts identity weights.
     */
    public void recordActivity(SkillCategory category) {
        if (category == SkillCategory.IDLE) return;

        // Update counts
        activityCounts.merge(category, 1, Integer::sum);
        totalActivities++;

        // Shift weights toward this category
        float currentWeight = weights.getOrDefault(category, 0.0f);
        float boost = ACTIVITY_LEARNING_RATE * (1.0f - currentWeight);

        // Apply boost to this category
        weights.put(category, Math.min(1.0f, currentWeight + boost));

        // Decay other categories proportionally
        float decayTotal = boost;
        for (SkillCategory other : weights.keySet()) {
            if (other != category) {
                float otherWeight = weights.get(other);
                float decay = decayTotal * (otherWeight / (1.0f - currentWeight + 0.001f));
                weights.put(other, Math.max(0.01f, otherWeight - decay));
            }
        }

        // Normalize to sum to 1.0
        normalizeWeights();

        // Check for lock-in
        checkLockIn();

        // Update archetype
        updateArchetype();
    }

    // === SleepAware ===

    @Override
    public void onSleep(float strength) {
        dayCount++;

        // Very gradual drift toward neutral (prevents over-specialization)
        if (!identityLocked) {
            float targetWeight = 1.0f / weights.size();
            for (SkillCategory category : weights.keySet()) {
                float current = weights.get(category);
                float drift = (targetWeight - current) * SLEEP_DECAY_RATE * strength;
                weights.put(category, current + drift);
            }
            normalizeWeights();
        }
    }

    @Override
    public int getSleepPriority() {
        return 45; // Late - after other systems have updated
    }

    @Override
    public String getSleepAwareName() {
        return "IdentityDriftModel";
    }

    // === Query ===

    /**
     * Get weight for a category.
     */
    public float getWeight(SkillCategory category) {
        return weights.getOrDefault(category, 0.0f);
    }

    /**
     * Get all weights as array [SKILLING, COMBAT, QUESTING, MONEY_MAKING, SOCIAL, ACHIEVEMENT].
     */
    public float[] getWeightArray() {
        return new float[] {
                getWeight(SkillCategory.SKILLING),
                getWeight(SkillCategory.COMBAT),
                getWeight(SkillCategory.QUESTING),
                getWeight(SkillCategory.MONEY_MAKING),
                getWeight(SkillCategory.SOCIAL),
                getWeight(SkillCategory.ACHIEVEMENT)
        };
    }

    /**
     * Get activity count for a category.
     */
    public int getActivityCount(SkillCategory category) {
        return activityCounts.getOrDefault(category, 0);
    }

    /**
     * Get the current archetype.
     */
    public AccountArchetype getArchetype() {
        return currentArchetype;
    }

    /**
     * Is identity locked in (specialization complete)?
     */
    public boolean isIdentityLocked() {
        return identityLocked;
    }

    /**
     * Get the locked category (if locked).
     */
    public SkillCategory getLockedCategory() {
        return lockedCategory;
    }

    /**
     * Get total activity count.
     */
    public int getTotalActivities() {
        return totalActivities;
    }

    /**
     * Get day count.
     */
    public long getDayCount() {
        return dayCount;
    }

    /**
     * Calculate behavioral entropy (how spread out activities are).
     * High = exploratory, Low = specialized
     */
    public float calculateEntropy() {
        float entropy = 0.0f;
        for (float weight : weights.values()) {
            if (weight > 0.001f) {
                entropy -= weight * (float) (Math.log(weight) / Math.log(2));
            }
        }
        return entropy;
    }

    /**
     * Get the dominant category.
     */
    public SkillCategory getDominantCategory() {
        SkillCategory dominant = null;
        float maxWeight = 0.0f;

        for (Map.Entry<SkillCategory, Float> entry : weights.entrySet()) {
            if (entry.getValue() > maxWeight) {
                maxWeight = entry.getValue();
                dominant = entry.getKey();
            }
        }

        return dominant;
    }

    // === For Inheritance ===

    /**
     * Create a copy with weakened weights (for alt inheritance).
     */
    public IdentityDriftModel createWeakenedCopy(float inheritanceStrength, float mutationSigma, java.util.Random random) {
        IdentityDriftModel copy = new IdentityDriftModel();

        for (SkillCategory category : weights.keySet()) {
            float parentWeight = weights.get(category);

            // Apply inheritance formula: v_child = clamp(v_parent * strength + noise)
            float noise = (float) (random.nextGaussian() * mutationSigma);
            float childWeight = parentWeight * inheritanceStrength + noise;
            childWeight = Math.max(0.01f, Math.min(1.0f, childWeight));

            copy.weights.put(category, childWeight);
        }

        copy.normalizeWeights();
        copy.updateArchetype();

        return copy;
    }

    // === Internal ===

    private void normalizeWeights() {
        float sum = 0.0f;
        for (float w : weights.values()) {
            sum += w;
        }

        if (sum > 0.001f) {
            for (SkillCategory category : weights.keySet()) {
                weights.put(category, weights.get(category) / sum);
            }
        }
    }

    private void checkLockIn() {
        if (identityLocked) return;

        // Check if any category exceeds lock-in threshold
        for (Map.Entry<SkillCategory, Float> entry : weights.entrySet()) {
            if (entry.getValue() >= LOCK_IN_THRESHOLD) {
                identityLocked = true;
                lockedCategory = entry.getKey();
                break;
            }
        }
    }

    private void updateArchetype() {
        currentArchetype = AccountArchetype.fromWeights(
                getWeight(SkillCategory.SKILLING),
                getWeight(SkillCategory.COMBAT),
                getWeight(SkillCategory.QUESTING),
                getWeight(SkillCategory.SOCIAL),
                getWeight(SkillCategory.MONEY_MAKING)
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("IdentityDriftModel{");
        sb.append("archetype=").append(currentArchetype);
        sb.append(", locked=").append(identityLocked);
        sb.append(", entropy=").append(String.format("%.2f", calculateEntropy()));
        sb.append(", weights={");
        for (Map.Entry<SkillCategory, Float> entry : weights.entrySet()) {
            sb.append(entry.getKey().name()).append("=")
                    .append(String.format("%.2f", entry.getValue())).append(", ");
        }
        sb.append("}}");
        return sb.toString();
    }
}
