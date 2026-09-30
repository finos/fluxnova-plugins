package org.finos.fluxnova.bpm.engine.ai.agent.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.model.AgentConfig;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigExtractorTest {

    private static final String PROCESS_DEFINITION_ID = "proc:1";

    private AgentConfigExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new AgentConfigExtractor();
    }

    private Element parseAdHocSubProcess(String bpmn) {
        BpmnXmlParser parser = new BpmnXmlParser();
        Parse parse = parser.createParse()
                .sourceInputStream(new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)))
                .execute();
        return parse.getRootElement()
                .element("process")
                .element("adHocSubProcess");
    }

    private String wrapInProcess(String innerXml) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="proc">
                    %s
                  </process>
                </definitions>
                """.formatted(innerXml);
    }

    // extract(Element, ...) valid config and defaulting

    @Test
    void extract_whenValidAgentConfig_returnsPopulatedAgentConfig() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="creditCheckAgent">
                  <extensionElements>
                    <agent:config provider="ollama"
                                  model="llama3.1"
                                  systemPrompt="You are a credit analyst."
                                  toolScopeElementId="creditCheckAgent"/>
                  </extensionElements>
                </adHocSubProcess>
                """);

        Optional<AgentConfig> result = extractor.extract(parseAdHocSubProcess(bpmn), PROCESS_DEFINITION_ID);

        assertTrue(result.isPresent());
        AgentConfig config = result.get();
        assertEquals(PROCESS_DEFINITION_ID, config.processDefinitionId());
        assertEquals("creditCheckAgent", config.elementId());
        assertEquals("ollama", config.provider());
        assertEquals("llama3.1", config.model());
        assertEquals("You are a credit analyst.", config.systemPrompt());
        assertEquals("creditCheckAgent", config.toolScopeElementId());
    }

    @Test
    void extract_whenToolScopeAbsent_defaultsToElementId() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama"
                                  model="llama3.1"
                                  systemPrompt="You are an assistant."/>
                  </extensionElements>
                </adHocSubProcess>
                """);

        Optional<AgentConfig> result = extractor.extract(parseAdHocSubProcess(bpmn), PROCESS_DEFINITION_ID);

        assertTrue(result.isPresent());
        assertEquals("myAgent", result.get().toolScopeElementId());
    }

    @Test
    void extract_whenToolScopeBlank_defaultsToElementId() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama"
                                  model="llama3.1"
                                  systemPrompt="You are an assistant."
                                  toolScopeElementId=""/>
                  </extensionElements>
                </adHocSubProcess>
                """);

        Optional<AgentConfig> result = extractor.extract(parseAdHocSubProcess(bpmn), PROCESS_DEFINITION_ID);

        assertTrue(result.isPresent());
        assertEquals("myAgent", result.get().toolScopeElementId());
    }

    // extract(Element, ...) missing and invalid config

    @Test
    void extract_whenNoExtensionElements_returnsEmpty() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="sub1"/>
                """);

        Optional<AgentConfig> result = extractor.extract(parseAdHocSubProcess(bpmn), PROCESS_DEFINITION_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void extract_whenExtensionElementsButNoAgentConfig_returnsEmpty() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:other="http://example.com/other">
                  <process id="p">
                    <adHocSubProcess id="sub1">
                      <extensionElements>
                        <other:config provider="x"/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        Optional<AgentConfig> result = extractor.extract(parseAdHocSubProcess(bpmn), PROCESS_DEFINITION_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void extract_whenSystemPromptAbsent_returnsConfigWithNullSystemPrompt() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama"
                                  model="llama3.1"/>
                  </extensionElements>
                </adHocSubProcess>
                """);

        Optional<AgentConfig> result = extractor.extract(parseAdHocSubProcess(bpmn), PROCESS_DEFINITION_ID);

        assertTrue(result.isPresent());
        assertNull(result.get().systemPrompt());
        assertEquals("ollama", result.get().provider());
        assertEquals("llama3.1", result.get().model());
    }

    // extract(Element, ...) userPrompt

    private AgentConfig extractWithConfigAttributes(String attributes) {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1" %s/>
                  </extensionElements>
                </adHocSubProcess>
                """.formatted(attributes));
        return extractor.extract(parseAdHocSubProcess(bpmn), PROCESS_DEFINITION_ID).orElseThrow();
    }

    @Test
    void extract_whenUserPromptPresent_returnsUserPrompt() {
        AgentConfig config = extractWithConfigAttributes(
                "systemPrompt=\"You are a credit analyst.\" userPrompt=\"Assess the loan application.\"");

        assertEquals("Assess the loan application.", config.userPrompt());
        assertEquals("You are a credit analyst.", config.systemPrompt());
    }

    @Test
    void extract_whenUserPromptAbsent_returnsNullUserPrompt() {
        AgentConfig config = extractWithConfigAttributes("systemPrompt=\"You are an assistant.\"");

        assertNull(config.userPrompt());
    }

    @Test
    void extract_whenUserPromptEmpty_returnsEmptyUserPrompt() {
        // Blank values are passed through raw; the orchestrator applies the default fallback.
        AgentConfig config = extractWithConfigAttributes("userPrompt=\"\"");

        assertEquals("", config.userPrompt());
    }

    @Test
    void extract_whenUserPromptContainsExpression_keepsRawExpressionText() {
        AgentConfig config = extractWithConfigAttributes(
                "userPrompt=\"Investigate alert ${alertId} for ${customerName}\"");

        assertEquals("Investigate alert ${alertId} for ${customerName}", config.userPrompt());
    }

    @Test
    void extract_whenUserPromptHasEncodedLineBreaks_preservesLineBreaks() {
        AgentConfig config = extractWithConfigAttributes(
                "userPrompt=\"Line one&#10;Line two&#10;&#10;Line four\"");

        assertEquals("Line one\nLine two\n\nLine four", config.userPrompt());
    }

    @Test
    void extract_whenUserPromptHasLiteralLineBreaks_normalisesThemPerXmlSpec() {
        // XML attribute-value normalisation turns literal line breaks into spaces.
        // Modelers must encode line breaks as &#10; to preserve them (see test above).
        AgentConfig config = extractWithConfigAttributes("userPrompt=\"Line one\nLine two\"");

        assertEquals("Line one Line two", config.userPrompt());
    }

    @Test
    void extract_whenUserPromptHasXmlEscapedSpecialCharacters_unescapesThem() {
        AgentConfig config = extractWithConfigAttributes(
                "userPrompt=\"Check &lt;amount&gt; &amp; &quot;status&quot; for O&apos;Brien — 100% ✓\"");

        assertEquals("Check <amount> & \"status\" for O'Brien — 100% ✓", config.userPrompt());
    }

    @Test
    void extract_whenUserPromptUsesSingleQuotedAttribute_keepsDoubleQuotes() {
        AgentConfig config = extractWithConfigAttributes(
                "userPrompt='Say \"hello\" to ${name}'");

        assertEquals("Say \"hello\" to ${name}", config.userPrompt());
    }

    @Test
    void extractAll_whenUserPromptPresent_populatesUserPrompt() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1" userPrompt="Start the review."/>
                  </extensionElements>
                </adHocSubProcess>
                """);

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(1, results.size());
        assertEquals("Start the review.", results.get(0).userPrompt());
    }

    // extractAll(...) tool-scope discovery

    @Test
    void extractAll_whenToolScopeReferencesSiblingElement_succeeds() {
        String bpmn = wrapInProcess("""
                <subProcess id="toolRegistry"/>
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama"
                                  model="llama3.1"
                                  systemPrompt="You are an assistant."
                                  toolScopeElementId="toolRegistry"/>
                  </extensionElements>
                </adHocSubProcess>
                """);

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(1, results.size());
        assertEquals("toolRegistry", results.get(0).toolScopeElementId());
    }

    // extractAll(...) discovery across BPMN structures

    @Test
    void extractAll_findsAgentConfigOnServiceTask() {
        String bpmn = wrapInProcess("""
                <serviceTask id="taskAgent">
                  <extensionElements>
                    <agent:config provider="ollama"
                                  model="llama3.1"
                                  systemPrompt="Task agent."/>
                  </extensionElements>
                </serviceTask>
                """);

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(1, results.size());
        assertEquals("taskAgent", results.get(0).elementId());
        assertEquals("taskAgent", results.get(0).toolScopeElementId());
    }

    @Test
    void extractAll_findsAdHocSubProcessInsideSubProcess() {
        String bpmn = wrapInProcess("""
                <subProcess id="outer">
                  <adHocSubProcess id="nestedAgent">
                    <extensionElements>
                      <agent:config provider="ollama"
                                    model="llama3.1"
                                    systemPrompt="Nested agent."/>
                    </extensionElements>
                  </adHocSubProcess>
                </subProcess>
                """);

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(1, results.size());
        assertEquals("nestedAgent", results.get(0).elementId());
    }

    @Test
    void extractAll_findsAdHocSubProcessInsideEventSubProcess() {
        String bpmn = wrapInProcess("""
                <subProcess id="eventSub" triggeredByEvent="true">
                  <adHocSubProcess id="eventAgent">
                    <extensionElements>
                      <agent:config provider="ollama"
                                    model="llama3.1"
                                    systemPrompt="Event agent."/>
                    </extensionElements>
                  </adHocSubProcess>
                </subProcess>
                """);

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(1, results.size());
        assertEquals("eventAgent", results.get(0).elementId());
    }

    @Test
    void extractAll_findsAdHocSubProcessInsideTransaction() {
        String bpmn = wrapInProcess("""
                <transaction id="tx1">
                  <adHocSubProcess id="transactionAgent">
                    <extensionElements>
                      <agent:config provider="ollama"
                                    model="llama3.1"
                                    systemPrompt="Transaction agent."/>
                    </extensionElements>
                  </adHocSubProcess>
                </transaction>
                """);

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(1, results.size());
        assertEquals("transactionAgent", results.get(0).elementId());
    }

    @Test
    void extractAll_findsMultipleAdHocSubProcessesInOneProcess() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="agentA">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1" systemPrompt="Agent A."/>
                  </extensionElements>
                </adHocSubProcess>
                <adHocSubProcess id="agentB">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1" systemPrompt="Agent B."/>
                  </extensionElements>
                </adHocSubProcess>
                """);

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(2, results.size());
        assertEquals("agentA", results.get(0).elementId());
        assertEquals("agentB", results.get(1).elementId());
    }

    @Test
    void extractAll_onlyWalksProcessMatchingProcessDefinitionId() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="proc">
                    <adHocSubProcess id="agentP1">
                      <extensionElements>
                        <agent:config provider="ollama" model="llama3.1" systemPrompt="P1 agent."/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                  <process id="other">
                    <adHocSubProcess id="agentOther">
                      <extensionElements>
                        <agent:config provider="ollama" model="llama3.1" systemPrompt="Other agent."/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        List<AgentConfig> results = extractor.extractAll(
                new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)), PROCESS_DEFINITION_ID);

        assertEquals(1, results.size());
        assertEquals("agentP1", results.get(0).elementId());
    }
}
