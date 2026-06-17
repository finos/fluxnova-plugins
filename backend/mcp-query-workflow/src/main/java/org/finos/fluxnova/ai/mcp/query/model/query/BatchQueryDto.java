package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.batch.BatchQuery;

import java.util.List;

/**
 * DTO for querying batches via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering batches.")
public record BatchQueryDto(
        @Schema(description = "Filter by the id of the batch.")
        String batchId,

        @Schema(description = "Filter by the type of the batch "
                + "(e.g. instance-migration, instance-deletion, set-job-retries, set-external-task-retries).")
        String type,

        @Schema(description = "Filter by a list of tenant ids. A batch must belong to one of the given tenants.")
        List<String> tenantIdIn,

        @Schema(description = "Only include batches which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Only include active (not suspended) batches. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include suspended batches. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended
) {
    public BatchQuery toQuery(ManagementService managementService) {
        BatchQuery query = managementService.createBatchQuery();
        if (batchId != null) {
            query.batchId(batchId);
        }
        if (type != null) {
            query.type(type);
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        return query;
    }
}
