package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.ai.agent.discovery.model.AgentToolCatalogue;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * Strategy for building an {@link AgentToolCatalogue} from a runtime BPMN scope activity.
 *
 * <p>Implementations derive the set of available tools from the child activities of a
 * scope, inferring names, descriptions, and variable read/write sets from the activity
 * model. Implementations are expected to be stateless and idempotent.
 */
public interface AgentToolCatalogueBuilder {

    /**
     * Builds a tool catalogue for all startable activities within the given scope.
     *
     * @param scope the {@link ActivityImpl} representing the tool scope; must not be
     *              {@code null}
     * @return a catalogue containing one {@link AgentToolEntry} per startable activity
     *         found in the scope; never {@code null}, but may contain an empty tool list
     *         if no startable activities are present
     */
    AgentToolCatalogue build(ActivityImpl scope);
}
