package net.runelite.client.plugins.microbot.aoi.memory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Storage and retrieval for narrative events.
 * Implements SleepAware for memory decay and consolidation.
 *
 * Key properties:
 * - Bounded storage (max 200 events by default)
 * - Decay over time (salience decreases)
 * - High-impact events resist discarding
 * - Ready for belief compression
 */
public class NarrativeStore implements SleepAware, NarrativeSink {

    private static final int DEFAULT_MAX_EVENTS = 200;
    private static final float DEFAULT_DECAY_RATE = 0.1f;
    private static final float DISCARD_THRESHOLD = 0.1f;

    private final List<NarrativeEvent> events;
    private final int maxEvents;
    private final float decayRate;
    private long currentTick;

    public NarrativeStore() {
        this(DEFAULT_MAX_EVENTS, DEFAULT_DECAY_RATE);
    }

    public NarrativeStore(int maxEvents, float decayRate) {
        this.events = new ArrayList<>();
        this.maxEvents = maxEvents;
        this.decayRate = decayRate;
        this.currentTick = 0;
    }

    // === NarrativeSink Implementation ===

    @Override
    public void record(
            NarrativeType type,
            MemorySubject subject,
            float emotionalImpact,
            float salience,
            float confidence,
            String narrative
    ) {
        record(new NarrativeEvent(currentTick, type, subject, emotionalImpact, salience, confidence, narrative));
    }

    @Override
    public void record(NarrativeEvent event) {
        events.add(event);

        // Enforce bounded storage
        if (events.size() > maxEvents) {
            pruneLowestSalience();
        }
    }

    // === SleepAware Implementation ===

    @Override
    public void onSleep(float strength) {
        // Apply decay to all events
        float effectiveDecay = decayRate * strength;
        for (NarrativeEvent event : events) {
            event.decay(effectiveDecay);
        }

        // Discard low-salience events
        events.removeIf(event -> event.shouldDiscard(DISCARD_THRESHOLD));
    }

    @Override
    public int getSleepPriority() {
        return 5; // Process early - other systems depend on this
    }

    @Override
    public String getSleepAwareName() {
        return "NarrativeStore";
    }

    // === Query Methods ===

    /**
     * Get all events for a specific subject.
     */
    public List<NarrativeEvent> getEventsFor(MemorySubject subject) {
        return events.stream()
                .filter(e -> e.getSubject().equals(subject))
                .collect(Collectors.toList());
    }

    /**
     * Get all events of a specific type.
     */
    public List<NarrativeEvent> getEventsByType(NarrativeType type) {
        return events.stream()
                .filter(e -> e.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Get recent events (within specified ticks).
     */
    public List<NarrativeEvent> getRecentEvents(int maxAge) {
        return events.stream()
                .filter(e -> e.getAge(currentTick) <= maxAge)
                .collect(Collectors.toList());
    }

    /**
     * Get events for a subject that haven't been compressed.
     */
    public List<NarrativeEvent> getUncompressedEventsFor(MemorySubject subject) {
        return events.stream()
                .filter(e -> e.getSubject().equals(subject))
                .filter(e -> !e.isCompressed())
                .collect(Collectors.toList());
    }

    /**
     * Get the average emotional impact for a subject.
     */
    public float getAverageImpact(MemorySubject subject) {
        List<NarrativeEvent> subjectEvents = getEventsFor(subject);
        if (subjectEvents.isEmpty()) return 0.0f;

        return (float) subjectEvents.stream()
                .mapToDouble(NarrativeEvent::getWeightedImpact)
                .average()
                .orElse(0.0);
    }

    /**
     * Get the total weighted impact for a subject.
     */
    public float getTotalWeightedImpact(MemorySubject subject) {
        return (float) getEventsFor(subject).stream()
                .mapToDouble(NarrativeEvent::getWeightedImpact)
                .sum();
    }

    /**
     * Count events for a subject.
     */
    public int getEventCount(MemorySubject subject) {
        return (int) events.stream()
                .filter(e -> e.getSubject().equals(subject))
                .count();
    }

    /**
     * Get all unique subjects in memory.
     */
    public Set<MemorySubject> getAllSubjects() {
        return events.stream()
                .map(NarrativeEvent::getSubject)
                .collect(Collectors.toSet());
    }

    /**
     * Get all events (for iteration/export).
     */
    public List<NarrativeEvent> getAllEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Get events sorted by weighted impact (most impactful first).
     */
    public List<NarrativeEvent> getEventsByImpact() {
        return events.stream()
                .sorted((a, b) -> Float.compare(
                        Math.abs(b.getWeightedImpact()),
                        Math.abs(a.getWeightedImpact())
                ))
                .collect(Collectors.toList());
    }

    // === State Management ===

    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    public int getEventCount() {
        return events.size();
    }

    public void clear() {
        events.clear();
    }

    // === Internal ===

    private void pruneLowestSalience() {
        // Sort by current salience (ascending) and remove lowest
        events.sort(Comparator.comparing(NarrativeEvent::getCurrentSalience));

        // Remove bottom 10% (but never high-impact events)
        int toRemove = maxEvents / 10;
        Iterator<NarrativeEvent> iter = events.iterator();
        int removed = 0;
        while (iter.hasNext() && removed < toRemove) {
            NarrativeEvent event = iter.next();
            if (!event.isHighImpact()) {
                iter.remove();
                removed++;
            }
        }

        // Re-sort by tick (chronological)
        events.sort(Comparator.comparing(NarrativeEvent::getTick));
    }
}
