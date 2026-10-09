package org.finos.fluxnova.bpm.engine.ai.a2a.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.ai.a2a.auth.A2aAuthProvider;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentCard;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory cache for agent cards, keyed by base URL.
 * Fetches from {baseUrl}/.well-known/agent-card.json on first access.
 * Caches on success. Does NOT cache failures (allows retry on next access).
 *
 * <p>Supports TTL-based expiry: cached entries older than the configured TTL are
 * treated as stale and re-fetched on the next access. The {@link #refreshAll()} method
 * can be called periodically (e.g. by a scheduler) to proactively refresh all stale
 * entries in the background.
 */
public class AgentCardCache {

    private static final Logger LOG = LoggerFactory.getLogger(AgentCardCache.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";

    /**
     * Sentinel TTL meaning "never expires". Represented as {@code null} internally
     * rather than a huge {@link Duration} to avoid instant arithmetic overflow.
     */
    private static final Duration NO_EXPIRY = null;

    private final RestClient restClient;
    private final A2aAuthProvider authProvider;
    /** The cache TTL, or {@code null} to disable expiry entirely. */
    private final Duration ttl;
    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<AgentCardChangeListener> changeListeners = new CopyOnWriteArrayList<>();

    /**
     * A cached agent card together with its fetch timestamp and the agentRef used
     * for authentication during the original fetch.
     */
    record CachedEntry(AgentCard card, Instant fetchedAt, String agentRef) {

        /**
         * @param ttl the time-to-live, or {@code null} / non-positive to mean "never expires"
         * @return whether this entry is older than the given TTL
         */
        boolean isExpired(Duration ttl) {
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                return false;
            }
            Instant now = Instant.now();
            // Compare via elapsed duration to avoid Instant.plus overflow for large TTLs.
            return Duration.between(fetchedAt, now).compareTo(ttl) > 0;
        }
    }

    /**
     * Callback invoked when a cached agent card's content changes (i.e. a re-fetch
     * returns a card that is not equal to the previously cached one). Listeners can use
     * this to invalidate any downstream state derived from agent cards.
     */
    @FunctionalInterface
    public interface AgentCardChangeListener {
        /**
         * @param baseUrl     the base URL whose agent card changed
         * @param previous    the previously cached card (never {@code null} — only fired on change)
         * @param updated     the newly fetched card
         */
        void onAgentCardChanged(String baseUrl, AgentCard previous, AgentCard updated);
    }

    /**
     * Registers a listener notified whenever a cached agent card's content changes.
     */
    public void addChangeListener(AgentCardChangeListener listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public AgentCardCache(RestClient restClient, A2aAuthProvider authProvider, Duration ttl) {
        this.restClient = restClient;
        this.authProvider = authProvider;
        // A null, zero, or negative TTL disables expiry (treated as "never expires").
        this.ttl = ttl;
    }

    public AgentCardCache(RestClient restClient, A2aAuthProvider authProvider) {
        this(restClient, authProvider, NO_EXPIRY);
    }

    /** Backward-compatible constructor (no auth, no TTL — never expires). */
    public AgentCardCache(RestClient restClient) {
        this(restClient, agentRef -> Map.of(), NO_EXPIRY);
    }

    /**
     * Returns cached card if present and not expired, otherwise fetches from
     * {baseUrl}/.well-known/agent-card.json. No authentication applied.
     */
    public Optional<AgentCard> getOrFetch(String baseUrl) {
        return getOrFetch(baseUrl, null);
    }

    /**
     * Returns cached card if present and not expired, otherwise fetches from
     * {baseUrl}/.well-known/agent-card.json. Applies authentication headers for the
     * given agentRef. Caches on success. Does NOT cache failures (allows retry on next
     * access).
     *
     * @param baseUrl  the base URL of the remote A2A agent
     * @param agentRef the agent reference for auth lookup (may be null)
     * @return the agent card if available, or empty if fetch failed
     */
    public Optional<AgentCard> getOrFetch(String baseUrl, String agentRef) {
        CachedEntry entry = cache.get(baseUrl);
        if (entry != null && !entry.isExpired(ttl)) {
            return Optional.of(entry.card());
        }

        return fetchAndCache(baseUrl, agentRef);
    }

    /**
     * Returns the cached agent card for the given base URL without fetching.
     * Returns the card even if expired (caller can check freshness separately).
     * Visible for testing.
     *
     * @param baseUrl the base URL of the remote A2A agent
     * @return the cached agent card, or empty if not cached
     */
    public Optional<AgentCard> getCached(String baseUrl) {
        CachedEntry entry = cache.get(baseUrl);
        return entry != null ? Optional.of(entry.card()) : Optional.empty();
    }

    /**
     * Refreshes all stale (expired) entries in the cache by re-fetching their agent
     * cards. Non-expired entries are left untouched. Failed re-fetches retain the
     * previous (stale) entry so callers always see the last-known-good card.
     *
     * <p>Intended to be called periodically by a scheduler.
     *
     * @return the number of entries that were successfully refreshed
     */
    public int refreshAll() {
        Set<Map.Entry<String, CachedEntry>> entries = cache.entrySet();
        int refreshed = 0;
        for (Map.Entry<String, CachedEntry> mapEntry : entries) {
            CachedEntry entry = mapEntry.getValue();
            if (!entry.isExpired(ttl)) {
                continue;
            }
            String baseUrl = mapEntry.getKey();
            LOG.debug("Refreshing stale agent card for '{}'", baseUrl);
            Optional<AgentCard> fetched = fetchAndCache(baseUrl, entry.agentRef());
            if (fetched.isPresent()) {
                refreshed++;
                LOG.debug("Successfully refreshed agent card for '{}'", baseUrl);
            } else {
                LOG.debug("Failed to refresh agent card for '{}', keeping stale entry", baseUrl);
            }
        }
        if (refreshed > 0) {
            LOG.info("Refreshed {} agent card(s)", refreshed);
        }
        return refreshed;
    }

    private void notifyAgentCardChanged(String baseUrl, AgentCard previous, AgentCard updated) {
        LOG.info("Agent card content changed for '{}', notifying {} listener(s)",
                baseUrl, changeListeners.size());
        for (AgentCardChangeListener listener : changeListeners) {
            try {
                listener.onAgentCardChanged(baseUrl, previous, updated);
            } catch (Exception e) {
                LOG.warn("Agent card change listener failed for '{}': {}", baseUrl, e.getMessage(), e);
            }
        }
    }

    /**
     * Evicts the cached entry for the given base URL.
     *
     * @param baseUrl the base URL whose agent card should be removed
     */
    public void evict(String baseUrl) {
        cache.remove(baseUrl);
    }

    /**
     * Evicts all cached entries.
     */
    public void evictAll() {
        cache.clear();
    }

    /**
     * Returns the number of entries currently in the cache.
     */
    public int size() {
        return cache.size();
    }

    private Optional<AgentCard> fetchAndCache(String baseUrl, String agentRef) {
        String url = baseUrl + AGENT_CARD_PATH;
        String responseBody;

        try {
            Map<String, String> authHeaders = authProvider.getAuthHeaders(agentRef);
            var requestSpec = restClient.get().uri(url);

            for (Map.Entry<String, String> header : authHeaders.entrySet()) {
                requestSpec = requestSpec.header(header.getKey(), header.getValue());
            }

            responseBody = requestSpec
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            LOG.warn("Failed to fetch agent card from '{}': {}", url, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warn("Unexpected error fetching agent card from '{}': {}", url, e.getMessage());
            return Optional.empty();
        }

        return parseAndCache(baseUrl, url, responseBody, agentRef);
    }

    private Optional<AgentCard> parseAndCache(String baseUrl, String url, String responseBody,
                                               String agentRef) {
        if (responseBody == null || responseBody.isBlank()) {
            LOG.warn("Agent card response from '{}' is empty", url);
            return Optional.empty();
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(responseBody);
        } catch (Exception e) {
            LOG.warn("Failed to parse agent card JSON from '{}': {}", url, e.getMessage());
            return Optional.empty();
        }

        try {
            String name = getRequiredTextField(root, "name", url);
            String description = getRequiredTextField(root, "description", url);
            String protocolVersion = getRequiredTextField(root, "protocolVersion", url);
            List<AgentSkill> skills = parseSkills(root, url);

            // url field is optional in the agent card
            String agentUrl = null;
            JsonNode urlNode = root.get("url");
            if (urlNode != null && urlNode.isTextual() && !urlNode.asText().isBlank()) {
                agentUrl = urlNode.asText();
            }

            if (name == null || description == null || protocolVersion == null || skills == null) {
                return Optional.empty();
            }

            AgentCard agentCard = new AgentCard(name, description, agentUrl, protocolVersion, skills);
            CachedEntry previous = cache.put(baseUrl, new CachedEntry(agentCard, Instant.now(), agentRef));
            LOG.debug("Cached agent card for '{}': name='{}', skills={}", baseUrl, name, skills.size());

            // Fire change listeners only when the content actually changed on a re-fetch.
            // AgentCard is a record, so equals() is a deep, value-based comparison.
            if (previous != null && !Objects.equals(previous.card(), agentCard)) {
                notifyAgentCardChanged(baseUrl, previous.card(), agentCard);
            }
            return Optional.of(agentCard);
        } catch (Exception e) {
            LOG.warn("Failed to extract agent card fields from '{}': {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private String getRequiredTextField(JsonNode root, String fieldName, String url) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull() || !node.isTextual()) {
            LOG.warn("Agent card from '{}' is missing required field '{}'", url, fieldName);
            return null;
        }
        return node.asText();
    }

    private List<AgentSkill> parseSkills(JsonNode root, String url) {
        JsonNode skillsNode = root.get("skills");
        if (skillsNode == null || skillsNode.isNull() || !skillsNode.isArray()) {
            LOG.warn("Agent card from '{}' is missing required 'skills' array", url);
            return null;
        }

        List<AgentSkill> skills = new ArrayList<>();
        for (JsonNode skillNode : skillsNode) {
            String name = skillNode.has("name") && skillNode.get("name").isTextual()
                    ? skillNode.get("name").asText() : "";
            String description = skillNode.has("description") && skillNode.get("description").isTextual()
                    ? skillNode.get("description").asText() : "";
            skills.add(new AgentSkill(name, description));
        }
        return skills;
    }
}
