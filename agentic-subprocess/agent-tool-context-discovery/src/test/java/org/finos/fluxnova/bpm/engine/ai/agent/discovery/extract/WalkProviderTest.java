package org.finos.fluxnova.bpm.engine.ai.agent.discovery.extract;

import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ConstantValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ListValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.MapValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.NullValueProvider;
import org.finos.fluxnova.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.finos.fluxnova.bpm.engine.impl.el.ElValueProvider;
import org.finos.fluxnova.bpm.engine.impl.scripting.ScriptValueProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalkProviderTest {

    private static ElValueProvider elProvider(String expressionText) {
        var expression = mock(org.finos.fluxnova.bpm.engine.impl.el.Expression.class);
        when(expression.getExpressionText()).thenReturn(expressionText);
        return new ElValueProvider(expression);
    }

    @Nested
    class ElValueProviderTests {

        @Test
        void simpleVariable_returnsVariableName() {
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(elProvider("${customerId}"));
            assertEquals(Set.of("customerId"), result);
        }

        @Test
        void dottedPath_returnsRootIdentifier() {
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(
                    elProvider("${customer.profile.email}"));
            assertEquals(Set.of("customer"), result);
        }

        @Test
        void complexExpression_returnsEmpty() {
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(
                    elProvider("${a + b}"));
            assertTrue(result.isEmpty());
        }

        @Test
        void methodCall_returnsEmpty() {
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(
                    elProvider("${svc.findById(id)}"));
            assertTrue(result.isEmpty());
        }

        @Test
        void withWhitespace_returnsVariable() {
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(
                    elProvider("  ${ x }  "));
            assertEquals(Set.of("x"), result);
        }
    }

    @Nested
    class ScriptValueProviderTests {

        @Test
        void scriptProvider_returnsEmpty() {
            ScriptValueProvider script = mock(ScriptValueProvider.class);
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(script);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class ListValueProviderTests {

        @Test
        void emptyList_returnsEmpty() {
            ListValueProvider list = new ListValueProvider(List.of());
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(list);
            assertTrue(result.isEmpty());
        }

        @Test
        void listWithElProviders_extractsFromEach() {
            ListValueProvider list = new ListValueProvider(List.of(
                    elProvider("${alpha}"),
                    elProvider("${beta}")));
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(list);
            assertEquals(Set.of("alpha", "beta"), result);
        }

        @Test
        void listWithMixedProviders_extractsOnlyElVars() {
            ListValueProvider list = new ListValueProvider(List.of(
                    elProvider("${userId}"),
                    new ConstantValueProvider("literal"),
                    elProvider("${roleId}")));
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(list);
            assertEquals(Set.of("userId", "roleId"), result);
        }

        @Test
        void listWithComplexElProvider_returnsEmpty() {
            ListValueProvider list = new ListValueProvider(List.of(
                    elProvider("${a + b}")));
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(list);
            assertTrue(result.isEmpty());
        }

        @Test
        void listWithScriptProvider_returnsEmpty() {
            ScriptValueProvider script = mock(ScriptValueProvider.class);
            ListValueProvider list = new ListValueProvider(List.of(script));
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(list);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class MapValueProviderTests {

        @Test
        void emptyMap_returnsEmpty() {
            MapValueProvider map = new MapValueProvider(new TreeMap<>());
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(map);
            assertTrue(result.isEmpty());
        }

        @Test
        void mapWithElValues_extractsFromValues() {
            TreeMap<ParameterValueProvider, ParameterValueProvider> providerMap = new TreeMap<>();
            providerMap.put(elProvider("id"), elProvider("${orderId}"));
            providerMap.put(elProvider("name"), elProvider("${customerName}"));
            MapValueProvider map = new MapValueProvider(providerMap);

            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(map);
            assertEquals(Set.of("orderId", "customerName"), result);
        }

        @Test
        void mapWithConstantValues_returnsEmpty() {
            TreeMap<ParameterValueProvider, ParameterValueProvider> providerMap = new TreeMap<>();
            providerMap.put(elProvider("key"), new ConstantValueProvider("static"));
            MapValueProvider map = new MapValueProvider(providerMap);

            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(map);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class ConstantValueProviderTests {

        @Test
        void constantProvider_returnsEmpty() {
            ConstantValueProvider constant = new ConstantValueProvider("hello");
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(constant);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class NullValueProviderTests {

        @Test
        void nullProvider_returnsEmpty() {
            NullValueProvider nullProvider = new NullValueProvider();
            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(nullProvider);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class NestedStructures {

        @Test
        void listInsideMap_extractsRecursively() {
            ListValueProvider innerList = new ListValueProvider(List.of(
                    elProvider("${innerVar}")));
            TreeMap<ParameterValueProvider, ParameterValueProvider> providerMap = new TreeMap<>();
            providerMap.put(elProvider("items"), innerList);
            MapValueProvider map = new MapValueProvider(providerMap);

            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(map);
            assertEquals(Set.of("innerVar"), result);
        }

        @Test
        void mapInsideList_extractsRecursively() {
            TreeMap<ParameterValueProvider, ParameterValueProvider> providerMap = new TreeMap<>();
            providerMap.put(elProvider("key"), elProvider("${nestedVal}"));
            MapValueProvider innerMap = new MapValueProvider(providerMap);
            ListValueProvider list = new ListValueProvider(List.of(innerMap));

            Set<String> result = AdHocSubProcessCatalogueBuilder.extractReadsFromValueProvider(list);
            assertEquals(Set.of("nestedVal"), result);
        }
    }
}
