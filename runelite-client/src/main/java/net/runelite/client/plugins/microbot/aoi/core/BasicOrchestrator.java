package net.runelite.client.plugins.microbot.aoi.core;

import net.runelite.client.plugins.microbot.aoi.burnout.BurnoutStateMachine;
import net.runelite.client.plugins.microbot.aoi.decision.*;
import net.runelite.client.plugins.microbot.aoi.identity.IdentityDriftModel;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;
import net.runelite.client.plugins.microbot.aoi.memory.*;
import net.runelite.client.plugins.microbot.aoi.memory.belief.BeliefSystem;
import net.runelite.client.plugins.microbot.aoi.social.SocialMemory;

import java.util.*;

/**
 * Central brain controlling all AOI behavior.
 *
 * The orchestrator:
 * - Manages session lifecycle (start, tick, end)
 * - Owns all memory systems
 * - Controls script selection and switching
 * - Interprets outcomes into narratives
 * - Triggers sleep consolidation
 *
 * Philosophy:
 * - Scripts report outcomes, don't assert them
 * - Orchestrator decides what counts as an activity
 * - Single source of truth for all state
 */
public class BasicOrchestrator {

    // Configuration
    private static final int MAX_SESSION_TICKS = 12000;    // ~2 hours max
    private static final int MIN_SCRIPT_TICKS = 100;       // ~1 minute minimum per script
    private static final float FATIGUE_RATE = 0.001f;      // Per tick fatigue increase
    private static final float STRESS_DECAY = 0.0005f;     // Per tick stress decay

    // Core components
    private final OrchestratorContext context;
    private final ActivityDecider decider;

    // Memory systems (owned by orchestrator)
    private final NarrativeStore narrativeStore;
    private final BeliefSystem beliefSystem;
    private final FailureMemory failureMemory;
    private final RegretMemory regretMemory;
    private final HabitModel habitModel;
    private final IdentityDriftModel identityDrift;
    private final BurnoutStateMachine burnoutMachine;
    private final SocialMemory socialMemory;
    private final SleepConsolidation sleepConsolidation;

    // Available scripts
    private final List<SkillScript> availableScripts;

    // Session state
    private boolean sessionActive;
    private int ticksThisSession;
    private int scriptSwitchCount;
    private float sessionFatigueSum;
    private int frustrationEvents;
    private int noveltyEvents;

    public BasicOrchestrator(long accountSeed, String accountName) {
        Random random = new Random(accountSeed);

        // Create context
        this.context = new OrchestratorContext(accountSeed, accountName);

        // Create memory systems
        this.narrativeStore = new NarrativeStore();
        this.beliefSystem = new BeliefSystem(narrativeStore);
        this.failureMemory = new FailureMemory();
        this.regretMemory = new RegretMemory();
        this.habitModel = new HabitModel();
        this.identityDrift = new IdentityDriftModel();
        this.burnoutMachine = new BurnoutStateMachine();
        this.socialMemory = new SocialMemory();

        // Create sleep consolidation and register all systems
        this.sleepConsolidation = new SleepConsolidation();
        sleepConsolidation.register(narrativeStore);
        sleepConsolidation.register(beliefSystem);
        sleepConsolidation.register(failureMemory);
        sleepConsolidation.register(regretMemory);
        sleepConsolidation.register(habitModel);
        sleepConsolidation.register(identityDrift);
        sleepConsolidation.register(burnoutMachine);
        sleepConsolidation.register(socialMemory);

        // Create decider
        this.decider = new WeightedActivityDecider(
                failureMemory,
                regretMemory,
                habitModel,
                identityDrift,
                narrativeStore,
                beliefSystem,
                burnoutMachine,
                socialMemory,
                random
        );

        // Initialize
        this.availableScripts = new ArrayList<>();
        this.sessionActive = false;
    }

    // === Session Management ===

    /**
     * Start a new play session.
     *
     * @param scripts Available scripts for this session
     */
    public void startSession(List<SkillScript> scripts) {
        availableScripts.clear();
        availableScripts.addAll(scripts);

        context.resetSession();
        sessionActive = true;
        ticksThisSession = 0;
        scriptSwitchCount = 0;
        sessionFatigueSum = 0.0f;
        frustrationEvents = 0;
        noveltyEvents = 0;

        // Select initial activity
        selectNextActivity();

        // Record session start narrative
        recordNarrative(
                NarrativeType.ROUTINE,
                MemorySubject.event("session_start"),
                0.1f,
                "Started a new session"
        );
    }

    /**
     * Execute a single tick of the orchestrator.
     *
     * @return true if session should continue, false if it should end
     */
    public boolean tick() {
        if (!sessionActive) {
            return false;
        }

        // Update tick counters
        context.advanceTick();
        ticksThisSession++;
        updateSystemTicks();

        // Check session limits
        if (shouldEndSession()) {
            endSession();
            return false;
        }

        // Execute current script
        SkillScript current = context.getCurrentScript();
        if (current == null) {
            selectNextActivity();
            return sessionActive;
        }

        // Run script tick
        TickResult result = current.tick(context);

        // Process result
        processTickResult(current, result);

        // Update fatigue and stress
        updateFatigueAndStress(current);

        // Check if script should switch
        if (shouldSwitchScript(result)) {
            stopCurrentScript(result);
            selectNextActivity();
        }

        return sessionActive;
    }

    /**
     * End the current session.
     */
    public void endSession() {
        if (!sessionActive) return;

        // Stop current script
        SkillScript current = context.getCurrentScript();
        if (current != null) {
            current.onStop(context);
        }

        // Update burnout
        float avgFatigue = ticksThisSession > 0 ?
                sessionFatigueSum / ticksThisSession : 0.0f;
        burnoutMachine.updateDaily(avgFatigue, frustrationEvents, noveltyEvents);

        // Run sleep consolidation
        sleepConsolidation.consolidate(1.0f);

        // Record session end
        recordNarrative(
                NarrativeType.REST,
                MemorySubject.event("session_end"),
                0.0f,
                "Ended session after " + ticksThisSession + " ticks"
        );

        sessionActive = false;
    }

    // === Script Management ===

    private void selectNextActivity() {
        SkillScript selected = decider.selectActivity(availableScripts, context);

        if (selected == null) {
            // No valid activity - end session
            endSession();
            return;
        }

        // Start new script
        context.setCurrentScript(selected);
        selected.onStart(context);
        scriptSwitchCount++;

        // Check for novelty
        MemorySubject subject = MemorySubject.skill(selected.getId());
        if (habitModel.getHabitStrength(subject) < 0.2f) {
            noveltyEvents++;
        }
    }

    private void stopCurrentScript(TickResult result) {
        SkillScript current = context.getCurrentScript();
        if (current == null) return;

        current.onStop(context);

        // Record completion narrative based on result
        MemorySubject subject = MemorySubject.skill(current.getId());

        switch (result) {
            case COMPLETED:
                recordNarrative(
                        NarrativeType.ACHIEVEMENT,
                        subject,
                        0.3f,
                        "Completed " + current.getName() + " successfully"
                );
                context.recordActivityCompletion(current.getId(), current.getCategory());
                identityDrift.recordActivity(current.getCategory());
                habitModel.recordActivity(subject);
                failureMemory.recordSuccess(current.getId());
                break;

            case FAILED:
                recordNarrative(
                        NarrativeType.FAILURE,
                        subject,
                        -0.4f,
                        "Failed at " + current.getName()
                );
                context.recordFailure();
                failureMemory.recordFailure(current.getId());
                frustrationEvents++;
                break;

            case INTERRUPTED:
                recordNarrative(
                        NarrativeType.INTERRUPTION,
                        subject,
                        -0.1f,
                        "Was interrupted during " + current.getName()
                );
                break;

            default:
                // WAITING, SUCCESS, IDLE - normal transition
                break;
        }
    }

    private boolean shouldSwitchScript(TickResult result) {
        // Definite switch conditions
        if (result == TickResult.COMPLETED || result == TickResult.FAILED) {
            return true;
        }

        // Time-based switch (avoid infinite loops)
        long ticksOnScript = context.getTicksSinceScriptStart();
        if (ticksOnScript >= MIN_SCRIPT_TICKS) {
            // Random chance to switch increases with time
            float switchChance = (ticksOnScript - MIN_SCRIPT_TICKS) / 5000.0f;
            switchChance = Math.min(0.3f, switchChance);  // Max 30% per tick

            if (context.getRandom().nextFloat() < switchChance) {
                return true;
            }
        }

        return false;
    }

    // === Result Processing ===

    private void processTickResult(SkillScript script, TickResult result) {
        MemorySubject subject = MemorySubject.skill(script.getId());

        switch (result) {
            case SUCCESS:
                // Minor positive narrative occasionally
                if (context.getRandom().nextFloat() < 0.05f) {
                    recordNarrative(
                            NarrativeType.ENJOYMENT,
                            subject,
                            0.1f,
                            "Making progress with " + script.getName()
                    );
                }
                break;

            case FAILED:
                // Already handled in stopCurrentScript
                break;

            case WAITING:
                // No narrative for waiting
                break;

            case IDLE:
                // Record as rest if fatigue high
                if (context.getFatigue() > 0.6f) {
                    if (context.getRandom().nextFloat() < 0.1f) {
                        recordNarrative(
                                NarrativeType.REST,
                                subject,
                                0.2f,
                                "Taking a break to recover"
                        );
                    }
                }
                break;

            case INTERRUPTED:
                // Handle death if combat
                if (script.getCategory() == SkillCategory.COMBAT) {
                    context.recordDeath();
                    recordNarrative(
                            NarrativeType.DEATH,
                            subject,
                            -0.8f,
                            "Died during combat"
                    );
                    regretMemory.recordRegret(subject, 0.6f);
                }
                break;

            default:
                break;
        }
    }

    // === Fatigue & Stress ===

    private void updateFatigueAndStress(SkillScript script) {
        // Accumulate fatigue
        float fatigueIncrease = FATIGUE_RATE * (1.0f + script.getStressLevel());
        context.addFatigue(fatigueIncrease);
        sessionFatigueSum += context.getFatigue();

        // Update stress toward script's stress level
        float currentStress = context.getStress();
        float targetStress = script.getStressLevel();
        float stressDelta = (targetStress - currentStress) * 0.1f;
        context.setStress(Math.max(0.0f, currentStress + stressDelta - STRESS_DECAY));
    }

    // === Session Limits ===

    private boolean shouldEndSession() {
        // Max ticks
        if (ticksThisSession >= MAX_SESSION_TICKS) {
            return true;
        }

        // Session length based on burnout phase
        int maxTicks = (int) (context.getMaxSessionMinutes() * 100 *
                burnoutMachine.getSessionLengthModifier());
        if (ticksThisSession >= maxTicks) {
            return true;
        }

        // Extreme fatigue
        if (context.getFatigue() >= 0.95f) {
            return true;
        }

        return false;
    }

    // === Narrative Recording ===

    private void recordNarrative(NarrativeType type, MemorySubject subject, float impact, String narrative) {
        narrativeStore.record(type, subject, impact, narrative);
    }

    // === System Updates ===

    private void updateSystemTicks() {
        long tick = context.getCurrentTick();
        narrativeStore.setCurrentTick(tick);
        failureMemory.setCurrentTick(tick);
        habitModel.setCurrentTick(tick);
        socialMemory.setCurrentTick(tick);
        beliefSystem.setCurrentTick(tick);
    }

    // === Getters ===

    public OrchestratorContext getContext() {
        return context;
    }

    public IdentityDriftModel getIdentityDrift() {
        return identityDrift;
    }

    public BurnoutStateMachine getBurnoutMachine() {
        return burnoutMachine;
    }

    public NarrativeStore getNarrativeStore() {
        return narrativeStore;
    }

    public BeliefSystem getBeliefSystem() {
        return beliefSystem;
    }

    public FailureMemory getFailureMemory() {
        return failureMemory;
    }

    public RegretMemory getRegretMemory() {
        return regretMemory;
    }

    public HabitModel getHabitModel() {
        return habitModel;
    }

    public SocialMemory getSocialMemory() {
        return socialMemory;
    }

    public SleepConsolidation getSleepConsolidation() {
        return sleepConsolidation;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public int getTicksThisSession() {
        return ticksThisSession;
    }

    public int getScriptSwitchCount() {
        return scriptSwitchCount;
    }

    public ActivityDecider getDecider() {
        return decider;
    }

    /**
     * Get status summary for debugging/UI.
     */
    public String getStatusSummary() {
        SkillScript current = context.getCurrentScript();
        return String.format(
                "Session: %s | Ticks: %d | Fatigue: %.2f | Stress: %.2f | Script: %s | Phase: %s | Archetype: %s",
                sessionActive ? "Active" : "Inactive",
                ticksThisSession,
                context.getFatigue(),
                context.getStress(),
                current != null ? current.getName() : "None",
                burnoutMachine.getCurrentPhase().getDisplayName(),
                identityDrift.getArchetype().getDisplayName()
        );
    }
}
