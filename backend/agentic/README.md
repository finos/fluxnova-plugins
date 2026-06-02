# Fluxnova Agentic Subprocess Plugins

Backend modules that add agentic subprocess capability to the Fluxnova process engine. Together they allow a BPMN ad-hoc subprocess to be driven by an LLM: the engine calls the model, the model selects tool activities to run, the activities execute, and the results feed back into the next model call — all within a standard BPMN process.

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| [`agent-config`](agent-config/README.md) | `fluxnova-engine-plugins-ai-agent-config` | Extracts and validates `agent:config` BPMN extension elements; provides runtime configuration lookup |
| [`agent-tool-context-discovery`](agent-tool-context-discovery/README.md) | `fluxnova-engine-plugins-ai-agent-tool-context-discovery` | Discovers available tool activities within a scope; resolves declared process variable snapshots |
| [`agent-llm-connector`](agent-llm-connector/README.md) | `fluxnova-engine-plugins-ai-agent-llm-connector` | Connects to LLM providers via Spring AI; validates provider references at BPMN parse time |
| [`agent-tool-invocation`](agent-tool-invocation/README.md) | `fluxnova-engine-plugins-ai-agentic-tool-invocation` | Translates tool-call requests from the LLM into process engine activity invocations |
| [`agent-orchestrator`](agent-orchestrator/README.md) | `fluxnova-engine-plugins-ai-agent-orchestrator` | Drives the per-turn loop: calls the LLM, dispatches tool activities, feeds results back |
| [`agentic-subprocess`](agentic-subprocess/README.md) | `fluxnova-engine-plugins-ai-agentic-subprocess` | Bundle module — a single dependency that pulls in all of the above |

## Quick Start

For most applications, add the bundle module. All modules auto-configure with Spring Boot.

```xml
<dependency>
    <groupId>org.finos.fluxnova.bpm</groupId>
    <artifactId>fluxnova-engine-plugins-ai-agentic-subprocess</artifactId>
</dependency>
```

Then annotate an ad-hoc subprocess in your BPMN:

```xml
<bpmn:definitions
    xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
    xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">

  <bpmn:process id="myProcess" isExecutable="true">
    <bpmn:adHocSubProcess id="myAgent">
      <bpmn:extensionElements>
        <agent:config
          provider="ollama"
          model="llama3"
          systemPrompt="You are a helpful assistant."/>
        <agent:context>
          <agent:variable name="customerId"/>
        </agent:context>
      </bpmn:extensionElements>

      <!-- Each child activity becomes a tool the LLM can invoke -->
      <bpmn:serviceTask id="lookupCustomer" name="Look Up Customer"/>
      <bpmn:serviceTask id="sendEmail"      name="Send Email"/>
    </bpmn:adHocSubProcess>
  </bpmn:process>

</bpmn:definitions>
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        BPMN Process                         │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              Ad-Hoc Subprocess (agent scope)        │   │
│   │                                                     │   │
│   │   ┌──────────────┐     ┌──────────────┐            │   │
│   │   │  Tool Task A │     │  Tool Task B │  . . .     │   │
│   │   └──────────────┘     └──────────────┘            │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
         │ on entry                     ▲ on activity end
         ▼                              │
┌─────────────────────────────────────────────────────────────┐
│                    agent-orchestrator                       │
│                                                             │
│   resolves config, catalogue, context                       │
│          │               │                                  │
│          ▼               ▼                                  │
│   agent-config   agent-tool-context-discovery               │
│                                                             │
│   calls LLM ──► agent-llm-connector                        │
│                                                             │
│   dispatches tools ──► agent-tool-invocation               │
└─────────────────────────────────────────────────────────────┘
```

## BPMN Namespace

All agentic extension elements use the namespace:

```
xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent"
```
