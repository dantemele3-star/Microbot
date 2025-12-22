package net.runelite.client.plugins.microbot.aoi.core;

import net.runelite.client.plugins.microbot.aoi.identity.SkillCategory;
import net.runelite.client.plugins.microbot.aoi.memory.MemorySubject;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Shared state object passed to all SkillScripts.
 * Scripts are stateless - context owns all state.
 *
 * The context is the "world" from the script's perspective.
 * It contains both objective state (fatigue, time) and
 * subjective state (emotions, identity).
 */
public class OrchestratorContext {

    // Identity
    private final long accountSeed;
    private final Random random;
    private final String accountName;

    // Timing
    private long sessionStartTime;
    private long currentTick;
    private LocalDateTime currentTime;

    // Fatigue & Stress
    private float fatigue;          // 0.0 - 1.0, accumulates over time
    private float stress;           // 0.0 - 1.0, activity-dependent
    private float burnout;          // 0.0 - 1.0, long-term exhaustion

    // Session tracking
    private int activitiesCompleted;
    private int failuresThisSession;
    private SkillScript currentScript;
    private SkillScript previousScript;
    private long scriptStartTick;

    // Activity tracking
    private final Map<String, Long> lastActivityTime;
    private final Map<String, Integer> activityCounts;
    private final Map<SkillCategory, Float> categoryExperience;

    // Temporary script state (cleared on script switch)
    private final Map<String, Object> scriptState;

    // Death/recovery state
    private boolean recentDeath;
    private long deathTick;

    public OrchestratorContext(long accountSeed, String accountName) {
        this.accountSeed = accountSeed;
        this.random = new Random(accountSeed);
        this.accountName = accountName;

        this.sessionStartTime = System.currentTimeMillis();
        this.currentTick = 0;
        this.currentTime = LocalDateTime.now();

        this.fatigue = 0.0f;
        this.stress = 0.0f;
        this.burnout = 0.0f;

        this.activitiesCompleted = 0;
        this.failuresThisSession = 0;

        this.lastActivityTime = new HashMap<>();
        this.activityCounts = new HashMap<>();
        this.categoryExperience = new HashMap<>();
        this.scriptState = new HashMap<>();

        this.recentDeath = false;
        this.deathTick = -1;
    }

    // === Getters ===

    public long getAccountSeed() {
        return accountSeed;
    }

    public Random getRandom() {
        return random;
    }

    public String getAccountName() {
        return accountName;
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public float getFatigue() {
        return fatigue;
    }

    public float getStress() {
        return stress;
    }

    public float getBurnout() {
        return burnout;
    }

    public SkillScript getCurrentScript() {
        return currentScript;
    }

    public SkillScript getPreviousScript() {
        return previousScript;
    }

    public int getActivitiesCompleted() {
        return activitiesCompleted;
    }

    public int getFailuresThisSession() {
        return failuresThisSession;
    }

    public boolean hadRecentDeath() {
        return recentDeath && (currentTick - deathTick) < 3000; // 30 minutes
    }

    // === Session State ===

    public long getSessionDurationMinutes() {
        return (System.currentTimeMillis() - sessionStartTime) / 60000;
    }

    public boolean isWeekend() {
        DayOfWeek day = currentTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public int getMaxSessionMinutes() {
        return isWeekend() ? 120 : 60;
    }

    public long getTicksSinceScriptStart() {
        return currentTick - scriptStartTick;
    }

    // === Activity Tracking ===

    public long getTicksSinceActivity(String scriptId) {
        Long lastTime = lastActivityTime.get(scriptId);
        return lastTime == null ? Long.MAX_VALUE : currentTick - lastTime;
    }

    public int getActivityCount(String scriptId) {
        return activityCounts.getOrDefault(scriptId, 0);
    }

    public float getCategoryExperience(SkillCategory category) {
        return categoryExperience.getOrDefault(category, 0.0f);
    }

    // === Script State ===

    @SuppressWarnings("unchecked")
    public <T> T getScriptState(String key, T defaultValue) {
        return (T) scriptState.getOrDefault(key, defaultValue);
    }

    public void setScriptState(String key, Object value) {
        scriptState.put(key, value);
    }

    public void clearScriptState() {
        scriptState.clear();
    }

    // === Mutators (called by orchestrator only) ===

    public void advanceTick() {
        currentTick++;
        currentTime = LocalDateTime.now();
    }

    public void setFatigue(float fatigue) {
        this.fatigue = Math.max(0.0f, Math.min(1.0f, fatigue));
    }

    public void addFatigue(float amount) {
        setFatigue(this.fatigue + amount);
    }

    public void setStress(float stress) {
        this.stress = Math.max(0.0f, Math.min(1.0f, stress));
    }

    public void setBurnout(float burnout) {
        this.burnout = Math.max(0.0f, Math.min(1.0f, burnout));
    }

    public void setCurrentScript(SkillScript script) {
        this.previousScript = this.currentScript;
        this.currentScript = script;
        this.scriptStartTick = currentTick;
        clearScriptState();
    }

    public void recordActivityCompletion(String scriptId, SkillCategory category) {
        activitiesCompleted++;
        lastActivityTime.put(scriptId, currentTick);
        activityCounts.merge(scriptId, 1, Integer::sum);
        categoryExperience.merge(category, 1.0f, Float::sum);
    }

    public void recordFailure() {
        failuresThisSession++;
    }

    public void recordDeath() {
        recentDeath = true;
        deathTick = currentTick;
        fatigue = Math.min(1.0f, fatigue + 0.4f); // Death causes massive fatigue spike
    }

    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        fatigue = 0.0f;
        stress = 0.0f;
        activitiesCompleted = 0;
        failuresThisSession = 0;
        currentScript = null;
        previousScript = null;
        clearScriptState();
    }

    // === Memory Subject Conversion ===

    public MemorySubject getCurrentScriptSubject() {
        if (currentScript == null) return null;
        return MemorySubject.skill(currentScript.getId());
    }

    public MemorySubject getCategorySubject(SkillCategory category) {
        return MemorySubject.category(category.name());
    }
}
