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
package org.finos.fluxnova.plugin;

import java.util.ArrayList;
import java.util.List;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScriptPreprocessorPlugin} in plain Java (non-Spring) startup mode.
 *
 * <p>All tests invoke {@link ScriptPreprocessorPlugin#preInit} directly with a mocked
 * {@link ProcessEngineConfigurationImpl}, using JVM system properties to control
 * {@link org.finos.fluxnova.plugin.preprocessor.core.ScriptingConfigurationProperties} resolution.
 *
 * <p>Coverage is organized into four nested groups:
 * <ul>
 *   <li><b>Master gate</b>: Verifies that {@code enableEnginePlugin=false} short-circuits
 *       {@code preInit} before any engine configuration is touched.</li>
 *   <li><b>Default preprocessor registration</b>: Verifies that
 *       {@link CamundaToFluxnovaRewritePreprocessor} is registered when the default flag is enabled,
 *       and skipped when disabled via the setter override.</li>
 *   <li><b>Chaining and ordering</b>: Verifies that new preprocessors are appended to any existing
 *       preprocessors on the engine config, and that the internal list is protected against external mutation.</li>
 *   <li><b>Null and empty handling</b>: Verifies no-registration behavior when neither the default
 *       preprocessor nor any custom preprocessors are active.</li>
 * </ul>
 *
 * @see ScriptPreprocessorPlugin
 */
@DisplayName("ScriptPreprocessorPlugin - Java startup")
class ScriptPreprocessorPluginTest {

  private static final String PREFIX = "fluxnova.bpm.plugin.script-preprocessing.";

  /**
   * Clears system properties after each test to avoid cross-test pollution.
   */
  @AfterEach
  void clearSystemProperties() {
    System.clearProperty(PREFIX + "enableEnginePlugin");
    System.clearProperty(PREFIX + "defaultCamundaToFluxnovaPreprocessorEnabled");
  }

  @Nested
  @DisplayName("Master gate behavior")
  class MasterGateBehavior {

    @Test
    @DisplayName("preInit returns immediately when enableEnginePlugin=false")
    void preInit_returnsImmediately_whenEnginePluginDisabled() {
      System.setProperty(PREFIX + "enableEnginePlugin", "false");
      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);

      plugin.preInit(config);

      verify(config, never()).setEnableScriptPreprocessing(true);
      verify(config, never()).getScriptPreprocessors();
      verify(config, never()).setScriptPreprocessors(org.mockito.ArgumentMatchers.anyList());
    }
  }

  @Nested
  @DisplayName("Default preprocessor registration")
  class DefaultPreprocessorRegistration {

    @Test
    @DisplayName("Registers default CamundaToFluxnovaRewritePreprocessor when enabled")
    void registersDefaultPreprocessor_whenEnabled() {
      System.setProperty(PREFIX + "enableEnginePlugin", "true");
      System.setProperty(PREFIX + "defaultCamundaToFluxnovaPreprocessorEnabled", "true");

      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
      when(config.getScriptPreprocessors()).thenReturn(null);

      plugin.preInit(config);

      verify(config).setEnableScriptPreprocessing(true);
      ArgumentCaptor<List<ScriptPreprocessor>> captor = ArgumentCaptor.forClass(List.class);
      verify(config).setScriptPreprocessors(captor.capture());
      List<ScriptPreprocessor> registered = captor.getValue();

      assertEquals(1, registered.size());
      assertInstanceOf(CamundaToFluxnovaRewritePreprocessor.class, registered.getFirst());
    }

    @Test
    @DisplayName("Skips default preprocessor when explicitly disabled by setter")
    void skipsDefaultPreprocessor_whenDisabledBySetter() {
      System.setProperty(PREFIX + "enableEnginePlugin", "true");
      System.setProperty(PREFIX + "defaultCamundaToFluxnovaPreprocessorEnabled", "true");

      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      plugin.setEnableDefaultCamundaToFluxnovaPreprocessor(false);
      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
      when(config.getScriptPreprocessors()).thenReturn(null);

      plugin.preInit(config);

      verify(config, never()).setEnableScriptPreprocessing(true);
      verify(config, never()).setScriptPreprocessors(org.mockito.ArgumentMatchers.anyList());
    }
  }

  @Nested
  @DisplayName("Chaining and ordering")
  class ChainingAndOrdering {

    @Test
    @DisplayName("Appends to existing preprocessors instead of replacing")
    void appendsToExistingPreprocessors_inOrder() {
      System.setProperty(PREFIX + "enableEnginePlugin", "true");
      System.setProperty(PREFIX + "defaultCamundaToFluxnovaPreprocessorEnabled", "true");

      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      ScriptPreprocessor custom = mock(ScriptPreprocessor.class);
      plugin.setScriptPreprocessors(List.of(custom));

      ScriptPreprocessor existing = mock(ScriptPreprocessor.class);
      List<ScriptPreprocessor> existingList = new ArrayList<>();
      existingList.add(existing);

      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
      when(config.getScriptPreprocessors()).thenReturn(existingList);

      plugin.preInit(config);

      verify(config).setEnableScriptPreprocessing(true);
      verify(config, never()).setScriptPreprocessors(org.mockito.ArgumentMatchers.anyList());

      assertEquals(3, existingList.size());
      assertSame(existing, existingList.get(0));
      assertInstanceOf(CamundaToFluxnovaRewritePreprocessor.class, existingList.get(1));
      assertSame(custom, existingList.get(2));
    }

    @Test
    @DisplayName("Setter protects against external list mutation")
    void setter_defensiveCopy_protectsFromExternalMutation() {
      System.setProperty(PREFIX + "enableEnginePlugin", "true");
      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      plugin.setEnableDefaultCamundaToFluxnovaPreprocessor(false);

      ScriptPreprocessor custom = mock(ScriptPreprocessor.class);
      List<ScriptPreprocessor> input = new ArrayList<>();
      input.add(custom);
      plugin.setScriptPreprocessors(input);

      // Mutate caller-owned list after passing it to plugin
      input.clear();

      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
      when(config.getScriptPreprocessors()).thenReturn(null);

      plugin.preInit(config);

      ArgumentCaptor<List<ScriptPreprocessor>> captor = ArgumentCaptor.forClass(List.class);
      verify(config).setScriptPreprocessors(captor.capture());
      List<ScriptPreprocessor> registered = captor.getValue();

      assertEquals(1, registered.size());
      assertSame(custom, registered.getFirst());
    }
  }

  @Nested
  @DisplayName("Null and empty handling")
  class NullAndEmptyHandling {

    @Test
    @DisplayName("Null custom list with default disabled results in no preprocessor registration")
    void noRegistration_whenNoDefaultAndNoCustoms() {
      System.setProperty(PREFIX + "enableEnginePlugin", "true");

      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      plugin.setEnableDefaultCamundaToFluxnovaPreprocessor(false);
      plugin.setScriptPreprocessors(null);

      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
      when(config.getScriptPreprocessors()).thenReturn(null);

      plugin.preInit(config);

      verify(config, never()).setEnableScriptPreprocessing(true);
      verify(config, never()).setScriptPreprocessors(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("Empty custom list with default disabled results in no preprocessor registration")
    void noRegistration_whenNoDefaultAndEmptyCustomList() {
      System.setProperty(PREFIX + "enableEnginePlugin", "true");

      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      plugin.setEnableDefaultCamundaToFluxnovaPreprocessor(false);
      plugin.setScriptPreprocessors(List.of());

      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
      when(config.getScriptPreprocessors()).thenReturn(null);

      plugin.preInit(config);

      verify(config, never()).setEnableScriptPreprocessing(true);
      verify(config, never()).setScriptPreprocessors(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("Setting custom preprocessors only registers exactly those when default disabled")
    void registersOnlyCustomPreprocessors_whenDefaultDisabled() {
      System.setProperty(PREFIX + "enableEnginePlugin", "true");

      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      plugin.setEnableDefaultCamundaToFluxnovaPreprocessor(false);

      ScriptPreprocessor customA = mock(ScriptPreprocessor.class);
      ScriptPreprocessor customB = mock(ScriptPreprocessor.class);
      plugin.setScriptPreprocessors(List.of(customA, customB));

      ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
      when(config.getScriptPreprocessors()).thenReturn(null);

      plugin.preInit(config);

      ArgumentCaptor<List<ScriptPreprocessor>> captor = ArgumentCaptor.forClass(List.class);
      verify(config).setScriptPreprocessors(captor.capture());
      List<ScriptPreprocessor> registered = captor.getValue();

      assertEquals(2, registered.size());
      assertSame(customA, registered.get(0));
      assertSame(customB, registered.get(1));
      assertFalse(registered.get(0) instanceof CamundaToFluxnovaRewritePreprocessor);
      assertFalse(registered.get(1) instanceof CamundaToFluxnovaRewritePreprocessor);
      assertTrue(registered.contains(customA));
      assertTrue(registered.contains(customB));
    }
  }
}

