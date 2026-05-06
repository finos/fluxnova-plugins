package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.shared.xml.BpmnXmlParser;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AdHocSubProcessCatalogueBuilderTest {

  private static final String PROC_DEF_ID = "proc:1";

  private AdHocSubProcessCatalogueBuilder builder;

  @BeforeEach
  void setUp() {
      builder = new AdHocSubProcessCatalogueBuilder();
  }

  private Element parseAdHocSubProcess(String bpmn) {
      Parse parse = new BpmnXmlParser().createParse()
              .sourceInputStream(new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8)))
              .execute();
      return parse.getRootElement()
              .element("process")
              .element("adHocSubProcess");
  }

  @Nested
  class ToolEligibility {

      @Test
      void build_includesActivityWithNoIncomingSequenceFlows() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(1, catalogue.tools().size());
          assertEquals("taskA", catalogue.tools().get(0).elementId());
      }

      @Test
      void build_excludesActivityThatIsSequenceFlowTarget() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A"/>
                        <serviceTask id="taskB" name="Task B"/>
                        <sequenceFlow id="flow1" sourceRef="taskA" targetRef="taskB"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(1, catalogue.tools().size());
          assertEquals("taskA", catalogue.tools().get(0).elementId());
      }

      @Test
      void build_excludesNonActivityElements() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A"/>
                        <startEvent id="start1"/>
                        <endEvent id="end1"/>
                        <exclusiveGateway id="gw1"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(1, catalogue.tools().size());
          assertEquals("taskA", catalogue.tools().get(0).elementId());
      }

      @Test
      void build_whenNoActivities_returnsEmptyCatalogue() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <startEvent id="start1"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);
          assertTrue(catalogue.tools().isEmpty());
      }

      @Test
      void build_recognisesAllActivityTypes() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <task id="t1" name="Generic Task"/>
                        <serviceTask id="t2" name="Service"/>
                        <sendTask id="t3" name="Send"/>
                        <receiveTask id="t4" name="Receive"/>
                        <userTask id="t5" name="User"/>
                        <manualTask id="t6" name="Manual"/>
                        <businessRuleTask id="t7" name="Rule"/>
                        <scriptTask id="t8" name="Script"/>
                        <subProcess id="t9" name="Sub"/>
                        <callActivity id="t10" name="Call"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(10, catalogue.tools().size());
      }

      @Test
      void build_multipleSequenceFlows_excludesAllTargets() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="A"/>
                        <serviceTask id="taskB" name="B"/>
                        <serviceTask id="taskC" name="C"/>
                        <sequenceFlow id="f1" sourceRef="taskA" targetRef="taskB"/>
                        <sequenceFlow id="f2" sourceRef="taskB" targetRef="taskC"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(1, catalogue.tools().size());
          assertEquals("taskA", catalogue.tools().get(0).elementId());
      }

      @Test
      void build_sequenceFlowWithMissingTargetRef_doesNotExclude() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="A"/>
                        <serviceTask id="taskB" name="B"/>
                        <sequenceFlow id="f1" sourceRef="taskA"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(2, catalogue.tools().size());
      }
  }

  @Nested
  class ToolMetadata {

      @Test
      void build_extractsNameAttribute() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Credit Score Check"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals("Credit Score Check", catalogue.tools().get(0).name());
      }

      @Test
      void build_whenNameAbsent_returnsNull() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertNull(catalogue.tools().get(0).name());
      }

      @Test
      void build_extractsDocumentation() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <documentation>Retrieves the credit score.</documentation>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals("Retrieves the credit score.", catalogue.tools().get(0).description());
      }

      @Test
      void build_whenNoDocumentation_descriptionIsNull() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertNull(catalogue.tools().get(0).description());
      }

      @Test
      void build_whenDocumentationIsBlank_descriptionIsNull() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <documentation>   </documentation>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertNull(catalogue.tools().get(0).description());
      }

      @Test
      void build_documentationIsStripped() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <documentation>  Some description  </documentation>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals("Some description", catalogue.tools().get(0).description());
      }
  }

  @Nested
  class ReadsExtraction {

      @Test
      void build_simpleElExpression_extractsRead() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="cid">${customerId}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("customerId"), catalogue.tools().get(0).reads());
      }

      @Test
      void build_dottedElExpression_extractsRootIdentifier() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="email">${customer.profile.email}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("customer"), catalogue.tools().get(0).reads());
      }

      @Test
      void build_complexExpression_skipped() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="full">${firstName + ' ' + lastName}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).reads().isEmpty());
      }

      @Test
      void build_methodCallExpression_skipped() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="user">${userService.findById(id)}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).reads().isEmpty());
      }

      @Test
      void build_multipleInputParameters_collectsAll() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="a">${customerId}</camunda:inputParameter>
                              <camunda:inputParameter name="b">${applicationId}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("customerId", "applicationId"), catalogue.tools().get(0).reads());
      }

      @Test
      void build_listInputParameter_extractsFromEachValue() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="emails">
                                <camunda:list>
                                  <camunda:value>${primaryEmail}</camunda:value>
                                  <camunda:value>${fallbackEmail}</camunda:value>
                                  <camunda:value>noreply@x.com</camunda:value>
                                </camunda:list>
                              </camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("primaryEmail", "fallbackEmail"), catalogue.tools().get(0).reads());
      }

      @Test
      void build_mapInputParameter_extractsFromEntryValues() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="data">
                                <camunda:map>
                                  <camunda:entry key="id">${customerId}</camunda:entry>
                                  <camunda:entry key="name">${customerName}</camunda:entry>
                                  <camunda:entry key="source">manual</camunda:entry>
                                </camunda:map>
                              </camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("customerId", "customerName"), catalogue.tools().get(0).reads());
      }

      @Test
      void build_scriptInputParameter_skipped() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="val">
                                <camunda:script scriptFormat="groovy">someVar + 1</camunda:script>
                              </camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).reads().isEmpty());
      }

      @Test
      void build_noExtensionElements_readsEmpty() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).reads().isEmpty());
      }

      @Test
      void build_noInputOutput_readsEmpty() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:properties>
                              <camunda:property name="x" value="y"/>
                            </camunda:properties>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).reads().isEmpty());
      }

      @Test
      void build_duplicateReadsAreDeduped() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="a">${customerId}</camunda:inputParameter>
                              <camunda:inputParameter name="b">${customerId}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("customerId"), catalogue.tools().get(0).reads());
          assertEquals(1, catalogue.tools().get(0).reads().size());
      }

      @Test
      void build_expressionWithWhitespace_extractsCorrectly() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="a">  ${ customerId }  </camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("customerId"), catalogue.tools().get(0).reads());
      }

      @Test
      void build_literalStringInputParam_notAddedToReads() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="type">manual</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).reads().isEmpty());
      }

      @Test
      void build_mixedSimpleAndComplexParams_onlyExtractsSimple() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="a">${customerId}</camunda:inputParameter>
                              <camunda:inputParameter name="b">${firstName + ' ' + lastName}</camunda:inputParameter>
                              <camunda:inputParameter name="c">${applicationId}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("customerId", "applicationId"), catalogue.tools().get(0).reads());
      }

      @Test
      void build_nonFluxnovaCompositeChildren_fallsThroughToText() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="p1">
                                <script>not-camunda-ns</script>
                              </camunda:inputParameter>
                              <camunda:inputParameter name="p2">
                                <list>not-camunda-ns</list>
                              </camunda:inputParameter>
                              <camunda:inputParameter name="p3">
                                <map>not-camunda-ns</map>
                              </camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          // Non-camunda children fall through to text extraction; none are EL expressions
          assertTrue(catalogue.tools().get(0).reads().isEmpty());
      }
  }

  @Nested
  class WritesExtraction {

      @Test
      void build_extractsOutputParameterNames() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:outputParameter name="creditScore">${creditScore}</camunda:outputParameter>
                              <camunda:outputParameter name="riskLevel">${riskLevel}</camunda:outputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("creditScore", "riskLevel"), catalogue.tools().get(0).writes());
      }

      @Test
      void build_noOutputParameters_writesEmpty() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:inputParameter name="a">${customerId}</camunda:inputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).writes().isEmpty());
      }

      @Test
      void build_noExtensionElements_writesEmpty() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertTrue(catalogue.tools().get(0).writes().isEmpty());
      }

      @Test
      void build_outputParameterWithoutName_isNotAddedToWrites() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                xmlns:camunda="http://camunda.org/schema/1.0/bpmn">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="Task A">
                          <extensionElements>
                            <camunda:inputOutput>
                              <camunda:outputParameter>${someValue}</camunda:outputParameter>
                              <camunda:outputParameter name="valid">${other}</camunda:outputParameter>
                            </camunda:inputOutput>
                          </extensionElements>
                        </serviceTask>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(Set.of("valid"), catalogue.tools().get(0).writes());
      }
  }

  @Nested
  class CatalogueMetadata {

      @Test
      void build_setsProcessDefinitionIdAndElementId() {
          String bpmn = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                    <process id="p">
                      <adHocSubProcess id="agent1">
                        <serviceTask id="taskA" name="A"/>
                      </adHocSubProcess>
                    </process>
                  </definitions>
                  """;

          AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

          assertEquals(PROC_DEF_ID, catalogue.processDefinitionId());
          assertEquals("agent1", catalogue.elementId());
      }
  }

  @Nested
  class ScopeReadFor {

      @Test
      void simpleVariable() {
          assertEquals(Optional.of("customerId"),
                  AdHocSubProcessCatalogueBuilder.scopeReadFor("${customerId}"));
      }

      @Test
      void dottedPath() {
          assertEquals(Optional.of("customer"),
                  AdHocSubProcessCatalogueBuilder.scopeReadFor("${customer.profile.email}"));
      }

      @Test
      void withWhitespace() {
          assertEquals(Optional.of("x"),
                  AdHocSubProcessCatalogueBuilder.scopeReadFor("  ${ x }  "));
      }

      @Test
      void concatenation_empty() {
          assertTrue(AdHocSubProcessCatalogueBuilder.scopeReadFor("${a + b}").isEmpty());
      }

      @Test
      void methodCall_empty() {
          assertTrue(AdHocSubProcessCatalogueBuilder.scopeReadFor("${svc.find(id)}").isEmpty());
      }

      @Test
      void literal_empty() {
          assertTrue(AdHocSubProcessCatalogueBuilder.scopeReadFor("hello").isEmpty());
      }

      @Test
      void null_empty() {
          assertTrue(AdHocSubProcessCatalogueBuilder.scopeReadFor(null).isEmpty());
      }

      @Test
      void emptyString_empty() {
          assertTrue(AdHocSubProcessCatalogueBuilder.scopeReadFor("").isEmpty());
      }

      @Test
      void multipleElSegments_empty() {
          assertTrue(AdHocSubProcessCatalogueBuilder.scopeReadFor("${a}${b}").isEmpty());
      }

      @Test
      void underscoreInIdentifier() {
          assertEquals(Optional.of("_myVar"),
                  AdHocSubProcessCatalogueBuilder.scopeReadFor("${_myVar}"));
      }
  }

  @Nested
  class InvalidAndEdgeCases {

    @Test
    void build_completelyEmptyScope_returnsEmptyCatalogue() {
      String bpmn = """
          <?xml version="1.0" encoding="UTF-8"?>
          <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
            <process id="p">
              <adHocSubProcess id="agent1"></adHocSubProcess>
            </process>
          </definitions>
          """;

      AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);
      assertTrue(catalogue.tools().isEmpty());
      assertEquals("agent1", catalogue.elementId());
    }

    @Test
    void build_taskWithNoId_throwsProcessEngineException() {
      String bpmn = """
          <?xml version="1.0" encoding="UTF-8"?>
          <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
            <process id="p">
              <adHocSubProcess id="agent1">
                <serviceTask name="Misconfigured Task"/>
                <serviceTask id="validTask" name="Valid Task"/>
              </adHocSubProcess>
            </process>
          </definitions>
          """;

      Element scope = parseAdHocSubProcess(bpmn);
      ProcessEngineException ex =
          assertThrows(ProcessEngineException.class, () -> builder.build(scope, PROC_DEF_ID));
      assertTrue(ex.getMessage().contains("missing id"));
    }

    @Test
    void build_activityWithBlankId_throwsProcessEngineException() {
      String bpmn = """
          <?xml version="1.0" encoding="UTF-8"?>
          <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
            <process id="p">
              <adHocSubProcess id="agent1">
                <serviceTask id="" name="Blank ID Task"/>
                <serviceTask id="validTask" name="Valid Task"/>
              </adHocSubProcess>
            </process>
          </definitions>
          """;

      Element scope = parseAdHocSubProcess(bpmn);
      ProcessEngineException ex =
          assertThrows(ProcessEngineException.class, () -> builder.build(scope, PROC_DEF_ID));
      assertTrue(ex.getMessage().contains("missing id"));
    }

    @Test
    void build_allToolsFilteredBySequenceFlows_returnsEmptyCatalogue() {
      String bpmn = """
          <?xml version="1.0" encoding="UTF-8"?>
          <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
            <process id="p">
              <adHocSubProcess id="agent1">
                <serviceTask id="taskA" name="A"/>
                <serviceTask id="taskB" name="B"/>
                <sequenceFlow id="f1" sourceRef="taskA" targetRef="taskB"/>
                <sequenceFlow id="f2" sourceRef="taskB" targetRef="taskA"/>
              </adHocSubProcess>
            </process>
          </definitions>
          """;

      AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);
      assertTrue(catalogue.tools().isEmpty());
    }

    @Test
    void build_serviceTaskWithNoImplementation_stillDiscovered() {
      String bpmn = """
          <?xml version="1.0" encoding="UTF-8"?>
          <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
            <process id="p">
              <adHocSubProcess id="agent1">
                <serviceTask id="bareService" name="Bare Service"/>
              </adHocSubProcess>
            </process>
          </definitions>
          """;

      AgentToolCatalogue catalogue = builder.build(parseAdHocSubProcess(bpmn), PROC_DEF_ID);

      assertEquals(1, catalogue.tools().size());
      assertEquals("bareService", catalogue.tools().get(0).elementId());
      assertEquals("Bare Service", catalogue.tools().get(0).name());
      assertNull(catalogue.tools().get(0).description());
      assertTrue(catalogue.tools().get(0).reads().isEmpty());
      assertTrue(catalogue.tools().get(0).writes().isEmpty());
    }
  }

  @Nested
  class Walk {

    private static final String CAMUNDA_NS_URI = "http://camunda.org/schema/1.0/bpmn";

    // --- plain inputParameter node ---

    @Test
    void walk_simpleElExpression_addsVariableToOut() {
      Element node = new Element(null, null, "inputParameter", null, null);
      node.appendText("${customerId}");

      Set<String> result = AdHocSubProcessCatalogueBuilder.walk(node);

      assertFalse(result.isEmpty());
      assertEquals(Set.of("customerId"), result);
    }

    @Test
    void walk_noText_returnsEmpty() {
      Element node = new Element(null, null, "inputParameter", null, null);

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(node).isEmpty());
    }

    @Test
    void walk_dottedPath_returnsRootIdentifier() {
      Element node = new Element(null, null, "inputParameter", null, null);
      node.appendText("${customer.profile.email}");

      assertEquals(Set.of("customer"), AdHocSubProcessCatalogueBuilder.walk(node));
    }

    @Test
    void walk_complexElText_returnsEmpty() {
      Element node = new Element(null, null, "inputParameter", null, null);
      node.appendText("${a + b}");

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(node).isEmpty());
    }

    @Test
    void walk_literalText_returnsEmpty() {
      Element node = new Element(null, null, "inputParameter", null, null);
      node.appendText("manual");

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(node).isEmpty());
    }

    // --- camunda:script node ---

    @Test
    void walk_camundaScriptNode_returnsEmpty() {
      Element script = new Element(CAMUNDA_NS_URI, "script", null, null, null);
      script.appendText("someVar + 1");

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(script).isEmpty());
    }

    // --- camunda:list node ---

    @Test
    void walk_emptyList_returnsEmpty() {
      Element list = new Element(CAMUNDA_NS_URI, "list", null, null, null);

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(list).isEmpty());
    }

    @Test
    void walk_camundaListNode_extractsFromValues() {
      Element list = new Element(CAMUNDA_NS_URI, "list", null, null, null);
      Element v1 = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      v1.appendText("${alpha}");
      Element v2 = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      v2.appendText("${beta}");
      list.add(v1);
      list.add(v2);

      assertEquals(Set.of("alpha", "beta"), AdHocSubProcessCatalogueBuilder.walk(list));
    }

    @Test
    void walk_camundaListWithMixedValues_extractsOnlyElVars() {
      Element list = new Element(CAMUNDA_NS_URI, "list", null, null, null);
      Element v1 = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      v1.appendText("${userId}");
      Element v2 = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      v2.appendText("literal-value");
      Element v3 = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      v3.appendText("${roleId}");
      list.add(v1);
      list.add(v2);
      list.add(v3);

      assertEquals(Set.of("userId", "roleId"), AdHocSubProcessCatalogueBuilder.walk(list));
    }

    @Test
    void walk_camundaListWithComplexElValue_returnsEmpty() {
      Element list = new Element(CAMUNDA_NS_URI, "list", null, null, null);
      Element value = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      value.appendText("${a + b}");
      list.add(value);

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(list).isEmpty());
    }

    // --- camunda:map node ---

    @Test
    void walk_emptyMap_returnsEmpty() {
      Element map = new Element(CAMUNDA_NS_URI, "map", null, null, null);

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(map).isEmpty());
    }

    @Test
    void walk_camundaMapNode_extractsFromEntries() {
      Element map = new Element(CAMUNDA_NS_URI, "map", null, null, null);
      Element e1 = new Element(CAMUNDA_NS_URI, "entry", null, null, null);
      e1.appendText("${orderId}");
      Element e2 = new Element(CAMUNDA_NS_URI, "entry", null, null, null);
      e2.appendText("${customerId}");
      map.add(e1);
      map.add(e2);

      assertEquals(Set.of("orderId", "customerId"), AdHocSubProcessCatalogueBuilder.walk(map));
    }

    @Test
    void walk_camundaMapWithLiteralEntries_returnsEmpty() {
      Element map = new Element(CAMUNDA_NS_URI, "map", null, null, null);
      Element entry = new Element(CAMUNDA_NS_URI, "entry", null, null, null);
      entry.appendText("static-value");
      map.add(entry);

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(map).isEmpty());
    }

    // --- node with Camunda composite child ---

    @Test
    void walk_nodeWithCamundaScriptChild_returnsEmpty() {
      Element node = new Element(null, null, "inputParameter", null, null);
      Element script = new Element(CAMUNDA_NS_URI, "script", null, null, null);
      script.appendText("someVar + 1");
      node.add(script);

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(node).isEmpty());
    }

    @Test
    void walk_nodeWithCamundaListChild_delegatesToList() {
      Element node = new Element(null, null, "inputParameter", null, null);
      Element list = new Element(CAMUNDA_NS_URI, "list", null, null, null);
      Element value = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      value.appendText("${invoiceId}");
      list.add(value);
      node.add(list);

      assertEquals(Set.of("invoiceId"), AdHocSubProcessCatalogueBuilder.walk(node));
    }

    @Test
    void walk_nodeWithCamundaMapChild_delegatesToMap() {
      Element node = new Element(null, null, "inputParameter", null, null);
      Element map = new Element(CAMUNDA_NS_URI, "map", null, null, null);
      Element entry = new Element(CAMUNDA_NS_URI, "entry", null, null, null);
      entry.appendText("${accountId}");
      map.add(entry);
      node.add(map);

      assertEquals(Set.of("accountId"), AdHocSubProcessCatalogueBuilder.walk(node));
    }

    @Test
    void walk_nodeWithCamundaScriptChildBeforeCamundaList_returnsEmpty() {
      // findFirst() returns the script node, so the list is never reached
      Element node = new Element(null, null, "inputParameter", null, null);
      Element script = new Element(CAMUNDA_NS_URI, "script", null, null, null);
      Element list = new Element(CAMUNDA_NS_URI, "list", null, null, null);
      Element value = new Element(CAMUNDA_NS_URI, "value", null, null, null);
      value.appendText("${ignored}");
      list.add(value);
      node.add(script);
      node.add(list);

      assertTrue(AdHocSubProcessCatalogueBuilder.walk(node).isEmpty());
    }
  }
}
