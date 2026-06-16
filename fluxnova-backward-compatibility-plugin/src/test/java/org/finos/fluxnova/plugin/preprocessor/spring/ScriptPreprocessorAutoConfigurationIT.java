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

import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessorRequest;
import org.finos.fluxnova.plugin.ScriptPreprocessorPlugin;
import org.finos.fluxnova.plugin.preprocessor.core.CamundaToFluxnovaRewritePreprocessor;
import org.finos.fluxnova.plugin.preprocessor.core.ScriptingConfigurationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link ScriptPreprocessorAutoConfiguration} using a real Spring Boot application context.
 *
 * <p>Each test starts a Spring Boot context to verify auto-configuration via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} discovery.
 *
 * <p>Coverage includes:
 * <ul>
 *   <li>Auto-configuration discovery and default bean registration</li>
 *   <li>End-to-end preprocessor output with classpath defaults and JVM system property overrides</li>
 *   <li>Negative gate: plugin and preprocessor beans absent when {@code enableEnginePlugin=false}</li>
 *   <li>User-defined bean backoff for both {@link ScriptPreprocessorPlugin} and {@link CamundaToFluxnovaRewritePreprocessor}</li>
 *   <li>BPM variable binding safety: plain {@code camunda}/{@code Camunda} identifiers and engine-injected variables are not rewritten</li>
 * </ul>
 *
 * <p>For property binding assertions without a full Boot context, see {@code ScriptPreprocessorAutoConfigurationTest}.
 *
 * @see ScriptPreprocessorAutoConfiguration
 * @see ScriptPreprocessorProperties
 */
@DisplayName("ScriptPreprocessorAutoConfiguration integration")
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ScriptPreprocessorAutoConfigurationIT {

  private static ConfigurableApplicationContext sharedDefaultContext;

  /**
   * Utility to create a mock ScriptPreprocessorRequest with the given script.
   */
  private static ScriptPreprocessorRequest requestWithScript(String script) {
    ScriptPreprocessorRequest request = mock(ScriptPreprocessorRequest.class);
    when(request.getScript()).thenReturn(script);
    return request;
  }

  /**
   * Starts a Spring Boot application context with the given sources and properties.
   */
  private static ConfigurableApplicationContext runContext(Class<?>[] sources, String... properties) {
    return new SpringApplicationBuilder(sources)
        .web(WebApplicationType.NONE)
        .properties(properties)
        .run();
  }

  @BeforeAll
  static void startSharedDefaultContext() {
    sharedDefaultContext = runContext(new Class<?>[] {IntegrationTestApplication.class});
  }

  @AfterAll
  static void stopSharedDefaultContext() {
    if (sharedDefaultContext != null) {
      sharedDefaultContext.close();
    }
  }

  private static ConfigurableApplicationContext defaultContext() {
    return sharedDefaultContext;
  }

  @Test
  @DisplayName("Auto-configuration imports register default beans and bind classpath defaults")
  void autoConfigurationImportsRegisterDefaultBeansAndBindClasspathDefaults() {
    ConfigurableApplicationContext context = defaultContext();
    assertEquals(1, context.getBeansOfType(ScriptPreprocessorProperties.class).size());
    assertEquals(1, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
    assertEquals(1, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());

    ScriptPreprocessorProperties props = context.getBean(ScriptPreprocessorProperties.class);
    assertTrue(props.isEnableEnginePlugin());
    assertTrue(props.isDefaultCamundaToFluxnovaPreprocessorEnabled());
    assertTrue(props.isRewriteCamundaReferencesInRootPackage());
    assertFalse(props.isRewriteCamundaReferencesInPackagePath());
    assertFalse(props.isRewriteCamundaReferencesInClassName());
  }

  @Test
  @DisplayName("Default preprocessor rewrites root package only with classpath defaults")
  void defaultPreprocessorRewritesRootPackageOnlyWithClasspathDefaults() {
    ConfigurableApplicationContext context = defaultContext();
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        context.getBean(CamundaToFluxnovaRewritePreprocessor.class);
    // root package rewritten; secondary package path and class name left untouched (defaults)
    assertEquals(
        "org.finos.fluxnova.bpm.camunda.engine.CamundaProcessEngine",
        preprocessor.process(requestWithScript("org.camunda.bpm.camunda.engine.CamundaProcessEngine")));
  }

  @Test
  @DisplayName("Default preprocessor rewrites Camunda type references but preserves BPM-injected variable names")
  void defaultPreprocessorRewritesCamundaTypesButPreservesBpmInjectedVariableNames() {
    ConfigurableApplicationContext context = defaultContext();
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        context.getBean(CamundaToFluxnovaRewritePreprocessor.class);

    // Simulates a realistic Groovy BPM script:
    // - 'execution' and 'task' are BPM-injected variable bindings: must NOT be rewritten
    // - Camunda type references in casts and declarations: must be rewritten
    String input = String.join("\n",
        "import org.camunda.bpm.engine.delegate.DelegateExecution",
        "org.camunda.bpm.engine.delegate.DelegateExecution delegateExec = (org.camunda.bpm.engine.delegate.DelegateExecution) execution",
        "org.camunda.bpm.engine.RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService()",
        "runtimeService.startProcessInstanceByKey(execution.getVariable('processKey') as String)",
        "def taskName = task.getName()"
    );

    // Camunda type references are rewritten; BPM variable names (execution, task) are untouched
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
  @DisplayName("Variable names 'camunda' and 'Camunda' are preserved when they are plain identifiers, not package references")
  void variableNamedCamundaOrCamundaIsNotRewritten() {
    ConfigurableApplicationContext context = defaultContext();
    CamundaToFluxnovaRewritePreprocessor preprocessor =
        context.getBean(CamundaToFluxnovaRewritePreprocessor.class);

    // 'camunda' and 'Camunda' appear as variable names / call targets / string literals —
    // none of these start with 'org.' so the root package pattern must not match them.
    String input = String.join("\n",
        "def camunda = execution.getVariable('camundaService')",
        "org.camunda.bpm.engine.ProcessEngine camundaEngine = camunda.getProcessEngine()",
        "Camunda.configure(camundaEngine)",
        "execution.setVariable('camundaResult', camunda.getStatus())"
    );

    // Only the fully-qualified org.camunda type reference on line 2 is rewritten.
    // Variable names (camunda, camundaEngine), the 'Camunda' call, and string literals are untouched.
    String expected = String.join("\n",
        "def camunda = execution.getVariable('camundaService')",
        "org.finos.fluxnova.bpm.engine.ProcessEngine camundaEngine = camunda.getProcessEngine()",
        "Camunda.configure(camundaEngine)",
        "execution.setVariable('camundaResult', camunda.getStatus())"
    );

    assertEquals(expected, preprocessor.process(requestWithScript(input)));
  }

  @Test
  @DisplayName("JVM system property overrides are wired into the default preprocessor bean")
  void jvmSystemPropertyOverridesAreWiredIntoDefaultPreprocessorBean() {
    String packagePathKey =
        "fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInPackagePath";
    String classNameKey =
        "fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInClassName";
    // Intentionally uses JVM system properties to override values through Spring Environment.
    System.setProperty(packagePathKey, "true");
    System.setProperty(classNameKey, "true");
    try (ConfigurableApplicationContext context =
        runContext(new Class<?>[] {IntegrationTestApplication.class})) {
      ScriptPreprocessorProperties props = context.getBean(ScriptPreprocessorProperties.class);
      assertTrue(props.isRewriteCamundaReferencesInPackagePath());
      assertTrue(props.isRewriteCamundaReferencesInClassName());

      CamundaToFluxnovaRewritePreprocessor preprocessor =
          context.getBean(CamundaToFluxnovaRewritePreprocessor.class);

      String input = "org.camunda.bpm.camunda.runtime.MyCamundaTask";
      String expected = "org.finos.fluxnova.bpm.fluxnova.runtime.MyFluxnovaTask";

      assertEquals(expected, preprocessor.process(requestWithScript(input)));
    } finally {
      System.clearProperty(packagePathKey);
      System.clearProperty(classNameKey);
    }
  }

  @Test
  @DisplayName("enableEnginePlugin=false prevents creation of the auto-configured plugin and preprocessor beans")
  void enableEnginePluginFalsePreventsAutoConfiguredPluginAndPreprocessorBeans() {
    String key = "fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin";
    System.setProperty(key, "false");
    try (ConfigurableApplicationContext context =
        runContext(new Class<?>[] {IntegrationTestApplication.class})) {
      assertEquals(1, context.getBeansOfType(ScriptPreprocessorProperties.class).size());

      ScriptPreprocessorProperties props = context.getBean(ScriptPreprocessorProperties.class);
      assertFalse(props.isEnableEnginePlugin());

      assertEquals(0, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
      assertEquals(0, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());
    } finally {
      System.clearProperty(key);
    }
  }

  @Test
  @DisplayName("User-defined ScriptPreprocessorPlugin backs off the auto-configured plugin bean")
  void userDefinedScriptPreprocessorPluginBacksOffAutoConfiguredPluginBean() {
    try (ConfigurableApplicationContext context =
        runContext(new Class<?>[] {IntegrationTestApplication.class, CustomPluginConfiguration.class})) {
      assertEquals(1, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
      assertSame(
          context.getBean("customScriptPreprocessorPlugin"),
          context.getBean(ScriptPreprocessorPlugin.class));

      assertEquals(1, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());
    }
  }

  @Test
  @DisplayName("User-defined CamundaToFluxnovaRewritePreprocessor backs off the default preprocessor bean")
  void userDefinedCamundaToFluxnovaRewritePreprocessorBacksOffDefaultBean() {
    try (ConfigurableApplicationContext context =
        runContext(
            new Class<?>[] {IntegrationTestApplication.class, CustomDefaultPreprocessorConfiguration.class})) {
      assertEquals(1, context.getBeansOfType(CamundaToFluxnovaRewritePreprocessor.class).size());
      CamundaToFluxnovaRewritePreprocessor preprocessor =
          context.getBean(CamundaToFluxnovaRewritePreprocessor.class);

      assertSame(
          context.getBean("customCamundaToFluxnovaRewritePreprocessor"),
          preprocessor);
      assertEquals(
          "org.camunda.bpm.runtime.MyFluxnovaTask",
          preprocessor.process(requestWithScript("org.camunda.bpm.runtime.MyCamundaTask")));
      assertEquals(1, context.getBeansOfType(ScriptPreprocessorPlugin.class).size());
    }
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class IntegrationTestApplication {}

  @Configuration(proxyBeanMethods = false)
  static class CustomPluginConfiguration {

    @Bean
    ScriptPreprocessorPlugin customScriptPreprocessorPlugin() {
      ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
      plugin.setEnableDefaultCamundaToFluxnovaPreprocessor(false);
      return plugin;
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomDefaultPreprocessorConfiguration {

    @Bean
    CamundaToFluxnovaRewritePreprocessor customCamundaToFluxnovaRewritePreprocessor() {
      return new CamundaToFluxnovaRewritePreprocessor(
          new ScriptingConfigurationProperties(true, false, false, true, true));
    }
  }
}
