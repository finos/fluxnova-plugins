# agent-config

Extracts and validates `agent:config` BPMN extension elements at deployment time and provides a runtime registry for resolving the resulting configuration by process definition and element id.

## Responsibilities

- Registers a BPMN parse listener that validates `agent:config` elements at deployment time, failing fast on missing required attributes or unresolvable `toolScopeElementId` references
- Provides `AgentConfigRegistry` for on-demand resolution of agent configuration at runtime
- Listens for process undeployment events and evicts stale cache entries

## Installation

```xml
<dependency>
    <groupId>org.finos.fluxnova.bpm</groupId>
    <artifactId>fluxnova-engine-plugins-ai-agent-config</artifactId>
</dependency>
```

Spring Boot auto-configuration activates automatically when a `RepositoryService` bean is present. No further setup is required.

## BPMN Reference

Declare the agent namespace on the root `<definitions>` element:

```xml
xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent"
```

Then add an `<agent:config>` block inside the `<extensionElements>` of any BPMN element:

```xml
<bpmn:adHocSubProcess id="myAgent">
  <bpmn:extensionElements>
    <agent:config
      provider="ollama"
      model="llama3"
      systemPrompt="You are a helpful assistant."
      userPrompt="Resolve support ticket ${ticketId} for ${customerName}."
      toolScopeElementId="myAgent"/>
  </bpmn:extensionElements>
</bpmn:adHocSubProcess>
```

### Attributes

| Attribute | Required | Description |
|---|---|---|
| `provider` | Yes | AI provider identifier (e.g. `ollama`, `openai`). Must match a configured provider at runtime. |
| `model` | Yes | Model identifier within the provider (e.g. `llama3`, `gpt-4o`). |
| `systemPrompt` | No | System prompt text supplied to the model on every turn: the agent's role and rules. Used as literal text; `${...}` expressions are not evaluated. |
| `userPrompt` | No | Initial user message sent on the agent's first turn: the concrete task for this process instance. Supports `${...}` expressions, evaluated once when the agent starts. Falls back to the default message when absent, blank, or when its expression fails or evaluates to blank. |
| `toolScopeElementId` | No | Id of the BPMN element that defines the tool scope. Defaults to the id of the element carrying `agent:config`. Must reference an element within the same process. |

### User Prompt and Expressions

`userPrompt` can be plain text, a single expression, or text mixed with expressions. Expressions are evaluated by the engine's expression manager against the agent's execution, so they see process-instance variables. The same deployed model therefore produces a different first message for each process instance. `systemPrompt` does not support expressions; keep it to static role and rules text.

```xml
<!-- Plain text: identical for every process instance -->
<agent:config provider="ollama" model="llama3" userPrompt="Review the open fraud alert."/>

<!-- Text mixed with expressions -->
<agent:config provider="ollama" model="llama3"
  systemPrompt="You are a fraud analyst."
  userPrompt="Investigate alert ${alertId} for ${customerName}, amount ${amount}."/>

<!-- Entire prompt supplied by the caller as a process variable -->
<agent:config provider="ollama" model="llama3" userPrompt="${taskPrompt}"/>

<!-- Conditional expression -->
<agent:config provider="ollama" model="llama3"
  userPrompt="Assess alert ${alertId}. Priority: ${amount > 50000 ? 'high' : 'normal'}."/>
```

The variables are typically supplied when the process instance starts:

```java
runtimeService.startProcessInstanceByKey("fraudProcess",
    Map.of("alertId", "A-101", "customerName", "Acme Ltd", "amount", 90000L));
```

When `userPrompt` is absent, blank, references an undefined variable, has invalid syntax, or evaluates to `null` or a blank string, the agent uses the default initial message:

```
Begin. Use the available tools to complete the task, then respond and stop.
```

Expression failures are logged at `WARN` and never fail the process.

The resolved `userPrompt` is stored in the agent's conversation history, so it is evaluated once per agent run.

#### Line breaks and special characters

Prompts (both `systemPrompt` and `userPrompt`) are XML attribute values, so standard XML escaping applies. The parser unescapes them before the prompt reaches the model:

| Character | Write in BPMN as |
|---|---|
| Line break | `&#10;` |
| `&` | `&amp;` |
| `<` / `>` | `&lt;` / `&gt;` |
| `"` | `&quot;` (or use a single-quoted attribute) |
| `'` | `&apos;` (inside a single-quoted attribute) |

Literal line breaks typed directly inside an attribute value are converted to spaces by XML attribute-value normalisation. Use `&#10;` to preserve them:

```xml
<agent:config provider="ollama" model="llama3"
  userPrompt="Review alert ${alertId}.&#10;1. Check the transaction history.&#10;2. Summarise the risk &amp; next steps."/>
```

### Validation

The parse listener enforces the following at deployment time, throwing a `BpmnParseException` if any rule is violated:

- `provider` must be present and non-blank
- `model` must be present and non-blank
- `toolScopeElementId`, when specified, must reference a known element within the same process

## Runtime Resolution

`AgentConfigRegistry` resolves configuration lazily on first access per process definition, then caches the result:

```java
Optional<AgentConfig> config = agentConfigRegistry.resolve(repositoryService, processDefinitionId, elementId);
```

The cached `AgentConfig` holds the raw prompt text as written in the BPMN model. Expressions are evaluated per process instance by the orchestrator, never at parse time.

## Key Classes

| Class | Package | Role |
|---|---|---|
| `AgentConfig` | `...agent.model` | Immutable configuration record extracted from a BPMN element |
| `AgentConfigRegistry` | `...agent.registry` | Runtime lookup for agent configurations; caches by process definition |
| `AgentConfigExtractor` | `...agent.extract` | Reads `agent:config` attributes from a BPMN XML element |
| `AgentConfigValidator` | `...agent.extract` | Validates `agent:config` attributes against the element set of the process |
| `AgentConfigParseListener` | `...agent.parser` | BPMN parse listener that runs validation at deployment time |
| `AgentConfigUndeployListener` | `...agent.lifecycle` | Clears the registry cache on process undeployment |
| `AgentConfigEnginePlugin` | `...agent.autoconfigure` | `ProcessEnginePlugin` that registers the parse listener |
| `AgentConfigAutoConfiguration` | `...agent.autoconfigure` | Spring Boot auto-configuration |
