package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentConfigAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentToolInvocationAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.autoconfigure.AgentDiscoveryAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.autoconfigure.AgentLlmOrchestratorAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.service.LlmService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AdHocAgentOrchestrationParseListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AgentOrchestratorEnginePlugin;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.AgentSubprocessEntryListener;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.engine.SubprocessToolCompletionListener;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.AgentOrchestrationJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.job.A2aRemoteInvokeJobHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AdHocSubprocessTerminator;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service.AgentTerminationHandler;
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.state.AgentStateManager;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@AutoConfiguration(
        after = {AgentConfigAutoConfiguration.class,
                AgentDiscoveryAutoConfiguration.class,
                AgentLlmOrchestratorAutoConfiguration.class,
                AgentToolInvocationAutoConfiguration.class})
public class AgentOrchestratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentStateManager agentStateManager() {
        return new AgentStateManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentSubprocessEntryListener agentSubprocessEntryListener() {
        return new AgentSubprocessEntryListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public SubprocessToolCompletionListener subprocessToolCompletionListener() {
        return new SubprocessToolCompletionListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentTerminationHandler adHocSubprocessTerminator() {
        return new AdHocSubprocessTerminator();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentOrchestrationJobHandler agentOrchestrationJobHandler(
            AgentConfigRegistry agentConfigRegistry,
            AgentToolCatalogueRegistry toolCatalogueRegistry,
            AgentContextSpecRegistry contextSpecRegistry,
            AgentContextResolver contextResolver,
            LlmService llmService,
            ToolInvocationService toolInvocationService,
            AgentStateManager stateManager,
            AgentTerminationHandler scopeCompleter) {
        return new AgentOrchestrationJobHandler(
                agentConfigRegistry,
                toolCatalogueRegistry,
                contextSpecRegistry,
                contextResolver,
                llmService,
                toolInvocationService,
                stateManager,
                scopeCompleter
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AdHocAgentOrchestrationParseListener AdHocAgentOrchestrationParseListener(
            AgentSubprocessEntryListener entryListener,
            SubprocessToolCompletionListener completionListener) {
        return new AdHocAgentOrchestrationParseListener(entryListener, completionListener);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2aRemoteInvokeJobHandler a2aRemoteInvokeJobHandler(
            A2aInvocationService a2aInvocationService,
            @Lazy RuntimeService runtimeService) {
        return new A2aRemoteInvokeJobHandler(a2aInvocationService, runtimeService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentOrchestratorEnginePlugin agentOrchestratorEnginePlugin(
            AdHocAgentOrchestrationParseListener parseListener) {
        return new AgentOrchestratorEnginePlugin(parseListener);
    }
}
