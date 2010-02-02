/**
 * 
 */
package net.frontlinesms.data.repository.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.repository.KeywordDao;

/**
 * In-memory implementation of {@link KeywordDao}.
 * @author Alex
 */
public class InMemoryKeywordDao implements KeywordDao {
	/** All the keywords that we have saved. */
	private HashSet<Keyword> allKeywords = new HashSet<Keyword>();
	
	/** @see KeywordDao#deleteKeyword(Keyword) */
	public void deleteKeyword(Keyword keyword) {
		this.allKeywords.remove(keyword);
	}

	/** @see KeywordDao#getAllKeywords() */
	public List<Keyword> getAllKeywords() {
		TreeMap<String, Keyword> sortedKeywords = new TreeMap<String, Keyword>();
		for(Keyword k : allKeywords) {
			sortedKeywords.put(k.getKeyword(), k);
		}
		ArrayList<Keyword> keywordList = new ArrayList<Keyword>();
		keywordList.addAll(sortedKeywords.values());
		return keywordList;
	}

	/** @see net.frontlinesms.data.repository.KeywordDao#getAllKeywords(int, int) */
	public List<Keyword> getAllKeywords(int startIndex, int limit) {
		List<Keyword> allKeywords = this.getAllKeywords();
		return allKeywords.subList(startIndex, Math.min(allKeywords.size(), startIndex+limit));
	}

	/** @see net.frontlinesms.data.repository.KeywordDao#getFromMessageText(java.lang.String) */
	public synchronized Keyword getFromMessageText(String messageText) {
		Keyword matchingKeyword = null;
		Keyword blankKeyword = null;
		// Trim the message text - leading and trailing whitespace are not relevant to keyword matching
		messageText = messageText.trim();
		for(Keyword k : this.allKeywords) {
			// Check if this keyword matches the message text
			if(k.matches(messageText)) {
				// Check if the previously matched keyword is longer than this one
				assert(matchingKeyword == null || matchingKeyword.getKeyword().length() != k.getKeyword().length()):"Two keywords cannot both match if they are the same length.";
				if(matchingKeyword == null || matchingKeyword.getKeyword().length() < k.getKeyword().length()) {
					matchingKeyword = k;
				}
			}
			
			if(k.getKeyword().equals("")) {
				// Cached the blank keyword in case we need to return it
				blankKeyword = k;
			}
		}
		
		if(matchingKeyword == null) {
			matchingKeyword = blankKeyword;
		}
		
		return matchingKeyword;
	}

	/** @see net.frontlinesms.data.repository.KeywordDao#getTotalKeywordCount() */
	public int getTotalKeywordCount() {
		return this.allKeywords.size();
	}

	/** @see KeywordDao#saveKeyword(Keyword) */
	public synchronized void saveKeyword(Keyword keyword) throws DuplicateKeyException {
		if(this.allKeywords.contains(keyword)) {
			// This keyword already exists, so throw a DuplicateKeyException
			throw new DuplicateKeyException();
		}
		this.allKeywords.add(keyword);
	}
}
