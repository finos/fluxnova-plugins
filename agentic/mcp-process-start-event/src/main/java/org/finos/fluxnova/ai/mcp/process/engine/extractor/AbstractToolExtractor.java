package org.finos.fluxnova.ai.mcp.process.engine.extractor;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractToolExtractor {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractToolExtractor.class);

    protected ToolDefinition buildToolDefinition(String processKey, String toolName, String description,
                                                 String propagateKeyStr, List<ToolParameter> parameters) {
        boolean propagateBusinessKey = propagateKeyStr == null || Boolean.parseBoolean(propagateKeyStr);

        if (propagateBusinessKey) {
            parameters.add(new ToolParameter("businessKey", "String", true));
        }

        ToolDefinition definition = new ToolDefinition(
                processKey,
                toolName,
                description != null ? description : "",
                parameters,
                propagateBusinessKey);

        LOG.debug("MCP - Extracted tool definition: {} for process: {}", toolName, processKey);
        return definition;
    }

    protected void addParameterIfValid(String name, String type, List<ToolParameter> parameters) {
        LOG.debug("MCP - Extracting parameter: name='{}', type='{}'", name, type);

        if (name != null && !name.isBlank() && type != null && !type.isBlank()) {
            parameters.add(new ToolParameter(name, type, false));
            LOG.debug("MCP - Added parameter: name='{}', type='{}'", name, type);
        } else {
            LOG.warn("MCP - Skipping invalid parameter with name='{}' type='{}'", name, type);
        }
    }
}
