package org.finos.fluxnova.bpm.engine.ai.a2a.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically refreshes stale agent card entries in the {@link AgentCardCache}.
 *
 * <p>The refresh interval is controlled by the Spring property
 * {@code fluxnova.a2a.agent-card.refresh-interval} (default: 5 minutes).
 * Uses {@link Scheduled#fixedDelayString()} so the next refresh starts only
 * after the previous one completes, avoiding overlap on slow networks.
 */
public class AgentCardRefreshScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AgentCardRefreshScheduler.class);

    private final AgentCardCache agentCardCache;

    public AgentCardRefreshScheduler(AgentCardCache agentCardCache) {
        this.agentCardCache = agentCardCache;
    }

    /**
     * Triggers a background refresh of all stale agent card cache entries.
     * Runs with a fixed delay (not fixed rate) to prevent overlap.
     */
    @Scheduled(fixedDelayString = "${fluxnova.a2a.agent-card.refresh-interval:PT5M}")
    public void refreshAgentCards() {
        LOG.debug("Scheduled agent card refresh starting");
        try {
            int refreshed = agentCardCache.refreshAll();
            LOG.debug("Scheduled agent card refresh completed, refreshed {} entries", refreshed);
        } catch (Exception e) {
            LOG.warn("Scheduled agent card refresh failed: {}", e.getMessage(), e);
        }
    }
}
