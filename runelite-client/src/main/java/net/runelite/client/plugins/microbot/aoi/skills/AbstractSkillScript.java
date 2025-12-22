package net.runelite.client.plugins.microbot.aoi.skills;

import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.core.TickResult;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;

/**
 * Base implementation for SkillScripts with common functionality.
 */
public abstract class AbstractSkillScript implements SkillScript {

    protected final String id;
    protected final String name;
    protected final SkillCategory category;
    protected final float stressLevel;
    protected final boolean afkFriendly;
    protected final float baseWeight;

    protected AbstractSkillScript(
            String id,
            String name,
            SkillCategory category,
            float stressLevel,
            boolean afkFriendly,
            float baseWeight
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.stressLevel = stressLevel;
        this.afkFriendly = afkFriendly;
        this.baseWeight = baseWeight;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SkillCategory getCategory() {
        return category;
    }

    @Override
    public float getStressLevel() {
        return stressLevel;
    }

    @Override
    public boolean isAfkFriendly() {
        return afkFriendly;
    }

    @Override
    public float getBaseWeight() {
        return baseWeight;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public boolean canStart(OrchestratorContext context) {
        return true;  // Override in subclasses for requirements
    }

    @Override
    public void onStart(OrchestratorContext context) {
        // Override in subclasses if needed
    }

    @Override
    public void onStop(OrchestratorContext context) {
        // Override in subclasses if needed
    }

    @Override
    public float getSynergyWith(SkillScript previousScript) {
        return 1.0f;  // Override for specific synergies
    }
}
