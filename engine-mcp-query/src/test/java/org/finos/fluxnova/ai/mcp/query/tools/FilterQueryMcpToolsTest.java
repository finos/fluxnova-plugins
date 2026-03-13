package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.FilterResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.FilterQueryDto;
import org.finos.fluxnova.bpm.engine.FilterService;
import org.finos.fluxnova.bpm.engine.filter.Filter;
import org.finos.fluxnova.bpm.engine.filter.FilterQuery;
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
class FilterQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static FilterQueryDto emptyQuery() {
        try {
            return MAPPER.readValue("{}", FilterQueryDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private FilterService filterService;

    private FilterQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new FilterQueryMcpTools(filterService, 200);
    }

    @Nested
    class QueryFilters {

        @Mock(answer = Answers.RETURNS_SELF)
        private FilterQuery query;

        @BeforeEach
        void setUp() {
            when(filterService.createFilterQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<FilterResultDto> result = tools.queryFilters(emptyQuery(), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).filterId(any());
            verify(query, never()).filterResourceType(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            FilterQueryDto dto = new FilterQueryDto(
                    "filter-1", "Task",
                    "My Tasks", "My%",
                    "admin"
            );

            tools.queryFilters(dto, null);

            verify(query).filterId("filter-1");
            verify(query).filterResourceType("Task");
            verify(query).filterName("My Tasks");
            verify(query).filterNameLike("My%");
            verify(query).filterOwner("admin");
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            FilterQueryDto dto = new FilterQueryDto(null, null, null, null, null);

            tools.queryFilters(dto, null);

            verify(query, never()).filterId(any());
            verify(query, never()).filterResourceType(any());
            verify(query, never()).filterName(any());
            verify(query, never()).filterNameLike(any());
            verify(query, never()).filterOwner(any());
        }

        @Test
        void resultMapping() {
            Filter filter = mock(Filter.class);
            when(filter.getId()).thenReturn("filter-1");
            when(filter.getResourceType()).thenReturn("Task");
            when(filter.getName()).thenReturn("My Tasks");
            when(filter.getOwner()).thenReturn("admin");
            when(query.list()).thenReturn(List.of(filter));

            List<FilterResultDto> result = tools.queryFilters(emptyQuery(), null);

            assertEquals(1, result.size());
            FilterResultDto r = result.getFirst();
            assertEquals("filter-1", r.id());
            assertEquals("Task", r.resourceType());
            assertEquals("My Tasks", r.name());
            assertEquals("admin", r.owner());
        }
    }
}
