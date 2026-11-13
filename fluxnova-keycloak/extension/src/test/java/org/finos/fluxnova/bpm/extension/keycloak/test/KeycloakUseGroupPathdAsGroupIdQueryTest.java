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
 * Group query test for the Keycloak identity provider.
 * Flag useGroupPathAsCamundaGroupId enabled.
 */
public class KeycloakUseGroupPathdAsGroupIdQueryTest extends AbstractKeycloakIdentityProviderTest {

	@BeforeAll
	public static void setUpProcessEngine() throws Exception {
		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
				.createProcessEngineConfigurationFromResource("camunda.useGroupPathAsCamundaGroupId.cfg.xml");
		configureKeycloakIdentityProviderPlugin(config);
		PluggableProcessEngineTestCase.cachedProcessEngine = config.buildProcessEngine();
	}

	@AfterAll
	public static void tearDownProcessEngine() throws Exception {
		PluggableProcessEngineTestCase.cachedProcessEngine.close();
		PluggableProcessEngineTestCase.cachedProcessEngine = null;
	}
	
	// ------------------------------------------------------------------------
	// Group Query tests
	// ------------------------------------------------------------------------
	
	@Test
	public void testFilterByGroupId() {
		Group group = identityService.createGroupQuery().groupId("fluxnova-admin").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals("fluxnova-admin", group.getId());
		assertEquals("fluxnova-admin", group.getName());
		assertEquals("SYSTEM", group.getType());

		group = identityService.createGroupQuery().groupId("whatever").singleResult();
		assertNull(group);
	}

	@Test
	public void testFilterByChildGroupId() {
		Group group = identityService.createGroupQuery().groupId("root/child1").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals("root/child1", group.getId());
		assertEquals("child1", group.getName());
	}

	@Test
	public void testFilterByUserId() {
		List<Group> result = identityService.createGroupQuery().groupMember("camunda@accso.de").list();
		assertEquals(1, result.size());
	}
	
	@Test
	public void testFilterByUserIdMemberOfChildGroups() {
		List<Group> groups = identityService.createGroupQuery().groupMember("johnfoo@gmail.com").list();
		assertEquals(2, groups.size());
		
		for (Group group : groups) {
			if (!group.getId().equals("root/child1/subchild1") && !group.getId().equals("root/child2")) {
				fail();
			}
		}
		
	}

	@Test
	public void testFilterByGroupTypeAndGroupId() {
		Group group = identityService.createGroupQuery().groupType("SYSTEM").groupId("cam-read-only").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals("cam-read-only", group.getId());
		assertEquals("cam-read-only", group.getName());
		assertEquals("SYSTEM", group.getType());
	}
	
	
	@Test
	public void testFilterByGroupIdIn() {
		List<Group> groups = identityService.createGroupQuery()
				.groupIdIn("fluxnova-admin", "manager")
				.list();

		assertEquals(2, groups.size());
		for (Group group : groups) {
			if (!group.getName().equals("fluxnova-admin") && !group.getName().equals("manager")) {
				fail();
			}
		}
	}

	@Test
	public void testFilterByChildGroupIdIn() {
		List<Group> groups = identityService.createGroupQuery()
				.groupIdIn("root/child1/subchild1", "root/child2")
				.list();

		assertEquals(2, groups.size());
		for (Group group : groups) {
			if (!group.getId().equals("root/child1/subchild1") && !group.getId().equals("root/child2")) {
				fail();
			}
		}
	}

	@Test
	public void testFilterByGroupIdInAndType() {
		Group group = identityService.createGroupQuery()
				.groupIdIn("fluxnova-admin", "manager")
				.groupType("WORKFLOW")
				.singleResult();
		assertNotNull(group);
		assertEquals("manager", group.getName());
		
		group = identityService.createGroupQuery()
				.groupIdIn("fluxnova-admin", "manager")
				.groupType("SYSTEM")
				.singleResult();
		assertNotNull(group);
		assertEquals("fluxnova-admin", group.getName());
	}

	@Test
	public void testFilterByGroupIdInAndUserId() {
		Group group = identityService.createGroupQuery()
				.groupIdIn("fluxnova-admin", "manager")
				.groupMember("camunda@accso.de")
				.singleResult();
		assertNotNull(group);
		assertEquals("fluxnova-admin", group.getName());
	}
	
	@Test
	public void testFilterByGroupName() {
		Group group = identityService.createGroupQuery().groupName("manager").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals("manager", group.getId());
		assertEquals("manager", group.getName());

		group = identityService.createGroupQuery().groupName("whatever").singleResult();
		assertNull(group);
	}

	@Test
	public void testFilterByGroupNameLike() {
		Group group = identityService.createGroupQuery().groupNameLike("manage*").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals("manager", group.getId());
		assertEquals("manager", group.getName());

		group = identityService.createGroupQuery().groupNameLike("what*").singleResult();
		assertNull(group);
	}
	
	@Test
	public void testFilterByGroupNameAndGroupNameLike() {
		Group group = identityService.createGroupQuery().groupNameLike("ma*").groupName("manager").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals("manager", group.getId());
		assertEquals("manager", group.getName());
	}

	@Test
	public void testFilterByGroupMember() {
		List<Group> list = identityService.createGroupQuery().groupMember("camunda@accso.de").list();
		assertEquals(1, list.size());
		list = identityService.createGroupQuery().groupMember("Gunnar.von-der-Beck@accso.de").list();
		assertEquals(2, list.size());
		list = identityService.createGroupQuery().groupMember("hans.mustermann@tradermail.info").list();
		assertEquals(1, list.size());
		list = identityService.createGroupQuery().groupMember("non-existing").list();
		assertEquals(0, list.size());
	}

	@Test
	public void testOrderByGroupId() {
		List<Group> groupList = identityService.createGroupQuery().orderByGroupId().desc().list();
		assertEquals(9, groupList.size());
		assertTrue(groupList.get(0).getId().compareTo(groupList.get(1).getId()) > 0);
		assertTrue(groupList.get(1).getId().compareTo(groupList.get(2).getId()) > 0);
		assertTrue(groupList.get(2).getId().compareTo(groupList.get(3).getId()) > 0);
		assertTrue(groupList.get(5).getId().compareTo(groupList.get(6).getId()) > 0);
		assertTrue(groupList.get(6).getId().compareTo(groupList.get(7).getId()) > 0);
	}

	@Test
	public void testOrderByGroupName() {
		List<Group> groupList = identityService.createGroupQuery().orderByGroupName().list();
		assertEquals(9, groupList.size());
		assertTrue(groupList.get(0).getName().compareTo(groupList.get(1).getName()) < 0);
		assertTrue(groupList.get(1).getName().compareTo(groupList.get(2).getName()) < 0);
		assertTrue(groupList.get(2).getName().compareTo(groupList.get(3).getName()) < 0);
		assertTrue(groupList.get(5).getName().compareTo(groupList.get(6).getName()) < 0);
		assertTrue(groupList.get(6).getName().compareTo(groupList.get(7).getName()) < 0);
	}

	// ------------------------------------------------------------------------
	// User Query tests
	// ------------------------------------------------------------------------

	@Test
	public void testFilterUserByGroupId() {
		List<User> result = identityService.createUserQuery().memberOfGroup("teamlead").list();
		assertEquals(2, result.size());

		result = identityService.createUserQuery().memberOfGroup("non-exist").list();
		assertEquals(0, result.size());
	}

	@Test
	public void testFilterUserByChildGroupId() {
		List<User> result = identityService.createUserQuery().memberOfGroup("root/child1/subchild1").list();
		assertEquals(1, result.size());
	}

	@Test
	public void testFilterUserByGroupIdAndFirstname() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup("teamlead")
				.userFirstName("Gunnar")
				.list();
		assertEquals(1, result.size());
	}

	@Test
	public void testFilterUserByGroupIdAndId() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup("teamlead")
				.userId("gunnar.von-der-beck@accso.de")
				.list();
		assertEquals(1, result.size());
	}

	@Test
	public void testFilterUserByChildGroupIdAndId() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup("root/child1/subchild1")
				.userId("johnfoo@gmail.com")
				.list();
		assertEquals(1, result.size());
	}

	@Test
	public void testFilterUserByGroupIdAndLastname() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup("teamlead")
				.userLastName("von der Beck")
				.list();
		assertEquals(1, result.size());
	}

	@Test
	public void testFilterUserByGroupIdAndEmail() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup("teamlead")
				.userEmail("gunnar.von-der-beck@accso.de")
				.list();
		assertEquals(1, result.size());
	}

	@Test
	public void testFilterUserByGroupIdAndEmailLike() {
		List<User> result = identityService.createUserQuery()
				.memberOfGroup("teamlead")
				.userEmailLike("*@accso.de")
				.list();
		assertEquals(1, result.size());
	}

	@Test
	public void testFilterUserByGroupIdSimilarToClientName() {
		User user = identityService.createUserQuery().memberOfGroup("camunda-identity-service").singleResult();
		assertNotNull(user);
		assertEquals("identity.service@test.de", user.getId());
	}
	
}
