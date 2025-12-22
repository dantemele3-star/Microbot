package net.runelite.client.plugins.microbot.aoi;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("aoi")
public interface AoiConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "General AOI settings",
            position = 0
    )
    String generalSection = "general";

    @ConfigItem(
            keyName = "enabled",
            name = "Enable AOI",
            description = "Enable the AOI orchestrator",
            position = 1,
            section = generalSection
    )
    default boolean enabled() {
        return false;
    }

    @ConfigItem(
            keyName = "accountSeed",
            name = "Account Seed",
            description = "Unique seed for this account's personality (use different seeds for different accounts)",
            position = 2,
            section = generalSection
    )
    default long accountSeed() {
        return System.currentTimeMillis();
    }

    @ConfigItem(
            keyName = "accountName",
            name = "Account Name",
            description = "Name for this account (for logging/display)",
            position = 3,
            section = generalSection
    )
    default String accountName() {
        return "AOI Account";
    }

    @ConfigSection(
            name = "Session",
            description = "Session behavior settings",
            position = 10
    )
    String sessionSection = "session";

    @ConfigItem(
            keyName = "maxSessionMinutes",
            name = "Max Session (minutes)",
            description = "Maximum session length in minutes",
            position = 11,
            section = sessionSection
    )
    default int maxSessionMinutes() {
        return 120;
    }

    @ConfigItem(
            keyName = "weekendBonus",
            name = "Weekend Bonus",
            description = "Allow longer sessions on weekends",
            position = 12,
            section = sessionSection
    )
    default boolean weekendBonus() {
        return true;
    }

    @ConfigSection(
            name = "Behavior",
            description = "Behavioral tuning",
            position = 20
    )
    String behaviorSection = "behavior";

    @ConfigItem(
            keyName = "explorationRate",
            name = "Exploration Rate",
            description = "How often to try unfamiliar activities (0.0 - 1.0)",
            position = 21,
            section = behaviorSection
    )
    default double explorationRate() {
        return 0.15;
    }

    @ConfigItem(
            keyName = "irrationalityFactor",
            name = "Irrationality Factor",
            description = "Random noise in decision-making (0.0 - 0.5)",
            position = 22,
            section = behaviorSection
    )
    default double irrationalityFactor() {
        return 0.1;
    }

    @ConfigItem(
            keyName = "preferAfkWhenTired",
            name = "Prefer AFK When Tired",
            description = "Prefer low-stress activities when fatigued",
            position = 23,
            section = behaviorSection
    )
    default boolean preferAfkWhenTired() {
        return true;
    }

    @ConfigSection(
            name = "Skills",
            description = "Skill activity settings",
            position = 30
    )
    String skillsSection = "skills";

    @ConfigItem(
            keyName = "enableFishing",
            name = "Enable Fishing",
            description = "Include fishing in available activities",
            position = 31,
            section = skillsSection
    )
    default boolean enableFishing() {
        return true;
    }

    @ConfigItem(
            keyName = "enableCooking",
            name = "Enable Cooking",
            description = "Include cooking in available activities",
            position = 32,
            section = skillsSection
    )
    default boolean enableCooking() {
        return true;
    }

    @ConfigItem(
            keyName = "enableMining",
            name = "Enable Mining",
            description = "Include mining in available activities",
            position = 33,
            section = skillsSection
    )
    default boolean enableMining() {
        return true;
    }

    @ConfigItem(
            keyName = "enableWoodcutting",
            name = "Enable Woodcutting",
            description = "Include woodcutting in available activities",
            position = 34,
            section = skillsSection
    )
    default boolean enableWoodcutting() {
        return true;
    }

    @ConfigItem(
            keyName = "enableCombat",
            name = "Enable Combat",
            description = "Include combat in available activities",
            position = 35,
            section = skillsSection
    )
    default boolean enableCombat() {
        return true;
    }

    @ConfigSection(
            name = "Debug",
            description = "Debug and development options",
            position = 90
    )
    String debugSection = "debug";

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Show AOI status overlay",
            position = 91,
            section = debugSection
    )
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "verboseLogging",
            name = "Verbose Logging",
            description = "Enable detailed logging for debugging",
            position = 92,
            section = debugSection
    )
    default boolean verboseLogging() {
        return false;
    }

    @ConfigItem(
            keyName = "simulationMode",
            name = "Simulation Mode",
            description = "Run in simulation mode (no actual game actions)",
            position = 93,
            section = debugSection
    )
    default boolean simulationMode() {
        return false;
    }
}
