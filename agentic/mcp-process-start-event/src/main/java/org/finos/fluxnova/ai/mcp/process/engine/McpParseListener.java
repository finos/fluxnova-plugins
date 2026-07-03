package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.engine.extractor.ElementToolExtractor;
import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * BPMN parse listener that intercepts start event parsing to extract and register MCP tool definitions.
 * <p>
 * This listener is registered with the Fluxnova process engine during deployment parsing. When a BPMN
 * process definition is deployed, this listener examines each start event for MCP-specific extension
 * elements (toolName, description, parameters). If found, it creates and registers an MCP tool that
 * can be invoked by MCP clients to start a new process instance.
 * </p>
 * <p>
 * The listener delegates to:
 * <ul>
 *   <li>{@link BpmnStartEventToolExtractor} - to extract MCP metadata from BPMN XML</li>
 *   <li>{@link ToolFactory} - to create and register the tool with the {@code ToolRegistry}</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Lifecycle:</strong> This listener is invoked during BPMN deployment, before the process
 * definition is activated. Tools are registered immediately and become available to MCP clients.
 * </p> *
 */
public class McpParseListener extends AbstractBpmnParseListener {
    private static final Logger LOG = LoggerFactory.getLogger(McpParseListener.class);

    private final ToolFactory factory;

    public McpParseListener(ToolFactory factory) {
        this.factory = factory;
        LOG.debug("MCP - McpParseListener instance created: {}", this);
    }

    @Override
    public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl activity) {
        try {
            String processKey = ((ProcessDefinitionEntity) scope.getProcessDefinition()).getKey();
            String startEventId = activity.getId();

            LOG.debug("MCP - Parsing start event '{}' in process '{}'", startEventId, processKey);
            ToolDefinition toolDefinition = new ElementToolExtractor().extract(startEventElement, processKey);
            
            if (toolDefinition != null) {
                validateReturnVariables(toolDefinition, startEventId);
            }
            
            factory.createAndRegister(toolDefinition);
        } catch (RuntimeException e) {
            LOG.error("MCP - Deployment validation error in process: {}",
                    ((ProcessDefinitionEntity) scope.getProcessDefinition()).getKey(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("MCP - Error processing MCP start event in process: {}",
                    ((ProcessDefinitionEntity) scope.getProcessDefinition()).getKey(), e);
        }
    }

    /**
     * Validates return variables for duplicate parameter names.
     * <p>
     * This method checks if the configured return variables contain any duplicate names.
     * If duplicates are found, deployment fails immediately with a meaningful error message.
     * </p>
     *
     * @param toolDefinition the tool definition containing return variables to validate
     * @param startEventId   the ID of the start event being parsed
     * @throws RuntimeException if duplicate return variable names are detected
     */
    private void validateReturnVariables(ToolDefinition toolDefinition, String startEventId) {
        if (toolDefinition == null || toolDefinition.returnVariables().isEmpty()) {
            return;  // No validation needed
        }

        Set<String> seenNames = new HashSet<>();

        for (ToolParameter returnVar : toolDefinition.returnVariables()) {
            if (seenNames.contains(returnVar.name())) {
                String errorMsg = String.format(
                    "MCP - Deployment validation failed: duplicate return variable name '%s' " +
                    "in start event '%s' of process '%s'. Each return variable must have a unique name.",
                    returnVar.name(), startEventId, toolDefinition.processKey());
                LOG.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            seenNames.add(returnVar.name());
        }

        LOG.debug("MCP - Return variables validation passed for process: {} - {} variables configured",
                toolDefinition.processKey(), toolDefinition.returnVariables().size());
    }
}
