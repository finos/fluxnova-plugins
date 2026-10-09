package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers A2A remote call components with the process engine.
 * <p>
 * This plugin:
 * <ul>
 *   <li>Registers the {@link A2aRemoteCallParseListener} as a custom post-BPMN parse listener</li>
 *   <li>Wires the {@link A2aRemoteCallActivityBehaviour} factory for
 *       {@code fluxnova:type="a2a-remote"} service tasks via the parse listener</li>
 * </ul>
 * <p>
 * Registered via SPI: {@code META-INF/services/org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEnginePlugin}
 * <p>
 * In Spring Boot environments, this plugin is typically created by
 * {@code A2aRemoteCallAutoConfiguration} which provides the required dependencies.
 * In standalone (non-Spring) SPI mode, the no-arg constructor creates default instances.
 */
public class A2aRemoteCallPlugin implements ProcessEnginePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(A2aRemoteCallPlugin.class);

    private final A2aInvocationService invocationService;

    /**
     * No-arg constructor for SPI-based discovery (standalone mode without Spring).
     * The {@link A2aInvocationService} is not available in SPI-only mode —
     * activity behaviour will not be set during parsing (requires Spring auto-configuration).
     */
    public A2aRemoteCallPlugin() {
        this(null);
    }

    /**
     * Constructor for Spring-managed creation with full dependency injection.
     *
     * @param invocationService the A2A invocation service (may be null in SPI-only mode)
     */
    public A2aRemoteCallPlugin(A2aInvocationService invocationService) {
        this.invocationService = invocationService;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        LOG.debug("A2A - Remote Call Plugin - Registering BPMN parse listener");

        A2aRemoteCallParseListener parseListener =
                new A2aRemoteCallParseListener(invocationService);

        List<BpmnParseListener> listeners = processEngineConfiguration.getCustomPostBPMNParseListeners();
        if (listeners == null) {
            listeners = new ArrayList<>();
            processEngineConfiguration.setCustomPostBPMNParseListeners(listeners);
        }
        listeners.add(parseListener);

        LOG.info("A2A - Remote Call Plugin initialized - BPMN parse listener registered");
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // No post-init actions required
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        // No post-build actions required
    }
}
