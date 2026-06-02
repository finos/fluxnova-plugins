package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentContextSpec;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;

/**
 * Strategy for building an {@link AgentContextSpec} from a BPMN element's extension
 * elements.
 *
 * <p>When no context declaration is present on the element, implementations should
 * return an {@link AgentContextSpec} with an empty variable list rather than
 * {@code null}. Implementations are expected to be stateless and idempotent.
 */
public interface AgentContextSpecBuilder {

    /**
     * Builds a context specification for the given BPMN element.
     *
     * @param scopeElement        the BPMN XML element that may carry context declarations
     *                            in its extension elements; must not be {@code null}
     * @param processDefinitionId the id of the process definition that owns the element;
     *                            stored verbatim in the returned spec for correlation
     * @return a context specification; never {@code null}. Returns a spec with an empty
     *         variable list if the element carries no context declaration.
     */
    AgentContextSpec build(Element scopeElement, String processDefinitionId);
}
