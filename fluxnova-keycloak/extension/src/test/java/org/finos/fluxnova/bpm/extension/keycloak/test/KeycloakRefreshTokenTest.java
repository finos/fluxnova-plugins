package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.extension.keycloak.KeycloakContext;
import org.finos.fluxnova.bpm.extension.keycloak.KeycloakContextProvider;
import org.finos.fluxnova.bpm.extension.keycloak.KeycloakIdentityProviderFactory;
import org.junit.jupiter.api.Test;

/**
 * Refresh access token test.
 */
public class KeycloakRefreshTokenTest extends AbstractKeycloakIdentityProviderTest {

	/**
	 * Tests refreshing access token.
	 * @throws Exception in case of errors
	 */
	@Test
	public void testRefreshToken() throws Exception {
		// access Keycloak
	    assertEquals(5, identityService.createUserQuery().count());

	    // expire current token (the dirty way)
	    KeycloakIdentityProviderFactory sessionFacory = (KeycloakIdentityProviderFactory) 
	    		((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getIdentityProviderSessionFactory();
	    KeycloakContextProvider keycloakContextProvider = getProtectedField(sessionFacory, "keycloakContextProvider");
	    KeycloakContext ctx = getProtectedField(keycloakContextProvider, "context");
	    Field expiresField = KeycloakContext.class.getDeclaredField("expiresAt");
	    expiresField.setAccessible(true);
	    expiresField.set(ctx, 0);
	    assertTrue(ctx.needsRefresh());
	    
		// access Keycloak again
	    assertEquals(5, identityService.createUserQuery().count());
	    
	}
	
	/**
	 * Helper for accessing protected fields.
	 * @param obj the parent object
	 * @param fieldName the name of the declared field in the parent object to retrieve
	 * @return the value of the field
	 * @throws Exception in case of errors
	 */
	@SuppressWarnings("unchecked")
	private <T> T getProtectedField(Object obj, String fieldName) throws Exception {
		Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(obj);
	}
	
}
