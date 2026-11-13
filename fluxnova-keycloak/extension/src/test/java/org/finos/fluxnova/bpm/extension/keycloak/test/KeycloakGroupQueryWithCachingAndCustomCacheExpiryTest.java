package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.junit.jupiter.api.Assertions.*;

import org.finos.fluxnova.bpm.engine.ProcessEngineConfiguration;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.GroupQuery;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.finos.fluxnova.bpm.extension.keycloak.CacheableKeycloakGroupQuery;
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
 * Tests group queries with caching enabled and cache duration configured
 */
public class KeycloakGroupQueryWithCachingAndCustomCacheExpiryTest extends AbstractKeycloakIdentityProviderTest {

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

		GroupQuery query = identityService.createGroupQuery();

		// query fluxnova-admin at time = 0
		assertEquals("fluxnova-admin", queryGroup(query, "fluxnova-admin").getName());

		// cache contains only fluxnova-admin at this point
		assertEquals(Collections.singletonList("fluxnova-admin"), getCacheEntries());

		// move clock by 2 minutes
		PredictableTicker.moveTimeForwardByMinutes(2);

		// query cam-read-only after 2 minutes
		assertEquals("cam-read-only", queryGroup(query, "cam-read-only").getName());

		// cache contains cam-read-only and fluxnova-admin
		assertEquals(Arrays.asList("cam-read-only", "fluxnova-admin"), getCacheEntries());

		// move clock by another 2 minutes
		PredictableTicker.moveTimeForwardByMinutes(2);

		// cache still contains cam-read-only and fluxnova-admin
		assertEquals(Arrays.asList("cam-read-only", "fluxnova-admin"), getCacheEntries());

		// move clock by another 2 minutes
		PredictableTicker.moveTimeForwardByMinutes(2);

		// fluxnova-admin was evicted because eviction timeout (5 minutes) has been breached 
		// it's been 6 minutes since fluxnova-admin was inserted into cache
		assertEquals(Collections.singletonList("cam-read-only"), getCacheEntries());

		// move clock by another 5 minutes
		PredictableTicker.moveTimeForwardByMinutes(5);

		// cache is empty. cam-read-only has also been evicted
		assertEquals(Collections.emptyList(), getCacheEntries());
	}

	private static Group queryGroup(GroupQuery query, String groupName) {
		Group group = query.groupName(groupName).singleResult();
		processPendingCacheEvictions();
		return group;
	}

	private static List<String> getCacheEntries() {
		processPendingCacheEvictions();
		return CacheAwareKeycloakIdentityProviderPluginForTest.groupQueryCache
						.asMap()
						.keySet()
						.stream()
						.map(CacheableKeycloakGroupQuery::getName)
						.sorted()
						.collect(Collectors.toList());
	}

	private static void processPendingCacheEvictions() {
		CacheAwareKeycloakIdentityProviderPluginForTest.groupQueryCache.cleanUp();
	}
}
