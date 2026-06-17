package org.finos.fluxnova.ai.mcp.query.tools;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XMLMcpToolsTest {

    @Mock
    private RepositoryService repositoryService;

    private XMLMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new XMLMcpTools(repositoryService);
    }

    private static InputStream streamOf(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    // ========================================================================
    // getProcessModelXml
    // ========================================================================

    @Nested
    class GetProcessModelXml {

        @Test
        void returnsXmlStringFromInputStream() {
            String bpmnXml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"><process id=\"myProcess\"/></definitions>";
            when(repositoryService.getProcessModel("proc-def-1")).thenReturn(streamOf(bpmnXml));

            String result = tools.getProcessModelXml("proc-def-1");

            assertEquals(bpmnXml, result);
            verify(repositoryService).getProcessModel("proc-def-1");
        }

        @Test
        void wrapsIOExceptionAsRuntimeException() throws IOException {
            InputStream failingStream = mock(InputStream.class);
            doThrow(new IOException("disk error")).when(failingStream).readAllBytes();
            when(repositoryService.getProcessModel("proc-def-1")).thenReturn(failingStream);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tools.getProcessModelXml("proc-def-1"));

            assertTrue(ex.getMessage().contains("proc-def-1"));
            assertInstanceOf(IOException.class, ex.getCause());
        }
    }

    // ========================================================================
    // getDecisionModelXml
    // ========================================================================

    @Nested
    class GetDecisionModelXml {

        @Test
        void returnsXmlStringFromInputStream() {
            String dmnXml = "<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\"><decision id=\"myDecision\"/></definitions>";
            when(repositoryService.getDecisionModel("dec-def-1")).thenReturn(streamOf(dmnXml));

            String result = tools.getDecisionModelXml("dec-def-1");

            assertEquals(dmnXml, result);
            verify(repositoryService).getDecisionModel("dec-def-1");
        }

        @Test
        void wrapsIOExceptionAsRuntimeException() throws IOException {
            InputStream failingStream = mock(InputStream.class);
            doThrow(new IOException("disk error")).when(failingStream).readAllBytes();
            when(repositoryService.getDecisionModel("dec-def-1")).thenReturn(failingStream);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tools.getDecisionModelXml("dec-def-1"));

            assertTrue(ex.getMessage().contains("dec-def-1"));
            assertInstanceOf(IOException.class, ex.getCause());
        }
    }

    // ========================================================================
    // getDecisionRequirementsModelXml
    // ========================================================================

    @Nested
    class GetDecisionRequirementsModelXml {

        @Test
        void returnsXmlStringFromInputStream() {
            String drdXml = "<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\"><decisionRequirementsGraph id=\"myDRG\"/></definitions>";
            when(repositoryService.getDecisionRequirementsModel("drg-def-1")).thenReturn(streamOf(drdXml));

            String result = tools.getDecisionRequirementsModelXml("drg-def-1");

            assertEquals(drdXml, result);
            verify(repositoryService).getDecisionRequirementsModel("drg-def-1");
        }

        @Test
        void wrapsIOExceptionAsRuntimeException() throws IOException {
            InputStream failingStream = mock(InputStream.class);
            doThrow(new IOException("disk error")).when(failingStream).readAllBytes();
            when(repositoryService.getDecisionRequirementsModel("drg-def-1")).thenReturn(failingStream);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tools.getDecisionRequirementsModelXml("drg-def-1"));

            assertTrue(ex.getMessage().contains("drg-def-1"));
            assertInstanceOf(IOException.class, ex.getCause());
        }
    }

    // ========================================================================
    // getCaseModelXml
    // ========================================================================

    @Nested
    class GetCaseModelXml {

        @Test
        void returnsXmlStringFromInputStream() {
            String cmmnXml = "<definitions xmlns=\"http://www.omg.org/spec/CMMN/20151109/MODEL\"><case id=\"myCase\"/></definitions>";
            when(repositoryService.getCaseModel("case-def-1")).thenReturn(streamOf(cmmnXml));

            String result = tools.getCaseModelXml("case-def-1");

            assertEquals(cmmnXml, result);
            verify(repositoryService).getCaseModel("case-def-1");
        }

        @Test
        void wrapsIOExceptionAsRuntimeException() throws IOException {
            InputStream failingStream = mock(InputStream.class);
            doThrow(new IOException("disk error")).when(failingStream).readAllBytes();
            when(repositoryService.getCaseModel("case-def-1")).thenReturn(failingStream);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> tools.getCaseModelXml("case-def-1"));

            assertTrue(ex.getMessage().contains("case-def-1"));
            assertInstanceOf(IOException.class, ex.getCause());
        }
    }
}
