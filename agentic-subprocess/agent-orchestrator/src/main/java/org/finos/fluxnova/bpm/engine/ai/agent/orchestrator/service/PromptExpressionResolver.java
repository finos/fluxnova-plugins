package org.finos.fluxnova.bpm.engine.ai.agent.orchestrator.service;

import org.finos.fluxnova.bpm.engine.delegate.VariableScope;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.el.ExpressionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves the {@code userPrompt} template configured on {@code agent:config} against a
 * process instance's variables.
 *
 * <p>Templates may be plain text, a single expression ({@code ${taskPrompt}}) or text mixed
 * with expressions ({@code "Investigate alert ${alertId} for ${customerName}"}). Expressions are
 * evaluated with the engine's {@link ExpressionManager}, the same mechanism used for other
 * expression-capable BPMN attributes.
 *
 * <p>The resolver never throws. It returns {@link Optional#empty()} when:
 * <ul>
 *   <li>the template is {@code null} or blank,</li>
 *   <li>evaluation fails (e.g. an undefined variable or invalid expression syntax), or</li>
 *   <li>the evaluated result is {@code null} or blank.</li>
 * </ul>
 * Callers apply their own fallback in these cases.
 */
public class PromptExpressionResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PromptExpressionResolver.class);

    private final Supplier<ExpressionManager> expressionManagerSupplier;

    /**
     * Creates a resolver that uses the expression manager of the current engine command context.
     */
    public PromptExpressionResolver() {
        this(() -> Context.getProcessEngineConfiguration().getExpressionManager());
    }

    /**
     * Creates a resolver backed by the given expression manager supplier. The supplier is only
     * invoked when a template actually contains an expression.
     */
    public PromptExpressionResolver(Supplier<ExpressionManager> expressionManagerSupplier) {
        this.expressionManagerSupplier = expressionManagerSupplier;
    }

    /**
     * Resolves the template against the given variable scope.
     *
     * @param template   raw template text from the BPMN model, may be {@code null}
     * @param scope      variable scope to evaluate expressions against (typically the agent's
     *                   scope execution, which also sees process-instance variables)
     * @param promptName attribute name used in log messages (e.g. {@code "userPrompt"})
     * @return the resolved, non-blank prompt text, or empty if a fallback should be used
     */
    public Optional<String> resolve(String template, VariableScope scope, String promptName) {
        if (template == null || template.isBlank()) {
            return Optional.empty();
        }
        if (!containsExpression(template)) {
            return Optional.of(template);
        }

        Object value;
        try {
            value = expressionManagerSupplier.get().createExpression(template).getValue(scope);
        } catch (RuntimeException e) {
            // Undefined variables, syntax errors and evaluation errors all fall back to the default.
            LOG.warn("Could not evaluate {} expression '{}', falling back to default: {}",
                    promptName, template, e.getMessage());
            return Optional.empty();
        }

        if (value == null) {
            LOG.debug("{} expression '{}' evaluated to null, falling back to default", promptName, template);
            return Optional.empty();
        }
        String resolved = String.valueOf(value);
        if (resolved.isBlank()) {
            LOG.debug("{} expression '{}' evaluated to blank, falling back to default", promptName, template);
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    static boolean containsExpression(String template) {
        return template.contains("${") || template.contains("#{");
    }
}
