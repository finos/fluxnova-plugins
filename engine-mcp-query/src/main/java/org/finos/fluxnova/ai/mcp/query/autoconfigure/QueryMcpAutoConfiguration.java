package org.finos.fluxnova.ai.mcp.query.autoconfigure;

import org.finos.fluxnova.ai.mcp.query.tools.*;
import org.finos.fluxnova.bpm.engine.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the engine-mcp-query extension.
 * <p>
 * Each tool class is registered as a bean guarded by a service-level toggle
 * (e.g. {@code fluxnova.mcp.query.tools.repository.enabled=false} disables all
 * repository query tools). All services are enabled by default.
 * <p>
 * Individual tools can also be excluded by name via
 * {@code fluxnova.mcp.query.tools.exclude=querySchemaLog,queryBatches}.
 */
@AutoConfiguration
@EnableConfigurationProperties(QueryToolsProperties.class)
@ComponentScan(basePackages = "org.finos.fluxnova.ai.mcp.query.model")
public class QueryMcpAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.repository.enabled",
            havingValue = "true", matchIfMissing = true)
    public RepositoryQueryMcpTools repositoryQueryMcpTools(
            RepositoryService repositoryService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new RepositoryQueryMcpTools(repositoryService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.runtime.enabled",
            havingValue = "true", matchIfMissing = true)
    public RuntimeQueryMcpTools runtimeQueryMcpTools(
            RuntimeService runtimeService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new RuntimeQueryMcpTools(runtimeService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.task.enabled",
            havingValue = "true", matchIfMissing = true)
    public TaskQueryMcpTools taskQueryMcpTools(
            TaskService taskService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new TaskQueryMcpTools(taskService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.history.enabled",
            havingValue = "true", matchIfMissing = true)
    public HistoryQueryMcpTools historyQueryMcpTools(
            HistoryService historyService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new HistoryQueryMcpTools(historyService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.external-task.enabled",
            havingValue = "true", matchIfMissing = true)
    public ExternalTaskQueryMcpTools externalTaskQueryMcpTools(
            ExternalTaskService externalTaskService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new ExternalTaskQueryMcpTools(externalTaskService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.authorization.enabled",
            havingValue = "true", matchIfMissing = true)
    public AuthorizationQueryMcpTools authorizationQueryMcpTools(
            AuthorizationService authorizationService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new AuthorizationQueryMcpTools(authorizationService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.filter.enabled",
            havingValue = "true", matchIfMissing = true)
    public FilterQueryMcpTools filterQueryMcpTools(
            FilterService filterService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new FilterQueryMcpTools(filterService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.case-service.enabled",
            havingValue = "true", matchIfMissing = true)
    public CaseQueryMcpTools caseQueryMcpTools(
            CaseService caseService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new CaseQueryMcpTools(caseService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.identity.enabled",
            havingValue = "true", matchIfMissing = true)
    public IdentityQueryMcpTools identityQueryMcpTools(
            IdentityService identityService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new IdentityQueryMcpTools(identityService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.management.enabled",
            havingValue = "true", matchIfMissing = true)
    public ManagementQueryMcpTools managementQueryMcpTools(
            ManagementService managementService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        return new ManagementQueryMcpTools(managementService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.xml.enabled",
            havingValue = "true", matchIfMissing = true)
    public XMLMcpTools xmlMcpTools(RepositoryService repositoryService) {
        return new XMLMcpTools(repositoryService);
    }

    @Bean
    ToolSpecificationFilter toolSpecificationFilter(QueryToolsProperties properties) {
        return new ToolSpecificationFilter(properties.getExclude());
    }
}
