package org.finos.fluxnova.bpm.engine.shared.xml;

import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BpmnXmlParserTest {

    private static final String MINIMAL_BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
              <process id="testProcess"/>
            </definitions>
            """;

    private static final String BPMN_WITH_DOCTYPE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE definitions [
                <!ELEMENT definitions ANY >
            ]>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                <process id="testProcess"/>
            </definitions>
            """;

    @Test
    void parse_returnsRootElementWithCorrectTagName() {
        BpmnXmlParser parser = new BpmnXmlParser();
        Parse parse = parser.createParse()
                .sourceInputStream(new ByteArrayInputStream(MINIMAL_BPMN.getBytes(StandardCharsets.UTF_8)))
                .execute();

        Element root = parse.getRootElement();

        assertNotNull(root);
        assertEquals("definitions", root.getTagName());
    }

    @Test
    void parse_navigatesToChildElement() {
        BpmnXmlParser parser = new BpmnXmlParser();
        Parse parse = parser.createParse()
                .sourceInputStream(new ByteArrayInputStream(MINIMAL_BPMN.getBytes(StandardCharsets.UTF_8)))
                .execute();

        Element process = parse.getRootElement().element("process");

        assertNotNull(process);
        assertEquals("testProcess", process.attribute("id"));
    }

    @Test
    void parse_rejectsDoctypeDeclarations() {
        BpmnXmlParser parser = new BpmnXmlParser();

        Exception thrown = assertThrows(Exception.class, () -> parser.createParse()
                .sourceInputStream(new ByteArrayInputStream(BPMN_WITH_DOCTYPE.getBytes(StandardCharsets.UTF_8)))
                .execute());

        assertTrue(thrown.getMessage().contains("DOCTYPE is disallowed"));
    }
}