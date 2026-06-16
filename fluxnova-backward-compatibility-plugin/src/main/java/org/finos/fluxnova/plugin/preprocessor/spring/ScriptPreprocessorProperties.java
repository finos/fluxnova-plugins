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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for the script preprocessor plugin.
 *
 * <p>Bound from the prefix
 * {@code fluxnova.bpm.plugin.script-preprocessing}.
 *
 * <p>These properties are used only when the host application runs with Spring.
 * Default values may be supplied from {@code script-preprocessor.properties} on the classpath.
 */
@ConfigurationProperties(prefix = "fluxnova.bpm.plugin.script-preprocessing")
public class ScriptPreprocessorProperties {

  private boolean enableEnginePlugin;

  /** Enables the default Camunda-to-Fluxnova rewrite preprocessor in Spring environments. */
  private boolean defaultCamundaToFluxnovaPreprocessorEnabled;

  /** Rewrites the root package {@code org.camunda} to {@code org.finos.fluxnova}. */
  private boolean rewriteCamundaReferencesInRootPackage;

  /** Rewrites secondary package path segments from {@code .camunda} to {@code .fluxnova}. */
  private boolean rewriteCamundaReferencesInPackagePath;

  /** Rewrites class name substrings from {@code Camunda} to {@code Fluxnova}. */
  private boolean rewriteCamundaReferencesInClassName;

  public boolean isEnableEnginePlugin() {
    return enableEnginePlugin;
  }

  public void setEnableEnginePlugin(boolean enableEnginePlugin) {
    this.enableEnginePlugin = enableEnginePlugin;
  }

  public boolean isDefaultCamundaToFluxnovaPreprocessorEnabled() {
    return defaultCamundaToFluxnovaPreprocessorEnabled;
  }

  public void setDefaultCamundaToFluxnovaPreprocessorEnabled(boolean defaultCamundaToFluxnovaPreprocessorEnabled) {
    this.defaultCamundaToFluxnovaPreprocessorEnabled = defaultCamundaToFluxnovaPreprocessorEnabled;
  }

  public boolean isRewriteCamundaReferencesInRootPackage() {
    return rewriteCamundaReferencesInRootPackage;
  }

  public void setRewriteCamundaReferencesInRootPackage(boolean rewriteCamundaReferencesInRootPackage) {
    this.rewriteCamundaReferencesInRootPackage = rewriteCamundaReferencesInRootPackage;
  }

  public boolean isRewriteCamundaReferencesInPackagePath() {
    return rewriteCamundaReferencesInPackagePath;
  }

  public void setRewriteCamundaReferencesInPackagePath(boolean rewriteCamundaReferencesInPackagePath) {
    this.rewriteCamundaReferencesInPackagePath = rewriteCamundaReferencesInPackagePath;
  }

  public boolean isRewriteCamundaReferencesInClassName() {
    return rewriteCamundaReferencesInClassName;
  }

  public void setRewriteCamundaReferencesInClassName(boolean rewriteCamundaReferencesInClassName) {
    this.rewriteCamundaReferencesInClassName = rewriteCamundaReferencesInClassName;
  }
}
