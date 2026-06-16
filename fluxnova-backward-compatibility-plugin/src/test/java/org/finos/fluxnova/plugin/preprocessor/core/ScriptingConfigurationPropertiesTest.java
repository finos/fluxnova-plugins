/*
 * Copyright 2025 FINOS
 *
 * The source files in this repository are made available under the Apache License Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Fluxnova uses and includes third-party dependencies published under various licenses.
 * By downloading and using Fluxnova artifacts, you agree to their terms and conditions.
 */
package org.finos.fluxnova.plugin.preprocessor.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScriptingConfigurationProperties}.
 *
 * <p>Tests cover all property resolution paths in priority order:
 * <ol>
 *   <li>JVM system property override (highest priority)</li>
 *   <li>Classpath {@code script-preprocessor.properties} file values</li>
 *   <li>Built-in hard-coded defaults (lowest priority)</li>
 * </ol>
 * Also verifies the explicit/programmatic constructor used by Spring and direct instantiation.
 */
@DisplayName("ScriptingConfigurationProperties")
class ScriptingConfigurationPropertiesTest {

    private static final String PREFIX = "fluxnova.bpm.plugin.script-preprocessing.";

    /**
     * Clears any system properties set during a test to avoid cross-test pollution.
     */
    @AfterEach
    void clearSystemProperties() {
        System.clearProperty(PREFIX + "enableEnginePlugin");
        System.clearProperty(PREFIX + "defaultCamundaToFluxnovaPreprocessorEnabled");
        System.clearProperty(PREFIX + "rewriteCamundaReferencesInRootPackage");
        System.clearProperty(PREFIX + "rewriteCamundaReferencesInPackagePath");
        System.clearProperty(PREFIX + "rewriteCamundaReferencesInClassName");
    }

    // -------------------------------------------------------------------------
    // 1. Classpath file defaults (script-preprocessor.properties on classpath)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Classpath file defaults")
    class ClasspathFileDefaults {

        @Test
        @DisplayName("enableEnginePlugin defaults to true from properties file")
        void enableEnginePlugin_defaultsToTrue() {
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertTrue(props.isEnableEnginePlugin());
        }

        @Test
        @DisplayName("defaultCamundaToFluxnovaPreprocessorEnabled defaults to true from properties file")
        void defaultCamundaToFluxnovaPreprocessorEnabled_defaultsToTrue() {
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertTrue(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
        }

        @Test
        @DisplayName("rewriteCamundaReferencesInRootPackage defaults to true from properties file")
        void rewriteRootPackage_defaultsToTrue() {
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertTrue(props.isRewriteCamundaReferencesInPackageRoot());
        }

        @Test
        @DisplayName("rewriteCamundaReferencesInPackagePath defaults to false from properties file")
        void rewritePackagePath_defaultsToFalse() {
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertFalse(props.isRewriteCamundaReferencesInPackagePath());
        }

        @Test
        @DisplayName("rewriteCamundaReferencesInClassName defaults to false from properties file")
        void rewriteClassName_defaultsToFalse() {
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertFalse(props.isRewriteCamundaReferencesInClassName());
        }
    }

    // -------------------------------------------------------------------------
    // 2. JVM system property overrides (highest priority)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("JVM system property overrides")
    class SystemPropertyOverrides {

        @Test
        @DisplayName("System property overrides enableEnginePlugin to false")
        void systemProperty_overridesEnableEnginePlugin_toFalse() {
            System.setProperty(PREFIX + "enableEnginePlugin", "false");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertFalse(props.isEnableEnginePlugin());
        }

        @Test
        @DisplayName("System property overrides enableEnginePlugin to true")
        void systemProperty_overridesEnableEnginePlugin_toTrue() {
            System.setProperty(PREFIX + "enableEnginePlugin", "true");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertTrue(props.isEnableEnginePlugin());
        }

        @Test
        @DisplayName("System property overrides defaultCamundaToFluxnovaPreprocessorEnabled to false")
        void systemProperty_overridesDefaultPreprocessorEnabled_toFalse() {
            System.setProperty(PREFIX + "defaultCamundaToFluxnovaPreprocessorEnabled", "false");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertFalse(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
        }

        @Test
        @DisplayName("System property overrides rewriteCamundaReferencesInRootPackage to false")
        void systemProperty_overridesRewriteRootPackage_toFalse() {
            System.setProperty(PREFIX + "rewriteCamundaReferencesInRootPackage", "false");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertFalse(props.isRewriteCamundaReferencesInPackageRoot());
        }

        @Test
        @DisplayName("System property overrides rewriteCamundaReferencesInPackagePath to true")
        void systemProperty_overridesRewritePackagePath_toTrue() {
            System.setProperty(PREFIX + "rewriteCamundaReferencesInPackagePath", "true");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertTrue(props.isRewriteCamundaReferencesInPackagePath());
        }

        @Test
        @DisplayName("System property overrides rewriteCamundaReferencesInClassName to true")
        void systemProperty_overridesRewriteClassName_toTrue() {
            System.setProperty(PREFIX + "rewriteCamundaReferencesInClassName", "true");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertTrue(props.isRewriteCamundaReferencesInClassName());
        }

        @Test
        @DisplayName("System property value is trimmed before parsing")
        void systemProperty_valueIsTrimmed() {
            System.setProperty(PREFIX + "rewriteCamundaReferencesInClassName", "  true  ");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            assertTrue(props.isRewriteCamundaReferencesInClassName());
        }

        @Test
        @DisplayName("Invalid system property value is treated as false")
        void systemProperty_invalidValue_treatedAsFalse() {
            System.setProperty(PREFIX + "rewriteCamundaReferencesInClassName", "yes");
            ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
            // Boolean.parseBoolean("yes") == false
            assertFalse(props.isRewriteCamundaReferencesInClassName());
        }

        @Test
        @DisplayName("Invalid system property value logs warning and is treated as false")
        void systemProperty_invalidValue_logsWarningAndTreatedAsFalse() {
            System.setProperty(PREFIX + "rewriteCamundaReferencesInClassName", "yes");
            Logger logger = (Logger) LoggerFactory.getLogger(ScriptingConfigurationProperties.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                ScriptingConfigurationProperties props = new ScriptingConfigurationProperties();
                assertFalse(props.isRewriteCamundaReferencesInClassName());

                assertTrue(
                    appender.list.stream().anyMatch(event ->
                        event.getLevel() == Level.WARN
                            && event.getFormattedMessage().contains("Invalid boolean value")
                            && event.getFormattedMessage().contains("rewriteCamundaReferencesInClassName")
                            && event.getFormattedMessage().contains("system property")));
            } finally {
                logger.detachAppender(appender);
                appender.stop();
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. Explicit (programmatic) constructor
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Explicit constructor (programmatic / Spring delegation)")
    class ExplicitConstructor {

        @Test
        @DisplayName("All flags set to true are preserved")
        void allTrue() {
            ScriptingConfigurationProperties props =
                new ScriptingConfigurationProperties(true, true, true, true, true);
            assertTrue(props.isEnableEnginePlugin());
            assertTrue(props.isRewriteCamundaReferencesInPackageRoot());
            assertTrue(props.isRewriteCamundaReferencesInPackagePath());
            assertTrue(props.isRewriteCamundaReferencesInClassName());
            assertTrue(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
        }

        @Test
        @DisplayName("All flags set to false are preserved")
        void allFalse() {
            ScriptingConfigurationProperties props =
                new ScriptingConfigurationProperties(false, false, false, false, false);
            assertFalse(props.isEnableEnginePlugin());
            assertFalse(props.isRewriteCamundaReferencesInPackageRoot());
            assertFalse(props.isRewriteCamundaReferencesInPackagePath());
            assertFalse(props.isRewriteCamundaReferencesInClassName());
            assertFalse(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
        }

        @Test
        @DisplayName("Mixed flags are stored independently")
        void mixedFlags() {
            ScriptingConfigurationProperties props =
                new ScriptingConfigurationProperties(true, true, false, false, true);
            assertTrue(props.isEnableEnginePlugin());
            assertTrue(props.isRewriteCamundaReferencesInPackageRoot());
            assertFalse(props.isRewriteCamundaReferencesInPackagePath());
            assertFalse(props.isRewriteCamundaReferencesInClassName());
            assertTrue(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
        }

        @Test
        @DisplayName("Explicit constructor is not affected by JVM system properties")
        void explicitConstructor_ignoresSystemProperties() {
            System.setProperty(PREFIX + "enableEnginePlugin", "false");
            System.setProperty(PREFIX + "rewriteCamundaReferencesInClassName", "true");
            // Explicit constructor bypasses property resolution entirely
            ScriptingConfigurationProperties props =
                new ScriptingConfigurationProperties(true, true, false, false, true);
            assertTrue(props.isEnableEnginePlugin());
            assertFalse(props.isRewriteCamundaReferencesInClassName());
        }
    }
}

