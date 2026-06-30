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
      toolScopeElementId="myAgent"/>
  </bpmn:extensionElements>
</bpmn:adHocSubProcess>
```

### Attributes

| Attribute | Required | Description |
|---|---|---|
| `provider` | Yes | AI provider identifier (e.g. `ollama`, `openai`). Must match a configured provider at runtime. |
| `model` | Yes | Model identifier within the provider (e.g. `llama3`, `gpt-4o`). |
| `systemPrompt` | No | System prompt text supplied to the model on every turn. |
| `toolScopeElementId` | No | Id of the BPMN element that defines the tool scope. Defaults to the id of the element carrying `agent:config`. Must reference an element within the same process. |

### Validation

The parse listener enforces the following at deployment time, throwing a `BpmnParseException` if any rule is violated:

- `provider` must be present and non-blank
- `model` must be present and non-blank
- `toolScopeElementId`, when specified, must reference a known element within the same process

## Runtime Resolution

`AgentConfigRegistry` resolves configuration lazily on first access per process definition, then caches the result:

```java
Optional<AgentConfig> config = agentConfigRegistry.resolve(processDefinitionId, elementId);
```

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
