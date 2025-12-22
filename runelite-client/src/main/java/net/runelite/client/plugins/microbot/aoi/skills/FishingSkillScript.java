package net.runelite.client.plugins.microbot.aoi.skills;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.core.TickResult;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Fishing skill script - a low-stress gathering activity.
 *
 * Represents various fishing activities (net, rod, harpoon, etc.)
 * AFK-friendly with consistent success rate.
 */
public class FishingSkillScript extends AbstractSkillScript {

    private static final float SUCCESS_RATE = 0.85f;
    private static final float COMPLETION_THRESHOLD = 0.95f;
    private static final int TICKS_PER_ACTION = 4;  // ~2.4 seconds per fish

    public FishingSkillScript() {
        super(
                "fishing",
                "Fishing",
                SkillCategory.SKILLING,
                0.15f,      // Low stress
                true,       // AFK friendly
                1.0f        // Normal weight
        );
    }

    @Override
    public void onStart(OrchestratorContext context) {
        context.setScriptState("fishCaught", 0);
        context.setScriptState("targetFish", 10 + context.getRandom().nextInt(20)); // 10-30 fish
        context.setScriptState("lastActionTick", context.getCurrentTick());
    }

    @Override
    public TickResult tick(OrchestratorContext context) {
        long lastAction = context.getScriptState("lastActionTick", 0L);
        int fishCaught = context.getScriptState("fishCaught", 0);
        int targetFish = context.getScriptState("targetFish", 20);

        // Check if it's time for an action
        if (context.getCurrentTick() - lastAction < TICKS_PER_ACTION) {
            return TickResult.WAITING;
        }

        // Attempt to catch fish
        if (context.getRandom().nextFloat() < SUCCESS_RATE) {
            fishCaught++;
            context.setScriptState("fishCaught", fishCaught);
        }

        context.setScriptState("lastActionTick", context.getCurrentTick());

        // Check completion
        if (fishCaught >= targetFish) {
            return TickResult.COMPLETED;
        }

        // Random early completion (inventory full, got bored, etc.)
        float progress = fishCaught / (float) targetFish;
        if (progress > 0.5f && context.getRandom().nextFloat() < 0.02f) {
            return TickResult.COMPLETED;
        }

        return TickResult.SUCCESS;
    }

    @Override
    public float getSynergyWith(SkillScript previousScript) {
        // Synergy with cooking
        if (previousScript != null && "cooking".equals(previousScript.getId())) {
            return 1.3f;
        }
        return 1.0f;
    }
}
