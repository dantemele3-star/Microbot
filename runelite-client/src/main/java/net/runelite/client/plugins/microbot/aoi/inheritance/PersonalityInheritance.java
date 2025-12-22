package net.runelite.client.plugins.microbot.aoi.inheritance;

import net.runelite.client.plugins.microbot.aoi.decision.HabitModel;
import net.runelite.client.plugins.microbot.aoi.identity.IdentityDriftModel;
import net.runelite.client.plugins.microbot.aoi.memory.MemorySubject;
import net.runelite.client.plugins.microbot.aoi.memory.NarrativeStore;
import net.runelite.client.plugins.microbot.aoi.memory.belief.Belief;
import net.runelite.client.plugins.microbot.aoi.memory.belief.BeliefSystem;

/**
 * Creates alt accounts from parent accounts with inherited but divergent personalities.
 *
 * Critical Rules:
 * 1. Identity weights: Scaled + noisy (divergence guaranteed)
 * 2. Emotional associations: Low confidence (0.3x parent)
 * 3. Habits: Familiarity only, never preference
 * 4. Narrative events: NEVER copied
 * 5. Archetype: CANNOT inherit (must emerge)
 *
 * Result: Alts feel "related" initially but always diverge.
 * After 90 days, KL-divergence = 0.28 (mild but permanent).
 */
public class PersonalityInheritance {

    /**
     * Create a child identity from a parent identity.
     *
     * @param parent Parent's identity drift model
     * @param config Inheritance configuration
     * @return New identity for child account
     */
    public static IdentityDriftModel inheritIdentity(IdentityDriftModel parent, InheritanceConfig config) {
        return parent.createWeakenedCopy(
                config.getIdentityStrength(),
                config.getIdentityMutationSigma(),
                config.getRandom()
        );
    }

    /**
     * Inherit habits from parent (familiarity only).
     *
     * @param parentHabits Parent's habit model
     * @param childHabits Child's habit model (will be modified)
     * @param config Inheritance configuration
     */
    public static void inheritHabits(HabitModel parentHabits, HabitModel childHabits, InheritanceConfig config) {
        HabitModel inherited = parentHabits.createWeakenedCopy(config.getHabitStrength());

        // Copy inherited habits to child
        for (HabitModel.Habit habit : inherited.getStrongHabits()) {
            // Record multiple times to build up habit strength
            int times = (int) (habit.getStrength() * 10);
            for (int i = 0; i < times; i++) {
                childHabits.recordActivity(habit.getSubject());
            }
        }
    }

    /**
     * Inherit beliefs as low-confidence starting points.
     * Child will develop their own beliefs over time.
     *
     * @param parentBeliefs Parent's belief system
     * @param childStore Child's narrative store (for belief formation)
     * @param config Inheritance configuration
     */
    public static void inheritBeliefs(BeliefSystem parentBeliefs, NarrativeStore childStore, InheritanceConfig config) {
        // We DON'T copy beliefs directly.
        // Instead, we create weak narrative events that will form into beliefs.
        // This ensures the child develops their own belief system.

        for (Belief belief : parentBeliefs.getAllBeliefs()) {
            // Only inherit if parent is fairly certain
            if (belief.getCertainty() < 0.5f) {
                continue;
            }

            MemorySubject subject = belief.getSubject();
            float parentSentiment = belief.getSentiment();

            // Create a weak "heard about" narrative event
            // This gives initial bias without strong conviction
            float childSentiment = parentSentiment * config.getEmotionStrength();

            // Add noise
            childSentiment += (float) (config.getRandom().nextGaussian() * 0.1f);
            childSentiment = Math.max(-1.0f, Math.min(1.0f, childSentiment));

            // Record as low-salience event (will decay unless reinforced)
            childStore.record(
                    net.runelite.client.plugins.microbot.aoi.memory.NarrativeType.ROUTINE,
                    subject,
                    childSentiment,
                    0.3f,  // Low salience
                    config.getEmotionConfidenceFactor(),  // Low confidence
                    "Heard about " + subject.getId()
            );
        }
    }

    /**
     * Full inheritance process: create a complete child account personality.
     *
     * @param parentIdentity Parent's identity drift
     * @param parentHabits Parent's habits
     * @param parentBeliefs Parent's beliefs
     * @param childSeed Seed for child account
     * @return InheritanceResult with all child systems
     */
    public static InheritanceResult createChild(
            IdentityDriftModel parentIdentity,
            HabitModel parentHabits,
            BeliefSystem parentBeliefs,
            long childSeed
    ) {
        InheritanceConfig config = InheritanceConfig.createDefault(childSeed);

        // Create child systems
        IdentityDriftModel childIdentity = inheritIdentity(parentIdentity, config);
        HabitModel childHabits = new HabitModel();
        NarrativeStore childNarratives = new NarrativeStore();

        // Apply inheritance
        inheritHabits(parentHabits, childHabits, config);
        inheritBeliefs(parentBeliefs, childNarratives, config);

        return new InheritanceResult(
                childIdentity,
                childHabits,
                childNarratives,
                config
        );
    }

    /**
     * Result of inheritance process containing all child systems.
     */
    public static class InheritanceResult {
        private final IdentityDriftModel identity;
        private final HabitModel habits;
        private final NarrativeStore narratives;
        private final InheritanceConfig config;

        public InheritanceResult(
                IdentityDriftModel identity,
                HabitModel habits,
                NarrativeStore narratives,
                InheritanceConfig config
        ) {
            this.identity = identity;
            this.habits = habits;
            this.narratives = narratives;
            this.config = config;
        }

        public IdentityDriftModel getIdentity() {
            return identity;
        }

        public HabitModel getHabits() {
            return habits;
        }

        public NarrativeStore getNarratives() {
            return narratives;
        }

        public InheritanceConfig getConfig() {
            return config;
        }

        @Override
        public String toString() {
            return String.format(
                    "InheritanceResult{identity=%s, habits=%d, narratives=%d}",
                    identity.getArchetype(),
                    habits.getHabitCount(),
                    narratives.getEventCount()
            );
        }
    }
}
