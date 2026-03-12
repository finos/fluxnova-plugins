package org.finos.fluxnova.ai.mcp.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.management.JobDefinition;

/**
 * Result DTO representing a job definition.
 */
@Schema(description = "Represents a job definition, describing how jobs are created for a given activity.")
public record JobDefinitionResultDto(
        @Schema(description = "The id of the job definition.")
        String id,

        @Schema(description = "The id of the process definition this job definition belongs to.")
        String processDefinitionId,

        @Schema(description = "The key of the process definition this job definition belongs to.")
        String processDefinitionKey,

        @Schema(description = "The type of the job (e.g. timer-start-event, async-continuation).")
        String jobType,

        @Schema(description = "The configuration of the job definition. For timer jobs this contains the timer configuration.")
        String jobConfiguration,

        @Schema(description = "The id of the activity this job definition is associated with.")
        String activityId,

        @Schema(description = "Whether this job definition is currently suspended.")
        boolean suspended,

        @Schema(description = "The priority that overrides the default BPMN priority for jobs of this definition, if set.")
        Long overridingJobPriority,

        @Schema(description = "The id of the tenant this job definition belongs to.")
        String tenantId
) {
    public static JobDefinitionResultDto fromJobDefinition(JobDefinition jobDefinition) {
        return new JobDefinitionResultDto(
                jobDefinition.getId(),
                jobDefinition.getProcessDefinitionId(),
                jobDefinition.getProcessDefinitionKey(),
                jobDefinition.getJobType(),
                jobDefinition.getJobConfiguration(),
                jobDefinition.getActivityId(),
                jobDefinition.isSuspended(),
                jobDefinition.getOverridingJobPriority(),
                jobDefinition.getTenantId()
        );
    }
}
