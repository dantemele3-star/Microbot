package net.runelite.client.plugins.microbot.aoi.decision;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;

import java.util.List;

/**
 * Interface for activity selection logic.
 *
 * The decider takes all available activities and the current context,
 * and returns the best activity to perform next.
 *
 * Decision factors can include:
 * - Fatigue and stress levels
 * - Recent activity history
 * - Failure memory and cooldowns
 * - Identity drift and preferences
 * - Social influences
 * - Burnout phase
 * - Emotional associations
 * - And many more...
 */
public interface ActivityDecider {

    /**
     * Select the next activity to perform.
     *
     * @param availableScripts All scripts that could potentially run
     * @param context Current orchestrator context
     * @return The selected script, or null if none available
     */
    SkillScript selectActivity(List<SkillScript> availableScripts, OrchestratorContext context);

    /**
     * Get the weight for a specific activity.
     * Useful for debugging and visualization.
     *
     * @param script The script to evaluate
     * @param context Current context
     * @return Calculated weight (higher = more likely to be selected)
     */
    float calculateWeight(SkillScript script, OrchestratorContext context);

    /**
     * Get explanation for why a script was or wasn't selected.
     * Useful for debugging.
     *
     * @param script The script to explain
     * @param context Current context
     * @return Human-readable explanation
     */
    default String explainWeight(SkillScript script, OrchestratorContext context) {
        return String.format("%s: weight=%.3f", script.getId(), calculateWeight(script, context));
    }
}
