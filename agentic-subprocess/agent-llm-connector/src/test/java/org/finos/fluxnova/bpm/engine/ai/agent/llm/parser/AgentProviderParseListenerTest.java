package org.finos.fluxnova.bpm.engine.ai.agent.llm.parser;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.ai.agent.llm.provider.AgentProviderRegistry;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentProviderParseListenerTest {

    private AgentProviderRegistry registry;
    private AgentProviderParseListener listener;

    @BeforeEach
    void setUp() {
        registry = mock(AgentProviderRegistry.class);
        listener = new AgentProviderParseListener(registry);
    }

    private Element rootElement(String bpmn) {
        Parse parse = new BpmnXmlParser().createParse()
                .sourceInputStream(new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)))
                .execute();
        return parse.getRootElement();
    }

    @Test
    void parseRootElement_whenProviderAvailable_succeeds() {
        when(registry.has("ollama")).thenReturn(true);

        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="myAgent">
                      <extensionElements>
                        <agent:config provider="ollama"
                                      model="llama3.1"
                                      systemPrompt="You are an assistant."/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        assertThatCode(() -> listener.parseRootElement(rootElement(bpmn), List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void parseRootElement_whenProviderUnavailable_throwsBpmnParseException() {
        when(registry.has("nonexistent")).thenReturn(false);

        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="badAgent">
                      <extensionElements>
                        <agent:config provider="nonexistent"
                                      model="llama3.1"
                                      systemPrompt="You are an assistant."/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        assertThatThrownBy(() -> listener.parseRootElement(rootElement(bpmn), List.of()))
                .isInstanceOf(BpmnParseException.class)
                .hasMessageContaining("badAgent")
                .hasMessageContaining("nonexistent");
    }

    @Test
    void parseRootElement_whenMultipleUnavailableProviders_exceptionListsAll() {
        when(registry.has("bad1")).thenReturn(false);
        when(registry.has("bad2")).thenReturn(false);

        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="agent1">
                      <extensionElements>
                        <agent:config provider="bad1"
                                      model="m1"
                                      systemPrompt="Prompt 1."/>
                      </extensionElements>
                    </adHocSubProcess>
                    <adHocSubProcess id="agent2">
                      <extensionElements>
                        <agent:config provider="bad2"
                                      model="m2"
                                      systemPrompt="Prompt 2."/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        assertThatThrownBy(() -> listener.parseRootElement(rootElement(bpmn), List.of()))
                .isInstanceOf(BpmnParseException.class)
                .hasMessageContaining("agent1")
                .hasMessageContaining("bad1")
                .hasMessageContaining("agent2")
                .hasMessageContaining("bad2");
    }

    @Test
    void parseRootElement_whenNoAgentConfig_doesNotThrow() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                  <process id="p">
                    <serviceTask id="task1"/>
                  </process>
                </definitions>
                """;

        assertThatCode(() -> listener.parseRootElement(rootElement(bpmn), List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void parseRootElement_whenProviderBlank_skipsValidation() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="myAgent">
                      <extensionElements>
                        <agent:config provider=""
                                      model="llama3.1"
                                      systemPrompt="You are an assistant."/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        assertThatCode(() -> listener.parseRootElement(rootElement(bpmn), List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void parseRootElement_whenProviderAbsent_skipsValidation() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                  <process id="p">
                    <adHocSubProcess id="myAgent">
                      <extensionElements>
                        <agent:config model="llama3.1"
                                      systemPrompt="You are an assistant."/>
                      </extensionElements>
                    </adHocSubProcess>
                  </process>
                </definitions>
                """;

        assertThatCode(() -> listener.parseRootElement(rootElement(bpmn), List.of()))
                .doesNotThrowAnyException();
    }
}
