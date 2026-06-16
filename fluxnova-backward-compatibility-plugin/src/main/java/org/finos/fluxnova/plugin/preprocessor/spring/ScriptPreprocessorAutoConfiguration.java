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

import java.util.List;
import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.finos.fluxnova.plugin.ScriptPreprocessorPlugin;
import org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor;
import org.finos.fluxnova.plugin.preprocessor.core.ScriptingConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring Boot auto-configuration for the Fluxnova script preprocessor plugin.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * and activated only when the Spring Boot auto-configuration infrastructure is present on the
 * classpath ({@code @ConditionalOnClass}).
 *
 * <p>This configuration class:
 * <ul>
 *   <li>Binds {@link ScriptPreprocessorProperties} from the
 *       {@code fluxnova.bpm.plugin.script-preprocessing.*} namespace.</li>
 *   <li>Provides a {@link org.finos.fluxnova.plugin.ScriptPreprocessorPlugin} bean when
 *       {@code enableEnginePlugin=true} and no user-defined bean of the same type exists.</li>
 *   <li>Provides a {@link org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor}
 *       bean when both {@code enableEnginePlugin=true} and
 *       {@code defaultCamundaToFluxnovaPreprocessorEnabled=true}, and no user-defined bean of the
 *       same type exists.</li>
 * </ul>
 *
 * <p>Default property values are loaded from {@code script-preprocessor.properties} on the
 * classpath and can be overridden via the Spring {@code Environment} (e.g.,
 * {@code application.properties}, system properties, or environment variables).
 *
 * <p>Both beans support user backoff via {@code @ConditionalOnMissingBean}: declare your own
 * {@link org.finos.fluxnova.plugin.ScriptPreprocessorPlugin} or
 * {@link org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor} bean
 * to suppress the auto-configured defaults.
 *
 * @see ScriptPreprocessorProperties
 * @see org.finos.fluxnova.plugin.ScriptPreprocessorPlugin
 * @see org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.boot.autoconfigure.AutoConfiguration")
@EnableConfigurationProperties({ScriptPreprocessorProperties.class})
@PropertySource(value = "classpath:script-preprocessor.properties", ignoreResourceNotFound = true)
public class ScriptPreprocessorAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ScriptPreprocessorPlugin.class)
  @ConditionalOnProperty(
      prefix = "fluxnova.bpm.plugin.script-preprocessing",
      name = "enableEnginePlugin",
      havingValue = "true")
  public ScriptPreprocessorPlugin scriptPreprocessorPlugin(
      List<ScriptPreprocessor> userPreprocessors) {
    ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
    // Prevent duplicate registration of the default preprocessor: it is managed as a separate bean.
    plugin.setEnableDefaultCamundaToFluxnovaPreprocessor(false);
    plugin.setScriptPreprocessors(userPreprocessors);
    return plugin;
  }

  @Bean
  @ConditionalOnMissingBean(
      CamundaToFluxnovaRewritePreprocessor.class)
  @ConditionalOnProperty(
      prefix = "fluxnova.bpm.plugin.script-preprocessing",
      name = {"enableEnginePlugin","defaultCamundaToFluxnovaPreprocessorEnabled"},
      havingValue = "true")
  public CamundaToFluxnovaRewritePreprocessor
      camundaToFluxnovaRewritePreprocessor(ScriptPreprocessorProperties props) {
    return new CamundaToFluxnovaRewritePreprocessor(
        new ScriptingConfigurationProperties(
            props.isEnableEnginePlugin(),
            props.isRewriteCamundaReferencesInRootPackage(),
            props.isRewriteCamundaReferencesInPackagePath(),
            props.isRewriteCamundaReferencesInClassName(),
            props.isDefaultCamundaToFluxnovaPreprocessorEnabled()));
  }
}
