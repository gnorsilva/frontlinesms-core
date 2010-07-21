/**
 * 
 */
package net.frontlinesms.data.domain;

import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 */
@Entity
public class FrontlineMultimediaMessage extends FrontlineMessage {
	private String subject;
	
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	private List<FrontlineMultimediaMessagePart> multimediaParts;
	
	FrontlineMultimediaMessage() {}
	public FrontlineMultimediaMessage(Type type, String subject, String textContent, List<FrontlineMultimediaMessagePart> multimediaParts) {
		super(type, textContent);
		
		this.subject = subject;
		this.multimediaParts = multimediaParts;
	}
	
	public List<FrontlineMultimediaMessagePart> getMultimediaParts() {
		return Collections.unmodifiableList(this.multimediaParts);
	}
	public boolean hasBinaryPart() {
		for (FrontlineMultimediaMessagePart part : this.multimediaParts) {
			if (part.isBinary()) {
				return true;
			}
		}
		
		return false;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getSubject() {
		return subject;
	}
}
