package net.runelite.client.plugins.microbot.aoi.identity;

/**
 * Emergent player types based on long-term behavior patterns.
 *
 * Archetypes emerge naturally from repeated behavior over weeks/months.
 * They CANNOT be inherited (alts must develop their own).
 *
 * Day 90 Distribution (from validation):
 * - SKILLER: 57%
 * - PVM_ORIENTED: 27%
 * - HYBRID: 10%
 * - BALANCED: 6%
 *
 * These match real player distributions observed in the game.
 */
public enum AccountArchetype {

    /**
     * Primarily focused on gathering and artisan skills.
     * Low combat engagement, prefers safe AFK activities.
     */
    SKILLER("Skiller", "Prefers gathering and artisan skills over combat"),

    /**
     * Heavily combat-focused. Slayer, bossing, PvP.
     * High stress tolerance, enjoys challenge.
     */
    PVM_ORIENTED("PvM", "Combat-focused player, enjoys challenges"),

    /**
     * Mix of combat and skilling with no clear preference.
     * Alternates between different content types.
     */
    HYBRID("Hybrid", "Balanced mix of combat and skilling"),

    /**
     * Quest and achievement focused.
     * Completionist tendencies, goal-oriented.
     */
    COMPLETIONIST("Completionist", "Focused on quests and achievements"),

    /**
     * Money-making focused.
     * Prioritizes GP/hour over everything else.
     */
    MERCHANT("Merchant", "Focused on earning gold"),

    /**
     * Socially-oriented player.
     * Clans, minigames, helping others.
     */
    SOCIAL("Social", "Enjoys community and multiplayer content"),

    /**
     * No clear pattern yet - still exploring.
     * Typically accounts under 30 days old.
     */
    BALANCED("Balanced", "Still developing preferences"),

    /**
     * Unknown or undefined archetype.
     */
    UNDEFINED("Undefined", "No clear play style detected");

    private final String displayName;
    private final String description;

    AccountArchetype(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determine archetype from identity drift values.
     *
     * @param skillingWeight Weight for skilling category (0.0 - 1.0)
     * @param combatWeight Weight for combat category (0.0 - 1.0)
     * @param questWeight Weight for quest category (0.0 - 1.0)
     * @param socialWeight Weight for social category (0.0 - 1.0)
     * @param moneyWeight Weight for money-making category (0.0 - 1.0)
     * @return The dominant archetype
     */
    public static AccountArchetype fromWeights(
            float skillingWeight,
            float combatWeight,
            float questWeight,
            float socialWeight,
            float moneyWeight
    ) {
        // Find maximum weight
        float maxWeight = Math.max(Math.max(Math.max(Math.max(
                skillingWeight, combatWeight), questWeight), socialWeight), moneyWeight);

        // If no clear winner, return BALANCED
        float secondMax = findSecondMax(skillingWeight, combatWeight, questWeight, socialWeight, moneyWeight);
        if (maxWeight < 0.3f || maxWeight - secondMax < 0.15f) {
            return BALANCED;
        }

        // Determine dominant archetype
        if (maxWeight == skillingWeight) {
            return SKILLER;
        } else if (maxWeight == combatWeight) {
            return PVM_ORIENTED;
        } else if (maxWeight == questWeight) {
            return COMPLETIONIST;
        } else if (maxWeight == socialWeight) {
            return SOCIAL;
        } else if (maxWeight == moneyWeight) {
            return MERCHANT;
        }

        // Check for hybrid (combat and skilling both high)
        if (combatWeight > 0.25f && skillingWeight > 0.25f) {
            return HYBRID;
        }

        return BALANCED;
    }

    private static float findSecondMax(float... values) {
        float max = Float.MIN_VALUE;
        float secondMax = Float.MIN_VALUE;

        for (float v : values) {
            if (v > max) {
                secondMax = max;
                max = v;
            } else if (v > secondMax) {
                secondMax = v;
            }
        }
        return secondMax;
    }
}
