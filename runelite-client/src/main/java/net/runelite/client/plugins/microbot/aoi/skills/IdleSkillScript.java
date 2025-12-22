package net.runelite.client.plugins.microbot.aoi.skills;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.core.TickResult;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Intentional idle/rest activity for fatigue recovery.
 *
 * This represents standing around, chatting, or doing nothing productive.
 * Important for behavioral realism - humans don't optimize constantly.
 */
public class IdleSkillScript extends AbstractSkillScript {

    private static final int MIN_IDLE_TICKS = 50;   // ~30 seconds
    private static final int MAX_IDLE_TICKS = 300;  // ~3 minutes
    private static final float FATIGUE_RECOVERY_RATE = 0.005f;

    public IdleSkillScript() {
        super(
                "idle",
                "Taking a break",
                SkillCategory.IDLE,
                0.0f,       // No stress
                true,       // Very AFK friendly
                0.8f        // Moderate base weight
        );
    }

    @Override
    public boolean canStart(OrchestratorContext context) {
        // Always available
        return true;
    }

    @Override
    public void onStart(OrchestratorContext context) {
        // Determine how long to idle based on fatigue
        float fatigue = context.getFatigue();
        int baseTicks = MIN_IDLE_TICKS + (int) ((MAX_IDLE_TICKS - MIN_IDLE_TICKS) * fatigue);

        // Add some randomness
        int variance = context.getRandom().nextInt(50) - 25;
        int targetTicks = Math.max(MIN_IDLE_TICKS, baseTicks + variance);

        context.setScriptState("targetTicks", targetTicks);
        context.setScriptState("startTick", context.getCurrentTick());
    }

    @Override
    public TickResult tick(OrchestratorContext context) {
        long startTick = context.getScriptState("startTick", context.getCurrentTick());
        int targetTicks = context.getScriptState("targetTicks", MIN_IDLE_TICKS);

        long ticksElapsed = context.getCurrentTick() - startTick;

        // Recover fatigue while idle
        float currentFatigue = context.getFatigue();
        context.setFatigue(Math.max(0.0f, currentFatigue - FATIGUE_RECOVERY_RATE));

        // Check if done idling
        if (ticksElapsed >= targetTicks) {
            return TickResult.COMPLETED;
        }

        return TickResult.IDLE;
    }

    @Override
    public float getSynergyWith(SkillScript previousScript) {
        // Idle is more likely after stressful activities
        if (previousScript != null && previousScript.getStressLevel() > 0.5f) {
            return 1.3f;
        }
        return 1.0f;
    }
}
