# Fluxnova Backward Compatibility Plugin

A Fluxnova Backward Compatibility plugin for the Fluxnova process engine that provides seamless backward compatibility by rewriting Camunda package references to Fluxnova equivalents at runtime during script execution.

---

## Key Features

- **Automated Migration**: Transparently rewrites Camunda references in scripts to Fluxnova equivalents.
- **Dual-Mode Operation**: Supports both Spring Boot auto-configuration and plain Java (non-Spring) environments.
- **Flexible Configuration**: Hierarchical property resolution (Spring, JVM, classpath, built-in defaults).
- **Fail-Open Safety**: Always returns the original script if any error occurs during processing.
- **Defensive Copying**: Prevents external mutation of preprocessor lists.
- **Chaining Support**: Multiple preprocessors and plugins can be safely combined.
- **Robust Null Handling**: Handles null configuration and script values gracefully.

---

## Overview

Implements the `ProcessEnginePlugin` interface and registers one or more `ScriptPreprocessor` instances to perform automated migration of script code by replacing:

- `org.camunda.*` → `org.finos.fluxnova.*` (root package)
- `.camunda.` → `.fluxnova.` (package path, configurable)
- `*Camunda*` → `*Fluxnova*` (class names, configurable, **off by default**)

---

## Configuration

### Property Hierarchy

1. **Spring environment properties** (if running under Spring Boot)
2. **JVM system properties**
3. **Classpath property file** (`script-preprocessor.properties`)
4. **Built-in defaults**

### Example `script-preprocessor.properties`

```properties
# Enable/disable the plugin (default: true)
fluxnova.bpm.plugin.script-preprocessing.enableEnginePlugin=true

# Enable default Camunda-to-Fluxnova rewrite preprocessor (default: true)
fluxnova.bpm.plugin.script-preprocessing.defaultCamundaToFluxnovaPreprocessorEnabled=true

# Rewrite org.camunda → org.finos.fluxnova (default: true)
fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInRootPackage=true

# Rewrite .camunda. → .fluxnova. (default: false)
fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInPackagePath=false

# Rewrite Camunda → Fluxnova in class names (default: false)
fluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInClassName=false
```

> **Note:** Class name rewriting is **off by default**. Enable it by setting the property to `true`.

### Overriding Properties

- **JVM**: `-Dfluxnova.bpm.plugin.script-preprocessing.rewriteCamundaReferencesInClassName=true`
- **Spring**: Set in `application.properties` or `application.yml` as shown in the previous section.

---

## Usage

### Spring Boot (Auto-configuration)

No explicit configuration is required. The plugin is auto-registered if Spring Boot is present.

### Spring XML

Register the plugin and preprocessors as beans in your XML configuration.

### Plain Java

```java
ScriptPreprocessorPlugin plugin = new ScriptPreprocessorPlugin();
plugin.setScriptPreprocessors(List.of(new CamundaToFluxnovaRewritePreprocessor()));
config.getProcessEnginePlugins().add(plugin);
```

---

## Example Transformation

**Input:**
```javascript
var engine = org.camunda.bpm.engine.ProcessEngineProvider.getDefaultProcessEngine();
var task = new org.camunda.bpm.engine.task.TaskQuery();
```

**Output (all rewrites enabled):**
```javascript
var engine = org.finos.fluxnova.bpm.engine.ProcessEngineProvider.getDefaultProcessEngine();
var task = new org.finos.fluxnova.bpm.engine.task.TaskQuery();
```

---

## Preprocessor Chaining & Safety

- Multiple preprocessors can be registered per plugin; order is preserved.
- Multiple plugins append their preprocessors to the engine's chain.
- If any preprocessor fails, the original script is returned and processing continues.

---

## Architecture

| Class                                   | Purpose                                                      |
|-----------------------------------------|--------------------------------------------------------------|
| `ScriptPreprocessorPlugin`              | Registers preprocessors with the engine                      |
| `CamundaToFluxnovaRewritePreprocessor`  | Regex-based rewrite logic; robust fail-open and null-safety  |
| `ScriptingConfigurationProperties`      | Loads and resolves all configuration properties              |
| `ScriptPreprocessorAutoConfiguration`   | Spring Boot auto-configuration                              |
| `ScriptPreprocessorProperties`          | Spring-specific property binding                             |

---

## Logging & Observability

- **DEBUG**: Rewrite statistics (root/package/class replacements)
- **WARN**: Missing config file or invalid property values
- **ERROR**: Processing errors (fail-open)

---

## Build & Compatibility

- **Java**: 21+
- **Spring Boot**: 4.0.5
- **Fluxnova Engine**: 3.0.0-SNAPSHOT
- **Build**: `mvn clean package`
- **Output**: `target/fluxnova-backward-compatibility-plugin-1.0.0-SNAPSHOT.jar`

---

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

---

## Contributing

Please submit issues and pull requests to the [FINOS Fluxnova project](https://github.com/orgs/finos/projects/116).
