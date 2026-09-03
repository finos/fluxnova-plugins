package org.finos.fluxnova.bpm.engine.ai.a2a.autoconfigure;

import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthConfigValidator;
import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthProperties;
import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthProvider;
import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthProviderImpl;
import org.finos.fluxnova.bpm.engine.ai.a2a.discovery.AgentCardCache;
import org.finos.fluxnova.bpm.engine.ai.a2a.discovery.AgentCardRefreshScheduler;
import org.finos.fluxnova.bpm.engine.ai.a2a.engine.A2aRemoteCallPlugin;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring Boot auto-configuration for the A2A Remote Call module.
 * <p>
 * Provides default beans for:
 * <ul>
 *   <li>{@link A2aInvocationService} — JSON-RPC invocation with configurable read timeout</li>
 *   <li>{@link AgentCardCache} — agent card discovery with TTL-based expiry and fixed 5s timeouts</li>
 *   <li>{@link AgentCardRefreshScheduler} — periodic background refresh of stale agent cards</li>
 *   <li>{@link A2aRemoteCallPlugin} — process engine plugin wired with Spring-managed dependencies</li>
 * </ul>
 * All beans are annotated with {@link ConditionalOnMissingBean} to allow overriding.
 */
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(A2aAuthProperties.class)
public class A2aRemoteCallAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public A2aAuthProvider a2aAuthProvider(A2aAuthProperties properties) {
        return new A2aAuthProviderImpl(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2aAuthConfigValidator a2aAuthConfigValidator(A2aAuthProperties properties) {
        return new A2aAuthConfigValidator(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2aInvocationService a2aInvocationService(
            @Value("${fluxnova.a2a.timeout:30s}") Duration readTimeout,
            A2aAuthProvider authProvider) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        RestClient restClient = RestClient.builder().requestFactory(factory).build();
        return new A2aInvocationService(restClient, authProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentCardCache agentCardCache(
            A2aAuthProvider authProvider,
            @Value("${fluxnova.a2a.agent-card.ttl:PT5M}") Duration ttl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        RestClient restClient = RestClient.builder().requestFactory(factory).build();
        return new AgentCardCache(restClient, authProvider, ttl);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentCardRefreshScheduler agentCardRefreshScheduler(AgentCardCache agentCardCache) {
        return new AgentCardRefreshScheduler(agentCardCache);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2aRemoteCallPlugin a2aRemoteCallPlugin(
            A2aInvocationService a2aInvocationService) {
        return new A2aRemoteCallPlugin(a2aInvocationService);
    }
}
