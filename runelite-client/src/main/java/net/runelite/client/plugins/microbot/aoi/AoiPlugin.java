package net.runelite.client.plugins.microbot.aoi;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aoi.core.BasicOrchestrator;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.skills.*;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * AOI (Autonomous Orchestrated Intelligence) Plugin.
 *
 * A cognitive architecture for human-like bot behavior with:
 * - Memory and belief systems
 * - Emotional associations
 * - Identity emergence
 * - Burnout and recovery cycles
 * - Social awareness
 * - Personality inheritance
 *
 * This is not a traditional bot - it's an identity simulator.
 */
@PluginDescriptor(
        name = PluginDescriptor.Default + "AOI Framework",
        description = "Autonomous Orchestrated Intelligence - Cognitive bot framework with emergent identity",
        tags = {"aoi", "autonomous", "cognitive", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class AoiPlugin extends Plugin {

    @Inject
    private AoiConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AoiOverlay overlay;

    private BasicOrchestrator orchestrator;
    private boolean initialized = false;

    @Override
    protected void startUp() {
        log.info("AOI Framework starting up...");

        if (config.showOverlay()) {
            overlayManager.add(overlay);
        }

        // Initialize orchestrator with config values
        initializeOrchestrator();

        log.info("AOI Framework started. Seed: {}, Account: {}",
                config.accountSeed(), config.accountName());
    }

    @Override
    protected void shutDown() {
        log.info("AOI Framework shutting down...");

        overlayManager.remove(overlay);

        if (orchestrator != null && orchestrator.isSessionActive()) {
            orchestrator.endSession();
        }

        initialized = false;
        log.info("AOI Framework stopped.");
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.enabled()) {
            return;
        }

        if (!Microbot.isLoggedIn()) {
            if (orchestrator != null && orchestrator.isSessionActive()) {
                log.info("Logged out - ending AOI session");
                orchestrator.endSession();
            }
            return;
        }

        // Initialize on first logged-in tick if needed
        if (!initialized) {
            initializeOrchestrator();
            initialized = true;
        }

        // Start session if not active
        if (!orchestrator.isSessionActive()) {
            List<SkillScript> scripts = buildScriptList();
            if (!scripts.isEmpty()) {
                log.info("Starting new AOI session with {} scripts", scripts.size());
                orchestrator.startSession(scripts);
            }
            return;
        }

        // Execute tick
        boolean continueSession = orchestrator.tick();

        if (!continueSession) {
            log.info("AOI session ended naturally");
        }

        // Verbose logging if enabled
        if (config.verboseLogging()) {
            log.debug(orchestrator.getStatusSummary());
        }
    }

    private void initializeOrchestrator() {
        orchestrator = new BasicOrchestrator(
                config.accountSeed(),
                config.accountName()
        );
        log.info("Orchestrator initialized for account: {}", config.accountName());
    }

    private List<SkillScript> buildScriptList() {
        List<SkillScript> scripts = new ArrayList<>();

        // Always include idle
        scripts.add(new IdleSkillScript());

        // Add configured skills
        if (config.enableFishing()) {
            scripts.add(new FishingSkillScript());
        }
        if (config.enableCooking()) {
            scripts.add(new CookingSkillScript());
        }
        if (config.enableMining()) {
            scripts.add(new MiningSkillScript());
        }
        if (config.enableWoodcutting()) {
            scripts.add(new WoodcuttingSkillScript());
        }
        if (config.enableCombat()) {
            scripts.add(new CombatSkillScript());
        }

        return scripts;
    }

    @Provides
    AoiConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AoiConfig.class);
    }

    // === Public Accessors for Overlay ===

    public BasicOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
