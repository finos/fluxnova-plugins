package org.finos.fluxnova.ai.mcp.query.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueryToolsPropertiesTest {

    @Test
    void allServicesEnabledByDefault() {
        var props = new QueryToolsProperties();

        assertTrue(props.getRepository().isEnabled());
        assertTrue(props.getRuntime().isEnabled());
        assertTrue(props.getTask().isEnabled());
        assertTrue(props.getHistory().isEnabled());
        assertTrue(props.getExternalTask().isEnabled());
        assertTrue(props.getAuthorization().isEnabled());
        assertTrue(props.getFilter().isEnabled());
        assertTrue(props.getCaseService().isEnabled());
        assertTrue(props.getIdentity().isEnabled());
        assertTrue(props.getManagement().isEnabled());
        assertTrue(props.getXml().isEnabled());
    }

    @Test
    void excludeSetEmptyByDefault() {
        var props = new QueryToolsProperties();

        assertTrue(props.getExclude().isEmpty());
    }

    @Test
    void serviceToggleCanBeDisabled() {
        var props = new QueryToolsProperties();

        props.getHistory().setEnabled(false);

        assertFalse(props.getHistory().isEnabled());
        assertTrue(props.getRepository().isEnabled());
    }

    @Test
    void excludeSetCanBeConfigured() {
        var props = new QueryToolsProperties();

        props.setExclude(Set.of("querySchemaLog", "queryBatches"));

        assertEquals(Set.of("querySchemaLog", "queryBatches"), props.getExclude());
    }
}
