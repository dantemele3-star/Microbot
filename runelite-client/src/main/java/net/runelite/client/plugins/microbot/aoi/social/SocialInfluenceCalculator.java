package net.runelite.client.plugins.microbot.aoi.social;

import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

import java.util.*;

/**
 * Calculates social influence on activity decisions.
 *
 * Social context nudges decisions, it never overrides agency.
 *
 * Influence range: ±15%
 *
 * Example interaction progression:
 * 1:  affinity=0.15, influence=1.08
 * 5:  affinity=0.45, influence=1.15
 * 10: affinity=0.68, influence=1.23
 *
 * Activity weight with friend nearby:
 * Base: 1.0 → With social: 1.23 (23% boost)
 */
public class SocialInfluenceCalculator {

    private static final float MAX_INFLUENCE = 0.25f;  // ±25% max
    private static final float CLAN_INFLUENCE_BOOST = 0.1f;

    private final SocialMemory socialMemory;

    public SocialInfluenceCalculator(SocialMemory socialMemory) {
        this.socialMemory = socialMemory;
    }

    /**
     * Calculate social influence modifier for an activity.
     *
     * @param category The skill category being considered
     * @param nearbyPlayers Players currently near the account
     * @return Influence modifier (0.85 - 1.25)
     */
    public float calculateInfluence(SkillCategory category, List<String> nearbyPlayers) {
        float totalInfluence = 1.0f;

        // Aggregate influence from nearby players
        for (String playerName : nearbyPlayers) {
            SocialRecord record = socialMemory.getPlayerRecord(playerName);
            if (record != null && record.isInfluential()) {
                // Friends doing the same activity boost it
                totalInfluence += (record.getInfluence() - 1.0f);
            }
        }

        // Clan membership boost for social activities
        if (socialMemory.isInClan() && category == SkillCategory.SOCIAL) {
            SocialRecord clanRecord = socialMemory.getRecord(socialMemory.getCurrentClan());
            if (clanRecord != null) {
                totalInfluence += CLAN_INFLUENCE_BOOST * clanRecord.getFamiliarity();
            }
        }

        // Clamp to reasonable range
        return Math.max(1.0f - MAX_INFLUENCE, Math.min(1.0f + MAX_INFLUENCE, totalInfluence));
    }

    /**
     * Calculate overall social comfort level.
     * High when among friends, low when isolated or among strangers.
     *
     * @param nearbyPlayers Players currently near the account
     * @return Comfort level (0.0 - 1.0)
     */
    public float calculateSocialComfort(List<String> nearbyPlayers) {
        if (nearbyPlayers.isEmpty()) {
            return 0.5f;  // Neutral when alone
        }

        float totalAffinity = 0.0f;
        float totalFamiliarity = 0.0f;
        int knownPlayers = 0;

        for (String playerName : nearbyPlayers) {
            SocialRecord record = socialMemory.getPlayerRecord(playerName);
            if (record != null) {
                totalAffinity += record.getAffinity();
                totalFamiliarity += record.getFamiliarity();
                knownPlayers++;
            }
        }

        if (knownPlayers == 0) {
            // Among strangers - slightly uncomfortable
            return 0.4f;
        }

        // Average affinity and familiarity
        float avgAffinity = totalAffinity / knownPlayers;
        float avgFamiliarity = totalFamiliarity / knownPlayers;

        // Comfort is combination of positive affinity and familiarity
        float comfort = 0.5f + (avgAffinity * 0.3f) + (avgFamiliarity * 0.2f);

        return Math.max(0.0f, Math.min(1.0f, comfort));
    }

    /**
     * Get the most influential friend for decision tie-breaking.
     *
     * @param nearbyPlayers Players currently near the account
     * @return Most influential friend record, or null if none
     */
    public SocialRecord getMostInfluentialFriend(List<String> nearbyPlayers) {
        SocialRecord mostInfluential = null;
        float maxInfluence = 1.0f;

        for (String playerName : nearbyPlayers) {
            SocialRecord record = socialMemory.getPlayerRecord(playerName);
            if (record != null && record.isFriend()) {
                if (record.getInfluence() > maxInfluence) {
                    maxInfluence = record.getInfluence();
                    mostInfluential = record;
                }
            }
        }

        return mostInfluential;
    }

    /**
     * Calculate category preferences based on social relationships.
     * Friends who specialize in certain categories influence preferences.
     *
     * @return Map of category to influence modifier
     */
    public Map<SkillCategory, Float> getSocialCategoryPreferences() {
        Map<SkillCategory, Float> preferences = new EnumMap<>(SkillCategory.class);

        // Initialize with neutral
        for (SkillCategory category : SkillCategory.values()) {
            preferences.put(category, 1.0f);
        }

        // Aggregate friend influences (placeholder - would need friend activity tracking)
        List<SocialRecord> friends = socialMemory.getFriends();
        if (!friends.isEmpty()) {
            // Social activities get a boost from having friends
            float friendBoost = Math.min(0.2f, friends.size() * 0.03f);
            preferences.put(SkillCategory.SOCIAL, 1.0f + friendBoost);
        }

        // Clan influence
        if (socialMemory.isInClan()) {
            SocialRecord clanRecord = socialMemory.getRecord(socialMemory.getCurrentClan());
            if (clanRecord != null) {
                float clanBoost = clanRecord.getFamiliarity() * 0.15f;
                preferences.put(SkillCategory.SOCIAL,
                        preferences.get(SkillCategory.SOCIAL) + clanBoost);
            }
        }

        return preferences;
    }
}
