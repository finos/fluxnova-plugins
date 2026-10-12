package org.finos.fluxnova.ai.mcp.query.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstanceQuery;

import java.util.HashSet;
import java.util.List;

/**
 * DTO for querying process instances via the process engine Query API.
 * All fields are optional filter criteria.
 */
@Schema(description = "Query parameters for filtering process instances.")
public record ProcessInstanceQueryDto(
        @Schema(description = "Filter by process instance id.")
        String processInstanceId,

        @Schema(description = "Filter by a list of process instance ids.")
        List<String> processInstanceIds,

        @Schema(description = "Filter by process instance business key.")
        String businessKey,

        @Schema(description = "Filter by process instance business key that the parameter is a substring of. "
                + "The string can include the wildcard character '%' to express like-strategy: "
                + "starts with (string%), ends with (%string) or contains (%string%).")
        String businessKeyLike,

        @Schema(description = "Filter by the key of the process definition the instances run on.")
        String processDefinitionKey,

        @Schema(description = "Filter by a list of process definition keys. "
                + "A process instance must have one of the given process definition keys.")
        List<String> processDefinitionKeyIn,

        @Schema(description = "Exclude instances by a list of process definition keys. "
                + "A process instance must not have one of the given process definition keys.")
        List<String> processDefinitionKeyNotIn,

        @Schema(description = "Filter by the id of the process definition the instances run on.")
        String processDefinitionId,

        @Schema(description = "Filter by the deployment the process instance belongs to.")
        String deploymentId,

        @Schema(description = "Restrict query to all process instances that are sub process instances of the given process instance. "
                + "Takes a process instance id.")
        String superProcessInstanceId,

        @Schema(description = "Restrict query to all process instances that have the given process instance as a sub process instance. "
                + "Takes a process instance id.")
        String subProcessInstanceId,

        @Schema(description = "Filter by case instance id.")
        String caseInstanceId,

        @Schema(description = "Restrict query to all process instances that are sub process instances of the given case instance. "
                + "Takes a case instance id.")
        String superCaseInstanceId,

        @Schema(description = "Restrict query to all process instances that have the given case instance as a sub case instance. "
                + "Takes a case instance id.")
        String subCaseInstanceId,

        @Schema(description = "Only include active process instances. "
                + "Value may only be true, as false is the default behavior.")
        Boolean active,

        @Schema(description = "Only include suspended process instances. "
                + "Value may only be true, as false is the default behavior.")
        Boolean suspended,

        @Schema(description = "Filter by presence of incidents. Selects only process instances that have an incident.")
        Boolean withIncident,

        @Schema(description = "Filter by the incident id.")
        String incidentId,

        @Schema(description = "Filter by incident type.", allowableValues = {"failedJob", "failedExternalTask"})
        String incidentType,

        @Schema(description = "Filter by the incident message. Exact match.")
        String incidentMessage,

        @Schema(description = "Filter by the incident message that the parameter is a substring of.")
        String incidentMessageLike,

        @Schema(description = "Filter by a list of tenant ids. A process instance must have one of the given tenant ids.")
        List<String> tenantIdIn,

        @Schema(description = "Only include process instances which belong to no tenant.")
        Boolean withoutTenantId,

        @Schema(description = "Only include process instances which process definition has no tenant id.")
        Boolean processDefinitionWithoutTenantId,

        @Schema(description = "Filter by a list of activity ids. "
                + "A process instance must currently wait in a leaf activity with one of the given activity ids.")
        List<String> activityIdIn,

        @Schema(description = "Restrict the query to all process instances that are top level process instances.")
        Boolean rootProcessInstances,

        @Schema(description = "Restrict the query to all process instances that are leaf instances "
                + "(i.e. don't have any sub instances).")
        Boolean leafProcessInstances,

        @Schema(description = "Match all variable names case-insensitively when used with variable value filters.")
        Boolean variableNamesIgnoreCase,

        @Schema(description = "Match all variable values case-insensitively when used with variable value filters.")
        Boolean variableValuesIgnoreCase
) {
    public ProcessInstanceQuery toQuery(RuntimeService runtimeService) {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        if (processInstanceId != null) {
            query.processInstanceId(processInstanceId);
        }
        if (processInstanceIds != null && !processInstanceIds.isEmpty()) {
            query.processInstanceIds(new HashSet<>(processInstanceIds));
        }
        if (businessKey != null) {
            query.processInstanceBusinessKey(businessKey);
        }
        if (businessKeyLike != null) {
            query.processInstanceBusinessKeyLike(businessKeyLike);
        }
        if (processDefinitionKey != null) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (processDefinitionKeyIn != null && !processDefinitionKeyIn.isEmpty()) {
            query.processDefinitionKeyIn(processDefinitionKeyIn.toArray(new String[0]));
        }
        if (processDefinitionKeyNotIn != null && !processDefinitionKeyNotIn.isEmpty()) {
            query.processDefinitionKeyNotIn(processDefinitionKeyNotIn.toArray(new String[0]));
        }
        if (processDefinitionId != null) {
            query.processDefinitionId(processDefinitionId);
        }
        if (deploymentId != null) {
            query.deploymentId(deploymentId);
        }
        if (superProcessInstanceId != null) {
            query.superProcessInstanceId(superProcessInstanceId);
        }
        if (subProcessInstanceId != null) {
            query.subProcessInstanceId(subProcessInstanceId);
        }
        if (caseInstanceId != null) {
            query.caseInstanceId(caseInstanceId);
        }
        if (superCaseInstanceId != null) {
            query.superCaseInstanceId(superCaseInstanceId);
        }
        if (subCaseInstanceId != null) {
            query.subCaseInstanceId(subCaseInstanceId);
        }
        if (Boolean.TRUE.equals(active)) {
            query.active();
        }
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        }
        if (Boolean.TRUE.equals(withIncident)) {
            query.withIncident();
        }
        if (incidentId != null) {
            query.incidentId(incidentId);
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
        if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
            query.tenantIdIn(tenantIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(withoutTenantId)) {
            query.withoutTenantId();
        }
        if (Boolean.TRUE.equals(processDefinitionWithoutTenantId)) {
            query.processDefinitionWithoutTenantId();
        }
        if (activityIdIn != null && !activityIdIn.isEmpty()) {
            query.activityIdIn(activityIdIn.toArray(new String[0]));
        }
        if (Boolean.TRUE.equals(rootProcessInstances)) {
            query.rootProcessInstances();
        }
        if (Boolean.TRUE.equals(leafProcessInstances)) {
            query.leafProcessInstances();
        }
        if (Boolean.TRUE.equals(variableNamesIgnoreCase)) {
            query.matchVariableNamesIgnoreCase();
        }
        if (Boolean.TRUE.equals(variableValuesIgnoreCase)) {
            query.matchVariableValuesIgnoreCase();
        }
        return query;
    }
}
