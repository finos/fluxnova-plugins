package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.finos.fluxnova.bpm.engine.ProcessEngineConfiguration;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * User query test for the Keycloak identity provider.
 * Flag useUsernameAsCamundaUserId enabled.
 */
public class KeycloakUseUsernameAsUserIdQueryTest extends AbstractKeycloakIdentityProviderTest {

	static List<String> USER_IDS = new ArrayList<String>();

	@BeforeAll
	public static void setUpProcessEngine() throws Exception {
		// setup Keycloak special test users
		// -------------------------------------
		HttpHeaders headers = authenticateKeycloakAdmin();
		String realm = "test";
		USER_IDS.add(createUser(headers, realm, "hans.wurst", null, null, null, null));

		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
				.createProcessEngineConfigurationFromResource("camunda.useUsernameAsCamundaUserId.cfg.xml");
		configureKeycloakIdentityProviderPlugin(config);
		PluggableProcessEngineTestCase.cachedProcessEngine = config.buildProcessEngine();
	}

	@AfterAll
	public static void tearDownProcessEngine() throws Exception {
		PluggableProcessEngineTestCase.cachedProcessEngine.close();
		PluggableProcessEngineTestCase.cachedProcessEngine = null;

		// delete special test users
		HttpHeaders headers = authenticateKeycloakAdmin();
		String realm = "test";
		USER_IDS.forEach(u -> deleteUser(headers, realm, u));
	}
	
	// ------------------------------------------------------------------------
	// Authorization tests
	// ------------------------------------------------------------------------
	
	@Test
	public void testKeycloakLoginSuccess() {
		assertTrue(identityService.checkPassword("camunda", "camunda1!"));
	}

	// ------------------------------------------------------------------------
	// User Query tests
	// ------------------------------------------------------------------------
	
	@Test
	public void testUserQueryFilterByUserId() {
		User user = identityService.createUserQuery().userId("hans.mustermann").singleResult();
		assertNotNull(user);

		user = identityService.createUserQuery().userId("camunda").singleResult();
		assertNotNull(user);

		// validate user
		assertEquals("camunda", user.getId());
		assertEquals("Admin", user.getFirstName());
		assertEquals("Camunda", user.getLastName());
		assertEquals("camunda@accso.de", user.getEmail());

		user = identityService.createUserQuery().userId("non-existing").singleResult();
		assertNull(user);
	}

	@Test
	public void testUserQueryFilterByUserIdIn() {
		List<User> users = identityService.createUserQuery().userIdIn("camunda", "hans.mustermann").list();
		assertNotNull(users);
		assertEquals(2, users.size());

		users = identityService.createUserQuery().userIdIn("camunda", "non-existing").list();
		assertNotNull(users);
		assertEquals(1, users.size());
	}

	@Test
	public void testUserQueryFilterByEmail() {
		User user = identityService.createUserQuery().userEmail("camunda@accso.de").singleResult();
		assertNotNull(user);

		// validate user
		assertEquals("camunda", user.getId());
		assertEquals("Admin", user.getFirstName());
		assertEquals("Camunda", user.getLastName());
		assertEquals("camunda@accso.de", user.getEmail());

		user = identityService.createUserQuery().userEmail("non-exist*").singleResult();
		assertNull(user);
	}
	
	@Test
	public void testUserQueryFilterByNonExistingAttributeLike() {
		// hans.wurst has no other attributes than his username set
		User user = identityService.createUserQuery().userId("hans.wurst").userEmailLike("*").singleResult();
		assertNotNull(user);
		user = identityService.createUserQuery().userId("hans.wurst").userEmailLike("camunda*").singleResult();
		assertNull(user);
		user = identityService.createUserQuery().userId("hans.wurst").userFirstNameLike("*").singleResult();
		assertNotNull(user);
		user = identityService.createUserQuery().userId("hans.wurst").userFirstNameLike("camunda*").singleResult();
		assertNull(user);
		user = identityService.createUserQuery().userId("hans.wurst").userLastNameLike("*").singleResult();
		assertNotNull(user);
		user = identityService.createUserQuery().userId("hans.wurst").userLastNameLike("camunda*").singleResult();
		assertNull(user);
	}

	@Test
	public void testUserQueryFilterByGroupIdAndId() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup(GROUP_ID_ADMIN)
				.userId("camunda")
				.list();
		assertEquals(1, result.size());

		result = identityService.createUserQuery()
				.memberOfGroup(GROUP_ID_ADMIN)
				.userId("non-exist")
				.list();
		assertEquals(0, result.size());

		result = identityService.createUserQuery()
				.memberOfGroup("non-exist")
				.userId("camunda")
				.list();
		assertEquals(0, result.size());
		
	}

	@Test
	public void testAuthenticatedUserSeesHimself() {
		try {
			processEngineConfiguration.setAuthorizationEnabled(true);

			identityService.setAuthenticatedUserId("non-existing");
			assertEquals(0, identityService.createUserQuery().count());

			identityService.setAuthenticatedUserId("camunda");
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
		List<Group> result = identityService.createGroupQuery().groupMember("camunda").list();
		assertEquals(1, result.size());

		result = identityService.createGroupQuery().groupMember("non-exist").list();
		assertEquals(0, result.size());
	}

	@Test
	public void testFilterByGroupIdAndUserId() {
		Group group = identityService.createGroupQuery()
				.groupId(GROUP_ID_ADMIN)
				.groupMember("camunda")
				.singleResult();
		assertNotNull(group);
		assertEquals("fluxnova-admin", group.getName());

		group = identityService.createGroupQuery()
				.groupId("non-exist")
				.groupMember("camunda")
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
				.groupMember("camunda")
				.singleResult();
		assertNotNull(group);
		assertEquals("fluxnova-admin", group.getName());

		group = identityService.createGroupQuery()
				.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_TEAMLEAD)
				.groupMember("non-exist")
				.singleResult();
		assertNull(group);
	}
	
	@Test
	public void testGroupQueryFilterByUserIdSimilarToClientName() {
		Group group = identityService.createGroupQuery().groupMember("camunda-identity-service").singleResult();
		assertNotNull(group);
		assertEquals(GROUP_ID_SIMILAR_CLIENT_NAME, group.getId());
		assertEquals("camunda-identity-service", group.getName());
	}
}
