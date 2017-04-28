/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/site/webapp/SiteImpl.java $
 * $Id: SiteImpl.java 11773 2015-10-06 02:27:56Z ggolden $
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

package org.etudes.site.webapp;

import static org.etudes.util.Different.different;
import static org.etudes.util.StringUtil.split;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.etudes.roster.api.Client;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.Term;
import org.etudes.service.api.Services;
import org.etudes.site.api.Link;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.site.api.Skin;
import org.etudes.tool.api.Tool;
import org.etudes.tracking.api.TrackingService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * User implementation.
 */
public class SiteImpl implements Site
{
	/** Our log. */
	// private static Log M_log = LogFactory.getLog(SiteImpl.class);

	protected boolean changed = false;

	protected Client client = null;

	protected Long createdByUserId = null;

	protected Date createdOn = null;

	protected Long id = null;

	protected List<Link> links = null;

	protected boolean loaded = false;

	protected Long modifiedByUserId = null;

	protected Date modifiedOn = null;

	protected String name = null;

	protected List<Link> origLinks = new ArrayList<Link>();

	protected Set<Tool> origTools = new HashSet<Tool>();

	/** If true, the site is published, but if false, it may still be published if there are any publish or unpublish dates set, depending on "now". */
	protected Boolean published = Boolean.FALSE;

	protected Date publishOn = null;

	protected Skin skin = null;

	protected Term term = null;

	protected Set<Tool> tools = null;

	protected Date unpublishOn = null;

	/**
	 * Construct.
	 * 
	 * @param userServiceImpl
	 */
	public SiteImpl()
	{
	}

	@Override
	public void assureTools(Set<Tool> newTools)
	{
		Set<Tool> tools = getTools();
		tools.addAll(newTools);
		setTools(tools);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SiteImpl)) return false;
		SiteImpl other = (SiteImpl) obj;
		if (different(this.id, other.id)) return false;
		return true;
	}

	@Override
	public AccessStatus getAccessStatus()
	{
		if (isPublished()) return AccessStatus.open;
		if (getPublishOn() != null)
		{
			if (new Date().before(getPublishOn())) return AccessStatus.willOpen;
		}
		return AccessStatus.closed;
	}

	@Override
	public Client getClient()
	{
		load();
		return this.client;
	}

	@Override
	public User getCreatedBy()
	{
		load();
		if (this.createdByUserId == null) return null;
		return userService().get(this.createdByUserId);
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
	public List<Link> getLinks()
	{
		if (this.links == null)
		{
			this.links = siteService().loadLinks(this);
			this.origLinks = new ArrayList<Link>(this.links);
		}

		return new ArrayList<Link>(this.links);
	}

	@Override
	public User getModifiedBy()
	{
		load();
		if (this.modifiedByUserId == null) return null;
		return userService().get(this.modifiedByUserId);
	}

	@Override
	public Date getModifiedOn()
	{
		load();
		return this.modifiedOn;
	}

	@Override
	public String getName()
	{
		load();
		return this.name;
	}

	@Override
	public List<Tool> getOrderedTools()
	{
		Set<Tool> tools = getTools();
		List<Tool> ordered = new ArrayList<Tool>(tools);
		Collections.sort(ordered, new Tool.ToolOrderComparator());

		return ordered;
	}

	@Override
	public Boolean getPublished()
	{
		load();
		return this.published;
	}

	@Override
	public Date getPublishOn()
	{
		load();
		return this.publishOn;
	}

	@Override
	public Skin getSkin()
	{
		if (skin == null)
		{
			// default
			return null;
		}
		else
		{
			return this.skin;
		}
	}

	@Override
	public Term getTerm()
	{
		load();
		return this.term;
	}

	@Override
	public Set<Tool> getTools()
	{
		if (this.tools == null)
		{
			this.tools = siteService().loadTools(this);
			this.origTools = new HashSet<Tool>(this.tools);
		}

		return new HashSet<Tool>(this.tools);
	}

	@Override
	public Date getUnpublishOn()
	{
		load();
		return this.unpublishOn;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
		return result;
	}

	/**
	 * Init the site links, one at a time.
	 * 
	 * @param link
	 *        The site link to add.
	 */
	public void initLink(Link link)
	{
		if (this.links == null)
		{
			this.links = new ArrayList<Link>();
		}

		if (link != null)
		{
			this.links.add(link);
			this.origLinks.add(link);
		}
	}

	@Override
	public Boolean isPublished()
	{
		load();

		// if neither date is set, base on the published flag
		if ((this.publishOn == null) && (this.unpublishOn == null)) return this.published;

		Date now = new Date();

		// we are not published if now is before the set publish date
		if ((this.publishOn != null) && (now.before(this.publishOn))) return Boolean.FALSE;

		// we are not published if now is after the set unpublish date
		if ((this.unpublishOn != null) && (now.after(this.unpublishOn))) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	@Override
	public Map<String, Object> send(Role role, Boolean withActivity)
	{
		Map<String, Object> siteMap = new HashMap<String, Object>();

		siteMap.put("title", getName());
		siteMap.put("id", getId());
		if (role != null) siteMap.put("role", role.getLevel());
		siteMap.put("accessStatus", getAccessStatus().getId());
		if (getPublishOn() != null) siteMap.put("publishOn", getPublishOn());

		// get the site's tools, sorted for display order
		List<Map<String, Object>> toolsMap = new ArrayList<Map<String, Object>>();
		siteMap.put("tools", toolsMap);
		List<Tool> tools = getOrderedTools();
		for (Tool t : tools)
		{
			// skip tools the user does not have access to
			if (!role.ge(t.getRole())) continue;

			toolsMap.add(t.send());
		}

		// get the site's links, in order
		List<Link> links = getLinks();
		List<Map<String, Object>> linksMap = new ArrayList<Map<String, Object>>();
		siteMap.put("links", linksMap);
		for (Link l : links)
		{
			Map<String, Object> linkMap = new HashMap<String, Object>();
			linksMap.add(linkMap);

			linkMap.put("title", l.getTitle());
			linkMap.put("url", l.getUrl());

			// TODO: following not needed by portal
			linkMap.put("id", l.getId());
			linkMap.put("position", l.getPosition());
		}

		siteMap.put("published", isPublished());
		if (getUnpublishOn() != null) siteMap.put("unpublishOn", getUnpublishOn());
		if (getCreatedOn() != null) siteMap.put("createdOn", getCreatedOn());

		Map<String, Object> skinMap = new HashMap<String, Object>();
		siteMap.put("skin", skinMap);
		skinMap.put("id", getSkin().getId());
		skinMap.put("name", getSkin().getName());
		skinMap.put("color", getSkin().getColor());
		skinMap.put("client", getSkin().getClient());

		Map<String, Object> clientMap = new HashMap<String, Object>();
		siteMap.put("client", clientMap);
		clientMap.put("id", getClient().getId());
		clientMap.put("name", getClient().getName());

		Map<String, Object> termMap = new HashMap<String, Object>();
		siteMap.put("term", termMap);
		termMap.put("id", getTerm().getId());
		termMap.put("name", getTerm().getName());

		// setup and roster
		Map<String, Object> setupMap = new HashMap<String, Object>();
		siteMap.put("setupTool", setupMap);

		setupMap.put("id", Tool.sitesetup.getId());
		setupMap.put("title", Tool.sitesetup.getTitle());
		setupMap.put("url", Tool.sitesetup.getUrl());
		setupMap.put("role", Tool.sitesetup.getRole().getLevel());

		Map<String, Object> rosterMap = new HashMap<String, Object>();
		siteMap.put("rosterTool", rosterMap);

		rosterMap.put("id", Tool.siteroster.getId());
		rosterMap.put("title", Tool.siteroster.getTitle());
		rosterMap.put("url", Tool.siteroster.getUrl());
		rosterMap.put("role", Tool.siteroster.getRole().getLevel());

		// activity
		if (withActivity)
		{
			Map<String, Object> activityMap = sendActivity();
			siteMap.put("activity", activityMap);
		}

		return siteMap;
	}

	@Override
	public Map<String, Object> sendActivity()
	{
		Map<String, Object> activityMap = new HashMap<String, Object>();

		activityMap.put("online", trackingService().countPresence(this, Tool.portal, 0L));
		activityMap.put("unreadMessages", Integer.valueOf(0));
		activityMap.put("unreadPosts", Integer.valueOf(9999));
		activityMap.put("notVisitAlerts", Integer.valueOf(16));
		activityMap.put("reviewCount", Integer.valueOf(3));

		return activityMap;
	}

	@Override
	public Map<String, Object> sendForPortal(Role role)
	{
		Map<String, Object> siteMap = new HashMap<String, Object>();

		siteMap.put("title", getName());
		siteMap.put("id", getId());
		if (role != null) siteMap.put("role", role.getLevel());
		siteMap.put("accessStatus", getAccessStatus().getId());

		// get the site's tool ids, sorted for display order
		List<Integer> toolsArray = new ArrayList<Integer>();
		siteMap.put("tools", toolsArray);
		List<Tool> tools = getOrderedTools();
		for (Tool t : tools)
		{
			// skip tools the user does not have access to
			if (!role.ge(t.getRole())) continue;

			toolsArray.add(t.getId());
		}

		// get the site's links, in order
		List<Link> links = getLinks();
		List<Map<String, Object>> linksMap = new ArrayList<Map<String, Object>>();
		siteMap.put("links", linksMap);
		for (Link l : links)
		{
			Map<String, Object> linkMap = new HashMap<String, Object>();
			linksMap.add(linkMap);

			linkMap.put("title", l.getTitle());
			linkMap.put("url", l.getUrl());

			// TODO: following not needed by portal
			linkMap.put("id", l.getId());
			linkMap.put("position", l.getPosition());
		}

		return siteMap;
	}

	@Override
	public Map<String, Object> sendIdTitleRole(Role role)
	{
		Map<String, Object> siteMap = new HashMap<String, Object>();

		siteMap.put("id", getId());
		siteMap.put("title", getName());
		if (role != null)
		{
			siteMap.put("role", role.getLevel());
		}

		return siteMap;
	}

	@Override
	public void setClient(Client client)
	{
		load();

		if (different(client, this.client))
		{
			this.client = client;
			this.changed = true;
		}
	}

	@Override
	public void setLinks(List<Link> links)
	{
		// check for a real change (loads the links if needed)
		List<Link> oldLinks = getLinks();
		if (!oldLinks.equals(links))
		{
			this.links = links;
			this.changed = true;
		}

		// check for individual changes
		else
		{
			for (Link l : links)
			{
				Link orig = oldLinks.get(oldLinks.indexOf(l));
				if (!l.exactlyEqual(orig))
				{
					this.links = links;
					this.changed = true;
					break;
				}
			}
		}
	}

	@Override
	public void setName(String name)
	{
		load();
		if (different(name, this.name))
		{
			this.name = name;
			this.changed = true;
		}
	}

	@Override
	public void setPublished(Boolean published)
	{
		load();
		if (different(published, this.published) || (this.publishOn != null) || (this.unpublishOn != null))
		{
			this.published = published;
			this.publishOn = null;
			this.unpublishOn = null;
			this.changed = true;
		}
	}

	@Override
	public void setPublishOn(Date publishOn)
	{
		load();
		if (different(publishOn, this.publishOn) || this.published)
		{
			this.publishOn = publishOn;
			this.published = Boolean.FALSE;
			this.changed = true;
		}
	}

	@Override
	public void setSkin(Skin skin)
	{
		load();

		if (different(skin, this.skin))
		{
			this.skin = skin;
			this.changed = true;
		}
	}

	@Override
	public void setTerm(Term term)
	{
		load();

		if (different(term, this.term))
		{
			this.term = term;
			this.changed = true;
		}
	}

	@Override
	public void setTools(Set<Tool> tools)
	{
		// check for a real change (loads the tools if needed)
		Set<Tool> oldTools = getTools();
		if (!oldTools.equals(tools))
		{
			this.tools = tools;
			this.changed = true;
		}
	}

	@Override
	public void setUnpublishOn(Date unpublishOn)
	{
		load();
		if (different(unpublishOn, this.unpublishOn) || this.published)
		{
			this.unpublishOn = unpublishOn;
			this.published = Boolean.FALSE;
			this.changed = true;
		}
	}

	@Override
	public Link wrap(Long id, String title, String url, Integer position)
	{
		Link link = new LinkImpl(id, title, url, position);
		return link;
	}

	/**
	 * Mark as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected Long getClientId()
	{
		return this.client.getId();
	}

	protected Long getCreatedByUserId()
	{
		return this.createdByUserId;
	}

	protected Long getModifiedByUserId()
	{
		return this.modifiedByUserId;
	}

	protected Long getTermId()
	{
		return this.term.getId();
	}

	protected void initClient(Client client)
	{
		this.client = client;
	}

	protected void initCreatedByUserId(Long id)
	{
		this.createdByUserId = id;
	}

	protected void initCreatedOn(Date date)
	{
		this.createdOn = date;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initModifiedByUserId(Long id)
	{
		this.modifiedByUserId = id;
	}

	protected void initModifiedOn(Date date)
	{
		this.modifiedOn = date;
	}

	protected void initName(String name)
	{
		this.name = name;
	}

	protected void initPublished(Boolean published)
	{
		this.published = published;
	}

	protected void initPublishOn(Date date)
	{
		this.publishOn = date;
	}

	protected void initSkin(Skin skin)
	{
		this.skin = skin;
	}

	protected void initTerm(Term term)
	{
		this.term = term;
	}

	protected void initTools(String toolIds)
	{
		if (this.tools == null)
		{
			this.tools = new HashSet<Tool>();

			if (toolIds != null)
			{
				String[] ids = split(toolIds, ",");
				for (String id : ids)
				{
					this.tools.add(Tool.valueOf(Integer.valueOf(id)));
				}
			}

			this.origTools = new HashSet<Tool>(this.tools);
		}
	}

	protected void initUnpublishOn(Date date)
	{
		this.unpublishOn = date;
	}

	/**
	 * Check if there are any changes.
	 * 
	 * @return true if changed, false if not.
	 */
	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * If not fully loaded, load (all but tools and links).
	 */
	protected void load()
	{
		if (this.loaded) return;

		siteService().load(this);
	}

	/**
	 * Set that the full site information has been loaded.
	 */
	protected void setLoaded()
	{
		this.loaded = true;
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteServiceImpl siteService()
	{
		return (SiteServiceImpl) Services.get(SiteService.class);
	}

	/**
	 * @return The registered TrackingService.
	 */
	private TrackingService trackingService()
	{
		return (TrackingService) Services.get(TrackingService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
