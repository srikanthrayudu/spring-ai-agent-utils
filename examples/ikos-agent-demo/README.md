# IKOS Agent Demo

Full-featured AI identity governance analyst with all tools.

## What it demonstrates

- **IKOS tools**: `IdentityGovernanceTools` + `EngineeringMemoryTools`
- **Core agent tools**: `ShellTools`, `GrepTool`, `FileSystemTools`, `TodoWriteTool`
- **User interaction**: `AskUserQuestionTool` with `CommandLineQuestionHandler`
- **Knowledge advisor**: `KnowledgeEvolutionAdvisor` augments every LLM call
- **Pre-seeded data**: 100 simulated identities with real risks to investigate

## Architecture

```
User → ChatClient
         ├── KnowledgeEvolutionAdvisor (IKOS context injection)
         ├── MessageChatMemoryAdvisor (conversation memory)
         └── Tools:
              ├── IdentityGovernanceTools (8 @Tool methods)
              ├── EngineeringMemoryTools (11 @Tool methods)
              ├── TodoWriteTool, ShellTools, GrepTool
              ├── FileSystemTools, AskUserQuestionTool
              └── LLM (Gemini/Claude/GPT)
```

## Run

```bash
export GOOGLE_CLOUD_PROJECT=your-project-id
mvn spring-boot:run
```

## Example queries

- `"List all identity risks and recommend remediations"`
- `"Investigate Sarah Jones across all platforms"`
- `"Create a remediation plan for the most critical risk"`
- `"Search the knowledge store for dormant admin patterns"`
- `"Record an observation: cross-platform admin accounts are increasing"`
