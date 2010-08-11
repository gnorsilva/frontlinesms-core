/**
 * 
 */
package net.frontlinesms.ui.handler.message;

// TODO Remove static imports
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_NO_CONTACT_SELECTED;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_ESTIMATED_MONEY;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_FIRST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_HELP;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_MSG_NUMBER;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_REMAINING_CHARS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_SECOND;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_THIRD;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_TOO_MANY_MESSAGES;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_MESSAGE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_RECIPIENT;

import java.awt.Color;
import java.util.regex.Pattern;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.UiGeneratorControllerConstants;
import net.frontlinesms.ui.UiProperties;
import net.frontlinesms.ui.handler.contacts.ContactSelecter;
import net.frontlinesms.ui.handler.keyword.BaseActionDialog;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.apache.log4j.Logger;
import org.smslib.util.GsmAlphabet;

/**
 * Controller for a panel which allows sending of text SMS messages
 * @author Alex
 */
public class MessagePanelHandler implements ThinletUiEventHandler {
//> STATIC CONSTANTS
	/** UI XML File Path: the panel containing the messaging controls */
	protected static final String UI_FILE_MESSAGE_PANEL = "/ui/core/messages/pnComposeMessage.xml";
	
//> THINLET COMPONENTS
	/** Thinlet component name: Button to send message */
	private static final String COMPONENT_BT_SEND = "btSend";

//> INSTANCE PROPERTIES
	/** Logging obhect */
	private final Logger log = FrontlineUtils.getLogger(this.getClass());
	/** The {@link UiGeneratorController} that shows the tab. */
	private final UiGeneratorController uiController;
	/** The parent component */
	private Object messagePanel;
	/** The number of people the current SMS will be sent to */
	private int numberToSend = 1;
	/** The boolean stipulating whether the recipient field should be displayed */
	private boolean shouldDisplayRecipientField;
	/** The boolean stipulating whether we should check the length of the message (we don't in the auto-reply, for example) */
	private boolean shouldCheckMaxMessageLength;
	/** The number of recipients, used to estimate the cost of the message */
	private int numberOfRecipients;

//> CONSTRUCTORS
	/**
	 * @param uiController
	 */
	private MessagePanelHandler(UiGeneratorController uiController, boolean shouldDisplay, boolean shouldCheckMaxMessageLength, int numberOfRecipients) {
		this.uiController 				 = uiController;
		this.shouldDisplayRecipientField = shouldDisplay;
		this.shouldCheckMaxMessageLength = shouldCheckMaxMessageLength;
		this.numberOfRecipients 		 = numberOfRecipients; 
	}
	
	private synchronized void init() {
		assert(this.messagePanel == null) : "This has already been initialised.";
		this.messagePanel = uiController.loadComponentFromFile(UI_FILE_MESSAGE_PANEL, this);
		Object pnRecipient = find(UiGeneratorControllerConstants.COMPONENT_PN_MESSAGE_RECIPIENT);
		Object lbTooManyMessages = find(UiGeneratorControllerConstants.COMPONENT_LB_TOO_MANY_MESSAGES);
		uiController.setVisible(pnRecipient, shouldDisplayRecipientField);
		
		if (lbTooManyMessages != null) {
			uiController.setVisible(lbTooManyMessages, false);
			uiController.setColor(lbTooManyMessages, "foreground", Color.RED);
		}
		messageChanged("", "");
	}

	private Object find(String component) {
		return this.uiController.find(this.messagePanel, component);
	}

	//> ACCESSORS
	/** @return {@link #messagePanel} */
	public Object getPanel() {
		return this.messagePanel;
	}
	
	private double getCostPerSms() {
		return UiProperties.getInstance().getCostPerSms();
	}
	
	/**
	 * Adds a constant substitution marker to the SMS text.
	 * @param currentText 
	 * @param textArea 
	 * @param type The constant that should be inserted
	 */
	public void addConstantToCommand(String currentText, Object tfMessage, String type) {
		BaseActionDialog.addConstantToCommand(uiController, currentText, tfMessage, type);
		
		String recipient = uiController.getText(find(COMPONENT_TF_RECIPIENT));
		messageChanged(recipient, uiController.getText(tfMessage));
	}
	
//> THINLET UI METHODS
	/** Sets the method called by the send button at the bottom of the compose message panel */
	public void setSendButtonMethod(ThinletUiEventHandler eventHandler, Object rootComponent, String methodCall) {
		Object sendButton = find(COMPONENT_BT_SEND);
		uiController.setAction(sendButton, methodCall, rootComponent, eventHandler);
	}
	
	/**
	 * Extract message details from the controls in the panel, and send an SMS.
	 */
	public void send() {
		String recipient = uiController.getText(find(COMPONENT_TF_RECIPIENT));
		String message = uiController.getText(find(COMPONENT_TF_MESSAGE));
		
		if (recipient.length() == 0) {
			uiController.alert(InternationalisationUtils.getI18NString(FrontlineSMSConstants.MESSAGE_BLANK_PHONE_NUMBER));
			return;
		} 
		this.uiController.getFrontlineController().sendTextMessage(recipient, message);
		
		// We clear the components
		uiController.setText(find(COMPONENT_TF_RECIPIENT), "");
		uiController.setText(find(COMPONENT_TF_MESSAGE), "");
		uiController.setText(find(COMPONENT_LB_REMAINING_CHARS), String.valueOf(FrontlineMessage.SMS_LENGTH_LIMIT));
		uiController.setText(find(COMPONENT_LB_MSG_NUMBER), "0");
		uiController.setIcon(find(COMPONENT_LB_FIRST), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_SECOND), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS_DISABLED);
		uiController.setText(find(COMPONENT_LB_ESTIMATED_MONEY), InternationalisationUtils.formatCurrency(0));
		if (shouldCheckMaxMessageLength) // Otherwise this component doesn't exist
			uiController.setVisible(find(COMPONENT_LB_TOO_MANY_MESSAGES), false);

		Object sendButton = find(COMPONENT_BT_SEND);
		if (sendButton != null) uiController.setEnabled(sendButton, false);
	}
	
	/**
	 * Event triggered when the message recipient has changed
	 * @param text the new text value for the message recipient
	 * 
	 */
	public void recipientChanged(String recipient, String message) {
		int recipientLength = recipient.length(),
			messageLength = message.length();
		
		Object sendButton = find(COMPONENT_BT_SEND);
		
		int totalLengthAllowed;
		if(GsmAlphabet.areAllCharactersValidGSM(message))totalLengthAllowed = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT * FrontlineMessage.SMS_LIMIT;
		else totalLengthAllowed = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT_UCS2 * FrontlineMessage.SMS_LIMIT;
		
		boolean shouldEnableSendButton = ((!shouldCheckMaxMessageLength || messageLength <= totalLengthAllowed)
											&& recipientLength > 0
											&& messageLength > 0);
		if (sendButton != null)
			uiController.setEnabled(sendButton, shouldEnableSendButton);
	}
	
	/** Method which triggers showing of the contact selecter. */
	public void selectMessageRecipient() {
		ContactSelecter contactSelecter = new ContactSelecter(this.uiController);
		final boolean shouldHaveEmail = false;
		contactSelecter.show(InternationalisationUtils.getI18NString(FrontlineSMSConstants.SENTENCE_SELECT_MESSAGE_RECIPIENT_TITLE), "setRecipientTextfield(contactSelecter_contactList, contactSelecter)", null, this, shouldHaveEmail);
	}
	
	/**
	 * Sets the phone number of the selected contact.
	 * 
	 * This method is triggered by the contact selected, as detailed in {@link #selectMessageRecipient()}.
	 * 
	 * @param contactSelecter_contactList
	 * @param dialog
	 */
	public void setRecipientTextfield(Object contactSelecter_contactList, Object dialog) {
		Object 	tfRecipient = find(UiGeneratorControllerConstants.COMPONENT_TF_RECIPIENT),
				tfMessage	= find(UiGeneratorControllerConstants.COMPONENT_TF_MESSAGE);
		Object selectedItem = uiController.getSelectedItem(contactSelecter_contactList);
		if (selectedItem == null) {
			uiController.alert(InternationalisationUtils.getI18NString(FrontlineSMSConstants.MESSAGE_NO_CONTACT_SELECTED));
			return;
		}
		Contact selectedContact = uiController.getContact(selectedItem);
		uiController.setText(tfRecipient, selectedContact.getPhoneNumber());
		uiController.remove(dialog);
		uiController.updateCost();
		
		// The recipient text has changed, we check whether the send button should be enabled
		this.recipientChanged(uiController.getText(tfRecipient), uiController.getText(tfMessage));
	}
	
	/**
	 * Event triggered when the message details have changed
	 * @param panel TODO this should be removed
	 * @param message the new text value for the message body
	 * 
	 */
	public void messageChanged(String recipient, String message) {
		int recipientLength = recipient.length();
		int messageLength = message.length();
		
		Object sendButton = find(COMPONENT_BT_SEND);
		boolean areAllCharactersValidGSM = GsmAlphabet.areAllCharactersValidGSM(message);
		int totalLengthAllowed;
		if(areAllCharactersValidGSM) {
			totalLengthAllowed = FrontlineMessage.SMS_LENGTH_LIMIT + FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT * (FrontlineMessage.SMS_LIMIT - 1);
		} else {
			totalLengthAllowed = FrontlineMessage.SMS_LENGTH_LIMIT_UCS2 + FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT_UCS2 * (FrontlineMessage.SMS_LIMIT - 1);
		}
		
		boolean shouldEnableSendButton = (messageLength > 0 && (!shouldCheckMaxMessageLength || messageLength <= totalLengthAllowed)
											&& (!shouldDisplayRecipientField || recipientLength > 0));
		
		if (sendButton != null)
			uiController.setEnabled(sendButton, shouldEnableSendButton);
		
		int singleMessageCharacterLimit;
		int multipartMessageCharacterLimit;
		if(areAllCharactersValidGSM) {
			singleMessageCharacterLimit = FrontlineMessage.SMS_LENGTH_LIMIT;
			multipartMessageCharacterLimit = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT;
		} else {
			// It appears there are some unicode-only characters here.  We should therefore
			// treat this message as if it will be sent as unicode.
			singleMessageCharacterLimit = FrontlineMessage.SMS_LENGTH_LIMIT_UCS2;
			multipartMessageCharacterLimit = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT_UCS2;
		}
		
		Object 	tfMessage = find(COMPONENT_TF_MESSAGE),
				lbTooManyMessages = find(COMPONENT_LB_TOO_MANY_MESSAGES);

		int numberOfMsgs, remaining;
		double costEstimate;
		
		if (shouldCheckMaxMessageLength && messageLength > totalLengthAllowed) {
			remaining = 0;
			costEstimate = 0;
			numberOfMsgs = (int)Math.ceil((double)messageLength / (double)multipartMessageCharacterLimit);
			
			uiController.setVisible(lbTooManyMessages, true);
			uiController.setColor(tfMessage, "foreground", Color.RED);
		} else {
			if (shouldCheckMaxMessageLength) {
				uiController.setVisible(lbTooManyMessages, false);
				uiController.setColor(tfMessage, "foreground", Color.BLACK);
			}
			
			if (messageLength <= singleMessageCharacterLimit) {
				numberOfMsgs = messageLength == 0 ? 0 : 1;
				remaining = (messageLength % singleMessageCharacterLimit) == 0 ? 0
						: singleMessageCharacterLimit - (messageLength % singleMessageCharacterLimit);	
			} else {
				int charCount = messageLength - singleMessageCharacterLimit;
				numberOfMsgs = (int)Math.ceil((double)charCount / (double)multipartMessageCharacterLimit) + 1;
				remaining = (charCount % multipartMessageCharacterLimit) == 0 ? 0
						: multipartMessageCharacterLimit - ((charCount % multipartMessageCharacterLimit));
			}
			
			costEstimate = numberOfMsgs * this.getCostPerSms() * this.numberToSend;
		}
		
		// The message will actually cost {numberOfRecipients} times the calculated cost
		costEstimate *= numberOfRecipients;
		
		uiController.setText(find(COMPONENT_LB_REMAINING_CHARS), String.valueOf(remaining));
		uiController.setText(find(COMPONENT_LB_ESTIMATED_MONEY), InternationalisationUtils.formatCurrency(costEstimate));
		uiController.setVisible(find(COMPONENT_LB_HELP), false);
		
		uiController.setText(find(COMPONENT_LB_MSG_NUMBER), String.valueOf(numberOfMsgs));
		uiController.setIcon(find(COMPONENT_LB_FIRST), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_SECOND), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS_DISABLED);
		
		if (numberOfMsgs >= 1) uiController.setIcon(find(COMPONENT_LB_FIRST), Icon.SMS);
		if (numberOfMsgs >= 2) uiController.setIcon(find(COMPONENT_LB_SECOND), Icon.SMS);
		if (numberOfMsgs == 3) uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS);
		if (numberOfMsgs > 3) uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS_ADD);
		

		if (Pattern.matches(".*\\$[^ ]*\\}.*", message)) {
			uiController.setVisible(find(COMPONENT_LB_HELP), true);
		}
	}
	
	/**
	 * Sets the phone number of the selected contact.
	 * 
	 * @param contactSelecter_contactList
	 * @param dialog
	 */
	public void homeScreen_setRecipientTextfield(Object contactSelecter_contactList, Object dialog) {
		Object tfRecipient = find(COMPONENT_TF_RECIPIENT);
		Object selectedItem = uiController.getSelectedItem(contactSelecter_contactList);
		if (selectedItem == null) {
			uiController.alert(InternationalisationUtils.getI18NString(MESSAGE_NO_CONTACT_SELECTED));
			return;
		}
		Contact selectedContact = uiController.getContact(selectedItem);
		uiController.setText(tfRecipient, selectedContact.getPhoneNumber());
		uiController.remove(dialog);
		this.numberToSend = 1;
		uiController.updateCost();
	}

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES
	/**
	 * Create and initialise a new {@link MessagePanelHandler}.
	 * @return a new, initialised instance of {@link MessagePanelHandler}
	 */
	public static final MessagePanelHandler create(UiGeneratorController ui, boolean shouldDisplayRecipientField, boolean checkMaxMessageLength, int numberOfRecipients) {
		MessagePanelHandler handler = new MessagePanelHandler(ui, shouldDisplayRecipientField, checkMaxMessageLength, numberOfRecipients);
		handler.init();
		return handler;
	}

//> STATIC HELPER METHODS
}
