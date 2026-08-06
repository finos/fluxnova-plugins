package org.finos.fluxnova.bpm.engine.shared.model;

/**
 * Token consumption reported by an LLM provider for a single completion request.
 *
 * <p>{@code cacheReadInputTokens} are tokens served from a prompt cache (billed at the cache-input
 * rate). {@code cacheWriteInputTokens} are tokens written into the cache (billed at the cache-write
 * rate). Provider reporting differs: OpenAI includes cache reads inside {@code promptTokens};
 * Anthropic typically reports cache buckets separately from {@code promptTokens}.
 */
public record TokenUsage(
    int promptTokens,
    int completionTokens,
    int totalTokens,
    int cacheReadInputTokens,
    int cacheWriteInputTokens) {

  public TokenUsage {
    if (totalTokens <= 0 && (promptTokens > 0 || completionTokens > 0)) {
      totalTokens = promptTokens + completionTokens;
    }
    if (cacheReadInputTokens < 0) {
      cacheReadInputTokens = 0;
    }
    if (cacheWriteInputTokens < 0) {
      cacheWriteInputTokens = 0;
    }
  }

  /** Convenience constructor when cache token counts are unavailable. */
  public TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
    this(promptTokens, completionTokens, totalTokens, 0, 0);
  }
}
