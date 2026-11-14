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
 * Test for checking a very low value of the maxResultSize parameter.
 * 
 */
public class KeycloakMaxResultSizeTest extends AbstractKeycloakIdentityProviderTest {

	static List<String> USER_IDS = new ArrayList<String>();
	static List<String> GROUP_IDS = new ArrayList<String>();
	
	@BeforeAll
	public static void setUpProcessEngine() throws Exception {
		// setup Keycloak mass data test users
		// -------------------------------------
		HttpHeaders headers = authenticateKeycloakAdmin();
		String realm = "test";
		for (int i = 0; i < 50; i++) {
			USER_IDS.add(createUser(headers, realm, "user.test" + i, "UTest" + i, "User Test" + i, "utest.user" + i + "@test.info", "test"));
		}
		USER_IDS.forEach(u -> assignUserGroup(headers, realm, u, GROUP_ID_MANAGER));

		for (int i = 0; i < 50; i++) {
			GROUP_IDS.add(createGroup(headers, realm, "group.test" + i, false));
		}
		GROUP_IDS.forEach(g -> assignUserGroup(headers, realm, USER_ID_TEAMLEAD, g));

		// setup process engine
		// -------------------------------------
		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
				.createProcessEngineConfigurationFromResource("camunda.configureSmallMaxResultSize.cfg.xml");
		configureKeycloakIdentityProviderPlugin(config).setAdministratorUserId(USER_ID_CAMUNDA_ADMIN);
		PluggableProcessEngineTestCase.cachedProcessEngine = config.buildProcessEngine();
	}

	@AfterAll
	public static void tearDownProcessEngine() throws Exception {
		// tear down process engine
		PluggableProcessEngineTestCase.cachedProcessEngine.close();
		PluggableProcessEngineTestCase.cachedProcessEngine = null;

		// delete mass data test users
		HttpHeaders headers = authenticateKeycloakAdmin();
		String realm = "test";
		USER_IDS.forEach(u -> deleteUser(headers, realm, u));
		GROUP_IDS.forEach(g -> deleteGroup(headers, realm, g));
	}

	@Test
	public void testUserQueryNoFilter() {
		List<User> result = identityService.createUserQuery().list();
		assertEquals(25, result.size());
	}

	@Test
	public void testQueryUnlimitedList() {
	    List<User> result = identityService.createUserQuery().unlimitedList();
	    assertEquals(25, result.size());
	}

	@Test
	public void testGroupMemberQuery() {
		List<User> result = identityService.createUserQuery().memberOfGroup(GROUP_ID_MANAGER).list();	
		assertEquals(25, result.size());
	}

	@Test
	public void testUserQueryUserFirstNameLike() {
		List<User> result = identityService.createUserQuery().userFirstNameLike("UT%").list();
		assertEquals(25, result.size());
	}

	@Test
	public void testUserQueryUserLastNameLike() {
		List<User> result = identityService.createUserQuery().userLastNameLike("User%").orderByUserLastName().desc().list();
		assertEquals(25, result.size());
	}
	
	@Test
	public void testGroupQueryNoFilter() {
		List<Group> result = identityService.createGroupQuery().list();
		assertEquals(25, result.size());
	}

	@Test
	public void testGroupQueryGroupNameLike() {
		List<Group> result = identityService.createGroupQuery().groupNameLike("group%").list();
		assertEquals(25, result.size());
	}

	@Test
	public void testGroupQueryGroupMember() {
		List<Group> result = identityService.createGroupQuery().groupMember("hans.mustermann@tradermail.info").list();
		assertEquals(25, result.size());
	}

}
