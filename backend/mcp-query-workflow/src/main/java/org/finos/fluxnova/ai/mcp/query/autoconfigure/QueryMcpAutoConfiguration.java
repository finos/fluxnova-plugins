package org.finos.fluxnova.ai.mcp.query.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.plugins.McpWorkflowQueryPlugin;
import org.finos.fluxnova.ai.mcp.query.registry.QueryToolRegistrar;
import org.finos.fluxnova.ai.mcp.query.tools.*;
import org.finos.fluxnova.ai.mcp.server.autoconfigure.McpServerSpringAutoConfiguration;
import org.finos.fluxnova.ai.mcp.server.registry.ToolRegistry;
import org.finos.fluxnova.bpm.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the mcp-workflow-query extension.
 * <p>
 * Each tool class is registered as a bean guarded by a service-level toggle
 * (e.g. {@code fluxnova.mcp.query.tools.repository.enabled=false} disables all
 * repository query tools). All services are enabled by default.
 * <p>
 * Individual tools can also be excluded by name via
 * {@code fluxnova.mcp.query.tools.exclude=querySchemaLog,queryBatches}.
 * <p>
 * Tools are registered programmatically with the {@link ToolRegistry} from
 * mcp-server-plugin via the {@link QueryToolRegistrar}.
 */
@AutoConfiguration(after = McpServerSpringAutoConfiguration.class)
@EnableConfigurationProperties(QueryToolsProperties.class)
@ComponentScan(basePackages = "org.finos.fluxnova.ai.mcp.query.model")
public class QueryMcpAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(QueryMcpAutoConfiguration.class);

    @Bean
    @ConditionalOnBean(ToolRegistry.class)
    public ApplicationRunner queryToolRegistrationRunner(
            QueryToolRegistrar registrar,
            ObjectProvider<RepositoryQueryMcpTools> repositoryTools,
            ObjectProvider<RuntimeQueryMcpTools> runtimeTools,
            ObjectProvider<TaskQueryMcpTools> taskTools,
            ObjectProvider<HistoryQueryMcpTools> historyTools,
            ObjectProvider<ExternalTaskQueryMcpTools> externalTaskTools,
            ObjectProvider<AuthorizationQueryMcpTools> authorizationTools,
            ObjectProvider<FilterQueryMcpTools> filterTools,
            ObjectProvider<CaseQueryMcpTools> caseTools,
            ObjectProvider<IdentityQueryMcpTools> identityTools,
            ObjectProvider<ManagementQueryMcpTools> managementTools,
            ObjectProvider<XMLMcpTools> xmlTools) {
        return args -> {
            log.info("MCP - Registering query workflow tools with ToolRegistry");

            repositoryTools.ifAvailable(registrar::registerRepositoryTools);
            runtimeTools.ifAvailable(registrar::registerRuntimeTools);
            taskTools.ifAvailable(registrar::registerTaskTools);
            historyTools.ifAvailable(registrar::registerHistoryTools);
            externalTaskTools.ifAvailable(registrar::registerExternalTaskTools);
            authorizationTools.ifAvailable(registrar::registerAuthorizationTools);
            filterTools.ifAvailable(registrar::registerFilterTools);
            caseTools.ifAvailable(registrar::registerCaseTools);
            identityTools.ifAvailable(registrar::registerIdentityTools);
            managementTools.ifAvailable(registrar::registerManagementTools);
            xmlTools.ifAvailable(registrar::registerXmlTools);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.repository.enabled",
            havingValue = "true", matchIfMissing = true)
    public RepositoryQueryMcpTools repositoryQueryMcpTools(
            RepositoryService repositoryService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring RepositoryQueryMcpTools bean");
        return new RepositoryQueryMcpTools(repositoryService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.runtime.enabled",
            havingValue = "true", matchIfMissing = true)
    public RuntimeQueryMcpTools runtimeQueryMcpTools(
            RuntimeService runtimeService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring RuntimeQueryMcpTools bean");
        return new RuntimeQueryMcpTools(runtimeService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.task.enabled",
            havingValue = "true", matchIfMissing = true)
    public TaskQueryMcpTools taskQueryMcpTools(
            TaskService taskService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring TaskQueryMcpTools bean");
        return new TaskQueryMcpTools(taskService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.history.enabled",
            havingValue = "true", matchIfMissing = true)
    public HistoryQueryMcpTools historyQueryMcpTools(
            HistoryService historyService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring HistoryQueryMcpTools bean");
        return new HistoryQueryMcpTools(historyService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.external-task.enabled",
            havingValue = "true", matchIfMissing = true)
    public ExternalTaskQueryMcpTools externalTaskQueryMcpTools(
            ExternalTaskService externalTaskService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring ExternalTaskQueryMcpTools bean");
        return new ExternalTaskQueryMcpTools(externalTaskService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.authorization.enabled",
            havingValue = "true", matchIfMissing = true)
    public AuthorizationQueryMcpTools authorizationQueryMcpTools(
            AuthorizationService authorizationService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring AuthorizationQueryMcpTools bean");
        return new AuthorizationQueryMcpTools(authorizationService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.filter.enabled",
            havingValue = "true", matchIfMissing = true)
    public FilterQueryMcpTools filterQueryMcpTools(
            FilterService filterService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring FilterQueryMcpTools bean");
        return new FilterQueryMcpTools(filterService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.case-service.enabled",
            havingValue = "true", matchIfMissing = true)
    public CaseQueryMcpTools caseQueryMcpTools(
            CaseService caseService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring CaseQueryMcpTools bean");
        return new CaseQueryMcpTools(caseService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.identity.enabled",
            havingValue = "true", matchIfMissing = true)
    public IdentityQueryMcpTools identityQueryMcpTools(
            IdentityService identityService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring IdentityQueryMcpTools bean");
        return new IdentityQueryMcpTools(identityService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.management.enabled",
            havingValue = "true", matchIfMissing = true)
    public ManagementQueryMcpTools managementQueryMcpTools(
            ManagementService managementService,
            @Value("${fluxnova.mcp.query.max-results:200}") int defaultMaxResults) {
        log.debug("MCP - Auto-configuring ManagementQueryMcpTools bean");
        return new ManagementQueryMcpTools(managementService, defaultMaxResults);
    }

    @Bean
    @ConditionalOnProperty(name = "fluxnova.mcp.query.tools.xml.enabled",
            havingValue = "true", matchIfMissing = true)
    public XMLMcpTools xmlMcpTools(RepositoryService repositoryService) {
        log.debug("MCP - Auto-configuring XMLMcpTools bean");
        return new XMLMcpTools(repositoryService);
    }

    @Bean
    @ConditionalOnBean(ToolRegistry.class)
    public QueryToolRegistrar queryToolRegistrar(
            ToolRegistry toolRegistry,
            ObjectMapper objectMapper,
            QueryToolsProperties properties) {
        log.debug("MCP - Auto-configuring QueryToolRegistrar bean");
        return new QueryToolRegistrar(toolRegistry, objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpWorkflowQueryPlugin fluxnovaMcpWorkflowQueryPlugin() {
        log.info("MCP - Server - Auto-configuring FluxnovaMcpWorkflowQueryPlugin bean");
        return new McpWorkflowQueryPlugin();
    }
}
