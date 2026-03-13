package org.finos.fluxnova.ai.mcp.query.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.ai.mcp.query.model.dto.BatchResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.JobDefinitionResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.JobResultDto;
import org.finos.fluxnova.ai.mcp.query.model.dto.SchemaLogEntryResultDto;
import org.finos.fluxnova.ai.mcp.query.model.query.BatchQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.JobDefinitionQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.JobQueryDto;
import org.finos.fluxnova.ai.mcp.query.model.query.SchemaLogQueryDto;
import org.finos.fluxnova.bpm.engine.ManagementService;
import org.finos.fluxnova.bpm.engine.batch.Batch;
import org.finos.fluxnova.bpm.engine.batch.BatchQuery;
import org.finos.fluxnova.bpm.engine.management.JobDefinition;
import org.finos.fluxnova.bpm.engine.management.JobDefinitionQuery;
import org.finos.fluxnova.bpm.engine.management.SchemaLogEntry;
import org.finos.fluxnova.bpm.engine.management.SchemaLogQuery;
import org.finos.fluxnova.bpm.engine.runtime.Job;
import org.finos.fluxnova.bpm.engine.runtime.JobQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagementQueryMcpToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static <T> T empty(Class<T> type) {
        try {
            return MAPPER.readValue("{}", type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private ManagementService managementService;

    private ManagementQueryMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new ManagementQueryMcpTools(managementService, 200);
    }

    @Nested
    class QueryJobs {

        @Mock(answer = Answers.RETURNS_SELF)
        private JobQuery query;

        @BeforeEach
        void setUp() {
            when(managementService.createJobQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<JobResultDto> result = tools.queryJobs(empty(JobQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).jobId(any());
            verify(query, never()).processInstanceId(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());
            Date dueBefore = new Date();
            Date dueAfter = new Date();
            Date createdBefore = new Date();
            Date createdAfter = new Date();

            JobQueryDto dto = new JobQueryDto(
                    "job-1", List.of("job-1", "job-2"),
                    "jdef-1", "root-pi-1", "pi-1", List.of("pi-1", "pi-2"),
                    "pd-1", "orderProcess",
                    "exec-1", "timerTask",
                    true, true, true, true, true,
                    dueBefore, dueAfter,
                    createdBefore, createdAfter,
                    5L, 100L,
                    true, "Connection timeout", "timerTask",
                    true, true,
                    List.of("t1"), true, true
            );

            tools.queryJobs(dto, null);

            verify(query).jobId("job-1");
            verify(query).jobIds(Set.of("job-1", "job-2"));
            verify(query).jobDefinitionId("jdef-1");
            verify(query).rootProcessInstanceId("root-pi-1");
            verify(query).processInstanceId("pi-1");
            verify(query).processInstanceIds(Set.of("pi-1", "pi-2"));
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).executionId("exec-1");
            verify(query).activityId("timerTask");
            verify(query).withRetriesLeft();
            verify(query).noRetriesLeft();
            verify(query).executable();
            verify(query).timers();
            verify(query).messages();
            verify(query).duedateLowerThan(dueBefore);
            verify(query).duedateHigherThan(dueAfter);
            verify(query).createdBefore(createdBefore);
            verify(query).createdAfter(createdAfter);
            verify(query).priorityHigherThanOrEquals(5L);
            verify(query).priorityLowerThanOrEquals(100L);
            verify(query).withException();
            verify(query).exceptionMessage("Connection timeout");
            verify(query).failedActivityId("timerTask");
            verify(query).active();
            verify(query).suspended();
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).includeJobsWithoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            JobQueryDto dto = new JobQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, Collections.emptyList(),
                    null, null, null, null,
                    false, null, false, null, false,
                    null, null, null, null,
                    null, null,
                    false, null, null,
                    null, false,
                    Collections.emptyList(), null, false
            );

            tools.queryJobs(dto, null);

            verify(query, never()).withRetriesLeft();
            verify(query, never()).noRetriesLeft();
            verify(query, never()).executable();
            verify(query, never()).timers();
            verify(query, never()).messages();
            verify(query, never()).withException();
            verify(query, never()).active();
            verify(query, never()).suspended();
            verify(query, never()).withoutTenantId();
            verify(query, never()).includeJobsWithoutTenantId();
        }

        @Test
        void resultMapping() {
            Job job = mock(Job.class);
            Date duedate = new Date();
            Date createTime = new Date();
            when(job.getId()).thenReturn("job-1");
            when(job.getDuedate()).thenReturn(duedate);
            when(job.getRootProcessInstanceId()).thenReturn("root-pi-1");
            when(job.getProcessInstanceId()).thenReturn("pi-1");
            when(job.getProcessDefinitionId()).thenReturn("pd-1");
            when(job.getProcessDefinitionKey()).thenReturn("orderProcess");
            when(job.getExecutionId()).thenReturn("exec-1");
            when(job.getJobDefinitionId()).thenReturn("jdef-1");
            when(job.getDeploymentId()).thenReturn("deploy-1");
            when(job.getRetries()).thenReturn(3);
            when(job.getExceptionMessage()).thenReturn(null);
            when(job.getFailedActivityId()).thenReturn(null);
            when(job.isSuspended()).thenReturn(false);
            when(job.getPriority()).thenReturn(0L);
            when(job.getTenantId()).thenReturn("t1");
            when(job.getCreateTime()).thenReturn(createTime);
            when(query.list()).thenReturn(List.of(job));

            List<JobResultDto> result = tools.queryJobs(empty(JobQueryDto.class), null);

            assertEquals(1, result.size());
            JobResultDto r = result.getFirst();
            assertEquals("job-1", r.id());
            assertEquals(duedate, r.duedate());
            assertEquals("pi-1", r.processInstanceId());
            assertEquals(3, r.retries());
            assertFalse(r.suspended());
            assertEquals("t1", r.tenantId());
            assertEquals(createTime, r.createTime());
        }
    }

    @Nested
    class QueryJobDefinitions {

        @Mock(answer = Answers.RETURNS_SELF)
        private JobDefinitionQuery query;

        @BeforeEach
        void setUp() {
            when(managementService.createJobDefinitionQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<JobDefinitionResultDto> result = tools.queryJobDefinitions(empty(JobDefinitionQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).jobDefinitionId(any());
            verify(query, never()).processDefinitionKey(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            JobDefinitionQueryDto dto = new JobDefinitionQueryDto(
                    "jdef-1", List.of("timerStart", "asyncTask"),
                    "pd-1", "orderProcess",
                    "timer-start-event", "R3/PT1H",
                    true, true, true,
                    List.of("t1"), true, true
            );

            tools.queryJobDefinitions(dto, null);

            verify(query).jobDefinitionId("jdef-1");
            verify(query).activityIdIn("timerStart", "asyncTask");
            verify(query).processDefinitionId("pd-1");
            verify(query).processDefinitionKey("orderProcess");
            verify(query).jobType("timer-start-event");
            verify(query).jobConfiguration("R3/PT1H");
            verify(query).active();
            verify(query).suspended();
            verify(query).withOverridingJobPriority();
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).includeJobDefinitionsWithoutTenantId();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            JobDefinitionQueryDto dto = new JobDefinitionQueryDto(
                    null, Collections.emptyList(),
                    null, null, null, null,
                    false, null, false,
                    Collections.emptyList(), null, false
            );

            tools.queryJobDefinitions(dto, null);

            verify(query, never()).active();
            verify(query, never()).suspended();
            verify(query, never()).withOverridingJobPriority();
            verify(query, never()).tenantIdIn(any(String[].class));
            verify(query, never()).withoutTenantId();
            verify(query, never()).includeJobDefinitionsWithoutTenantId();
        }

        @Test
        void resultMapping() {
            JobDefinition jd = mock(JobDefinition.class);
            when(jd.getId()).thenReturn("jdef-1");
            when(jd.getProcessDefinitionId()).thenReturn("pd-1");
            when(jd.getProcessDefinitionKey()).thenReturn("orderProcess");
            when(jd.getJobType()).thenReturn("timer-start-event");
            when(jd.getJobConfiguration()).thenReturn("R3/PT1H");
            when(jd.getActivityId()).thenReturn("timerStart");
            when(jd.isSuspended()).thenReturn(false);
            when(jd.getOverridingJobPriority()).thenReturn(null);
            when(jd.getTenantId()).thenReturn("t1");
            when(query.list()).thenReturn(List.of(jd));

            List<JobDefinitionResultDto> result = tools.queryJobDefinitions(empty(JobDefinitionQueryDto.class), null);

            assertEquals(1, result.size());
            JobDefinitionResultDto r = result.getFirst();
            assertEquals("jdef-1", r.id());
            assertEquals("pd-1", r.processDefinitionId());
            assertEquals("timer-start-event", r.jobType());
            assertFalse(r.suspended());
            assertEquals("t1", r.tenantId());
        }
    }

    @Nested
    class QueryBatches {

        @Mock(answer = Answers.RETURNS_SELF)
        private BatchQuery query;

        @BeforeEach
        void setUp() {
            when(managementService.createBatchQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<BatchResultDto> result = tools.queryBatches(empty(BatchQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).batchId(any());
            verify(query, never()).type(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            BatchQueryDto dto = new BatchQueryDto(
                    "batch-1", "instance-migration",
                    List.of("t1"), true, true, true
            );

            tools.queryBatches(dto, null);

            verify(query).batchId("batch-1");
            verify(query).type("instance-migration");
            verify(query).tenantIdIn("t1");
            verify(query).withoutTenantId();
            verify(query).active();
            verify(query).suspended();
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            BatchQueryDto dto = new BatchQueryDto(
                    null, null, Collections.emptyList(), null, false, false
            );

            tools.queryBatches(dto, null);

            verify(query, never()).tenantIdIn(any(String[].class));
            verify(query, never()).withoutTenantId();
            verify(query, never()).active();
            verify(query, never()).suspended();
        }

        @Test
        void resultMapping() {
            Batch batch = mock(Batch.class);
            Date startTime = new Date();
            Date execStartTime = new Date();
            when(batch.getId()).thenReturn("batch-1");
            when(batch.getType()).thenReturn("instance-migration");
            when(batch.getTotalJobs()).thenReturn(100);
            when(batch.getJobsCreated()).thenReturn(50);
            when(batch.getBatchJobsPerSeed()).thenReturn(10);
            when(batch.getInvocationsPerBatchJob()).thenReturn(1);
            when(batch.getSeedJobDefinitionId()).thenReturn("seed-jdef-1");
            when(batch.getMonitorJobDefinitionId()).thenReturn("monitor-jdef-1");
            when(batch.getBatchJobDefinitionId()).thenReturn("batch-jdef-1");
            when(batch.getTenantId()).thenReturn("t1");
            when(batch.getCreateUserId()).thenReturn("admin");
            when(batch.isSuspended()).thenReturn(false);
            when(batch.getStartTime()).thenReturn(startTime);
            when(batch.getExecutionStartTime()).thenReturn(execStartTime);
            when(query.list()).thenReturn(List.of(batch));

            List<BatchResultDto> result = tools.queryBatches(empty(BatchQueryDto.class), null);

            assertEquals(1, result.size());
            BatchResultDto r = result.getFirst();
            assertEquals("batch-1", r.id());
            assertEquals("instance-migration", r.type());
            assertEquals(100, r.totalJobs());
            assertEquals("admin", r.createUserId());
            assertFalse(r.suspended());
        }
    }

    @Nested
    class QuerySchemaLog {

        @Mock(answer = Answers.RETURNS_SELF)
        private SchemaLogQuery query;

        @BeforeEach
        void setUp() {
            when(managementService.createSchemaLogQuery()).thenReturn(query);
        }

        @Test
        void emptyDto_callsListWithNoFilters() {
            when(query.list()).thenReturn(Collections.emptyList());

            List<SchemaLogEntryResultDto> result = tools.querySchemaLog(empty(SchemaLogQueryDto.class), null);

            assertTrue(result.isEmpty());
            verify(query).list();
            verify(query, never()).version(any());
        }

        @Test
        void allFiltersApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            SchemaLogQueryDto dto = new SchemaLogQueryDto("7.19.0");

            tools.querySchemaLog(dto, null);

            verify(query).version("7.19.0");
        }

        @Test
        void booleanFalseAndNull_notApplied() {
            when(query.list()).thenReturn(Collections.emptyList());

            SchemaLogQueryDto dto = new SchemaLogQueryDto(null);

            tools.querySchemaLog(dto, null);

            verify(query, never()).version(any());
        }

        @Test
        void resultMapping() {
            SchemaLogEntry entry = mock(SchemaLogEntry.class);
            Date timestamp = new Date();
            when(entry.getId()).thenReturn("1");
            when(entry.getTimestamp()).thenReturn(timestamp);
            when(entry.getVersion()).thenReturn("7.19.0");
            when(query.list()).thenReturn(List.of(entry));

            List<SchemaLogEntryResultDto> result = tools.querySchemaLog(empty(SchemaLogQueryDto.class), null);

            assertEquals(1, result.size());
            SchemaLogEntryResultDto r = result.getFirst();
            assertEquals("1", r.id());
            assertEquals(timestamp, r.timestamp());
            assertEquals("7.19.0", r.version());
        }
    }
}
