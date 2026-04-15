package org.finos.fluxnova.ai.mcp.security.engine;

import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("EngineBasicAuthProvider")
class EngineBasicAuthProviderTest {

    @Mock
    private ProcessEngine processEngine;

    @Mock
    private IdentityService identityService;

    private EngineBasicAuthProvider authProvider;

    @BeforeEach
    void setUp() {
        lenient().when(processEngine.getIdentityService()).thenReturn(identityService);
        authProvider = new EngineBasicAuthProvider(processEngine);
    }

    @Nested
    @DisplayName("authenticate()")
    class Authenticate {

        @Test
        @DisplayName("should return authenticated token when credentials are valid")
        void validCredentials_returnsAuthenticatedToken() {
            when(identityService.checkPassword("admin", "secret")).thenReturn(true);
            Authentication input = new UsernamePasswordAuthenticationToken("admin", "secret");

            Authentication result = authProvider.authenticate(input);

            assertTrue(result.isAuthenticated());
            assertEquals("admin", result.getName());
            assertNull(result.getCredentials(), "Password should be erased from token");
        }

        @Test
        @DisplayName("should grant ROLE_MCP_USER authority on successful authentication")
        void validCredentials_grantsRoleMcpUser() {
            when(identityService.checkPassword("admin", "secret")).thenReturn(true);
            Authentication input = new UsernamePasswordAuthenticationToken("admin", "secret");

            Authentication result = authProvider.authenticate(input);

            assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MCP_USER")),
                    "Authenticated user should have ROLE_MCP_USER authority");
            assertEquals(1, result.getAuthorities().size(),
                    "Should only have exactly one authority");
        }

        @Test
        @DisplayName("should throw BadCredentialsException when password is wrong")
        void invalidPassword_throwsBadCredentials() {
            when(identityService.checkPassword("admin", "wrong")).thenReturn(false);
            Authentication input = new UsernamePasswordAuthenticationToken("admin", "wrong");

            BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                    () -> authProvider.authenticate(input));
            assertTrue(ex.getMessage().contains("admin"),
                    "Exception message should contain the username");
        }

        @Test
        @DisplayName("should throw BadCredentialsException when user does not exist")
        void unknownUser_throwsBadCredentials() {
            when(identityService.checkPassword("nobody", "pass")).thenReturn(false);
            Authentication input = new UsernamePasswordAuthenticationToken("nobody", "pass");

            assertThrows(BadCredentialsException.class,
                    () -> authProvider.authenticate(input));
        }

        @Test
        @DisplayName("should pass through engine exceptions during password check")
        void engineException_propagates() {
            when(identityService.checkPassword(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Engine unavailable"));
            Authentication input = new UsernamePasswordAuthenticationToken("admin", "secret");

            assertThrows(RuntimeException.class,
                    () -> authProvider.authenticate(input));
        }

        @Test
        @DisplayName("should delegate password check to engine identity service")
        void authenticate_delegatesToIdentityService() {
            when(identityService.checkPassword("user1", "pass1")).thenReturn(true);
            Authentication input = new UsernamePasswordAuthenticationToken("user1", "pass1");

            authProvider.authenticate(input);

            verify(identityService).checkPassword("user1", "pass1");
        }

        @Test
        @DisplayName("should handle empty password string")
        void emptyPassword_delegatesToEngine() {
            when(identityService.checkPassword("admin", "")).thenReturn(false);
            Authentication input = new UsernamePasswordAuthenticationToken("admin", "");

            assertThrows(BadCredentialsException.class,
                    () -> authProvider.authenticate(input));
        }

        @Test
        @DisplayName("should handle empty username string")
        void emptyUsername_delegatesToEngine() {
            when(identityService.checkPassword("", "secret")).thenReturn(false);
            Authentication input = new UsernamePasswordAuthenticationToken("", "secret");

            assertThrows(BadCredentialsException.class,
                    () -> authProvider.authenticate(input));
        }
    }

    @Nested
    @DisplayName("supports()")
    class Supports {

        @Test
        @DisplayName("should support UsernamePasswordAuthenticationToken")
        void supportsUsernamePasswordToken() {
            assertTrue(authProvider.supports(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("should not support other authentication types")
        void doesNotSupportOtherTokenTypes() {
            assertFalse(authProvider.supports(TestingAuthenticationToken.class));
        }

        @Test
        @DisplayName("should not support raw Authentication interface")
        void doesNotSupportRawAuthenticationInterface() {
            assertFalse(authProvider.supports(Authentication.class));
        }
    }
}
