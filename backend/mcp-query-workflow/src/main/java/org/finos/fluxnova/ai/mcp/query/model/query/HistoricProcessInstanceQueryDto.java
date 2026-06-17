package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.history.HistoricProcessInstanceQuery;

import java.util.Date;
import java.util.List;

/**
 * DTO for querying historic process instances via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering historic process instances.")
public record HistoricProcessInstanceQueryDto(
        @Schema(description = "Filter by a specific historic process instance id.")
        String processInstanceId,

        @Schema(description = "Filter by a list of historic process instance ids.")
        List<String> processInstanceIds,

        @Schema(description = "Filter by ids NOT in this list.")
        List<String> processInstanceIdNotIn,

        @Schema(description = "Filter by the id of the process definition the instances were started for.")
        String processDefinitionId,

        @Schema(description = "Filter by the key of the process definition the instances were started for.")
        String processDefinitionKey,

        @Schema(description = "Filter by a list of process definition keys.")
        List<String> processDefinitionKeyIn,

        @Schema(description = "Exclude instances whose process definition key is in this list.")
        List<String> processDefinitionKeyNotIn,

        @Schema(description = "Filter by the name of the process definition.")
        String processDefinitionName,

        @Schema(description = "Filter by a process definition name pattern (use % as wildcard).")
        String processDefinitionNameLike,

        @Schema(description = "Filter by the business key of the process instance.")
        String processInstanceBusinessKey,

        @Schema(description = "Filter by a list of business keys.")
        List<String> processInstanceBusinessKeyIn,

        @Schema(description = "Filter by a business key pattern (use % as wildcard).")
        String processInstanceBusinessKeyLike,

        @Schema(description = "Only include process instances that are completely finished.")
        Boolean finished,

        @Schema(description = "Only include process instances that have not yet finished.")
        Boolean unfinished,

        @Schema(description = "Only include process instances that have incidents.")
        Boolean withIncidents,

        @Schema(description = "Only include process instances that have root incidents.")
        Boolean withRootIncidents,

        @Schema(description = "Filter by incident status: 'open' or 'resolved'.")
        String incidentStatus,

        @Schema(description = "Filter by incident type (e.g. failedJob).")
        String incidentType,

        @Schema(description = "Filter by the incident message.")
        String incidentMessage,

        @Schema(description = "Filter by an incident message pattern (use % as wildcard).")
        String incidentMessageLike,

        @Schema(description = "Only include instances that had retrying jobs.")
        Boolean withJobsRetrying,

        @Schema(description = "Filter by the id of the case instance that started this process instance.")
        String caseInstanceId,

        @Schema(description = "Filter by a super process instance id.")
        String superProcessInstanceId,

        @Schema(description = "Filter by a sub process instance id.")
        String subProcessInstanceId,

        @Schema(description = "Filter by a super case instance id.")
        String superCaseInstanceId,

        @Schema(description = "Filter by a sub case instance id.")
        String subCaseInstanceId,

        @Schema(description = "Only include top-level process instances (no super process instance).")
        Boolean rootProcessInstances,

        @Schema(description = "Filter by instances started before this date.")
        Date startedBefore,

        @Schema(description = "Filter by instances started after this date.")
        Date startedAfter,

        @Schema(description = "Filter by instances finished before this date.")
        Date finishedBefore,

        @Schema(description = "Filter by instances finished after this date.")
        Date finishedAfter,

        @Schema(description = "Filter by the user who started the process instance.")
        String startedBy,

        @Schema(description = "Only include active (not suspended) process instances.")
        Boolean active,

        @Schema(description = "Only include suspended process instances.")
        Boolean suspended,

        @Schema(description = "Only include completed process instances.")
        Boolean completed,

        @Schema(description = "Only include externally terminated process instances.")
        Boolean externallyTerminated,

        @Schema(description = "Only include internally terminated process instances.")
        Boolean internallyTerminated,

        @Schema(description = "Filter by a list of tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include process instances which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Maximum number of results to return.")
        Integer maxResults
) {
    public HistoricProcessInstanceQuery toQuery(HistoryService historyService) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (processInstanceIds != null && !processInstanceIds.isEmpty()) {
            query.processInstanceIds(new java.util.HashSet<>(processInstanceIds));
        }
        if (processInstanceIdNotIn != null && !processInstanceIdNotIn.isEmpty()) {
            query.processInstanceIdNotIn(processInstanceIdNotIn.toArray(new String[0]));
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (processDefinitionKeyIn != null && !processDefinitionKeyIn.isEmpty()) {
            query.processDefinitionKeyIn(processDefinitionKeyIn.toArray(new String[0]));
        }
        if (processDefinitionKeyNotIn != null && !processDefinitionKeyNotIn.isEmpty()) {
            query.processDefinitionKeyNotIn(processDefinitionKeyNotIn);
        }
        if (processDefinitionName != null) {
            query.processDefinitionName(processDefinitionName);
        }
        if (processDefinitionNameLike != null) {
            query.processDefinitionNameLike(processDefinitionNameLike);
        }
        if (processInstanceBusinessKey != null) {
            query.processInstanceBusinessKey(processInstanceBusinessKey);
        }
        if (processInstanceBusinessKeyIn != null && !processInstanceBusinessKeyIn.isEmpty()) {
            query.processInstanceBusinessKeyIn(processInstanceBusinessKeyIn.toArray(new String[0]));
        }
        if (processInstanceBusinessKeyLike != null) {
            query.processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
        }
        if (Boolean.TRUE.equals(finished)) {
            query.finished();
        }
        if (Boolean.TRUE.equals(unfinished)) {
            query.unfinished();
        }
        if (Boolean.TRUE.equals(withIncidents)) {
            query.withIncidents();
        }
        if (Boolean.TRUE.equals(withRootIncidents)) {
            query.withRootIncidents();
        }
        if (incidentStatus != null) {
            query.incidentStatus(incidentStatus);
        }
        if (incidentType != null) {
            query.incidentType(incidentType);
        }
        if (incidentMessage != null) {
            query.incidentMessage(incidentMessage);
        }
        if (incidentMessageLike != null) {
            query.incidentMessageLike(incidentMessageLike);
        }
        if (Boolean.TRUE.equals(withJobsRetrying)) {
            query.withJobsRetrying();
        }
        if (caseInstanceId != null) {
            query.caseInstanceId(caseInstanceId);
        }
        if (superProcessInstanceId != null) {
            query.superProcessInstanceId(superProcessInstanceId);
        }
        if (subProcessInstanceId != null) {
            query.subProcessInstanceId(subProcessInstanceId);
        }
        if (superCaseInstanceId != null) {
            query.superCaseInstanceId(superCaseInstanceId);
        }
        if (subCaseInstanceId != null) {
            query.subCaseInstanceId(subCaseInstanceId);
        }
        if (Boolean.TRUE.equals(rootProcessInstances)) {
            query.rootProcessInstances();
        }
        if (startedBefore != null) {
            query.startedBefore(startedBefore);
        }
        if (startedAfter != null) {
            query.startedAfter(startedAfter);
        }
        if (finishedBefore != null) {
            query.finishedBefore(finishedBefore);
        }
        if (finishedAfter != null) {
            query.finishedAfter(finishedAfter);
        }
        if (startedBy != null) {
            query.startedBy(startedBy);
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (Boolean.TRUE.equals(completed)) {
            query.completed();
        }
        if (Boolean.TRUE.equals(externallyTerminated)) {
            query.externallyTerminated();
        }
        if (Boolean.TRUE.equals(internallyTerminated)) {
            query.internallyTerminated();
        }
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        return query;
    }
}
