/**
 * 
 */
package net.frontlinesms.ui.handler.keyword;

import static net.frontlinesms.FrontlineSMSConstants.ACTION_ADD_KEYWORD;
import static net.frontlinesms.FrontlineSMSConstants.ACTION_CREATE;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_BLANK;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_EDITING_KEYWORD;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_KEYWORD_ACTIONS_OF;
import static net.frontlinesms.FrontlineSMSConstants.DEFAULT_END_DATE;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_KEYWORD_EXISTS;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_KEYWORD_SAVED;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_ACTION_LIST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_BT_CLEAR;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_BT_SAVE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_ACTION_TYPE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_AUTO_REPLY;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_GROUPS_TO_JOIN;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_GROUPS_TO_LEAVE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_JOIN_GROUP;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_LEAVE_GROUP;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_KEYWORDS_DIVIDER;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_KEYWORD_LIST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_KEY_ACT_PANEL;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_KEY_PANEL;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_MENU_ITEM_CREATE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_BUTTON_DONE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_FORM_DESCRIPTION;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_FORM_KEYWORD;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_FORM_TITLE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_PN_TIP;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_AUTO_REPLY;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_KEYWORD;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import thinlet.Thinlet;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.data.repository.KeywordActionDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.BaseTabHandler;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author Alex Anderson 
 * <li> alex(at)masabi(dot)com
 * @author Carlos Eduardo Genz
 * <li> kadu(at)masabi(dot)com
 */
public class KeywordTabHandler extends BaseTabHandler {
//> UI LAYOUT FILES
	public static final String UI_FILE_KEYWORDS_TAB = "/ui/core/keyword/keywordsTab.xml";
	public static final String UI_FILE_KEYWORDS_SIMPLE_VIEW = "/ui/core/keyword/pnSimpleView.xml";
	public static final String UI_FILE_KEYWORDS_ADVANCED_VIEW = "/ui/core/keyword/pnAdvancedView.xml";
	public static final String UI_FILE_NEW_KEYWORD_FORM = "/ui/core/keyword/newKeywordForm.xml";

	private GroupDao groupDao;
	private KeywordDao keywordDao;
	private KeywordActionDao keywordActionDao;

	private Object keywordListComponent;
	
	public KeywordTabHandler(UiGeneratorController ui, FrontlineSMS frontlineController) {
		super(ui);

		this.groupDao = frontlineController.getGroupDao();
		this.keywordDao = frontlineController.getKeywordDao();
		this.keywordActionDao = frontlineController.getKeywordActionDao(); 
	}

	protected Object initialiseTab() {
		Object tabComponent = ui.loadComponentFromFile(UI_FILE_KEYWORDS_TAB, this);
		this.keywordListComponent = ui.find(tabComponent, COMPONENT_KEYWORD_LIST);
		return tabComponent;
	}

	public void refresh() {
		updateKeywordList();
	}
	
//> UI EVENT METHODS
	/**
	 * Shows the export wizard dialog for exporting contacts.
	 * @param list The list to get selected items from.
	 */
	public void showExportWizard(Object list) {
		this.ui.showExportWizard(list, "keywords");
	}
	
	public void autoReplyChanged(String reply, Object cbAutoReply) {
		ui.setSelected(cbAutoReply, reply.length() > 0);
	}
	
	/**
	 * @param index
     *  0 - Auto Reply
     *  1 - Auto Forward
     *  2 - Join Group
     *  3 - Leave Group
     *  4 - E-mail
     *  5 - External Command 
     */
	public void keywordTab_createAction(int index) {
		switch (index) {
		case 0:
			show_newKActionReplyForm(keywordListComponent);
			break;
		case 1:
			show_newKActionForwardForm(keywordListComponent);
			break;
		case 2:
			show_newKActionJoinForm(keywordListComponent);
			break;
		case 3:
			show_newKActionLeaveForm(keywordListComponent);
			break;
		case 4:
			show_newKActionEmailForm(keywordListComponent);
			break;
		case 5:
			show_newKActionExternalCmdForm(keywordListComponent);
			break;
		}
	}
	
	/**
	 * Shows the new forward message action dialog.
	 * @param keywordList
	 */
	public void show_newKActionForwardForm(Object keywordList) {
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordList));
		ForwardActionDialog dialog = new ForwardActionDialog(ui, this);
		dialog.init(keyword);
		dialog.show();
	}

	/**
	 * Shows the new auto reply action dialog.
	 * 
	 * @param keywordList
	 */
	public void show_newKActionReplyForm(Object keywordList) {
		ReplyActionDialogHandler replyActionDialogHandler = new ReplyActionDialogHandler(ui, this);
		replyActionDialogHandler.init(ui.getKeyword(ui.getSelectedItem(keywordList)));
		replyActionDialogHandler.show();
	}
	
	/**
	 * Shows the new email action dialog.
	 * 
	 * @param keywordList
	 */
	public void show_newKActionEmailForm(Object keywordList) {
		log.trace("ENTER");
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordList));
		EmailActionDialog dialog = new EmailActionDialog(ui, this);
		dialog.init(keyword);
		dialog.show();
		log.trace("EXIT");
	}

	public void keywordShowAdvancedView() {
		Object divider = find(COMPONENT_KEYWORDS_DIVIDER);
		if (ui.getItems(divider).length >= 2) {
			ui.remove(ui.getItems(divider)[ui.getItems(divider).length - 1]);
		}
		Object panel = ui.loadComponentFromFile(UI_FILE_KEYWORDS_ADVANCED_VIEW, this);
		Object table = ui.find(panel, COMPONENT_ACTION_LIST);
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordListComponent));
		String key = keyword.getKeyword().length() == 0 ? "<" + InternationalisationUtils.getI18NString(COMMON_BLANK) + ">" : keyword.getKeyword();
		ui.setText(panel, InternationalisationUtils.getI18NString(COMMON_KEYWORD_ACTIONS_OF, key));
		for (KeywordAction action : this.keywordActionDao.getActions(keyword)) {
			ui.add(table, ui.getRow(action));
		}
		enableKeywordActionFields(table, ui.find(panel, COMPONENT_KEY_ACT_PANEL));
		ui.add(divider, panel);
	}
	
	/**
	 * UI Method.
	 * Deletes the keyword that is selected in {@link #keywordListComponent}.
	 */
	public void removeSelectedFromKeywordList() {
		// Get the selected keyword
		Object selected = ui.getSelectedItem(keywordListComponent);
		Keyword keyword = ui.getAttachedObject(selected, Keyword.class);
		
		// Delete attached actions, and then delete they keyword
		for(KeywordAction action : this.keywordActionDao.getActions(keyword)) {
			this.keywordActionDao.deleteKeywordAction(action);
		}
		this.keywordDao.deleteKeyword(keyword);

		// Now update the UI - remove the selected item and set a new selected item
		ui.remove(selected);
		ui.setSelectedIndex(keywordListComponent, 0);
		showSelectedKeyword();

		// Finally, remove the "confirm delete" dialog
		ui.removeConfirmationDialog();
	}
	
	/**
	 * Event fired when the popup menu (in the keyword manager tab) is shown.
	 * If there is no keyword listed in the tree, the only option allowed is
	 * to create one. Otherwise, all components are allowed.
	 */
	public void enableKeywordFields(Object component) {
		log.trace("ENTER");
		int selected = ui.getSelectedIndex(keywordListComponent);
		String field = Thinlet.getClass(component) == Thinlet.PANEL ? Thinlet.ENABLED : Thinlet.VISIBLE;
		if (selected <= 0) {
			log.debug("Nothing selected, so we only allow keyword creation.");
			for (Object o : ui.getItems(component)) {
				String name = ui.getString(o, Thinlet.NAME);
				if (name == null)
					continue;
				if (!name.equals(COMPONENT_MENU_ITEM_CREATE)) {
					ui.setBoolean(o, field, false);
				} else {
					ui.setBoolean(o, field, true);
				}
			}
		} else {
			//Keyword selected
			for (Object o : ui.getItems(component)) {
				ui.setBoolean(o, field, true);
			}
		}
		log.trace("EXIT");
	}
	
	public void keywordTab_newAction(Object combo) {
		keywordTab_createAction(ui.getSelectedIndex(combo));
	}

	/**
	 * Shows the new keyword dialog.
	 * 
	 * @param keywordList
	 */
	public void show_createKeywordForm(Object keywordList) {
		showNewKeywordForm(ui.getKeyword(ui.getSelectedItem(keywordList)));
	}

	/**
	 * Create a new keyword with the supplied information (newKeyword and description).
	 * 
	 * @param formPanel The panel to be removed from the application.
	 * @param newKeyword The desired keyword.
	 * @param description The description for this new keyword.
	 */
	public void do_createKeyword(Object formPanel, String newKeyword, String description) {
		log.trace("ENTER");
		log.debug("Creating keyword [" + newKeyword + "] with description [" + description + "]");
		try {
			// Trim the keyword to remove trailing and leading whitespace
			newKeyword = newKeyword.trim();
			// Remove any double-spaces within the keyword
			newKeyword = newKeyword.replaceAll("\\s+", " ");
			
			Keyword keyword = new Keyword(newKeyword, description);
			this.keywordDao.saveKeyword(keyword);
		} catch (DuplicateKeyException e) {
			ui.alert(InternationalisationUtils.getI18NString(MESSAGE_KEYWORD_EXISTS));
			log.trace("EXIT");
			return;
		}
		updateKeywordList();
		ui.remove(formPanel);
		log.trace("EXIT");
	}
	
	public void keywordTab_doSave(Object panel) {
		log.trace("ENTER");
		long startDate;
		try {
			startDate = InternationalisationUtils.parseDate(InternationalisationUtils.getDefaultStartDate()).getTime();
		} catch (ParseException e) {
			log.debug("We never should get this", e);
			log.trace("EXIT");
			return;
		}
		
		// Get the KeywordAction details
		String replyText = keywordSimple_getAutoReply(panel);
		Group joinGroup = keywordSimple_getJoin(panel);
		Group leaveGroup = keywordSimple_getLeave(panel);
		
		// Get the keyword attached to the selected item.  If the "Add Keyword" option is selected,
		// there will be no keyword attached to it.
		Keyword keyword = null;
		Object selectedKeywordItem = ui.getSelectedItem(keywordListComponent);
		if(selectedKeywordItem != null) keyword = ui.getKeyword(selectedKeywordItem);
		
		if (keyword == null) {
			//Adding keyword as well as actions
			String newkeyword = ui.getText(ui.find(panel, COMPONENT_TF_KEYWORD));
			try {
				keyword = new Keyword(newkeyword, "");
				this.keywordDao.saveKeyword(keyword);
			} catch (DuplicateKeyException e) {
				ui.alert(InternationalisationUtils.getI18NString(MESSAGE_KEYWORD_EXISTS));
				log.trace("EXIT");
				return;
			}
			keywordTab_doClear(panel);
		} else {
			// Editing an existent keyword.  This keyword may already have actions applied to it, so
			// we need to check for actions and update them as appropriate.
			KeywordAction replyAction = this.keywordActionDao.getAction(keyword, KeywordAction.TYPE_REPLY);
			if (replyAction != null) {
				if (replyText == null) {
					// The reply action has been removed
					keywordActionDao.deleteKeywordAction(replyAction);
				} else {
					replyAction.setReplyText(replyText);
					this.keywordActionDao.updateKeywordAction(replyAction);
					//We set null to don't add it in the end
					replyText = null;
				}
			}
			
			KeywordAction joinAction = this.keywordActionDao.getAction(keyword, KeywordAction.TYPE_JOIN);
			if (joinAction != null) {
				if (joinGroup == null) {
					// Previous join action has been removed, so delete it.
					keywordActionDao.deleteKeywordAction(joinAction);
				} else {
					// Group to join has been updated
					joinAction.setGroup(joinGroup);
					this.keywordActionDao.updateKeywordAction(joinAction);
					// Join Group has been handled, so unset it.
					joinGroup = null;
				}
			}
			
			KeywordAction leaveAction = this.keywordActionDao.getAction(keyword, KeywordAction.TYPE_LEAVE);
			if (leaveAction != null) {
				if (leaveGroup == null) {
					keywordActionDao.deleteKeywordAction(leaveAction);
				} else {
					leaveAction.setGroup(leaveGroup);
					this.keywordActionDao.updateKeywordAction(leaveAction);
					//We set null to don't add it in the end
					leaveGroup = null;
				}
			}
		}
		
		// Handle creation of new KeywordActions if required
		if (replyText != null) {
			KeywordAction action = KeywordAction.createReplyAction(keyword, replyText, startDate, DEFAULT_END_DATE);
			keywordActionDao.saveKeywordAction(action);
		}
		if (joinGroup != null) {
			KeywordAction action = KeywordAction.createGroupJoinAction(keyword, joinGroup, startDate, DEFAULT_END_DATE);
			keywordActionDao.saveKeywordAction(action);
		}
		if (leaveGroup != null) {
			KeywordAction action = KeywordAction.createGroupLeaveAction(keyword, leaveGroup, startDate, DEFAULT_END_DATE);
			keywordActionDao.saveKeywordAction(action);
		}
		
		// Refresh the UI
		updateKeywordList();
		ui.setStatus(InternationalisationUtils.getI18NString(MESSAGE_KEYWORD_SAVED));
		log.trace("EXIT");
	}

	/**
	 * Removes selected keyword action.
	 */
	public void removeSelectedFromKeywordActionsList() {
		ui.removeConfirmationDialog();
		Object list = find(COMPONENT_ACTION_LIST);
		Object selected = ui.getSelectedItem(list);
		KeywordAction keyAction = ui.getAttachedObject(selected, KeywordAction.class);
		this.keywordActionDao.deleteKeywordAction(keyAction);
		ui.remove(selected);
		enableKeywordActionFields(list, find(COMPONENT_KEY_ACT_PANEL));
	}

	/**
	 * Event fired when the popup menu (in the keyword manager tab) is shown.
	 * If there is no keyword action listed in the table, the only option allowed is
	 * to create one. Otherwise, all components are allowed.
	 */
	public void enableKeywordActionFields(Object table, Object component) {
		log.trace("ENTER");
		int selected = ui.getSelectedIndex(table);
		String field = Thinlet.getClass(component) == Thinlet.PANEL ? Thinlet.ENABLED : Thinlet.VISIBLE;
		if (selected < 0) {
			log.debug("Nothing selected, so we only allow keyword action creation.");
			for (Object o : ui.getItems(component)) {
				String name = ui.getString(o, Thinlet.NAME);
				if (name == null)
					continue;
				if (!name.equals(COMPONENT_MENU_ITEM_CREATE)
						&& !name.equals(COMPONENT_CB_ACTION_TYPE)) {
					ui.setBoolean(o, field, false);
				} else {
					ui.setBoolean(o, field, true);
				}
			}
		} else {
			//Keyword action selected
			for (Object o : ui.getItems(component)) {
				ui.setBoolean(o, field, true);
			}
		}
		log.trace("EXIT");
	}
	
	/**
	 * Shows the new join group action dialog.
	 * 
	 * @param keywordList
	 */
	public void show_newKActionJoinForm(Object keywordList) {
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordList));
		JoinGroupActionDialog dialog = new JoinGroupActionDialog(ui, this);
		dialog.init(keyword);
		dialog.show();
	}
	
	/**
	 * Shows the new leave group action dialog.
	 * 
	 * @param keywordList
	 */
	public void show_newKActionLeaveForm(Object keywordList) {
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordList));
		LeaveGroupActionDialog dialog = new LeaveGroupActionDialog(ui, this);
		dialog.init(keyword);
		dialog.show();
	}
	
	public void keywordTab_doClear(Object panel) {
		ui.setText(ui.find(panel, COMPONENT_TF_KEYWORD), "");
		ui.setSelected(ui.find(panel, COMPONENT_CB_AUTO_REPLY), false);
		ui.setText(ui.find(panel, COMPONENT_TF_AUTO_REPLY), "");
		ui.setSelected(ui.find(panel, COMPONENT_CB_JOIN_GROUP), false);
		ui.setSelectedIndex(ui.find(panel, COMPONENT_CB_GROUPS_TO_JOIN), 0);
		ui.setSelected(ui.find(panel, COMPONENT_CB_LEAVE_GROUP), false);
		ui.setSelectedIndex(ui.find(panel, COMPONENT_CB_GROUPS_TO_LEAVE), 0);
	}
	
	/**
	 * Method called when the user has selected the edit option inside the Keywords tab.
	 * 
	 * @param tree
	 */
	public void keywordManager_edit(Object tree) {
		log.trace("ENTER");
		Object selectedObj = ui.getSelectedItem(tree);
		if (ui.isAttachment(selectedObj, KeywordAction.class)) {
			//KEYWORD ACTION EDITION
			KeywordAction action = ui.getKeywordAction(selectedObj);
			log.debug("Editing keyword action [" + action + "]");
			showActionEditDialog(action);
		} else {
			Keyword keyword = ui.getKeyword(selectedObj);
			//KEYWORD EDITION
			log.debug("Editing keyword [" + keyword.getKeyword() + "]");
			showKeywordDialogForEdition(keyword);
		} 
		log.trace("EXIT");
	}
	
	/**
	 * Method called when the user has finished to edit a keyword.
	 * 
	 * @param dialog The dialog, which is holding the current reference to the keyword being edited.
	 * @param desc The new description for the keyword.
	 */
	public void finishKeywordEdition(Object dialog, String desc) {
		log.trace("ENTER");
		Keyword key = ui.getKeyword(dialog);
		log.debug("New description [" + desc + "] for keyword [" + key.getKeyword() + "]");
		key.setDescription(desc);
		ui.removeDialog(dialog);
		log.trace("EXIT");
	}

	public void showSelectedKeyword() {
		int index = ui.getSelectedIndex(keywordListComponent);
		Object selected = ui.getSelectedItem(keywordListComponent);
		Object divider = find(COMPONENT_KEYWORDS_DIVIDER);
		if (ui.getItems(divider).length >= 2) {
			ui.remove(ui.getItems(divider)[ui.getItems(divider).length - 1]);
		}
		if (index == 0) {
			//Add keyword selected
			Object panel = ui.loadComponentFromFile(UI_FILE_KEYWORDS_SIMPLE_VIEW, this);
			fillGroups(panel);
			Object btSave = ui.find(panel, COMPONENT_BT_SAVE);
			ui.setText(btSave, InternationalisationUtils.getI18NString(ACTION_CREATE));
			ui.setVisible(ui.find(panel,COMPONENT_PN_TIP), false);
			ui.add(divider, panel);
		} else if (index > 0) {
			//An existent keyword is selected, let's check if it is simple or advanced.
			Keyword keyword = ui.getAttachedObject(selected, Keyword.class);
			Collection<KeywordAction> actions = this.keywordActionDao.getActions(keyword);
			boolean simple = actions.size() <= 3;
			if (simple) {
				int previousType = -1;
				for (KeywordAction action : actions) {
					int type = action.getType();
					if (type != KeywordAction.TYPE_REPLY
							&& type != KeywordAction.TYPE_JOIN
							&& type != KeywordAction.TYPE_LEAVE) {
						simple = false;
						break;
					}
					
					if (action.getEndDate() != DEFAULT_END_DATE) {
						simple = false;
						break;
					}
					
					if (type == previousType) {
						simple = false;
						break;
					}
					
					previousType = type;
				}
			}
			if (simple) {
				Object panel = ui.loadComponentFromFile(UI_FILE_KEYWORDS_SIMPLE_VIEW, this);
				//Fill every field
				fillGroups(panel);
				Object tfKeyword = ui.find(panel, COMPONENT_TF_KEYWORD);
				ui.setEnabled(tfKeyword, false);
				String key = keyword.getKeyword().length() == 0 ? "<" + InternationalisationUtils.getI18NString(COMMON_BLANK) + ">" : keyword.getKeyword();
				ui.setText(tfKeyword, key);
				for (KeywordAction action : actions) {
					int type = action.getType();
					if (type == KeywordAction.TYPE_REPLY) {
						Object cbReply = ui.find(panel, COMPONENT_CB_AUTO_REPLY);
						Object tfReply = ui.find(panel, COMPONENT_TF_AUTO_REPLY);
						ui.setSelected(cbReply, true);
						ui.setText(tfReply, action.getUnformattedReplyText());
					} else if (type == KeywordAction.TYPE_JOIN) {
						Object checkboxJoin = ui.find(panel, COMPONENT_CB_JOIN_GROUP);
						Object cbJoinGroup = ui.find(panel, COMPONENT_CB_GROUPS_TO_JOIN);
						for (int i = 0; i < ui.getItems(cbJoinGroup).length; i++) {
							Group g = ui.getAttachedObject(ui.getItems(cbJoinGroup)[i], Group.class);
							if (g.equals(action.getGroup())) {
								ui.setInteger(cbJoinGroup, Thinlet.SELECTED, i);
								break;
							}
						}
						ui.setSelected(checkboxJoin, true);
					} else if (type == KeywordAction.TYPE_LEAVE) {
						Object checkboxLeave = ui.find(panel, COMPONENT_CB_LEAVE_GROUP);
						Object cbLeaveGroup = ui.find(panel, COMPONENT_CB_GROUPS_TO_LEAVE);
						for (int i = 0; i < ui.getItems(cbLeaveGroup).length; i++) {
							Group g = ui.getAttachedObject(ui.getItems(cbLeaveGroup)[i], Group.class);
							if (g.equals(action.getGroup())) {
								ui.setInteger(cbLeaveGroup, Thinlet.SELECTED, i);
								break;
							}
						}
						ui.setSelected(checkboxLeave, true);
					}
				}
				
				ui.setVisible(ui.find(panel, COMPONENT_BT_CLEAR), false);
				ui.add(divider, panel);
			} else {
				Object panel = ui.loadComponentFromFile(UI_FILE_KEYWORDS_ADVANCED_VIEW, this);
				Object table = ui.find(panel, COMPONENT_ACTION_LIST);
				String key = keyword.getKeyword().length() == 0 ? "<" + InternationalisationUtils.getI18NString(COMMON_BLANK) + ">" : keyword.getKeyword();
				ui.setText(panel, InternationalisationUtils.getI18NString(COMMON_KEYWORD_ACTIONS_OF, key));
				//Fill every field
				for (KeywordAction action : actions) {
					ui.add(table, ui.getRow(action));
				}
				ui.add(divider, panel);
				enableKeywordActionFields(table, ui.find(panel, COMPONENT_KEY_ACT_PANEL));
			}
		}
		enableKeywordFields(ui.find(COMPONENT_KEY_PANEL));
	}

//> UI HELPER METHODS
	/** 
	 * In advanced mode, updates the list of keywords in the Keyword Manager.  
	 * <br>Has no effect in classic mode.
	 */
	private void updateKeywordList() {
		int selectedIndex = ui.getSelectedIndex(keywordListComponent);
		ui.removeAll(keywordListComponent);
		Object newKeyword = ui.createListItem(InternationalisationUtils.getI18NString(ACTION_ADD_KEYWORD), null);
		ui.setIcon(newKeyword, Icon.KEYWORD_NEW);
		ui.add(keywordListComponent, newKeyword);
		for(Keyword keyword : keywordDao.getAllKeywords()) {
			ui.add(keywordListComponent, ui.createListItem(keyword));
		}
		if (selectedIndex >= ui.getItems(keywordListComponent).length || selectedIndex == -1) {
			selectedIndex = 0;
		}
		ui.setSelectedIndex(keywordListComponent, selectedIndex);
		showSelectedKeyword();
	}

	/**
	 * Shows the new keyword dialog.
	 * 
	 * @param keyword
	 */
	private void showNewKeywordForm(Keyword keyword) {
		String title = "Create new keyword.";
		Object keywordForm = ui.loadComponentFromFile(UI_FILE_NEW_KEYWORD_FORM, this);
		ui.setAttachedObject(keywordForm, keyword);
		ui.setText(ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_TITLE), title);
		// Pre-populate the keyword textfield with currently-selected keyword string so that
		// a sub-keyword can easily be created.  Append a space to save the user from having
		// to do it!
		if (keyword != null) ui.setText(ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_KEYWORD), keyword.getKeyword() + ' ');
		ui.add(keywordForm);
	}
	
	private void fillGroups(Object panel) {
		Object cbJoin = ui.find(panel, COMPONENT_CB_GROUPS_TO_JOIN);
		Object cbLeave = ui.find(panel, COMPONENT_CB_GROUPS_TO_LEAVE);
		Object cbJoinGroup = ui.find(panel, COMPONENT_CB_JOIN_GROUP);
		Object cbLeaveGroup = ui.find(panel, COMPONENT_CB_LEAVE_GROUP);
		List<Group> groups = this.groupDao.getAllGroups();
		for (Group g : groups) {
			Object item = createComboBoxChoice(g);
			ui.add(cbJoin, item);
			ui.add(cbLeave, item);
		}
		if (groups.size() == 0) {
			ui.setEnabled(cbJoinGroup, false);
			ui.setEnabled(cbJoin, false);
			ui.setEnabled(cbLeaveGroup , false);
			ui.setEnabled(cbLeave, false);
		} else {
			ui.setSelectedIndex(cbJoin, 0);
			ui.setSelectedIndex(cbLeave, 0);
		}
	}
	
	/**
	 * This method invokes the correct edit dialog according to the supplied action type.
	 * 
	 * @param action
	 */
	private void showActionEditDialog(KeywordAction action) {
		switch (action.getType()) {
			case KeywordAction.TYPE_FORWARD:
			ForwardActionDialog dialog = new ForwardActionDialog(ui, this);
			dialog.init(action);
			dialog.show();
				break;
			case KeywordAction.TYPE_JOIN: 
				JoinGroupActionDialog joinDialog = new JoinGroupActionDialog(ui, this);
				joinDialog.init(action);
				joinDialog.show();
				break;
			case KeywordAction.TYPE_LEAVE: 
				JoinGroupActionDialog leaveDialog = new JoinGroupActionDialog(ui, this);
				leaveDialog.init(action);
				leaveDialog.show();
				break;
			case KeywordAction.TYPE_REPLY:
			ReplyActionDialogHandler dialog1 = new ReplyActionDialogHandler(ui, this);
			dialog1.init(action);
			dialog1.show();
				break;
			case KeywordAction.TYPE_EXTERNAL_CMD:
				show_newKActionExternalCmdFormForEdition(action);
				break;
			case KeywordAction.TYPE_EMAIL:
				EmailActionDialog emailDialog = new EmailActionDialog(ui, this);
				emailDialog.init(action);
				emailDialog.show();
				break;
		}
	}
	
	/**
	 * Shows the keyword dialog for edit purpose.
	 * 
	 * @param keyword The object to be edited.
	 */
	private void showKeywordDialogForEdition(Keyword keyword) {
		String key = keyword.getKeyword().length() == 0 ? "<" + InternationalisationUtils.getI18NString(COMMON_BLANK) + ">" : keyword.getKeyword();
		String title = InternationalisationUtils.getI18NString(COMMON_EDITING_KEYWORD, key);
		Object keywordForm = ui.loadComponentFromFile(UI_FILE_NEW_KEYWORD_FORM, this);
		ui.setAttachedObject(keywordForm, keyword);
		ui.setText(ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_TITLE), title);
		// Pre-populate the keyword textfield with currently-selected keyword string so that
		// a sub-keyword can easily be created.  Append a space to save the user from having
		// to do it!
		Object textField = ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_KEYWORD);
		Object textFieldDescription = ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_DESCRIPTION);
		ui.setText(textField, key);
		ui.setEnabled(textField, false);
		if (keyword.getDescription() != null) { 
			ui.setText(textFieldDescription, keyword.getDescription());
		}
		String method = "finishKeywordEdition(newKeywordForm, newKeywordForm_description.text)";
		ui.setAction(ui.find(keywordForm, COMPONENT_NEW_KEYWORD_BUTTON_DONE), method, keywordForm, this);
		ui.add(keywordForm);
	}
	
	void updateKeywordActionList_(KeywordAction action, boolean isNew) {
		updateKeywordActionList(action, isNew);
	}
	
	private void updateKeywordActionList(KeywordAction action, boolean isNew) {
		Object table = find(COMPONENT_ACTION_LIST);
		if (isNew) {
			ui.add(table, ui.getRow(action));
		} else {
			int index = -1;
			for (Object o : ui.getItems(table)) {
				KeywordAction a = ui.getKeywordAction(o);
				if (a.equals(action)) {
					index = ui.getIndex(table, o);
					ui.remove(o);
				}
			}
			ui.add(table, ui.getRow(action), index);
		}
	}

	/**
	 * Shows the new external command action dialog.
	 * 
	 * @param keywordList
	 */
	public void show_newKActionExternalCmdForm(Object keywordList) {
		log.trace("ENTER");
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordList));
		ExternalCommandActionDialog dialog = new ExternalCommandActionDialog(ui, this);
		dialog.init(keyword);
		dialog.show();
		log.trace("EXIT");
	}

	private String keywordSimple_getAutoReply(Object panel) {
		String ret = null;
		if (ui.isSelected(ui.find(panel, COMPONENT_CB_AUTO_REPLY))) {
			ret = ui.getText(ui.find(panel, COMPONENT_TF_AUTO_REPLY));
		}
		return ret;
	}
	
	private Group keywordSimple_getJoin(Object panel) {
		Group ret = null;
		if (ui.isSelected(ui.find(panel, COMPONENT_CB_JOIN_GROUP))) {
			ret = ui.getAttachedObject(ui.getSelectedItem(ui.find(panel, COMPONENT_CB_GROUPS_TO_JOIN)), Group.class);
		}
		return ret;
	}
	
	private Group keywordSimple_getLeave(Object panel) {
		Group ret = null;
		if (ui.isSelected(ui.find(panel, COMPONENT_CB_LEAVE_GROUP))) {
			ret = ui.getAttachedObject(ui.getSelectedItem(ui.find(panel, COMPONENT_CB_GROUPS_TO_LEAVE)), Group.class);
		}
		return ret;
	}
	
	/**
	 * Shows the new external command action dialog for edition.
	 * 
	 * @param keywordList
	 */
	private void show_newKActionExternalCmdFormForEdition(KeywordAction action) {
		log.trace("ENTER");

		ExternalCommandActionDialog dialog = new ExternalCommandActionDialog(ui, this);
		dialog.init(action);
		dialog.show();
		
		log.trace("EXIT");
	}

	private Object createComboBoxChoice(Group g) {
		Object item = ui.createComboboxChoice(g.getName(), g);
		ui.setIcon(item, Icon.GROUP);
		return item;
	}
}
