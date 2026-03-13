package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * MCP tools for retrieving raw XML model definitions from the process engine.
 * <p>
 * Provides access to deployed BPMN process models, DMN decision models,
 * DMN decision requirements models, and CMMN case models as raw XML strings
 * via the process engine's {@link RepositoryService}.
 */
public class XMLMcpTools {

    private static final Logger LOG = LoggerFactory.getLogger(XMLMcpTools.class);

    private final RepositoryService repositoryService;

    public XMLMcpTools(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    // ---- BPMN ----

    @McpTool(description = "Retrieve the BPMN 2.0 XML source of a deployed process definition. "
            + "Returns the raw BPMN XML as a string, which describes the process flow, "
            + "tasks, gateways, events, and other elements of the workflow. "
            + "Use this tool to inspect or analyse the structure of a specific process definition.")
    public String getProcessModelXml(
            @McpToolParam(description = "The ID of the process definition to retrieve the BPMN XML for. "
                    + "Use the queryProcessDefinitions tool to find the ID of the desired process definition.")
            String processDefinitionId) {
        LOG.info("Retrieving BPMN XML for process definition: {}", processDefinitionId);
        try (InputStream stream = repositoryService.getProcessModel(processDefinitionId)) {
            String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LOG.info("Successfully retrieved BPMN XML for process definition: {}", processDefinitionId);
            return xml;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read BPMN XML for process definition: " + processDefinitionId, e);
        }
    }

    // ---- DMN ----

    @McpTool(description = "Retrieve the DMN 1.1 XML source of a deployed decision definition. "
            + "Returns the raw DMN XML as a string, which describes the decision table or "
            + "literal expression logic used to evaluate business decisions. "
            + "Use this tool to inspect or analyse the logic of a specific decision definition.")
    public String getDecisionModelXml(
            @McpToolParam(description = "The ID of the decision definition to retrieve the DMN XML for. "
                    + "Use the queryDecisionDefinitions tool to find the ID of the desired decision definition.")
            String decisionDefinitionId) {
        LOG.info("Retrieving DMN XML for decision definition: {}", decisionDefinitionId);
        try (InputStream stream = repositoryService.getDecisionModel(decisionDefinitionId)) {
            String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LOG.info("Successfully retrieved DMN XML for decision definition: {}", decisionDefinitionId);
            return xml;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read DMN XML for decision definition: " + decisionDefinitionId, e);
        }
    }

    @McpTool(description = "Retrieve the DMN 1.1 XML source of a deployed decision requirements definition. "
            + "Returns the raw DMN XML as a string, which describes the decision requirements graph (DRG) "
            + "containing a set of related decisions and their dependencies. "
            + "Use this tool to inspect the full decision requirements structure for a DMN resource.")
    public String getDecisionRequirementsModelXml(
            @McpToolParam(description = "The ID of the decision requirements definition to retrieve the DMN XML for. "
                    + "Use the queryDecisionRequirementsDefinitions tool to find the ID of the desired definition.")
            String decisionRequirementsDefinitionId) {
        LOG.info("Retrieving DMN XML for decision requirements definition: {}", decisionRequirementsDefinitionId);
        try (InputStream stream = repositoryService.getDecisionRequirementsModel(decisionRequirementsDefinitionId)) {
            String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LOG.info("Successfully retrieved DMN XML for decision requirements definition: {}", decisionRequirementsDefinitionId);
            return xml;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read DMN XML for decision requirements definition: " + decisionRequirementsDefinitionId, e);
        }
    }

    // ---- CMMN ----

    @McpTool(description = "Retrieve the CMMN 1.0 XML source of a deployed case definition. "
            + "Returns the raw CMMN XML as a string, which describes the case plan model, "
            + "stages, tasks, milestones, and sentries of the case. "
            + "Use this tool to inspect or analyse the structure of a specific case definition.")
    public String getCaseModelXml(
            @McpToolParam(description = "The ID of the case definition to retrieve the CMMN XML for. "
                    + "Use the queryCaseDefinitions tool to find the ID of the desired case definition.")
            String caseDefinitionId) {
        LOG.info("Retrieving CMMN XML for case definition: {}", caseDefinitionId);
        try (InputStream stream = repositoryService.getCaseModel(caseDefinitionId)) {
            String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LOG.info("Successfully retrieved CMMN XML for case definition: {}", caseDefinitionId);
            return xml;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CMMN XML for case definition: " + caseDefinitionId, e);
        }
    }
}
