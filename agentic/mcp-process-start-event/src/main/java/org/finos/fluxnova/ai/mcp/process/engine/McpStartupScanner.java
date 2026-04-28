package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.engine.extractor.StartEventToolExtractor;
import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.finos.fluxnova.bpm.model.bpmn.BpmnModelInstance;
import org.finos.fluxnova.bpm.model.bpmn.instance.StartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static org.finos.fluxnova.ai.mcp.process.model.MCPConstants.MCP_NAMESPACE;

/**
 * Scans existing deployed process definitions at application startup to register MCP tools.
 * <p>
 * This scanner complements {@link McpParseListener} by handling processes that were already
 * deployed before the MCP plugin was activated. While {@code McpParseListener} registers tools
 * during new deployments, this scanner ensures that existing processes are also exposed as
 * MCP tools.
 * </p>
 * <p>
 * The scanner:
 * <ul>
 *   <li>Queries the repository for all latest-version process definitions</li>
 *   <li>Retrieves and parses the BPMN XML for each process</li>
 *   <li>Extracts MCP tool metadata from start events</li>
 *   <li>Registers discovered tools with the {@code ToolRegistry}, avoiding duplicates.</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Lifecycle:</strong> This scanner is typically invoked once during application startup,
 * after the process engine has initialized but before the application accepts requests.
 * </p>
 * <p>
 * <strong>Error Handling:</strong> Failures to scan individual processes are logged but do not
 * prevent the scanner from continuing with remaining processes. This ensures partial failures
 * don't block application startup.
 * </p>
 */
public class McpStartupScanner {
    private static final Logger LOG = LoggerFactory.getLogger(McpStartupScanner.class);

    private final RepositoryService repositoryService;
    private final ToolFactory factory;

    public McpStartupScanner(RepositoryService repositoryService, ToolFactory factory) {
        this.repositoryService = repositoryService;
        this.factory = factory;
    }

    public void scanAndRegisterExistingProcesses() {
        LOG.debug("MCP - Scanning existing process definitions for MCP tools");

        List<ProcessDefinition> definitions = repositoryService
                .createProcessDefinitionQuery()
                .latestVersion() // TODO
                .list();

        for (ProcessDefinition definition : definitions) {
            try {
                scanProcessDefinition(definition);
            } catch (Exception e) {
                LOG.error("MCP - Failed to scan process: {}", definition.getKey(), e);
            }
        }

        LOG.info("MCP - McpStartupScanner complete.");
    }


    private void scanProcessDefinition(ProcessDefinition definition) {
        // this 2nd database hit unavoidable. Since it only runs at startup i think the current impl is acceptable.
        BpmnModelInstance model = repositoryService.getBpmnModelInstance(definition.getId());
        Collection<StartEvent> startEvents = model.getModelElementsByType(StartEvent.class);

        for (StartEvent startEvent : startEvents) {
            String type = startEvent.getAttributeValueNs(MCP_NAMESPACE, "type");
            if ("mcpToolStart".equals(type)) {
                String toolName = startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName");
                if (factory.toolAlreadyRegistered(toolName)) {
                    // MCP enforces unique tool names, skipping duplicates
                    LOG.warn("MCP - Tool already registered: {}. This instance will not be registered.", toolName);
                    continue;
                }
                ToolDefinition toolDefinition = new StartEventToolExtractor().extract(startEvent, definition.getKey());
                factory.createAndRegister(toolDefinition);
            }
        }
    }

}