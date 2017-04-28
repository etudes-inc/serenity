/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/java/org/etudes/syllabus/webapp/SyllabusImpl.java $
 * $Id: SyllabusImpl.java 11454 2015-08-15 04:13:37Z ggolden $
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.syllabus.api.Syllabus;
import org.etudes.syllabus.api.SyllabusExternal;
import org.etudes.syllabus.api.SyllabusSection;
import org.etudes.syllabus.api.SyllabusService;
import org.etudes.user.api.User;

/**
 * SyllabusImpl implements Syllabus.
 */
public class SyllabusImpl implements Syllabus
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SyllabusServiceImpl.class);

	protected boolean changed = false;

	protected User createdBy = null;

	protected Date createdOn = null;

	protected SyllabusExternalImpl external = new SyllabusExternalImpl();

	protected Long id = null;

	protected User modifiedBy = null;

	protected Date modifiedOn = null;

	protected List<SyllabusSection> removedSections = new ArrayList<SyllabusSection>();

	protected List<SyllabusSection> sections = null;

	protected Site site = null;

	Source source = Source.sections;

	@Override
	public SyllabusSection findSectionById(Long id)
	{
		for (SyllabusSection section : getSections())
		{
			if (!different(section.getId(), id))
			{
				return section;
			}
		}

		return null;
	}

	@Override
	public SyllabusSection findSectionByTitle(String title)
	{
		for (SyllabusSection section : getSections())
		{
			if (!different(section.getTitle(), title))
			{
				return section;
			}
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
	public SyllabusExternal getExternal()
	{
		return this.external;
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
	};

	@Override
	public Set<User> getReferencedUsers()
	{
		Set<User> rv = new HashSet<User>();
		rv.add(getCreatedBy());
		rv.add(getModifiedBy());

		return rv;
	}

	@Override
	public List<SyllabusSection> getSections()
	{
		if (this.sections == null)
		{
			this.sections = syllabusService().getSections(this);
		}

		return this.sections;
	}

	@Override
	public Site getSite()
	{
		return this.site;
	}

	@Override
	public Source getSource()
	{
		return this.source;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		Source source = Source.fromCode(cdpService().readString(parameters.get(prefix + "source")));
		setSource(source);

		// Note: external and sections read separately
	}

	@Override
	public void removeSection(SyllabusSection section)
	{
		if (section == null) return;
		boolean removed = getSections().remove(section);
		if (removed)
		{
			this.removedSections.add(section);
			this.changed = true;
		}
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("source", getSource().getCode());

		rv.put("id", getId());
		rv.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) rv.put("createdOn", cdpService().sendDate(getCreatedOn()));
		rv.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) rv.put("modifiedOn", cdpService().sendDate(getModifiedOn()));

		rv.put("external", getExternal().send());

		List<Map<String, Object>> sectionsMap = new ArrayList<Map<String, Object>>();
		rv.put("sections", sectionsMap);
		for (SyllabusSection section : getSections())
		{
			sectionsMap.add(section.send());
		}

		return rv;
	}

	@Override
	public void setSource(Source source)
	{
		if (different(source, this.source))
		{
			this.changed = true;
			this.source = source;
		}
	}

	/**
	 * Mark the blog as having no changes.
	 */
	protected void clearChanged()
	{
		this.external.clearChanged();
		if (this.sections != null)
		{
			for (SyllabusSection s : this.sections)
			{
				((SyllabusSectionImpl) s).clearChanged();
			}
		}
		this.changed = false;
	}

	protected void clearRemovedSections()
	{
		this.removedSections = new ArrayList<SyllabusSection>();
	}

	protected List<SyllabusSection> getRemovedSections()
	{
		return this.removedSections;
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

	protected void initSite(Site site)
	{
		this.site = site;
	}

	protected void initSource(Source source)
	{
		this.source = source;
	}

	protected boolean isChanged()
	{
		if (this.external.isChanged()) return true;
		if (this.sections != null)
		{
			for (SyllabusSection s : this.sections)
			{
				if (((SyllabusSectionImpl) s).isChanged()) return true;
			}
		}
		return this.changed;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered SyllabusService.
	 */
	private SyllabusService syllabusService()
	{
		return (SyllabusService) Services.get(SyllabusService.class);
	}
}
