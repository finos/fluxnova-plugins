package org.finos.fluxnova.ai.mcp.query.autoconfigure;

import io.modelcontextprotocol.server.McpServerFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Filters individual MCP tool specifications by name.
 * <p>
 * Intercepts the {@code List<SyncToolSpecification>} bean produced by Spring AI's
 * annotation scanner and removes any tools whose names appear in the configured
 * {@link QueryToolsProperties#getExclude() exclude set}.
 */
class ToolSpecificationFilter implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ToolSpecificationFilter.class);

    private final Set<String> excludedTools;

    ToolSpecificationFilter(Set<String> excludedTools) {
        this.excludedTools = excludedTools;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (excludedTools.isEmpty() || !(bean instanceof List<?> list)) {
            return bean;
        }
        if (list.isEmpty() || !(list.getFirst() instanceof McpServerFeatures.SyncToolSpecification)) {
            return bean;
        }
        List<McpServerFeatures.SyncToolSpecification> specs =
                (List<McpServerFeatures.SyncToolSpecification>) bean;
        List<McpServerFeatures.SyncToolSpecification> filtered = new ArrayList<>();
        for (McpServerFeatures.SyncToolSpecification spec : specs) {
            String toolName = spec.tool().name();
            if (excludedTools.contains(toolName)) {
                LOG.info("Excluding MCP tool: {}", toolName);
            } else {
                filtered.add(spec);
            }
        }
        return filtered;
    }
}
