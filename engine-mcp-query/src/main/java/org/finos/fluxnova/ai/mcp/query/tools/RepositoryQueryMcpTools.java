package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 * MCP tools for querying repository data from the process engine.
 * <p>
 * Provides read-only query access to process definitions and deployments
 * through the process engine's RepositoryService Query API.
 */
public class RepositoryQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryQueryMcpTools.class);

    private final RepositoryService repositoryService;
    private final int defaultMaxResults;

    public RepositoryQueryMcpTools(RepositoryService repositoryService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.repositoryService = repositoryService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Process Definition Query ----

    @McpTool(description = "Query process definitions in the process engine. "
            + "Returns a list of process definitions matching the given filter criteria. "
            + "A process definition is a deployed workflow template (e.g. a BPMN 2.0 process) "
            + "that can be instantiated as a process instance. "
            + "Use this tool to discover available workflows, find specific versions of a process, "
            + "or check which definitions are deployed, active, or suspended. "
            + "All filter parameters are optional.")
    public List<ProcessDefinitionResultDto> queryProcessDefinitions(
            @McpToolParam ProcessDefinitionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying process definitions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ProcessDefinitionResultDto> resultDtos = queryDto.toQuery(repositoryService).list().stream()
                .limit(limit)
                .map(ProcessDefinitionResultDto::fromProcessDefinition)
                .toList();

        LOG.info("Process definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Deployment Query ----

    @McpTool(description = "Query deployments in the process engine. "
            + "Returns a list of deployments matching the given filter criteria. "
            + "A deployment is a container for process definitions, case definitions, "
            + "decision definitions, and other resources that have been deployed to the engine. "
            + "Use this tool to find when and what was deployed, or to list deployments by "
            + "name, source, tenant, or date range. "
            + "All filter parameters are optional.")
    public List<DeploymentResultDto> queryDeployments(
            @McpToolParam DeploymentQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying deployments with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<DeploymentResultDto> resultDtos = queryDto.toQuery(repositoryService).list().stream()
                .limit(limit)
                .map(DeploymentResultDto::fromDeployment)
                .toList();

        LOG.info("Deployment query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Case Definition Query ----

    @McpTool(description = "Query case definitions in the process engine. "
            + "Returns a list of case definitions matching the given filter criteria. "
            + "A case definition is a deployed CMMN 2.0 case template that represents a plan of work "
            + "for a case instance. "
            + "Use this tool to discover available case templates, find specific versions, "
            + "or check which definitions are deployed. "
            + "All filter parameters are optional.")
    public List<CaseDefinitionResultDto> queryCaseDefinitions(
            @McpToolParam CaseDefinitionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying case definitions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<CaseDefinitionResultDto> resultDtos = queryDto.toQuery(repositoryService).list().stream()
                .limit(limit)
                .map(CaseDefinitionResultDto::fromCaseDefinition)
                .toList();

        LOG.info("Case definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Decision Definition Query ----

    @McpTool(description = "Query decision definitions in the process engine. "
            + "Returns a list of decision definitions matching the given filter criteria. "
            + "A decision definition is a deployed DMN 1.0 decision table or literal expression "
            + "that can be evaluated to produce a result. "
            + "Use this tool to discover available decision logic, find specific versions, "
            + "or look up definitions by their decision requirements definition. "
            + "All filter parameters are optional.")
    public List<DecisionDefinitionResultDto> queryDecisionDefinitions(
            @McpToolParam DecisionDefinitionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying decision definitions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<DecisionDefinitionResultDto> resultDtos = queryDto.toQuery(repositoryService).list().stream()
                .limit(limit)
                .map(DecisionDefinitionResultDto::fromDecisionDefinition)
                .toList();

        LOG.info("Decision definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Decision Requirements Definition Query ----

    @McpTool(description = "Query decision requirements definitions in the process engine. "
            + "Returns a list of decision requirements definitions matching the given filter criteria. "
            + "A decision requirements definition is a container for a set of related decision definitions "
            + "that belong to the same DMN resource (decision requirements graph). "
            + "Use this tool to discover DMN resources and their versions. "
            + "All filter parameters are optional.")
    public List<DecisionRequirementsDefinitionResultDto> queryDecisionRequirementsDefinitions(
            @McpToolParam DecisionRequirementsDefinitionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying decision requirements definitions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<DecisionRequirementsDefinitionResultDto> resultDtos = queryDto.toQuery(repositoryService).list().stream()
                .limit(limit)
                .map(DecisionRequirementsDefinitionResultDto::fromDecisionRequirementsDefinition)
                .toList();

        LOG.info("Decision requirements definition query returned {} results", resultDtos.size());
        return resultDtos;
    }
}
