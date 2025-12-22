package net.runelite.client.plugins.microbot.aoi.memory;

import java.util.Objects;

/**
 * Proper keying for memory systems.
 *
 * Before: Map<String, EmotionalAssociation>
 *         associations.get("Fishing"); // Ambiguous - skill or quest?
 *
 * After:  Map<MemorySubject, EmotionalAssociation>
 *         MemorySubject.skill("Fishing")  // Unambiguous
 *         MemorySubject.quest("Fishing")  // Different namespace
 *         MemorySubject.player("Name")    // Social (ready!)
 *
 * Benefits:
 * - No ambiguity between subject types
 * - Ready for social memory (players, clans)
 * - Future-proof keying system
 * - Type-safe lookups
 */
public final class MemorySubject {

    public enum SubjectType {
        SKILL,      // A specific skill activity (e.g., "Fishing", "Mining")
        CATEGORY,   // A skill category (e.g., "COMBAT", "SKILLING")
        QUEST,      // A specific quest
        LOCATION,   // A game location
        PLAYER,     // Another player (social memory)
        CLAN,       // A clan (social memory)
        NPC,        // An NPC (for quest/combat associations)
        ITEM,       // An item (for preference tracking)
        EVENT       // A game event type (random events, deaths, etc.)
    }

    private final SubjectType type;
    private final String id;

    private MemorySubject(SubjectType type, String id) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null").toLowerCase();
    }

    // === Factory Methods ===

    public static MemorySubject skill(String skillName) {
        return new MemorySubject(SubjectType.SKILL, skillName);
    }

    public static MemorySubject category(String categoryName) {
        return new MemorySubject(SubjectType.CATEGORY, categoryName);
    }

    public static MemorySubject quest(String questName) {
        return new MemorySubject(SubjectType.QUEST, questName);
    }

    public static MemorySubject location(String locationName) {
        return new MemorySubject(SubjectType.LOCATION, locationName);
    }

    public static MemorySubject player(String playerName) {
        return new MemorySubject(SubjectType.PLAYER, playerName);
    }

    public static MemorySubject clan(String clanName) {
        return new MemorySubject(SubjectType.CLAN, clanName);
    }

    public static MemorySubject npc(String npcName) {
        return new MemorySubject(SubjectType.NPC, npcName);
    }

    public static MemorySubject item(String itemName) {
        return new MemorySubject(SubjectType.ITEM, itemName);
    }

    public static MemorySubject event(String eventType) {
        return new MemorySubject(SubjectType.EVENT, eventType);
    }

    // === Getters ===

    public SubjectType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    // === Type Checks ===

    public boolean isSkill() {
        return type == SubjectType.SKILL;
    }

    public boolean isCategory() {
        return type == SubjectType.CATEGORY;
    }

    public boolean isQuest() {
        return type == SubjectType.QUEST;
    }

    public boolean isSocial() {
        return type == SubjectType.PLAYER || type == SubjectType.CLAN;
    }

    public boolean isLocation() {
        return type == SubjectType.LOCATION;
    }

    // === Object Overrides ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemorySubject that = (MemorySubject) o;
        return type == that.type && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public String toString() {
        return type.name() + ":" + id;
    }

    /**
     * Parse a string representation back to MemorySubject.
     * Format: "TYPE:id"
     */
    public static MemorySubject parse(String str) {
        int colonIndex = str.indexOf(':');
        if (colonIndex <= 0) {
            throw new IllegalArgumentException("Invalid MemorySubject format: " + str);
        }
        SubjectType type = SubjectType.valueOf(str.substring(0, colonIndex));
        String id = str.substring(colonIndex + 1);
        return new MemorySubject(type, id);
    }
}
