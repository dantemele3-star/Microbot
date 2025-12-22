package net.runelite.client.plugins.microbot.aoi.decision;

import net.runelite.client.plugins.microbot.aoi.burnout.BurnoutStateMachine;
import net.runelite.client.plugins.microbot.aoi.core.OrchestratorContext;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.identity.IdentityDriftModel;
import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;
import net.runelite.client.plugins.microbot.aoi.memory.*;
import net.runelite.client.plugins.microbot.aoi.memory.belief.BeliefSystem;
import net.runelite.client.plugins.microbot.aoi.social.SocialInfluenceCalculator;
import net.runelite.client.plugins.microbot.aoi.social.SocialMemory;

import java.util.*;

/**
 * Weighted activity selection with all 34 decision factors.
 *
 * Decision Factors:
 * 1-5:   Basic constraints (time-box, login state, skill cooldowns)
 * 6-10:  Fatigue and stress (current fatigue, stress level, AFK preference)
 * 11-15: Recent activity (repetition penalty, failure memory, synergy)
 * 16-19: Schedule and self-audit (weekday/weekend, efficiency warnings)
 * 20:    Regret memory (avoidance with reconciliation)
 * 21:    Identity drift (long-term specialization)
 * 22:    Habit bias (compressed memory)
 * 23-24: Mood influence & irrationality
 * 25:    Autobiographical emotion
 * 26:    Emotional associations (derived from beliefs)
 * 27:    Social influence (friends/clan)
 * 28-30: Burnout modifiers (phase-based)
 * 31:    Belief bias (compressed narratives)
 * 32-33: Phase idle boost & goal suppression
 * 34:    Phase exploration ratio
 */
public class WeightedActivityDecider implements ActivityDecider {

    // Core systems
    private final FailureMemory failureMemory;
    private final RegretMemory regretMemory;
    private final HabitModel habitModel;
    private final IdentityDriftModel identityDrift;
    private final NarrativeStore narrativeStore;
    private final BeliefSystem beliefSystem;
    private final BurnoutStateMachine burnoutMachine;
    private final SocialMemory socialMemory;
    private final SocialInfluenceCalculator socialInfluence;
    private final DerivedEmotions derivedEmotions;

    // Configuration
    private final Random random;
    private float explorationRate = 0.15f;    // Base exploration rate
    private float irrationalityFactor = 0.1f; // Random noise in decisions

    // Nearby players (updated externally)
    private List<String> nearbyPlayers = new ArrayList<>();

    public WeightedActivityDecider(
            FailureMemory failureMemory,
            RegretMemory regretMemory,
            HabitModel habitModel,
            IdentityDriftModel identityDrift,
            NarrativeStore narrativeStore,
            BeliefSystem beliefSystem,
            BurnoutStateMachine burnoutMachine,
            SocialMemory socialMemory,
            Random random
    ) {
        this.failureMemory = failureMemory;
        this.regretMemory = regretMemory;
        this.habitModel = habitModel;
        this.identityDrift = identityDrift;
        this.narrativeStore = narrativeStore;
        this.beliefSystem = beliefSystem;
        this.burnoutMachine = burnoutMachine;
        this.socialMemory = socialMemory;
        this.random = random;

        this.socialInfluence = new SocialInfluenceCalculator(socialMemory);
        this.derivedEmotions = new DerivedEmotions(beliefSystem, narrativeStore);
    }

    @Override
    public SkillScript selectActivity(List<SkillScript> availableScripts, OrchestratorContext context) {
        if (availableScripts.isEmpty()) {
            return null;
        }

        // Filter to only runnable scripts
        List<SkillScript> runnable = new ArrayList<>();
        Map<SkillScript, Float> weights = new HashMap<>();

        for (SkillScript script : availableScripts) {
            if (script.canStart(context)) {
                runnable.add(script);
                weights.put(script, calculateWeight(script, context));
            }
        }

        if (runnable.isEmpty()) {
            return null;
        }

        // Check for exploration (try something new)
        float currentExplorationRate = burnoutMachine.getExplorationRate();
        if (random.nextFloat() < currentExplorationRate) {
            // Pick a random activity (weighted toward unfamiliar)
            return selectExploratoryActivity(runnable, context);
        }

        // Weighted random selection
        return weightedRandomSelect(runnable, weights);
    }

    @Override
    public float calculateWeight(SkillScript script, OrchestratorContext context) {
        float weight = script.getBaseWeight();

        // === Basic Constraints (Factors 1-5) ===

        // Factor 1-2: Login/availability (handled by canStart)

        // Factor 3: Failure cooldown
        weight *= failureMemory.getFailureWeight(script.getId());

        // Factor 4-5: Time constraints (handled by context)

        // === Fatigue and Stress (Factors 6-10) ===

        // Factor 6: Current fatigue
        float fatigue = context.getFatigue();
        if (fatigue > 0.7f) {
            // High fatigue: strongly prefer AFK activities
            if (script.isAfkFriendly()) {
                weight *= 1.5f;
            } else {
                weight *= 0.5f;
            }
        }

        // Factor 7: Stress level
        float stress = context.getStress();
        if (stress > 0.6f) {
            // High stress: avoid high-stress activities
            weight *= (1.0f - script.getStressLevel() * 0.5f);
        }

        // Factor 8-10: AFK preference based on fatigue/stress
        if (script.isAfkFriendly() && (fatigue > 0.5f || stress > 0.5f)) {
            weight *= 1.2f;
        }

        // === Recent Activity (Factors 11-15) ===

        // Factor 11: Repetition penalty
        long ticksSinceActivity = context.getTicksSinceActivity(script.getId());
        if (ticksSinceActivity < 1000) {  // Less than ~10 minutes ago
            weight *= 0.7f;  // Reduce preference for very recent activities
        }

        // Factor 12: Failure memory (already applied above)

        // Factor 13-14: Synergy with previous activity
        if (context.getPreviousScript() != null) {
            weight *= script.getSynergyWith(context.getPreviousScript());
        }

        // Factor 15: Death recovery
        if (context.hadRecentDeath() && script.getCategory() == SkillCategory.COMBAT) {
            weight *= 0.3f;  // Strong avoidance of combat after death
        }

        // === Schedule (Factors 16-19) ===

        // Factor 16-17: Weekend vs weekday (affects session length, not weights)

        // Factor 18-19: Self-audit (would check efficiency, not implemented yet)

        // === Memory Systems (Factors 20-26) ===

        // Factor 20: Regret memory
        MemorySubject subject = MemorySubject.skill(script.getId());
        weight *= regretMemory.getRegretWeight(subject);

        // Factor 21: Identity drift
        float identityWeight = identityDrift.getWeight(script.getCategory());
        weight *= (0.5f + identityWeight);  // 0.5 - 1.5 range

        // Factor 22: Habit bias
        weight *= habitModel.getHabitWeight(subject);

        // Factor 23-24: Mood/irrationality
        weight *= (1.0f + (random.nextFloat() - 0.5f) * irrationalityFactor * 2);

        // Factor 25-26: Emotional associations
        if (derivedEmotions.hasEmotions(subject)) {
            EmotionalAssociation emotions = derivedEmotions.derive(subject);
            weight *= (1.0f + emotions.getPreferenceWeight() * 0.3f);

            // Strong avoidance if should avoid
            if (emotions.shouldAvoid()) {
                weight *= 0.2f;
            }
        }

        // === Social (Factor 27) ===
        float socialModifier = socialInfluence.calculateInfluence(script.getCategory(), nearbyPlayers);
        weight *= socialModifier;

        // === Burnout (Factors 28-34) ===

        // Factor 28-30: Burnout modifiers
        if (script.isAfkFriendly()) {
            weight *= burnoutMachine.getIdleWeightModifier();
        }

        // Factor 31: Belief bias
        if (beliefSystem.hasBelief(subject)) {
            float beliefSentiment = beliefSystem.getBelief(subject).getWeightedSentiment();
            weight *= (1.0f + beliefSentiment * 0.2f);
        }

        // Factor 32-33: Quest suppression in burnout
        if (script.getCategory() == SkillCategory.QUESTING) {
            weight *= burnoutMachine.getQuestSuppressionFactor();
        }

        // Factor 34: Goal suppression in disengaged phase
        if (burnoutMachine.shouldSuppressGoals() && !script.isAfkFriendly()) {
            weight *= 0.5f;
        }

        return Math.max(0.01f, weight);  // Never go to zero
    }

    @Override
    public String explainWeight(SkillScript script, OrchestratorContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s (category=%s, stress=%.2f):\n",
                script.getId(), script.getCategory(), script.getStressLevel()));

        MemorySubject subject = MemorySubject.skill(script.getId());

        sb.append(String.format("  Base weight: %.3f\n", script.getBaseWeight()));
        sb.append(String.format("  Failure weight: %.3f\n", failureMemory.getFailureWeight(script.getId())));
        sb.append(String.format("  Regret weight: %.3f\n", regretMemory.getRegretWeight(subject)));
        sb.append(String.format("  Identity weight: %.3f\n", identityDrift.getWeight(script.getCategory())));
        sb.append(String.format("  Habit weight: %.3f\n", habitModel.getHabitWeight(subject)));
        sb.append(String.format("  Burnout idle modifier: %.3f\n", burnoutMachine.getIdleWeightModifier()));
        sb.append(String.format("  FINAL WEIGHT: %.3f\n", calculateWeight(script, context)));

        return sb.toString();
    }

    // === Weighted Selection ===

    private SkillScript weightedRandomSelect(List<SkillScript> scripts, Map<SkillScript, Float> weights) {
        float totalWeight = 0.0f;
        for (float w : weights.values()) {
            totalWeight += w;
        }

        if (totalWeight <= 0.0f) {
            return scripts.get(random.nextInt(scripts.size()));
        }

        float roll = random.nextFloat() * totalWeight;
        float cumulative = 0.0f;

        for (SkillScript script : scripts) {
            cumulative += weights.get(script);
            if (roll <= cumulative) {
                return script;
            }
        }

        // Fallback
        return scripts.get(scripts.size() - 1);
    }

    private SkillScript selectExploratoryActivity(List<SkillScript> scripts, OrchestratorContext context) {
        // Prefer activities we haven't done much
        List<SkillScript> unfamiliar = new ArrayList<>();

        for (SkillScript script : scripts) {
            MemorySubject subject = MemorySubject.skill(script.getId());
            if (habitModel.getHabitStrength(subject) < 0.3f) {
                unfamiliar.add(script);
            }
        }

        if (unfamiliar.isEmpty()) {
            unfamiliar = scripts;
        }

        return unfamiliar.get(random.nextInt(unfamiliar.size()));
    }

    // === Configuration ===

    public void setNearbyPlayers(List<String> players) {
        this.nearbyPlayers = new ArrayList<>(players);
    }

    public void setExplorationRate(float rate) {
        this.explorationRate = Math.max(0.0f, Math.min(1.0f, rate));
    }

    public void setIrrationalityFactor(float factor) {
        this.irrationalityFactor = Math.max(0.0f, Math.min(0.5f, factor));
    }
}
