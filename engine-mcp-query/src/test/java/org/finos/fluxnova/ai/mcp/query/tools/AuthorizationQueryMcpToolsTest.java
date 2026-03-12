package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.AuthorizationResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.AuthorizationQueryDto;
import org.finos.fluxnova.bpm.engine.AuthorizationService;
import org.finos.fluxnova.bpm.engine.authorization.Authorization;
import org.finos.fluxnova.bpm.engine.authorization.AuthorizationQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static AuthorizationQueryDto emptyQuery() {
        try {
            return MAPPER.readValue("{}", AuthorizationQueryDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private AuthorizationService authorizationService;

    private AuthorizationQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new AuthorizationQueryMcpTools(authorizationService, 200);
    }

    @Nested
    class QueryAuthorizations {

        @Mock(answer = Answers.RETURNS_SELF)
        private AuthorizationQuery query;

        @BeforeEach
        void setUp() {
            when(authorizationService.createAuthorizationQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<AuthorizationResultDto> result = tools.queryAuthorizations(emptyQuery(), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).authorizationId(any());
            verify(query, never()).authorizationType(anyInt());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            AuthorizationQueryDto dto = new AuthorizationQueryDto(
                    "auth-1", 1,
                    List.of("user1", "user2"),
                    List.of("group1", "group2"),
                    2, "resource-1"
            );

            tools.queryAuthorizations(dto, null);

            verify(query).authorizationId("auth-1");
            verify(query).authorizationType(1);
            verify(query).userIdIn("user1", "user2");
            verify(query).groupIdIn("group1", "group2");
            verify(query).resourceType(2);
            verify(query).resourceId("resource-1");
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            AuthorizationQueryDto dto = new AuthorizationQueryDto(
                    null, null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    null, null
            );

            tools.queryAuthorizations(dto, null);

            verify(query, never()).authorizationId(any());
            verify(query, never()).authorizationType(anyInt());
            verify(query, never()).userIdIn(any(String[].class));
            verify(query, never()).groupIdIn(any(String[].class));
            verify(query, never()).resourceType(anyInt());
            verify(query, never()).resourceId(any());
        }

        @Test
        void resultMapping() {
            Authorization auth = mock(Authorization.class);
            when(auth.getId()).thenReturn("auth-1");
            when(auth.getAuthorizationType()).thenReturn(1);
            when(auth.getUserId()).thenReturn("user1");
            when(auth.getGroupId()).thenReturn(null);
            when(auth.getResourceType()).thenReturn(2);
            when(auth.getResourceId()).thenReturn("resource-1");
            when(query.list()).thenReturn(List.of(auth));

            List<AuthorizationResultDto> result = tools.queryAuthorizations(emptyQuery(), null);

            assertEquals(1, result.size());
            AuthorizationResultDto r = result.getFirst();
            assertEquals("auth-1", r.id());
            assertEquals(1, r.authorizationType());
            assertEquals("user1", r.userId());
            assertNull(r.groupId());
            assertEquals(2, r.resourceType());
            assertEquals("resource-1", r.resourceId());
        }
    }
}
