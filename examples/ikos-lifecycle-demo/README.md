# IKOS Lifecycle Demo

Full IKOS lifecycle with an LLM-powered identity governance agent.

## What it demonstrates

- **Identity Event → Risk Detection → Knowledge Evolution → Remediation**
- `IdentityGovernanceTools` as Spring AI `@Tool` methods
- `KnowledgeEvolutionAdvisor` for context-augmented LLM responses
- `TodoWriteTool` for remediation task tracking
- Interactive chat loop with conversation memory

## Architecture

```
User Query → ChatClient → KnowledgeEvolutionAdvisor → IdentityGovernanceTools (@Tool)
                ↑                                          ↓
            Response  ←  LLM (Gemini/Claude)  ←  Tool Results + IKOS Context
```

## Run

```bash
export GOOGLE_CLOUD_PROJECT=your-project-id
mvn spring-boot:run
```

## Example queries

- `"Detect offboarding gaps for John Smith disabled on AD but active on AWS"`
- `"List all identity risks"`
- `"Recommend remediation for the top risk"`
- `"Record a security incident: unauthorized API key usage"`
