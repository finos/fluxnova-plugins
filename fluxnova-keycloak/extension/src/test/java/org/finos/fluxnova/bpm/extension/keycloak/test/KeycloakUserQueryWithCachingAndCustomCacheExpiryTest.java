package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.junit.jupiter.api.Assertions.*;

import org.finos.fluxnova.bpm.engine.ProcessEngineConfiguration;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.finos.fluxnova.bpm.engine.identity.UserQuery;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.finos.fluxnova.bpm.extension.keycloak.CacheableKeycloakUserQuery;
import org.finos.fluxnova.bpm.extension.keycloak.plugin.KeycloakIdentityProviderPlugin;
import org.finos.fluxnova.bpm.extension.keycloak.test.util.CacheAwareKeycloakIdentityProviderPluginForTest;
import org.finos.fluxnova.bpm.extension.keycloak.test.util.CountingHttpRequestInterceptor;
import org.finos.fluxnova.bpm.extension.keycloak.test.util.PredictableTicker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * User query test for the Keycloak identity provider with caching enabled and cache duration configured
 */
public class KeycloakUserQueryWithCachingAndCustomCacheExpiryTest extends AbstractKeycloakIdentityProviderTest {

	@BeforeAll
	public static void setUpProcessEngine() throws Exception {
		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
						.createProcessEngineConfigurationFromResource("camunda.enableCachingAndConfigureCacheDuration.cfg.xml");
		configureKeycloakIdentityProviderPlugin(config);
		PluggableProcessEngineTestCase.cachedProcessEngine = config.buildProcessEngine();
	}

	@AfterAll
	public static void tearDownProcessEngine() throws Exception {
		PluggableProcessEngineTestCase.cachedProcessEngine.close();
		PluggableProcessEngineTestCase.cachedProcessEngine = null;
	}

	@AfterEach
	public void cleanUp() throws Exception {
		// delete all created authorizations
		processEngine.getAuthorizationService().createAuthorizationQuery().list().forEach(a -> {
			processEngine.getAuthorizationService().deleteAuthorization(a.getId());
		});
		this.clearCache();
		PredictableTicker.reset();
		CountingHttpRequestInterceptor.resetCount();
	}

	/**
	 * clears the query cache so each test can start with a clean slate
	 */
	private void clearCache() {
		processEngineConfiguration.getProcessEnginePlugins()
						.stream()
						.filter(KeycloakIdentityProviderPlugin.class::isInstance)
						.map(KeycloakIdentityProviderPlugin.class::cast)
						.forEach(KeycloakIdentityProviderPlugin::clearCache);
	}

	// ------------------------------------------------------------------------
	// Test configuration
	// ------------------------------------------------------------------------

	@Test
	public void testCacheEntriesEvictedWhenCacheTimeoutIsReached() {

		UserQuery query = identityService.createUserQuery();

		// query Admin at time = 0
		assertEquals("Admin", queryUser(query, "Admin").getFirstName());

		// cache contains only Admin at this point
		assertEquals(Collections.singletonList("Admin"), getCacheEntries());

		// move clock by 2 minutes
		PredictableTicker.moveTimeForwardByMinutes(2);

		// query Identity after 2 minutes
		assertEquals("Identity", queryUser(query, "Identity").getFirstName());

		// cache contains Identity and Admin
		assertEquals(Arrays.asList("Admin", "Identity"), getCacheEntries());

		// move clock by another 2 minutes
		PredictableTicker.moveTimeForwardByMinutes(2);

		// cache still contains Identity and Admin
		assertEquals(Arrays.asList("Admin", "Identity"), getCacheEntries());

		// move clock by another 2 minutes
		PredictableTicker.moveTimeForwardByMinutes(2);

		// Admin was evicted because eviction timeout (5 minutes) has been breached 
		// it's been 6 minutes since Admin was inserted into cache
		assertEquals(Collections.singletonList("Identity"), getCacheEntries());

		// move clock by another 5 minutes
		PredictableTicker.moveTimeForwardByMinutes(5);

		// cache is empty. Identity has also been evicted
		assertEquals(Collections.emptyList(), getCacheEntries());
	}

	private static User queryUser(UserQuery query, String firstName) {
		User user = query.userFirstName(firstName).singleResult();
		processPendingCacheEvictions();
		return user;
	}

	private static List<String> getCacheEntries() {
		processPendingCacheEvictions();
		return CacheAwareKeycloakIdentityProviderPluginForTest.userQueryCache
						.asMap()
						.keySet()
						.stream()
						.map(CacheableKeycloakUserQuery::getFirstName)
						.sorted()
						.collect(Collectors.toList());
	}

	private static void processPendingCacheEvictions() {
		CacheAwareKeycloakIdentityProviderPluginForTest.userQueryCache.cleanUp();
	}

}
