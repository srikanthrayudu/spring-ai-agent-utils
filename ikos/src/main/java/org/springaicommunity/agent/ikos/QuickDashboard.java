package org.springaicommunity.agent.ikos;

import org.springaicommunity.agent.ikos.identity.DefaultIdentityCorrelationEngine;
import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.report.InteractiveDashboardGenerator;
import org.springaicommunity.agent.ikos.risk.AlertConsolidationEngine;
import org.springaicommunity.agent.ikos.risk.BehavioralAnalyzer;
import org.springaicommunity.agent.ikos.risk.DefaultRiskDetectionEngine;
import org.springaicommunity.agent.ikos.simulation.SimulatedDataGenerator;

import java.io.FileWriter;
import java.util.List;

/**
 * Quick-run dashboard generator — generates the IKOS dashboard without the interactive CLI.
 * Run: mvn exec:java -Dexec.mainClass="org.springaicommunity.agent.ikos.QuickDashboard" -pl spring-ai-agent-utils
 */
public class QuickDashboard {
    public static void main(String[] args) throws Exception {
        System.out.println("IKOS Quick Dashboard Generator");
        System.out.println("==============================");

        // Generate data
        var generator = new SimulatedDataGenerator();
        var data = generator.generate(200);
        System.out.println("Generated: " + data.accounts().size() + " accounts, " +
                data.auditEvents().size() + " events, " +
                data.temporaryExceptions().size() + " exceptions");

        // Correlate identities
        var correlator = new DefaultIdentityCorrelationEngine();
        List<UnifiedIdentity> identities = correlator.correlate(data.accounts());
        System.out.println("Correlated into: " + identities.size() + " unified identities");

        // Detect risks
        var riskEngine = new DefaultRiskDetectionEngine();
        List<KnowledgeUnit> risks = new java.util.ArrayList<>(riskEngine.detectRisks(identities));
        List<KnowledgeUnit> staleRisks = riskEngine.detectStaleExceptions(data.temporaryExceptions());
        risks.addAll(staleRisks);
        System.out.println("Risks detected: " + risks.size() + " (incl. " + staleRisks.size() + " stale exceptions)");

        // Alert consolidation
        var consolidator = new AlertConsolidationEngine();
        var result = consolidator.consolidate(risks);
        System.out.printf("Alert consolidation: %d → %d (%.0f%% reduction, target met: %s)%n",
                result.originalAlertCount(), result.consolidatedAlertCount(),
                result.reductionPercentage(), result.meetsTarget());

        // Behavioral analysis
        var behavioral = new BehavioralAnalyzer().analyze(data.auditEvents());
        System.out.println("Behavioral anomalies: " + behavioral.anomaliesDetected());

        // Generate dashboard
        var dashGen = new InteractiveDashboardGenerator();
        String html = dashGen.generate(identities, risks, data.offboardingRecords(),
                data.temporaryExceptions(), behavioral, data.groupHierarchy(), data.dataStats());

        String storageDir = System.getProperty("ikos.storage",
                System.getProperty("user.home") + "/.ikos-demo");
        new java.io.File(storageDir).mkdirs();
        String path = storageDir + "/ikos-dashboard.html";
        try (var fw = new FileWriter(path)) {
            fw.write(html);
        }
        System.out.println("\n✓ Dashboard saved: " + path);
        System.out.println("  Open: file://" + path);
    }
}
