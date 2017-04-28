/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/user/webapp/UserImpl.java $
 * $Id: UserImpl.java 12054 2015-11-10 22:01:23Z ggolden $
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

package org.etudes.user.webapp;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.etudes.util.Different.different;
import static org.etudes.util.StringUtil.split;
import static org.etudes.util.StringUtil.trimToNull;
import static org.etudes.util.StringUtil.trimToZero;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpService;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.Iid;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * User implementation.
 */
public class UserImpl implements User
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(UserImpl.class);

	protected InputStream avatarContents = null;

	protected Reference avatarFile = null;

	protected String avatarName = null;

	protected Long avatarReferenceId = null;

	protected boolean avatarRemove = false;

	protected int avatarSize = 0;

	protected String avatarType = null;

	protected boolean changed = false;

	protected String connectAim = null;

	protected String connectFacebook = null;

	protected String connectGooglePlus = null;

	protected String connectLinkedIn = null;

	protected String connectSkype = null;

	protected String connectTwitter = null;

	protected String connectWeb = null;

	protected User createdBy = null;

	protected Date createdOn = null;

	protected String eid = null;

	protected Boolean emailExposed = Boolean.FALSE;

	protected String emailOfficial = null;

	protected String emailUser = null;

	protected String firstName = null;

	protected Long id = null;

	protected List<Iid> iids = null;

	protected String lastName = null;

	protected boolean loaded = false;

	protected User modifiedBy = null;

	protected Date modifiedOn = null;

	protected List<Iid> origIids = new ArrayList<Iid>();

	protected String password = null;

	protected String profileInterests = null;

	protected String profileLocation = null;

	protected String profileOccupation = null;

	protected Boolean rosterUser = Boolean.FALSE;

	protected String signatureNew = null;

	protected Long signatureReferenceId = null;

	protected boolean signatureRemove = false;

	protected String timeZone = null;

	/**
	 * Construct.
	 */
	public UserImpl()
	{
	}

	@Override
	public Boolean checkPassword(String password)
	{
		load();
		String encoded = encodePassword(password);

		if (different(encoded, this.password)) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof UserImpl)) return false;
		UserImpl other = (UserImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public Reference getAvatar()
	{
		load();

		return fileService().getReference(this.avatarReferenceId);
	}

	@Override
	public String getConnectAim()
	{
		load();

		return this.connectAim;
	}

	@Override
	public String getConnectFacebook()
	{
		load();

		return this.connectFacebook;
	}

	@Override
	public String getConnectGooglePlus()
	{
		load();

		return this.connectGooglePlus;
	}

	@Override
	public String getConnectLinkedIn()
	{
		load();

		return this.connectLinkedIn;
	}

	@Override
	public String getConnectSkype()
	{
		load();

		return this.connectSkype;
	}

	@Override
	public String getConnectTwitter()
	{
		load();

		return this.connectTwitter;
	}

	@Override
	public String getConnectWeb()
	{
		load();

		return this.connectWeb;
	}

	@Override
	public User getCreatedBy()
	{
		load();

		return this.createdBy;
	}

	@Override
	public Date getCreatedOn()
	{
		load();

		return this.createdOn;
	}

	@Override
	public String getEid()
	{
		load();

		return this.eid;
	}

	@Override
	public String getEmailOfficial()
	{
		load();

		return this.emailOfficial;
	}

	@Override
	public String getEmailUser()
	{
		load();

		return this.emailUser;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public String getIidDisplay()
	{
		load();

		List<Iid> iids = getIids();

		StringBuilder buf = new StringBuilder();
		for (Iid iid : iids)
		{
			buf.append(iid);
			buf.append(", ");
		}

		// knock off the trailing ", "
		if (buf.length() > 0) buf.setLength(buf.length() - 2);
		return buf.toString();
	}

	@Override
	public String getIidDisplay(Client client)
	{
		load();

		List<Iid> iids = getIids();

		String rv = null;
		for (Iid iid : iids)
		{
			if (iid.getCode().equals(client.getIidCode()))
			{
				rv = iid.getId();
				break;
			}
		}

		return rv;
	}

	@Override
	public List<Iid> getIids()
	{
		load();

		if (this.iids == null)
		{
			List<Iid> iids = userService().getUserIid(this);
			this.iids = iids;
			this.origIids = new ArrayList<Iid>(this.iids);
		}

		// make a copy, sorted
		List<Iid> rv = new ArrayList<Iid>(this.iids);
		Collections.sort(rv);

		return rv;
	}

	@Override
	public User getModifiedBy()
	{
		load();

		return this.modifiedBy;
	}

	@Override
	public Date getModifiedOn()
	{
		load();

		return this.modifiedOn;
	}

	@Override
	public String getNameDisplay()
	{
		load();

		String rv = "?";
		if ((this.firstName == null) && (this.lastName == null))
		{
			// for the no-name user, use official email
			rv = this.emailOfficial;
		}
		else if ((this.firstName == null) || (this.lastName == null))
		{
			// for the one name user
			rv = trimToZero(this.firstName) + trimToZero(this.lastName);
		}
		else
		{
			rv = this.firstName + " " + this.lastName;
		}

		return rv;
	}

	@Override
	public String getNameFirst()
	{
		load();

		return this.firstName;
	}

	@Override
	public String getNameLast()
	{
		load();

		return this.lastName;
	}

	@Override
	public String getNameSort()
	{
		load();

		String rv = "?";
		if ((this.firstName == null) && (this.lastName == null))
		{
			// for the no-name user, use official email
			rv = this.emailOfficial;
		}
		else if ((this.firstName == null) || (this.lastName == null))
		{
			// for the one name user
			rv = trimToZero(this.firstName) + trimToZero(this.lastName);
		}
		else
		{
			rv = this.lastName + ", " + this.firstName;
		}

		return rv;
	}

	@Override
	public String getPasswordEncoded()
	{
		load();

		return this.password;
	}

	@Override
	public String getProfileInterests()
	{
		load();

		return this.profileInterests;
	}

	@Override
	public String getProfileLocation()
	{
		load();

		return this.profileLocation;
	}

	@Override
	public String getProfileOccupation()
	{
		load();

		return this.profileOccupation;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(siteService().wrap(0L), Tool.user, this.id);
	}

	@Override
	public String getSignature(boolean forDownload)
	{
		Reference ref = getSignatureReference();
		if (ref != null)
		{
			String signature = ref.getFile().readString();
			if (forDownload) signature = fileService().processContentPlaceholderToDownload(signature, getReference());

			return signature;
		}

		return null;
	}

	@Override
	public String getTimeZone()
	{
		load();

		return this.timeZone;
	}

	@Override
	public String getTimeZoneDflt()
	{
		String tz = getTimeZone();
		if (tz == null)
		{
			tz = "America/Los_Angeles";
		}

		return tz;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public Boolean isAdmin()
	{
		// admin is user id 1
		return (this.id.equals(Long.valueOf(UserService.ADMIN)));
	}

	@Override
	public Boolean isEmailExposed()
	{
		load();

		return this.emailExposed;
	}

	@Override
	public Boolean isRosterUser()
	{
		load();

		return this.rosterUser;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters, boolean isAdmin)
	{
		String connectAim = cdpService().readString(parameters.get(prefix + "connectAim"));
		String connectFacebook = cdpService().readString(parameters.get(prefix + "connectFacebook"));
		String connectGooglePlus = cdpService().readString(parameters.get(prefix + "connectGooglePlus"));
		String connectLinkedIn = cdpService().readString(parameters.get(prefix + "connectLinkedIn"));
		String connectSkype = cdpService().readString(parameters.get(prefix + "connectSkype"));
		String connectTwitter = cdpService().readString(parameters.get(prefix + "connectTwitter"));
		String connectWeb = cdpService().readString(parameters.get(prefix + "connectWeb"));
		String eid = cdpService().readString(parameters.get(prefix + "eid"));
		String iidList = cdpService().readString(parameters.get(prefix + "iid"));
		Boolean emailExposed = cdpService().readBoolean(parameters.get(prefix + "emailExposed"));
		String emailUser = cdpService().readString(parameters.get(prefix + "emailUser"));
		String nameFirst = cdpService().readString(parameters.get(prefix + "nameFirst"));
		String nameLast = cdpService().readString(parameters.get(prefix + "nameLast"));
		String profileInterests = cdpService().readString(parameters.get(prefix + "profileInterests"));
		String profileLocation = cdpService().readString(parameters.get(prefix + "profileLocation"));
		String profileOccupation = cdpService().readString(parameters.get(prefix + "profileOccupation"));
		String signature = cdpService().readString(parameters.get(prefix + "signature"));
		String timeZone = cdpService().readString(parameters.get(prefix + "timeZone"));
		String password = cdpService().readString(parameters.get(prefix + "newPassword"));
		String passwordVerify = cdpService().readString(parameters.get(prefix + "newPasswordVerify"));

		FileItem avatar = null;
		Object o = parameters.get(prefix + "avatarNew");
		if ((o != null) && (o instanceof FileItem))
		{
			avatar = (FileItem) o;
		}
		// make sure we got an image file
		if ((avatar != null) && (avatar.getSize() > 0) && (!avatar.getContentType().startsWith("image/")))
		{
			avatar = null;
		}
		Boolean avatarRemove = cdpService().readBoolean(parameters.get(prefix + "avatarRemove"));

		Long avatarRefId = cdpService().readLong(parameters.get(prefix + "avatarReference"));
		Reference avatarReference = null;
		if (avatarRefId != null)
		{
			avatarReference = fileService().getReference(avatarRefId);
		}

		if ((avatarRemove != null) && (avatarRemove))
		{
			removeAvatar();
		}
		else if (avatar != null)
		{
			try
			{
				setAvatar(avatar.getName(), (int) avatar.getSize(), avatar.getContentType(), avatar.getInputStream());
			}
			catch (IOException e)
			{
				M_log.warn("read: avatar: " + e.toString());
			}
		}
		else if (avatarReference != null)
		{
			setAvatar(avatarReference);
		}

		if (connectAim != null) setConnectAim(trimToNull(connectAim));
		if (connectFacebook != null) setConnectFacebook(trimToNull(connectFacebook));
		if (connectGooglePlus != null) setConnectGooglePlus(trimToNull(connectGooglePlus));
		if (connectLinkedIn != null) setConnectLinkedIn(trimToNull(connectLinkedIn));
		if (connectSkype != null) setConnectSkype(trimToNull(connectSkype));
		if (connectTwitter != null) setConnectTwitter(trimToNull(connectTwitter));
		if (connectWeb != null) setConnectWeb(trimToNull(connectWeb));

		// eid only if admin
		if (isAdmin)
		{
			if (eid != null) setEid(trimToNull(eid));
		}

		if (emailExposed != null) setEmailExposed(emailExposed);
		if (emailUser != null) setEmailUser(trimToNull(emailUser));

		// names only if admin, or user is non-roster
		if (isAdmin || (!isRosterUser()))
		{
			if (nameFirst != null) setNameFirst(trimToNull(nameFirst));
			if (nameLast != null) setNameLast(trimToNull(nameLast));
		}

		if (profileInterests != null) setProfileInterests(trimToNull(profileInterests));
		if (profileLocation != null) setProfileLocation(trimToNull(profileLocation));
		if (profileOccupation != null) setProfileOccupation(trimToNull(profileOccupation));
		if (signature != null) setSignature(trimToNull(signature), true);
		if (timeZone != null) setTimeZone(trimToNull(timeZone));
		if ((password != null) && (password.length() > 0) && (password.equals(passwordVerify))) setPassword(trimToNull(password));

		// IID only for admin
		if (isAdmin)
		{
			if (iidList != null)
			{
				List<Iid> iids = new ArrayList<Iid>();
				String[] iidDisplays = split(iidList, ", ");
				for (String display : iidDisplays)
				{
					iids.add(userService().makeIid(display));
				}

				setIids(iids);
			}
		}
	}

	@Override
	public void removeAvatar()
	{
		load();

		setAvatar(null, 0, null, null);
		avatarRemove = true;
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("id", getId().toString());

		Reference avatar = getAvatar();
		if (avatar != null)
		{
			String downloadUrl = avatar.getDownloadUrl();
			if (downloadUrl != null)
			{
				userMap.put("avatar", downloadUrl);
			}
		}

		userMap.put("connectAim", trimToZero(getConnectAim()));
		userMap.put("connectFacebook", trimToZero(getConnectFacebook()));
		userMap.put("connectGooglePlus", trimToZero(getConnectGooglePlus()));
		userMap.put("connectLinkedIn", trimToZero(getConnectLinkedIn()));
		userMap.put("connectSkype", trimToZero(getConnectSkype()));
		userMap.put("connectTwitter", trimToZero(getConnectTwitter()));
		userMap.put("connectWeb", trimToZero(getConnectWeb()));
		userMap.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) userMap.put("createdOn", cdpService().sendDate(getCreatedOn()));
		userMap.put("eid", getEid());
		userMap.put("iid", trimToZero(getIidDisplay()));
		userMap.put("emailExposed", isEmailExposed());
		userMap.put("emailOfficial", getEmailOfficial());
		userMap.put("emailUser", trimToZero(getEmailUser()));
		userMap.put("nameFirst", trimToZero(getNameFirst()));
		userMap.put("nameLast", trimToZero(getNameLast()));
		userMap.put("nameDisplay", getNameDisplay());
		userMap.put("nameSort", getNameSort());
		userMap.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) userMap.put("modifiedOn", cdpService().sendDate(getModifiedOn()));
		userMap.put("profileInterests", trimToZero(getProfileInterests()));
		userMap.put("profileLocation", trimToZero(getProfileLocation()));
		userMap.put("profileOccupation", trimToZero(getProfileOccupation()));
		userMap.put("rosterUser", isRosterUser());
		userMap.put("signature", trimToZero(getSignature(true)));
		userMap.put("timeZone", getTimeZoneDflt());
		userMap.put("admin", isAdmin());
		userMap.put("helpdesk", Boolean.valueOf(getId().equals(Long.valueOf(UserService.HELPDESK))));
		userMap.put("lang", "en-us"); // TODO:
		userMap.put("protected", userService().isProtected(this));

		return userMap;
	}

	@Override
	public void setAvatar(Reference ref)
	{
		if (this.avatarContents != null)
		{
			try
			{
				this.avatarContents.close();
			}
			catch (IOException e)
			{
				M_log.warn("setAvatar: closing prior stream: " + e.toString());
			}
		}

		this.avatarContents = null;
		this.avatarSize = 0;
		this.avatarName = null;
		this.avatarType = null;
		this.avatarRemove = false;

		this.avatarFile = ref;

		this.changed = true;
	}

	@Override
	public void setAvatar(String name, int size, String type, InputStream contents)
	{
		load();

		if (this.avatarContents != null)
		{
			try
			{
				this.avatarContents.close();
			}
			catch (IOException e)
			{
				M_log.warn("setAvatar: closing prior stream: " + e.toString());
			}
		}

		this.avatarContents = contents;
		this.avatarSize = size;
		this.avatarName = name;
		this.avatarType = type;
		this.avatarRemove = false;
		this.avatarFile = null;

		this.changed = true;
	}

	@Override
	public void setConnectAim(String aim)
	{
		load();

		if (different(aim, this.connectAim))
		{
			this.changed = true;
			this.connectAim = aim;
		}
	}

	@Override
	public void setConnectFacebook(String facebook)
	{
		load();

		if (different(facebook, this.connectFacebook))
		{
			this.changed = true;
			this.connectFacebook = facebook;
		}
	}

	@Override
	public void setConnectGooglePlus(String googlePlus)
	{
		load();

		if (different(googlePlus, this.connectGooglePlus))
		{
			this.changed = true;
			this.connectGooglePlus = googlePlus;
		}
	}

	@Override
	public void setConnectLinkedIn(String linkedIn)
	{
		load();

		if (different(linkedIn, this.connectLinkedIn))
		{
			this.changed = true;
			this.connectLinkedIn = linkedIn;
		}
	}

	@Override
	public void setConnectSkype(String skype)
	{
		load();

		if (different(skype, this.connectSkype))
		{
			this.changed = true;
			this.connectSkype = skype;
		}
	}

	@Override
	public void setConnectTwitter(String twitter)
	{
		load();

		if (different(twitter, this.connectTwitter))
		{
			this.changed = true;
			this.connectTwitter = twitter;
		}
	}

	@Override
	public void setConnectWeb(String web)
	{
		load();

		if (different(web, this.connectWeb))
		{
			this.changed = true;
			this.connectWeb = web;
		}
	}

	@Override
	public void setEid(String eid)
	{
		load();

		if (different(eid, this.eid))
		{
			this.changed = true;
			this.eid = eid;
		}
	}

	@Override
	public void setEmailExposed(Boolean isEmailExposed)
	{
		load();

		if (different(isEmailExposed, this.emailExposed))
		{
			this.changed = true;
			this.emailExposed = isEmailExposed;
		}
	}

	@Override
	public void setEmailOfficial(String email)
	{
		load();

		if (different(email, this.emailOfficial))
		{
			this.changed = true;
			this.emailOfficial = email;
		}
	}

	@Override
	public void setEmailUser(String email)
	{
		load();

		if (different(email, this.emailUser))
		{
			this.changed = true;
			this.emailUser = email;
		}
	}

	@Override
	public void setIids(List<Iid> iids)
	{
		load();

		// look for a change - order does not matter
		Set<Iid> newIids = new HashSet<Iid>(iids);
		Set<Iid> origIids = new HashSet<Iid>(this.origIids);
		if (!newIids.equals(origIids))
		{
			this.changed = true;
		}

		this.iids = iids;
	}

	@Override
	public void setNameFirst(String firstName)
	{
		load();

		if (different(firstName, this.firstName))
		{
			this.changed = true;
			this.firstName = firstName;
		}
	}

	@Override
	public void setNameLast(String lastName)
	{
		load();

		if (different(lastName, this.lastName))
		{
			this.changed = true;
			this.lastName = lastName;
		}
	}

	@Override
	public void setPassword(String password)
	{
		load();

		// reject (quietly) weak passwords
		if (!userService().strongPassword(password)) return;

		String encoded = encodePassword(password);
		if (different(encoded, this.password))
		{
			this.changed = true;
			this.password = encoded;
		}
	}

	@Override
	public void setProfileInterests(String interests)
	{
		load();

		if (different(interests, this.profileInterests))
		{
			this.changed = true;
			this.profileInterests = interests;
		}
	}

	@Override
	public void setProfileLocation(String location)
	{
		load();

		if (different(location, this.profileLocation))
		{
			this.changed = true;
			this.profileLocation = location;
		}
	}

	@Override
	public void setProfileOccupation(String occupation)
	{
		load();

		if (different(occupation, this.profileOccupation))
		{
			this.changed = true;
			this.profileOccupation = occupation;
		}
	}

	@Override
	public void setRosterUser(Boolean isRosterUser)
	{
		load();

		if (different(isRosterUser, this.rosterUser))
		{
			this.changed = true;
			this.rosterUser = isRosterUser;
		}
	}

	@Override
	public void setSignature(String signature, boolean downloadReferenceFormat)
	{
		load();

		// make sure signature ends up with embedded reference URLs in placeholder format
		if (downloadReferenceFormat && (signature != null)) signature = fileService().processContentDownloadToPlaceholder(signature);

		if (different(signature, this.getSignature(false)))
		{
			this.signatureNew = signature;
			signatureRemove = (signature == null);
			this.changed = true;
		}
	}

	@Override
	public void setTimeZone(String timeZone)
	{
		load();

		if (different(timeZone, this.timeZone))
		{
			this.changed = true;
			this.timeZone = timeZone;
		}
	}

	/**
	 * Mark the user as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	/**
	 * Encode the clear text password.
	 * 
	 * @param password
	 *        The clear text password.
	 * @return The encoded password, or null if there was a problem.
	 */
	protected String encodePassword(String password)
	{
		try
		{
			// static and dynamic salt the password
			String salted = "ETUDES" + password + this.id.toString();
			byte[] bytes = salted.getBytes("UTF-8");

			// digest with MD5, repeated a few times
			MessageDigest md = MessageDigest.getInstance("MD5");

			for (int i = 0; i < 1001; i++)
			{
				md.update(bytes);
				bytes = md.digest();
			}

			String encoded = encodeBase64String(bytes);
			return encoded;
		}
		catch (NoSuchAlgorithmException e)
		{
			M_log.warn("encodePassword: " + e.toString());
		}
		catch (UnsupportedEncodingException e)
		{
			M_log.warn("encodePassword: " + e.toString());
		}

		return null;
	}

	protected Long getAvatarReferenceId()
	{
		return this.avatarReferenceId;
	}

	protected Reference getSignatureReference()
	{
		return fileService().getReference(this.signatureReferenceId);
	}

	protected Long getSignatureReferenceId()
	{
		return this.signatureReferenceId;
	}

	protected void init(UserImpl other)
	{
		this.avatarReferenceId = other.avatarReferenceId;
		this.connectAim = other.connectAim;
		this.connectFacebook = other.connectFacebook;
		this.connectGooglePlus = other.connectGooglePlus;
		this.connectLinkedIn = other.connectLinkedIn;
		this.connectSkype = other.connectSkype;
		this.connectTwitter = other.connectTwitter;
		this.connectWeb = other.connectWeb;
		this.createdBy = other.createdBy;
		this.createdOn = other.createdOn;
		this.eid = other.eid;
		this.emailExposed = other.emailExposed;
		this.emailOfficial = other.emailOfficial;
		this.emailUser = other.emailUser;
		this.firstName = other.firstName;
		this.id = other.id;
		this.iids = other.iids;
		this.origIids = other.origIids;
		this.lastName = other.lastName;
		this.loaded = other.loaded;
		this.modifiedBy = other.modifiedBy;
		this.modifiedOn = other.modifiedOn;
		this.password = other.password;
		this.profileInterests = other.profileInterests;
		this.profileLocation = other.profileLocation;
		this.profileOccupation = other.profileOccupation;
		this.rosterUser = other.rosterUser;
		this.signatureReferenceId = other.signatureReferenceId;
		this.timeZone = other.timeZone;
	}

	protected void initAvatar(Long avatar)
	{
		this.avatarReferenceId = avatar;
	}

	protected void initConnectAim(String aim)
	{
		this.connectAim = aim;
	}

	protected void initConnectFacebook(String facebook)
	{
		this.connectFacebook = facebook;
	}

	protected void initConnectGooglePlus(String googlePlus)
	{
		this.connectGooglePlus = googlePlus;
	}

	protected void initConnectLinkedIn(String linkedIn)
	{
		this.connectLinkedIn = linkedIn;
	}

	protected void initConnectSkype(String skype)
	{
		this.connectSkype = skype;
	}

	protected void initConnectTwitter(String twitter)
	{
		this.connectTwitter = twitter;
	}

	protected void initConnectWeb(String web)
	{
		this.connectWeb = web;
	}

	protected void initCreatedBy(User user)
	{
		this.createdBy = user;
	}

	protected void initCreatedOn(Date createdOn)
	{
		this.createdOn = createdOn;
	}

	protected void initEid(String eid)
	{
		this.eid = eid;
	}

	protected void initEmailExposed(Boolean isEmailExposed)
	{
		this.emailExposed = isEmailExposed;
	}

	protected void initEmailOfficial(String email)
	{
		this.emailOfficial = email;
	}

	protected void initEmailUser(String email)
	{
		this.emailUser = email;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initIids(String iidList)
	{
		if (this.iids == null)
		{
			this.iids = new ArrayList<Iid>();

			if (iidList != null)
			{
				String[] iidDisplays = split(iidList, ",");
				for (String display : iidDisplays)
				{
					this.iids.add(new IidImpl(display));
				}
			}

			this.origIids = new ArrayList<Iid>(this.iids);
		}
	}

	protected void initModifiedBy(User user)
	{
		this.modifiedBy = user;
	}

	protected void initModifiedOn(Date modifiedOn)
	{

		this.modifiedOn = modifiedOn;
	}

	protected void initNameFirst(String firstName)
	{
		this.firstName = firstName;
	}

	protected void initNameLast(String lastName)
	{
		this.lastName = lastName;
	}

	protected void initPassword(String encoded)
	{
		this.password = encoded;
	}

	protected void initProfileInterests(String interests)
	{
		this.profileInterests = interests;
	}

	protected void initProfileLocation(String location)
	{
		this.profileLocation = location;
	}

	protected void initProfileOccupation(String occupation)
	{
		this.profileOccupation = occupation;
	}

	protected void initRosterUser(Boolean isRosterUser)
	{
		this.rosterUser = isRosterUser;
	}

	protected void initSignature(Long ref)
	{
		this.signatureReferenceId = ref;
	}

	protected void initTimeZone(String timeZone)
	{
		this.timeZone = timeZone;
	}

	/**
	 * Check if the user has any changes.
	 * 
	 * @return true if changed, false if not.
	 */
	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * Check if the full site data has been loaded.
	 * 
	 * @return true if fully loaded, false of only the id is set.
	 */
	protected boolean isLoaded()
	{
		return this.loaded;
	}

	/**
	 * If not fully loaded, load.
	 */
	protected void load()
	{
		if (this.loaded) return;

		userService().refresh(this);
	}

	protected void saveSignatureAndAvatar(User savedBy)
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		this.signatureReferenceId = fileService().savePrivateFile(this.signatureRemove, this.signatureNew, "signature.html", "text/html", null,
				this.signatureReferenceId, getReference(), Role.authenticated, keepers);

		// the avatar
		this.avatarReferenceId = fileService().saveMyFile(this.avatarRemove, this.avatarName, this.avatarSize, this.avatarType, this,
				this.avatarContents, this.avatarFile, this.avatarReferenceId, getReference(), Role.authenticated, keepers);

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		this.signatureNew = null;
		this.signatureRemove = false;
		this.avatarContents = null;
		this.avatarSize = 0;
		this.avatarName = null;
		this.avatarType = null;
		this.avatarRemove = false;
		this.avatarFile = null;
	}

	/**
	 * Set that the full user information has been loaded.
	 */
	protected void setLoaded()
	{
		this.loaded = true;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
