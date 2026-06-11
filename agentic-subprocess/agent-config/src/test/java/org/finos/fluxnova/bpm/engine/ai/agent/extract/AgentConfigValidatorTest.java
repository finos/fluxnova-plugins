package org.finos.fluxnova.bpm.engine.ai.agent.extract;

import org.finos.fluxnova.bpm.engine.shared.agent.AgentModelConstants;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigValidatorTest {

    private Element parseConfigElement(String bpmn) {
        BpmnXmlParser parser = new BpmnXmlParser();
        Parse parse = parser.createParse()
                .sourceInputStream(new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)))
                .execute();
        return parse.getRootElement()
                .element("process")
                .element("adHocSubProcess")
                .element("extensionElements")
                .elementNS(AgentModelConstants.AGENT_NS, "config");
    }

    private String wrapInProcess(String innerXml) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    %s
                  </process>
                </definitions>
                """.formatted(innerXml);
    }

    @Test
    void validate_whenAllRequiredAttrsPresent_returnsNoErrors() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama"
                                  model="llama3.1"
                                  systemPrompt="You are an assistant."/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
    }

    @Test
    void validate_whenSystemPromptMissing_returnsNoErrors() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertTrue(errors.isEmpty(), "systemPrompt should be optional but got: " + errors);
    }

    @Test
    void validate_whenProviderMissing_returnsError() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config model="llama3.1" systemPrompt="x"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("provider"));
        assertTrue(errors.get(0).contains("myAgent"));
    }

    @Test
    void validate_whenProviderBlank_returnsError() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="" model="llama3.1" systemPrompt="x"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("provider"));
    }

    @Test
    void validate_whenModelMissing_returnsError() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" systemPrompt="x"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("model"));
        assertTrue(errors.get(0).contains("myAgent"));
    }

    @Test
    void validate_whenProviderAndModelBothMissing_returnsBothErrors() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config systemPrompt="x"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("provider")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("model")));
    }

    @Test
    void validate_whenToolScopeElementIdReferencesUnknownElement_returnsError() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1"
                                  toolScopeElementId="doesNotExist"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("doesNotExist"));
        assertTrue(errors.get(0).contains("myAgent"));
    }

    @Test
    void validate_whenToolScopeElementIdAbsent_doesNotValidateReference() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_whenToolScopeElementIdBlank_doesNotValidateReference() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1"
                                  toolScopeElementId=""/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent", Set.of("p", "myAgent"));

        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_whenToolScopeElementIdReferencesSibling_returnsNoErrors() {
        String bpmn = wrapInProcess("""
                <adHocSubProcess id="myAgent">
                  <extensionElements>
                    <agent:config provider="ollama" model="llama3.1"
                                  toolScopeElementId="toolRegistry"/>
                  </extensionElements>
                </adHocSubProcess>
                """);
        Element config = parseConfigElement(bpmn);

        List<String> errors = AgentConfigValidator.validate(config, "myAgent",
                Set.of("p", "myAgent", "toolRegistry"));

        assertTrue(errors.isEmpty());
    }
}
