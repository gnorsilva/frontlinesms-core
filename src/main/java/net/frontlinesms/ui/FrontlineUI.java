/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.ui;

import java.awt.Image;

import net.frontlinesms.ErrorUtils;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.ui.i18n.LanguageBundle;

import org.apache.log4j.Logger;

import thinlet.FrameLauncher;

/**
 * Base UI used for FrontlineSMS.
 */
@SuppressWarnings("serial")
public abstract class FrontlineUI extends ExtendedThinlet implements ThinletUiEventHandler {
	
//> UI DEFINITION FILES
	/** Thinlet UI layout File: alert popup box */
	protected static final String UI_FILE_ALERT = "/ui/core/util/dgAlert.xml";
	/** Thinlet UI layout File: info popup box */
	protected static final String UI_FILE_INFO = "/ui/core/util/dgInfo.xml";

//> UI COMPONENTS
	/** Component of {@link #UI_FILE_ALERT} which contains the message to display */
	private static final String COMPONENT_ALERT_MESSAGE = "alertMessage";
	/** Component of {@link #UI_FILE_INFO} which contains the message to display */
	private static final String COMPONENT_INFO_MESSAGE = "infoMessage";
	
//> INSTANCE PROPERTIES
	/** Logging object */
	protected final Logger log = FrontlineUtils.getLogger(this.getClass());
	/** The language bundle currently in use */
	public static LanguageBundle currentResourceBundle;
	/** Frame launcher that this UI instance is displayed within.  We need to keep a handle on it so that we can dispose of it when we quit or change UI modes. */
	protected FrameLauncher frameLauncher;

	/**
	 * Gets the icon for a specific language bundle
	 * @param languageBundle
	 * @return the flag image for the language bundle, or <code>null</code> if none could be found.
	 */
	public Image getFlagIcon(LanguageBundle languageBundle) {
		return getFlagIcon(languageBundle.getCountry());
	}
	
	/**
	 * Gets the icon for a specific country
	 * @param languageBundle
	 * @return the flag image for the language bundle, or <code>null</code> if none could be found.
	 */
	public Image getFlagIcon(String country) {
		String flagFile = country != null ? "/icons/flags/" + country + ".png" : null;
		return country == null ? null : getIcon(flagFile);
	}
	
	/**
	 * Loads a Thinlet UI descriptor from an XML file.  If there are any
	 * problems loading the file, this will log Throwables thrown and 
	 * allow the program to continue running.
	 * 
	 * {@link #loadComponentFromFile(String, Object)} should always be used by external handlers in preference to this.
	 * @param filename path of the UI XML file to load from
	 * @return thinlet component loaded from the file
	 */
	public Object loadComponentFromFile(String filename) {
		return loadComponentFromFile(filename, this);
	}
	
	/**
	 * Loads a Thinlet UI descriptor from an XML file and sets the provided event handler.
	 * If there are any problems loading the file, this will log Throwables thrown and 
	 * allow the program to continue running.
	 * @param filename path of the UI XML file to load from
	 * @param thinletEventHandler event handler for the UI component
	 * @return thinlet component loaded from the file
	 */
	public Object loadComponentFromFile(String filename, ThinletUiEventHandler thinletEventHandler) {
		log.trace("ENTER");
		try {
			log.debug("Filename [" + filename + "]");
			log.trace("EXIT");
			return parse(filename, thinletEventHandler);
		} catch(Throwable t) {
			log.error("Error parsing file [" + filename + "]", t);
			log.trace("EXIT");
			throw new RuntimeException(t);
		}
	}
	
	/**
	 * This method opens a fileChooser.
	 * @param textFieldToBeSet The text field whose value should be sert to the chosen file
	 */
	public void showFileChooser(Object textFieldToBeSet) {
		FileChooser.showFileChooser(this, textFieldToBeSet);
	}

	/**
	 * Popup an alert to the user with the supplied message.
	 * @param alertMessage
	 */
	public void alert(String alertMessage) {
		Object alertDialog = loadComponentFromFile(UI_FILE_ALERT);
		setText(find(alertDialog, COMPONENT_ALERT_MESSAGE), alertMessage);
		add(alertDialog);
	}
	
	/**
	 * Popup an info message to the user with the supplied message.
	 * @param infoMessage
	 */
	public void infoMessage(String infoMessage) {
		Object infoDialog = loadComponentFromFile(UI_FILE_INFO);
		setText(find(infoDialog, COMPONENT_INFO_MESSAGE), infoMessage);
		add(infoDialog);
	}
	
	/**
	 * Removes the supplied dialog from the application.
	 * 
	 * @param dialog
	 */
	public void removeDialog(Object dialog) {
		remove(dialog);
	}
	
	/**
	 * Opens a link in the system browser
	 * @param url the url to open
	 * @see FrontlineUtils#openExternalBrowser(String)
	 */
	public void openBrowser(String url) {
		FrontlineUtils.openExternalBrowser(url);
	}

	/**
	 * Opens a page of the help manual
	 * @param page The name of the help manual page, including file extension.
	 */
	public void showHelpPage(String page) {
		FrontlineUtils.openHelpPageInBrowser(page);
	}
	
	/**
	 * Shows an error dialog informing the user that an unhandled error has occurred.
	 */
	@Override
	protected void handleException(Throwable throwable) {
		log.error("Unhandled exception from thinlet.", throwable);
		ErrorUtils.showErrorDialog("Unexpected error", "There was an unexpected error.", throwable, false);
	}
}
