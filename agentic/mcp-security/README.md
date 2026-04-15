# engine-mcp-security

Security extension for a Fluxnova MCP server.

This module secures MCP endpoints with Spring Security and currently supports only Basic Authentication runtime modes.

The authenticated user is propagated into the Fluxnova process engine identity context. Authorization is enforced via a custom MCP resource type, requiring users to hold the `ACCESS` permission on the `MCP` resource.

## Requirements

- Java 21+
- Fluxnova BPM Engine 1.0.0+
- Spring Boot 3.5+

## Installation

Add the dependency to your Fluxnova Spring Boot application:

```xml
<dependency>
    <groupId>org.finos.fluxnova.ai.mcp</groupId>
    <artifactId>engine-mcp-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Auto-configuration is loaded from:

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

## Endpoint Scope

Security is applied to MCP endpoints under:

- `/mcp/**`
- `/sse/**`

This means that security is still applied whether using SSE or Streamable HTTP.
Other application endpoints are unaffected by this module's filter chains.

### Basic Auth

To use:

- Add Authorization header to connecting payload.
- Add `Basic base64(username:password)` to the header value.

Main behavior:

- Requires HTTP Basic Auth on `/mcp/**` and `/sse/**`.
- Validates credentials using Fluxnova engine `IdentityService.checkPassword(...)`.
- Runs stateless (no HTTP session).
- Enforces `McpPermission.ACCESS` on `McpResource.MCP` (resource type 22) for every request.
- On first startup, grants `CAMUNDA_ADMIN` group `ACCESS` permission by default.

No additional module-specific properties are required.

## Key Components

- `SecurityMcpAutoConfiguration`: Auto-configuration that wires all beans and imports `SecurityConfig`.
- `SecurityConfig`: HTTP Basic filter chain for `/mcp/**` and `/sse/**`.
- `EngineBasicAuthProvider`: Validates Basic Auth credentials against the engine identity service; grants `ROLE_MCP_USER`.
- `EngineAuthenticationContextFilter`: Propagates Spring Security identity into engine auth context and enforces MCP authorization per request.
- `McpSecurityEnginePlugin`: Process engine plugin that registers the MCP permission provider and seeds default admin authorization.
- `McpPermissionProvider`: Resolves `McpPermission` values by name and resource type for the engine authorization framework.
- `McpResource`: Enum declaring the `MCP` resource (type 22).
- `McpPermission`: Enum declaring `NONE` and `ACCESS` permission levels for the MCP resource.
