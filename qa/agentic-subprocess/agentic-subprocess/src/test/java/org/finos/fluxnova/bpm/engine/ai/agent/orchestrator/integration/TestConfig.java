package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.integration;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentConfigEnginePlugin;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AdHocSubProcessCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.BpmnExtensionContextSpecBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.extract.AgentConfigExtractor;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AdHocAgentOrchestrationParseListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AgentOrchestratorEnginePlugin;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AgentSubprocessEntryListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.SubprocessToolCompletionListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AdHocSubprocessTerminator;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AgentTerminationHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.service.AdHocActivityToolInvocationServiceImpl;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.jobexecutor.JobHandler;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Wires all five agentic plugins manually. The plugin auto-configurations are excluded because
 * their {@code @ConditionalOnBean} checks fail due to auto-config ordering (they evaluate before
 * FluxnovaBpmAutoConfiguration creates engine service beans).
 *
 * <p>
 * Only {@link LlmOrchestrationService} is mocked — everything else uses real implementations backed
 * by the in-memory Fluxnova engine.
 *
 * <p>
 * Beans that depend on engine services (RuntimeService, RepositoryService) use the
 * {@link org.springframework.beans.factory.ObjectProvider} pattern to break the circular
 * dependency: engine plugins are collected during engine creation, but registries/services need the
 * engine to exist first. ObjectProvider defers resolution until first use (process deployment or
 * job execution), by which time the engine is fully initialised.
 */
@TestConfiguration
public class TestConfig {

    // -- Mock boundary --

    @Bean
    public LlmService llmOrchestrationService() {
        return Mockito.mock(LlmService.class);
    }

    // -- agent-config plugin --

    @Bean
    public AgentConfigExtractor agentConfigExtractor() {
        return new AgentConfigExtractor();
    }

    @Bean
    public AgentConfigRegistry agentConfigRegistry(ObjectProvider<RepositoryService> repositoryService,
                                                   AgentConfigExtractor extractor) {
        return new AgentConfigRegistry(repositoryService, extractor);
    }

    @Bean
    public AgentConfigEnginePlugin agentConfigEnginePlugin() {
        return new AgentConfigEnginePlugin();
    }

    // -- agent-tool-context-discovery plugin --

    @Bean
    public AgentToolCatalogueBuilder agentToolCatalogueBuilder() {
        return new AdHocSubProcessCatalogueBuilder();
    }

    @Bean
    public AgentContextSpecBuilder agentContextSpecBuilder() {
        return new BpmnExtensionContextSpecBuilder();
    }

    @Bean
    public AgentToolCatalogueRegistry agentToolCatalogueRegistry(
            ObjectProvider<RepositoryService> repositoryService, AgentConfigRegistry configRegistry,
            AgentToolCatalogueBuilder builder) {
        return new AgentToolCatalogueRegistry(repositoryService, configRegistry, builder);
    }

    @Bean
    public AgentContextSpecRegistry agentContextSpecRegistry(ObjectProvider<RepositoryService> repositoryService,
                                                             AgentConfigRegistry configRegistry, AgentContextSpecBuilder builder) {
        return new AgentContextSpecRegistry(repositoryService, configRegistry, builder);
    }

    @Bean
    public AgentContextResolver agentContextResolver(ObjectProvider<RuntimeService> runtimeService) {
        return new AgentContextResolver(runtimeService);
    }

    // -- agent-tool-invocation plugin --
    // The real AdHocActivityToolInvocationServiceImpl is used here.
    // The test BPMN suppresses the default ad-hoc completion condition via
    // <completionCondition>${false}</completionCondition>, which prevents the subprocess from
    // terminating prematurely after the first synchronous tool activity completes.
    // To be reviewed with final ad-hoc subprocess semantics.

    @Bean
    public ToolInvocationService toolInvocationService(ObjectProvider<RuntimeService> runtimeService) {
        return new AdHocActivityToolInvocationServiceImpl(runtimeService);
    }

    // -- agent-orchestrator plugin --

    @Bean
    public AgentStateManager agentStateManager(ObjectProvider<RuntimeService> runtimeService) {
        return new AgentStateManager(runtimeService);
    }

    @Bean
    public AgentSubprocessEntryListener agentSubprocessEntryListener() {
        return new AgentSubprocessEntryListener();
    }

    @Bean
    public SubprocessToolCompletionListener subprocessToolCompletionListener() {
        return new SubprocessToolCompletionListener();
    }

    @Bean
    public AgentTerminationHandler agentTerminationHandler(ObjectProvider<RuntimeService> runtimeService) {
        return new AdHocSubprocessTerminator(runtimeService);
    }

    @Bean
    public AdHocAgentOrchestrationParseListener adHocAgentOrchestrationParseListener(
            AgentSubprocessEntryListener entryListener,
            SubprocessToolCompletionListener completionListener) {
        return new AdHocAgentOrchestrationParseListener(entryListener, completionListener);
    }

    @Bean
    public AgentOrchestratorEnginePlugin agentOrchestratorEnginePlugin(
            AdHocAgentOrchestrationParseListener parseListener) {
        return new AgentOrchestratorEnginePlugin(parseListener);
    }

    // The job handler and its registration plugin are combined into a single bean.
    // All engine-service-dependent beans use ObjectProvider internally, so there is
    // no circular dependency at construction time. Resolution happens at job execution
    // time, when the engine is fully available.
    //
    // Job handler registration is currently missing from AgentOrchestratorEnginePlugin
    // — this plugin fills that gap until the production code is updated.
    @Bean
    public AbstractProcessEnginePlugin jobHandlerRegistrationPlugin(
            AgentConfigRegistry configRegistry,
            AgentToolCatalogueRegistry toolCatalogueRegistry,
            AgentContextSpecRegistry contextSpecRegistry,
            AgentContextResolver contextResolver,
            LlmService llmOrchestrationService,
            ToolInvocationService toolInvocationService, AgentStateManager stateManager,
            AgentTerminationHandler terminationHandler) {
        AgentOrchestrationJobHandler handler = new AgentOrchestrationJobHandler(configRegistry,
                toolCatalogueRegistry, contextSpecRegistry, contextResolver,
                llmOrchestrationService, toolInvocationService, stateManager, terminationHandler);
        return new AbstractProcessEnginePlugin() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl config) {
                List<JobHandler> handlers = config.getCustomJobHandlers();
                if (handlers == null) {
                    handlers = new ArrayList<>();
                    config.setCustomJobHandlers(handlers);
                }
                handlers.add(handler);
            }
        };
    }
}
