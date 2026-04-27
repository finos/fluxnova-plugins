package org.finos.fluxnova.ai.mcp.process.engine.extractor;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.ai.mcp.process.model.ToolParameter;
import org.finos.fluxnova.bpm.model.bpmn.instance.ExtensionElements;
import org.finos.fluxnova.bpm.model.bpmn.instance.StartEvent;
import org.finos.fluxnova.bpm.model.xml.instance.DomElement;

import java.util.ArrayList;
import java.util.List;

import static org.finos.fluxnova.ai.mcp.process.model.MCPConstants.MCP_NAMESPACE;

public class StartEventToolExtractor extends AbstractToolExtractor {

    public ToolDefinition extract(StartEvent startEvent, String processKey) {
        try {
            String toolName = startEvent.getAttributeValueNs(MCP_NAMESPACE, "toolName");
            if (toolName == null || toolName.isBlank()) {
                return null;
            }

            String description = startEvent.getAttributeValueNs(MCP_NAMESPACE, "description");
            String propagateKeyStr = startEvent.getAttributeValueNs(MCP_NAMESPACE, "propagateBusinessKey");
            List<ToolParameter> parameters = extractParameters(startEvent);

            return buildToolDefinition(processKey, toolName, description, propagateKeyStr, parameters);
        } catch (Exception e) {
            LOG.error("MCP - Failed to extract tool definition from StartEvent in process: {}", processKey, e);
            return null;
        }
    }

    private List<ToolParameter> extractParameters(StartEvent startEvent) {
        List<ToolParameter> parameters = new ArrayList<>();

        ExtensionElements extensionElements = startEvent.getExtensionElements();
        if (extensionElements == null) {
            return parameters;
        }

        DomElement domExtensions = extensionElements.getDomElement();
        DomElement parametersElement = findParametersElement(domExtensions);

        if (parametersElement == null) {
            return parameters;
        }

        List<DomElement> paramElements = parametersElement.getChildElements();
        LOG.debug("MCP - Found {} parameter elements", paramElements.size());

        for (DomElement paramElement : paramElements) {
            if (!"parameter".equals(paramElement.getLocalName()) ||
                    !MCP_NAMESPACE.equals(paramElement.getNamespaceURI())) {
                continue;
            }

            String name = paramElement.getAttribute("paramName");
            String type = paramElement.getAttribute("paramType");
            addParameterIfValid(name, type, parameters);
        }

        LOG.debug("MCP - Total parameters extracted: {}", parameters.size());
        return parameters;
    }

    private DomElement findParametersElement(DomElement domExtensions) {
        for (DomElement child : domExtensions.getChildElements()) {
            if ("parameters".equals(child.getLocalName()) &&
                    MCP_NAMESPACE.equals(child.getNamespaceURI())) {
                return child;
            }
        }
        return null;
    }
}
