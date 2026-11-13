# mcp-workflow-query

A read-only [MCP](https://modelcontextprotocol.io/) server extension for
the [Fluxnova](https://github.com/finos/fluxnova-bpm-platform) process engine. It exposes process engine query functionality as MCP
tools, allowing LLM-based agents to inspect and monitor running workflows without being able to modify them.

## Overview

`mcp-workflow-query` interacts directly with the Fluxnova process engine Query API to provide safe, read-only access to
runtime data.

## Architecture

### Request / Response Flow

1. **MCP Client** (an LLM agent) discovers available tools via the MCP `tools/list` endpoint.
   The set of tools exposed is controlled by configuration (see [Configuration](#configuration)):
    - **Service-level toggles** — e.g. `fluxnova.mcp.query.tools.history.enabled=false` disables all history tools.
    - **Per-tool exclusion** — e.g. `fluxnova.mcp.query.tools.exclude=querySchemaLog` removes individual tools.
    - **Result limit** — `fluxnova.mcp.query.max-results` caps the number of results any single tool call can return (
      default 200).

2. **Tool call** — the client sends a JSON-RPC `tools/call` request with the tool name and its arguments: an optional
   `queryDto` object containing filter criteria and an optional `maxResults` integer. The `queryDto` is deserialized
   into a **Query DTO** record (`TaskQueryDto`, `ProcessInstanceQueryDto`, etc.).

3. **Query execution** — the tool class calls `queryDto.toQuery(service)` which builds a native engine query, applying
   only the non-null filters from the DTO. The query is executed against the process engine database.

4. **Response** — engine entities are mapped to lightweight **Result DTO** records (`TaskResultDto`,
   `ProcessInstanceResultDto`, etc.) and serialized to JSON. The MCP server wraps the JSON in a standard MCP text
   content block and returns it to the client.

### Available Tools

The extension ships tool components covering twelve engine services:

#### RuntimeService (`RuntimeQueryMcpTools`)

| Tool                      | Description                                                                                                 |
|---------------------------|-------------------------------------------------------------------------------------------------------------|
| `queryProcessInstances`   | Find active or suspended process instances by definition, business key, tenant, incident status, and more.  |
| `queryExecutions`         | Inspect execution paths within process instances, including those waiting for signals or messages.          |
| `queryIncidents`          | Diagnose process execution failures such as failed jobs or failed external tasks.                           |
| `queryEventSubscriptions` | Find which process instances are waiting for specific message, signal, compensation, or conditional events. |
| `queryVariableInstances`  | Inspect the current values of process variables across instances, executions, or tasks.                     |

#### RepositoryService (`RepositoryQueryMcpTools`)

| Tool                      | Description                                                                                |
|---------------------------|--------------------------------------------------------------------------------------------|
| `queryProcessDefinitions` | Discover available workflow templates, find specific versions, or check deployment status. |
| `queryDeployments`        | List deployments by name, source, tenant, or date range.                                   |

#### TaskService (`TaskQueryMcpTools`)

| Tool         | Description                                                                                                     |
|--------------|-----------------------------------------------------------------------------------------------------------------|
| `queryTasks` | Find user tasks by assignee, candidate group, process context, priority, due dates, delegation state, and more. |

#### ExternalTaskService (`ExternalTaskQueryMcpTools`)

| Tool                 | Description                                                                                                        |
|----------------------|--------------------------------------------------------------------------------------------------------------------|
| `queryExternalTasks` | Find external tasks by topic, worker, process instance, activity, priority, lock status, retry status, and tenant. |

#### AuthorizationService (`AuthorizationQueryMcpTools`)

| Tool                  | Description                                                                         |
|-----------------------|-------------------------------------------------------------------------------------|
| `queryAuthorizations` | Find authorization entries by id, type, user, group, resource type, or resource id. |

#### FilterService (`FilterQueryMcpTools`)

| Tool           | Description                                                                 |
|----------------|-----------------------------------------------------------------------------|
| `queryFilters` | Find saved task filters by id, resource type, name, name pattern, or owner. |

#### CaseService (`CaseQueryMcpTools`)

| Tool                  | Description                                                                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `queryCaseInstances`  | Find CMMN case instances by id, business key, case definition, lifecycle state (active, completed, terminated), super/sub linkage, or tenant. |
| `queryCaseExecutions` | Find CMMN case executions (stages, milestones, tasks) within case instances by id, activity, case definition, or lifecycle state.             |

#### IdentityService (`IdentityQueryMcpTools`)

| Tool           | Description                                                                                                     |
|----------------|-----------------------------------------------------------------------------------------------------------------|
| `queryUsers`   | Find users by id, first/last name, email, group membership, or tenant membership. Passwords are never returned. |
| `queryGroups`  | Find groups by id, name, type, member user, or tenant.                                                          |
| `queryTenants` | Find tenants by id, name, or by the users and groups that are members of them.                                  |

#### ManagementService (`ManagementQueryMcpTools`)

| Tool                  | Description                                                                                                       |
|-----------------------|-------------------------------------------------------------------------------------------------------------------|
| `queryJobs`           | Find async jobs by id, definition, process, activity, retry status, due date, priority, exception, and tenant.    |
| `queryJobDefinitions` | Find job definitions by id, activity, process definition, job type, configuration, override priority, and tenant. |
| `queryBatches`        | Find batch operations (e.g. instance migration, deletion, set-retries) by id, type, activity state, and tenant.   |
| `querySchemaLog`      | Find schema log entries recording the database schema version history.                                            |

#### RepositoryService — XML models (`XMLMcpTools`)

| Tool                              | Description                                                                             |
|-----------------------------------|-----------------------------------------------------------------------------------------|
| `getProcessModelXml`              | Return the raw BPMN 2.0 XML source of a deployed process definition.                    |
| `getDecisionModelXml`             | Return the raw DMN 1.1 XML source of a deployed decision definition.                    |
| `getDecisionRequirementsModelXml` | Return the raw DMN 1.1 XML source of a deployed decision requirements definition (DRG). |
| `getCaseModelXml`                 | Return the raw CMMN 1.0 XML source of a deployed case definition.                       |

Unlike the query tools, XML tools accept a single ID parameter and return the complete XML document as a string rather
than a list of result DTOs.

#### HistoryService (`HistoryQueryMcpTools`)

| Tool                                 | Description                                                                                                          |
|--------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `queryHistoricProcessInstances`      | Find completed or running process instances with historical context: start/end times, state, business key, and more. |
| `queryHistoricActivityInstances`     | Find historical activity instance records including start/end times, type, assignee, completion, and cancellation.   |
| `queryHistoricTaskInstances`         | Find historical task records including assignee, owner, priority, lifecycle dates, and candidate group details.      |
| `queryHistoricDetails`               | Find historical detail records (variable updates, form fields, form properties) within process or case scope.        |
| `queryHistoricVariableInstances`     | Find historical variable instance records by name, type, process, case, task, execution, or activity instance.       |
| `queryUserOperationLog`              | Find audit log entries for operations performed by users or the engine itself on process and task entities.          |
| `queryHistoricIncidents`             | Find historical incident records (failed jobs, external tasks) including open and resolved incidents.                |
| `queryHistoricIdentityLinkLog`       | Find the history of identity link (assignee, owner, candidate user/group) changes on tasks.                          |
| `queryHistoricCaseInstances`         | Find historical CMMN case instance records by id, business key, definition, state, and dates.                        |
| `queryHistoricCaseActivityInstances` | Find historical CMMN case activity instance records (stages, milestones, tasks) within case instances.               |
| `queryHistoricDecisionInstances`     | Find historical DMN decision evaluation records including the decision definition and evaluation time.               |
| `queryHistoricJobLog`                | Find job lifecycle log entries tracking job creation, execution, failure, and deletion events.                       |
| `queryHistoricBatches`               | Find historical batch operation records by id, type, completion state, and tenant.                                   |
| `queryHistoricExternalTaskLog`       | Find external task lifecycle log entries tracking creation, success, failure, and deletion events.                   |

Each query tool takes two optional arguments: `queryDto`, an object with the filter criteria, and `maxResults`, an
integer limiting the number of results returned. Results are serialized to JSON.

## Requirements

- Java 21+
- Fluxnova BPM Engine 3.0.0+
- Spring Boot 4.0.x
- Spring AI 2.0.x
- [`mcp-server-plugin`](../mcp-server-plugin/README.md) (pulled in transitively)

## Installation

Add the dependency to your Fluxnova Spring Boot application:

```xml
<dependency>
    <groupId>org.finos.fluxnova.bpm</groupId>
    <artifactId>fluxnova-engine-plugins-ai-mcp-query-workflow</artifactId>
    <version>${fluxnova-plugins-ai.version}</version>
</dependency>
```

This brings in `fluxnova-engine-plugins-ai-mcp-server`, which runs the Spring AI MCP server and provides the
`ToolRegistry` that the query tools are registered with. Configure the MCP server as usual for Spring AI, e.g.:

```yaml
spring:
  ai:
    mcp:
      server:
        name: fluxnova-mcp-server
        protocol: STREAMABLE
        type: SYNC
```

The extension uses Spring Boot auto-configuration. Once the JAR is on the classpath, `QueryMcpAutoConfiguration` is
detected via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. No additional
configuration is required — the extension picks up the engine service beans already present in your Fluxnova
application context.

A runnable example application with end-to-end tests lives in
[fluxnova-examples](https://github.com/finos/fluxnova-examples) under `ai/mcp-workflow-query-example`.

## How It Works

1. **Auto-configuration** (`QueryMcpAutoConfiguration`, ordered after the mcp-server-plugin's auto-configuration)
   creates one bean per tool class (`RuntimeQueryMcpTools`, `RepositoryQueryMcpTools`, `TaskQueryMcpTools`,
   `ExternalTaskQueryMcpTools`, `AuthorizationQueryMcpTools`, `FilterQueryMcpTools`, `CaseQueryMcpTools`,
   `IdentityQueryMcpTools`, `ManagementQueryMcpTools`, `HistoryQueryMcpTools`, `XMLMcpTools`), each guarded by its
   service-level `@ConditionalOnProperty` toggle and injected with its engine service.
2. **Registration** — on application start-up, `QueryToolRegistrar` registers each tool method programmatically with
   the mcp-server-plugin's `ToolRegistry`, skipping any tool named in `fluxnova.mcp.query.tools.exclude`. Each tool's
   input schema is generated from its Query DTO record by `QueryToolSchemaGenerator`, using the `@Schema` descriptions
   on the record components.
3. **Each tool call**:
    - Deserializes the `queryDto` argument into the Query DTO record (e.g. `ProcessInstanceQueryDto`).
    - Builds a native engine query (`RuntimeService.createProcessInstanceQuery()`, etc.) by applying only the non-null
      filters from the DTO.
    - Applies a result limit: `maxResults` if provided and positive (capped at the configured maximum), otherwise the
      configured maximum.
    - Maps the engine entity results into lightweight result DTOs, which the MCP server serializes to JSON.

## Configuration

The extension supports the following application properties:

### General

| Property                         | Default | Description                                                                                                                                                                  |
|----------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fluxnova.mcp.query.max-results` | `200`   | Maximum number of results any tool call can return. Individual tool calls may request fewer via the `maxResults` tool argument, but this value acts as an absolute ceiling. |

### Service-Level Toggles

Each engine service's tools can be enabled or disabled as a group. All services are enabled by default.

| Property                                         | Default | Description                       |
|--------------------------------------------------|---------|-----------------------------------|
| `fluxnova.mcp.query.tools.repository.enabled`    | `true`  | Enable RepositoryService tools    |
| `fluxnova.mcp.query.tools.runtime.enabled`       | `true`  | Enable RuntimeService tools       |
| `fluxnova.mcp.query.tools.task.enabled`          | `true`  | Enable TaskService tools          |
| `fluxnova.mcp.query.tools.history.enabled`       | `true`  | Enable HistoryService tools       |
| `fluxnova.mcp.query.tools.external-task.enabled` | `true`  | Enable ExternalTaskService tools  |
| `fluxnova.mcp.query.tools.authorization.enabled` | `true`  | Enable AuthorizationService tools |
| `fluxnova.mcp.query.tools.filter.enabled`        | `true`  | Enable FilterService tools        |
| `fluxnova.mcp.query.tools.case-service.enabled`  | `true`  | Enable CaseService tools          |
| `fluxnova.mcp.query.tools.identity.enabled`      | `true`  | Enable IdentityService tools      |
| `fluxnova.mcp.query.tools.management.enabled`    | `true`  | Enable ManagementService tools    |
| `fluxnova.mcp.query.tools.xml.enabled`           | `true`  | Enable XML model retrieval tools  |

### Per-Tool Exclusion

Individual tools can be excluded by name, even when their parent service is enabled.

| Property                           | Default   | Description                                   |
|------------------------------------|-----------|-----------------------------------------------|
| `fluxnova.mcp.query.tools.exclude` | _(empty)_ | Comma-separated list of tool names to exclude |

### Examples

Disable all history tools:

```properties
fluxnova.mcp.query.tools.history.enabled=false
```

Keep all services enabled but exclude specific tools:

```properties
fluxnova.mcp.query.tools.exclude=querySchemaLog,queryHistoricBatches
```

Only expose runtime and task tools:

```properties
fluxnova.mcp.query.tools.repository.enabled=false
fluxnova.mcp.query.tools.history.enabled=false
fluxnova.mcp.query.tools.external-task.enabled=false
fluxnova.mcp.query.tools.authorization.enabled=false
fluxnova.mcp.query.tools.filter.enabled=false
fluxnova.mcp.query.tools.case-service.enabled=false
fluxnova.mcp.query.tools.identity.enabled=false
fluxnova.mcp.query.tools.management.enabled=false
```

## Usage Examples

The examples below show the `arguments` of an MCP `tools/call` request.

### Query all active process instances

```json
{
  "queryDto": {
    "active": true
  }
}
```

### Query process instances by definition key, limited to 10 results

```json
{
  "queryDto": {
    "processDefinitionKey": "invoice-approval"
  },
  "maxResults": 10
}
```

### Find incidents for a specific process instance

```json
{
  "queryDto": {
    "processInstanceId": "abc-123"
  }
}
```

### Find executions waiting for a message

```json
{
  "queryDto": {
    "messageEventSubscriptionName": "paymentReceived",
    "active": true
  }
}
```

### Query variables for a process instance

```json
{
  "queryDto": {
    "processInstanceIdIn": ["abc-123"],
    "variableName": "orderTotal"
  }
}
```

### Find event subscriptions by type

```json
{
  "queryDto": {
    "eventType": "signal",
    "tenantIdIn": ["tenant-a", "tenant-b"]
  }
}
```

### Retrieve the BPMN XML for a process definition

First use `queryProcessDefinitions` to find the process definition ID, then call `getProcessModelXml` with
`{"processDefinitionId": "<id>"}`. The tool returns the full BPMN 2.0 XML string, e.g.:

```
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" ...>
  <process id="invoice-approval" name="Invoice Approval" isExecutable="true">
    ...
  </process>
</definitions>
```

The same pattern applies to DMN files via `getDecisionModelXml` / `getDecisionRequirementsModelXml`, and CMMN files via
`getCaseModelXml`.

## Project Structure

```
src/main/java/org/finos/fluxnova/ai/mcp/query/
├── autoconfigure/
│   ├── QueryMcpAutoConfiguration.java        # Spring Boot auto-configuration: tool beans + registration runner
│   └── QueryToolsProperties.java             # @ConfigurationProperties for tool toggles and exclusions
├── model/
│   ├── dto/                                  # Result DTO records (tool output), one per engine entity
│   └── query/                                # Query DTO records (tool input); each builds its native engine query
├── plugins/
│   └── McpWorkflowQueryPlugin.java           # ProcessEnginePlugin (lifecycle logging)
├── registry/
│   ├── QueryToolRegistrar.java               # Registers tool methods with the mcp-server-plugin ToolRegistry
│   └── QueryToolSchemaGenerator.java         # Generates MCP input schemas from Query DTO records
└── tools/                                    # One class per engine service, wrapping its query API
    ├── AuthorizationQueryMcpTools.java
    ├── CaseQueryMcpTools.java
    ├── ExternalTaskQueryMcpTools.java
    ├── FilterQueryMcpTools.java
    ├── HistoryQueryMcpTools.java
    ├── IdentityQueryMcpTools.java
    ├── ManagementQueryMcpTools.java
    ├── RepositoryQueryMcpTools.java
    ├── RuntimeQueryMcpTools.java
    ├── TaskQueryMcpTools.java
    └── XMLMcpTools.java                      # Raw XML model retrieval (BPMN, DMN, CMMN)
```

## Building

```bash
mvn clean install
```

## License

See [LICENSE](../../LICENSE) for details.
