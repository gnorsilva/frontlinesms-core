/**
 * 
 */
package net.frontlinesms.data.repository;

import java.util.List;

import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Group;

/**
 * @author Alex
 */
public interface GroupMembershipDao {
	/**
	 * @param group
	 * @return all members of a group and its descendants
	 */
	public List<Contact> getMembers(Group group);
	/** @return active members of a group */
	public List<Contact> getActiveMembers(Group group);
	public int getMemberCount(Group group);
	public List<Contact> getMembers(Group group, int startIndex, int limit);

	/**
	 * @param contact
	 * @return all groups this contact is a direct member of
	 */
	public List<Group> getGroups(Contact contact);

	/** Add a contact to a group
	 * @return <code>true</code> if the contact was added to the group, <code>false</code> if he was already a member  */
	public boolean addMembership(Group g, Contact contact);

	/** Remove a contact from a group
	 * @return <code>true</code> if the contact was removed from the group, <code>false</code> if he was not a member */
	public boolean removeMembership(Group g, Contact contact);

	/** @return <code>true</code> if the contact is a member of the group, <code>false</code> otherwise */
	public boolean isMember(Group group, Contact contact);
}
