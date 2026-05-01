package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.ContextVariableDeclaration;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BpmnExtensionContextSpecBuilderTest {

    private static final String PROC_DEF_ID = "proc:1";

    private Element parseAdHocSubProcess(String bpmn) {
        Parse parse = new BpmnXmlParser().createParse()
                .sourceInputStream(new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)))
                .execute();
        return parse.getRootElement()
                .element("process")
                .element("adHocSubProcess");
    }

    // ---------- Catalogue metadata ----------

    @Nested
    class CatalogueMetadata {

        @Test
        void build_setsProcessDefinitionIdAndElementId() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                      <process id="p">
                        <adHocSubProcess id="agent1"/>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            assertEquals(PROC_DEF_ID, spec.processDefinitionId());
            assertEquals("agent1", spec.elementId());
        }
    }

    // ---------- Declared variables ----------

    @Nested
    class DeclaredVariables {

        @Test
        void build_whenNoExtensionElements_returnsEmpty() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                      <process id="p">
                        <adHocSubProcess id="agent1"/>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            assertTrue(spec.declaredVariables().isEmpty());
        }

        @Test
        void build_whenExtensionElementsButNoAgentContext_returnsEmpty() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                 xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                      <process id="p">
                        <adHocSubProcess id="agent1">
                          <extensionElements>
                            <agent:config provider="anthropic" model="claude-sonnet-4-6"
                                          systemPrompt="You are an assistant."/>
                          </extensionElements>
                        </adHocSubProcess>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            assertTrue(spec.declaredVariables().isEmpty());
        }

        @Test
        void build_whenAgentContextPresent_returnsDeclaredVariables() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                 xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                      <process id="p">
                        <adHocSubProcess id="agent1">
                          <extensionElements>
                            <agent:context>
                              <agent:variable name="customerId"/>
                              <agent:variable name="applicationAmount"/>
                            </agent:context>
                          </extensionElements>
                        </adHocSubProcess>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            assertEquals(2, spec.declaredVariables().size());
            assertEquals("customerId", spec.declaredVariables().get(0).name());
            assertEquals("applicationAmount", spec.declaredVariables().get(1).name());
        }

        @Test
        void build_whenAgentContextEmpty_returnsEmpty() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                 xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                      <process id="p">
                        <adHocSubProcess id="agent1">
                          <extensionElements>
                            <agent:context/>
                          </extensionElements>
                        </adHocSubProcess>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            assertTrue(spec.declaredVariables().isEmpty());
        }

        @Test
        void build_skipsVariablesWithBlankName() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                 xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                      <process id="p">
                        <adHocSubProcess id="agent1">
                          <extensionElements>
                            <agent:context>
                              <agent:variable name="customerId"/>
                              <agent:variable name="   "/>
                              <agent:variable name="amount"/>
                            </agent:context>
                          </extensionElements>
                        </adHocSubProcess>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            List<String> names = spec.declaredVariables().stream()
                    .map(ContextVariableDeclaration::name).toList();
            assertEquals(List.of("customerId", "amount"), names);
        }

        @Test
        void build_skipsVariablesWithNoNameAttribute() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                 xmlns:agent="http://fluxnova.finos.org/schema/1.0/ai/agent">
                      <process id="p">
                        <adHocSubProcess id="agent1">
                          <extensionElements>
                            <agent:context>
                              <agent:variable name="customerId"/>
                              <agent:variable/>
                            </agent:context>
                          </extensionElements>
                        </adHocSubProcess>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            assertEquals(1, spec.declaredVariables().size());
            assertEquals("customerId", spec.declaredVariables().get(0).name());
        }

        @Test
        void build_wrongNamespaceForContext_returnsEmpty() {
            String bpmn = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                 xmlns:other="http://example.com/other">
                      <process id="p">
                        <adHocSubProcess id="agent1">
                          <extensionElements>
                            <other:context>
                              <other:variable name="customerId"/>
                            </other:context>
                          </extensionElements>
                        </adHocSubProcess>
                      </process>
                    </definitions>
                    """;

            AgentContextSpec spec = new BpmnExtensionContextSpecBuilder().build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

            assertTrue(spec.declaredVariables().isEmpty());
        }
    }
}
