package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.autoconfigure;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentConfigAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.autoconfigure.AgentDiscoveryAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentContextSpecRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry.AgentToolCatalogueRegistry;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime.AgentContextResolver;
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
import org.finos.fluxnova.bpm.engine.ai.agent.service.ToolInvocationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(
        after = {AgentConfigAutoConfiguration.class, AgentDiscoveryAutoConfiguration.class})
@ConditionalOnBean({RuntimeService.class, LlmService.class,
        ToolInvocationService.class})
public class AgentOrchestratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentStateManager agentStateManager(ObjectProvider<RuntimeService> runtimeService) {
        return new AgentStateManager(runtimeService);
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
    public AgentTerminationHandler adHocSubprocessTerminator(ObjectProvider<RuntimeService> runtimeService) {
        return new AdHocSubprocessTerminator(runtimeService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentOrchestrationJobHandler agentOrchestrationJobHandler(
            AgentConfigRegistry agentConfigRegistry,
            AgentToolCatalogueRegistry toolCatalogueRegistry,
            AgentContextSpecRegistry contextSpecRegistry, AgentContextResolver contextResolver,
            LlmService llmService,
            ToolInvocationService toolInvocationService, AgentStateManager stateManager,
            AgentTerminationHandler scopeCompleter) {
        return new AgentOrchestrationJobHandler(agentConfigRegistry, toolCatalogueRegistry,
                contextSpecRegistry, contextResolver, llmService,
                toolInvocationService, stateManager, scopeCompleter);
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
    public AgentOrchestratorEnginePlugin agentOrchestratorEnginePlugin(
            AdHocAgentOrchestrationParseListener parseListener) {
        return new AgentOrchestratorEnginePlugin(parseListener);
    }
}
