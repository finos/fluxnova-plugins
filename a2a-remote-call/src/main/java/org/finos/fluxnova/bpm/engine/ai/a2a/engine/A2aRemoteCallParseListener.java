package org.finos.fluxnova.bpm.engine.ai.a2a.engine;

import org.finos.fluxnova.bpm.engine.BpmnParseException;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.A2aRemoteCallConfig;
import org.finos.fluxnova.bpm.engine.ai.a2a.service.A2aInvocationService;
import org.finos.fluxnova.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ActivityImpl;
import org.finos.fluxnova.bpm.engine.impl.pvm.process.ScopeImpl;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BPMN parse listener that processes {@code <a2a-remote:config>} on service tasks
 * with {@code camunda:type="a2a-remote"}.
 * <p>
 * Detection requires both:
 * <ul>
 *   <li>{@code camunda:type="a2a-remote"} attribute on the service task</li>
 *   <li>{@code <a2a-remote:config>} extension element with the remote agent configuration</li>
 * </ul>
 * <p>
 * No network requests are issued during parsing — only structural validation is performed.
 */
public class A2aRemoteCallParseListener extends AbstractBpmnParseListener {

    private static final Logger LOG = LoggerFactory.getLogger(A2aRemoteCallParseListener.class);

    private static final Namespace A2A_REMOTE_NS =
            new Namespace("http://fluxnova.finos.org/schema/1.0/ai/a2a-remote");

    private static final Namespace FLUXNOVA_NS =
            new Namespace("http://camunda.org/schema/1.0/bpmn");

    static final String A2A_REMOTE_TYPE = "a2a-remote";

    static final String CONFIG_PROPERTY_KEY = "a2aRemoteCallConfig";

    private final A2aInvocationService invocationService;

    /**
     * Creates a parse listener without an invocation service.
     * In this mode, the listener stores config but does not set the activity behaviour.
     * Useful for testing and when behaviour is set externally.
     */
    public A2aRemoteCallParseListener() {
        this(null);
    }

    /**
     * Creates a parse listener with an invocation service.
     * When provided, the listener also sets the {@link A2aRemoteCallActivityBehaviour}
     * on service tasks with {@code camunda:type="a2a-remote"}.
     */
    public A2aRemoteCallParseListener(A2aInvocationService invocationService) {
        this.invocationService = invocationService;
    }

    @Override
    public void parseServiceTask(Element element, ScopeImpl scope, ActivityImpl activity) {
        String type = element.attributeNS(FLUXNOVA_NS, "type");
        if (!A2A_REMOTE_TYPE.equals(type)) {
            return;
        }

        Element extensionElements = element.element("extensionElements");
        if (extensionElements == null) {
            return;
        }

        Element configElement = extensionElements.elementNS(A2A_REMOTE_NS, "config");
        if (configElement == null) {
            return;
        }

        String elementId = element.attribute("id");
        String url = configElement.attribute("url");
        String prompt = configElement.attribute("prompt");
        String name = configElement.attribute("name");
        String description = configElement.attribute("description");
        String agentRef = configElement.attribute("agent-ref");
        String timeoutStr = configElement.attribute("timeout");

        if (url == null || url.isBlank()) {
            throw new BpmnParseException(
                    "Element '" + elementId + "': <a2a-remote:config> requires a non-blank 'url' attribute",
                    element);
        }

        if (description == null || description.isBlank()) {
            LOG.warn("A2A - Element '{}': <a2a-remote:config> has blank 'description' — "
                    + "LLM tool selection may be impaired", elementId);
        }

        if (agentRef == null || agentRef.isBlank()) {
            LOG.warn("A2A - Element '{}': <a2a-remote:config> is missing 'agent-ref' attribute — "
                    + "authentication and agent routing may not work correctly", elementId);
        }

        Long timeout = null;
        if (timeoutStr != null && !timeoutStr.isBlank()) {
            try {
                timeout = Long.parseLong(timeoutStr);
            } catch (NumberFormatException e) {
                LOG.warn("A2A - Element '{}': invalid 'timeout' value '{}' — ignoring", elementId, timeoutStr);
            }
        }

        A2aRemoteCallConfig config = new A2aRemoteCallConfig(
                url,
                prompt == null || prompt.isBlank() ? null : prompt,
                name == null || name.isBlank() ? "" : name,
                description == null || description.isBlank() ? "" : description,
                agentRef == null || agentRef.isBlank() ? null : agentRef,
                timeout
        );

        activity.setProperty(CONFIG_PROPERTY_KEY, config);

        // Set the activity behaviour if invocation service is available
        if (invocationService != null) {
            activity.setActivityBehavior(new A2aRemoteCallActivityBehaviour(invocationService));
        }

        LOG.debug("A2A - Parsed standalone remote call config on service task '{}': url={}", elementId, url);
    }
}
