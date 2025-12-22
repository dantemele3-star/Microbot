package net.runelite.client.plugins.microbot.aoi.skills;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.core.TickResult;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Cooking skill script - low-stress artisan activity.
 *
 * Has natural synergy with fishing.
 */
public class CookingSkillScript extends AbstractSkillScript {

    private static final float SUCCESS_RATE = 0.80f;  // Can burn food
    private static final int TICKS_PER_ACTION = 3;    // ~1.8 seconds per item

    public CookingSkillScript() {
        super(
                "cooking",
                "Cooking",
                SkillCategory.SKILLING,
                0.2f,       // Low-moderate stress (burning is frustrating)
                true,       // AFK friendly
                0.9f        // Slightly lower weight than fishing
        );
    }

    @Override
    public void onStart(OrchestratorContext context) {
        context.setScriptState("itemsCooked", 0);
        context.setScriptState("itemsBurned", 0);
        context.setScriptState("targetItems", 10 + context.getRandom().nextInt(18)); // 10-28 items
        context.setScriptState("lastActionTick", context.getCurrentTick());
    }

    @Override
    public TickResult tick(OrchestratorContext context) {
        long lastAction = context.getScriptState("lastActionTick", 0L);
        int itemsCooked = context.getScriptState("itemsCooked", 0);
        int itemsBurned = context.getScriptState("itemsBurned", 0);
        int targetItems = context.getScriptState("targetItems", 20);

        // Check if it's time for an action
        if (context.getCurrentTick() - lastAction < TICKS_PER_ACTION) {
            return TickResult.WAITING;
        }

        // Attempt to cook
        if (context.getRandom().nextFloat() < SUCCESS_RATE) {
            itemsCooked++;
            context.setScriptState("itemsCooked", itemsCooked);
        } else {
            itemsBurned++;
            context.setScriptState("itemsBurned", itemsBurned);
        }

        context.setScriptState("lastActionTick", context.getCurrentTick());

        int totalProcessed = itemsCooked + itemsBurned;

        // Check completion
        if (totalProcessed >= targetItems) {
            // Fail if burned too many (>30%)
            if (itemsBurned / (float) totalProcessed > 0.3f) {
                return TickResult.FAILED;
            }
            return TickResult.COMPLETED;
        }

        return TickResult.SUCCESS;
    }

    @Override
    public float getSynergyWith(SkillScript previousScript) {
        // Strong synergy with fishing (fish → cook)
        if (previousScript != null && "fishing".equals(previousScript.getId())) {
            return 1.5f;
        }
        return 1.0f;
    }
}
