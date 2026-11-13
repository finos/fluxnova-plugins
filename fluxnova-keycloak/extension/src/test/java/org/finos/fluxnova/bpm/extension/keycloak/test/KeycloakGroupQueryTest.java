package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.finos.fluxnova.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.finos.fluxnova.bpm.engine.authorization.Authorization;
import org.finos.fluxnova.bpm.engine.authorization.Permission;
import org.finos.fluxnova.bpm.engine.authorization.Resource;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.extension.keycloak.CacheableKeycloakGroupQuery;
import org.finos.fluxnova.bpm.extension.keycloak.KeycloakGroupQuery;
import org.junit.jupiter.api.Test;

/**
 * Tests group queries.
 */
public class KeycloakGroupQueryTest extends AbstractKeycloakIdentityProviderTest {

	@Test
	public void testQueryNoFilter() {
		List<Group> groupList = identityService.createGroupQuery().list();
		assertEquals(9, groupList.size());
	}

	@Test
	public void testQueryUnlimitedList() {
		List<Group> groupList = identityService.createGroupQuery().unlimitedList();
		assertEquals(9, groupList.size());
	}

	@Test
	public void testQueryPaging() {
		// First page
		List<Group> result = identityService.createGroupQuery().listPage(0, 3);
		assertEquals(3, result.size());

		// Next page
		List<Group> resultNext = identityService.createGroupQuery().listPage(3, 3);
		assertEquals(3, resultNext.size());

		// Next page
		List<Group> resultLast = identityService.createGroupQuery().listPage(6, 10);
		assertEquals(3, resultLast.size());

		// unique results
		assertEquals(0, result.stream().filter(group -> resultNext.contains(group)).count());
		assertEquals(0, result.stream().filter(group -> resultLast.contains(group)).count());
	}

	@Test
	public void testFilterByGroupId() {
		Group group = identityService.createGroupQuery().groupId(GROUP_ID_ADMIN).singleResult();
		assertNotNull(group);

		// validate result
		assertEquals(GROUP_ID_ADMIN, group.getId());
		assertEquals("fluxnova-admin", group.getName());
		assertEquals("SYSTEM", group.getType());

		group = identityService.createGroupQuery().groupId("whatever").singleResult();
		assertNull(group);
	}

	@Test
	public void testFilterByUserId() {
		List<Group> result = identityService.createGroupQuery().groupMember("camunda@accso.de").list();
		assertEquals(1, result.size());
	}

	@Test
	public void testAuthenticatedUserCanQueryOwnGroups() {
		try {
			processEngineConfiguration.setAuthorizationEnabled(true);
			identityService.setAuthenticatedUserId("johnfoo@gmail.com");

			assertEquals(0, identityService.createGroupQuery().groupMember("camunda@accso.de").count());
			assertEquals(2, identityService.createGroupQuery().groupMember("johnfoo@gmail.com").count());

		} finally {
			processEngineConfiguration.setAuthorizationEnabled(false);
			identityService.clearAuthentication();
		}
	}
	
	/* The REST API of Keycloak (get list(!) of groups) does not deliver group attributes :-(
	public void testFilterByGroupType() {
		List<Group> result = identityService.createGroupQuery().groupType("SYSTEM").list();
		assertEquals(2, result.size());
	}
	*/

	@Test
	public void testFilterByGroupTypeAndGroupId() {
		Group group = identityService.createGroupQuery().groupType("SYSTEM").groupId(GROUP_ID_SYSTEM_READONLY).singleResult();
		assertNotNull(group);

		// validate result
		assertEquals(GROUP_ID_SYSTEM_READONLY, group.getId());
		assertEquals("cam-read-only", group.getName());
		assertEquals("SYSTEM", group.getType());
	}


	@Test
	public void testFilterByGroupIdIn() {
		List<Group> groups = identityService.createGroupQuery()
						.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_MANAGER)
						.list();

		assertEquals(2, groups.size());
		for (Group group : groups) {
			if (!group.getName().equals("fluxnova-admin") && !group.getName().equals("manager")) {
				fail();
			}
		}
		
		groups = identityService.createGroupQuery().groupIdIn(GROUP_ID_MANAGER,  "non-existent").list();
		assertEquals(1, groups.size());
		assertEquals("manager", groups.get(0).getName());

		groups = identityService.createGroupQuery().groupIdIn(GROUP_ID_MANAGER).list();
		assertEquals(1, groups.size());
		assertEquals("manager", groups.get(0).getName());
	
	}

	@Test
	public void testFilterByGroupIdInAndType() {
		Group group = identityService.createGroupQuery()
						.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_MANAGER)
						.groupType("WORKFLOW")
						.singleResult();
		assertNotNull(group);
		assertEquals("manager", group.getName());

		group = identityService.createGroupQuery()
						.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_MANAGER)
						.groupType("SYSTEM")
						.singleResult();
		assertNotNull(group);
		assertEquals("fluxnova-admin", group.getName());
	}

	@Test
	public void testFilterByGroupIdInAndUserId() {
		Group group = identityService.createGroupQuery()
						.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_MANAGER)
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
		assertEquals(GROUP_ID_MANAGER, group.getId());
		assertEquals("manager", group.getName());

		group = identityService.createGroupQuery().groupName("whatever").singleResult();
		assertNull(group);
	}

	@Test
	public void testFilterByGroupNameLike() {
		Group group = identityService.createGroupQuery().groupNameLike("manage*").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals(GROUP_ID_MANAGER, group.getId());
		assertEquals("manager", group.getName());

		group = identityService.createGroupQuery().groupNameLike("what*").singleResult();
		assertNull(group);
	}

	@Test
	public void testFilterByGroupNameAndGroupNameLike() {
		Group group = identityService.createGroupQuery().groupNameLike("ma*").groupName("manager").singleResult();
		assertNotNull(group);

		// validate result
		assertEquals(GROUP_ID_MANAGER, group.getId());
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

	@Test
	public void testOrderByGroupType() {
		List<Group> groupList = identityService.createGroupQuery().orderByGroupType().desc().list();
		assertEquals(9, groupList.size());
		assertTrue(groupList.get(0).getType().compareTo(groupList.get(1).getType()) >= 0);
		assertTrue(groupList.get(1).getType().compareTo(groupList.get(2).getType()) >= 0);
		assertTrue(groupList.get(2).getType().compareTo(groupList.get(3).getType()) >= 0);
		assertTrue(groupList.get(5).getType().compareTo(groupList.get(6).getType()) >= 0);
		assertTrue(groupList.get(6).getType().compareTo(groupList.get(7).getType()) >= 0);
	}

	@Test
	public void testQueryObjectEquality() {

		KeycloakGroupQuery q1 = (KeycloakGroupQuery) identityService.createGroupQuery();
		KeycloakGroupQuery q2 = (KeycloakGroupQuery) identityService.createGroupQuery();

		assertNotSame(q1, q2); // not the same object (by identity)
		assertNotEquals(q1, q2); // not equal

		assertNotSame(CacheableKeycloakGroupQuery.of(q1), CacheableKeycloakGroupQuery.of(q2)); // not the same object
		assertEquals(CacheableKeycloakGroupQuery.of(q1), CacheableKeycloakGroupQuery.of(q2)); // but they are equal

		q1.groupId("id1");

		// not equal because first query has a filter
		assertNotEquals(CacheableKeycloakGroupQuery.of(q1), CacheableKeycloakGroupQuery.of(q2));

		q2.groupId("id1");

		// equal now because second query also has same filter
		assertEquals(CacheableKeycloakGroupQuery.of(q1), CacheableKeycloakGroupQuery.of(q2));
	}

	protected void createGrantAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
		Authorization authorization = createAuthorization(AUTH_TYPE_GRANT, resource, resourceId);
		authorization.setUserId(userId);
		for (Permission permission : permissions) {
			authorization.addPermission(permission);
		}
		authorizationService.saveAuthorization(authorization);
	}

	protected Authorization createAuthorization(int type, Resource resource, String resourceId) {
		Authorization authorization = authorizationService.createNewAuthorization(type);

		authorization.setResource(resource);
		if (resourceId != null) {
			authorization.setResourceId(resourceId);
		}

		return authorization;
	}

}
