package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.finos.fluxnova.bpm.engine.ProcessEngineConfiguration;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * User query test for the Keycloak identity provider.
 * Flags useEmailAsCamundaUserId and useUsernameAsCamundaUserId disabled.
 */
public class KeycloakUseKeycloakIdAsUserIdQueryTest extends AbstractKeycloakIdentityProviderTest {

	@BeforeAll
	public static void setUpProcessEngine() throws Exception {
		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
				.createProcessEngineConfigurationFromResource("camunda.useKeycloakIdAsCamundaUserId.cfg.xml");
		configureKeycloakIdentityProviderPlugin(config);
		PluggableProcessEngineTestCase.cachedProcessEngine = config.buildProcessEngine();
	}

	@AfterAll
	public static void tearDownProcessEngine() throws Exception {
		PluggableProcessEngineTestCase.cachedProcessEngine.close();
		PluggableProcessEngineTestCase.cachedProcessEngine = null;
	}

	// ------------------------------------------------------------------------
	// Authorization tests
	// ------------------------------------------------------------------------
	
	@Test
	public void testKeycloakLoginSuccess() {
		assertTrue(identityService.checkPassword(USER_ID_CAMUNDA_ADMIN, "camunda1!"));
	}

	// ------------------------------------------------------------------------
	// User Query tests
	// ------------------------------------------------------------------------
	
	@Test
	public void testUserQueryFilterByUserId() {
		User user = identityService.createUserQuery().userId(USER_ID_TEAMLEAD).singleResult();
		assertNotNull(user);

		user = identityService.createUserQuery().userId(USER_ID_CAMUNDA_ADMIN).singleResult();
		assertNotNull(user);

		// validate user
		assertEquals(USER_ID_CAMUNDA_ADMIN, user.getId());
		assertEquals("Admin", user.getFirstName());
		assertEquals("Camunda", user.getLastName());
		assertEquals("camunda@accso.de", user.getEmail());

		user = identityService.createUserQuery().userId("non-existing").singleResult();
		assertNull(user);
	}

	@Test
	public void testUserQueryFilterByUserIdIn() {
		List<User> users = identityService.createUserQuery().userIdIn(USER_ID_CAMUNDA_ADMIN, USER_ID_TEAMLEAD).list();
		assertNotNull(users);
		assertEquals(2, users.size());

		users = identityService.createUserQuery().userIdIn(USER_ID_CAMUNDA_ADMIN, "non-existing").list();
		assertNotNull(users);
		assertEquals(1, users.size());
	}

	@Test
	public void testUserQueryFilterByEmail() {
		User user = identityService.createUserQuery().userEmail("camunda@accso.de").singleResult();
		assertNotNull(user);

		// validate user
		assertEquals(USER_ID_CAMUNDA_ADMIN, user.getId());
		assertEquals("Admin", user.getFirstName());
		assertEquals("Camunda", user.getLastName());
		assertEquals("camunda@accso.de", user.getEmail());

		user = identityService.createUserQuery().userEmail("non-exist*").singleResult();
		assertNull(user);
	}

	@Test
	public void testUserQueryFilterByGroupIdAndId() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup(GROUP_ID_ADMIN)
				.userId(USER_ID_CAMUNDA_ADMIN)
				.list();
		assertEquals(1, result.size());

		result = identityService.createUserQuery()
				.memberOfGroup(GROUP_ID_ADMIN)
				.userId("non-exist")
				.list();
		assertEquals(0, result.size());

		result = identityService.createUserQuery()
				.memberOfGroup("non-exist")
				.userId(USER_ID_CAMUNDA_ADMIN)
				.list();
		assertEquals(0, result.size());
		
	}

	@Test
	public void testAuthenticatedUserSeesHimself() {
		try {
			processEngineConfiguration.setAuthorizationEnabled(true);

			identityService.setAuthenticatedUserId("non-existing");
			assertEquals(0, identityService.createUserQuery().count());

			identityService.setAuthenticatedUserId(USER_ID_CAMUNDA_ADMIN);
			assertEquals(1, identityService.createUserQuery().count());

		} finally {
			processEngineConfiguration.setAuthorizationEnabled(false);
			identityService.clearAuthentication();
		}
	}

	// ------------------------------------------------------------------------
	// Group query tests
	// ------------------------------------------------------------------------

	@Test
	public void testGroupQueryFilterByUserId() {
		List<Group> result = identityService.createGroupQuery().groupMember(USER_ID_CAMUNDA_ADMIN).list();
		assertEquals(1, result.size());

		result = identityService.createGroupQuery().groupMember("non-exist").list();
		assertEquals(0, result.size());
	}

	@Test
	public void testFilterByGroupIdAndUserId() {
		Group group = identityService.createGroupQuery()
				.groupId(GROUP_ID_ADMIN)
				.groupMember(USER_ID_CAMUNDA_ADMIN)
				.singleResult();
		assertNotNull(group);
		assertEquals("fluxnova-admin", group.getName());

		group = identityService.createGroupQuery()
				.groupId("non-exist")
				.groupMember(USER_ID_CAMUNDA_ADMIN)
				.singleResult();
		assertNull(group);

		group = identityService.createGroupQuery()
				.groupId(GROUP_ID_ADMIN)
				.groupMember("non-exist")
				.singleResult();
		assertNull(group);
	}
	
	@Test
	public void testFilterByGroupIdInAndUserId() {
		Group group = identityService.createGroupQuery()
				.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_TEAMLEAD)
				.groupMember(USER_ID_CAMUNDA_ADMIN)
				.singleResult();
		assertNotNull(group);
		assertEquals("fluxnova-admin", group.getName());

		group = identityService.createGroupQuery()
				.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_TEAMLEAD)
				.groupMember("non-exist")
				.singleResult();
		assertNull(group);
	}
	
}
