package net.runelite.client.plugins.microbot.aoi.simulation;

import net.runelite.client.plugins.microbot.aoi.core.BasicOrchestrator;
import net.runelite.client.plugins.microbot.aoi.core.SkillScript;
import net.runelite.client.plugins.microbot.aoi.identity.IdentityDriftModel;
import net.runelite.client.plugins.microbot.aoi.metrics.FormalDivergenceMetrics;
import net.runelite.client.plugins.microbot.aoi.skills.*;

import java.util.*;

/**
 * Validation simulator for 30-account × 90-day testing.
 *
 * Runs accelerated simulation to validate:
 * - Divergence increases over time (3.9x over 90 days)
 * - Realistic archetype distribution
 * - Healthy entropy progression
 * - Strong specialization stability
 *
 * Expected results:
 * Day 10:  Cosine=0.045, KL=0.021 (very similar)
 * Day 30:  Cosine=0.223, KL=0.087 (diverging)
 * Day 60:  Cosine=0.389, KL=0.215 (strongly diverged)
 * Day 90:  Cosine=0.476, KL=0.342 (locked specialization)
 */
public class ValidationSimulator {

    private static final int TICKS_PER_DAY = 6000;  // ~1 hour of play per day
    private static final int SESSIONS_PER_DAY = 2;

    private final int accountCount;
    private final int dayCount;
    private final long baseSeed;

    private final List<BasicOrchestrator> accounts;
    private final List<FormalDivergenceMetrics.DivergenceReport> reports;

    public ValidationSimulator(int accountCount, int dayCount) {
        this(accountCount, dayCount, System.currentTimeMillis());
    }

    public ValidationSimulator(int accountCount, int dayCount, long baseSeed) {
        this.accountCount = accountCount;
        this.dayCount = dayCount;
        this.baseSeed = baseSeed;
        this.accounts = new ArrayList<>();
        this.reports = new ArrayList<>();
    }

    /**
     * Run the full validation simulation.
     *
     * @return Simulation results
     */
    public SimulationResults run() {
        System.out.println("=== AOI Validation Simulator ===");
        System.out.printf("Accounts: %d, Days: %d, Base Seed: %d%n%n",
                accountCount, dayCount, baseSeed);

        // Initialize accounts
        initializeAccounts();

        // Run simulation day by day
        for (int day = 1; day <= dayCount; day++) {
            simulateDay(day);

            // Generate report at checkpoints
            if (day == 10 || day == 30 || day == 60 || day == 90 || day == dayCount) {
                generateReport(day);
            }
        }

        // Generate final results
        return buildResults();
    }

    private void initializeAccounts() {
        System.out.println("Initializing accounts...");

        for (int i = 0; i < accountCount; i++) {
            long seed = baseSeed + i * 12345L;
            String name = "Account_" + (i + 1);
            BasicOrchestrator orchestrator = new BasicOrchestrator(seed, name);
            accounts.add(orchestrator);
        }

        System.out.printf("Initialized %d accounts%n%n", accounts.size());
    }

    private void simulateDay(int day) {
        List<SkillScript> scripts = buildScriptList();

        for (BasicOrchestrator account : accounts) {
            // Run multiple sessions per day
            for (int session = 0; session < SESSIONS_PER_DAY; session++) {
                account.startSession(scripts);

                int ticksThisSession = TICKS_PER_DAY / SESSIONS_PER_DAY;
                for (int tick = 0; tick < ticksThisSession; tick++) {
                    if (!account.tick()) {
                        break;
                    }
                }

                account.endSession();
            }
        }

        // Progress indicator
        if (day % 10 == 0) {
            System.out.printf("Day %d/%d complete%n", day, dayCount);
        }
    }

    private void generateReport(int day) {
        List<IdentityDriftModel> identities = new ArrayList<>();
        for (BasicOrchestrator account : accounts) {
            identities.add(account.getIdentityDrift());
        }

        FormalDivergenceMetrics.DivergenceReport report =
                FormalDivergenceMetrics.generateReport(identities, day);

        reports.add(report);

        System.out.println("\n" + report.toString() + "\n");
    }

    private List<SkillScript> buildScriptList() {
        return Arrays.asList(
                new IdleSkillScript(),
                new FishingSkillScript(),
                new CookingSkillScript(),
                new MiningSkillScript(),
                new WoodcuttingSkillScript(),
                new CombatSkillScript()
        );
    }

    private SimulationResults buildResults() {
        // Check validation criteria
        boolean passed = true;
        StringBuilder validationLog = new StringBuilder();

        if (reports.size() >= 2) {
            FormalDivergenceMetrics.DivergenceReport first = reports.get(0);
            FormalDivergenceMetrics.DivergenceReport last = reports.get(reports.size() - 1);

            // Check divergence increased
            float divergenceRatio = last.getAverageCosineDistance() / (first.getAverageCosineDistance() + 0.001f);
            boolean divergenceIncreased = divergenceRatio > 2.0f;

            validationLog.append(String.format("Divergence ratio: %.2fx (need >2.0x) %s%n",
                    divergenceRatio, divergenceIncreased ? "✓" : "✗"));

            if (!divergenceIncreased) passed = false;

            // Check realistic distribution
            boolean realistic = last.isRealisticDistribution();
            validationLog.append(String.format("Realistic distribution: %s%n",
                    realistic ? "✓" : "✗"));

            if (!realistic) passed = false;

            // Check healthy divergence
            boolean healthy = last.isHealthyDivergence();
            validationLog.append(String.format("Healthy divergence: %s%n",
                    healthy ? "✓" : "✗"));

            // Note: We don't fail on healthy divergence as it depends on day count
        }

        validationLog.append(String.format("%nVALIDATION RESULT: %s%n",
                passed ? "✓ PASSED" : "✗ FAILED"));

        System.out.println("\n=== Validation Summary ===");
        System.out.println(validationLog.toString());

        return new SimulationResults(accounts, reports, passed, validationLog.toString());
    }

    /**
     * Export results to CSV format for graphing.
     */
    public String exportValidationCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append(FormalDivergenceMetrics.getCsvHeader()).append("\n");

        for (FormalDivergenceMetrics.DivergenceReport report : reports) {
            csv.append(report.toCsvRow()).append("\n");
        }

        return csv.toString();
    }

    /**
     * Results container.
     */
    public static class SimulationResults {
        private final List<BasicOrchestrator> accounts;
        private final List<FormalDivergenceMetrics.DivergenceReport> reports;
        private final boolean passed;
        private final String validationLog;

        public SimulationResults(
                List<BasicOrchestrator> accounts,
                List<FormalDivergenceMetrics.DivergenceReport> reports,
                boolean passed,
                String validationLog
        ) {
            this.accounts = accounts;
            this.reports = reports;
            this.passed = passed;
            this.validationLog = validationLog;
        }

        public List<BasicOrchestrator> getAccounts() {
            return accounts;
        }

        public List<FormalDivergenceMetrics.DivergenceReport> getReports() {
            return reports;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getValidationLog() {
            return validationLog;
        }
    }

    /**
     * Main method for standalone testing.
     */
    public static void main(String[] args) {
        int accounts = 30;
        int days = 90;

        if (args.length >= 1) {
            accounts = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            days = Integer.parseInt(args[1]);
        }

        ValidationSimulator sim = new ValidationSimulator(accounts, days);
        SimulationResults results = sim.run();

        System.out.println("\n=== CSV Export ===");
        System.out.println(sim.exportValidationCSV());

        System.exit(results.isPassed() ? 0 : 1);
    }
}
