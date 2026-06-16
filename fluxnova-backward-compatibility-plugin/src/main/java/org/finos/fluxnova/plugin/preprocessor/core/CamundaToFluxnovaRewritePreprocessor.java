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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * Script preprocessor that rewrites Camunda package references to Fluxnova equivalents.
 *
 * <p>This preprocessor should be enabled only after migration readiness has been confirmed.
 *
 * <p>This preprocessor performs automated migration of script code by replacing:
 *
 * <ul>
 *   <li>Package names: {@code org.camunda.*} → {@code org.finos.fluxnova.*}
 *   <li>Package path segments: {@code .camunda.} → {@code .fluxnova.} (configurable)
 *   <li>Class names containing {@code Camunda}: {@code *Camunda*} → {@code *Fluxnova*}
 *       (configurable)
 * </ul>
 *
 * <p><strong>Example transformation:</strong>
 *
 * <pre>
 * // Input:
 * import org.camunda.bpm.engine.ProcessEngine;
 *
 * // Output:
 * import org.finos.fluxnova.bpm.engine.ProcessEngine;
 * </pre>
 *
 * <p>The preprocessor behaviour is controlled via {@code script-preprocessor.properties}:
 *
 * <ul>
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInRootPackage} – Controls root package replacement
 *       ({@code org.camunda} → {@code org.finos.fluxnova})
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInPackagePath} – Controls secondary package segment
 *       replacement ({@code .camunda.} → {@code .fluxnova.})
 *   <li>{@code fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInClassName} – Controls class name substring replacement
 *       ({@code Camunda} → {@code Fluxnova})
 * </ul>
 *
 * @see ScriptingConfigurationProperties
 * @since 1.0.0
 */
public class CamundaToFluxnovaRewritePreprocessor implements ScriptPreprocessor {

  private static final Logger log =
      LoggerFactory.getLogger(CamundaToFluxnovaRewritePreprocessor.class);

  private final ScriptingConfigurationProperties config;

  /**
   * Creates a preprocessor using configuration loaded from {@code script-preprocessor.properties}
   * on the classpath.
   */
  public CamundaToFluxnovaRewritePreprocessor() {
    this(new ScriptingConfigurationProperties());
  }

  /**
   * Creates a preprocessor with an explicit configuration (useful for testing).
   *
   * <p>If {@code config} is {@code null}, the preprocessor falls back to a configuration loaded
   * from {@code script-preprocessor.properties}.
   *
   * @param config the configuration to use; may be {@code null}
   */
  public CamundaToFluxnovaRewritePreprocessor(ScriptingConfigurationProperties config) {
    if (config == null) {
      log.warn(
          "{} : Received null configuration, falling back to classpath defaults.",
          getClass().getSimpleName());
      this.config = new ScriptingConfigurationProperties();
      return;
    }
    this.config = config;
  }

  /**
   * Regex pattern for matching Camunda package references.
   *
   * <p>Pattern breakdown:
   *
   * <ul>
   *   <li>{@code (^|[^A-Za-z0-9_])} - Word boundary (start of line or non-identifier character)
   *   <li>{@code org\.camunda} - Root package literal
   *   <li>{@code (?=\.|[^A-Za-z0-9_]|$)} - Lookahead for package separator, boundary, or end
   *   <li>{@code (?:\.[a-z_][a-z0-9_]*)*} - Optional lowercase package segments
   *   <li>{@code (?:\.[A-Z][A-Za-z0-9_]*)?} - Optional class name (starts with uppercase)
   * </ul>
   */
  private static final Pattern ROOT_PACKAGE_PATTERN =
      Pattern.compile(
          "(^|[^A-Za-z0-9_])(org\\.camunda(?=\\.|[^A-Za-z0-9_]|$)(?:\\.[a-z_][a-z0-9_]*)*(?:\\.[A-Z][A-Za-z0-9_]*)?)");

  /** Package path segment to replace (old). */
  private static final String SECONDARY_PACKAGE_PATTERN_OLD = ".camunda";

  /** Package path segment replacement (new). */
  private static final String SECONDARY_PACKAGE_PATTERN_NEW = ".fluxnova";

  /** Part of the class name to replace (old). */
  private static final String OLD_CLASS_NAME = "Camunda";

  /** Part of the class name to replace (new). */
  private static final String NEW_CLASS_NAME = "Fluxnova";

  /**
   * Processes the script by rewriting Camunda package and class references to Fluxnova.
   *
   * @param request the preprocessing request containing the script to process; may be {@code null}
   * @return the processed script with rewritten package references, or the original if request/script is null/empty
   */
  @Override
  public String process(ScriptPreprocessorRequest request) {
    if (request == null) {
      log.debug("{} : Request is null, skipping preprocessing.", getName());
      return null;
    }
    String script = request.getScript();
    if (script == null || script.isEmpty()) {
      log.debug("{} : Script is null or empty, skipping preprocessing.", getName());
      return script;
    }
    if (!script.contains("org.camunda")) {
      log.debug("{} : No Camunda references found, skipping preprocessing.", getName());
      return script;
    }
    log.debug("{} : STARTING SCRIPT PREPROCESSING", getName());
    try {
      return replacePackage(script);
    } catch (Exception e) {
      log.error("{} : Error during script preprocessing, returning original script.", getName(), e);
      return script;
    }
  }

  /**
   * Returns the unique name of this preprocessor.
   *
   * @return the preprocessor name
   */
  @Override
  public String getName() {
    return "CamundaToFluxnovaRewritePreprocessor";
  }

  /**
   * Replaces all Camunda package references in the script with Fluxnova equivalents.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Matches all fully-qualified Camunda package references
   *   <li>Optionally rewrites the root package from {@code org.camunda} to
   *       {@code org.finos.fluxnova} (if configured)
   *   <li>Optionally replaces secondary package segments in path (if configured)
   *   <li>Optionally replaces class name prefixes (if configured)
   *   <li>Preserves surrounding context and word boundaries
   * </ol>
   *
   * @param script the original script content
   * @return the script with replaced package and class references
   */
  protected String replacePackage(String script) {
    Matcher matcher = ROOT_PACKAGE_PATTERN.matcher(script);
    StringBuilder result = new StringBuilder();
    CamundaToFluxnovaRewriteStats stats = new CamundaToFluxnovaRewriteStats();
    while (matcher.find()) {
      String boundary = matcher.group(1);
      String rewritten = matcher.group(2);
      if (config.isRewriteCamundaReferencesInPackageRoot()) {
        rewritten = rewritten.replaceFirst("^org\\.camunda", "org.finos.fluxnova");
        stats.incrementRootPackageReplacements(1);
      }
      if (config.isRewriteCamundaReferencesInPackagePath()
          && rewritten.contains(SECONDARY_PACKAGE_PATTERN_OLD)) {
        int count = StringUtils.countMatches(rewritten, SECONDARY_PACKAGE_PATTERN_OLD);
        rewritten = rewritten.replace(SECONDARY_PACKAGE_PATTERN_OLD, SECONDARY_PACKAGE_PATTERN_NEW);
        stats.incrementPackagePathReplacements(count);
      }
      if (config.isRewriteCamundaReferencesInClassName()) {
        rewritten = replaceClassNames(rewritten, stats);
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(boundary + rewritten));
    }
    matcher.appendTail(result);
    stats.logStats(this.getName());
    return result.toString();
  }

  /**
   * Replaces occurrences of {@code Camunda} in class names with {@code Fluxnova}.
   *
   * <p>Only replaces within the class name if:
   *
   * <ul>
   *   <li>The fully-qualified name contains a class name (segment after last dot)
   *   <li>The class name starts with an uppercase letter (follows Java naming conventions)
   *   <li>Contains the "Camunda" substring
   * </ul>
   *
   * <p><strong>Examples:</strong>
   *
   * <ul>
   *   <li>{@code org.finos.fluxnova.CamundaTask} → {@code org.finos.fluxnova.FluxnovaTask}
   *   <li>{@code org.finos.fluxnova.MyCamundaHelper} → {@code org.finos.fluxnova.MyFluxnovaHelper}
   *   <li>{@code org.finos.fluxnova.TaskCamundaProcessor} → {@code
   *       org.finos.fluxnova.TaskFluxnovaProcessor}
   * </ul>
   *
   * @param fullyQualifiedName the fully-qualified class name to process
   * @return the fully-qualified name with all "Camunda" occurrences replaced in the class name , or
   *     original if no class name present
   */
  private String replaceClassNames(String fullyQualifiedName, CamundaToFluxnovaRewriteStats stats) {
    int lastDotIndex = fullyQualifiedName.lastIndexOf('.');
    if (lastDotIndex == -1) {
      return fullyQualifiedName; // No class name segment; return as-is.
    }
    String packageName = fullyQualifiedName.substring(0, lastDotIndex + 1);
    String className = fullyQualifiedName.substring(lastDotIndex + 1);
    if (!className.isEmpty()
        && className.contains(OLD_CLASS_NAME)
        && Character.isUpperCase(className.charAt(0))) {
      int count = StringUtils.countMatches(className, OLD_CLASS_NAME);
      className = className.replace(OLD_CLASS_NAME, NEW_CLASS_NAME);
      stats.incrementClassNameReplacements(count);
    }
    return packageName + className;
  }

  private static final class CamundaToFluxnovaRewriteStats {
    private int rootPackageReplacements;
    private int packagePathReplacements;
    private int classNameReplacements;

    void incrementRootPackageReplacements(int count) {
      this.rootPackageReplacements = rootPackageReplacements + count;
    }

    void incrementPackagePathReplacements(int count) {
      this.packagePathReplacements = packagePathReplacements + count;
    }

    void incrementClassNameReplacements(int count) {
      this.classNameReplacements = classNameReplacements + count;
    }

    void logStats(String preprocessorName) {
      log.debug(
          "{} : Preprocessing complete – rootPackageReplacements={}, packagePathReplacements={}, classNameReplacements={}.",
          preprocessorName,
          this.rootPackageReplacements,
          this.packagePathReplacements,
          this.classNameReplacements);
    }
  }
}
