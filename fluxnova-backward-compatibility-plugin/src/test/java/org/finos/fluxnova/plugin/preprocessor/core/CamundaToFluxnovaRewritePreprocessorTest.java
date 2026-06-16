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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CamundaToFluxnovaRewritePreprocessor}.
 *
 * <p>Each test constructs the preprocessor with an explicit
 * {@link ScriptingConfigurationProperties} via the programmatic constructor, allowing precise
 * control over which rewrite flags are active. This isolates the rewrite logic from the classpath
 * property-loading path tested in {@link ScriptPreprocessorJavaConfigIT}.
 *
 * <p>Coverage includes:
 * <ul>
 *   <li>Null and empty input handling</li>
 *   <li>Each rewrite mode individually</li>
 *   <li>All rewrite modes combined</li>
 *   <li>Boundary conditions on class-name casing</li>
 *   <li>Explicit fail-open (internal error fallback)</li>
 * </ul>
 *
 * @see CamundaToFluxnovaRewritePreprocessor
 * @see ScriptingConfigurationProperties
 */
@DisplayName("CamundaToFluxnovaRewritePreprocessor")
class CamundaToFluxnovaRewritePreprocessorTest {

  /**
   * Utility to create a mock ScriptPreprocessorRequest with the given script.
   */
  private static ScriptPreprocessorRequest requestWithScript(String script) {
    ScriptPreprocessorRequest request = mock(ScriptPreprocessorRequest.class);
    when(request.getScript()).thenReturn(script);
    return request;
  }

  @Test
  @DisplayName("Returns null when request is null")
  void returnsNullWhenRequestIsNull() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties(true, true, true, true, true));

    assertNull(preprocessor.process(null));
  }

  @Test
  @DisplayName("Returns null when script is null")
  void returnsOriginalWhenScriptIsNull() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties(true, true, true, true, true));

    assertNull(preprocessor.process(requestWithScript(null)));
  }

  @Test
  @DisplayName("Returns original script when script is empty")
  void returnsOriginalWhenScriptIsEmpty() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties(true, true, true, true, true));

    assertEquals("", preprocessor.process(requestWithScript("")));
  }

  @Test
  @DisplayName("Returns original script when no org.camunda references are present")
  void returnsOriginalWhenNoCamundaReferencesPresent() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties(true, true, true, true, true));
    String input = "var value = com.acme.foo.Bar;";

    assertEquals(input, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("Rewrites root package only when only root rewrite is enabled")
  void rewritesRootPackageOnly() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(
            new ScriptingConfigurationProperties(true, true, false, false, true));
    String input = "org.camunda.bpm.camunda.engine.CamundaProcessEngine";
    String expected = "org.finos.fluxnova.bpm.camunda.engine.CamundaProcessEngine";

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("Rewrites package path segments when package-path rewrite is enabled")
  void rewritesPackagePathSegments() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(
            new ScriptingConfigurationProperties(true, true, true, false, true));
    String input = "org.camunda.bpm.camunda.runtime.CamundaTask";
    String expected = "org.finos.fluxnova.bpm.fluxnova.runtime.CamundaTask";

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("Rewrites class name substrings when class-name rewrite is enabled")
  void rewritesClassNameSubstrings() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(
            new ScriptingConfigurationProperties(true, true, false, true, true));
    String input = "org.camunda.bpm.runtime.MyCamundaTask";
    String expected = "org.finos.fluxnova.bpm.runtime.MyFluxnovaTask";

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("Applies all rewrites when all rewrite flags are enabled")
  void appliesAllRewritesWhenEnabled() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(
            new ScriptingConfigurationProperties(true, true, true, true, true));
    String input = "org.camunda.bpm.camunda.runtime.MyCamundaTask";
    String expected = "org.finos.fluxnova.bpm.fluxnova.runtime.MyFluxnovaTask";

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("Rewrites multiple references on one line with mixed rewrite modes")
  void rewritesMultipleReferencesOnOneLineWithMixedModes() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(
            new ScriptingConfigurationProperties(true, true, false, true, true));
    String input = "org.camunda.a.CamundaX + org.camunda.b.camunda.CamundaY";
    // root-package and class-name rewrites are on; package-path rewrite remains off.
    String expected = "org.finos.fluxnova.a.FluxnovaX + org.finos.fluxnova.b.camunda.FluxnovaY";

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("Preserves class name when it does not start with uppercase")
  void doesNotRewriteClassNameThatStartsLowercase() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(
            new ScriptingConfigurationProperties(true, true, false, true, true));
    String input = "org.camunda.bpm.runtime.camundaTask";
    String expected = "org.finos.fluxnova.bpm.runtime.camundaTask";

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("With null config, implementation falls back to default config")
  void returnsOriginalScriptOnInternalError() {
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        new CamundaToFluxnovaRewritePreprocessor(null);
    String input = "org.camunda.bpm.runtime.CamundaTask";
    String expected = "org.finos.fluxnova.bpm.runtime.CamundaTask";

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("Returns original script if replacePackage throws an exception (explicit fail-open)")
  void returnsOriginalScriptIfReplacePackageThrows() {
    CamundaToFluxnovaRewritePreprocessor preprocessor = new CamundaToFluxnovaRewritePreprocessor(new ScriptingConfigurationProperties(true, true, true, true, true)) {
      @Override
      protected String replacePackage(String script) {
        throw new RuntimeException("Simulated failure");
      }
    };
    String input = "org.camunda.bpm.runtime.CamundaTask";

    assertEquals(input, preprocessor.process(requestWithScript(input)));
  }
}
