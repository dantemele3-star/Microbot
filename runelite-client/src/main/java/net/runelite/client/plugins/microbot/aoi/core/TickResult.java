package net.runelite.client.plugins.microbot.aoi.core;

/**
 * Granular outcome reporting for SkillScript execution.
 * Scripts report outcomes - they don't assert them.
 * The orchestrator decides what counts as an activity.
 */
public enum TickResult {
    /**
     * Script is waiting for a condition (e.g., walking, animation)
     * No XP expected, but still actively engaged
     */
    WAITING,

    /**
     * Script successfully completed an action tick
     * May or may not have gained XP
     */
    SUCCESS,

    /**
     * Script has completed its current goal
     * Ready to be reassigned or continue
     */
    COMPLETED,

    /**
     * Script encountered a failure
     * Orchestrator should consider cooldowns
     */
    FAILED,

    /**
     * Script was interrupted (e.g., random event, combat)
     * Not the script's fault, may resume
     */
    INTERRUPTED,

    /**
     * Script is idle by design (e.g., IdleSkillScript)
     * Intentional downtime for fatigue recovery
     */
    IDLE
}
