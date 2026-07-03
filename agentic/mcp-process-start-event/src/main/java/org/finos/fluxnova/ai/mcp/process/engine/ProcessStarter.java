package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles starting Fluxnova process instances from MCP tool invocations.
 * 
 * Uses the Fluxnova builder pattern API with {@code createProcessInstanceByKey().setVariables().executeWithVariablesInReturn()}
 * to efficiently capture process-scope variables at the first async boundary.
 *
 * <h2>Return Variables</h2>
 * 
 * This class supports optional configuration of process-scope variables 
 * that should be returned to the MCP client in the process start response.
 * Return variables are resolved exclusively from process-scope variables. 
 * Execution-local variables are not considered. Returned values represent 
 * a snapshot of process-scope variables at the first asynchronous/wait-state 
 * commit boundary encountered during execution.
 * 
 * <h2>Response Sizes</h2>
 * 
 * <strong>Warning:</strong> Returned variables are embedded directly into 
 * the MCP response. Returning large collections, large object graphs, large 
 * JSON payloads, files, or binary content may significantly increase response 
 * size, serialization cost, memory utilisation, and MCP response latency. 
 * No framework size restrictions are enforced. BPMN authors are responsible 
 * for ensuring that returned variables are appropriately sized for AI consumption.
 */
public class ProcessStarter {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessStarter.class);

    private final RuntimeService runtimeService;
    private final VariableSerializer variableSerializer;

    public ProcessStarter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
        this.variableSerializer = new VariableSerializer();
    }

    /**
     * Starts a Fluxnova process instance with the provided arguments.
     * 
     * Uses the Fluxnova builder pattern API ({@code createProcessInstanceByKey().setVariables().executeWithVariablesInReturn()})
     * to efficiently capture process-scope variables at the natural return point. If return variables are configured, 
     * collects them from the pre-captured process variables after process execution reaches the natural return point. 
     * Variables are serialized using Fluxnova/Spin capabilities.
     * 
     * @param definition the tool definition with optional return variables
     * @param arguments  the arguments passed to the tool
     * @return a map containing process instance details and optional return variables
     * @throws RuntimeException if process execution fails
     */
    public Map<String, Object> startProcess(ToolDefinition definition, Map<String, Object> arguments) {
        try {
            LOG.info("MCP - Starting process '{}' via MCP tool '{}' with {} return variables configured",
                    definition.processKey(), definition.toolName(), definition.returnVariables().size());

            String businessKey = arguments.get("businessKey") != null
                    ? arguments.get("businessKey").toString()
                    : null;

            // Use builder pattern to capture variables at first async boundary
            ProcessInstanceWithVariables instanceWithVars = runtimeService
                    .createProcessInstanceByKey(definition.processKey())
                    .businessKey(businessKey)
                    .setVariables(arguments)
                    .executeWithVariablesInReturn();

            LOG.info("MCP - Process instance created: {} for tool '{}' with business key '{}'",
                    instanceWithVars.getId(), definition.toolName(), businessKey);
            
            LOG.debug("startProcess: instanceWithVars = {}", instanceWithVars);
            LOG.debug("startProcess: calling buildResponse with instanceWithVars");

            // Build response with configured return variables
            return buildResponse(definition, instanceWithVars, businessKey);

        } catch (Exception e) {
            LOG.error("MCP - Failed to start process '{}' via tool '{}'",
                    definition.processKey(), definition.toolName(), e);
            throw new RuntimeException("Failed to start process: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the MCP response including return variables if configured.
     *
     * Extracts the process instance ID and captured variables from ProcessInstanceWithVariables.
     * If return variable retrieval or serialization fails for an individual variable,
     * that variable is included in the response with a null value and processing continues.
     *
     * @param definition the tool definition
     * @param instanceWithVars the started process instance with pre-captured variables
     * @param businessKey the business key if provided
     * @return response map with processInstanceId, businessKey, message, and optional values
     */
    private Map<String, Object> buildResponse(ToolDefinition definition, ProcessInstanceWithVariables instanceWithVars, String businessKey) {
        // ProcessInstanceWithVariables extends ProcessInstance and provides getVariables()
        LOG.debug("buildResponse: instanceWithVars = {}", instanceWithVars);
        LOG.debug("buildResponse: calling getVariables()");

        Object variablesObj = instanceWithVars.getVariables();
        LOG.debug("buildResponse: variablesObj = {}, class = {}", variablesObj, (variablesObj != null ? variablesObj.getClass() : "null"));
        Map<String, Object> capturedVariables = variableSerializer.convertToMap(variablesObj);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("processInstanceId", instanceWithVars.getId());
        response.put("businessKey", businessKey != null ? businessKey : "");
        response.put("message", String.format(
                "Process %s started successfully. ProcessInstanceId=%s BusinessKey=%s",
                definition.processKey(), instanceWithVars.getId(), businessKey != null ? businessKey : ""
        ));

        // Collect return variables if configured
        if (!definition.returnVariables().isEmpty()) {
            Map<String, Object> values = variableSerializer.collectReturnVariables(definition, capturedVariables);
            response.put("values", values);
            LOG.debug("MCP - Return variables collected for process instance {}: {} variables",
                    instanceWithVars.getId(), values.size());
        }

        return response;
    }
}
