/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.domain.GroupMembership;
import net.frontlinesms.data.repository.GroupMembershipDao;

/**
 * @author aga
 *
 */
public class HibernateGroupMembershipDao extends BaseHibernateDao<GroupMembership> implements GroupMembershipDao {

	HibernateGroupMembershipDao() {
		super(GroupMembership.class);
	}
	
	/**
	 * @see GroupMembershipDao#addMembership(Group, Contact)
	 */
	public boolean addMembership(Group g, Contact contact) {
		GroupMembership membership = new GroupMembership(g, contact);
		try {
			super.save(membership);
			return true;
		} catch (DuplicateKeyException e) {
			return false;
		}
	}

	/**
	 * @see GroupMembershipDao#getActiveMembers(Group)
	 */
	public List<Contact> getActiveMembers(Group group) {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	/**
	 * @see GroupMembershipDao#getGroups(Contact)
	 */
	@SuppressWarnings("unchecked")
	public List<Group> getGroups(Contact contact) {
		return this.getHibernateTemplate().find("SELECT mem.group FROM GroupMembership AS mem WHERE mem.contact='" + contact.getId() + "'");
	}

	/**
	 * @see GroupMembershipDao#getMemberCount(Group)
	 */
	public int getMemberCount(Group group) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see GroupMembershipDao#getMembers(Group)
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> getMembers(Group group) {
		if(group.isRoot()) {
			return this.getHibernateTemplate().findByCriteria(DetachedCriteria.forClass(Contact.class));
		} else {
			return this.getHibernateTemplate().find("SELECT mem.contact FROM GroupMembership AS mem WHERE mem.group='" + group.getPath() + "'");
////			DetachedCriteria crit = DetachedCriteria.forClass(Contact.class);
////			crit.createAlias(GroupMembership.class.getName(), "mem");
////			crit.add(Restrictions.eq("mem.group", group));
////			return super.getHibernateTemplate().findByCriteria(crit);
//			
//			DetachedCriteria gmCrit = DetachedCriteria.forClass(GroupMembership.class);
//			gmCrit.add(Restrictions.eq("group", group));
//			
//			DetachedCriteria crit = DetachedCriteria.forClass(Contact.class);
//			crit.add(gmCrit);
////			DetachedCriteria gmCrit = crit.createCriteria("GroupMembership", "mem");
////			gmCrit.add(Restrictions.eq("mem.group", group));
////			return super.getHibernateTemplate().findByCriteria(crit);
		}
	}

	/**
	 * @see GroupMembershipDao#getMembers(Group, int, int)
	 */
	public List<Contact> getMembers(Group group, int startIndex, int limit) {
		// TODO Auto-generated method stub
		List<Contact> allMembers = getMembers(group);
		return allMembers.subList(startIndex, Math.min(allMembers.size(), startIndex + limit));
	}

	/**
	 * @see GroupMembershipDao#isMember(Group, Contact)
	 */
	public boolean isMember(Group group, Contact contact) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see GroupMembershipDao#removeMembership(Group, Contact)
	 */
	public boolean removeMembership(Group g, Contact contact) {
		GroupMembership membership = new GroupMembership(g, contact);
		try {
			super.delete(membership);
			return true;
		} catch(DataAccessException ex) {
			return false;
		}
	}

}
