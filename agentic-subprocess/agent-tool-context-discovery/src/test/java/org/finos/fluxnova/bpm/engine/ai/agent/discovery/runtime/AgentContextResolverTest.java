package org.finos.fluxnova.bpm.engine.ai.agent.discovery.runtime;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ResolvedContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentContextResolverTest {

    private static final String EXECUTION_ID = "exec-123";
    private static final String PROC_DEF_ID = "proc:1";

    @Mock
    private RuntimeService runtimeService;

    private AgentContextResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AgentContextResolver();
    }

    private Map<String, Object> allVariables() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("customerId", "C-001");
        vars.put("applicationAmount", 50000);
        vars.put("creditScore", 720);
        return vars;
    }

    @Nested
    class EmptyDeclarations {

        @Test
        void resolve_whenDeclaredVariablesEmpty_returnsEmptyContext() {
            when(runtimeService.getVariables(EXECUTION_ID)).thenReturn(allVariables());
            AgentContextSpec spec = new AgentContextSpec(PROC_DEF_ID, "agent1", List.of());

            ResolvedContext result = resolver.resolve(runtimeService, EXECUTION_ID, spec);

            assertTrue(result.variables().isEmpty());
        }
    }

    @Nested
    class DeclaredScope {

        @Test
        void resolve_whenVariablesAreDeclared_returnsOnlyDeclaredVars() {
            when(runtimeService.getVariables(EXECUTION_ID)).thenReturn(allVariables());
            AgentContextSpec spec = new AgentContextSpec(PROC_DEF_ID, "agent1", List.of(
                    new ContextVariableDeclaration("customerId"),
                    new ContextVariableDeclaration("applicationAmount")
            ));

            ResolvedContext result = resolver.resolve(runtimeService, EXECUTION_ID, spec);

            assertEquals(2, result.variables().size());
            assertEquals("C-001", result.variables().get("customerId"));
            assertEquals(50000, result.variables().get("applicationAmount"));
        }

        @Test
        void resolve_declaredVarAbsentFromScope_isNotIncluded() {
            when(runtimeService.getVariables(EXECUTION_ID)).thenReturn(allVariables());
            AgentContextSpec spec = new AgentContextSpec(PROC_DEF_ID, "agent1", List.of(
                    new ContextVariableDeclaration("customerId"),
                    new ContextVariableDeclaration("nonExistentVar")
            ));

            ResolvedContext result = resolver.resolve(runtimeService, EXECUTION_ID, spec);

            assertEquals(1, result.variables().size());
            assertTrue(result.variables().containsKey("customerId"));
        }


        void resolve_whenDeclaredVarsHaveMixedTypes_preservesValueTypes() {
            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("count", 42);
            vars.put("active", true);
            vars.put("name", "test");
            vars.put("items", List.of("a", "b"));
            vars.put("data", Map.of("k", "v"));
            when(runtimeService.getVariables(EXECUTION_ID)).thenReturn(vars);
            AgentContextSpec spec = new AgentContextSpec(PROC_DEF_ID, "agent1", List.of(
                    new ContextVariableDeclaration("count"),
                    new ContextVariableDeclaration("active"),
                    new ContextVariableDeclaration("name"),
                    new ContextVariableDeclaration("items"),
                    new ContextVariableDeclaration("data")
            ));

            ResolvedContext result = resolver.resolve(runtimeService, EXECUTION_ID, spec);

            assertEquals(42, result.variables().get("count"));
            assertEquals(true, result.variables().get("active"));
            assertEquals("test", result.variables().get("name"));
            assertEquals(List.of("a", "b"), result.variables().get("items"));
            assertEquals(Map.of("k", "v"), result.variables().get("data"));
        }
    }
}