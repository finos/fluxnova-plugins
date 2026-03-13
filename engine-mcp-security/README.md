# engine-mcp-security

Security extension for a Fluxnova MCP server.

This module secures MCP endpoints with Spring Security and currently supports only Basic Authentication runtime modes

The authenticated user is propagated into the Fluxnova process engine identity context so engine-level authorization checks continue to work.

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

No additional module-specific properties are required.

## Key Components

- `SecurityConfig`: HTTP Basic fallback chain for `/mcp/**` and `/sse/**`.
- `EngineBasicAuthProvider`: Validates Basic Auth credentials against the engine identity service.
- `EngineAuthenticationContextFilter`: Propagates Spring Security identity into engine auth context per request.
