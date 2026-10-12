package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.*;
import org.finos.fluxnova.ai.mcp.query.model.query.*;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

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
            int defaultMaxResults) {
        this.repositoryService = repositoryService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Process Definition Query ----
    public List<ProcessDefinitionResultDto> queryProcessDefinitions(
            ProcessDefinitionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying process definitions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(repositoryService) : repositoryService.createProcessDefinitionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ProcessDefinitionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(ProcessDefinitionResultDto::fromProcessDefinition)
                .toList();

        LOG.info("Process definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Deployment Query ----
    public List<DeploymentResultDto> queryDeployments(
            DeploymentQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying deployments with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(repositoryService) : repositoryService.createDeploymentQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<DeploymentResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(DeploymentResultDto::fromDeployment)
                .toList();

        LOG.info("Deployment query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Case Definition Query ----
    public List<CaseDefinitionResultDto> queryCaseDefinitions(
            CaseDefinitionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying case definitions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(repositoryService) : repositoryService.createCaseDefinitionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<CaseDefinitionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(CaseDefinitionResultDto::fromCaseDefinition)
                .toList();

        LOG.info("Case definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Decision Definition Query ----
    public List<DecisionDefinitionResultDto> queryDecisionDefinitions(
            DecisionDefinitionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying decision definitions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(repositoryService) : repositoryService.createDecisionDefinitionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<DecisionDefinitionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(DecisionDefinitionResultDto::fromDecisionDefinition)
                .toList();

        LOG.info("Decision definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Decision Requirements Definition Query ----
    public List<DecisionRequirementsDefinitionResultDto> queryDecisionRequirementsDefinitions(
            DecisionRequirementsDefinitionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying decision requirements definitions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(repositoryService) : repositoryService.createDecisionRequirementsDefinitionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<DecisionRequirementsDefinitionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(DecisionRequirementsDefinitionResultDto::fromDecisionRequirementsDefinition)
                .toList();

        LOG.info("Decision requirements definition query returned {} results", resultDtos.size());
        return resultDtos;
    }
}
