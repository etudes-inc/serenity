/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-webapp/src/main/java/org/etudes/home/webapp/HomeItemImpl.java $
 * $Id: HomeItemImpl.java 11578 2015-09-07 00:43:58Z ggolden $
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

package org.etudes.home.webapp;

import static org.etudes.util.Different.different;
import static org.etudes.util.StringUtil.trimToNull;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.home.api.HomeItem;
import org.etudes.home.api.HomeService;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

public class HomeItemImpl implements HomeItem
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(HomeItemImpl.class);

	protected String alt = null;
	protected boolean changed = false;
	protected Long contentReferenceId = null;
	protected User createdBy = null;
	protected Date createdOn = null;
	protected String dimensions = null;
	protected Long id = null;
	protected boolean loaded = false;
	protected User modifiedBy = null;
	protected Date modifiedOn = null;
	protected Reference myFileReference = null;
	protected String newContent = null;
	protected Boolean published = Boolean.FALSE;
	protected Date releaseDate = null;
	protected boolean removeContent = false;
	protected File sharedContent = null;
	protected Site site = null;
	protected Source source = Source.web;
	protected Status status = Status.unknown;
	protected String title = null;
	protected String url = null;

	// /**
	// * Construct as a copy of another.
	// *
	// * @param other
	// * The other.
	// */
	// public HomeItemImpl(HomeItemImpl other)
	// {
	// init(other);
	// }

	/**
	 * Construct empty.
	 */
	public HomeItemImpl()
	{
	}

	/**
	 * Construct with just an id.
	 * 
	 * @param id
	 *        The id.
	 */
	public HomeItemImpl(Long id)
	{
		initId(id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof HomeItemImpl)) return false;
		HomeItemImpl other = (HomeItemImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public String getAlt()
	{
		load();
		return this.alt;
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
	public String getDimensions()
	{
		load();
		return this.dimensions;
	}

	@Override
	public Long getId()
	{
		return this.id;
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
	public Boolean getPublished()
	{
		load();
		return this.published;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(this.site, Tool.home, this.id);
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
	public Source getSource()
	{
		load();
		return this.source;
	}

	@Override
	public Status getStatus()
	{
		load();
		return this.status;
	}

	@Override
	public String getTitle()
	{
		load();
		return this.title;
	}

	@Override
	public String getUrl()
	{
		load();
		return this.url;
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
	public Boolean isValid()
	{
		boolean rv = true;

		switch (getSource())
		{
			case authored:
			{
				// nothing to check
				break;
			}
			case file:
			{
				// make sure we have a reference to a file
				if (getContentReference() == null)
				{
					// we might be about to get one ...
					if (this.myFileReference == null) rv = false;
				}
				break;
			}
			case web:
			{
				// make sure we have a url
				if (getUrl() == null) rv = false;
				break;
			}
			case youtube:
			{
				// make sure we have a url
				if (getUrl() == null) rv = false;
				break;
			}
		}

		// make sure we have a release date
		if (getReleaseDate() == null) rv = false;

		return Boolean.valueOf(rv);
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		String title = cdpService().readString(parameters.get(prefix + "title"));
		Boolean published = cdpService().readBoolean(parameters.get(prefix + "published"));
		Date releaseDate = cdpService().readDate(parameters.get(prefix + "releaseOn"));
		Source source = Source.fromCode(cdpService().readString(parameters.get(prefix + "source")));

		setTitle(title);
		setPublished(published);
		setReleaseDate(releaseDate);
		setSource(source);

		switch (source)
		{
			case authored:
			{
				String content = cdpService().readString(parameters.get(prefix + "content"));
				setContent(content, true);
				break;
			}
			case file:
			{
				Long refId = cdpService().readLong(parameters.get(prefix + "fileRefId"));
				String alt = cdpService().readString(parameters.get(prefix + "alt"));

				Reference myFilesRef = fileService().getReference(refId);
				if (myFilesRef != null)
				{
					useMyFile(myFilesRef);
				}
				setAlt(alt);

				break;
			}
			case web:
			{
				String url = cdpService().readString(parameters.get(prefix + "url"));
				String alt = cdpService().readString(parameters.get(prefix + "alt"));
				String height = cdpService().readString(parameters.get(prefix + "height"));

				setUrl(url);
				setAlt(alt);
				setDimensions(height);

				break;
			}
			case youtube:
			{
				String youtubeId = cdpService().readString(parameters.get(prefix + "youtubeId"));
				String ratio = cdpService().readString(parameters.get(prefix + "ratio"));

				setUrl(youtubeId);
				setDimensions(ratio);
				break;
			}
		}
	}

	@Override
	public Map<String, Object> send(User user)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());
		rv.put("title", getTitle());
		rv.put("published", getPublished());
		rv.put("status", getStatus().getCode());
		rv.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) rv.put("createdOn", getCreatedOn());
		rv.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) rv.put("modifiedOn", getModifiedOn());
		if (getReleaseDate() != null) rv.put("releaseOn", getReleaseDate());

		rv.put("source", getSource().getCode());
		switch (getSource())
		{
			case authored:
			{
				rv.put("content", getContent(true));
				break;
			}
			case file:
			{
				Reference ref = getContentReference();
				if (ref != null)
				{
					// fileUrl - the download url for our reference to the file
					rv.put("fileUrl", ref.getDownloadUrl());

					rv.put("fileName", ref.getFile().getName());

					// fileRefId - the myfiles reference to the selected file
					if (user != null)
					{
						Reference myFilesRef = fileService().getReference(ref, user);
						if (myFilesRef != null)
						{
							rv.put("fileRefId", myFilesRef.getId());
						}
					}
				}

				rv.put("alt", getAlt());
				break;
			}
			case web:
			{
				rv.put("url", getUrl());
				rv.put("alt", getAlt());
				rv.put("height", getDimensions());
				break;
			}
			case youtube:
			{
				rv.put("youtubeId", getUrl());
				rv.put("ratio", getDimensions());
				break;
			}
		}

		return rv;
	}

	@Override
	public void setAlt(String alt)
	{
		load();

		if (different(alt, this.alt))
		{
			this.changed = true;
			this.alt = alt;
		}
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
			this.sharedContent = null;
			this.removeContent = (content == null);
			this.changed = true;
		}
	}

	@Override
	public void setDimensions(String dimensions)
	{
		load();

		if (different(dimensions, this.dimensions))
		{
			this.changed = true;
			this.dimensions = dimensions;
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

	@Override
	public void setSource(Source source)
	{
		load();
		if (source == null) return;

		if (different(source, this.source))
		{
			this.changed = true;
			this.source = source;
		}
	}

	public void setTitle(String title)
	{
		load();
		if (different(title, this.title))
		{
			this.title = title;
			this.changed = true;
		}
	}

	@Override
	public void setUrl(String url)
	{
		load();

		if (different(url, this.url))
		{
			this.changed = true;
			this.url = url;
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

	@Override
	public void useMyFile(Reference ref)
	{
		load();

		if (different(ref, this.myFileReference))
		{
			this.myFileReference = ref;
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

	protected void initAlt(String alt)
	{
		this.alt = alt;
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

	protected void initDimensions(String dimensions)
	{
		this.dimensions = dimensions;
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

	protected void initSource(Source source)
	{
		this.source = source;
	}

	protected void initStatus(Status status)
	{
		this.status = status;
	}

	protected void initTitle(String title)
	{
		this.title = title;
	}

	protected void initUrl(String url)
	{
		this.url = url;
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

		homeService().itemRefresh(this);
	}

	protected void saveContent(User savedBy)
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		// update the content for authored items
		if (getSource() == Source.authored)
		{
			this.contentReferenceId = fileService().savePrivateFile(this.removeContent, this.newContent, "homeitem.html", "text/html",
					this.sharedContent, this.contentReferenceId, getReference(), Role.guest, keepers);
		}

		// update the myFile selected for file items
		else if (getSource() == Source.file)
		{
			this.contentReferenceId = fileService().saveMyFile(this.removeContent, null, 0, null, savedBy, null, this.myFileReference,
					this.contentReferenceId, getReference(), Role.guest, keepers);
		}

		else
		{
			this.contentReferenceId = null;
		}

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		this.newContent = null;
		this.removeContent = false;
		this.sharedContent = null;
		this.myFileReference = null;
	}

	/**
	 * Set that the full assessment information has been loaded.
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
	 * @return The registered HomeService.
	 */
	private HomeService homeService()
	{
		return (HomeService) Services.get(HomeService.class);
	}
}
