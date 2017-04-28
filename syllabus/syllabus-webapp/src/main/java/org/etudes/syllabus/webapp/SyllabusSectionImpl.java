/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/java/org/etudes/syllabus/webapp/SyllabusSectionImpl.java $
 * $Id: SyllabusSectionImpl.java 11578 2015-09-07 00:43:58Z ggolden $
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

package org.etudes.syllabus.webapp;

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
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.syllabus.api.Syllabus;
import org.etudes.syllabus.api.SyllabusSection;
import org.etudes.syllabus.api.SyllabusService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * SyllabusImpl implements Syllabus.
 */
public class SyllabusSectionImpl implements SyllabusSection
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SyllabusServiceImpl.class);

	protected boolean changed = false;

	protected Long contentReferenceId = null;

	protected User createdBy = null;

	protected Date createdOn = null;

	protected Long id = null;

	protected Boolean isPublic = Boolean.FALSE;

	protected User modifiedBy = null;

	protected Date modifiedOn = null;

	protected String newContent = null;

	protected Integer order = null;

	protected Boolean published = Boolean.FALSE;

	protected boolean removeContent = false;

	protected File sharedContent = null;

	protected Syllabus syllabus = null;

	protected Long syllabusId = null;

	protected String title = null;

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
		return this.createdBy;
	}

	@Override
	public Date getCreatedOn()
	{
		return this.createdOn;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public User getModifiedBy()
	{
		return this.modifiedBy;
	}

	@Override
	public Date getModifiedOn()
	{
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
		return this.published;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(getSyllabus().getSite(), Tool.syllabus, this.id);
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
	public Syllabus getSyllabus()
	{
		if (this.syllabus == null)
		{
			this.syllabus = syllabusService().get(this.syllabusId);
		}

		return this.syllabus;
	}

	@Override
	public String getTitle()
	{
		return this.title;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		String title = cdpService().readString(parameters.get(prefix + "title"));
		Integer order = cdpService().readInt(parameters.get(prefix + "order"));
		Boolean published = cdpService().readBoolean(parameters.get(prefix + "published"));
		Boolean isPublic = cdpService().readBoolean(parameters.get(prefix + "isPublic"));
		String content = cdpService().readString(parameters.get(prefix + "content"));

		setTitle(title);
		setOrder(order);
		setPublished(published);
		setPublic(isPublic);
		setContent(content, true);
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId().toString());
		rv.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) rv.put("createdOn", cdpService().sendDate(getCreatedOn()));
		rv.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) rv.put("modifiedOn", cdpService().sendDate(getModifiedOn()));
		rv.put("title", getTitle());
		rv.put("order", cdpService().sendInt(getOrder()));
		rv.put("published", getPublished());
		rv.put("isPublic", getPublic());
		rv.put("content", getContent(true));

		return rv;
	}

	@Override
	public void setContent(String content, boolean downloadReferenceFormat)
	{
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
		if (different(order, this.order))
		{
			this.changed = true;
			this.order = order;
		}
	}

	@Override
	public void setPublic(Boolean isPublic)
	{
		if (different(isPublic, this.isPublic))
		{
			this.changed = true;
			this.isPublic = isPublic;
		}
	}

	@Override
	public void setPublished(Boolean published)
	{
		if (different(published, this.published))
		{
			this.changed = true;
			this.published = published;
		}
	}

	@Override
	public void setTitle(String title)
	{
		if (different(title, this.title))
		{
			this.changed = true;
			this.title = title;
		}
	}

	@Override
	public void shareContent(File file)
	{
		if (different(file, this.sharedContent))
		{
			this.sharedContent = file;
			this.newContent = null;
			this.removeContent = false;
			this.changed = true;
		}
	}

	/**
	 * Mark as having no changes.
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

	protected Long getSyllabusId()
	{
		return this.syllabusId;
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

	protected void initSyllabusId(Long id)
	{
		this.syllabusId = id;
	}

	protected void initTitle(String title)
	{
		this.title = title;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	protected void saveContent(User savedBy)
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		// update the content
		this.contentReferenceId = fileService().savePrivateFile(this.removeContent, this.newContent, "section.html", "text/html", this.sharedContent,
				this.contentReferenceId, getReference(), Role.guest, keepers);

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		this.newContent = null;
		this.removeContent = false;
		this.sharedContent = null;
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
	 * @return The registered SyllabusService.
	 */
	private SyllabusService syllabusService()
	{
		return (SyllabusService) Services.get(SyllabusService.class);
	}
}
