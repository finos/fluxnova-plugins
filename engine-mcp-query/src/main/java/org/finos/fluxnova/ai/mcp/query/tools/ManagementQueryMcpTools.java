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
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;

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
                                   @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        this.managementService = managementService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- Job Query ----

    @McpTool(description = "Query jobs in the process engine. "
            + "A job is an asynchronous unit of work that is executed by the job executor. "
            + "Jobs include timers (timer start events, timer boundary events, timer intermediate events) "
            + "and asynchronous continuations. "
            + "Use this tool to find jobs by process instance, execution, activity, job definition, "
            + "retry status, due date, exception status, tenant, and suspension state. "
            + "All filter parameters are optional.")
    public List<JobResultDto> queryJobs(
            @McpToolParam JobQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying jobs with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<JobResultDto> resultDtos = queryDto.toQuery(managementService).list().stream()
                .limit(limit)
                .map(JobResultDto::fromJob)
                .toList();

        LOG.info("Job query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Job Definition Query ----

    @McpTool(description = "Query job definitions in the process engine. "
            + "A job definition is installed when a BPMN 2.0 process containing timer activities or "
            + "asynchronous continuations is deployed. It describes how jobs will be created for a "
            + "given activity. "
            + "Use this tool to find job definitions by process definition, activity, job type, "
            + "configuration, overriding priority, tenant, and suspension state. "
            + "All filter parameters are optional.")
    public List<JobDefinitionResultDto> queryJobDefinitions(
            @McpToolParam JobDefinitionQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying job definitions with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<JobDefinitionResultDto> resultDtos = queryDto.toQuery(managementService).list().stream()
                .limit(limit)
                .map(JobDefinitionResultDto::fromJobDefinition)
                .toList();

        LOG.info("Job definition query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Batch Query ----

    @McpTool(description = "Query batch operations in the process engine. "
            + "A batch represents a number of jobs that execute engine commands asynchronously "
            + "across a large number of process instances. "
            + "Batch types include: instance-migration, instance-modification, instance-restart, "
            + "instance-deletion, historic-instance-deletion, set-job-retries, "
            + "set-external-task-retries, set-variables, and correlate-message. "
            + "Use this tool to find batches by id, type, tenant, or suspension state. "
            + "All filter parameters are optional.")
    public List<BatchResultDto> queryBatches(
            @McpToolParam BatchQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying batches with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<BatchResultDto> resultDtos = queryDto.toQuery(managementService).list().stream()
                .limit(limit)
                .map(BatchResultDto::fromBatch)
                .toList();

        LOG.info("Batch query returned {} results", resultDtos.size());
        return resultDtos;
    }

    // ---- Schema Log Query ----

    @McpTool(description = "Query the database schema version log. "
            + "Each entry records a schema upgrade applied to the process engine database. "
            + "Use this tool to retrieve the history of schema versions or check what version "
            + "is currently active. All filter parameters are optional.")
    public List<SchemaLogEntryResultDto> querySchemaLog(
            @McpToolParam SchemaLogQueryDto queryDto,
            @McpToolParam(description = "Maximum number of results to return. "
                    + "If not specified, defaults to the configured maximum. "
                    + "Cannot exceed the configured maximum.") Integer maxResults) {
        LOG.info("Querying schema log with criteria: {}", queryDto);

        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<SchemaLogEntryResultDto> resultDtos = queryDto.toQuery(managementService).list().stream()
                .limit(limit)
                .map(SchemaLogEntryResultDto::fromSchemaLogEntry)
                .toList();

        LOG.info("Schema log query returned {} results", resultDtos.size());
        return resultDtos;
    }
}
