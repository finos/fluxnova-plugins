package org.finos.fluxnova.bpm.extension.keycloak;

import java.util.List;

import org.finos.fluxnova.bpm.engine.identity.User;
import org.finos.fluxnova.bpm.engine.identity.UserQuery;
import org.finos.fluxnova.bpm.engine.impl.Page;
import org.finos.fluxnova.bpm.engine.impl.UserQueryImpl;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandExecutor;

/**
 * Keycloak specific user query implementation.
 */
public class KeycloakUserQuery extends UserQueryImpl {

	private static final long serialVersionUID = 1L;

	public KeycloakUserQuery() {
		super();
	}

	public KeycloakUserQuery(CommandExecutor commandExecutor) {
		super(commandExecutor);
	}

	// execute queries /////////////////////////////////////////

	@Override
	public long executeCount(CommandContext commandContext) {
		final KeycloakIdentityProviderSession provider = getKeycloakIdentityProvider(commandContext);
		return provider.findUserCountByQueryCriteria(this);
	}

	@Override
	public List<User> executeList(CommandContext commandContext, Page page) {
		final KeycloakIdentityProviderSession provider = getKeycloakIdentityProvider(commandContext);
		return provider.findUserByQueryCriteria(this);
	}

	protected KeycloakIdentityProviderSession getKeycloakIdentityProvider(CommandContext commandContext) {
		return (KeycloakIdentityProviderSession) commandContext.getReadOnlyIdentityProvider();
	}

	// unimplemented features //////////////////////////////////

	@Override
	public UserQuery memberOfTenant(String tenantId) {
		throw new UnsupportedOperationException("The Keycloak identity provider does currently not support tenant queries.");
	}

	@Override
	public UserQuery potentialStarter(String procDefId) {
		throw new UnsupportedOperationException("The Keycloak identity provider does currently not support potential starter queries.");
	}

}
