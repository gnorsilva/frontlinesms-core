/**
 * 
 */
package net.frontlinesms.ui.handler.phones;

import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_MODEM_LIST_UPDATED;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.TAB_ADVANCED_PHONE_MANAGER;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArraySet;

import net.frontlinesms.CommUtils;
import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.data.domain.SmsModemSettings;
import net.frontlinesms.data.events.DatabaseEntityNotification;
import net.frontlinesms.data.repository.SmsModemSettingsDao;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.messaging.FrontlineMessagingService;
import net.frontlinesms.messaging.FrontlineMessagingServiceStatus;
import net.frontlinesms.messaging.FrontlineMessagingServiceEventListener;
import net.frontlinesms.messaging.mms.MmsService;
import net.frontlinesms.messaging.mms.MmsServiceManager;
import net.frontlinesms.messaging.mms.email.MmsEmailService;
import net.frontlinesms.messaging.mms.email.MmsEmailServiceStatus;
import net.frontlinesms.messaging.mms.events.MmsServiceStatusNotification;
import net.frontlinesms.messaging.sms.SmsService;
import net.frontlinesms.messaging.sms.SmsServiceManager;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceStatus;
import net.frontlinesms.messaging.sms.modem.SmsModem;
import net.frontlinesms.messaging.sms.modem.SmsModemStatus;
import net.frontlinesms.ui.Event;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.SmsInternetServiceSettingsHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.TabChangedNotification;
import net.frontlinesms.ui.handler.BaseTabHandler;
import net.frontlinesms.ui.handler.email.EmailAccountSettingsDialogHandler;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

import org.smslib.AbstractATHandler;
import org.smslib.handler.CATHandler;

import serial.CommPortIdentifier;
import serial.NoSuchPortException;

/**
 * Event handler for the Phones tab and associated dialogs
 * @author Alex
 */
@TextResourceKeyOwner(prefix={"COMMON_", "I18N_", "MESSAGE_"})
public class PhoneTabHandler extends BaseTabHandler implements FrontlineMessagingServiceEventListener, EventObserver {
//> STATIC CONSTANTS
	/** The fully-qualified name of the default {@link CATHandler} class. */
	private static final String DEFAULT_CAT_HANDLER_CLASS_NAME = CATHandler.class.getName();
	/** {@link Comparator} used for sorting {@link FrontlineMessagingService}s into a friendly order. */
	private static final Comparator<? super FrontlineMessagingService> MESSAGING_SERVICE_COMPARATOR = new Comparator<FrontlineMessagingService>() {
			public int compare(FrontlineMessagingService one, FrontlineMessagingService tother) {
				int comparison = 0;
				
				// Always Modems first, then Internet Services, then MMS Services
				if (one instanceof SmsModem) {
					if (tother instanceof SmsModem) {
						comparison = ((SmsModem)one).getPort().compareTo(((SmsModem)tother).getPort());
					} else {
						comparison = -1;
					}
				} else if (one.getClass().equals(tother.getClass())) {
					comparison = one.getServiceName().compareTo(tother.getServiceName());
				} else if (one instanceof SmsInternetService) {
					comparison = (tother instanceof SmsModem ? 1 : -1);
				}
				
				return comparison;
			}};
	
//> THINLET UI LAYOUT FILES
	/** UI XML File Path: the Phones Tab itself */
	private static final String UI_FILE_PHONES_TAB = "/ui/core/phones/phonesTab.xml";
	/** UI XML File Path: phone settings dialog TODO what is this dialog for? */
	private static final String UI_FILE_MODEM_SETTINGS_DIALOG = "/ui/core/phones/dgModemSettings.xml";
	/** UI XML File Path: phone config dialog TODO what is this dialog for? */
	private static final String UI_FILE_MODEM_MANUAL_CONFIG_DIALOG = "/ui/core/phones/dgModemManualConfig.xml";
	
//> I18n TEXT KEYS
	/** I18n Text Key: TODO */
	private static final String COMMON_PHONE_CONNECTED = "common.phone.connected";
	/** I18n Text Key: TODO */
	private static final String COMMON_SETTINGS_FOR_PHONE = "common.settings.for.phone";
	/** I18n Text Key: TODO */
	private static final String COMMON_SMS_INTERNET_SERVICE_CONNECTED = "common.sms.internet.service.connected";
	/** I18n Text Key: TODO */
	private static final String MESSAGE_INVALID_BAUD_RATE = "message.invalid.baud.rate";
	/** I18n Text Key: TODO */
	private static final String MESSAGE_PORT_NOT_FOUND = "message.port.not.found";
	/** I18n Text Key: TODO */
	private static final String MESSAGE_PORT_ALREADY_CONNECTED = "message.port.already.connected";
	/** I18n Text Key: The requested port is already in use. */
	private static final String I18N_PORT_IN_USE = "com.port.inuse";
	/** I18n Text Key: Last checked: %0. */
	private static final String I18N_EMAIL_LAST_CHECKED = "email.last.checked";
	
//> THINLET UI COMPONENT NAMES	
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_RB_PHONE_DETAILS_ENABLE = "rbPhoneDetailsEnable";
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_PHONE_SENDING = "cbSending";
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_PHONE_RECEIVING = "cbReceiving";
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_PHONE_DELETE = "cbDeleteMsgs";
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_PHONE_DELIVERY_REPORTS = "cbUseDeliveryReports";
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_PN_PHONE_SETTINGS = "pnPhoneSettings";
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_PHONE_MANAGER_MODEM_LIST = "phoneManager_modemList";
	/** UI Compoenent name: TODO */
	private static final String COMPONENT_PHONE_MANAGER_MODEM_LIST_ERROR = "phoneManager_modemListError";

//> INSTANCE PROPERTIES
	/** Object to synchronize on before updating the phones list.  This is to prevent the list being rewritten by two different sources at the same time. */
	private final Object PHONES_LIST_SYNCH_OBJECT = new Object();
	/** The manager of {@link FrontlineMessagingService}s */
	private final SmsServiceManager phoneManager;
	/** Data Access Object for {@link SmsModemSettings}s */
	private final SmsModemSettingsDao smsModelSettingsDao;
	private MmsServiceManager mmsServiceManager;

//> CONSTRUCTORS
	/**
	 * Create a new instance of this class.
	 * @param uiController value for {@link #ui}
	 */
	public PhoneTabHandler(UiGeneratorController ui) {
		super(ui);
		this.phoneManager = ui.getPhoneManager();
		this.mmsServiceManager = ui.getFrontlineController().getMmsServiceManager();
		this.smsModelSettingsDao = ui.getPhoneDetailsManager();
	}
	
	@Override
	protected Object initialiseTab() {
		// We register the observer to the UIGeneratorController, which notifies when tabs have changed
		this.ui.getFrontlineController().getEventBus().registerObserver(this);
		
		return ui.loadComponentFromFile(UI_FILE_PHONES_TAB, this);
	}

//> ACCESSORS
	/** @return the compoenent containing the list of connected devices */
	private Object getModemListComponent() {
		return ui.find(this.getTab(), COMPONENT_PHONE_MANAGER_MODEM_LIST);
	}
	
//> THINLET UI METHODS
	
	/**
	 * Event fired when the view phone details action is fired. 
	 * @param list the thinlet UI list containing details of the devices
	 */
	public void showPhoneSettingsDialog(Object list) {
		Object selected = ui.getSelectedItem(list);
		if (selected != null) {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			if (service instanceof SmsModem) {
				if (!((SmsModem) service).isDisconnecting())
					showPhoneSettingsDialog((SmsModem) service, false);
			} else if (service instanceof SmsInternetService) {
				SmsInternetServiceSettingsHandler serviceHandler = new SmsInternetServiceSettingsHandler(this.ui);
				serviceHandler.showConfigureService((SmsInternetService) service, null);
			} else if (service instanceof MmsEmailService) {
				EmailAccountSettingsDialogHandler emailAccountSettingsDialogHandler = new EmailAccountSettingsDialogHandler(ui, true);
				emailAccountSettingsDialogHandler.initDialog(((MmsEmailService) service).getEmailAccount());
				this.ui.add(emailAccountSettingsDialogHandler.getDialog());
			}
		}
	}
	
	/**
	 * Event fired when the view phone details action is chosen.
	 * @param phone The phone we are showing settings for
	 * @param isNewPhone <code>true</code> TODO <if this phone has previously connected (i.e. not first time it has connected)> OR <if this phone has just connected (e.g. may have connected before, but not today)> 
	 */
	public void showPhoneSettingsDialog(SmsModem phone, boolean isNewPhone) {
		Object phoneSettingsDialog = this.ui.loadComponentFromFile(UI_FILE_MODEM_SETTINGS_DIALOG, this);
		this.ui.setText(phoneSettingsDialog, InternationalisationUtils.getI18NString(COMMON_SETTINGS_FOR_PHONE) + " '" + phone.getModel() + "'");
		
		if(!isNewPhone) {
			boolean useForSending = phone.isUseForSending();
			boolean useForReceiving = phone.isUseForReceiving();
			
			if(useForSending || useForReceiving) {
				this.ui.setSelected(this.ui.find(phoneSettingsDialog, COMPONENT_PHONE_SENDING), useForSending);
				Object cbDeliveryReports = this.ui.find(phoneSettingsDialog, COMPONENT_PHONE_DELIVERY_REPORTS);
				ui.setEnabled(cbDeliveryReports, useForSending);
				ui.setSelected(cbDeliveryReports, phone.isUseDeliveryReports());
				ui.setSelected(this.ui.find(phoneSettingsDialog, COMPONENT_PHONE_RECEIVING), useForReceiving);
				Object cbDeleteMessages = this.ui.find(phoneSettingsDialog, COMPONENT_PHONE_DELETE);
				this.ui.setEnabled(cbDeleteMessages, useForReceiving);
				this.ui.setSelected(cbDeleteMessages, phone.isDeleteMessagesAfterReceiving());
			} else {
				ui.setSelected(ui.find(phoneSettingsDialog, "rbPhoneDetailsDisable"), true);
				ui.setSelected(ui.find(phoneSettingsDialog, COMPONENT_RB_PHONE_DETAILS_ENABLE), false);
				ui.deactivate(ui.find(phoneSettingsDialog, COMPONENT_PN_PHONE_SETTINGS));
			}
		}
		
		if(!phone.supportsReceive()) {
			// If this phone does not support SMS receiving, we need to pass this info onto
			// the user.  We also want to gray out the options for receiving.
			ui.setEnabled(ui.find(phoneSettingsDialog, COMPONENT_PHONE_RECEIVING), false);
			ui.setEnabled(ui.find(phoneSettingsDialog, COMPONENT_PHONE_DELETE), false);
		} else {
			// No error, so remove the error message.
			ui.remove(ui.find(phoneSettingsDialog, "lbReceiveNotSupported"));
		}
		
		ui.setAttachedObject(phoneSettingsDialog, phone);
		ui.add(phoneSettingsDialog);
	}
	
	/**
	 * Enables or disables the <b>Edit Phone Settings</b>. 
	 * <br>If the supplied list is not empty, then the option is enabled. Otherwise, it is disabled.
	 * TODO would be good to know when this event is triggered
	 * @param list
	 * @param menuItem
	 */
	public void editPhoneEnabled(Object list, Object menuItem) {
		ui.setVisible(menuItem, ui.getSelectedItem(list) != null);
	}

	/**
	 * Disconnect from a specific {@link FrontlineMessagingService}.
	 * @param list The list of connected {@link FrontlineMessagingService}s in the Phones tab.
	 */
	public void disconnectFromSelected(Object list) {
		Object selected = ui.getSelectedItem(list);
		
		if (selected != null) {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			
			if (service instanceof SmsService) {
				phoneManager.disconnect((SmsService) service);
			} else if (service instanceof MmsEmailService) {
				this.mmsServiceManager.connectMmsEmailService((MmsEmailService) service, false);
			}
			
			refresh();
		}
	}
	
	/**
	 * Stop detection of the {@link FrontlineMessagingService} on a specific port.
	 * @param list The list of ports which are currently being probed for connected {@link FrontlineMessagingService}s.
	 */
	public void stopDetection(Object list) {
		FrontlineMessagingService dev = ui.getAttachedObject(ui.getSelectedItem(list), FrontlineMessagingService.class);
		if (dev instanceof SmsModem) {
			SmsModem modem = (SmsModem) dev;
			phoneManager.stopDetection(modem.getPort());
			
		}
		refresh();
	}
	
	/**
	 * Action triggered when an item in the unconnected phones/ports list is selected.  This method
	 * enables and disables relevant items in the contextual popupmenu for the selected port.
	 * @param popUp
	 * @param list
	 */
	public void phoneManager_enabledFields(Object popUp, Object list) {
		Object selected = ui.getSelectedItem(list);
		if (selected == null) {
			ui.setVisible(popUp, false);
		} else {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			if (service instanceof SmsModem) {
				SmsModem modem = (SmsModem) service;
				ui.setVisible(ui.find(popUp, "miEditPhone"), false);
				ui.setVisible(ui.find(popUp, "miAutoConnect"), !modem.isDetecting() && !modem.isTryToConnect());
				ui.setVisible(ui.find(popUp, "miManualConnection"), !modem.isDetecting() && !modem.isTryToConnect());
				ui.setVisible(ui.find(popUp, "miCancelDetection"), modem.isDetecting());
			} else {
				for (Object o : ui.getItems(popUp)) {
					ui.setVisible(o, ui.getName(o).equals("miEditPhone") || ui.getName(o).equals("miAutoConnect"));
				}
			}
		}
	}
	
	/**
	 * Attempt to automatically connect to the service currently selected in the list of
	 * unconnected services.
	 */
	public void connectToSelectedPhoneHandler() {
		Object modemListError = ui.find(COMPONENT_PHONE_MANAGER_MODEM_LIST_ERROR);
		Object selected = ui.getSelectedItem(modemListError);
		
		if (selected != null) {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			
			if (service instanceof SmsModem) {
				SmsModem modem = (SmsModem) service;
				try {
					phoneManager.requestConnect(modem.getPort());
				} catch (NoSuchPortException ex) {
					log.info("", ex);
				}
			} else if (service instanceof SmsInternetService) {
				phoneManager.addSmsInternetService((SmsInternetService) service);
			} else if (service instanceof MmsEmailService) {
				this.mmsServiceManager.connectMmsEmailService((MmsEmailService) service, true);
			}
		}
	}
	
	/**
	 * Show the dialog for connecting a phone with manual configuration.
	 * @param list TODO what is this list, and why is it necessary?  Could surely just find it
	 */
	public void showPhoneConfigDialog(Object list) {
		Object configDialog = ui.loadComponentFromFile(UI_FILE_MODEM_MANUAL_CONFIG_DIALOG, this);
		
		Object portList = ui.find(configDialog, "lbPortName");
		Enumeration<CommPortIdentifier> commPortEnumeration = CommUtils.getPortIdentifiers();
		while (commPortEnumeration.hasMoreElements()) {
			CommPortIdentifier commPortIdentifier = commPortEnumeration.nextElement();
			ui.add(portList, ui.createComboboxChoice(commPortIdentifier.getName(), null));
		}
		
		Object handlerList = ui.find(configDialog, "lbCATHandlers");
		int trimLength = DEFAULT_CAT_HANDLER_CLASS_NAME.length() + 1;
		
		for (Class<? extends AbstractATHandler> handler : AbstractATHandler.getHandlers()) {
			String handlerName = handler.getName();
			if(handlerName.equals(DEFAULT_CAT_HANDLER_CLASS_NAME)) handlerName = "<default>";
			else handlerName = handlerName.substring(trimLength);
			ui.add(handlerList, ui.createComboboxChoice(handlerName, handler));
		}
		
		Object selected = ui.getSelectedItem(list);
		FrontlineMessagingService selectedPhone = ui.getAttachedObject(selected, FrontlineMessagingService.class);
		if (selectedPhone instanceof SmsModem) {
			SmsModem modem = (SmsModem) selectedPhone;
			ui.setText(ui.find(configDialog,"lbPortName"), modem.getPort());
			ui.setText(ui.find(configDialog,"lbBaudRate"), String.valueOf(modem.getBaudRate()));
		}
		
		ui.add(configDialog);
	}
	
	/**
	 * Connect to a phone using the manual settings provided in the {@link #showPhoneConfigDialog(Object)}.
	 * @param phoneConfigDialog
	 */
	public void connectToPhone(Object phoneConfigDialog) {
		String baudRateAsString = ui.getText(ui.find(phoneConfigDialog,"lbBaudRate"));
		String requestedPortName = ui.getText(ui.find(phoneConfigDialog,"lbPortName"));
		if (!phoneManager.hasPhoneConnected(requestedPortName)) {
			try {
				String preferredCATHandler = ui.getText(ui.find(phoneConfigDialog,"lbCATHandlers"));
				if(phoneManager.requestConnect(
						requestedPortName,
						Integer.parseInt(baudRateAsString),
						preferredCATHandler)) {
					ui.remove(phoneConfigDialog);
				} else {
					ui.alert(InternationalisationUtils.getI18NString(I18N_PORT_IN_USE));
				}
			} catch (NumberFormatException e) {
				ui.alert(InternationalisationUtils.getI18NString(MESSAGE_INVALID_BAUD_RATE, baudRateAsString));
			} catch (NoSuchPortException e) {
				ui.alert(InternationalisationUtils.getI18NString(MESSAGE_PORT_NOT_FOUND, requestedPortName));
			}
		} else {
			ui.alert(InternationalisationUtils.getI18NString(MESSAGE_PORT_ALREADY_CONNECTED, requestedPortName));
		}
	}

	/** Starts the phone auto-detector. */
	public void phoneManager_detectModems() {
		phoneManager.refreshPhoneList(true);
	}
	
	/**
	 * called when one of the SMS devices (phones or http senders) has a change in status,
	 * such as detection, connection, disconnecting, running out of batteries, etc.
	 * see PhoneHandler.STATUS_CODE_MESSAGES[smsDeviceEventCode] to get the relevant messages
	 * @param messagingService
	 * @param serviceStatus
	 */
	public void messagingServiceEvent(FrontlineMessagingService messagingService, FrontlineMessagingServiceStatus serviceStatus) {
		log.trace("ENTER");
		
		// Handle modems first
		if (messagingService instanceof SmsModem) {
			SmsModem activeService = (SmsModem) messagingService;
			// FIXME re-implement status update on the AWT Event Queue - doing it here causes splitpanes to collapse
			// ui.setStatus(InternationalisationUtils.getI18NString(MESSAGE_PHONE) + ": " + activeDevice.getPort() + ' ' + getSmsDeviceStatusAsString(device));
			if (serviceStatus.equals(SmsModemStatus.CONNECTED)) {
				log.debug("Phone is connected. Try to read details from database!");
				String serial = activeService.getSerial();
				SmsModemSettings settings = smsModelSettingsDao.getSmsModemSettings(serial);
				
				// If this is the first time we've attached this phone, or no settings were
				// saved last time, we should show the settings dialog automatically
				if(settings == null) {
					log.debug("User need to make setting related this phone.");
					showPhoneSettingsDialog(activeService, true);
				} else {
					// Let's update the Manufacturer & Model for this device, if it wasn't previously set
					if (settings.getManufacturer() == null || settings.getModel() == null) {
						settings.setManufacturer(activeService.getManufacturer());
						settings.setModel(activeService.getModel());
						
						smsModelSettingsDao.updateSmsModemSettings(settings);
					}
					activeService.setUseForSending(settings.useForSending());
					activeService.setUseDeliveryReports(settings.useDeliveryReports());

					if(activeService.supportsReceive()) {
						activeService.setUseForReceiving(settings.useForReceiving());
						activeService.setDeleteMessagesAfterReceiving(settings.deleteMessagesAfterReceiving());
					}
				}

				ui.newEvent(new Event(Event.TYPE_PHONE_CONNECTED, InternationalisationUtils.getI18NString(COMMON_PHONE_CONNECTED) + ": " + activeService.getModel()));
			}
		} else {
			SmsInternetService service = (SmsInternetService) messagingService;
			// TODO document why newEvent is called here, and why it is only called for certain statuses.
			if (serviceStatus.equals(SmsInternetServiceStatus.CONNECTED)) {
				ui.newEvent(new Event(
						Event.TYPE_SMS_INTERNET_SERVICE_CONNECTED,
						InternationalisationUtils.getI18NString(COMMON_SMS_INTERNET_SERVICE_CONNECTED) 
						+ ": " + SmsInternetServiceSettingsHandler.getProviderName(service.getClass()) + " - " + service.getIdentifier()));
			} else if (serviceStatus.equals(SmsInternetServiceStatus.RECEIVING_FAILED)) {
				ui.newEvent(new Event(
						Event.TYPE_SMS_INTERNET_SERVICE_RECEIVING_FAILED,
						SmsInternetServiceSettingsHandler.getProviderName(service.getClass()) + " - " + service.getIdentifier()
						+ ": " + InternationalisationUtils.getI18NString(FrontlineSMSConstants.COMMON_SMS_INTERNET_SERVICE_RECEIVING_FAILED)));
			}
		}
		refresh();
		log.trace("EXIT");
	}
	
	public void phoneManagerDetailsUse(Object phoneSettingsDialog, Object radioButton) {
		Object pnPhoneSettings = ui.find(phoneSettingsDialog, COMPONENT_PN_PHONE_SETTINGS);
		if(COMPONENT_RB_PHONE_DETAILS_ENABLE.equals(ui.getName(radioButton))) {
			ui.activate(pnPhoneSettings);
			// If this phone does not support SMS receiving, we need to pass this info onto
			// the user.  We also want to gray out the options for receiving.
			SmsModem modem = ui.getAttachedObject(phoneSettingsDialog, SmsModem.class);
			if(!modem.supportsReceive()) {
				ui.setEnabled(ui.find(pnPhoneSettings, COMPONENT_PHONE_RECEIVING), false);
				ui.setEnabled(ui.find(pnPhoneSettings, COMPONENT_PHONE_DELETE), false);
			}
		} else ui.deactivate(pnPhoneSettings);
	}
	
	public void phoneManagerDetailsCheckboxChanged(Object checkbox) {
		ui.setEnabled(ui.getNextItem(ui.getParent(checkbox), checkbox, false), ui.isSelected(checkbox));
	}
	
	/**
	 * Event fired when the view phone details action is chosen.  We save the details
	 * of the phone to the database.
	 */
	public void updatePhoneDetails(Object dialog) {
		SmsModem phone = ui.getAttachedObject(dialog, SmsModem.class);
		String serial = phone.getSerial();
		String manufacturer = phone.getManufacturer();
		String model = phone.getModel();

		boolean useForSending;
		boolean useDeliveryReports;
		boolean useForReceiving;
		boolean deleteMessagesAfterReceiving;
		if(ui.isSelected(ui.find(dialog, COMPONENT_RB_PHONE_DETAILS_ENABLE))) {
			useForSending = ui.isSelected(ui.find(dialog, COMPONENT_PHONE_SENDING));
			useDeliveryReports = ui.isSelected(ui.find(dialog, COMPONENT_PHONE_DELIVERY_REPORTS));
			useForReceiving = ui.isSelected(ui.find(dialog, COMPONENT_PHONE_RECEIVING));
			deleteMessagesAfterReceiving = ui.isSelected(ui.find(dialog, COMPONENT_PHONE_DELETE));
		} else {
			useForSending = false;
			useDeliveryReports = false;
			useForReceiving = false;
			deleteMessagesAfterReceiving = false;
		}
		
		phone.setUseForSending(useForSending);
		phone.setUseDeliveryReports(useDeliveryReports);
		if(phone.supportsReceive()) {
			phone.setUseForReceiving(useForReceiving);
			phone.setDeleteMessagesAfterReceiving(deleteMessagesAfterReceiving);
		} else {
			useForReceiving = false;
			deleteMessagesAfterReceiving = false;
		}
		
		SmsModemSettings settings = this.smsModelSettingsDao.getSmsModemSettings(serial);
		if(settings != null) {
			settings.setDeleteMessagesAfterReceiving(deleteMessagesAfterReceiving);
			settings.setUseDeliveryReports(useDeliveryReports);
			settings.setUseForReceiving(useForReceiving);
			settings.setUseForSending(useForSending);
			this.smsModelSettingsDao.updateSmsModemSettings(settings);
		} else {
			settings = new SmsModemSettings(serial, manufacturer, model, useForSending, useForReceiving, deleteMessagesAfterReceiving, useDeliveryReports);
			this.smsModelSettingsDao.saveSmsModemSettings(settings);
		}
		
		// Phone settings may have changed.  As we're now displaying these on the table, we
		// need to update the table.
		refresh();
		
		removeDialog(dialog);
	}

//> INSTANCE HELPER METHODS
	/** 
	 * Refreshes the list of PhoneHandlers displayed on the PhoneManager tab.
	 */
	public void refresh() {
		synchronized (PHONES_LIST_SYNCH_OBJECT) {
			Object modemListError = ui.find(COMPONENT_PHONE_MANAGER_MODEM_LIST_ERROR);
			// cache the selected item so we can reselect it when we've finished!
			int index = ui.getSelectedIndex(modemListError);
			
			int indexTop = ui.getSelectedIndex(getModemListComponent());
			
			ui.removeAll(getModemListComponent());
			ui.removeAll(modemListError);

			Collection<FrontlineMessagingService> messagingServices = new CopyOnWriteArraySet<FrontlineMessagingService>(); 
			messagingServices.addAll(this.phoneManager.getAll());
			messagingServices.addAll(this.mmsServiceManager.getAll());
			
			FrontlineMessagingService[] messagingServicesArray = messagingServices.toArray(new FrontlineMessagingService[0]);
			Arrays.sort(messagingServicesArray, MESSAGING_SERVICE_COMPARATOR);
			
			for (FrontlineMessagingService messagingService : messagingServicesArray) {
				if (messagingService.isConnected()) {
					ui.add(getModemListComponent(), getTableRow(messagingService, true));
				} else {
					ui.add(modemListError, getTableRow(messagingService, false));
				}
			}			

			ui.setSelectedIndex(getModemListComponent(), indexTop);
			ui.setSelectedIndex(modemListError, index);
		}
	}
	
	private Object getTableRow(FrontlineMessagingService service, boolean isConnected) {
		Object row = ui.createTableRow(service);
		this.ui.setAttachedObject(row, service);

		/** TYPE CELL (Icon) */
		final String typeIcon;
		if (service instanceof SmsModem) {
			typeIcon = Icon.PHONE_NUMBER;
		} else {
			typeIcon = Icon.SMS_HTTP;
		}

		Object typeCell = ui.createTableCell("");
		ui.setIcon(typeCell, typeIcon);
		ui.add(row, typeCell);
		
		/** MESSAGE TYPE CELL (Icon) */
		Object messageTypeCell = ui.createTableCell("");
		if (service instanceof MmsService) {
			ui.setIcon(messageTypeCell, Icon.MMS);
		} else if (service.isConnected() || service instanceof SmsInternetService) {
			ui.setIcon(messageTypeCell, Icon.SMS);
		}
		ui.add(row, messageTypeCell);
		
		
		/** PORT CELL */
		Object portCell = ui.createTableCell(service.getDisplayPort());
		ui.add(row, portCell);
		
		/** NAME CELL */
		Object nameCell = ui.createTableCell(service.getServiceName());
		ui.add(row, nameCell);
		
		/** ID CELL */
		Object idCell = ui.createTableCell(service.getServiceIdentification());
		ui.add(row, idCell);
		
		
		/** USE FOR RECEIVING/SENDING CELLS */
		Object useForSendingCell = ui.createTableCell("");
		Object useForReceiveCell = ui.createTableCell("");
		if (isConnected) {
			if (service.isUseForSending()) ui.setIcon(useForSendingCell, Icon.TICK);
			if (service.isUseForReceiving()) ui.setIcon(useForReceiveCell, Icon.TICK);
			ui.add(row, useForSendingCell);
			ui.add(row, useForReceiveCell);
		}
		
		
		/** STATUS CELL */
		// Add "status cell" - "traffic light" showing how functional the device is
		final String statusIcon;
		FrontlineMessagingServiceStatus status = service.getStatus();
		if (status.equals(SmsModemStatus.CONNECTING) ||
			status.equals(SmsModemStatus.DETECTED) ||
			status.equals(SmsModemStatus.TRY_TO_CONNECT) ||
			status.equals(MmsEmailServiceStatus.FETCHING)) {
			statusIcon = Icon.LED_AMBER;	
		} else if (service.isConnected()){
			statusIcon = Icon.LED_GREEN;
		} else {
			statusIcon = Icon.LED_RED;
		}
	
		Object statusCell = ui.createTableCell(getServiceStatusAsString(service));
		ui.setIcon(statusCell, statusIcon);
		ui.add(row, statusCell);
		
		return row;
	}

//> STATIC FACTORIES

//> STATIC HELPER METHODS
	/**
	 * Gets the status of an {@link FrontlineMessagingService} as an internationalised {@link String}.
	 * @param service
	 * @return An internationalised {@link String} describing the status of the {@link FrontlineMessagingService}.
	 */
	private static String getServiceStatusAsString(FrontlineMessagingService service) {
		String statusString = InternationalisationUtils.getI18NString(service.getStatus().getI18nKey(), service.getStatusDetail());
		
		if (service instanceof MmsEmailService && service.getStatus().equals(MmsEmailServiceStatus.READY)) {
			Long lastChecked = ((MmsEmailService) service).getEmailAccount().getLastCheck();
			if (lastChecked != null) {
				statusString += " - " + InternationalisationUtils.getI18NString(I18N_EMAIL_LAST_CHECKED, InternationalisationUtils.getDatetimeFormat().format(new Date (lastChecked)));
			}
		}
		
		return statusString;
	}
	
	/**
	 * UI event called when the user changes tab
	 */
	public void notify(FrontlineEventNotification notification) {
		// This object is registered to the UIGeneratorController and get notified when the users changes tab
		if (notification instanceof TabChangedNotification) {
			String newTabName = ((TabChangedNotification) notification).getNewTabName();
			if (newTabName.equals(TAB_ADVANCED_PHONE_MANAGER)) {
				this.refresh();
				this.ui.setStatus(InternationalisationUtils.getI18NString(MESSAGE_MODEM_LIST_UPDATED));
			}
		} else if (notification instanceof MmsServiceStatusNotification) {
			this.refresh();
		} else if (notification instanceof DatabaseEntityNotification<?>) {
			// Database notification
			if (((DatabaseEntityNotification<?>) notification).getDatabaseEntity() instanceof EmailAccount) {
				// If there is any change in the E-Mail accounts, we refresh the list of Messaging Services
				this.refresh();
			}
		}
	}
}
