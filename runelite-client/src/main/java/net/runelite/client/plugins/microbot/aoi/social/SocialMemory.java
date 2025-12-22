package net.runelite.client.plugins.microbot.aoi.social;

import net.runelite.client.plugins.microbot.aoi.memory.MemorySubject;
import net.runelite.client.plugins.microbot.aoi.memory.SleepAware;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Memory system for social relationships.
 *
 * Tracks relationships with players and clans.
 * Implements SleepAware for relationship decay.
 *
 * Social context nudges, it never overrides agency.
 *
 * Features:
 * - Friends influence skill preference
 * - Clan activities boost related categories
 * - Weak connections decay naturally
 * - Social clusters amplify divergence
 */
public class SocialMemory implements SleepAware {

    private static final int MAX_RELATIONSHIPS = 100;

    private final Map<MemorySubject, SocialRecord> relationships;
    private long currentTick;

    // Clan membership
    private MemorySubject currentClan;

    public SocialMemory() {
        this.relationships = new HashMap<>();
        this.currentTick = 0;
        this.currentClan = null;
    }

    // === Interaction Recording ===

    /**
     * Record a positive interaction with a player.
     */
    public void recordPositivePlayerInteraction(String playerName, float intensity) {
        MemorySubject subject = MemorySubject.player(playerName);
        getOrCreateRecord(subject).recordPositiveInteraction(intensity, currentTick);
    }

    /**
     * Record a negative interaction with a player.
     */
    public void recordNegativePlayerInteraction(String playerName, float intensity) {
        MemorySubject subject = MemorySubject.player(playerName);
        getOrCreateRecord(subject).recordNegativeInteraction(intensity, currentTick);
    }

    /**
     * Record proximity/neutral interaction with a player.
     */
    public void recordProximity(String playerName) {
        MemorySubject subject = MemorySubject.player(playerName);
        getOrCreateRecord(subject).recordNeutralInteraction(currentTick);
    }

    /**
     * Join a clan.
     */
    public void joinClan(String clanName) {
        currentClan = MemorySubject.clan(clanName);
        getOrCreateRecord(currentClan).recordPositiveInteraction(0.5f, currentTick);
    }

    /**
     * Leave current clan.
     */
    public void leaveClan() {
        currentClan = null;
    }

    /**
     * Record clan activity.
     */
    public void recordClanActivity(float intensity) {
        if (currentClan != null) {
            getOrCreateRecord(currentClan).recordPositiveInteraction(intensity, currentTick);
        }
    }

    // === Query ===

    /**
     * Get record for a player.
     */
    public SocialRecord getPlayerRecord(String playerName) {
        return relationships.get(MemorySubject.player(playerName));
    }

    /**
     * Get record for a social subject.
     */
    public SocialRecord getRecord(MemorySubject subject) {
        return relationships.get(subject);
    }

    /**
     * Check if we have any relationship with this player.
     */
    public boolean knows(String playerName) {
        return relationships.containsKey(MemorySubject.player(playerName));
    }

    /**
     * Get all friends (positive affinity + familiarity).
     */
    public List<SocialRecord> getFriends() {
        return relationships.values().stream()
                .filter(SocialRecord::isFriend)
                .collect(Collectors.toList());
    }

    /**
     * Get all influential relationships.
     */
    public List<SocialRecord> getInfluentialRelationships() {
        return relationships.values().stream()
                .filter(SocialRecord::isInfluential)
                .collect(Collectors.toList());
    }

    /**
     * Get current clan (if any).
     */
    public MemorySubject getCurrentClan() {
        return currentClan;
    }

    /**
     * Is in a clan?
     */
    public boolean isInClan() {
        return currentClan != null;
    }

    /**
     * Get all relationship records.
     */
    public Collection<SocialRecord> getAllRelationships() {
        return Collections.unmodifiableCollection(relationships.values());
    }

    /**
     * Get relationship count.
     */
    public int getRelationshipCount() {
        return relationships.size();
    }

    // === SleepAware ===

    @Override
    public void onSleep(float strength) {
        // Decay all relationships
        for (SocialRecord record : relationships.values()) {
            record.decay(strength);
        }

        // Remove forgotten relationships
        relationships.entrySet().removeIf(entry ->
                entry.getValue().shouldForget(currentTick)
        );
    }

    @Override
    public int getSleepPriority() {
        return 35; // After most memory systems
    }

    @Override
    public String getSleepAwareName() {
        return "SocialMemory";
    }

    // === State Management ===

    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    public void clear() {
        relationships.clear();
        currentClan = null;
    }

    // === Internal ===

    private SocialRecord getOrCreateRecord(MemorySubject subject) {
        return relationships.computeIfAbsent(subject, s -> {
            // Enforce max relationships
            if (relationships.size() >= MAX_RELATIONSHIPS) {
                pruneWeakestRelationship();
            }
            return new SocialRecord(s);
        });
    }

    private void pruneWeakestRelationship() {
        // Find and remove the weakest non-friend relationship
        SocialRecord weakest = null;
        float weakestScore = Float.MAX_VALUE;

        for (SocialRecord record : relationships.values()) {
            if (!record.isFriend()) {
                float score = record.getFamiliarity() + record.getTrust();
                if (score < weakestScore) {
                    weakestScore = score;
                    weakest = record;
                }
            }
        }

        if (weakest != null) {
            relationships.remove(weakest.getSubject());
        }
    }
}
