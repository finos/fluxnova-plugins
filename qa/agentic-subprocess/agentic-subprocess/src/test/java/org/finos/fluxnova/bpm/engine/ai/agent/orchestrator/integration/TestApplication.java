package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.integration;

import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentConfigAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.autoconfigure.AgentToolInvocationAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.autoconfigure.AgentDiscoveryAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.autoconfigure.AgentLlmOrchestratorAutoConfiguration;
import org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.autoconfigure.AgentOrchestratorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Exclude all plugin auto-configurations — their @ConditionalOnBean checks fail because
// they are evaluated before FluxnovaBpmAutoConfiguration (alphabetical ordering).
// Plugin beans are wired explicitly in TestConfig instead.
// AgentLlmOrchestratorAutoConfiguration is also excluded: it registers AgentProviderParseListener
// which validates provider names at deploy time, but tests mock LlmService directly and
// use provider="test" which has no real registration.
@SpringBootApplication(exclude = {AgentConfigAutoConfiguration.class,
                AgentDiscoveryAutoConfiguration.class, AgentToolInvocationAutoConfiguration.class,
                AgentLlmOrchestratorAutoConfiguration.class,
                AgentOrchestratorAutoConfiguration.class})
public class TestApplication {
}
