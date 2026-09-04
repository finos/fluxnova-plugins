# mcp-workflow-query

A read-only [MCP](https://modelcontextprotocol.io/) server extension for
the [Fluxnova](https://github.com/finos/fluxnova) process engine. It exposes process engine query functionality as MCP
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

2. **Tool call** — the client sends a JSON-RPC `tools/call` request with the tool name and a JSON object containing
   filter criteria. The MCP server deserializes the criteria into a **Query DTO** (`TaskQueryDto`,
   `ProcessInstanceQueryDto`, etc.).

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

Each tool accepts a query DTO with optional filter criteria and an optional `maxResults` parameter to control the number
of results returned. Results are serialized to JSON.

## Requirements

- Java 21+
- Fluxnova BPM Engine 1.0.0+
- Spring Boot 3.5+
- Spring AI 1.1+

## Installation

Add the dependency to your Fluxnova Spring Boot application:

```xml

<dependency>
    <groupId>org.finos.fluxnova.ai.mcp</groupId>
    <artifactId>mcp-workflow-query</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

The extension uses Spring Boot auto-configuration. Once the JAR is on the classpath, the `QueryMcpAutoConfiguration`
class is detected automatically via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

No additional configuration is required — the extension picks up the engine service beans already present in your
Fluxnova application context.

## How It Works

1. **Auto-configuration** registers each tool class as a Spring bean, guarded by a `@ConditionalOnProperty` toggle.
2. **Tool classes** (`RuntimeQueryMcpTools`, `RepositoryQueryMcpTools`, `TaskQueryMcpTools`,`ExternalTaskQueryMcpTools`,
   `AuthorizationQueryMcpTools`, `FilterQueryMcpTools`, `CaseQueryMcpTools`,`IdentityQueryMcpTools`,
   `ManagementQueryMcpTools`, `HistoryQueryMcpTools`, `XMLMcpTools`) inject their respective engine service and expose
   `@McpTool`-annotated methods.
3. Each tool method:
    - Accepts a query DTO (e.g. `ProcessInstanceQueryDto`) describing the filter criteria, and an optional `maxResults`
      parameter.
    - Builds a native engine query (`RuntimeService.createProcessInstanceQuery()`, etc.) by applying only the non-null
      filters from the DTO.
    - Applies a result limit: `maxResults` if provided (capped at the configured maximum), otherwise the configured
      default.
    - Maps the engine entity results into lightweight result DTOs and serializes them to JSON.

## Configuration

The extension supports the following application properties:

### General

| Property                         | Default | Description                                                                                                                                                                  |
|----------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fluxnova.mcp.query.max-results` | `200`   | Maximum number of results any tool call can return. Individual tool calls may request fewer via the `maxResults` tool parameter, but this value acts as an absolute ceiling. |

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

### Query all active process instances

```json
{
  "active": true
}
```

### Query process instances by definition key

Query DTO:

```json
{
  "processDefinitionKey": "invoice-approval"
}
```

Pass `maxResults` as a separate tool parameter (e.g. `10`) to limit how many results are returned.

### Find incidents for a specific process instance

```json
{
  "processInstanceId": "abc-123"
}
```

### Find executions waiting for a message

```json
{
  "messageEventSubscriptionName": "paymentReceived",
  "active": true
}
```

### Query variables for a process instance

```json
{
  "processInstanceIdIn": [
    "abc-123"
  ],
  "variableName": "orderTotal"
}
```

### Find event subscriptions by type

```json
{
  "eventType": "signal",
  "tenantIdIn": [
    "tenant-a",
    "tenant-b"
  ]
}
```

### Retrieve the BPMN XML for a process definition

First use `queryProcessDefinitions` to find the process definition ID, then call `getProcessModelXml` with that ID. The
tool returns the full BPMN 2.0 XML string, e.g.:

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
│   ├── QueryMcpAutoConfiguration.java        # Spring Boot auto-configuration with @ConditionalOnProperty beans
│   ├── QueryToolsProperties.java             # @ConfigurationProperties for tool toggles and exclusions
│   └── ToolSpecificationFilter.java          # BeanPostProcessor for per-tool exclusion filtering
├── model/
│   ├── dto/                                  # Result DTOs (tool output)
│   │   ├── ProcessInstanceResultDto.java
│   │   ├── ExecutionResultDto.java
│   │   ├── IncidentResultDto.java
│   │   ├── EventSubscriptionResultDto.java
│   │   ├── VariableInstanceResultDto.java
│   │   ├── ProcessDefinitionResultDto.java
│   │   ├── DeploymentResultDto.java
│   │   ├── TaskResultDto.java
│   │   ├── CaseDefinitionResultDto.java
│   │   ├── DecisionDefinitionResultDto.java
│   │   ├── DecisionRequirementsDefinitionResultDto.java
│   │   ├── ExternalTaskResultDto.java
│   │   ├── AuthorizationResultDto.java
│   │   └── FilterResultDto.java
│   └── query/                                # Query DTOs (tool input)
│       ├── ProcessInstanceQueryDto.java
│       ├── ExecutionQueryDto.java
│       ├── IncidentQueryDto.java
│       ├── EventSubscriptionQueryDto.java
│       ├── VariableInstanceQueryDto.java
│       ├── ProcessDefinitionQueryDto.java
│       ├── DeploymentQueryDto.java
│       ├── TaskQueryDto.java
│       ├── CaseDefinitionQueryDto.java
│       ├── DecisionDefinitionQueryDto.java
│       ├── DecisionRequirementsDefinitionQueryDto.java
│       ├── ExternalTaskQueryDto.java
│       ├── AuthorizationQueryDto.java
│       └── FilterQueryDto.java
└── tools/
    ├── RuntimeQueryMcpTools.java             # MCP tools for RuntimeService queries
    ├── RepositoryQueryMcpTools.java          # MCP tools for RepositoryService queries
    ├── TaskQueryMcpTools.java                # MCP tools for TaskService queries
    ├── ExternalTaskQueryMcpTools.java        # MCP tools for ExternalTaskService queries
    ├── AuthorizationQueryMcpTools.java       # MCP tools for AuthorizationService queries
    ├── FilterQueryMcpTools.java              # MCP tools for FilterService queries
    └── XMLMcpTools.java                      # MCP tools for raw XML model retrieval (BPMN, DMN, CMMN)
```

## Building

```bash
mvn clean install
```

## License

See [LICENSE](../../LICENSE) for details.
