package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.batch.history.HistoricBatch;

import java.util.Date;

/**
 * DTO representing a single historic batch result.
 */
@Schema(description = "A historic batch.")
public record HistoricBatchResultDto(
        @Schema(description = "The id of the historic batch.") String id,
        @Schema(description = "The type of the batch.") String type,
        @Schema(description = "The total number of jobs created by the batch.") int totalJobs,
        @Schema(description = "The number of batch jobs created per seed job invocation.") int batchJobsPerSeed,
        @Schema(description = "The number of invocations per batch job.") int invocationsPerBatchJob,
        @Schema(description = "The id of the seed job definition.") String seedJobDefinitionId,
        @Schema(description = "The id of the monitor job definition.") String monitorJobDefinitionId,
        @Schema(description = "The id of the batch job definition.") String batchJobDefinitionId,
        @Schema(description = "The id of the tenant.") String tenantId,
        @Schema(description = "The id of the user who created the batch.") String createUserId,
        @Schema(description = "The time the batch was started.") Date startTime,
        @Schema(description = "The time the batch execution started.") Date executionStartTime,
        @Schema(description = "The time the batch ended.") Date endTime,
        @Schema(description = "The time this historic batch will be removed.") Date removalTime
) {
    public static HistoricBatchResultDto fromHistoricBatch(HistoricBatch historicBatch) {
        return new HistoricBatchResultDto(
                historicBatch.getId(),
                historicBatch.getType(),
                historicBatch.getTotalJobs(),
                historicBatch.getBatchJobsPerSeed(),
                historicBatch.getInvocationsPerBatchJob(),
                historicBatch.getSeedJobDefinitionId(),
                historicBatch.getMonitorJobDefinitionId(),
                historicBatch.getBatchJobDefinitionId(),
                historicBatch.getTenantId(),
                historicBatch.getCreateUserId(),
                historicBatch.getStartTime(),
                historicBatch.getExecutionStartTime(),
                historicBatch.getEndTime(),
                historicBatch.getRemovalTime()
        );
    }
}
