/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/user/api/User.java $
 * $Id: User.java 11561 2015-09-06 00:45:58Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.etudes.user.api;

import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.etudes.file.api.Reference;
import org.etudes.roster.api.Client;
import org.etudes.tool.api.ToolItemReference;

/**
 * User models an Etudes user.
 */
public interface User
{
	public static class NameDisplayComparator implements Comparator<User>
	{
		public int compare(User o1, User o2)
		{
			return o1.getNameDisplay().compareTo(o2.getNameDisplay());
		}
	}

	public static class NameSortComparator implements Comparator<User>
	{
		public int compare(User o1, User o2)
		{
			return o1.getNameSort().compareTo(o2.getNameSort());
		}
	}

	/**
	 * Check the given plain text password against the user's encrypted stored password.
	 * 
	 * @param password
	 *        The plain text password.
	 * @return TRUE if the password matches, FALSE if not.
	 */
	Boolean checkPassword(String password);

	/**
	 * @return The user's Avatar image reference id, or null if none set.
	 */
	Reference getAvatar();

	/**
	 * @return The user's connect AIM id, or null if none set.
	 */
	String getConnectAim();

	/**
	 * @return The user's connect Facebook id, or null if none set.
	 */
	String getConnectFacebook();

	/**
	 * @return The user's connect Google+ id, or null if none set.
	 */
	String getConnectGooglePlus();

	/**
	 * @return The user's connect LinkedIn id, or null if none set.
	 */
	String getConnectLinkedIn();

	/**
	 * @return The user's connect Skype id, or null if none set.
	 */
	String getConnectSkype();

	/**
	 * @return The user's connect Twitter id, or null if none set.
	 */
	String getConnectTwitter();

	/**
	 * @return The user's connect web site URL, or null if none set.
	 */
	String getConnectWeb();

	/**
	 * @return the user who created this user.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * @return The user's Etudes (login) id. EID is case insensitive.
	 */
	String getEid();

	/**
	 * @return The user's official roster-set email address.
	 */
	String getEmailOfficial();

	/**
	 * @return The user's self-set email address.
	 */
	String getEmailUser();

	/**
	 * @return the internal user id.
	 */
	Long getId();

	/**
	 * @return the user's IID information in a single display string.
	 */
	String getIidDisplay();

	/**
	 * Get the user's IID ID for this client.
	 * 
	 * @param The
	 *        client.
	 * @return the user's IID ID for this client, or null if there is none.
	 */
	String getIidDisplay(Client client);

	/**
	 * @return the user's IIDs in a List, or an empty list if no IIDs are defined.
	 */
	List<Iid> getIids();

	/**
	 * @return the user who last modified this user.
	 */
	User getModifiedBy();

	/**
	 * @return the date last modified.
	 */
	Date getModifiedOn();

	/**
	 * @return The user's name for display purposes.
	 */
	String getNameDisplay();

	/**
	 * @return The user's first name.
	 */
	String getNameFirst();

	/**
	 * @return The user's last name.
	 */
	String getNameLast();

	/**
	 * @return The user's name for sorting purposes.
	 */
	String getNameSort();

	/**
	 * @return The user's password, encoded.
	 */
	String getPasswordEncoded();

	/**
	 * @return The user's profile interests setting (text), or null if none set.
	 */
	String getProfileInterests();

	/**
	 * @return The user's profile location setting (text), or null if none set.
	 */
	String getProfileLocation();

	/**
	 * @return The user's profile occupation setting (text), or null if none set.
	 */
	String getProfileOccupation();

	/**
	 * @return The ToolItemReference for the user.
	 */
	ToolItemReference getReference();

	/**
	 * Get user's signature text (html).
	 * 
	 * @param forDownload
	 *        if true, modify the signature to convert embedded references from stored placeholder URLs to download URLs using references for this item.
	 * @return The signature, or null if none set.
	 */
	String getSignature(boolean forDownload);

	/**
	 * @return The user's preferred time zone for all date UI, or null if none set.
	 */
	String getTimeZone();

	/**
	 * @return The user's preferred time zone for all date UI, or the default if none set.
	 */
	String getTimeZoneDflt();

	/**
	 * @return TRUE if the user is the Admin user, FALSE if not.
	 */
	Boolean isAdmin();

	/**
	 * @return TRUE if the user want to expose their email address to users other than their instructors, FALSE if they do not.
	 */
	Boolean isEmailExposed();

	/**
	 * @return TRUE if the user was created as a result of roster processing, FALSE if not.
	 */
	Boolean isRosterUser();

	/**
	 * Update from CDP parameters.
	 * 
	 * @param prefix
	 *        The parameter names prefix.
	 * @param parameters
	 *        The parameters.
	 * @param isAdmin
	 *        true if the updating user is an admin, to enable setting EID and IID.
	 */
	void read(String prefix, Map<String, Object> parameters, boolean isAdmin);

	/**
	 * Remove any set avatar.
	 */
	void removeAvatar();

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the user's avatar image to an existing image (by Reference).
	 * 
	 * @param ref
	 *        The existing image file Reference.
	 */
	void setAvatar(Reference ref);

	/**
	 * Set the user's avatar image.
	 * 
	 * @param name
	 *        The upload file name.
	 * @param size
	 *        The file size.
	 * @param type
	 *        The file mime type.
	 * @param contents
	 *        The image file's contents.
	 */
	void setAvatar(String name, int size, String type, InputStream contents);

	/**
	 * Set the user's connect AIM id.
	 * 
	 * @param aim
	 *        The user's AIM id, or null to clear.
	 */
	void setConnectAim(String aim);

	/**
	 * Set the user's connect Facebook id.
	 * 
	 * @param facebook
	 *        The user's connect Facebook id, or null to clear.
	 */
	void setConnectFacebook(String facebook);

	/**
	 * Set the user's connect Google+ id.
	 * 
	 * @param googlePlus
	 *        The user's Google+ id, or null to clear.
	 */
	void setConnectGooglePlus(String googlePlus);

	/**
	 * Set the user's connect LinkedIn id.
	 * 
	 * @param linkedIn
	 *        The user's LinkedIn id, or null to clear.
	 */
	void setConnectLinkedIn(String linkedIn);

	/**
	 * Set the user's connect Skype id.
	 * 
	 * @param skype
	 *        The user's Skype id, or null to clear.
	 */
	void setConnectSkype(String skype);

	/**
	 * Set the user's connect Twitter id.
	 * 
	 * @param twitter
	 *        The user's Twitter id, or null to clear.
	 */
	void setConnectTwitter(String twitter);

	/**
	 * Set the user's connect web URL.
	 * 
	 * @param web
	 *        The user's web URL, or null to clear.
	 */
	void setConnectWeb(String web);

	/**
	 * Set the user's Etudes (login) id.
	 * 
	 * @param eid
	 *        The Etudes (login) id. EID is case insensitive, but stored as given.
	 */
	void setEid(String eid);

	/**
	 * Set the user's preference for exposing their email to users other than their instructor.
	 * 
	 * @param isEmailExposed
	 *        TRUE to expose, FALSE to hide.
	 */
	void setEmailExposed(Boolean isEmailExposed);

	/**
	 * Set the user's official email address (from roster processing).
	 * 
	 * @param email
	 *        The email address.
	 */
	void setEmailOfficial(String email);

	/**
	 * Set the user's use-set email address.
	 * 
	 * @param email
	 *        The email address, or null to clear it out.
	 */
	void setEmailUser(String email);

	/**
	 * Set the user's set of IIDs, replacing any currently held.
	 * 
	 * @param iids
	 *        The new set of IIDs, or null to clear all iids for the user.
	 */
	void setIids(List<Iid> iids);

	/**
	 * Set the user's first name.
	 * 
	 * @param firstName
	 *        The first name.
	 */
	void setNameFirst(String firstName);

	/**
	 * Set the user's last name.
	 * 
	 * @param lastName
	 *        The last name.
	 */
	void setNameLast(String lastName);

	/**
	 * Set the user's password (encrypted from the given plain text).
	 * 
	 * @param password
	 *        The plain text password.
	 */
	void setPassword(String password);

	/**
	 * Set the user's profile interests.
	 * 
	 * @param interests
	 *        The interests string, or null to clear it out.
	 */
	void setProfileInterests(String interests);

	/**
	 * Set the user's profile location.
	 * 
	 * @param location
	 *        The location string, or null to clear it out.
	 */
	void setProfileLocation(String location);

	/**
	 * Set the user's profile occupation.
	 * 
	 * @param occupation
	 *        The occupation string, or null to clear it out.
	 */
	void setProfileOccupation(String occupation);

	/**
	 * Set if this is a roster-created user.
	 * 
	 * @param isRosterUser
	 *        TRUE if the user is roster created, FALSE if not.
	 */
	void setRosterUser(Boolean isRosterUser);

	/**
	 * Set the user's signature HTML.
	 * 
	 * @param signature
	 *        The signature HTML, or null to clear it out.
	 * @param downloadReferenceFormat
	 *        if true, the content has embedded references in download (not placeholder) format.
	 */
	void setSignature(String signature, boolean downloadReferenceFormat);

	/**
	 * Set the user's preferred time zone.
	 * 
	 * @param timeZone
	 *        The time zone
	 */
	void setTimeZone(String timeZone);
}
