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
package org.finos.fluxnova.plugin.preprocessor.spring;

import org.finos.fluxnova.plugin.ScriptPreprocessorPlugin;
import org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link ScriptPreprocessorAutoConfiguration} Spring auto-configuration.
 *
 * <p>This class is the <strong>canonical {@code @ConfigurationProperties} binding contract</strong>
 * for {@link ScriptPreprocessorProperties}: it verifies that all five properties are correctly
 * bound from the classpath defaults and that they can be overridden via the Spring
 * {@code Environment}.
 *
 * <p>Bean-creation conditions ({@code @ConditionalOnProperty} and
 * {@code @ConditionalOnMissingBean}) are also covered here using
 * {@link ApplicationContextRunner}, which avoids starting a full Spring Boot application context.
 *
 * <p>For end-to-end auto-configuration discovery (via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}) and
 * preprocessor output verification, see
 * {@code ScriptPreprocessorAutoConfigurationIT}.
 */
@DisplayName("ScriptPreprocessorAutoConfiguration canonical ConfigurationProperties binding contract")
class ScriptPreprocessorAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(ScriptPreprocessorAutoConfiguration.class);

  // -------------------------------------------------------------------------
  // ConfigurationProperties binding assertions (canonical contract)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("ConfigurationProperties binding: binds defaults from script-preprocessor.properties")
  void bindsScriptPreprocessorPropertiesDefaults() {
    contextRunner.run(
        context -> {
          assertEquals(1, context.getBeansOfType(ScriptPreprocessorProperties.class).size());
          ScriptPreprocessorProperties props = context.getBean(ScriptPreprocessorProperties.class);

          assertTrue(props.isEnableEnginePlugin());
          assertTrue(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
          assertTrue(props.isRewriteCamundaReferencesInRootPackage());
          assertFalse(props.isRewriteCamundaReferencesInPackagePath());
          assertFalse(props.isRewriteCamundaReferencesInClassName());
        });
  }

  @Test
  @DisplayName("ConfigurationProperties binding: application properties override classpath defaults")
  void bindsScriptPreprocessorPropertiesOverrides() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin=true",
            "fluxnova.bpm.plugin.script-preprocessing.defaultCamundaToFluxnovaPreprocessorEnabled=false",
            "fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInRootPackage=false",
            "fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInPackagePath=true",
            "fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInClassName=true")
        .run(
            context -> {
              ScriptPreprocessorProperties props = context.getBean(ScriptPreprocessorProperties.class);

              assertTrue(props.isEnableEnginePlugin());
              assertFalse(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
              assertFalse(props.isRewriteCamundaReferencesInRootPackage());
              assertTrue(props.isRewriteCamundaReferencesInPackagePath());
              assertTrue(props.isRewriteCamundaReferencesInClassName());
            });
  }

  // -------------------------------------------------------------------------
  // Bean-creation conditions gated by bound properties
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Creates plugin and default preprocessor beans when both enable flags are true")
  void createsPluginAndDefaultPreprocessorBeansWhenEnabled() {
    contextRunner.run(
        context -> {
          assertEquals(1, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
          assertEquals(1, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());
        });
  }

  @Test
  @DisplayName("Does not create plugin or preprocessor bean when enableEnginePlugin is false")
  void doesNotCreateBeansWhenEnginePluginDisabled() {
    contextRunner
        .withPropertyValues("fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin=false")
        .run(
            context -> {
              assertEquals(0, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
              assertEquals(0, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());
            });
  }

  @Test
  @DisplayName("Does not create beans when enableEnginePlugin=false even if default preprocessor is true")
  void doesNotCreateBeansWhenMasterGateDisabledEvenIfDefaultPreprocessorEnabled() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin=false",
            "fluxnova.bpm.plugin.script-preprocessing.defaultCamundaToFluxnovaPreprocessorEnabled=true")
        .run(
            context -> {
              assertEquals(0, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
              assertEquals(0, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());
            });
  }

  @Test
  @DisplayName("Creates plugin but not default preprocessor when default preprocessor flag is false")
  void createsPluginWithoutDefaultPreprocessorWhenDisabled() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin=true",
            "fluxnova.bpm.plugin.script-preprocessing.defaultCamundaToFluxnovaPreprocessorEnabled=false")
        .run(
            context -> {
              assertEquals(1, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
              assertEquals(0, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());
            });
  }
}

