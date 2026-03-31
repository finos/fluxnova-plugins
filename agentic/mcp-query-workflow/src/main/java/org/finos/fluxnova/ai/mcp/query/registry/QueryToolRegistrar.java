package org.finos.fluxnova.ai.mcp.query.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import org.finos.fluxnova.ai.mcp.query.autoconfigure.QueryToolsProperties;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.ai.mcp.query.tools.*;
import org.finos.fluxnova.ai.mcp.server.registry.ToolConfig;
import org.finos.fluxnova.ai.mcp.server.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Registers all query MCP tools with the {@link ToolRegistry}.
 * <p>
 * Acts as the bridge between the mcp-query-workflow tool classes and the
 * mcp-server-plugin framework. Each tool method is wrapped in a {@link ToolConfig}
 * with a generated JSON schema and a handler that deserializes arguments and delegates
 * to the actual tool method.
 */
public class QueryToolRegistrar {

    private static final Logger LOG = LoggerFactory.getLogger(QueryToolRegistrar.class);

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final Set<String> excludedTools;

    public QueryToolRegistrar(ToolRegistry toolRegistry, ObjectMapper objectMapper,
                              QueryToolsProperties properties) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.excludedTools = properties.getExclude() != null ? properties.getExclude() : Set.of();
    }

    /**
     * Registers tools from the given tool bean if the tool name is not excluded.
     */
    public void registerRepositoryTools(RepositoryQueryMcpTools tools) {
        registerQueryTool("queryProcessDefinitions", tools::queryProcessDefinitions,
                "Query process definitions in the process engine. Returns a list of process definitions matching the given filter criteria. A process definition is a deployed workflow template (e.g. a BPMN 2.0 process) that can be instantiated as a process instance. Use this tool to discover available workflows, find specific versions of a process, or check which definitions are deployed, active, or suspended. All filter parameters are optional.",
                ProcessDefinitionQueryDto.class);

        registerQueryTool("queryDeployments", tools::queryDeployments,
                "Query deployments in the process engine. Returns a list of deployments matching the given filter criteria. A deployment is a container for process definitions, case definitions, decision definitions, and other resources that have been deployed to the engine. Use this tool to find when and what was deployed, or to list deployments by name, source, tenant, or date range. All filter parameters are optional.",
                DeploymentQueryDto.class);

        registerQueryTool("queryCaseDefinitions", tools::queryCaseDefinitions,
                "Query case definitions in the process engine. Returns a list of case definitions matching the given filter criteria. A case definition is a deployed CMMN 2.0 case template that represents a plan of work for a case instance. Use this tool to discover available case templates, find specific versions, or check which definitions are deployed. All filter parameters are optional.",
                CaseDefinitionQueryDto.class);

        registerQueryTool("queryDecisionDefinitions", tools::queryDecisionDefinitions,
                "Query decision definitions in the process engine. Returns a list of decision definitions matching the given filter criteria. A decision definition is a deployed DMN 1.0 decision table or literal expression that can be evaluated to produce a result. Use this tool to discover available decision logic, find specific versions, or look up definitions by their decision requirements definition. All filter parameters are optional.",
                DecisionDefinitionQueryDto.class);

        registerQueryTool("queryDecisionRequirementsDefinitions", tools::queryDecisionRequirementsDefinitions,
                "Query decision requirements definitions in the process engine. Returns a list of decision requirements definitions matching the given filter criteria. A decision requirements definition is a container for a set of related decision definitions that belong to the same DMN resource (decision requirements graph). Use this tool to discover DMN resources and their versions. All filter parameters are optional.",
                DecisionRequirementsDefinitionQueryDto.class);
    }

    public void registerRuntimeTools(RuntimeQueryMcpTools tools) {
        registerQueryTool("queryProcessInstances", tools::queryProcessInstances,
                "Query running process instances in the process engine. Returns a list of process instances matching the given filter criteria. Process instances represent individual executions of a process definition (workflow). Use this tool to find active or suspended process instances by their definition, business key, tenant, incident status, or other attributes. All filter parameters are optional.",
                ProcessInstanceQueryDto.class);

        registerQueryTool("queryExecutions", tools::queryExecutions,
                "Query executions in the process engine. Returns a list of executions matching the given filter criteria. An execution represents a path of execution within a process instance - a process instance is itself the root execution. Parallel gateways and multi-instance activities create additional concurrent executions. Use this tool to inspect execution state, find executions waiting for signals or messages, or examine execution-level details. All filter parameters are optional.",
                ExecutionQueryDto.class);

        registerQueryTool("queryIncidents", tools::queryIncidents,
                "Query incidents in the process engine. Returns a list of incidents matching the given filter criteria. Incidents represent problems that occurred during process execution, such as failed jobs, failed external tasks, or other error conditions. Use this tool to find and diagnose process execution failures. All filter parameters are optional.",
                IncidentQueryDto.class);

        registerQueryTool("queryEventSubscriptions", tools::queryEventSubscriptions,
                "Query event subscriptions in the process engine. Returns a list of event subscriptions matching the given filter criteria. Event subscriptions represent points where a process instance is waiting for an external event, such as a message event, signal event, compensation event, or conditional event. Use this tool to find which process instances are waiting for specific events. All filter parameters are optional.",
                EventSubscriptionQueryDto.class);

        registerQueryTool("queryVariableInstances", tools::queryVariableInstances,
                "Query variable instances in the process engine. Returns a list of variable instances matching the given filter criteria. Variables store data associated with process instances, executions, tasks, or case instances. Each variable has a name, type, and value. Use this tool to inspect the current state of process data across running or completed activities. All filter parameters are optional.",
                VariableInstanceQueryDto.class);
    }

    public void registerTaskTools(TaskQueryMcpTools tools) {
        registerQueryTool("queryTasks", tools::queryTasks,
                "Query user tasks in the process engine. Returns a list of tasks matching the given filter criteria. A task represents a piece of work that needs to be done by a human user, typically a user task in a BPMN process or a human task in a CMMN case. Use this tool to find tasks assigned to or available for a specific user or group, filter by process or case context, priority, due dates, follow-up dates, delegation state, or other task attributes. All filter parameters are optional.",
                TaskQueryDto.class);
    }

    public void registerHistoryTools(HistoryQueryMcpTools tools) {
        registerQueryTool("queryHistoricProcessInstances", tools::queryHistoricProcessInstances,
                "Query historic process instances using the criteria provided in the query DTO. Returns a list of historic process instances that match the specified filters.",
                HistoricProcessInstanceQueryDto.class);

        registerQueryTool("queryHistoricActivityInstances", tools::queryHistoricActivityInstances,
                "Query historic activity instances using the criteria provided in the query DTO. Returns a list of historic activity instances that match the specified filters.",
                HistoricActivityInstanceQueryDto.class);

        registerQueryTool("queryHistoricTaskInstances", tools::queryHistoricTaskInstances,
                "Query historic task instances using the criteria provided in the query DTO. Returns a list of historic task instances that match the specified filters.",
                HistoricTaskInstanceQueryDto.class);

        registerQueryTool("queryHistoricDetails", tools::queryHistoricDetails,
                "Query historic details (variable updates, form fields, and form properties) using the criteria provided in the query DTO. Returns a list of historic details that match the specified filters.",
                HistoricDetailQueryDto.class);

        registerQueryTool("queryHistoricVariableInstances", tools::queryHistoricVariableInstances,
                "Query historic variable instances using the criteria provided in the query DTO. Returns a list of historic variable instances that match the specified filters.",
                HistoricVariableInstanceQueryDto.class);

        registerQueryTool("queryUserOperationLog", tools::queryUserOperationLog,
                "Query user operation log entries using the criteria provided in the query DTO. Returns a list of user operation log entries that match the specified filters.",
                UserOperationLogQueryDto.class);

        registerQueryTool("queryHistoricIncidents", tools::queryHistoricIncidents,
                "Query historic incidents using the criteria provided in the query DTO. Returns a list of historic incidents that match the specified filters.",
                HistoricIncidentQueryDto.class);

        registerQueryTool("queryHistoricIdentityLinkLog", tools::queryHistoricIdentityLinkLog,
                "Query historic identity link log entries using the criteria provided in the query DTO. Returns a list of historic identity link log entries that match the specified filters.",
                HistoricIdentityLinkLogQueryDto.class);

        registerQueryTool("queryHistoricCaseInstances", tools::queryHistoricCaseInstances,
                "Query historic case instances using the criteria provided in the query DTO. Returns a list of historic case instances that match the specified filters.",
                HistoricCaseInstanceQueryDto.class);

        registerQueryTool("queryHistoricCaseActivityInstances", tools::queryHistoricCaseActivityInstances,
                "Query historic case activity instances using the criteria provided in the query DTO. Returns a list of historic case activity instances that match the specified filters.",
                HistoricCaseActivityInstanceQueryDto.class);

        registerQueryTool("queryHistoricDecisionInstances", tools::queryHistoricDecisionInstances,
                "Query historic decision instances using the criteria provided in the query DTO. Returns a list of historic decision instances that match the specified filters.",
                HistoricDecisionInstanceQueryDto.class);

        registerQueryTool("queryHistoricJobLog", tools::queryHistoricJobLog,
                "Query historic job log entries using the criteria provided in the query DTO. Returns a list of historic job log entries that match the specified filters.",
                HistoricJobLogQueryDto.class);

        registerQueryTool("queryHistoricBatches", tools::queryHistoricBatches,
                "Query historic batches using the criteria provided in the query DTO. Returns a list of historic batches that match the specified filters.",
                HistoricBatchQueryDto.class);

        registerQueryTool("queryHistoricExternalTaskLog", tools::queryHistoricExternalTaskLog,
                "Query historic external task log entries using the criteria provided in the query DTO. Returns a list of historic external task log entries that match the specified filters.",
                HistoricExternalTaskLogQueryDto.class);
    }

    public void registerExternalTaskTools(ExternalTaskQueryMcpTools tools) {
        registerQueryTool("queryExternalTasks", tools::queryExternalTasks,
                "Query external tasks in the process engine. Returns a list of external tasks matching the given filter criteria. An external task is created when a service-task-like activity is configured with the external task pattern. The task is placed on a topic and picked up by an external worker that completes it. Use this tool to find pending, locked, or failed external tasks by topic, worker, process instance, priority, or retry status. All filter parameters are optional.",
                ExternalTaskQueryDto.class);
    }

    public void registerAuthorizationTools(AuthorizationQueryMcpTools tools) {
        registerQueryTool("queryAuthorizations", tools::queryAuthorizations,
                "Query authorizations in the process engine. Returns a list of authorizations matching the given filter criteria. An authorization assigns a set of permissions to a user or group for a specific resource. There are three authorization types: global (0), grant (1), and revoke (2). Use this tool to inspect which permissions users or groups have for specific resources. All filter parameters are optional.",
                AuthorizationQueryDto.class);
    }

    public void registerFilterTools(FilterQueryMcpTools tools) {
        registerQueryTool("queryFilters", tools::queryFilters,
                "Query saved query filters in the process engine. Returns a list of filters matching the given filter criteria. A filter is a saved query (e.g., a saved task query) that can be reused to retrieve a predefined set of results. Filters have a resource type such as 'Task', a name, and an owner. Use this tool to discover what saved filters exist and who owns them. All filter parameters are optional.",
                FilterQueryDto.class);
    }

    public void registerCaseTools(CaseQueryMcpTools tools) {
        registerQueryTool("queryCaseInstances", tools::queryCaseInstances,
                "Query CMMN case instances in the process engine. A case instance is the running execution of a CMMN case definition. Use this tool to find case instances by id, business key, case definition, lifecycle state (active, completed, terminated), super/sub process or case linkage, and tenant. All filter parameters are optional.",
                CaseInstanceQueryDto.class);

        registerQueryTool("queryCaseExecutions", tools::queryCaseExecutions,
                "Query CMMN case executions in the process engine. A case execution represents a planned item (stage, milestone, human task, or process task) within a running case instance. Use this tool to find executions by id, case instance, case definition, activity, or lifecycle state (available, enabled, active, disabled, required). All filter parameters are optional.",
                CaseExecutionQueryDto.class);
    }

    public void registerIdentityTools(IdentityQueryMcpTools tools) {
        registerQueryTool("queryUsers", tools::queryUsers,
                "Query users in the process engine identity service. Use this tool to find users by id, name, email, group membership, or tenant membership. Passwords are never included in the results. All filter parameters are optional.",
                UserQueryDto.class);

        registerQueryTool("queryGroups", tools::queryGroups,
                "Query groups in the process engine identity service. Use this tool to find groups by id, name, type, member user, or tenant. All filter parameters are optional.",
                GroupQueryDto.class);

        registerQueryTool("queryTenants", tools::queryTenants,
                "Query tenants in the process engine identity service. Use this tool to find tenants by id, name, or by the users and groups that are members of them. All filter parameters are optional.",
                TenantQueryDto.class);
    }

    public void registerManagementTools(ManagementQueryMcpTools tools) {
        registerQueryTool("queryJobs", tools::queryJobs,
                "Query jobs in the process engine. A job is an asynchronous unit of work that is executed by the job executor. Jobs include timers and asynchronous continuations. Use this tool to find jobs by process instance, execution, activity, job definition, retry status, due date, exception status, tenant, and suspension state. All filter parameters are optional.",
                JobQueryDto.class);

        registerQueryTool("queryJobDefinitions", tools::queryJobDefinitions,
                "Query job definitions in the process engine. A job definition describes how jobs will be created for a given activity. Use this tool to find job definitions by process definition, activity, job type, configuration, overriding priority, tenant, and suspension state. All filter parameters are optional.",
                JobDefinitionQueryDto.class);

        registerQueryTool("queryBatches", tools::queryBatches,
                "Query batch operations in the process engine. A batch represents a number of jobs that execute engine commands asynchronously across a large number of process instances. Use this tool to find batches by id, type, tenant, or suspension state. All filter parameters are optional.",
                BatchQueryDto.class);

        registerQueryTool("querySchemaLog", tools::querySchemaLog,
                "Query the database schema version log. Each entry records a schema upgrade applied to the process engine database. Use this tool to retrieve the history of schema versions or check what version is currently active. All filter parameters are optional.",
                SchemaLogQueryDto.class);
    }

    public void registerXmlTools(XMLMcpTools tools) {
        registerStringTool("getProcessModelXml", tools::getProcessModelXml,
                "Retrieve the BPMN 2.0 XML source of a deployed process definition. Returns the raw BPMN XML as a string, which describes the process flow, tasks, gateways, events, and other elements of the workflow. Use this tool to inspect or analyse the structure of a specific process definition.",
                "processDefinitionId",
                "The ID of the process definition to retrieve the BPMN XML for. Obtain this from available process definition discovery/query tools or existing process metadata.");

        registerStringTool("getDecisionModelXml", tools::getDecisionModelXml,
                "Retrieve the DMN 1.1 XML source of a deployed decision definition. Returns the raw DMN XML as a string, which describes the decision table or literal expression logic used to evaluate business decisions. Use this tool to inspect or analyse the logic of a specific decision definition.",
                "decisionDefinitionId",
                "The ID of the decision definition to retrieve the DMN XML for. Obtain this from available decision definition discovery/query tools or existing decision metadata.");

        registerStringTool("getDecisionRequirementsModelXml", tools::getDecisionRequirementsModelXml,
                "Retrieve the DMN 1.1 XML source of a deployed decision requirements definition. Returns the raw DMN XML as a string, which describes the decision requirements graph (DRG) containing a set of related decisions and their dependencies. Use this tool to inspect the full decision requirements structure for a DMN resource.",
                "decisionRequirementsDefinitionId",
                "The ID of the decision requirements definition to retrieve the DMN XML for. Obtain this from available decision requirements discovery/query tools or existing decision metadata.");

        registerStringTool("getCaseModelXml", tools::getCaseModelXml,
                "Retrieve the CMMN 1.0 XML source of a deployed case definition. Returns the raw CMMN XML as a string, which describes the case plan model, stages, tasks, milestones, and sentries of the case. Use this tool to inspect or analyse the structure of a specific case definition.",
                "caseDefinitionId",
                "The ID of the case definition to retrieve the CMMN XML for. Obtain this from available case definition discovery/query tools or existing case metadata.");
    }

    // --- Internal registration methods ---

    /**
     * Registers a standard query tool (QueryDto + optional maxResults).
     */
    @SuppressWarnings("unchecked")
    private <Q extends Record> void registerQueryTool(
            String toolName,
            QueryToolMethod<Q> method,
            String description,
            Class<Q> queryDtoClass) {

        if (excludedTools.contains(toolName)) {
            LOG.info("MCP - Excluding tool: {}", toolName);
            return;
        }

        JsonSchema schema = QueryToolSchemaGenerator.generateQueryToolSchema(queryDtoClass);

        ToolConfig config = new ToolConfig(toolName, description, schema, args -> {
            Q queryDto = null;
            Object queryDtoRaw = args.get("queryDto");
            if (queryDtoRaw != null) {
                queryDto = objectMapper.convertValue(queryDtoRaw, queryDtoClass);
            }
            Integer maxResults = null;
            Object maxResultsRaw = args.get("maxResults");
            if (maxResultsRaw != null) {
                maxResults = objectMapper.convertValue(maxResultsRaw, Integer.class);
            }
            return method.execute(queryDto, maxResults);
        });

                if (toolRegistry.register(config)) {
            LOG.debug("MCP - Registered query tool: {}", toolName);
        }
    }

    /**
     * Registers a simple string-parameter tool (e.g. XML retrieval).
     */
    private void registerStringTool(
            String toolName,
            Function<String, String> method,
            String description,
            String paramName,
            String paramDescription) {

        if (excludedTools.contains(toolName)) {
            LOG.info("MCP - Excluding tool: {}", toolName);
            return;
        }

        JsonSchema schema = QueryToolSchemaGenerator.generateStringParamSchema(paramName, paramDescription);

        ToolConfig config = new ToolConfig(toolName, description, schema, args -> {
            String paramValue = (String) args.get(paramName);
            return method.apply(paramValue);
        });

                if (toolRegistry.register(config)) {
            LOG.debug("MCP - Registered XML tool: {}", toolName);
        }
    }

    /**
     * Functional interface for standard query tool methods.
     */
    @FunctionalInterface
    interface QueryToolMethod<Q> {
        Object execute(Q queryDto, Integer maxResults);
    }
}
