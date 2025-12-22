package net.runelite.client.plugins.microbot.aoi.skills;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.TickResult;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Woodcutting skill script - low-stress gathering activity.
 *
 * Very AFK friendly, similar to fishing.
 */
public class WoodcuttingSkillScript extends AbstractSkillScript {

    private static final float SUCCESS_RATE = 0.80f;
    private static final int TICKS_PER_LOG = 4;

    public WoodcuttingSkillScript() {
        super(
                "woodcutting",
                "Woodcutting",
                SkillCategory.SKILLING,
                0.1f,       // Very low stress
                true,       // Very AFK friendly
                1.0f
        );
    }

    @Override
    public void onStart(OrchestratorContext context) {
        context.setScriptState("logsCut", 0);
        context.setScriptState("targetLogs", 20 + context.getRandom().nextInt(30));
        context.setScriptState("lastActionTick", context.getCurrentTick());
    }

    @Override
    public TickResult tick(OrchestratorContext context) {
        long lastAction = context.getScriptState("lastActionTick", 0L);
        int logsCut = context.getScriptState("logsCut", 0);
        int targetLogs = context.getScriptState("targetLogs", 30);

        if (context.getCurrentTick() - lastAction < TICKS_PER_LOG) {
            return TickResult.WAITING;
        }

        if (context.getRandom().nextFloat() < SUCCESS_RATE) {
            logsCut++;
            context.setScriptState("logsCut", logsCut);
        }

        context.setScriptState("lastActionTick", context.getCurrentTick());

        if (logsCut >= targetLogs) {
            return TickResult.COMPLETED;
        }

        return TickResult.SUCCESS;
    }
}
