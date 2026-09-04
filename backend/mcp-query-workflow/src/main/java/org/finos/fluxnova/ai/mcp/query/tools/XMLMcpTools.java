package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public String getProcessModelXml(
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
    public String getDecisionModelXml(
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
    public String getDecisionRequirementsModelXml(
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
    public String getCaseModelXml(
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
