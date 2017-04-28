/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/site/api/Site.java $
 * $Id: Site.java 11770 2015-10-05 21:52:28Z ggolden $
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

package org.etudes.site.api;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.etudes.roster.api.Client;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.Term;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * Site models an Etudes site.
 */
public interface Site
{
	public enum AccessStatus
	{
		closed(2), open(0), willOpen(1);

		private final Integer id;

		private AccessStatus(int id)
		{
			this.id = Integer.valueOf(id);
		}

		public Integer getId()
		{
			return this.id;
		}
	}

	/**
	 * Make sure the site has these tools - add any missing.
	 * 
	 * @param tools
	 *        The desired tools.
	 */
	void assureTools(Set<Tool> tools);

	/**
	 * @return The site's access status (based on publication settings and the calendar).
	 */
	AccessStatus getAccessStatus();

	/**
	 * @return The site's client.
	 */
	Client getClient();

	/**
	 * @return the creating user.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * @return The site id.
	 */
	Long getId();

	/**
	 * @return The list of Links defined for this site.
	 */
	List<Link> getLinks();

	/**
	 * @return the last modifying user.
	 */
	User getModifiedBy();

	/**
	 * @return the date last modified.
	 */
	Date getModifiedOn();

	/**
	 * @return The site's name.
	 */
	String getName();

	/**
	 * @return The List of Tools enabled for this site, ordered.
	 */
	List<Tool> getOrderedTools();

	/**
	 * @return TRUE if the site is set as published, FALSE if not. Use isPublished() to test if the site is currently published, based on all settings (i.e. dates).
	 */
	Boolean getPublished();

	/**
	 * @return The publication date for the site, or null if not set.
	 */
	Date getPublishOn();

	/**
	 * @return The site's skin.
	 */
	Skin getSkin();

	/**
	 * @return Get the site's term.
	 */
	Term getTerm();

	/**
	 * @return The Set of Tools enabled for this site.
	 */
	Set<Tool> getTools();

	/**
	 * @return The un-publication date for the site, or null if not set.
	 */
	Date getUnpublishOn();

	/**
	 * @return TRUE if the site is currently published, FALSE if not, considering all settings (i.e. dates).
	 */
	Boolean isPublished();

	/**
	 * Format for sending via CDP.
	 * 
	 * @param role
	 *        The user's role in the site (optional).
	 * @param withActivity
	 *        if TRUE, include site activity info.
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send(Role role, Boolean withActivity);

	/**
	 * Format the site activity for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> sendActivity();

	/**
	 * Format for sending via CDP, just what the portal needs.
	 * 
	 * @param role
	 *        The user's role in the site (optional).
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> sendForPortal(Role role);

	/**
	 * Format for sending via CDP, short (id/title/role only) form.
	 * 
	 * @param role
	 *        The user's role in the site (optional).
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> sendIdTitleRole(Role role);

	/**
	 * Set the site's client.
	 * 
	 * @param client
	 *        The client.
	 */
	void setClient(Client client);

	/**
	 * Set the links for the site.
	 */
	void setLinks(List<Link> links);

	/**
	 * Set the site name.
	 * 
	 * @param name
	 *        The name.
	 */
	void setName(String name);

	/**
	 * Set the site's publication status.
	 * 
	 * @param published
	 *        TRUE for published, FALSE for not.
	 */
	void setPublished(Boolean published);

	/**
	 * Set the site's publication date.
	 * 
	 * @param publishOn
	 *        The publication date, or null to set no date.
	 */
	void setPublishOn(Date publishOn);

	/**
	 * Set the site's skin.
	 * 
	 * @param skin
	 *        The skin.
	 */
	void setSkin(Skin skin);

	/**
	 * Set the site's term.
	 * 
	 * @param term
	 *        The term.
	 */
	void setTerm(Term term);

	/**
	 * Set the tools in the site.
	 * 
	 * @param Tools
	 *        The Set of Tools for the site.
	 */
	void setTools(Set<Tool> tools);

	/**
	 * Set the site's unpublication date.
	 * 
	 * @param unpublishOn
	 *        The unpublication date, or null to set no date.
	 */
	void setUnpublishOn(Date unpublishOn);

	/**
	 * Encapsulate data into a Link for this site.
	 * 
	 * @param id
	 *        The id.
	 * @param title
	 *        The title.
	 * @param url
	 *        The url.
	 * @param position
	 *        The position.
	 * @return The Link.
	 */
	Link wrap(Long id, String title, String url, Integer position);
}
