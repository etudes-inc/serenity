/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-webapp/src/main/java/org/etudes/blog/webapp/BlogImpl.java $
 * $Id: BlogImpl.java 10088 2015-02-18 23:22:59Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

package org.etudes.blog.webapp;

import static org.etudes.util.Different.different;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.etudes.blog.api.Blog;
import org.etudes.blog.api.BlogEntry;
import org.etudes.blog.api.BlogService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * User implementation.
 */
public class BlogImpl implements Blog
{
	/** Our log. */
	// private static Log M_log = LogFactory.getLog(BlogImpl.class);

	protected boolean changed = false;

	protected User createdByUser;

	protected Date createdOn = null;

	protected List<BlogEntry> entries = null;

	protected Long id = null;

	protected User modifiedBy = null;;

	protected Date modifiedOn = null;

	protected String name = null;

	protected User owner = null;

	protected Site site = null;

	/**
	 * Construct.
	 * 
	 * @param userServiceImpl
	 */
	public BlogImpl()
	{
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof BlogImpl)) return false;
		BlogImpl other = (BlogImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public BlogEntry findEntryByTitle(String title)
	{
		List<BlogEntry> entries = getEntries();
		for (BlogEntry e : entries)
		{
			if (e.getTitle().equals(title)) return e;
		}

		return null;
	}

	@Override
	public User getCreatedBy()
	{
		return this.createdByUser;
	}

	@Override
	public Date getCreatedOn()
	{
		return this.createdOn;
	}

	@Override
	public List<BlogEntry> getEntries()
	{
		if (this.entries == null)
		{
			this.entries = blogService().getEntries(this);
		}

		return this.entries;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public User getModifiedBy()
	{
		return modifiedBy;
	}

	@Override
	public Date getModifiedOn()
	{
		return this.modifiedOn;
	}

	@Override
	public String getName()
	{
		return this.name;
	}

	@Override
	public User getOwner()
	{
		return this.owner;
	}

	@Override
	public Set<User> getReferencedUsers()
	{
		Set<User> rv = new HashSet<User>();
		rv.add(getCreatedBy());
		rv.add(getModifiedBy());
		rv.add(getOwner());

		return rv;
	}

	@Override
	public Site getSite()
	{
		return this.site;
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
	public void setName(String name)
	{
		if (different(name, this.name))
		{
			this.changed = true;
			this.name = name;
		}
	}

	@Override
	public void setOwner(User owner)
	{
		if (different(owner, this.owner))
		{
			this.changed = true;
			this.owner = owner;
		}
	}

	/**
	 * Mark the blog as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected void initCreatedBy(User user)
	{
		this.createdByUser = user;
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

	protected void initName(String name)
	{
		this.name = name;
	}

	protected void initOwner(User user)
	{
		this.owner = user;
	}

	protected void initSite(Site site)
	{
		this.site = site;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * @return The registered BlogService.
	 */
	private BlogService blogService()
	{
		return (BlogService) Services.get(BlogService.class);
	}
}
