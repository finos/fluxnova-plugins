[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)

# Fluxnova Plugins

This repository is the plugin monorepo for the [Fluxnova BPM Platform](https://github.com/finos/fluxnova-bpm-platform). It hosts multiple Fluxnova engine plugin projects in a single build and release container, with project-specific build and release automation for each plugin family.

These plugins extend the Fluxnova BPM engine with capabilities such as backward-compatibility migration support, AI/MCP integration, and agentic subprocess orchestration.

NOTE: The Plugin repository's root pom is only used for build orchestration and dependency management. Each plugin project is self-contained with its own `pom.xml` and source layout.

## Plugins in This Repository

| Plugin                                   | Executive Summary                                                                                                                                                                                           | Key Modules / Artifacts                                                                                                                              | Documentation                                                                                                                                                       |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fluxnova-backward-compatibility-plugin` | Runtime script preprocessor plugin that helps existing Camunda-based scripts run on Fluxnova by rewriting Camunda API/package references to Fluxnova equivalents without requiring source changes.          | Standalone JAR plugin: `fluxnova-backward-compatibility-plugin`                                                                                      | [Project README](fluxnova-backward-compatibility-plugin/README.md)                                                                                                  |
| `agentic`                                | AI/MCP plugin suite that exposes Fluxnova processes as MCP-compatible tools so AI assistants and LLM clients can discover and invoke BPMN-driven business capabilities.                                     | `mcp-server-plugin`, `mcp-process-start-event`                                                                                                       | [Project README](agentic/README.md), [MCP Server Plugin](agentic/mcp-server-plugin/README.md), [MCP Process Start Event](agentic/mcp-process-start-event/README.md) |
| `agentic-subprocess`                     | Agentic subprocess plugin family that enables LLM-driven orchestration of BPMN ad-hoc subprocesses, including agent configuration, tool discovery, LLM connectivity, invocation routing, and orchestration. | `shared`, `agent-config`, `agent-tool-context-discovery`, `agent-llm-connector`, `agent-tool-invocation`, `agent-orchestrator`, `agentic-subprocess` | [Project README](agentic-subprocess/README.md), [Bundle Module](agentic-subprocess/agentic-subprocess/README.md)                                                    |


## Workflow Documentation
- [Build and Release Workflows Guide](.github/README.md)
- [New Project Onboarding Guide](NEW_PROJECT_ONBOARDING.md)

## Contributing
Please see the Fluxnova [contribution guidelines](https://github.com/finos/fluxnova-bpm-platform/blob/main/CONTRIBUTING.md) for how to raise issues and how to contribute code.

## License

Copyright 2026 FINOS

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
