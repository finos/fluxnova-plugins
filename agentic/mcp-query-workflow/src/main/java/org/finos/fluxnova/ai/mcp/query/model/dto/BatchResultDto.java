package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.batch.Batch;

import java.util.Date;

/**
 * Result DTO representing a batch operation.
 */
@Schema(description = "Represents a batch operation, which processes a number of engine commands asynchronously.")
public record BatchResultDto(
        @Schema(description = "The id of the batch.")
        String id,

        @Schema(description = "The type of the batch (e.g. instance-migration, instance-deletion, set-job-retries).")
        String type,

        @Schema(description = "The number of batch execution jobs required to complete the batch.")
        int totalJobs,

        @Schema(description = "The number of batch execution jobs already created by the seed job.")
        int jobsCreated,

        @Schema(description = "The number of batch jobs created per batch seed job invocation.")
        int batchJobsPerSeed,

        @Schema(description = "The number of invocations executed per batch job.")
        int invocationsPerBatchJob,

        @Schema(description = "The id of the batch seed job definition.")
        String seedJobDefinitionId,

        @Schema(description = "The id of the batch monitor job definition.")
        String monitorJobDefinitionId,

        @Schema(description = "The id of the batch execution job definition.")
        String batchJobDefinitionId,

        @Schema(description = "The id of the tenant this batch belongs to.")
        String tenantId,

        @Schema(description = "The user id of the person who created this batch.")
        String createUserId,

        @Schema(description = "Whether this batch is currently suspended.")
        boolean suspended,

        @Schema(description = "The date the batch was started.")
        Date startTime,

        @Schema(description = "The date the batch execution started.")
        Date executionStartTime
) {
    public static BatchResultDto fromBatch(Batch batch) {
        return new BatchResultDto(
                batch.getId(),
                batch.getType(),
                batch.getTotalJobs(),
                batch.getJobsCreated(),
                batch.getBatchJobsPerSeed(),
                batch.getInvocationsPerBatchJob(),
                batch.getSeedJobDefinitionId(),
                batch.getMonitorJobDefinitionId(),
                batch.getBatchJobDefinitionId(),
                batch.getTenantId(),
                batch.getCreateUserId(),
                batch.isSuspended(),
                batch.getStartTime(),
                batch.getExecutionStartTime()
        );
    }
}
