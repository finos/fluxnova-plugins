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
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor;
import org.finos.fluxnova.plugin.preprocessor.core.ScriptingConfigurationProperties;

/**
 * A {@link ProcessEnginePlugin} that registers one or more {@link ScriptPreprocessor}s with the
 * Fluxnova process engine at startup.
 *
 * <p>Preprocessors are appended to any preprocessors already configured on the engine, so multiple
 * plugins can co-exist without overwriting each other's contributions.
 *
 * <p><strong>Configuration:</strong>
 *
 * <p>Behavior is controlled by properties in {@code script-preprocessor.properties}:
 * <ul>
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin} - master gate; if
 *       {@code false}, plugin returns immediately.</li>
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.defaultCamundaToFluxnovaPreprocessorEnabled}
 *       - enables registration of the default Camunda-to-Fluxnova preprocessor instance.</li>
 *   <li>Additional rewrite flags control behavior of the default preprocessor.</li>
 * </ul>
 *
 * <p>Properties can be overridden via JVM system properties or Spring environment properties.
 *
 * <p><strong>Usage (bpm.cfg.xml / programmatic):</strong>
 *
 * <pre>
 * ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
 * plugin.setScriptPreprocessors(List.of(new CamundaToFluxnovaRewritePreprocessor()));
 * config.getProcessEnginePlugins().add(plugin);
 * </pre>
 *
 * @since 1.0.0
 */
public class ScriptPreprocessorPlugin implements ProcessEnginePlugin {

  /** The list of script preprocessors to register with the engine. May be {@code null}. */
  private List<ScriptPreprocessor> scriptPreprocessors;

  private Boolean enableDefaultCamundaToFluxnovaPreprocessor;

  /**
   * Registers the configured {@link ScriptPreprocessor}s with the engine before initialization.
   *
   * <p>Checks the master property
   * {@code fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin}; if disabled, returns
   * immediately. Then evaluates the default-preprocessor flag and any user-provided
   * preprocessors, and registers the resulting chain with the engine.
   *
   * @param config the engine configuration being initialized
   */
  @Override
  public void preInit(ProcessEngineConfigurationImpl config) {
    ScriptingConfigurationProperties properties = new ScriptingConfigurationProperties();
    if (!properties.isEnableEnginePlugin()) {
      return;
    }
    boolean enableDefaultPreprocessor =
        enableDefaultCamundaToFluxnovaPreprocessor != null
            ? enableDefaultCamundaToFluxnovaPreprocessor
            : properties.isDefaultCamundaToFluxnovaPreprocessorEnabled();

    List<ScriptPreprocessor> allPreprocessors = new ArrayList<>();
    if (enableDefaultPreprocessor) {
      allPreprocessors.add(new CamundaToFluxnovaRewritePreprocessor());
    }
    if (scriptPreprocessors != null && !scriptPreprocessors.isEmpty()) {
      allPreprocessors.addAll(scriptPreprocessors);
    }
    if (allPreprocessors.isEmpty()) {
      return;
    }
    config.setEnableScriptPreprocessing(true);
    List<ScriptPreprocessor> existingPreprocessors = config.getScriptPreprocessors();
    if (existingPreprocessors == null || existingPreprocessors.isEmpty()) {
      config.setScriptPreprocessors(allPreprocessors);
    } else {
      existingPreprocessors.addAll(allPreprocessors);
    }
  }

  /**
   * Called after engine initialisation. No action required for this plugin.
   *
   * @param config the engine configuration after initialisation
   */
  @Override
  public void postInit(ProcessEngineConfigurationImpl config) {
    // no-op
  }

  /**
   * Called after the process engine has been built. No action required for this plugin.
   *
   * @param processEngine the fully built process engine
   */
  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {
    // no-op
  }

  /**
   * Sets the {@link ScriptPreprocessor}s to register with the engine.
   *
   * @param scriptPreprocessors the preprocessors to register; may be {@code null} or empty
   */
  public void setScriptPreprocessors(List<ScriptPreprocessor> scriptPreprocessors) {
    this.scriptPreprocessors =
        scriptPreprocessors != null ? new ArrayList<>(scriptPreprocessors) : null;
  }

  /**
   * Overrides the default-preprocessor toggle for this plugin instance.
   *
   * <p>When set, this value takes precedence over
   * {@code fluxnova.bpm.plugin.script-preprocessing.defaultCamundaToFluxnovaPreprocessorEnabled}
   * loaded from properties.
   *
   * @param enable whether to register the default Camunda-to-Fluxnova preprocessor
   */
  public void setEnableDefaultCamundaToFluxnovaPreprocessor(boolean enable) {
    this.enableDefaultCamundaToFluxnovaPreprocessor = enable;
  }

}