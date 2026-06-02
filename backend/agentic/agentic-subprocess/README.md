# agentic-subprocess

Bundle module that pulls in all agentic subprocess modules as a single dependency.

## What's included

| Module | Artifact |
|---|---|
| [`agent-config`](../agent-config/README.md) | `fluxnova-engine-plugins-ai-agent-config` |
| [`agent-tool-context-discovery`](../agent-tool-context-discovery/README.md) | `fluxnova-engine-plugins-ai-agent-tool-context-discovery` |
| [`agent-llm-connector`](../agent-llm-connector/README.md) | `fluxnova-engine-plugins-ai-agent-llm-connector` |
| [`agent-tool-invocation`](../agent-tool-invocation/README.md) | `fluxnova-engine-plugins-ai-agentic-tool-invocation` |
| [`agent-orchestrator`](../agent-orchestrator/README.md) | `fluxnova-engine-plugins-ai-agent-orchestrator` |

## Installation

```xml
<dependency>
    <groupId>org.finos.fluxnova.bpm</groupId>
    <artifactId>fluxnova-engine-plugins-ai-agentic-subprocess</artifactId>
</dependency>
```

All modules auto-configure with Spring Boot. You will also need at least one Spring AI provider starter on the classpath, for example:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

If you need only a subset of the modules — for example to use a custom orchestration strategy — add the individual module dependencies instead of this bundle.
