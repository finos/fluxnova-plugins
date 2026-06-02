# agent-llm-connector

Connects the process engine to LLM providers via Spring AI. Provides `LlmService` for making model calls, auto-discovers available `ChatModel` beans by naming convention, and validates provider references in `agent:config` elements at BPMN deployment time.

## Responsibilities

- Provides `LlmService`, a stateless façade for calling an LLM with an agent configuration, tool catalogue, context snapshot, and conversation history
- Provides `AgentProviderRegistry`, which maps provider ids to Spring AI `ChatModel` beans
- Validates that provider ids referenced in `agent:config` elements correspond to available `ChatModel` beans, failing deployment if not
- Converts an `AgentToolCatalogue` to Spring AI `ToolCallback` instances for inclusion in a model call, without executing them

## Prerequisites

- `agent-config` must be on the classpath
- `agent-tool-context-discovery` must be on the classpath
- At least one Spring AI `ChatModel` starter must be present (e.g. `spring-ai-ollama-spring-boot-starter`)

## Installation

```xml
<dependency>
    <groupId>org.finos.fluxnova.bpm</groupId>
    <artifactId>fluxnova-engine-plugins-ai-agent-llm-connector</artifactId>
</dependency>
```

Spring Boot auto-configuration activates automatically when a `ChatModel` bean is on the classpath. No further setup is required for providers that follow the Spring AI naming convention.

## Provider Configuration

### Automatic discovery

Provider ids are derived from the names of `ChatModel` beans on the classpath. A bean named `ollamaChatModel` maps to provider id `ollama`; `openaiChatModel` maps to `openai`, and so on. Any Spring AI provider starter that registers a `ChatModel` bean with this convention is supported automatically.

### Explicit overrides

For non-standard bean names, or to map a provider id to a specific bean instance, use `fluxnova.ai.agent.provider-overrides`:

```yaml
fluxnova:
  ai:
    agent:
      provider-overrides:
        my-provider: myCustomChatModelBean
```

The key is the provider id used in `agent:config`; the value is the Spring bean name.

## Provider Validation

When a BPMN process is deployed, `AgentProviderParseListener` checks that every `provider` attribute in an `agent:config` element corresponds to a registered `ChatModel`. Deployment is rejected with a `BpmnParseException` if any reference is unresolvable.

## Calling the LLM

Inject `LlmService` and call it with the resolved agent configuration. Tool calls returned by the model are not executed by this service — they are returned to the caller for dispatch.

```java
LlmResponse response = llmService.call(
    agentConfig,
    toolCatalogue,
    resolvedContext,
    conversationHistory
);

// Persist the updated history for the next turn
List<ConversationEntry> nextHistory = response.updatedHistory();

// Dispatch any tool calls the model requested
List<ToolCallRequest> toolCalls = response.toolCalls();
```

The four overloads of `LlmService.call` support progressively richer calls — from a single-shot call with no history through to a full call with tools, context, and history. See `LlmService` for details.

## Key Classes

| Class | Package | Role |
|---|---|---|
| `LlmService` | `...llm.service` | Interface for making LLM calls |
| `SpringAiLlmService` | `...llm.service` | Spring AI implementation of `LlmService` |
| `AgentProviderRegistry` | `...llm.provider` | Maps provider ids to `ChatModel` beans |
| `AgentProviderProperties` | `...llm.provider` | Configuration properties for explicit provider overrides |
| `AgentToolSchemaConverter` | `...llm.tool` | Converts an `AgentToolCatalogue` to Spring AI `ToolCallback` instances |
| `AgentProviderParseListener` | `...llm.parser` | BPMN parse listener that validates provider references at deployment |
| `AgentProviderEnginePlugin` | `...llm.parser` | `ProcessEnginePlugin` that registers the parse listener |
| `AgentLlmOrchestratorAutoConfiguration` | `...llm.autoconfigure` | Spring Boot auto-configuration |
