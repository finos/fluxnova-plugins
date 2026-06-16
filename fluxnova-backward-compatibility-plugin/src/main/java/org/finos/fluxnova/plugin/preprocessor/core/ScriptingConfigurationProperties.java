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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain-Java configuration holder for the Script Preprocessor Plugin.
 *
 * <p>Properties are loaded from {@code script-preprocessor.properties} on the classpath.
 * Each property can be overridden at runtime via a Java system property with the same key.
 *
 * <p><strong>Supported properties:</strong>
 *
 * <ul>
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin}
 *       (default: {@code true})
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.defaultCamundaToFluxnovaPreprocessorEnabled}
 *       (default: {@code true})
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInRootPackage}
 *       (default: {@code true})
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInPackagePath}
 *       (default: {@code false})
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInClassName}
 *       (default: {@code false})
 * </ul>
 *
 * <p>The legacy unprefixed keys are also accepted as a fallback for non-Spring/manual usage.
 *
 * <p>Boolean values must be {@code true} or {@code false} (case-insensitive). Any other non-null
 * value (for example, {@code yes}) is treated as {@code false} to match
 * {@link Boolean#parseBoolean(String)} semantics, and a warning is logged.
 *
 * @since 1.0.0
 */
public final class ScriptingConfigurationProperties {

  private static final Logger log = LoggerFactory.getLogger(ScriptingConfigurationProperties.class);

  private static final String PROPERTIES_FILE = "script-preprocessor.properties";

  private static final String PREFIX = "fluxnova.bpm.plugin.script-preprocessing.";

  private static final String KEY_REWRITE_PACKAGE_ROOT =
      "rewriteCamundaReferencesInRootPackage";
  private static final String KEY_REWRITE_PACKAGE_PATH =
      "rewriteCamundaReferencesInPackagePath";
  private static final String KEY_REWRITE_CLASS_NAME =
      "rewriteCamundaReferencesInClassName";
  private static final String KEY_ENABLE_ENGINE_PLUGIN = "enableEnginePlugin";
  private static final String KEY_ENABLE_CAM_FXN_REWRITE_PLUGIN =
      "defaultCamundaToFluxnovaPreprocessorEnabled";

  private static final String PREFIXED_KEY_ENABLE_ENGINE_PLUGIN = PREFIX + KEY_ENABLE_ENGINE_PLUGIN;
  private static final String PREFIXED_KEY_REWRITE_PACKAGE_ROOT = PREFIX + KEY_REWRITE_PACKAGE_ROOT;
  private static final String PREFIXED_KEY_REWRITE_PACKAGE_PATH = PREFIX + KEY_REWRITE_PACKAGE_PATH;
  private static final String PREFIXED_KEY_REWRITE_CLASS_NAME = PREFIX + KEY_REWRITE_CLASS_NAME;
  private static final String PREFIXED_KEY_ENABLE_CAM_FXN_REWRITE_PLUGIN =
      PREFIX + KEY_ENABLE_CAM_FXN_REWRITE_PLUGIN;

  private final boolean enableEnginePlugin;
  private final boolean rewriteCamundaReferencesInPackageRoot;
  private final boolean rewriteCamundaReferencesInPackagePath;
  private final boolean rewriteCamundaReferencesInClassName;
  private final boolean defaultCamundaToFluxnovaPreprocessorEnabled;

  /**
   * Creates an instance by loading configuration from {@code script-preprocessor.properties} on
   * the classpath. System properties take precedence over file-based values.
   */
  public ScriptingConfigurationProperties() {
    Properties props = loadProperties();
    this.enableEnginePlugin =
        resolveBoolean(props, PREFIXED_KEY_ENABLE_ENGINE_PLUGIN, KEY_ENABLE_ENGINE_PLUGIN, true);
    this.rewriteCamundaReferencesInPackageRoot =
        resolveBoolean(
            props, PREFIXED_KEY_REWRITE_PACKAGE_ROOT, KEY_REWRITE_PACKAGE_ROOT, true);
    this.rewriteCamundaReferencesInPackagePath =
        resolveBoolean(
            props, PREFIXED_KEY_REWRITE_PACKAGE_PATH, KEY_REWRITE_PACKAGE_PATH, false);
    this.rewriteCamundaReferencesInClassName =
        resolveBoolean(
            props, PREFIXED_KEY_REWRITE_CLASS_NAME, KEY_REWRITE_CLASS_NAME, false);
    this.defaultCamundaToFluxnovaPreprocessorEnabled =
        resolveBoolean(props, PREFIXED_KEY_ENABLE_CAM_FXN_REWRITE_PLUGIN,
            KEY_ENABLE_CAM_FXN_REWRITE_PLUGIN, true);

    log.debug(
        "ScriptingConfigurationProperties loaded: enableEnginePlugin={}, rewritePackageRoot={}, rewritePackagePath={}, rewriteClassName={}, camundaToFluxnovaPreprocessorEnabled={}",
        this.enableEnginePlugin,
        this.rewriteCamundaReferencesInPackageRoot,
        this.rewriteCamundaReferencesInPackagePath,
        this.rewriteCamundaReferencesInClassName,
        this.defaultCamundaToFluxnovaPreprocessorEnabled);
  }

  /**
   * Creates an instance with explicit configuration values.
   *
   * <p>Useful for testing or programmatic configuration. Spring auto-configuration also uses this
   * constructor to create instances from bound properties.
   *
   * @param enableEnginePlugin whether the script preprocessor engine plugin should be active
   * @param rewriteCamundaReferencesInPackageRoot whether to rewrite root package names
   *     ({@code org.camunda} -> {@code org.finos.fluxnova})
   * @param rewriteCamundaReferencesInPackagePath whether to rewrite secondary package path segments
   *     ({@code .camunda.} -> {@code .fluxnova.})
   * @param rewriteCamundaReferencesInClassName whether to rewrite class name substrings
   *     ({@code Camunda} -> {@code Fluxnova})
   * @param defaultCamundaToFluxnovaPreprocessorEnabled whether to enable the default
   *     Camunda-to-Fluxnova rewrite preprocessor
   */
  public ScriptingConfigurationProperties(
      boolean enableEnginePlugin,
      boolean rewriteCamundaReferencesInPackageRoot,
      boolean rewriteCamundaReferencesInPackagePath,
      boolean rewriteCamundaReferencesInClassName,
      boolean defaultCamundaToFluxnovaPreprocessorEnabled) {
    this.enableEnginePlugin = enableEnginePlugin;
    this.rewriteCamundaReferencesInPackageRoot = rewriteCamundaReferencesInPackageRoot;
    this.rewriteCamundaReferencesInPackagePath = rewriteCamundaReferencesInPackagePath;
    this.rewriteCamundaReferencesInClassName = rewriteCamundaReferencesInClassName;
    this.defaultCamundaToFluxnovaPreprocessorEnabled =
        defaultCamundaToFluxnovaPreprocessorEnabled;
  }

  /** Returns {@code true} if the script preprocessor engine plugin should be active. */
  public boolean isEnableEnginePlugin() {
    return enableEnginePlugin;
  }

  /**
   * Returns {@code true} if root package names ({@code org.camunda}) should be rewritten to
   * ({@code org.finos.fluxnova}).
   */
  public boolean isRewriteCamundaReferencesInPackageRoot() {
    return rewriteCamundaReferencesInPackageRoot;
  }

  /**
   * Returns {@code true} if secondary package path segments ({@code .camunda.}) should be
   * rewritten to ({@code .fluxnova.}).
   */
  public boolean isRewriteCamundaReferencesInPackagePath() {
    return rewriteCamundaReferencesInPackagePath;
  }

  /**
   * Returns {@code true} if class name substrings ({@code Camunda}) should be rewritten to
   * ({@code Fluxnova}).
   */
  public boolean isRewriteCamundaReferencesInClassName() {
    return rewriteCamundaReferencesInClassName;
  }

  /**
   * Returns {@code true} if the default Camunda-to-Fluxnova rewrite preprocessor is enabled.
   */
  public boolean isDefaultCamundaToFluxnovaPreprocessorEnabled() {
    return defaultCamundaToFluxnovaPreprocessorEnabled;
  }
  
  /**
   * Loads properties from {@code script-preprocessor.properties} on the classpath.
   *
   * <p>If the file is not found or cannot be read, an empty {@link Properties} instance is
   * returned and a warning is logged; callers fall back to built-in defaults via
   * {@link #resolveBoolean}.
   *
   * @return a {@link Properties} instance populated from the file, or empty on failure
   */
  private static Properties loadProperties() {
    Properties props = new Properties();
    try (InputStream in =
        ScriptingConfigurationProperties.class
            .getClassLoader()
            .getResourceAsStream(PROPERTIES_FILE)) {
      if (in == null) {
        log.warn(
            "Properties file '{}' not found on classpath; using built-in defaults.",
            PROPERTIES_FILE);
      } else {
        props.load(in);
        log.debug("Loaded properties from '{}'.", PROPERTIES_FILE);
      }
    } catch (IOException e) {
      log.warn(
          "Failed to load '{}'; using built-in defaults. Cause: {}",
          PROPERTIES_FILE,
          e.getMessage());
    }
    return props;
  }

  /**
   * Resolves a boolean property. System properties take precedence over classpath file values,
   * which in turn take precedence over the supplied {@code defaultValue}.
   *
   * <p>When a value is present but not a valid boolean ({@code true}/{@code false}), a warning is
   * logged and the value is interpreted as {@code false}.
   *
   * @param props        base properties loaded from the classpath file
   * @param preferredKey the preferred property key to look up first
   * @param fallbackKey  the legacy fallback property key to look up if the preferred key is absent
   * @param defaultValue fallback value when the key is absent from both system and file properties
   * @return the resolved boolean value
   */
  private static boolean resolveBoolean(
      Properties props, String preferredKey, String fallbackKey, boolean defaultValue) {
    String systemValue = System.getProperty(preferredKey);
    if (systemValue != null) {
      return parseBooleanWithWarning(systemValue, preferredKey, "system property");
    }
    systemValue = System.getProperty(fallbackKey);
    if (systemValue != null) {
      return parseBooleanWithWarning(systemValue, fallbackKey, "system property");
    }
    String fileValue = props.getProperty(preferredKey);
    if (fileValue != null) {
      return parseBooleanWithWarning(fileValue, preferredKey, "classpath property file");
    }
    fileValue = props.getProperty(fallbackKey);
    if (fileValue != null) {
      return parseBooleanWithWarning(fileValue, fallbackKey, "classpath property file");
    }
    return defaultValue;
  }

  /**
   * Parses a boolean value and warns when the input is non-boolean.
   *
   * <p>Valid values are {@code true} and {@code false} (case-insensitive). Invalid values are
   * parsed as {@code false} to preserve {@link Boolean#parseBoolean(String)} behavior.
   */
  private static boolean parseBooleanWithWarning(String rawValue, String key, String source) {
    String value = rawValue.trim();
    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
      log.warn(
          "Invalid boolean value '{}' for key '{}' from {}; expected true/false. Interpreting as false.",
          rawValue,
          key,
          source);
    }
    return Boolean.parseBoolean(value);
  }
}

