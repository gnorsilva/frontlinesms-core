/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.List;

import net.frontlinesms.junit.HibernateTestCase;

import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.repository.KeywordActionDao;
import net.frontlinesms.data.repository.KeywordDao;

import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateKeywordActionDao}
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class HibernateKeywordActionDaoTest extends HibernateTestCase {
//> PROPERTIES
	/** {@link KeywordActionDao} instance to test against. */
	private KeywordActionDao keywordActionDao;
	/** {@link KeywordDao} instance to test against. */
	private KeywordDao keywordDao;
	
	private Keyword testKeyword;
	private Keyword testKeyword2;

//> TEST METHODS
	public void test() {
		KeywordAction action = KeywordAction.createReplyAction(this.testKeyword, "some reply text", 14343274L, 21340345L);
		this.keywordActionDao.saveKeywordAction(action);
		
		assertEquals(action, this.keywordActionDao.getAction(testKeyword, KeywordAction.Type.TYPE_REPLY));
		List<KeywordAction> retrievedActionList = this.keywordActionDao.getActions(testKeyword);
		assertEquals(1, retrievedActionList.size());
		assertEquals(action, retrievedActionList.get(0));
		
		this.keywordActionDao.deleteKeywordAction(action);
		
		assertEquals(0, this.keywordActionDao.getActions(testKeyword).size());
		assertNull(this.keywordActionDao.getAction(testKeyword, KeywordAction.Type.TYPE_REPLY));
	}
	
	public void testKeywordActionsCount () {
		KeywordAction action = KeywordAction.createReplyAction(this.testKeyword, "some reply text", 14343274L, 21340345L);
		KeywordAction action2 = KeywordAction.createReplyAction(this.testKeyword, "some reply text 2", 14343274L, 21340345L);
		KeywordAction action3 = KeywordAction.createReplyAction(this.testKeyword2, "some reply text for keyword 2", 14343274L, 21340345L);
		KeywordAction action4 = KeywordAction.createEmailAction(this.testKeyword2, "Reply Text", null, "", "", 14343274L, 21340345L);
		
		this.keywordActionDao.saveKeywordAction(action);
		this.keywordActionDao.saveKeywordAction(action2);
		this.keywordActionDao.saveKeywordAction(action3);
		
		int expectedCount = 3;
		int actualCount = this.keywordActionDao.getTotalCount();
		assertEquals(expectedCount, actualCount);
		
		this.keywordActionDao.deleteKeywordAction(action2);
		expectedCount = 2;
		actualCount = this.keywordActionDao.getTotalCount();
		assertEquals(expectedCount, actualCount);
		
		this.keywordActionDao.deleteKeywordAction(action3);
		expectedCount = 1;
		actualCount = this.keywordActionDao.getTotalCount();
		assertEquals(expectedCount, actualCount);
		
		this.keywordActionDao.deleteKeywordAction(action);
		this.keywordActionDao.saveKeywordAction(action4);
		expectedCount = 1;
		actualCount = this.keywordActionDao.getTotalCount();
		assertEquals(expectedCount, actualCount);
	}
	
//> INIT METHODS
	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		
		this.testKeyword = new Keyword("test", "test keyword");
		this.testKeyword2 = new Keyword("test2", "test keyword 2");
		
		this.keywordDao.saveKeyword(this.testKeyword);
		this.keywordDao.saveKeyword(this.testKeyword2);
	}
	
//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setKeywordActionDao(KeywordActionDao d) {
		this.keywordActionDao = d;
	}
	/** @param d The DAO to use for the test. */
	@Required
	public void setKeywordDao(KeywordDao d) {
		this.keywordDao = d;
	}
}
