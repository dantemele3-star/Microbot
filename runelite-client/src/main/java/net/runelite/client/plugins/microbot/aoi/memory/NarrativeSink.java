package net.runelite.client.plugins.microbot.aoi.memory;

/**
 * Write authority boundary for narrative memory.
 *
 * BEFORE: Anyone could call narrative.recordEvent(...)
 * AFTER:  Only orchestrator owns NarrativeSink
 *
 * Benefits:
 * - Single source of truth for memory writes
 * - Deterministic simulation (predictable recording)
 * - No double-recording possible
 * - Scripts emit signals, orchestrator interprets
 *
 * Design: Scripts NEVER write directly to memory.
 * They report outcomes (TickResult), and the orchestrator
 * interprets those outcomes into narrative events.
 */
public interface NarrativeSink {

    /**
     * Record a narrative event.
     *
     * Only the orchestrator should call this method.
     * Scripts report outcomes through TickResult.
     *
     * @param type The type of narrative interpretation
     * @param subject What/who this event is about
     * @param emotionalImpact Impact from -1.0 (very negative) to +1.0 (very positive)
     * @param salience How memorable this is (0.0 - 1.0)
     * @param confidence How certain the interpretation is (0.0 - 1.0)
     * @param narrative The subjective story/description
     */
    void record(
            NarrativeType type,
            MemorySubject subject,
            float emotionalImpact,
            float salience,
            float confidence,
            String narrative
    );

    /**
     * Record a narrative event with default confidence (0.8).
     */
    default void record(
            NarrativeType type,
            MemorySubject subject,
            float emotionalImpact,
            float salience,
            String narrative
    ) {
        record(type, subject, emotionalImpact, salience, 0.8f, narrative);
    }

    /**
     * Record a narrative event with default salience (0.5) and confidence (0.8).
     */
    default void record(
            NarrativeType type,
            MemorySubject subject,
            float emotionalImpact,
            String narrative
    ) {
        record(type, subject, emotionalImpact, 0.5f, 0.8f, narrative);
    }

    /**
     * Record a pre-constructed narrative event.
     */
    void record(NarrativeEvent event);
}
