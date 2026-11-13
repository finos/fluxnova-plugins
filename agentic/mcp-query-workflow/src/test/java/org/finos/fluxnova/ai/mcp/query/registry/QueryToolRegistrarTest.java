package org.finos.fluxnova.ai.mcp.query.registry;

import tools.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.autoconfigure.QueryToolsProperties;
import org.finos.fluxnova.ai.mcp.query.tools.*;
import org.finos.fluxnova.ai.mcp.server.registry.ToolConfig;
import org.finos.fluxnova.ai.mcp.server.registry.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class QueryToolRegistrarTest {

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private TaskQueryMcpTools taskQueryMcpTools;

    @Mock
    private RepositoryQueryMcpTools repositoryQueryMcpTools;

    @Mock
    private XMLMcpTools xmlMcpTools;

    @Mock
    private HistoryQueryMcpTools historyQueryMcpTools;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(toolRegistry.register(any(ToolConfig.class))).thenReturn(true);
    }

    @Test
    void shouldRegisterTaskTools() {
        QueryToolsProperties properties = new QueryToolsProperties();
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerTaskTools(taskQueryMcpTools);

        verify(toolRegistry).register(argThat(config -> "queryTasks".equals(config.name())));
    }

    @Test
    void shouldRegisterRepositoryTools() {
        QueryToolsProperties properties = new QueryToolsProperties();
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerRepositoryTools(repositoryQueryMcpTools);

        verify(toolRegistry, times(5)).register(any(ToolConfig.class));
        verify(toolRegistry).register(argThat(config -> "queryProcessDefinitions".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "queryDeployments".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "queryCaseDefinitions".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "queryDecisionDefinitions".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "queryDecisionRequirementsDefinitions".equals(config.name())));
    }

    @Test
    void shouldRegisterXmlTools() {
        QueryToolsProperties properties = new QueryToolsProperties();
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerXmlTools(xmlMcpTools);

        verify(toolRegistry, times(4)).register(any(ToolConfig.class));
        verify(toolRegistry).register(argThat(config -> "getProcessModelXml".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "getDecisionModelXml".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "getDecisionRequirementsModelXml".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "getCaseModelXml".equals(config.name())));
    }

    @Test
    void shouldRegisterHistoryTools() {
        QueryToolsProperties properties = new QueryToolsProperties();
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerHistoryTools(historyQueryMcpTools);

        verify(toolRegistry, times(14)).register(any(ToolConfig.class));
        verify(toolRegistry).register(argThat(config -> "queryHistoricProcessInstances".equals(config.name())));
        verify(toolRegistry).register(argThat(config -> "queryHistoricActivityInstances".equals(config.name())));
    }

    @Test
    void shouldExcludeToolsByName() {
        QueryToolsProperties properties = new QueryToolsProperties();
        properties.setExclude(Set.of("queryTasks"));
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerTaskTools(taskQueryMcpTools);

        verify(toolRegistry, never()).register(any(ToolConfig.class));
    }

    @Test
    void shouldExcludeSpecificToolWhileRegisteringOthers() {
        QueryToolsProperties properties = new QueryToolsProperties();
        properties.setExclude(Set.of("queryDeployments"));
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerRepositoryTools(repositoryQueryMcpTools);

        // 5 tools minus 1 excluded = 4
        verify(toolRegistry, times(4)).register(any(ToolConfig.class));
        verify(toolRegistry, never()).register(argThat(config -> "queryDeployments".equals(config.name())));
    }

    @Test
    void shouldRegisterToolsWithRawSchema() {
        QueryToolsProperties properties = new QueryToolsProperties();
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerTaskTools(taskQueryMcpTools);

        verify(toolRegistry).register(argThat(config ->
                config.rawSchema() != null && "object".equals(config.rawSchema().type())
        ));
    }

    @Test
    void shouldRegisterToolsWithDescription() {
        QueryToolsProperties properties = new QueryToolsProperties();
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, properties);

        registrar.registerTaskTools(taskQueryMcpTools);

        verify(toolRegistry).register(argThat(config ->
                config.description() != null && config.description().contains("Query user tasks")
        ));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void shouldTreatNonPositiveMaxResultsAsNotSpecified(int maxResults) throws Exception {
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, new QueryToolsProperties());
        registrar.registerTaskTools(taskQueryMcpTools);
        ArgumentCaptor<ToolConfig> captor = ArgumentCaptor.forClass(ToolConfig.class);
        verify(toolRegistry).register(captor.capture());

        captor.getValue().handler().execute(Map.of("maxResults", maxResults));

        verify(taskQueryMcpTools).queryTasks(null, null);
    }

    @Test
    void shouldPassPositiveMaxResultsThrough() throws Exception {
        QueryToolRegistrar registrar = new QueryToolRegistrar(toolRegistry, objectMapper, new QueryToolsProperties());
        registrar.registerTaskTools(taskQueryMcpTools);
        ArgumentCaptor<ToolConfig> captor = ArgumentCaptor.forClass(ToolConfig.class);
        verify(toolRegistry).register(captor.capture());

        captor.getValue().handler().execute(Map.of("maxResults", 5));

        verify(taskQueryMcpTools).queryTasks(null, 5);
    }
}
