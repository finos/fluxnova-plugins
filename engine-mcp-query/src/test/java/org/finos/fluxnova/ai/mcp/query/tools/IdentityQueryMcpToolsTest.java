package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.GroupResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.TenantResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.UserResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.GroupQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.TenantQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.UserQueryDto;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.GroupQuery;
import org.finos.fluxnova.bpm.engine.identity.Tenant;
import org.finos.fluxnova.bpm.engine.identity.TenantQuery;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.finos.fluxnova.bpm.engine.identity.UserQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static <T> T empty(Class<T> type) {
        try {
            return MAPPER.readValue("{}", type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private IdentityService identityService;

    private IdentityQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new IdentityQueryMcpTools(identityService, 200);
    }

    @Nested
    class QueryUsers {

        @Mock(answer = Answers.RETURNS_SELF)
        private UserQuery query;

        @BeforeEach
        void setUp() {
            when(identityService.createUserQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<UserResultDto> result = tools.queryUsers(empty(UserQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).userId(any());
            verify(query, never()).userEmail(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            UserQueryDto dto = new UserQueryDto(
                    "john", List.of("john", "jane"),
                    "John", "Jo%",
                    "Doe", "Do%",
                    "john@example.com", "%@example.com",
                    "managers", "acme"
            );

            tools.queryUsers(dto, null);

            verify(query).userId("john");
            verify(query).userIdIn("john", "jane");
            verify(query).userFirstName("John");
            verify(query).userFirstNameLike("Jo%");
            verify(query).userLastName("Doe");
            verify(query).userLastNameLike("Do%");
            verify(query).userEmail("john@example.com");
            verify(query).userEmailLike("%@example.com");
            verify(query).memberOfGroup("managers");
            verify(query).memberOfTenant("acme");
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            UserQueryDto dto = new UserQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null,
                    null, null, null, null
            );

            tools.queryUsers(dto, null);

            verify(query, never()).userId(any());
            verify(query, never()).userIdIn(any(String[].class));
            verify(query, never()).memberOfGroup(any());
            verify(query, never()).memberOfTenant(any());
        }

        @Test
        void resultMapping() {
            User user = mock(User.class);
            when(user.getId()).thenReturn("john");
            when(user.getFirstName()).thenReturn("John");
            when(user.getLastName()).thenReturn("Doe");
            when(user.getEmail()).thenReturn("john@example.com");
            when(query.list()).thenReturn(List.of(user));

            List<UserResultDto> result = tools.queryUsers(empty(UserQueryDto.class), null);

            assertEquals(1, result.size());
            UserResultDto r = result.getFirst();
            assertEquals("john", r.id());
            assertEquals("John", r.firstName());
            assertEquals("Doe", r.lastName());
            assertEquals("john@example.com", r.email());
        }
    }

    @Nested
    class QueryGroups {

        @Mock(answer = Answers.RETURNS_SELF)
        private GroupQuery query;

        @BeforeEach
        void setUp() {
            when(identityService.createGroupQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<GroupResultDto> result = tools.queryGroups(empty(GroupQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).groupId(any());
            verify(query, never()).groupName(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            GroupQueryDto dto = new GroupQueryDto(
                    "managers", List.of("managers", "admins"),
                    "Managers", "Man%",
                    "SYSTEM", "john", "acme"
            );

            tools.queryGroups(dto, null);

            verify(query).groupId("managers");
            verify(query).groupIdIn("managers", "admins");
            verify(query).groupName("Managers");
            verify(query).groupNameLike("Man%");
            verify(query).groupType("SYSTEM");
            verify(query).groupMember("john");
            verify(query).memberOfTenant("acme");
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            GroupQueryDto dto = new GroupQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null, null
            );

            tools.queryGroups(dto, null);

            verify(query, never()).groupId(any());
            verify(query, never()).groupIdIn(any(String[].class));
            verify(query, never()).memberOfTenant(any());
        }

        @Test
        void resultMapping() {
            Group group = mock(Group.class);
            when(group.getId()).thenReturn("managers");
            when(group.getName()).thenReturn("Managers");
            when(group.getType()).thenReturn("SYSTEM");
            when(query.list()).thenReturn(List.of(group));

            List<GroupResultDto> result = tools.queryGroups(empty(GroupQueryDto.class), null);

            assertEquals(1, result.size());
            GroupResultDto r = result.getFirst();
            assertEquals("managers", r.id());
            assertEquals("Managers", r.name());
            assertEquals("SYSTEM", r.type());
        }
    }

    @Nested
    class QueryTenants {

        @Mock(answer = Answers.RETURNS_SELF)
        private TenantQuery query;

        @BeforeEach
        void setUp() {
            when(identityService.createTenantQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<TenantResultDto> result = tools.queryTenants(empty(TenantQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).tenantId(any());
            verify(query, never()).tenantName(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            TenantQueryDto dto = new TenantQueryDto(
                    "acme", List.of("acme", "globex"),
                    "Acme Corp", "Acme%",
                    "john", "managers", true
            );

            tools.queryTenants(dto, null);

            verify(query).tenantId("acme");
            verify(query).tenantIdIn("acme", "globex");
            verify(query).tenantName("Acme Corp");
            verify(query).tenantNameLike("Acme%");
            verify(query).userMember("john");
            verify(query).groupMember("managers");
            verify(query).includingGroupsOfUser(true);
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            TenantQueryDto dto = new TenantQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null, false
            );

            tools.queryTenants(dto, null);

            verify(query, never()).tenantId(any());
            verify(query, never()).tenantIdIn(any(String[].class));
            verify(query, never()).includingGroupsOfUser(anyBoolean());
        }

        @Test
        void resultMapping() {
            Tenant tenant = mock(Tenant.class);
            when(tenant.getId()).thenReturn("acme");
            when(tenant.getName()).thenReturn("Acme Corp");
            when(query.list()).thenReturn(List.of(tenant));

            List<TenantResultDto> result = tools.queryTenants(empty(TenantQueryDto.class), null);

            assertEquals(1, result.size());
            TenantResultDto r = result.getFirst();
            assertEquals("acme", r.id());
            assertEquals("Acme Corp", r.name());
        }
    }
}
