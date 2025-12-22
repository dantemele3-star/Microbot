package net.runelite.client.plugins.microbot.aoi.memory;

import java.util.*;

/**
 * Manages sleep consolidation across all SleepAware systems.
 *
 * Sleep consolidation is when:
 * - Memories decay (low-salience events forgotten)
 * - Beliefs form (events compress into stable associations)
 * - Burnout recovers (motivation slowly returns)
 * - Social bonds weaken (if not maintained)
 * - Identity stabilizes (drift values consolidate)
 *
 * Systems register themselves rather than being explicitly called.
 * This follows the Open-Closed Principle.
 */
public class SleepConsolidation {

    private final List<SleepAware> systems;
    private boolean sorted;

    public SleepConsolidation() {
        this.systems = new ArrayList<>();
        this.sorted = false;
    }

    /**
     * Register a system for sleep consolidation.
     * Systems will be called in priority order during consolidate().
     */
    public void register(SleepAware system) {
        systems.add(system);
        sorted = false; // Need to re-sort
    }

    /**
     * Unregister a system from sleep consolidation.
     */
    public void unregister(SleepAware system) {
        systems.remove(system);
    }

    /**
     * Run sleep consolidation across all registered systems.
     *
     * @param strength How restful this sleep was (0.0 - 1.0)
     */
    public void consolidate(float strength) {
        ensureSorted();

        for (SleepAware system : systems) {
            try {
                system.onSleep(strength);
            } catch (Exception e) {
                // Log but don't propagate - one system failing shouldn't crash others
                System.err.println("Error in sleep consolidation for " +
                        system.getSleepAwareName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Consolidate with default strength (full rest = 1.0).
     */
    public void consolidate() {
        consolidate(1.0f);
    }

    /**
     * Get list of registered systems (for debugging).
     */
    public List<String> getRegisteredSystemNames() {
        ensureSorted();
        List<String> names = new ArrayList<>();
        for (SleepAware system : systems) {
            names.add(system.getSleepAwareName() + " (priority: " + system.getSleepPriority() + ")");
        }
        return names;
    }

    /**
     * Get number of registered systems.
     */
    public int getSystemCount() {
        return systems.size();
    }

    private void ensureSorted() {
        if (!sorted) {
            systems.sort(Comparator.comparingInt(SleepAware::getSleepPriority));
            sorted = true;
        }
    }
}
