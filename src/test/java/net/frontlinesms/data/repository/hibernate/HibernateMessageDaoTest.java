/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.List;

import net.frontlinesms.junit.HibernateTestCase;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.Message;
import net.frontlinesms.data.domain.Message.Type;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.data.repository.MessageDao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateMessageDao}
 * @author Alex
 */
public class HibernateMessageDaoTest extends HibernateTestCase {
//> STATIC CONSTANTS
	private static final String ARTHUR = "+44123456789";
	private static final String BERNADETTE = "+447890123456";
	
//> INSTANCE PROPERTIES
	/** Logging object */
	private final Log log = LogFactory.getLog(getClass());
	/** Instance of this DAO implementation we are testing. */
	private MessageDao dao;
	/** Keyword DAO */
	private KeywordDao keywordDao;
	
//> TEST METHODS

	/**
	 * Test everything all at once!
	 */
	public void testSaveDeleteSimple() {
		checkSanity();
		
		long startTime = System.currentTimeMillis();
		Message m = Message.createIncomingMessage(startTime + 1000, ARTHUR, BERNADETTE, "Hello mate.");
		dao.saveMessage(m);
	
		checkSanity();
		assertEquals(1, dao.getSMSCount(0l, Long.MAX_VALUE));
		assertEquals(1, dao.getSMSCountForMsisdn(ARTHUR, 0l, Long.MAX_VALUE));
		assertEquals(1, dao.getSMSCountForMsisdn(BERNADETTE, 0l, Long.MAX_VALUE));
		assertEquals(0, dao.getSMSCountForMsisdn("whatever i am invented", 0l, Long.MAX_VALUE));
		assertEquals(0, dao.getMessageCount(Type.TYPE_OUTBOUND, 0l, Long.MAX_VALUE));
		assertEquals(1, dao.getMessageCount(Type.TYPE_RECEIVED, 0l, Long.MAX_VALUE));
		
		dao.deleteMessage(m);

		checkSanity();
		assertEquals(0, dao.getSMSCount(startTime, Long.MAX_VALUE));
	}
	
	/**
	 * Test {@link MessageDao#getMessagesForKeyword(int, net.frontlinesms.data.domain.Keyword)}.
	 * @throws DuplicateKeyException 
	 */
	public void testGetMessagesForKeyword() throws DuplicateKeyException {
		// Create a number of keywords and messages, and perform queries over them
		createKeywords("", "te", "test", "test complex");
		createMessages(
				"",															// -> ""
				"te",														// -> "te"
				"test",														// -> "test"
				"test complex",												// -> "test complex"
				"Here is a message that should show as blank.",				// -> ""
				"Test the test keyword with this message",					// -> "test"
				"Test Complex keyword behaviour with this message",			// -> "test complex"
				"test test test",											// -> "test"
				"don't test me"												// -> ""
				);

		testGetMessagesForKeyword("", 3);
		testGetMessagesForKeyword("te", 1);
		testGetMessagesForKeyword("test", 3);
		testGetMessagesForKeyword("test complex", 3);
	}
	
	/**
	 * Test individual values for {@link #testGetMessagesForKeyword()}
	 * @param keywordString the keyword string to match
	 * @param expectedMessageCount the expected number of incoming and outgoing messages.  Total messages should be twice this.
	 */
	private void testGetMessagesForKeyword(String keywordString, int expectedMessageCount) {
		Keyword keyword = new Keyword(keywordString, "Test keyword.");
		List<Message> allMessagesForBlankKeyword = dao.getMessagesForKeyword(Message.Type.TYPE_ALL, keyword);
		List<Message> incomingMessagesForBlankKeyword = dao.getMessagesForKeyword(Message.Type.TYPE_RECEIVED, keyword);
		List<Message> outgoingMessagesForBlankKeyword = dao.getMessagesForKeyword(Message.Type.TYPE_OUTBOUND, keyword);
		
		int allMessageCount = allMessagesForBlankKeyword.size();
		int incomingMessageCount = incomingMessagesForBlankKeyword.size();
		int outgoingMessageCount = outgoingMessagesForBlankKeyword.size();
		assertTrue(incomingMessageCount == outgoingMessageCount);
		assertTrue(allMessageCount == 2 * incomingMessageCount);
		assertEquals(expectedMessageCount, incomingMessageCount);	
	}
	
	/** TODO may not be necessary to create keywords. */
	private void createKeywords(String... keywordStrings) throws DuplicateKeyException {
		for(String keywordString : keywordStrings) {
			createKeyword(keywordString);
		}
	}
	
	private void createKeyword(String keywordString) throws DuplicateKeyException {
		Keyword k = new Keyword(keywordString, "generated for test in " + this.getClass().getName());
		this.keywordDao.saveKeyword(k);
	}
	
	private void createMessages(String... messageContents) {
		for(String messageContent : messageContents) {
			createIncomingMessage(messageContent);
			createOutgoingMessage(messageContent);
		}
	}

	private void createOutgoingMessage(String messageContent) {
		Message m = Message.createOutgoingMessage(0, "testSender", "testRecipient", messageContent);
		this.dao.saveMessage(m);
	}

	private void createIncomingMessage(String messageContent) {
		Message m = Message.createIncomingMessage(0, "testSender", "testRecipient", messageContent);
		this.dao.saveMessage(m);
	}

	//> INSTANCE HELPER METHODS
	/**
	 * Check that various methods agree with each other.
	 */
	private void checkSanity() {
		assertEquals(dao.getSMSCount(0l, Long.MAX_VALUE), dao.getAllMessages().size());
	}
//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setMessageDao(MessageDao d) {
		this.dao = d;
	}
	
	@Required
	public void setKeywordDao(KeywordDao keywordDao) {
		this.keywordDao = keywordDao;
	}
}
