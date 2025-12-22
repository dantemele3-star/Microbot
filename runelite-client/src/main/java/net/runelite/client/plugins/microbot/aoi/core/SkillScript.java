package net.runelite.client.plugins.microbot.aoi.core;

import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Universal contract for all AOI activities.
 *
 * Design Philosophy:
 * - Scripts never loop forever (time-boxed execution)
 * - Scripts are stateless (context owns all state)
 * - Orchestrator owns control (scripts can't force behavior)
 * - Scripts report outcomes, don't assert them
 */
public interface SkillScript {

    /**
     * Unique identifier for this script type.
     * Used for logging, failure memory, and emotional associations.
     */
    String getId();

    /**
     * Human-readable name for UI display.
     */
    String getName();

    /**
     * The primary skill category this script trains.
     * Used for identity drift, synergy calculations, and archetype tracking.
     */
    SkillCategory getCategory();

    /**
     * Stress level of this activity (0.0 - 1.0).
     * Higher stress = faster fatigue accumulation.
     * Combat = 0.7-1.0, AFK skills = 0.1-0.3
     */
    float getStressLevel();

    /**
     * Whether this script can be paused mid-execution.
     * Some activities (combat) shouldn't be interrupted.
     */
    boolean canInterrupt();

    /**
     * Check if the script can currently start.
     * Validates requirements (items, location, level, etc.)
     */
    boolean canStart(OrchestratorContext context);

    /**
     * Called once when the script is selected.
     * Sets up any necessary state in the context.
     */
    void onStart(OrchestratorContext context);

    /**
     * Execute a single tick of this script.
     * Called every game tick (600ms) while active.
     *
     * @return The outcome of this tick
     */
    TickResult tick(OrchestratorContext context);

    /**
     * Called when the script is stopped (normally or due to switch).
     * Clean up any temporary state.
     */
    void onStop(OrchestratorContext context);

    /**
     * Get synergy bonus with another script.
     * e.g., Fishing → Cooking should return positive value
     *
     * @param previousScript The script that just completed
     * @return Synergy multiplier (1.0 = no effect, >1.0 = bonus, <1.0 = penalty)
     */
    default float getSynergyWith(SkillScript previousScript) {
        return 1.0f;
    }

    /**
     * Whether this is an AFK-friendly activity.
     * AFK scripts preferred during high fatigue.
     */
    default boolean isAfkFriendly() {
        return getStressLevel() < 0.3f;
    }

    /**
     * Get the base weight for activity selection.
     * Higher = more likely to be selected.
     */
    default float getBaseWeight() {
        return 1.0f;
    }
}
