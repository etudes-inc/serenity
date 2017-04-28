/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/site/api/SiteService.java $
 * $Id: SiteService.java 12553 2016-01-14 20:03:28Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015, 2016 Etudes, Inc.
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.etudes.roster.api.Client;
import org.etudes.roster.api.Term;
import org.etudes.user.api.User;

/**
 * The Serenity Site service.
 */
public interface SiteService
{
	/** Some well-known site ids. */
	final Long ADMIN = 1L;
	final Long MONITOR = 2L;
	final Long HELPDESK = 3L;
	final Long USERSGROUP = 4L;

	/**
	 * Create a new site, assigning a new site id.
	 * 
	 * @param name
	 *        The site name.
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @param addedBy
	 *        The user doing the adding.
	 * @return The added Site.
	 */
	Site add(String name, Client client, Term term, User addedBy);

	/**
	 * Check that this id is a valid site id. Use check() instead of get() if you don't need the full site information loaded.
	 * 
	 * @param id
	 *        The site id.
	 * @return A Site object with the id (at least) set, or null if not found
	 */
	Site check(Long id);

	/**
	 * As a companion to the paging find(), count the total sites across all pages with this criteria.
	 * 
	 * @param client
	 *        Client criteria - if specified, return sites only from this client.
	 * @param term
	 *        The term criteria - if specified, return sites only from this term.
	 * @param search
	 *        The search string - partial match against title if specified.
	 * @return The count of sites meeting the criteria.
	 */
	Integer count(Client client, Term term, String search);

	/**
	 * Create an Site object, populated from the result set, in the field order of our "sqlSelectFragment" SQL code, starting at index.
	 * 
	 * @param result
	 *        The SQL ResultSet
	 * @param index
	 *        The starting index
	 * @return The Site object.
	 */
	Site createFromResultSet(ResultSet result, int index) throws SQLException;

	/**
	 * Find all the sites for this client in this term
	 * 
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @return A List of Site, may be empty.
	 */
	List<Site> find(Client client, Term term);

	/**
	 * Find a page of sites meeting this criteria.
	 * 
	 * @param client
	 *        Client criteria - if specified, return sites only from this client.
	 * @param term
	 *        The term criteria - if specified, return sites only from this term.
	 * @param search
	 *        The search string - partial match against title if specified.
	 * @param byTerm
	 *        if TRUE, order by term / name, else order by creation date / name
	 * @param pageNum
	 *        The 1 based page number to get.
	 * @param pageSize
	 *        The number of sites per page.
	 * @return The List of sites meeting the criteria.
	 */
	List<Site> find(Client client, Term term, String search, Boolean byTerm, Integer pageNum, Integer pageSize);

	/**
	 * Get a site.
	 * 
	 * @param id
	 *        The site id.
	 * @return The Site, or null if not found.
	 */
	Site get(Long id);

	/**
	 * Find a site by name.
	 * 
	 * @param name
	 *        The site name.
	 * @return The site, or null if not found.
	 */
	Site get(String name);

	/**
	 * Get the skin to be used by this client
	 * 
	 * @param name
	 *        The skin name
	 * @return The skin, or the default skin if the name is not found.
	 */
	Skin getSkin(Client client);

	/**
	 * Get the skin.
	 * 
	 * @param id
	 *        The skin id
	 * @return The skin, or the default skin if the id is not found.
	 */
	Skin getSkin(Long id);

	/**
	 * @return A List of all skins.
	 */
	List<Skin> getSkins();

	/**
	 * Remove this site, completely, from Etudes.
	 * 
	 * @param site
	 *        The site.
	 */
	void remove(Site site);

	/**
	 * Save any changes made to this site.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param site
	 *        The site to save.
	 */
	void save(User savedBy, Site site);

	/**
	 * If using sqlSelectFragment, make sure to add this in the JOIN part of the query, providing the field to use when joining to SITE to match S.ID
	 * 
	 * @param on
	 *        the field to use when joining to SITE to match S.ID.
	 * @return The SQL fragment.
	 */
	String sqlJoinFragment(String on);

	/**
	 * Get a SQL fragment that can be added to any select statement that joins with the CLIENT C table to read in the client.
	 * 
	 * @return The SQL fragment.
	 */
	String sqlSelectFragment();

	/**
	 * @return The size of the index increment used up by the sqlSelectClientFragment - i.e. the # fields.
	 */
	Integer sqlSelectFragmentNumFields();

	/**
	 * Encapsulate a site id into a Site object. The site id is not checked.
	 * 
	 * @param id
	 *        The site id.
	 * @return A Site object with this id set.
	 */
	Site wrap(Long id);
}
