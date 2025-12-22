package net.runelite.client.plugins.microbot.aoi;

import net.runelite.client.plugins.microbot.aoi.burnout.BurnoutPhase;
import net.runelite.client.plugins.microbot.aoi.core.BasicOrchestrator;
import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.identity.AccountArchetype;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

/**
 * Overlay displaying AOI status information.
 */
public class AoiOverlay extends OverlayPanel {

    private final AoiPlugin plugin;
    private final AoiConfig config;

    @Inject
    public AoiOverlay(AoiPlugin plugin, AoiConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        BasicOrchestrator orchestrator = plugin.getOrchestrator();
        if (orchestrator == null) {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AOI: Not Initialized")
                    .color(Color.GRAY)
                    .build());
            return super.render(graphics);
        }

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("AOI Framework")
                .color(Color.CYAN)
                .build());

        // Session status
        boolean active = orchestrator.isSessionActive();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Session:")
                .right(active ? "Active" : "Inactive")
                .rightColor(active ? Color.GREEN : Color.RED)
                .build());

        if (!active) {
            return super.render(graphics);
        }

        OrchestratorContext context = orchestrator.getContext();
        SkillScript current = context.getCurrentScript();

        // Current activity
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Activity:")
                .right(current != null ? current.getName() : "None")
                .build());

        // Fatigue bar
        float fatigue = context.getFatigue();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Fatigue:")
                .right(String.format("%.0f%%", fatigue * 100))
                .rightColor(getFatigueColor(fatigue))
                .build());

        // Stress bar
        float stress = context.getStress();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Stress:")
                .right(String.format("%.0f%%", stress * 100))
                .rightColor(getStressColor(stress))
                .build());

        // Burnout phase
        BurnoutPhase phase = orchestrator.getBurnoutMachine().getCurrentPhase();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Phase:")
                .right(phase.getDisplayName())
                .rightColor(getPhaseColor(phase))
                .build());

        // Archetype
        AccountArchetype archetype = orchestrator.getIdentityDrift().getArchetype();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Archetype:")
                .right(archetype.getDisplayName())
                .build());

        // Session stats
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Ticks:")
                .right(String.valueOf(orchestrator.getTicksThisSession()))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Switches:")
                .right(String.valueOf(orchestrator.getScriptSwitchCount()))
                .build());

        // Identity entropy
        float entropy = orchestrator.getIdentityDrift().calculateEntropy();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Entropy:")
                .right(String.format("%.2f", entropy))
                .rightColor(getEntropyColor(entropy))
                .build());

        return super.render(graphics);
    }

    private Color getFatigueColor(float fatigue) {
        if (fatigue < 0.3f) return Color.GREEN;
        if (fatigue < 0.6f) return Color.YELLOW;
        if (fatigue < 0.8f) return Color.ORANGE;
        return Color.RED;
    }

    private Color getStressColor(float stress) {
        if (stress < 0.3f) return Color.GREEN;
        if (stress < 0.5f) return Color.YELLOW;
        if (stress < 0.7f) return Color.ORANGE;
        return Color.RED;
    }

    private Color getPhaseColor(BurnoutPhase phase) {
        switch (phase) {
            case ENGAGED: return Color.GREEN;
            case TIRED: return Color.YELLOW;
            case DISENGAGED: return Color.RED;
            case RECOVERING: return Color.CYAN;
            default: return Color.WHITE;
        }
    }

    private Color getEntropyColor(float entropy) {
        if (entropy > 2.0f) return Color.CYAN;   // Exploratory
        if (entropy > 1.2f) return Color.WHITE;  // Directed
        return Color.ORANGE;                      // Specialized
    }
}
