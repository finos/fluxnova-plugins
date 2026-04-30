package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.batch.history.HistoricBatchQuery;

import java.util.List;

/**
 * DTO for querying historic batches.
 */
@Schema(description = "Parameters for querying historic batches.")
public record HistoricBatchQueryDto(
        @Schema(description = "Filter by the id of the historic batch.") String batchId,
        @Schema(description = "Filter by the type of the batch.") String type,
        @Schema(description = "If true, only completed batches are returned. "
                + "If false, only non-completed batches are returned. "
                + "If null, all batches are returned.") Boolean completed,
        @Schema(description = "Filter by a list of tenant ids.") List<String> tenantIdIn,
        @Schema(description = "If true, only batches without a tenant id are returned.") Boolean withoutTenantId,
        @Schema(description = "Maximum number of results to return.") Integer maxResults
) {
    public HistoricBatchQuery toQuery(HistoricBatchQuery query) {
        if (batchId != null) query.batchId(batchId);
        if (type != null) query.type(type);
        if (completed != null) query.completed(completed);
        if (tenantIdIn != null && !tenantIdIn.isEmpty())
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        if (Boolean.TRUE.equals(withoutTenantId)) query.withoutTenantId();
        return query;
    }
}
