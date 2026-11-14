package org.finos.fluxnova.bpm.extension.keycloak.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.junit.jupiter.api.Test;

/**
 * Tests queries with focus on nested group hierarchy.
 */
public class KeycloakNestedGroupsQueryTest extends AbstractKeycloakIdentityProviderTest {

	@Test
	public void testGroupQueryFilterByGroupIdRoot() {
		Group group = identityService.createGroupQuery().groupId(GROUP_ID_HIERARCHY_ROOT).singleResult();
		assertNotNull(group);
		assertEquals(GROUP_ID_HIERARCHY_ROOT, group.getId());
		assertEquals("root", group.getName());
	}

	@Test
	public void testGroupQueryFilterByGroupIdChild() {
		Group group = identityService.createGroupQuery().groupId(GROUP_ID_HIERARCHY_CHILD1).singleResult();
		assertNotNull(group);
		assertEquals(GROUP_ID_HIERARCHY_CHILD1, group.getId());
		assertEquals("child1", group.getName());
	}

	@Test
	public void testGroupQueryFilterByGroupIdSubChild() {
		Group group = identityService.createGroupQuery().groupId(GROUP_ID_HIERARCHY_SUBCHILD1).singleResult();
		assertNotNull(group);
		assertEquals(GROUP_ID_HIERARCHY_SUBCHILD1, group.getId());
		assertEquals("subchild1", group.getName());
	}

	
	@Test
	public void testGroupQueryFilterByUserId() {
		List<Group> result = identityService.createGroupQuery().groupMember("johnfoo@gmail.com").list();
		assertEquals(2, result.size());
		assertEquals(1, result.stream().filter(g -> g.getName().equals("child2")).count(), "expected johnfoo@gmail.com to member of group child2");
		assertEquals(1, result.stream().filter(g -> g.getName().equals("subchild1")).count(), "expected johnfoo@gmail.com to member of group subchild1");
	}
	
	
	@Test
	public void testGroupQueryFilterByGroupIdIn() {
		List<Group> groups = identityService.createGroupQuery()
				.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_HIERARCHY_CHILD1)
				.list();
		assertEquals(2, groups.size());
		assertEquals(1, groups.stream().filter(g -> g.getName().equals("fluxnova-admin")).count(), "fluxnova-admin not found");
		assertEquals(1, groups.stream().filter(g -> g.getName().equals("child1")).count(), "child1 not found");
	}

	@Test
	public void testGroupQueryFilterByGroupIdInAndUserId() {
		Group group = identityService.createGroupQuery()
				.groupIdIn(GROUP_ID_ADMIN, GROUP_ID_MANAGER, GROUP_ID_HIERARCHY_CHILD2, GROUP_ID_HIERARCHY_CHILD1)
				.groupMember("johnfoo@gmail.com")
				.singleResult();
		assertNotNull(group);
		assertEquals("child2", group.getName());
	}
	
	@Test
	public void testGroupQueryFilterByGroupName() {
		Group group = identityService.createGroupQuery().groupName("child1").singleResult();
		assertNotNull(group);
		assertEquals(GROUP_ID_HIERARCHY_CHILD1, group.getId());
		assertEquals("child1", group.getName());
	}

	@Test
	public void testGroupQueryFilterByGroupNameLike() {
		List<Group> result = identityService.createGroupQuery().groupNameLike("child*").list();
		assertEquals(2, result.size());
		assertEquals(1, result.stream().filter(g -> g.getName().equals("child1")).count(), "expected group child1 to be included");
		assertEquals(1, result.stream().filter(g -> g.getName().equals("child2")).count(), "expected group child2 to be included");

		result = identityService.createGroupQuery().groupNameLike("*child*").list();
		assertEquals(3, result.size());
		assertEquals(1, result.stream().filter(g -> g.getName().equals("child1")).count(), "expected group child1 to be included");
		assertEquals(1, result.stream().filter(g -> g.getName().equals("child2")).count(), "expected group child2 to be included");
		assertEquals(1, result.stream().filter(g -> g.getName().equals("subchild1")).count(), "expected group subchild1 to be included");
	}
	
	@Test
	public void testGroupQueryFilterByGroupNameAndGroupNameLike() {
		Group group = identityService.createGroupQuery().groupNameLike("child*").groupName("child2").singleResult();
		assertNotNull(group);
		assertEquals(GROUP_ID_HIERARCHY_CHILD2, group.getId());
		assertEquals("child2", group.getName());
	}

	@Test
	public void testGroupQueryFilterByGroupMember() {
		List<Group> result = identityService.createGroupQuery().groupMember("johnfoo@gmail.com").list();
		assertEquals(2, result.size());
		assertEquals(1, result.stream().filter(g -> g.getName().equals("child2")).count(), "expected johnfoo@gmail.com to member of group child2");
		assertEquals(1, result.stream().filter(g -> g.getName().equals("subchild1")).count(), "expected johnfoo@gmail.com to member of group subchild1");
	}
	
	@Test
	public void testUserQueryFilterByMemberOfGroup() {
		User user = identityService.createUserQuery().memberOfGroup(GROUP_ID_HIERARCHY_SUBCHILD1).singleResult();
		assertNotNull(user);
		assertEquals("johnfoo@gmail.com", user.getId());

		user = identityService.createUserQuery().memberOfGroup(GROUP_ID_HIERARCHY_CHILD2).singleResult();
		assertNotNull(user);
		assertEquals("johnfoo@gmail.com", user.getId());

		user = identityService.createUserQuery().memberOfGroup(GROUP_ID_HIERARCHY_CHILD1).singleResult();
		assertNull(user);
	}

}
