package org.finos.fluxnova.bpm.engine.ai.a2a.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentCard;
import org.finos.fluxnova.bpm.engine.ai.a2a.model.AgentSkill;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link AgentCardCache}.
 *
 * <p>Validates: Requirements 8.2, 8.3, 8.5</p>
 */
class AgentCardCachePropertyTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WireMockServer wireMockServer;

    @BeforeProperty
    void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterProperty
    void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    private AgentCardCache createCache() {
        RestClient restClient = RestClient.builder()
                .build();
        return new AgentCardCache(restClient);
    }

    // ========================================================================
    // Property 5: Agent Card Parse Round-Trip
    // ========================================================================

    /**
     * Property 5: Agent Card Parse Round-Trip
     *
     * <p>For any valid agent card JSON (with name, description, protocolVersion, skills),
     * the cache parses it correctly and the resulting AgentCard fields match the input.</p>
     *
     * <p><b>Validates: Requirements 8.2, 8.3</b></p>
     */
    @Property(tries = 50)
    void validAgentCardParseRoundTrip(
            @ForAll("agentNames") String name,
            @ForAll("agentDescriptions") String description,
            @ForAll("protocolVersions") String protocolVersion,
            @ForAll("skillLists") List<SkillData> skills) throws Exception {

        // Reset WireMock state for each trial
        wireMockServer.resetAll();

        // Build valid agent card JSON
        String agentCardJson = buildValidAgentCardJson(name, description, protocolVersion, skills);

        // Stub WireMock to return the agent card
        wireMockServer.stubFor(get(urlEqualTo("/.well-known/agent-card.json"))
                .willReturn(okJson(agentCardJson)));

        AgentCardCache cache = createCache();
        String baseUrl = wireMockServer.baseUrl();

        Optional<AgentCard> result = cache.getOrFetch(baseUrl);

        // Verify parsing succeeded
        assertTrue(result.isPresent(), "Valid agent card should be parsed successfully");

        AgentCard card = result.get();

        // Verify all fields match the input
        assertEquals(name, card.name(), "Parsed name must match input");
        assertEquals(description, card.description(), "Parsed description must match input");
        assertEquals(protocolVersion, card.protocolVersion(), "Parsed protocolVersion must match input");
        assertEquals(skills.size(), card.skills().size(), "Number of skills must match");

        for (int i = 0; i < skills.size(); i++) {
            SkillData expected = skills.get(i);
            AgentSkill actual = card.skills().get(i);
            assertEquals(expected.name(), actual.name(), "Skill name at index " + i + " must match");
            assertEquals(expected.description(), actual.description(), "Skill description at index " + i + " must match");
        }

        // Verify it was cached
        Optional<AgentCard> cached = cache.getCached(baseUrl);
        assertTrue(cached.isPresent(), "Successfully parsed card must be cached");
        assertEquals(card, cached.get(), "Cached card must match returned card");
    }

    // ========================================================================
    // Property 6: Invalid Agent Card JSON Is Always Rejected
    // ========================================================================

    /**
     * Property 6: Invalid Agent Card JSON Is Always Rejected
     *
     * <p>For any malformed or incomplete agent card JSON (missing required fields,
     * invalid JSON, empty), the cache returns Optional.empty() and does NOT cache it.</p>
     *
     * <p><b>Validates: Requirements 8.3, 8.5</b></p>
     */
    @Property(tries = 50)
    void invalidAgentCardJsonIsAlwaysRejected(@ForAll("invalidAgentCardBodies") String body) {
        // Reset WireMock state for each trial
        wireMockServer.resetAll();

        // Stub WireMock to return the invalid body
        wireMockServer.stubFor(get(urlEqualTo("/.well-known/agent-card.json"))
                .willReturn(okJson(body)));

        AgentCardCache cache = createCache();
        String baseUrl = wireMockServer.baseUrl();

        Optional<AgentCard> result = cache.getOrFetch(baseUrl);

        // Verify parsing failed
        assertTrue(result.isEmpty(),
                "Invalid agent card JSON should return Optional.empty(), body was: " + body);

        // Verify it was NOT cached (D12: failed fetches NOT cached)
        Optional<AgentCard> cached = cache.getCached(baseUrl);
        assertTrue(cached.isEmpty(),
                "Failed parse must NOT be cached, body was: " + body);
    }

    // ========================================================================
    // Generators / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> agentNames() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(100)
                .alpha()
                .numeric()
                .withChars(' ', '-', '_');
    }

    @Provide
    Arbitrary<String> agentDescriptions() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(200)
                .alpha()
                .numeric()
                .withChars(' ', '.', ',', '-', '_', '!');
    }

    @Provide
    Arbitrary<String> protocolVersions() {
        return Arbitraries.of("0.1.0", "0.2.0", "0.3.0", "1.0.0", "1.1.0", "2.0.0");
    }

    @Provide
    Arbitrary<List<SkillData>> skillLists() {
        Arbitrary<SkillData> skill = Combinators.combine(
                Arbitraries.strings().ofMinLength(1).ofMaxLength(50).alpha().numeric().withChars(' ', '-', '_'),
                Arbitraries.strings().ofMinLength(1).ofMaxLength(100).alpha().numeric().withChars(' ', '.', ',', '-')
        ).as(SkillData::new);

        return skill.list().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<String> invalidAgentCardBodies() {
        return Arbitraries.oneOf(
                // Not valid JSON at all
                Arbitraries.of(
                        "not json at all",
                        "{invalid",
                        "[]",
                        "123",
                        ""
                ),
                // Valid JSON but missing required fields
                Arbitraries.of(
                        // Missing name
                        "{\"description\":\"desc\",\"protocolVersion\":\"0.3.0\",\"skills\":[]}",
                        // Missing description
                        "{\"name\":\"agent\",\"protocolVersion\":\"0.3.0\",\"skills\":[]}",
                        // Missing protocolVersion
                        "{\"name\":\"agent\",\"description\":\"desc\",\"skills\":[]}",
                        // Missing skills
                        "{\"name\":\"agent\",\"description\":\"desc\",\"protocolVersion\":\"0.3.0\"}",
                        // Empty object
                        "{}",
                        // All fields null
                        "{\"name\":null,\"description\":null,\"protocolVersion\":null,\"skills\":null}",
                        // name is not a string
                        "{\"name\":123,\"description\":\"desc\",\"protocolVersion\":\"0.3.0\",\"skills\":[]}",
                        // description is not a string
                        "{\"name\":\"agent\",\"description\":42,\"protocolVersion\":\"0.3.0\",\"skills\":[]}",
                        // protocolVersion is not a string
                        "{\"name\":\"agent\",\"description\":\"desc\",\"protocolVersion\":3,\"skills\":[]}",
                        // skills is not an array
                        "{\"name\":\"agent\",\"description\":\"desc\",\"protocolVersion\":\"0.3.0\",\"skills\":\"not-array\"}",
                        // skills is null
                        "{\"name\":\"agent\",\"description\":\"desc\",\"protocolVersion\":\"0.3.0\",\"skills\":null}"
                )
        );
    }

    // ========================================================================
    // Helper Records and Methods
    // ========================================================================

    record SkillData(String name, String description) {}

    private String buildValidAgentCardJson(String name, String description,
                                           String protocolVersion, List<SkillData> skills) throws Exception {
        List<Map<String, String>> skillMaps = skills.stream()
                .map(s -> Map.of("name", s.name(), "description", s.description()))
                .collect(Collectors.toList());

        Map<String, Object> cardMap = Map.of(
                "name", name,
                "description", description,
                "protocolVersion", protocolVersion,
                "skills", skillMaps
        );

        return OBJECT_MAPPER.writeValueAsString(cardMap);
    }
}
