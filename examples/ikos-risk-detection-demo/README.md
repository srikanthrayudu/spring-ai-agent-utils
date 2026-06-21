# IKOS Risk Detection Demo

Standalone risk detection pipeline — **no LLM required**.

## What it demonstrates

- Simulated identity data generation (200 accounts across 5 platforms)
- Cross-platform identity correlation
- Automated risk detection (offboarding gaps, dormant admins, privilege creep)
- Effective privilege analysis via nested group traversal
- Behavioral analysis on audit events
- Alert consolidation (noise reduction)
- Pattern auto-discovery and governance promotion
- Interactive HTML dashboard generation

## Run

```bash
mvn spring-boot:run
```

No API keys or LLM provider needed. The demo generates simulated data and runs the full 9-step pipeline.

## Output

- Console: step-by-step risk analysis with ANSI colors
- Dashboard: `~/.ikos-risk-demo/ikos-dashboard.html` (open in browser)
