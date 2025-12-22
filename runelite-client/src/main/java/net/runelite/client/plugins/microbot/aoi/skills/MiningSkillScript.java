package net.runelite.client.plugins.microbot.aoi.skills;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.TickResult;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Mining skill script - medium-stress gathering activity.
 *
 * Slightly more engaging than fishing due to rock depletion.
 */
public class MiningSkillScript extends AbstractSkillScript {

    private static final float SUCCESS_RATE = 0.75f;
    private static final int TICKS_PER_ORE = 5;

    public MiningSkillScript() {
        super(
                "mining",
                "Mining",
                SkillCategory.SKILLING,
                0.25f,      // Slightly higher stress than fishing
                true,       // Still AFK friendly
                0.95f
        );
    }

    @Override
    public void onStart(OrchestratorContext context) {
        context.setScriptState("oresMined", 0);
        context.setScriptState("targetOres", 15 + context.getRandom().nextInt(15));
        context.setScriptState("lastActionTick", context.getCurrentTick());
    }

    @Override
    public TickResult tick(OrchestratorContext context) {
        long lastAction = context.getScriptState("lastActionTick", 0L);
        int oresMined = context.getScriptState("oresMined", 0);
        int targetOres = context.getScriptState("targetOres", 20);

        if (context.getCurrentTick() - lastAction < TICKS_PER_ORE) {
            return TickResult.WAITING;
        }

        if (context.getRandom().nextFloat() < SUCCESS_RATE) {
            oresMined++;
            context.setScriptState("oresMined", oresMined);
        }

        context.setScriptState("lastActionTick", context.getCurrentTick());

        if (oresMined >= targetOres) {
            return TickResult.COMPLETED;
        }

        return TickResult.SUCCESS;
    }
}
