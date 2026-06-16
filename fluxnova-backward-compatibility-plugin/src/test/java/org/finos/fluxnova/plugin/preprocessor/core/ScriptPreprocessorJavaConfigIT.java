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

import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessorRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the script preprocessor in plain Java (non-Spring) environments.
 *
 * <p>Each test exercises the Java-only configuration chain without starting a Spring context:
 * <ol>
 *   <li>{@link ScriptingConfigurationProperties} is instantiated via the no-arg constructor,
 *       loading values from {@code script-preprocessor.properties} on the classpath.</li>
 *   <li>A {@link CamundaToFluxnovaRewritePreprocessor} is created with those loaded properties.</li>
 *   <li>The {@code process()} method is invoked and its output is asserted.</li>
 * </ol>
 *
 * <p>This complements the unit tests in {@link CamundaToFluxnovaRewritePreprocessorTest}, which
 * construct {@link ScriptingConfigurationProperties} via the explicit constructor. Here, properties
 * are resolved from the real classpath file and (optionally) overridden via JVM system properties,
 * exercising the same configuration path used in non-Spring deployments.
 *
 * <p>Coverage includes:
 * <ul>
 *   <li>Classpath default property values: root package rewrite only.</li>
 *   <li>Variable binding safety: BPM-injected variables (e.g., {@code execution}, {@code task}) and plain
 *       {@code camunda}/{@code Camunda} identifiers are not rewritten.</li>
 *   <li>JVM system property overrides enabling each optional rewrite flag individually and in combination.</li>
 *   <li>Disabling the root package rewrite so the script is returned unchanged.</li>
 * </ul>
 *
 * @see ScriptingConfigurationProperties
 * @see CamundaToFluxnovaRewritePreprocessor
 * @see CamundaToFluxnovaRewritePreprocessorTest
 */
@DisplayName("ScriptPreprocessorPlugin Java-path integration")
class ScriptPreprocessorJavaConfigIT {

  private static final String PREFIX = "fluxnova.bpm.plugin.script-preprocessing.";

  /**
   * Utility to create a mock ScriptPreprocessorRequest with the given script.
   */
  private static ScriptPreprocessorRequest requestWithScript(String script) {
    ScriptPreprocessorRequest request = mock(ScriptPreprocessorRequest.class);
    when(request.getScript()).thenReturn(script);
    return request;
  }

  @AfterEach
  void clearSystemProperties() {
    System.clearProperty(PREFIX + "enableEnginePlugin");
    System.clearProperty(PREFIX + "rewriteCamundaReferencesInRootPackage");
    System.clearProperty(PREFIX + "rewriteCamundaReferencesInPackagePath");
    System.clearProperty(PREFIX + "rewriteCamundaReferencesInClassName");
    System.clearProperty(PREFIX + "defaultCamundaToFluxnovaPreprocessorEnabled");
  }

  // -------------------------------------------------------------------------
  // Classpath defaults (no system property overrides)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Classpath defaults")
  class ClasspathDefaults {

    @Test
    @DisplayName("Rewrites root package only; package path and class name remain unchanged")
    void rewritesRootPackageOnly() {
      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      // Only the root package (org.camunda) is rewritten; secondary package and class name are not.
      assertEquals(
          "org.finos.fluxnova.bpm.camunda.engine.CamundaProcessEngine",
          preprocessor.process(
              requestWithScript("org.camunda.bpm.camunda.engine.CamundaProcessEngine")));
    }

    @Test
    @DisplayName("Script without org.camunda references is returned unchanged")
    void scriptWithoutCamundaReferencesIsReturnedUnchanged() {
      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      String input = "com.acme.service.MyTask";
      assertEquals(input, preprocessor.process(requestWithScript(input)));
    }

    @Test
    @DisplayName("Rewrites Camunda type references but preserves BPM-injected variable names")
    void rewritesCamundaTypesButPreservesBpmInjectedVariableNames() {
      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      // Simulates a Groovy BPM script:
      // - 'execution' and 'task' are BPM-injected variable bindings: must NOT be rewritten
      // - Camunda type references in import, cast, and declaration positions: must be rewritten
      String input = String.join("\n",
          "import org.camunda.bpm.engine.delegate.DelegateExecution",
          "org.camunda.bpm.engine.delegate.DelegateExecution delegateExec = (org.camunda.bpm.engine.delegate.DelegateExecution) execution",
          "org.camunda.bpm.engine.RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService()",
          "runtimeService.startProcessInstanceByKey(execution.getVariable('processKey') as String)",
          "def taskName = task.getName()"
      );

      String expected = String.join("\n",
          "import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution",
          "org.finos.fluxnova.bpm.engine.delegate.DelegateExecution delegateExec = (org.finos.fluxnova.bpm.engine.delegate.DelegateExecution) execution",
          "org.finos.fluxnova.bpm.engine.RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService()",
          "runtimeService.startProcessInstanceByKey(execution.getVariable('processKey') as String)",
          "def taskName = task.getName()"
      );

      assertEquals(expected, preprocessor.process(requestWithScript(input)));
    }

    @Test
    @DisplayName("Variable names 'camunda' and 'Camunda' are preserved as plain identifiers")
    void variableNamedCamundaOrCamundaIsNotRewritten() {
      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      // 'camunda' and 'Camunda' appear as variable names, parameter names, or string literals —
      // none of these start with 'org.' so the root package pattern does not match them.
      String input = String.join("\n",
          "def camunda = execution.getVariable('camundaService')",
          "org.camunda.bpm.engine.ProcessEngine camundaEngine = camunda.getProcessEngine()",
          "Camunda.configure(camundaEngine)",
          "execution.setVariable('camundaResult', camunda.getStatus())"
      );

      // Only the fully-qualified org.camunda type reference on line 2 is rewritten.
      // Variable names (camunda, camundaEngine), the 'Camunda' class call, and string literals are untouched.
      String expected = String.join("\n",
          "def camunda = execution.getVariable('camundaService')",
          "org.finos.fluxnova.bpm.engine.ProcessEngine camundaEngine = camunda.getProcessEngine()",
          "Camunda.configure(camundaEngine)",
          "execution.setVariable('camundaResult', camunda.getStatus())"
      );

      assertEquals(expected, preprocessor.process(requestWithScript(input)));
    }
  }

  // -------------------------------------------------------------------------
  // JVM system property overrides
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("JVM system property overrides")
  class SystemPropertyOverrides {

    @Test
    @DisplayName("Enabling rewriteCamundaReferencesInPackagePath rewrites secondary package segments")
    void systemPropertyEnablesPackagePathRewrite() {
      System.setProperty(PREFIX + "rewriteCamundaReferencesInPackagePath", "true");

      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      // Both root package and .camunda. segments are rewritten.
      assertEquals(
          "org.finos.fluxnova.bpm.fluxnova.engine.CamundaProcessEngine",
          preprocessor.process(
              requestWithScript("org.camunda.bpm.camunda.engine.CamundaProcessEngine")));
    }

    @Test
    @DisplayName("Enabling rewriteCamundaReferencesInClassName rewrites class name substrings")
    void systemPropertyEnablesClassNameRewrite() {
      System.setProperty(PREFIX + "rewriteCamundaReferencesInClassName", "true");

      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      // Root package is rewritten; class name 'Camunda' is also rewritten.
      assertEquals(
          "org.finos.fluxnova.bpm.camunda.engine.FluxnovaProcessEngine",
          preprocessor.process(
              requestWithScript("org.camunda.bpm.camunda.engine.CamundaProcessEngine")));
    }

    @Test
    @DisplayName("All rewrite flags enabled produces fully migrated output")
    void allRewriteFlagsEnabledProducesFullyMigratedOutput() {
      System.setProperty(PREFIX + "rewriteCamundaReferencesInPackagePath", "true");
      System.setProperty(PREFIX + "rewriteCamundaReferencesInClassName", "true");

      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      assertEquals(
          "org.finos.fluxnova.bpm.fluxnova.runtime.MyFluxnovaTask",
          preprocessor.process(
              requestWithScript("org.camunda.bpm.camunda.runtime.MyCamundaTask")));
    }

    @Test
    @DisplayName("Disabling rewriteCamundaReferencesInRootPackage leaves root package unchanged")
    void disablingRootPackageRewriteLeavesRootPackageUntouched() {
      System.setProperty(PREFIX + "rewriteCamundaReferencesInRootPackage", "false");

      CamundaToFluxnovaRewritePreprocessor preprocessor =
          new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties());

      // All rewrites disabled; script is returned unchanged.
      String input = "org.camunda.bpm.engine.ProcessEngine";
      assertEquals(input, preprocessor.process(requestWithScript(input)));
    }
  }
}

