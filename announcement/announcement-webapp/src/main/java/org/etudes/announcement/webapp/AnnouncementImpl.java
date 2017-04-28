/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/announcement/announcement-webapp/src/main/java/org/etudes/announcement/webapp/AnnouncementImpl.java $
 * $Id: AnnouncementImpl.java 11871 2015-10-19 20:19:57Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015 Etudes, Inc.
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

package org.etudes.announcement.webapp;

import static org.etudes.util.Different.different;
import static org.etudes.util.StringUtil.trimToNull;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.announcement.api.Announcement;
import org.etudes.announcement.api.AnnouncementService;
import org.etudes.cdp.api.CdpService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * AnnouncementImpl implements Announcement.
 */
public class AnnouncementImpl implements Announcement
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AnnouncementImpl.class);

	protected boolean changed = false;
	protected Long contentReferenceId = null;
	protected User createdBy = null;
	protected Date createdOn = null;
	protected Long id = null;
	protected Boolean isPublic = Boolean.FALSE;
	protected boolean loaded = false;
	protected User modifiedBy = null;
	protected Date modifiedOn = null;
	protected String newContent = null;
	protected Integer order = null;
	protected Boolean published = Boolean.FALSE;
	protected Date releaseDate = null;
	protected boolean removeContent = false;
	protected File sharedContent = null;
	protected Site site = null;
	protected String siteName = null;

	protected String subject = null;

	/**
	 * Construct empty.
	 */
	public AnnouncementImpl()
	{
	}

	/**
	 * Construct with just an id.
	 * 
	 * @param id
	 *        The id.
	 */
	public AnnouncementImpl(Long id)
	{
		initId(id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof AnnouncementImpl)) return false;
		AnnouncementImpl other = (AnnouncementImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public Date getBylineDate()
	{
		if (getReleaseDate() != null) return getReleaseDate();
		return getModifiedOn();
	}

	@Override
	public String getContent(boolean forDownload)
	{
		Reference ref = getContentReference();
		if (ref != null)
		{
			String content = ref.getFile().readString();
			if (forDownload) content = fileService().processContentPlaceholderToDownload(content, getReference());

			return content;
		}

		return null;
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
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Boolean getIsReleased()
	{
		if (!getPublished()) return Boolean.FALSE;
		if (getReleaseDate() == null) return Boolean.TRUE;

		if (getReleaseDate().after(new Date())) return Boolean.FALSE;

		return Boolean.TRUE;
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
	public Integer getOrder()
	{
		return this.order;
	}

	@Override
	public Boolean getPublic()
	{
		return this.isPublic;
	}

	@Override
	public Boolean getPublished()
	{
		load();
		return this.published;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(this.site, Tool.announcement, this.id);
	}

	@Override
	public Set<User> getReferencedUsers()
	{
		Set<User> rv = new HashSet<User>();
		rv.add(getCreatedBy());
		rv.add(getModifiedBy());

		return rv;
	}

	@Override
	public Date getReleaseDate()
	{
		load();
		return this.releaseDate;
	}

	@Override
	public Site getSite()
	{
		load();
		return this.site;
	}

	@Override
	public String getSubject()
	{
		load();
		return this.subject;
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
	public void read(String prefix, Map<String, Object> parameters)
	{
		Boolean published = cdpService().readBoolean(parameters.get(prefix + "published"));
		Date releaseDate = cdpService().readDate(parameters.get(prefix + "releaseDate"));
		String subject = cdpService().readString(parameters.get(prefix + "title"));
		String content = cdpService().readString(parameters.get(prefix + "content"));
		Boolean isPublic = cdpService().readBoolean(parameters.get(prefix + "isPublic"));

		setPublished(published);
		setReleaseDate(releaseDate);
		setSubject(subject);
		setContent(content, true);
		setPublic(isPublic);
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> announcementMap = new HashMap<String, Object>();

		announcementMap.put("id", getId().toString());
		announcementMap.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) announcementMap.put("createdOn", getCreatedOn());
		announcementMap.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) announcementMap.put("modifiedOn", getModifiedOn());
		announcementMap.put("order", getOrder());
		announcementMap.put("published", getPublished());
		if (getReleaseDate() != null) announcementMap.put("releaseDate", getReleaseDate());
		announcementMap.put("released", getIsReleased());
		announcementMap.put("bylineDate", getBylineDate());
		announcementMap.put("isPublic", getPublic());
		announcementMap.put("title", getSubject());
		announcementMap.put("content", getContent(true));

		return announcementMap;
	}

	@Override
	public void setContent(String content, boolean downloadReferenceFormat)
	{
		load();

		content = trimToNull(content);

		// make sure content ends up with embedded reference URLs in placeholder format
		if (downloadReferenceFormat) content = fileService().processContentDownloadToPlaceholder(content);

		if (different(content, this.getContent(false)))
		{
			this.newContent = content;
			this.removeContent = (content == null);
			this.sharedContent = null;
			this.changed = true;
		}
	}

	@Override
	public void setOrder(Integer order)
	{
		load();
		if (order == null) return;

		if (different(order, this.order))
		{
			this.changed = true;
			this.order = order;
		}
	}

	@Override
	public void setPublic(Boolean isPublic)
	{
		load();
		if (isPublic == null) return;

		if (different(isPublic, this.isPublic))
		{
			this.changed = true;
			this.isPublic = isPublic;
		}
	}

	@Override
	public void setPublished(Boolean published)
	{
		load();
		if (published == null) return;

		if (different(published, this.published))
		{
			this.changed = true;
			this.published = published;
		}
	}

	@Override
	public void setReleaseDate(Date date)
	{
		load();
		if (different(date, this.releaseDate))
		{
			this.changed = true;
			this.releaseDate = date;
		}
	}

	public void setSubject(String subject)
	{
		load();
		if (different(subject, this.subject))
		{
			this.subject = subject;
			this.changed = true;
		}
	}

	@Override
	public void shareContent(File file)
	{
		load();

		if (different(file, this.sharedContent))
		{
			this.sharedContent = file;
			this.newContent = null;
			this.removeContent = false;
			this.changed = true;
		}
	}

	/**
	 * Mark the assessment as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected Reference getContentReference()
	{
		return fileService().getReference(this.contentReferenceId);
	}

	protected Long getContentReferenceId()
	{
		return this.contentReferenceId;
	}

	protected void initContentReferenceId(Long id)
	{
		this.contentReferenceId = id;
	}

	protected void initCreatedBy(User user)
	{
		this.createdBy = user;
	}

	protected void initCreatedOn(Date createdOn)
	{
		this.createdOn = createdOn;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initModifiedBy(User user)
	{
		this.modifiedBy = user;
	}

	protected void initModifiedOn(Date modifiedOn)
	{
		this.modifiedOn = modifiedOn;
	}

	protected void initOrder(Integer order)
	{
		this.order = order;
	}

	protected void initPublic(Boolean isPublic)
	{
		this.isPublic = isPublic;
	}

	protected void initPublished(Boolean published)
	{
		this.published = published;
	}

	protected void initReleaseDate(Date date)
	{
		this.releaseDate = date;
	}

	protected void initSite(Site site)
	{
		this.site = site;
	}

	protected void initSiteName(String name)
	{
		this.siteName = name;
	}

	protected void initSubject(String subject)
	{
		this.subject = subject;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * @return true if loaded, false if not.
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

		announcementService().refresh(this);
	}

	protected void saveContent(User savedBy)
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		// update the content
		this.contentReferenceId = fileService().savePrivateFile(this.removeContent, this.newContent, "announcement.html", "text/html",
				this.sharedContent, this.contentReferenceId, getReference(), Role.guest, keepers);

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		this.newContent = null;
		this.removeContent = false;
		this.sharedContent = null;
	}

	/**
	 * Set that the full assessment information has been loaded.
	 */
	protected void setLoaded()
	{
		this.loaded = true;
	}

	/**
	 * @return The registered AnnouncementService.
	 */
	private AnnouncementService announcementService()
	{
		return (AnnouncementService) Services.get(AnnouncementService.class);
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
}
