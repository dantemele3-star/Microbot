package net.runelite.client.plugins.microbot.aoi.skills;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.core.TickResult;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Combat skill script - high-stress, failure-prone activity.
 *
 * Used to validate death recovery and combat avoidance behaviors.
 * Higher risk, higher engagement.
 */
public class CombatSkillScript extends AbstractSkillScript {

    private static final float KILL_SUCCESS_RATE = 0.90f;
    private static final float DEATH_RATE = 0.02f;  // 2% chance of death per monster
    private static final int TICKS_PER_KILL = 8;    // ~4.8 seconds per kill

    public CombatSkillScript() {
        super(
                "combat",
                "Combat Training",
                SkillCategory.COMBAT,
                0.7f,       // High stress
                false,      // Not AFK friendly
                0.8f        // Moderate weight
        );
    }

    @Override
    public boolean canInterrupt() {
        // Combat shouldn't be interrupted mid-fight
        return false;
    }

    @Override
    public void onStart(OrchestratorContext context) {
        context.setScriptState("kills", 0);
        context.setScriptState("targetKills", 5 + context.getRandom().nextInt(10)); // 5-15 kills
        context.setScriptState("lastActionTick", context.getCurrentTick());
        context.setScriptState("inCombat", false);
    }

    @Override
    public TickResult tick(OrchestratorContext context) {
        long lastAction = context.getScriptState("lastActionTick", 0L);
        int kills = context.getScriptState("kills", 0);
        int targetKills = context.getScriptState("targetKills", 10);
        boolean inCombat = context.getScriptState("inCombat", false);

        // Check if it's time for an action
        if (context.getCurrentTick() - lastAction < TICKS_PER_KILL) {
            context.setScriptState("inCombat", true);
            return TickResult.WAITING;
        }

        // Check for death first
        if (context.getRandom().nextFloat() < DEATH_RATE) {
            // Death! This triggers death recovery in orchestrator
            return TickResult.INTERRUPTED;
        }

        // Attempt kill
        if (context.getRandom().nextFloat() < KILL_SUCCESS_RATE) {
            kills++;
            context.setScriptState("kills", kills);
        }

        context.setScriptState("lastActionTick", context.getCurrentTick());
        context.setScriptState("inCombat", false);

        // Check completion
        if (kills >= targetKills) {
            return TickResult.COMPLETED;
        }

        // Occasional failure (ran out of food, had to flee)
        if (context.getRandom().nextFloat() < 0.01f) {
            return TickResult.FAILED;
        }

        return TickResult.SUCCESS;
    }

    @Override
    public float getSynergyWith(SkillScript previousScript) {
        // Anti-synergy with idle (rested → ready for combat)
        if (previousScript != null && "idle".equals(previousScript.getId())) {
            return 1.2f;
        }
        // Anti-synergy with more combat (combat fatigue)
        if (previousScript != null && "combat".equals(previousScript.getId())) {
            return 0.7f;
        }
        return 1.0f;
    }
}
