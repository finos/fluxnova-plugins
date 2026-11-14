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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests group queries with caching enabled and max size configured
 */
public class KeycloakGroupQueryWithCachingAndMaxSizeTest extends AbstractKeycloakIdentityProviderTest {

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

		GroupQuery query = identityService.createGroupQuery();

		int countBefore = CountingHttpRequestInterceptor.getHttpRequestCount();

		assertEquals(0, countBefore);

		assertEquals("fluxnova-admin", queryGroup(query, "fluxnova-admin").getName());

		// fluxnova-admin has not been queried before so http call count should increase by 1
		assertEquals(countBefore + 1, CountingHttpRequestInterceptor.getHttpRequestCount());

		// cache contains only fluxnova-admin at this point
		assertEquals(Collections.singletonList("fluxnova-admin"), getCacheEntries());

		assertEquals("cam-read-only", queryGroup(query, "cam-read-only").getName());

		// cam-read-only has not been queried before so http call count should increase by 1
		assertEquals(countBefore + 2, CountingHttpRequestInterceptor.getHttpRequestCount());

		// cache contains cam-read-only and fluxnova-admin
		assertEquals(Arrays.asList("cam-read-only", "fluxnova-admin"), getCacheEntries());

		assertEquals("fluxnova-admin", queryGroup(query, "fluxnova-admin").getName());

		// fluxnova-admin has already been queried and is still in the cache so count stays same
		assertEquals(countBefore + 2, CountingHttpRequestInterceptor.getHttpRequestCount());

		// cache still contains cam-read-only and fluxnova-admin
		assertEquals(Arrays.asList("cam-read-only", "fluxnova-admin"), getCacheEntries());

		assertEquals("manager", queryGroup(query, "manager").getName());

		// manager has not been queried before so http call count should increase by 1
		assertEquals(countBefore + 3, CountingHttpRequestInterceptor.getHttpRequestCount());

		// cam-read-only was evicted because maxSize(2) was breached and it was used fewer times than fluxnova-admin
		assertEquals(Arrays.asList("fluxnova-admin", "manager"), getCacheEntries());

		// query cam-read-only again
		assertEquals("cam-read-only", queryGroup(query, "cam-read-only").getName());

		// count should increase because cam-read-only was removed from cache before the query
		assertEquals(countBefore + 4, CountingHttpRequestInterceptor.getHttpRequestCount());

		// manager was evicted because it was used fewer times than fluxnova-admin
		assertEquals(Arrays.asList("cam-read-only", "fluxnova-admin"), getCacheEntries());
	}

	private static Group queryGroup(GroupQuery query, String groupName) {
		Group group = query.groupName(groupName).singleResult();
		CacheAwareKeycloakIdentityProviderPluginForTest.groupQueryCache.cleanUp();
		return group;
	}

	private static List<String> getCacheEntries() {
		return CacheAwareKeycloakIdentityProviderPluginForTest.groupQueryCache
						.asMap()
						.keySet()
						.stream()
						.map(CacheableKeycloakGroupQuery::getName)
						.sorted()
						.collect(Collectors.toList());
	}
}
