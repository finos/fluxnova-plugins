package org.finos.fluxnova.ai.mcp.query.plugins;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpWorkflowQueryPlugin implements ProcessEnginePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(McpWorkflowQueryPlugin.class);


    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        LOG.debug("MCP - Workflow Query Plugin - Pre-initialization complete");
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        LOG.debug("MCP - Workflow Query Plugin - Post-initialization complete");
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        LOG.info("MCP - Workflow Query Plugin - Process engine built");
    }
}
