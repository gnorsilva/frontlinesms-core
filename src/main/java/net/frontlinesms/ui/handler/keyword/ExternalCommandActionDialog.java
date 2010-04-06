/**
 * 
 */
package net.frontlinesms.ui.handler.keyword;

import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_NO_GROUP_SELECTED_TO_FWD;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_START_DATE_AFTER_END;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_AUTO_REPLY;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_MESSAGE;

import java.util.List;

import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.domain.KeywordAction.ExternalCommandResponseActionType;
import net.frontlinesms.data.domain.KeywordAction.ExternalCommandResponseType;
import net.frontlinesms.data.domain.KeywordAction.ExternalCommandType;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author aga
 *
 */
public class ExternalCommandActionDialog extends BaseActionDialog {
	/** UI XML Layout file for editing external command actions */
	private static final String UI_FILE_NEW_KACTION_EXTERNAL_COMMAND_FORM = "/ui/core/keyword/dgEditExternalCommandAction.xml";
	
	private static final String COMPONENT_EXTERNAL_COMMAND_GROUP_LIST = "groupList";
	private static final String COMPONENT_PN_RESPONSE = "pnResponse";
	private static final String COMPONENT_RB_FRONTLINE_COMMANDS = "rbFrontlineCommands";
	private static final String COMPONENT_RB_PLAIN_TEXT = "rbPlainText";
	private static final String COMPONENT_RB_NO_RESPONSE = "rbNoResponse";
	private static final String COMPONENT_RB_TYPE_COMMAND_LINE = "rbTypeCL";
	private static final String COMPONENT_RB_TYPE_HTTP = "rbTypeHTTP";
	private static final String COMPONENT_TF_COMMAND = "tfCommand";
	private static final String COMPONENT_CB_FORWARD = "cbForward";
	
	/** DAO for {@link Group}s */
	private final GroupDao groupDao;

//> CONSTRUCTORS
	/**
	 * Create a new instance, setting required fields.
	 * @param ui the UI which this is tied to
	 * @param owner the {@link KeywordTabHandler} which spawned this
	 */
	ExternalCommandActionDialog(UiGeneratorController ui, KeywordTabHandler owner) {
		super(ui, owner);
		this.groupDao = ui.getFrontlineController().getGroupDao();
	}

	/** @see net.frontlinesms.ui.handler.keyword.BaseActionDialog#_init() */
	@Override
	protected void _init() {
		//Adds the date panel to it
		addDatePanel(super.getDialogComponent());
		Object list = find(COMPONENT_EXTERNAL_COMMAND_GROUP_LIST);
		List<Group> userGroups = this.groupDao.getAllGroups();
		for (Group g : userGroups) {
			log.debug("Adding group [" + g.getName() + "] to list");
			Object item = ui.createListItem(g.getName(), g);
			ui.setIcon(item, Icon.GROUP);
			ui.add(list, item);
		}
		
		if(isEditing()) {
			KeywordAction action = super.getTargetObject(KeywordAction.class);
			
			//COMMAND TYPE
			ui.setSelected(find(COMPONENT_RB_TYPE_HTTP), action.getExternalCommandType() == KeywordAction.ExternalCommandType.EXTERNAL_HTTP_REQUEST);
			ui.setSelected(find(COMPONENT_RB_TYPE_COMMAND_LINE), action.getExternalCommandType() == KeywordAction.ExternalCommandType.EXTERNAL_COMMAND_LINE);
			
			//COMMAND
			ui.setText(find(COMPONENT_TF_COMMAND), action.getUnformattedCommand());
			
			Object pnResponse = find(COMPONENT_PN_RESPONSE);
			//RESPONSE TYPE
			if (action.getExternalCommandResponseType() == ExternalCommandResponseType.EXTERNAL_RESPONSE_PLAIN_TEXT) {
				log.debug("Setting up dialog for PLAIN TEXT response.");
				ui.setSelected(find(COMPONENT_RB_PLAIN_TEXT), true);
				ui.setSelected(find(COMPONENT_RB_FRONTLINE_COMMANDS), false);
				ui.setSelected(find(COMPONENT_RB_NO_RESPONSE), false);
				
				ui.activate(pnResponse);
				ui.deactivate(list);
				//RESPONSE PANEL
				ui.setText(find(COMPONENT_TF_MESSAGE), action.getUnformattedCommandText());
				ExternalCommandResponseActionType responseActionType = action.getCommandResponseActionType();
				ui.setSelected(find(COMPONENT_CB_AUTO_REPLY),
							responseActionType == KeywordAction.ExternalCommandResponseActionType.TYPE_REPLY || responseActionType == KeywordAction.ExternalCommandResponseActionType.EXTERNAL_REPLY_AND_FORWARD);
			
				if (responseActionType == KeywordAction.ExternalCommandResponseActionType.TYPE_FORWARD || responseActionType == KeywordAction.ExternalCommandResponseActionType.EXTERNAL_REPLY_AND_FORWARD) {
					ui.setSelected(find(COMPONENT_CB_FORWARD), true);
					ui.activate(list);
					//Select group
					Group g = action.getGroup();
					for (Object item : ui.getItems(list)) {
						Group it = ui.getGroup(item);
						if (it.equals(g)) {
							log.debug("Selecting group [" + g.getName() + "].");
							ui.setSelected(item, true);
							break;
						}
					}
				}
			} else if (action.getExternalCommandResponseType() == ExternalCommandResponseType.EXTERNAL_RESPONSE_LIST_COMMANDS) {
				log.debug("Setting up dialog for LIST COMMANDS response.");
				ui.setSelected(find(COMPONENT_RB_PLAIN_TEXT), false);
				ui.setSelected(find(COMPONENT_RB_FRONTLINE_COMMANDS), true);
				ui.setSelected(find(COMPONENT_RB_NO_RESPONSE), false);
				ui.deactivate(pnResponse);
			} else {
				log.debug("Setting up dialog for NO response.");
				ui.setSelected(find(COMPONENT_RB_PLAIN_TEXT), false);
				ui.setSelected(find(COMPONENT_RB_FRONTLINE_COMMANDS), false);
				ui.setSelected(find(COMPONENT_RB_NO_RESPONSE), true);
				ui.deactivate(pnResponse);
			}
			
			initDateFields();
		}
	}
	
	/** @see net.frontlinesms.ui.handler.keyword.BaseActionDialog#getLayoutFilePath() */
	@Override
	protected String getLayoutFilePath() {
		return UI_FILE_NEW_KACTION_EXTERNAL_COMMAND_FORM;
	}
	
	@Override
	protected void handleRemoved() {
		// no special action required
	}

//> UI EVENT METHODS
	/**
	 * Creates a new forward message action.
	 */
	public void save() {
		log.trace("ENTER");

		long start, end;
		try {
			start = getEnteredStartDate();
			end = getEnteredEndDate();
		} catch(DialogValidationException ex) {
			ui.alert(ex.getUserMessage());
			return;
		}
		if(end < start) {
			log.debug("Start date is not before the end date");
			ui.alert(InternationalisationUtils.getI18NString(MESSAGE_START_DATE_AFTER_END));
			log.trace("EXIT");
			return;
		}
		
		ExternalCommandType commandType = ui.isSelected(find(COMPONENT_RB_TYPE_HTTP)) ? KeywordAction.ExternalCommandType.EXTERNAL_HTTP_REQUEST : KeywordAction.ExternalCommandType.EXTERNAL_COMMAND_LINE;
		String commandLine = ui.getText(find(COMPONENT_TF_COMMAND));
		ExternalCommandResponseType responseType = ExternalCommandResponseType.EXTERNAL_RESPONSE_DONT_WAIT;
		if (ui.isSelected(find(COMPONENT_RB_PLAIN_TEXT))) {
			responseType = ExternalCommandResponseType.EXTERNAL_RESPONSE_PLAIN_TEXT;
		} else if (ui.isSelected(find(COMPONENT_RB_FRONTLINE_COMMANDS))) {
			responseType = ExternalCommandResponseType.EXTERNAL_RESPONSE_LIST_COMMANDS;
		}
		
		log.debug("Command type [" + commandType + "]");
		log.debug("Command [" + commandLine + "]");
		log.debug("Response type [" + responseType + "]");
		
		Group group = null;
		String message = null;
		KeywordAction.ExternalCommandResponseActionType responseActionType = KeywordAction.ExternalCommandResponseActionType.EXTERNAL_DO_NOTHING; 
		if (responseType == ExternalCommandResponseType.EXTERNAL_RESPONSE_PLAIN_TEXT) {
			boolean reply = ui.isSelected(find(COMPONENT_CB_AUTO_REPLY));
			boolean fwd = ui.isSelected(find(COMPONENT_CB_FORWARD));
			
			if (reply && fwd) {
				responseActionType = KeywordAction.ExternalCommandResponseActionType.EXTERNAL_REPLY_AND_FORWARD;
			} else if (reply) {
				responseActionType = KeywordAction.ExternalCommandResponseActionType.TYPE_REPLY;
			} else if (fwd) {
				responseActionType = KeywordAction.ExternalCommandResponseActionType.TYPE_FORWARD;
			}
			log.debug("Response Action type [" + responseActionType + "]");
			if (responseActionType == KeywordAction.ExternalCommandResponseActionType.TYPE_REPLY 
					|| responseActionType == KeywordAction.ExternalCommandResponseActionType.TYPE_FORWARD
					|| responseActionType == KeywordAction.ExternalCommandResponseActionType.EXTERNAL_REPLY_AND_FORWARD) {
				message = ui.getText(find(COMPONENT_TF_MESSAGE));
				log.debug("Message [" + message + "]");
			}
			if (responseActionType == KeywordAction.ExternalCommandResponseActionType.TYPE_FORWARD 
					|| responseActionType == KeywordAction.ExternalCommandResponseActionType.EXTERNAL_REPLY_AND_FORWARD) {
				group = ui.getGroup(ui.getSelectedItem(find(COMPONENT_EXTERNAL_COMMAND_GROUP_LIST)));
				if (group == null) {
					log.debug("No group selected to forward");
					ui.alert(InternationalisationUtils.getI18NString(MESSAGE_NO_GROUP_SELECTED_TO_FWD));
					log.trace("EXIT");
					return;
				}
				log.debug("Group [" + group.getName() + "]");
			}
		}
		KeywordAction action = null;
		boolean isNew = false;
		if (isEditing()) {
			//Editing
			action = super.getTargetObject(KeywordAction.class);
			log.debug("We are editing action [" + action + "]. Setting new values.");
			if (group != null) {
				action.setGroup(group);
			}
			action.setCommandLine(commandLine);
			action.setExternalCommandType(commandType);
			action.setExternalCommandResponseType(responseType);
			action.setCommandResponseActionType(responseActionType);
			action.setCommandText(message);
			action.setStartDate(start);
			action.setEndDate(end);
			super.update(action);
		} else {
			isNew = true;
			Keyword keyword = super.getTargetObject(Keyword.class);
			log.debug("Creating new keyword action for keyword [" + keyword.getKeyword() + "]");
			action = KeywordAction.createExternalCommandAction(
					keyword,
					commandLine,
					commandType,
					responseType,
					responseActionType,
					message,
					group,
					start,
					end
			);
			super.save(action);
		}
		updateKeywordActionList(action, isNew);
		
		removeDialog();
		log.trace("EXIT");
	}
	
	/**
	 * Activates or deactivates the group selection list.
	 * @param selected
	 */
	public void handleForwardChanged(boolean selected) {
		activateGroupList(selected);
	}
	
	public void setResponseType(String type) {
		if(type.equals("simple")) {
			// Enable the response panel
			ui.activate(find(COMPONENT_PN_RESPONSE));
			
			// Enable or disable the group list as appropriate
			boolean forwardCheckboxChecked = ui.isSelected(find(COMPONENT_CB_FORWARD));
			if(forwardCheckboxChecked) {
				activateGroupList(true);
			}
		} else {
			// Disable the response panel
			ui.deactivate(find(COMPONENT_PN_RESPONSE));
		}
	}
	
//> UI HELPER METHODS
	private void activateGroupList(boolean activate) {
		Object list = find(COMPONENT_EXTERNAL_COMMAND_GROUP_LIST);
		if (activate) {
			ui.activate(list);
		} else {
			ui.deactivate(list);
		}
	}
	
//> UI PASSTHROUGH METHODS
	/** @see UiGeneratorController#setText(Object, String) */
	public void setText(Object component, String value) {
		this.ui.setText(component, value);
	}
	/** @see UiGeneratorController#activate(Object) */
	public void activate(Object component) {
		this.ui.activate(component);
	}
	/** @see UiGeneratorController#deactivate(Object) */
	public void deactivate(Object component) {
		this.ui.deactivate(component);
	}
}
