package org.finos.fluxnova.bpm.engine.ai.agent.parser;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentConfigParseListenerTest {

    private final AgentConfigParseListener listener = new AgentConfigParseListener();

    private Element parseRoot(String bpmn) {
        BpmnXmlParser parser = new BpmnXmlParser();
        Parse parse = parser.createParse()
                .sourceInputStream(new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)))
                .execute();
        return parse.getRootElement();
    }

    @Test
    void parseRootElement_whenValidConfig_doesNotThrow() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="myAgent">
                      <extensionElements>
                        <agent:config provider="ollama" model="llama3.1"/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        assertDoesNotThrow(() -> listener.parseRootElement(parseRoot(bpmn), List.of()));
    }

    @Test
    void parseRootElement_whenNoAgentConfig_doesNotThrow() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                  <process id="p">
                    <serviceTask id="t1"/>
                  </process>
                </definitions>
                """;

        assertDoesNotThrow(() -> listener.parseRootElement(parseRoot(bpmn), List.of()));
    }

    @Test
    void parseRootElement_whenProviderMissing_throwsBpmnParseException() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="myAgent">
                      <extensionElements>
                        <agent:config model="llama3.1"/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        BpmnParseException ex = assertThrows(BpmnParseException.class,
                () -> listener.parseRootElement(parseRoot(bpmn), List.of()));
        assertTrue(ex.getMessage().contains("provider"));
        assertTrue(ex.getMessage().contains("myAgent"));
    }

    @Test
    void parseRootElement_whenMultipleErrors_reportsAllInFirstElement() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="agentA">
                      <extensionElements>
                        <agent:config model="llama3.1"/>
                      </extensionElements>
                    </adHocSubProcess>
                    <adHocSubProcess id="agentB">
                      <extensionElements>
                        <agent:config provider="ollama"/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        BpmnParseException ex = assertThrows(BpmnParseException.class,
                () -> listener.parseRootElement(parseRoot(bpmn), List.of()));
        assertTrue(ex.getMessage().contains("agentA"), "expected agentA in message: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("agentB"), "expected agentB in message: " + ex.getMessage());
    }

    @Test
    void parseRootElement_whenErrorsAcrossProcesses_reportsAllInFirstElement() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p1">
                    <adHocSubProcess id="agentP1">
                      <extensionElements>
                        <agent:config model="llama3.1"/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                  <process id="p2">
                    <adHocSubProcess id="agentP2">
                      <extensionElements>
                        <agent:config provider="ollama"/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        BpmnParseException ex = assertThrows(BpmnParseException.class,
                () -> listener.parseRootElement(parseRoot(bpmn), List.of()));
        assertTrue(ex.getMessage().contains("agentP1"));
        assertFalse(ex.getMessage().contains("agentP2"));
    }

}
