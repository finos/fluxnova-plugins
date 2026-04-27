package org.finos.fluxnova.ai.mcp.process.engine;

import org.finos.fluxnova.ai.mcp.process.model.ToolDefinition;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinitionQuery;
import org.finos.fluxnova.bpm.model.bpmn.Bpmn;
import org.finos.fluxnova.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class McpStartupScannerTest {

    private RepositoryService repositoryService;
    private ToolFactory factory;
    private McpStartupScanner scanner;

    @BeforeEach
    void setUp() {
        repositoryService = mock(RepositoryService.class);
        factory = mock(ToolFactory.class);
        scanner = new McpStartupScanner(repositoryService, factory);
    }

    @Test
    void shouldQueryLatestProcessDefinitions() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        scanner.scanAndRegisterExistingProcesses();

        verify(repositoryService).createProcessDefinitionQuery();
        verify(query).latestVersion();
        verify(query).list();
    }

    @Test
    void shouldRegisterToolsForTwoProcessDefinitions() {
        // Given - First process definition
        String bpmnXml1 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp"
                                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="process1" name="Process 1" isExecutable="true" camunda:historyTimeToLive="30">
                        <bpmn:startEvent id="start1" mcp:type="mcpToolStart" mcp:toolName="tool1" mcp:description="First tool"/>
                    </bpmn:process>
                </bpmn:definitions>
                """;

        // Given - Second process definition
        String bpmnXml2 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp"
                                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="process2" name="Process 2" isExecutable="true" camunda:historyTimeToLive="30">
                        <bpmn:startEvent id="start2" mcp:type="mcpToolStart" mcp:toolName="tool2" mcp:description="Second tool"/>
                    </bpmn:process>
                </bpmn:definitions>
                """;

        ProcessDefinition processDefinition1 = mock(ProcessDefinition.class);
        ProcessDefinition processDefinition2 = mock(ProcessDefinition.class);
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);

        when(repositoryService.createProcessDefinitionQuery())
                .thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion())
                .thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.list())
                .thenReturn(Arrays.asList(processDefinition1, processDefinition2));

        when(processDefinition1.getId()).thenReturn("process1:1:1");
        when(processDefinition2.getId()).thenReturn("process2:1:1");

        BpmnModelInstance model1 = createBpmnModel(bpmnXml1);
        BpmnModelInstance model2 = createBpmnModel(bpmnXml2);

        when(repositoryService.getBpmnModelInstance("process1:1:1"))
                .thenReturn(model1);
        when(repositoryService.getBpmnModelInstance("process2:1:1"))
                .thenReturn(model2);

        when(processDefinition1.getKey()).thenReturn("process1");
        when(processDefinition2.getKey()).thenReturn("process2");

        // Mock duplicate check - both tools are new
        when(factory.toolAlreadyRegistered("tool1")).thenReturn(false);
        when(factory.toolAlreadyRegistered("tool2")).thenReturn(false);

        // When
        scanner.scanAndRegisterExistingProcesses();

        // Then - Verify factory.createAndRegister called twice
        ArgumentCaptor<ToolDefinition> captor = ArgumentCaptor.forClass(ToolDefinition.class);
        verify(factory, times(2)).createAndRegister(captor.capture());

        // Verify the two unique tools
        List<ToolDefinition> capturedTools = captor.getAllValues();
        assertEquals(2, capturedTools.size());
        assertEquals("tool1", capturedTools.get(0).toolName());
        assertEquals("First tool", capturedTools.get(0).description());
        assertEquals("tool2", capturedTools.get(1).toolName());
        assertEquals("Second tool", capturedTools.get(1).description());
    }

    @Test
    void shouldSkipDuplicateToolNames() {
        // Given - Two processes with the same tool name
        String bpmnXml1 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp"
                                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="process1" name="Process 1" isExecutable="true" camunda:historyTimeToLive="30">
                        <bpmn:startEvent id="start1" mcp:type="mcpToolStart" mcp:toolName="duplicateTool" mcp:description="First instance"/>
                    </bpmn:process>
                </bpmn:definitions>
                """;

        String bpmnXml2 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp"
                                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="process2" name="Process 2" isExecutable="true" camunda:historyTimeToLive="30">
                        <bpmn:startEvent id="start2" mcp:type="mcpToolStart" mcp:toolName="duplicateTool" mcp:description="Second instance"/>
                    </bpmn:process>
                </bpmn:definitions>
                """;

        ProcessDefinition processDefinition1 = mock(ProcessDefinition.class);
        ProcessDefinition processDefinition2 = mock(ProcessDefinition.class);
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);

        when(repositoryService.createProcessDefinitionQuery())
                .thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion())
                .thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.list())
                .thenReturn(Arrays.asList(processDefinition1, processDefinition2));

        when(processDefinition1.getId()).thenReturn("process1:1:1");
        when(processDefinition2.getId()).thenReturn("process2:1:1");

        BpmnModelInstance model1 = createBpmnModel(bpmnXml1);
        BpmnModelInstance model2 = createBpmnModel(bpmnXml2);

        when(repositoryService.getBpmnModelInstance("process1:1:1"))
                .thenReturn(model1);
        when(repositoryService.getBpmnModelInstance("process2:1:1"))
                .thenReturn(model2);

        when(processDefinition1.getKey()).thenReturn("process1");
        when(processDefinition2.getKey()).thenReturn("process2");

        // Mock duplicate check - first is new, second is duplicate
        when(factory.toolAlreadyRegistered("duplicateTool"))
                .thenReturn(false)  // First call - not registered yet
                .thenReturn(true);  // Second call - already registered

        // When
        scanner.scanAndRegisterExistingProcesses();

        // Then - Verify factory.createAndRegister called only once
        verify(factory, times(1)).createAndRegister(any(ToolDefinition.class));

        // Verify duplicate check was called twice
        verify(factory, times(2)).toolAlreadyRegistered("duplicateTool");
    }

    @Test
    void shouldSkipStartEventsWithoutMcpToolStartType() {
        // Given - Process with start event that doesn't have mcp:type="mcpToolStart"
        String bpmnXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp"
                                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="process1" name="Process 1" isExecutable="true" camunda:historyTimeToLive="30">
                            <bpmn:startEvent id="StartEvent_1"/>
                    </bpmn:process>
                </bpmn:definitions>
                """;

        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);

        when(repositoryService.createProcessDefinitionQuery())
                .thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion())
                .thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.list())
                .thenReturn(List.of(processDefinition));

        when(processDefinition.getId()).thenReturn("process1:1:1");
        when(processDefinition.getKey()).thenReturn("process1");

        BpmnModelInstance model = createBpmnModel(bpmnXml);
        when(repositoryService.getBpmnModelInstance("process1:1:1"))
                .thenReturn(model);

        // When
        scanner.scanAndRegisterExistingProcesses();

        // Then - No tools should be registered
        verify(factory, never()).createAndRegister(any());
        verify(factory, never()).toolAlreadyRegistered(any());
    }

    @Test
    void shouldHandleEmptyProcessDefinitionList() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        scanner.scanAndRegisterExistingProcesses();

        verify(repositoryService).createProcessDefinitionQuery();
        verifyNoInteractions(factory);
    }

    @Test
    void shouldNotFailWhenProcessModelCannotBeLoaded() {
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getId()).thenReturn("failing-process");
        when(processDefinition.getKey()).thenReturn("failingProcess");

        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.list()).thenReturn(List.of(processDefinition));

        when(repositoryService.getBpmnModelInstance("failing-process"))
                .thenThrow(new RuntimeException("Failed to load BPMN"));

        // should not throw exception
        scanner.scanAndRegisterExistingProcesses();

        verify(repositoryService).getBpmnModelInstance("failing-process");
    }

    @Test
    void shouldProcessMultipleDefinitions() {
        ProcessDefinition def1 = mock(ProcessDefinition.class);
        when(def1.getId()).thenReturn("process-1");
        when(def1.getKey()).thenReturn("process1");

        ProcessDefinition def2 = mock(ProcessDefinition.class);
        when(def2.getId()).thenReturn("process-2");
        when(def2.getKey()).thenReturn("process2");

        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.list()).thenReturn(List.of(def1, def2));

        String bpmnXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:mcp="http://fluxnova.finos.org/schema/1.0/ai/mcp"
                                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                                  targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="test" isExecutable="true" camunda:historyTimeToLive="30">
                        <bpmn:startEvent id="start" mcp:type="mcpToolStart" mcp:toolName="testTool" mcp:description="Test"/>
                    </bpmn:process>
                </bpmn:definitions>
                """;

        BpmnModelInstance model = createBpmnModel(bpmnXml);
        when(repositoryService.getBpmnModelInstance("process-1")).thenReturn(model);
        when(repositoryService.getBpmnModelInstance("process-2")).thenReturn(model);

        // Mock duplicate check
        when(factory.toolAlreadyRegistered("testTool"))
                .thenReturn(false)
                .thenReturn(true);

        scanner.scanAndRegisterExistingProcesses();

        verify(repositoryService).getBpmnModelInstance("process-1");
        verify(repositoryService).getBpmnModelInstance("process-2");
        verify(factory, times(1)).createAndRegister(any(ToolDefinition.class));
    }

    private BpmnModelInstance createBpmnModel(String bpmnXml) {
        return Bpmn.readModelFromStream(
                new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8))
        );
    }
}
