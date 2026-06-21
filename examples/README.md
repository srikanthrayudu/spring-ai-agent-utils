# Examples

This directory contains IKOS hackathon demos plus lower-level Spring AI Agent Utils examples used by the implementation.

## Available Examples

### [IKOS Risk Detection Demo](ikos-risk-detection-demo)

Standalone identity-risk pipeline with no LLM required. It generates simulated hybrid identity data, correlates identities, detects risks, consolidates alerts, and writes an interactive dashboard.

### [IKOS Agent Demo](ikos-agent-demo)

Full Spring AI identity-governance analyst using IKOS tools, knowledge advisor, task tracking, and specialist agent resources.

### [IKOS Lifecycle Demo](ikos-lifecycle-demo)

Knowledge evolution lifecycle demo showing how observations, patterns, governance review, and remediation outcomes become reusable identity-security knowledge.

## Lower-Level Utility Examples

### [Code Agent Demo](code-agent-demo)

A full-featured AI coding assistant with interactive command-line interface, inspired by Claude Code.

See the [Code Agent Demo README](code-agent-demo/README.md) for full documentation.

---

### [Ask User Question Demo](ask-user-question-demo)

Demonstrates the AskUserQuestionTool for interactive agent-user communication with structured questions and multiple-choice options.

---

### [Sub-Agent Demo](subagent-demo)

Demonstrates the hierarchical sub-agent system using Markdown-defined local subagents with the TaskTool dispatcher pattern.

See the [Sub-Agent Demo README](subagent-demo/README.md) for architecture details.

---

### [Sub-Agent A2A Demo](subagent-a2a-demo)

Extends the sub-agent system with [A2A (Agent-to-Agent) protocol](https://google.github.io/A2A/) support for delegating tasks to remote agents over HTTP.

See the [Sub-Agent A2A Demo README](subagent-a2a-demo/README.md) for setup instructions.

---

### [Skills Demo](skills-demo)

Focused demonstration of the SkillsTool system and custom skill development.

See the [Skills Demo README](skills-demo/README.md) for full documentation.

---

### [Todo Demo](todo-demo)

Demonstrates the `TodoWriteTool` for structured task management in agents. Shows how LLMs can create, track, and update task lists during execution with real-time progress display via Spring application events.

See the [Todo Demo README](todo-demo/README.md) for full documentation.

## Prerequisites

IKOS risk detection demo:
- Java 25, matching the root project configuration
- Maven wrapper from the repository root
- No LLM API key required

Agent and utility demos may also require:
- Java 17 or higher
- Maven 3.6+
- At least one AI provider API key (Anthropic, OpenAI, or Google)
- Optional: Brave API key for web search

### Building

From the project root:
```bash
./mvnw compile -pl ikos -am
```

## Documentation

- [IKOS Architecture](../docs/ARCHITECTURE.md)
- [Enterprise Expectations](../docs/ENTERPRISE_EXPECTATIONS.md)
- [Spring AI Agent Utils Integration](../docs/SPRING_AI_AGENT_UTILS_INTEGRATION.md)
- [spring-ai-agent-utils implementation reference](../spring-ai-agent-utils/README.md)

## License

Apache License 2.0
