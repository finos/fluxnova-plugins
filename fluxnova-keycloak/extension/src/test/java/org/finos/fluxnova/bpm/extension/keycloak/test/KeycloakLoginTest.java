package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
/**
 * Keycloak login test.
 */
public class KeycloakLoginTest extends AbstractKeycloakIdentityProviderTest {
  
	@Test
	public void testKeycloakLoginSuccess() {
		assertTrue(identityService.checkPassword("camunda@accso.de", "camunda1!"));
	}

	@Test
	public void testKeycloakLoginCapitalization() {
		assertTrue(identityService.checkPassword("Camunda@Accso.de", "camunda1!"));
	}

	@Test
	public void testKeycloakLoginFailure() {
		assertFalse(identityService.checkPassword("camunda@accso.de", "c"));
		assertFalse(identityService.checkPassword("non-existing", "camunda1!"));
	}

	@Test
	public void testKeycloakLoginNullValues() {
		assertFalse(identityService.checkPassword(null, "camunda1!"));
		assertFalse(identityService.checkPassword("camunda@accso.de", null));
		assertFalse(identityService.checkPassword(null, null));
	}

	@Test
	public void testKeycloakLoginEmptyPassword() {
		assertFalse(identityService.checkPassword("camunda@accso.de", ""));
	}
	
	@Test
	public void testKeycloakLoginSpecialCharacterPassword() {
		assertTrue(identityService.checkPassword("johnfoo@gmail.com", "!§$%&/()=?#'-_.:,;+*~@€"));
		assertTrue(identityService.checkPassword("hans.mustermann@tradermail.info", "äöüÄÖÜ"));
	}

}
