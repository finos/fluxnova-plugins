package org.finos.fluxnova.bpm.extension.keycloak.test.util;

import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.extension.keycloak.CacheableKeycloakCheckPasswordCall;
import org.finos.fluxnova.bpm.extension.keycloak.CacheableKeycloakGroupQuery;
import org.finos.fluxnova.bpm.extension.keycloak.CacheableKeycloakUserQuery;
import org.finos.fluxnova.bpm.extension.keycloak.KeycloakIdentityProviderFactory;
import org.finos.fluxnova.bpm.extension.keycloak.cache.CacheConfiguration;
import org.finos.fluxnova.bpm.extension.keycloak.cache.CaffeineCache;
import org.finos.fluxnova.bpm.extension.keycloak.plugin.KeycloakIdentityProviderPlugin;

import java.util.List;

public class CacheAwareKeycloakIdentityProviderPluginForTest extends KeycloakIdentityProviderPlugin {

	public static CaffeineCache<CacheableKeycloakUserQuery, List<User>> userQueryCache;
	public static CaffeineCache<CacheableKeycloakGroupQuery, List<Group>> groupQueryCache;
	public static CaffeineCache<CacheableKeycloakCheckPasswordCall, Boolean> checkPasswordCache;

	@Override
	public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
		super.preInit(processEngineConfiguration);
		KeycloakIdentityProviderFactory factory =
						(KeycloakIdentityProviderFactory) processEngineConfiguration.getIdentityProviderSessionFactory();

		PredictableTicker ticker = new PredictableTicker();
		CacheConfiguration cacheConfiguration = CacheConfiguration.from(this);
		CacheConfiguration loginCacheConfiguration = CacheConfiguration.fromLoginConfigOf(this);

		// instantiate with ticker that can be controlled in tests
		userQueryCache = new CaffeineCache<>(cacheConfiguration, ticker);
		groupQueryCache = new CaffeineCache<>(cacheConfiguration, ticker);
		checkPasswordCache = new CaffeineCache<>(loginCacheConfiguration, ticker);

		factory.setUserQueryCache(userQueryCache);
		factory.setGroupQueryCache(groupQueryCache);
		factory.setCheckPasswordCache(checkPasswordCache);
	}
}
