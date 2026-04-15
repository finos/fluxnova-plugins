package org.finos.fluxnova.ai.mcp.security.engine;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Spring Security {@link AuthenticationProvider} that validates HTTP Basic Auth credentials
 * directly against the Fluxnova process engine identity service.
 *
 * <p>Any user account defined in the engine (including those created via the Fluxnova
 * Admin webapp) can authenticate to the MCP server. No separate user store is needed.</p>
 */
public class EngineBasicAuthProvider implements AuthenticationProvider {

  private final ProcessEngine processEngine;

  public EngineBasicAuthProvider(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    boolean valid = processEngine.getIdentityService().checkPassword(username, password);
    if (!valid) {
      throw new BadCredentialsException("Invalid credentials for user: " + username);
    }

    return new UsernamePasswordAuthenticationToken(
        username,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_MCP_USER"))
    );
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
