package org.finos.fluxnova.ai.mcp.process.engine.extractor;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;

import java.util.ArrayList;
import java.util.List;

import static org.finos.fluxnova.ai.mcp.process.model.MCPConstants.MCP_NAMESPACE;

public class ElementToolExtractor extends AbstractToolExtractor {
    private static final Namespace MCP_NS = new Namespace(MCP_NAMESPACE, "mcp");

    public ToolDefinition extract(Element startEvent, String processKey) {
        try {
            String toolName = startEvent.attributeNS(MCP_NS, "toolName");
            if (toolName == null || toolName.isBlank()) {
                return null;
            }

            String description = startEvent.attributeNS(MCP_NS, "description");
            String propagateKeyStr = startEvent.attributeNS(MCP_NS, "propagateBusinessKey");
            List<ToolParameter> parameters = extractParameters(startEvent);
            List<ToolParameter> returnVariables = extractReturnVariables(startEvent);
            
            LOG.debug("MCP - Extracted {} return variables for tool definition", returnVariables.size());

            return buildToolDefinition(processKey, toolName, description, propagateKeyStr, parameters, returnVariables);
        } catch (Exception e) {
            LOG.error("MCP - Failed to extract tool definition from Element in process: {}", processKey, e);
            return null;
        }
    }

    private List<ToolParameter> extractParameters(Element startEvent) {
        List<ToolParameter> parameters = new ArrayList<>();

        Element extensionElements = startEvent.element("extensionElements");
        if (extensionElements == null) {
            return parameters;
        }

        Element parametersElement = extensionElements.elementNS(MCP_NS, "parameters");
        if (parametersElement == null) {
            return parameters;
        }

        List<Element> paramElements = parametersElement.elementsNS(MCP_NS, "parameter");
        LOG.debug("MCP - Found {} parameter elements", paramElements.size());

        for (Element paramElement : paramElements) {
            String name = paramElement.attribute("paramName");
            String type = paramElement.attribute("paramType");
            addParameterIfValid(name, type, parameters);
        }

        LOG.debug("MCP - Total parameters extracted: {}", parameters.size());
        return parameters;
    }

    private List<ToolParameter> extractReturnVariables(Element startEvent) {
        List<ToolParameter> returnVariables = new ArrayList<>();

        Element extensionElements = startEvent.element("extensionElements");
        if (extensionElements == null) {
            return returnVariables;
        }

        Element returnVarsElement = extensionElements.elementNS(MCP_NS, "returnVariables");
        if (returnVarsElement == null) {
            return returnVariables;
        }

        List<Element> returnVarElements = returnVarsElement.elementsNS(MCP_NS, "returnVariable");
        LOG.debug("MCP - Found {} return variable elements", returnVarElements.size());

        for (Element returnVarElement : returnVarElements) {
            String name = returnVarElement.attribute("paramName");
            String type = returnVarElement.attribute("paramType");
            addParameterIfValid(name, type, returnVariables);
        }

        LOG.debug("MCP - Total return variables extracted: {}", returnVariables.size());
        return returnVariables;
    }
}
