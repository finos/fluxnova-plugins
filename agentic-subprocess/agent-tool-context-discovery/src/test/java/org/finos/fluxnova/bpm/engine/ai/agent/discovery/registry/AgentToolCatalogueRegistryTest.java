package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract.AgentToolCatalogueBuilder;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolEntry;
import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.ai.agent.registry.AgentConfigRegistry;
import org.finos.fluxnova.bpm.engine.exception.NotFoundException;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentToolCatalogueRegistryTest {

    private static final String PROC_DEF_ID = "creditCheck:1:abc";
    private static final String AGENT_ELEMENT_ID = "creditCheckAgent";
    private static final String TOOL_SCOPE_ID = "toolScope1";

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private AgentConfigRegistry agentConfigRegistry;

    @Mock
    private AgentToolCatalogueBuilder catalogueBuilder;

    private AgentToolCatalogueRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentToolCatalogueRegistry(agentConfigRegistry, catalogueBuilder);
    }

    private AgentConfig config() {
        return config(TOOL_SCOPE_ID);
    }

    private AgentConfig config(String toolScopeElementId) {
        return new AgentConfig(PROC_DEF_ID, AGENT_ELEMENT_ID, "anthropic", "claude-sonnet-4-6",
                "You are a credit analyst.", toolScopeElementId);
    }

    private ProcessDefinitionEntity procDefWithScope(String scopeId) {
        ProcessDefinitionEntity procDef = new ProcessDefinitionEntity();
        procDef.setId(PROC_DEF_ID);
        procDef.createActivity(scopeId);
        return procDef;
    }

    private AgentToolCatalogue catalogue() {
        return new AgentToolCatalogue(PROC_DEF_ID, TOOL_SCOPE_ID, List.of(
                new AgentToolEntry("task1", "Task 1", "desc", Set.of(), Set.of())));
    }

    @Nested
    class ConfigResolution {

        @Test
        void resolve_whenNoAgentConfig_returnsEmpty() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.empty());

            Optional<AgentToolCatalogue> result = registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(repositoryService);
        }
    }

    @Nested
    class ProcessDefinitionLookup {

        @Test
        void resolve_whenAgentConfigExists_returnsBuilderResult() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinitionEntity procDef = procDefWithScope(TOOL_SCOPE_ID);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);
            AgentToolCatalogue expected = catalogue();
            when(catalogueBuilder.build(any(ActivityImpl.class))).thenReturn(expected);

            Optional<AgentToolCatalogue> result = registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);

            assertTrue(result.isPresent());
            assertEquals(expected, result.get());
        }
    }

    @Nested
    class MissingProcessDefinition {

        @Test
        void resolve_whenNotFoundException_returnsEmpty() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessDefinition(PROC_DEF_ID))
                    .thenThrow(new NotFoundException("not found"));

            assertTrue(registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID).isEmpty());
        }

        @Test
        void resolve_whenIncorrectType_returnsEmpty() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinition processDefinition = mock(ProcessDefinition.class);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID))
                    .thenReturn(processDefinition);

            assertTrue(registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID).isEmpty());
        }
    }

    @Nested
    class UnauthorizedAccess {

        @Test
        void resolve_whenAuthorizationException_returnsEmpty() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            when(repositoryService.getProcessDefinition(PROC_DEF_ID))
                    .thenThrow(new AuthorizationException("denied"));

            assertTrue(registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID).isEmpty());
        }
    }

    @Nested
    class MissingScopeActivity {

        @Test
        void resolve_whenScopeActivityNotFound_returnsEmpty() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinitionEntity procDef = new ProcessDefinitionEntity(); // no activities
            procDef.setId(PROC_DEF_ID);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);

            assertTrue(registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID).isEmpty());
            verifyNoInteractions(catalogueBuilder);
        }
    }

    @Nested
    class BuilderInvocation {

        @Test
        void resolve_passesCorrectActivityImplToBuilder() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinitionEntity procDef = procDefWithScope(TOOL_SCOPE_ID);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);
            when(catalogueBuilder.build(any(ActivityImpl.class))).thenReturn(catalogue());

            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);

            verify(catalogueBuilder).build(argThat(activity ->
                    TOOL_SCOPE_ID.equals(activity.getId())));
        }
    }

    @Nested
    class CachingBehavior {

        @Test
        void resolve_calledTwice_scansOnlyOnce() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinitionEntity procDef = procDefWithScope(TOOL_SCOPE_ID);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);
            when(catalogueBuilder.build(any(ActivityImpl.class))).thenReturn(catalogue());

            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);
            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);

            verify(catalogueBuilder, times(1)).build(any());
        }

        @Test
        void resolve_unregisterAll_clearsCache() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinitionEntity procDef = procDefWithScope(TOOL_SCOPE_ID);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);
            when(catalogueBuilder.build(any(ActivityImpl.class))).thenReturn(catalogue());

            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);
            registry.unregisterAll();

            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.empty());
            assertTrue(registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID).isEmpty());
        }

        @Test
        void resolve_afterUnregisterAll_rescans() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinitionEntity procDef = procDefWithScope(TOOL_SCOPE_ID);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);
            when(catalogueBuilder.build(any(ActivityImpl.class))).thenReturn(catalogue());

            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);
            registry.unregisterAll();

            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);

            verify(catalogueBuilder, times(2)).build(any());
        }

        @Test
        void resolve_whenNoAgentConfig_isCached_doesNotRescanOnSecondCall() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.empty());

            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);
            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);

            verify(agentConfigRegistry, times(1)).resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);
        }

        @Test
        void resolve_whenScopeActivityNotFound_isCached_doesNotRescanOnSecondCall() {
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID)).thenReturn(Optional.of(config()));
            ProcessDefinitionEntity procDef = new ProcessDefinitionEntity(); // no activities
            procDef.setId(PROC_DEF_ID);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);

            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);
            registry.resolve(repositoryService, PROC_DEF_ID, AGENT_ELEMENT_ID);

            verify(repositoryService, times(1)).getProcessDefinition(PROC_DEF_ID);
        }
    }

    @Nested
    class CacheIdentityBasedOnToolScopeId {

        @Test
        void resolve_multipleAgentsWithSameToolScope_buildsCatalogueOnlyOnce() {
            String agentA = "agentA";
            String agentB = "agentB";
            String sharedScope = "sharedScope";

            AgentConfig configA = new AgentConfig(PROC_DEF_ID, agentA, "anthropic", "claude-sonnet-4-6",
                    "prompt A", sharedScope);
            AgentConfig configB = new AgentConfig(PROC_DEF_ID, agentB, "anthropic", "claude-sonnet-4-6",
                    "prompt B", sharedScope);

            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, agentA)).thenReturn(Optional.of(configA));
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, agentB)).thenReturn(Optional.of(configB));
            ProcessDefinitionEntity procDef = procDefWithScope(sharedScope);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);
            AgentToolCatalogue expected = new AgentToolCatalogue(PROC_DEF_ID, sharedScope, List.of());
            when(catalogueBuilder.build(any(ActivityImpl.class))).thenReturn(expected);

            Optional<AgentToolCatalogue> resultA = registry.resolve(repositoryService, PROC_DEF_ID, agentA);
            Optional<AgentToolCatalogue> resultB = registry.resolve(repositoryService, PROC_DEF_ID, agentB);

            assertTrue(resultA.isPresent());
            assertTrue(resultB.isPresent());
            assertSame(resultA.get(), resultB.get());
            verify(catalogueBuilder, times(1)).build(any());
        }

        @Test
        void resolve_differentToolScopes_buildsSeparateCatalogues() {
            String agentA = "agentA";
            String agentB = "agentB";
            String scopeA = "scopeA";
            String scopeB = "scopeB";

            AgentConfig configA = new AgentConfig(PROC_DEF_ID, agentA, "anthropic", "claude-sonnet-4-6",
                    "prompt A", scopeA);
            AgentConfig configB = new AgentConfig(PROC_DEF_ID, agentB, "anthropic", "claude-sonnet-4-6",
                    "prompt B", scopeB);

            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, agentA)).thenReturn(Optional.of(configA));
            when(agentConfigRegistry.resolve(repositoryService, PROC_DEF_ID, agentB)).thenReturn(Optional.of(configB));
            ProcessDefinitionEntity procDef = new ProcessDefinitionEntity();
            procDef.setId(PROC_DEF_ID);
            procDef.createActivity(scopeA);
            procDef.createActivity(scopeB);
            when(repositoryService.getProcessDefinition(PROC_DEF_ID)).thenReturn(procDef);

            AgentToolCatalogue catA = new AgentToolCatalogue(PROC_DEF_ID, scopeA, List.of());
            AgentToolCatalogue catB = new AgentToolCatalogue(PROC_DEF_ID, scopeB, List.of());
            when(catalogueBuilder.build(argThat(a -> a != null && scopeA.equals(a.getId())))).thenReturn(catA);
            when(catalogueBuilder.build(argThat(a -> a != null && scopeB.equals(a.getId())))).thenReturn(catB);

            Optional<AgentToolCatalogue> resultA = registry.resolve(repositoryService, PROC_DEF_ID, agentA);
            Optional<AgentToolCatalogue> resultB = registry.resolve(repositoryService, PROC_DEF_ID, agentB);

            assertTrue(resultA.isPresent());
            assertTrue(resultB.isPresent());
            assertEquals(scopeA, resultA.get().elementId());
            assertEquals(scopeB, resultB.get().elementId());
            verify(catalogueBuilder, times(2)).build(any());
        }
    }
}
