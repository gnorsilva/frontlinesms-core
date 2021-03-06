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
package net.frontlinesms.csv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.data.repository.*;
import net.frontlinesms.data.DuplicateKeyException;

import org.apache.log4j.Logger;

/**
 * This file contains methods for importing data to the FrontlineSMS service
 * from CSV files.
 * 
 * FIXME display a meaningful message if import fails!
 * 
 * @author Carlos Eduardo Genz 
 * <li> kadu(at)masabi(dot)com
 * @author Alex Anderson 
 * <li> alex(at)masabi(dot)com
 */
public class CsvImporter {
	/** Logging object */
	private static Logger LOG = FrontlineUtils.getLogger(CsvImporter.class); 
	
	/** The delimiter to use between group names when they are exported. */
	protected static final String GROUPS_DELIMITER = "\\\\"; 
	
	/**
	 * Import contacts from a CSV file.
	 * @param importFile the file to import from
	 * @param contactDao
	 * @param rowFormat 
	 * @throws IOException If there was a problem accessing the file
	 * @throws CsvParseException If there was a problem with the format of the file
	 */
	public static void importContacts(File importFile, ContactDao contactDao, GroupMembershipDao groupMembershipDao, GroupDao groupDao, CsvRowFormat rowFormat) throws IOException, CsvParseException {
		LOG.trace("ENTER");
		if(LOG.isDebugEnabled()) LOG.debug("File [" + importFile.getAbsolutePath() + "]");
		Utf8FileReader reader = null;
		try {
			reader = new Utf8FileReader(importFile);
			boolean firstLine = true;
			String[] lineValues;
			while((lineValues = CsvUtils.readLine(reader)) != null) {
				if(firstLine) {
					// Ignore the first line of the CSV file as it should be the column titles
					firstLine = false;
				} else {
					String name = getString(lineValues, rowFormat, CsvUtils.MARKER_CONTACT_NAME);
					String number = getString(lineValues, rowFormat, CsvUtils.MARKER_CONTACT_PHONE);
					String email = getString(lineValues, rowFormat, CsvUtils.MARKER_CONTACT_EMAIL);
					String notes = getString(lineValues, rowFormat, CsvUtils.MARKER_CONTACT_NOTES);
					String otherPhoneNumber = getString(lineValues, rowFormat, CsvUtils.MARKER_CONTACT_OTHER_PHONE);
					boolean active = Boolean.valueOf(getString(lineValues, rowFormat, CsvUtils.MARKER_CONTACT_STATUS));
					String groups = getString(lineValues, rowFormat, CsvUtils.MARKER_CONTACT_GROUPS);
					
					Contact c = new Contact(name, number, otherPhoneNumber, email, notes, active);						
					try {
						contactDao.saveContact(c);
					} catch (DuplicateKeyException e) {
						// FIXME should actually pass details of this back to the user.
						LOG.debug("Contact already exist with this number [" + number + "]", e);
						// If the contact already existed, let's reach the existing one to fill the groupMembership
						c = contactDao.getFromMsisdn(number);
					}
					
					// We make the contact join its groups
					String[] pathList = groups.split(GROUPS_DELIMITER);
					for (String path : pathList) {
						if (path.length() == 0) continue;
						
						if (!path.startsWith(String.valueOf(Group.PATH_SEPARATOR))) {
							path = Group.PATH_SEPARATOR + path;
						}
						
						Group group = createGroups(groupDao, path);
						groupMembershipDao.addMember(group, c);
					}
				}
			}
		} finally {
			if (reader != null) reader.close();
		}
		LOG.trace("EXIT");
	}

	/**
	 * Import contacts from a CSV file.
	 * @param filename the file to import from
	 * @param rowFormat 
	 * @throws IOException If there was a problem accessing the file
	 * @throws CsvParseException If there was a problem with the format of the file
	 */
	public static List<String[]> getContactsFromCsvFile(String filename) throws IOException, CsvParseException {
		LOG.trace("ENTER");
		File importFile = new File(filename);
		List<String[]> contactsList = new ArrayList<String[]>();
		
		if(LOG.isDebugEnabled()) LOG.debug("File [" + importFile.getAbsolutePath() + "]");
		Utf8FileReader reader = null;
		try {
			reader = new Utf8FileReader(importFile);
			boolean firstLine = true;
			String[] lineValues;
			while((lineValues = CsvUtils.readLine(reader)) != null) {
				if(firstLine) {
					firstLine = false;
				} else {
					contactsList.add(lineValues);
				}
			}
		} finally {
			if (reader != null) reader.close();
		}
		
		LOG.trace("EXIT");
		return contactsList;
	}

//> STATIC HELPER METHODS	
	/**
	 * Gets the string from a particular index of an array.  If the array is not long
	 * enough to contain that index, returns an empty string.
	 * @param values
	 * @param index
	 * @return The value in the specified index of the array, or an empty string if the array index is out of bounds.
	 */
	private static String getString(String[] values, int index) {
		assert(index >= 0) : "Supplied array index must be greater than or equal to zero.";
		if(values.length > index) {
			return values[index];
		} else return "";
	}
	
	/**
	 * Gets an optional String from the supplied String array, returning "" if the string is not available.
	 * @param values The values of the row of CSV
	 * @param rowFormat The format of the row we are importing
	 * @param marker The marker we are looking for in the row format
	 * @return The value in the specified index of the array, or an empty string if the array index is out of bounds.
	 */
	private static String getString(String[] values, CsvRowFormat rowFormat, String marker) {
		Integer index = rowFormat.getIndex(marker);
		if(index == null) return "";
		else return getString(values, index);
	}

	/**
	 * Creates the group and all parent groups for a supplied path.
	 * The behaviour of this method is undefined if a group is deleted externally while this method
	 * is executing.
	 * @param groupDao
	 * @param path
	 * @return
	 */
	static Group createGroups(GroupDao groupDao, String path) {
		if (path.length() == 0) {
			return new Group(null, null);
		} else {
			int pos = path.lastIndexOf(Group.PATH_SEPARATOR);
			if (pos == -1) pos = 0;
			
			Group parent = createGroups(groupDao, path.substring(0, pos));
			path = path.substring(pos, path.length());
			if (path.startsWith(String.valueOf(Group.PATH_SEPARATOR))) {
				path = path.substring(1, path.length());
			}
			
			Group group = new Group(parent, path);
			try {
				groupDao.saveGroup(group);
			} catch (DuplicateKeyException ex) {
				// It's not a problem if this group already exists
			}
			
			return group;
		}
	}
}
