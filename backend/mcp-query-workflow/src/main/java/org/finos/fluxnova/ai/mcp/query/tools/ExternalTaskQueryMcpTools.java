package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.ai.mcp.query.model.dto.ExternalTaskResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.ExternalTaskQueryDto;
import org.finos.fluxnova.bpm.engine.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MCP tools for querying external task data from the process engine.
 * <p>
 * Provides read-only query access to external tasks
 * through the process engine's ExternalTaskService Query API.
 */
public class ExternalTaskQueryMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalTaskQueryMcpTools.class);

    private final ExternalTaskService externalTaskService;
    private final int defaultMaxResults;

    public ExternalTaskQueryMcpTools(ExternalTaskService externalTaskService,
                                     int defaultMaxResults) {
        this.externalTaskService = externalTaskService;
        this.defaultMaxResults = defaultMaxResults;
    }

    // ---- External Task Query ----
    public List<ExternalTaskResultDto> queryExternalTasks(
            ExternalTaskQueryDto queryDto,
            Integer maxResults) {
        LOG.info("Querying external tasks with criteria: {}", queryDto);

        var query = queryDto != null ? queryDto.toQuery(externalTaskService) : externalTaskService.createExternalTaskQuery();
        int limit = maxResults != null ? Math.min(maxResults, defaultMaxResults) : defaultMaxResults;
        List<ExternalTaskResultDto> resultDtos = query.listPage(0, limit).stream()
                .map(ExternalTaskResultDto::fromExternalTask)
                .toList();

        LOG.info("External task query returned {} results", resultDtos.size());
        return resultDtos;
    }
}
