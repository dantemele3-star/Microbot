package net.runelite.client.plugins.microbot.aoi.memory.belief;

import net.runelite.client.plugins.microbot.aoi.memory.*;

import java.util.*;

/**
 * System for compressing narrative events into stable beliefs.
 *
 * Compression Process (Every ~30 days):
 * 1. Group narrative events by subject
 * 2. Calculate weighted emotional mean
 * 3. Generate belief summary
 * 4. Mark events as compressed
 * 5. Delete low-salience compressed events
 *
 * Benefits:
 * - Prevents memory bloat
 * - Stable long-term bias
 * - Explainability (belief tracing)
 * - Old events can be safely discarded
 */
public class BeliefSystem implements SleepAware {

    private static final int COMPRESSION_THRESHOLD = 5;      // Min events before compression
    private static final float CERTAINTY_THRESHOLD = 20.0f;  // Events for max certainty
    private static final int COMPRESSION_INTERVAL_TICKS = 180000; // ~30 days of play

    private final NarrativeStore narrativeStore;
    private final Map<MemorySubject, Belief> beliefs;
    private long lastCompressionTick;
    private long currentTick;

    public BeliefSystem(NarrativeStore narrativeStore) {
        this.narrativeStore = narrativeStore;
        this.beliefs = new HashMap<>();
        this.lastCompressionTick = 0;
        this.currentTick = 0;
    }

    // === Query ===

    /**
     * Get the belief for a subject (may be null if no belief formed).
     */
    public Belief getBelief(MemorySubject subject) {
        return beliefs.get(subject);
    }

    /**
     * Check if a belief exists for a subject.
     */
    public boolean hasBelief(MemorySubject subject) {
        return beliefs.containsKey(subject);
    }

    /**
     * Get all beliefs.
     */
    public Collection<Belief> getAllBeliefs() {
        return Collections.unmodifiableCollection(beliefs.values());
    }

    /**
     * Get beliefs sorted by weighted sentiment (most positive first).
     */
    public List<Belief> getBeliefsBySentiment() {
        List<Belief> sorted = new ArrayList<>(beliefs.values());
        sorted.sort((a, b) -> Float.compare(b.getWeightedSentiment(), a.getWeightedSentiment()));
        return sorted;
    }

    // === SleepAware ===

    @Override
    public void onSleep(float strength) {
        // Check if it's time for compression
        if (currentTick - lastCompressionTick >= COMPRESSION_INTERVAL_TICKS) {
            compressAll();
            lastCompressionTick = currentTick;
        }

        // Decay existing beliefs slowly
        for (Belief belief : beliefs.values()) {
            belief.decay(strength);
        }
    }

    @Override
    public int getSleepPriority() {
        return 15; // After narrative store, before derived emotions
    }

    @Override
    public String getSleepAwareName() {
        return "BeliefSystem";
    }

    // === Compression ===

    /**
     * Compress all subjects with enough uncompressed events.
     */
    public void compressAll() {
        Set<MemorySubject> subjects = narrativeStore.getAllSubjects();

        for (MemorySubject subject : subjects) {
            compressIfReady(subject);
        }
    }

    /**
     * Compress events for a specific subject if threshold met.
     */
    public boolean compressIfReady(MemorySubject subject) {
        List<NarrativeEvent> uncompressed = narrativeStore.getUncompressedEventsFor(subject);

        if (uncompressed.size() < COMPRESSION_THRESHOLD) {
            return false; // Not enough events yet
        }

        // Calculate weighted sentiment
        float totalWeight = 0;
        float weightedSum = 0;

        for (NarrativeEvent event : uncompressed) {
            float weight = event.getCurrentSalience() * event.getConfidence();
            weightedSum += event.getEmotionalImpact() * weight;
            totalWeight += weight;
        }

        float sentiment = totalWeight > 0 ? weightedSum / totalWeight : 0;

        // Calculate certainty based on event count
        float certainty = Math.min(1.0f, uncompressed.size() / CERTAINTY_THRESHOLD);

        // Generate summary
        String summary = generateSummary(subject, sentiment, uncompressed);

        // Create or update belief
        Belief existing = beliefs.get(subject);
        if (existing != null) {
            existing.update(sentiment, certainty, summary, uncompressed.size(), currentTick);
        } else {
            beliefs.put(subject, new Belief(subject, sentiment, certainty, summary, uncompressed.size(), currentTick));
        }

        // Mark events as compressed
        for (NarrativeEvent event : uncompressed) {
            event.markCompressed();
        }

        return true;
    }

    /**
     * Force compression for a subject (ignore threshold).
     */
    public void forceCompress(MemorySubject subject) {
        List<NarrativeEvent> events = narrativeStore.getEventsFor(subject);
        if (events.isEmpty()) return;

        // Same compression logic but without threshold check
        float totalWeight = 0;
        float weightedSum = 0;

        for (NarrativeEvent event : events) {
            if (event.isCompressed()) continue;
            float weight = event.getCurrentSalience() * event.getConfidence();
            weightedSum += event.getEmotionalImpact() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) return;

        float sentiment = weightedSum / totalWeight;
        float certainty = Math.min(1.0f, events.size() / CERTAINTY_THRESHOLD);
        String summary = generateSummary(subject, sentiment, events);

        Belief existing = beliefs.get(subject);
        if (existing != null) {
            existing.update(sentiment, certainty, summary, events.size(), currentTick);
        } else {
            beliefs.put(subject, new Belief(subject, sentiment, certainty, summary, events.size(), currentTick));
        }

        for (NarrativeEvent event : events) {
            event.markCompressed();
        }
    }

    // === State Management ===

    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    public int getBeliefCount() {
        return beliefs.size();
    }

    public void clear() {
        beliefs.clear();
        lastCompressionTick = 0;
    }

    // === Summary Generation ===

    private String generateSummary(MemorySubject subject, float sentiment, List<NarrativeEvent> events) {
        String subjectName = subject.getId();

        // Count event types for context
        long achievements = events.stream().filter(e -> e.getType() == NarrativeType.ACHIEVEMENT).count();
        long failures = events.stream().filter(e -> e.getType() == NarrativeType.FAILURE).count();
        long deaths = events.stream().filter(e -> e.getType() == NarrativeType.DEATH).count();

        if (sentiment > 0.5f) {
            if (achievements > 0) {
                return capitalize(subjectName) + " is rewarding and brings accomplishment";
            }
            return capitalize(subjectName) + " is enjoyable and satisfying";
        } else if (sentiment > 0.2f) {
            return capitalize(subjectName) + " is generally pleasant";
        } else if (sentiment > -0.2f) {
            return capitalize(subjectName) + " is routine, neither good nor bad";
        } else if (sentiment > -0.5f) {
            if (failures > 2) {
                return capitalize(subjectName) + " has been frustrating lately";
            }
            return capitalize(subjectName) + " is somewhat unpleasant";
        } else {
            if (deaths > 0) {
                return capitalize(subjectName) + " is dangerous and stressful";
            }
            return capitalize(subjectName) + " is deeply frustrating";
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
