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
 * User query test for the Keycloak identity provider with caching enabled and max size configured
 */
public class KeycloakUserQueryWithCachingAndMaxSizeTest extends AbstractKeycloakIdentityProviderTest {

	@BeforeAll
	public static void setUpProcessEngine() throws Exception {
		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
						.createProcessEngineConfigurationFromResource("camunda.enableCachingAndConfigureMaxCacheSize.cfg.xml");
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
	public void testCacheEntriesEvictedWhenMaxSizeIsReached() {

		UserQuery query = identityService.createUserQuery();

		int countBefore = CountingHttpRequestInterceptor.getHttpRequestCount();

		assertEquals(0, countBefore);

		assertEquals("Admin", queryUser(query, "Admin").getFirstName());

		// Admin has not been queried before so http call count should increase by 1
		assertEquals(countBefore + 1, CountingHttpRequestInterceptor.getHttpRequestCount());

		// cache contains only Admin at this point
		assertEquals(Collections.singletonList("Admin"), getCacheEntries());

		assertEquals("Identity", queryUser(query, "Identity").getFirstName());

		// Identity has not been queried before so http call count should increase by 1
		assertEquals(countBefore + 2, CountingHttpRequestInterceptor.getHttpRequestCount());

		// cache contains Identity and Admin
		assertEquals(Arrays.asList("Admin", "Identity"), getCacheEntries());

		assertEquals("Admin", queryUser(query, "Admin").getFirstName());

		// Admin has already been queried and is still in the cache so count stays same
		assertEquals(countBefore + 2, CountingHttpRequestInterceptor.getHttpRequestCount());

		// cache still contains Identity and Admin
		assertEquals(Arrays.asList("Admin", "Identity"), getCacheEntries());

		assertEquals("Gunnar", queryUser(query, "Gunnar").getFirstName());

		// Gunnar has not been queried before so http call count should increase by 1
		assertEquals(countBefore + 3, CountingHttpRequestInterceptor.getHttpRequestCount());

		// Identity was evicted because maxSize(2) was breached and it was used fewer times than Admin
		assertEquals(Arrays.asList("Admin", "Gunnar"), getCacheEntries());

		// query Identity again
		assertEquals("Identity", queryUser(query, "Identity").getFirstName());

		// count should increase because Identity was removed from cache before the query
		assertEquals(countBefore + 4, CountingHttpRequestInterceptor.getHttpRequestCount());

		// Gunnar was evicted because the name was used fewer times than Admin
		assertEquals(Arrays.asList("Admin", "Identity"), getCacheEntries());
	}

	private static User queryUser(UserQuery query, String firstName) {
		User user = query.userFirstName(firstName).singleResult();
		CacheAwareKeycloakIdentityProviderPluginForTest.userQueryCache.cleanUp();
		return user;
	}

	private static List<String> getCacheEntries() {
		return CacheAwareKeycloakIdentityProviderPluginForTest.userQueryCache
						.asMap()
						.keySet()
						.stream()
						.map(CacheableKeycloakUserQuery::getFirstName)
						.sorted()
						.collect(Collectors.toList());
	}

}
