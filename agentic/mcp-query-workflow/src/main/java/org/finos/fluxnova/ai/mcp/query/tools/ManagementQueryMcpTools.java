package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.BatchResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.JobDefinitionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.JobResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.SchemaLogEntryResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.BatchQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.JobDefinitionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.JobQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.SchemaLogQueryDto;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MCP tools for querying management data from the process engine.
 * <p>
 * Provides read-only query access to jobs, job definitions, batches, and schema log entries
 * through the process engine's ManagementService Query API.
 */
public class ManagementQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(ManagementQueryMcpTools.class);

    private final ManagementService managementService;
    private final int defaultMaxResults;

    public ManagementQueryMcpTools(ManagementService managementService,
                                   int defaultMaxResults) {
        this.managementService = managementService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Job Query ----
    public List<JobResultDto> queryJobs(
            JobQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying jobs with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(managementService) : managementService.createJobQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<JobResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(JobResultDto::fromJob)
                .toList();

        LOG.info("Job query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Job Definition Query ----
    public List<JobDefinitionResultDto> queryJobDefinitions(
            JobDefinitionQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying job definitions with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(managementService) : managementService.createJobDefinitionQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<JobDefinitionResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(JobDefinitionResultDto::fromJobDefinition)
                .toList();

        LOG.info("Job definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Batch Query ----
    public List<BatchResultDto> queryBatches(
            BatchQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying batches with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(managementService) : managementService.createBatchQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<BatchResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(BatchResultDto::fromBatch)
                .toList();

        LOG.info("Batch query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Schema Log Query ----
    public List<SchemaLogEntryResultDto> querySchemaLog(
            SchemaLogQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying schema log with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(managementService) : managementService.createSchemaLogQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<SchemaLogEntryResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(SchemaLogEntryResultDto::fromSchemaLogEntry)
                .toList();

        LOG.info("Schema log query returned {} results", resultDtos.size());
        return resultDtos;
    }
}
