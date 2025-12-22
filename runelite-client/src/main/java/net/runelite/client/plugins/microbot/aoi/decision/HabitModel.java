package net.runelite.client.plugins.microbot.aoi.decision;

import net.runelite.client.plugins.microbot.aoi.memory.MemorySubject;
import net.runelite.client.plugins.microbot.aoi.memory.SleepAware;

import java.util.*;

/**
 * Tracks habit formation for activities.
 *
 * Habits are compressed memories of repeated behavior.
 * They provide familiarity bonuses but never preference.
 *
 * Inheritance: Familiarity only (not preference).
 * Alts inherit the familiarity of habits but must develop
 * their own preferences.
 */
public class HabitModel implements SleepAware {

    private static final float HABIT_FORMATION_RATE = 0.02f;
    private static final float HABIT_DECAY_RATE = 0.01f;
    private static final float FAMILIARITY_BONUS = 0.15f;

    /**
     * Represents a formed habit.
     */
    public static class Habit {
        private final MemorySubject subject;
        private float strength;         // 0.0 - 1.0
        private int repetitionCount;
        private long lastPerformedTick;

        public Habit(MemorySubject subject) {
            this.subject = subject;
            this.strength = 0.0f;
            this.repetitionCount = 0;
            this.lastPerformedTick = 0;
        }

        public MemorySubject getSubject() {
            return subject;
        }

        public float getStrength() {
            return strength;
        }

        public int getRepetitionCount() {
            return repetitionCount;
        }

        public long getLastPerformedTick() {
            return lastPerformedTick;
        }

        void perform(long tick) {
            repetitionCount++;
            lastPerformedTick = tick;

            // Strength increases asymptotically
            strength = Math.min(1.0f, strength + (HABIT_FORMATION_RATE * (1.0f - strength)));
        }

        void decay(float rate) {
            strength = Math.max(0.0f, strength - rate);
        }

        /**
         * Is this a strong habit?
         */
        public boolean isStrong() {
            return strength > 0.5f;
        }

        /**
         * Get familiarity bonus from this habit.
         */
        public float getFamiliarityBonus() {
            return strength * FAMILIARITY_BONUS;
        }
    }

    private final Map<MemorySubject, Habit> habits;
    private long currentTick;

    public HabitModel() {
        this.habits = new HashMap<>();
        this.currentTick = 0;
    }

    /**
     * Record performing an activity.
     *
     * @param subject The activity performed
     */
    public void recordActivity(MemorySubject subject) {
        habits.computeIfAbsent(subject, Habit::new).perform(currentTick);
    }

    /**
     * Get habit for a subject.
     *
     * @param subject What to check
     * @return Habit or null if none
     */
    public Habit getHabit(MemorySubject subject) {
        return habits.get(subject);
    }

    /**
     * Get habit strength.
     *
     * @param subject What to check
     * @return Strength (0.0 - 1.0)
     */
    public float getHabitStrength(MemorySubject subject) {
        Habit habit = habits.get(subject);
        return habit != null ? habit.getStrength() : 0.0f;
    }

    /**
     * Get familiarity bonus from habit.
     *
     * @param subject What to check
     * @return Familiarity bonus (0.0 - 0.15)
     */
    public float getFamiliarityBonus(MemorySubject subject) {
        Habit habit = habits.get(subject);
        return habit != null ? habit.getFamiliarityBonus() : 0.0f;
    }

    /**
     * Get all strong habits.
     */
    public List<Habit> getStrongHabits() {
        List<Habit> strong = new ArrayList<>();
        for (Habit habit : habits.values()) {
            if (habit.isStrong()) {
                strong.add(habit);
            }
        }
        return strong;
    }

    /**
     * Get weight modifier based on habit.
     * Familiar activities get a small boost.
     *
     * @param subject What to check
     * @return Weight multiplier (1.0 - 1.15)
     */
    public float getHabitWeight(MemorySubject subject) {
        return 1.0f + getFamiliarityBonus(subject);
    }

    // === SleepAware ===

    @Override
    public void onSleep(float strength) {
        // Decay habits not recently performed
        for (Habit habit : habits.values()) {
            long ticksSincePerformed = currentTick - habit.getLastPerformedTick();
            // Decay faster if not performed recently
            float decayMultiplier = ticksSincePerformed > 6000 ? 2.0f : 1.0f;
            habit.decay(HABIT_DECAY_RATE * strength * decayMultiplier);
        }

        // Remove very weak habits
        habits.entrySet().removeIf(e -> e.getValue().getStrength() < 0.05f);
    }

    @Override
    public int getSleepPriority() {
        return 28;
    }

    @Override
    public String getSleepAwareName() {
        return "HabitModel";
    }

    // === State Management ===

    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    public int getHabitCount() {
        return habits.size();
    }

    public void clear() {
        habits.clear();
    }

    // === For Inheritance ===

    /**
     * Create weakened copy for alt inheritance.
     * Inherits familiarity only (strength * factor).
     *
     * @param inheritanceFactor How much to inherit (0.0 - 1.0)
     * @return New HabitModel with weakened habits
     */
    public HabitModel createWeakenedCopy(float inheritanceFactor) {
        HabitModel copy = new HabitModel();

        for (Map.Entry<MemorySubject, Habit> entry : habits.entrySet()) {
            Habit original = entry.getValue();
            Habit copied = new Habit(entry.getKey());

            // Inherit weakened strength
            copied.strength = original.strength * inheritanceFactor;

            if (copied.strength > 0.05f) {
                copy.habits.put(entry.getKey(), copied);
            }
        }

        return copy;
    }
}
