package net.runelite.client.plugins.microbot.aoi.identity;

/**
 * Categories of activities that contribute to identity formation.
 * These are used for identity drift, archetype determination,
 * and emotional association grouping.
 */
public enum SkillCategory {
    /**
     * Gathering and processing skills (Fishing, Mining, Woodcutting, etc.)
     */
    SKILLING(0.1f, true),

    /**
     * Combat-related activities (Melee, Ranged, Magic, Slayer)
     */
    COMBAT(0.7f, false),

    /**
     * Quest progression and story content
     */
    QUESTING(0.4f, false),

    /**
     * Moneymaking activities (merchanting, high-alch, etc.)
     */
    MONEY_MAKING(0.3f, true),

    /**
     * Social activities (chatting, trading, minigames)
     */
    SOCIAL(0.2f, true),

    /**
     * Achievement hunting (diaries, collection log)
     */
    ACHIEVEMENT(0.5f, false),

    /**
     * Intentional idle/rest periods
     */
    IDLE(0.0f, true);

    private final float baseStress;
    private final boolean afkFriendly;

    SkillCategory(float baseStress, boolean afkFriendly) {
        this.baseStress = baseStress;
        this.afkFriendly = afkFriendly;
    }

    public float getBaseStress() {
        return baseStress;
    }

    public boolean isAfkFriendly() {
        return afkFriendly;
    }
}
