package net.runelite.client.plugins.microbot.aoi.memory;

/**
 * Registry interface for systems that need consolidation during sleep.
 *
 * BEFORE: Sleep knew about every system explicitly:
 *         SleepConsolidation.consolidate(narrative, regret, failure, ...)
 *
 * AFTER:  Systems register themselves:
 *         SleepConsolidation.register(narrativeMemory.getStore());
 *         SleepConsolidation.register(regretMemory);
 *         // Future systems just call register()
 *
 * Benefits:
 * - Extensible without editing core code (Open-Closed Principle)
 * - Ready for social memory and future systems
 * - Clean separation of concerns
 * - No temporal coupling
 */
public interface SleepAware {

    /**
     * Called during sleep consolidation.
     *
     * This is when memories decay, beliefs form, and
     * the account's "sense of self" is updated.
     *
     * @param strength How restful this sleep was (0.0 - 1.0)
     *                 1.0 = full night's rest (strong consolidation)
     *                 0.5 = short nap (moderate consolidation)
     *                 0.0 = minimal rest (very weak consolidation)
     */
    void onSleep(float strength);

    /**
     * Get the priority of this system during sleep.
     * Lower numbers process first.
     *
     * Suggested order:
     * 0-10:   Core memory systems (narrative store)
     * 10-20:  Derived systems (beliefs, emotions)
     * 20-30:  Behavioral systems (habits, burnout)
     * 30-40:  Social systems (relationships)
     * 40-50:  Meta systems (identity drift)
     */
    default int getSleepPriority() {
        return 25;
    }

    /**
     * Name of this system for logging.
     */
    default String getSleepAwareName() {
        return getClass().getSimpleName();
    }
}
