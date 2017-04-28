/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/RosterService.java $
 * $Id: RosterService.java 10895 2015-05-20 02:59:07Z ggolden $
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

package org.etudes.roster.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.etudes.cron.api.RunTime;
import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * The E3 Roster service. Maintains rosters, and answers questions about them. Associates rosters to sites, in a many to many relationship.
 */
public interface RosterService
{
	/** The term id for project - align with the default terms in roster.sql. */
	public final static Long TERM_PROJECT = Long.valueOf(5l);

	/** Some well-known roster ids. */
	final Long ADMIN = 1L;
	final Long HELPDESK = 2L;
	final Long USERSGROUP = 4L; // the adhoc roster

	/**
	 * Add a mapping between the site and roster.
	 * 
	 * @param site
	 *        The site.
	 * @param roster
	 *        The roster.
	 */
	void addMapping(Site site, Roster roster);

	/**
	 * Create a new roster.
	 * 
	 * @param name
	 *        The roster name.
	 * @param official
	 *        TRUE if an official roster, FALSE if adhoc.
	 * @param client
	 *        For this client.
	 * @param term
	 *        In this term.
	 * @return The new roster.
	 */
	Roster addRoster(String name, Boolean official, Client client, Term term);

	/**
	 * Block this user from access to the site.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 */
	void block(Site site, User user);

	/**
	 * Check if a user is blocked in the site.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 * @return TRUE if the user is blocked, FALSE if not.
	 */
	Boolean checkBlocked(Site site, User user);

	/**
	 * Check if the site and roster are mapped.
	 * 
	 * @param site
	 *        The site.
	 * @param roster
	 *        The roster.
	 * @return TRUE if the site uses the roster, FALSE if not.
	 */
	Boolean checkMapping(Site site, Roster roster);

	/**
	 * Remove this site's mapping to any rosters, and any of those rosters that are no longer used. Leave only instructors in the site's master roster.
	 * 
	 * @param site
	 *        The site.
	 */
	void clear(Site site);

	/**
	 * Create an Client object, populated from the result set, in the field order of our "sqlSelectClientFragment" SQL code, starting at index.
	 * 
	 * @param result
	 *        The SQL ResultSet
	 * @param index
	 *        The starting index
	 * @return The Client object.
	 */
	Client createClientFromResultSet(ResultSet result, int index) throws SQLException;

	/**
	 * Create a site, either from template or defaults.
	 * 
	 * @param name
	 *        The site name.
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @return The site.
	 */
	Site createSite(String name, Client client, Term term);

	/**
	 * Create an Term object, populated from the result set, in the field order of our "sqlSelectTermFragment" SQL code, starting at index.
	 * 
	 * @param result
	 *        The SQL ResultSet
	 * @param index
	 *        The starting index
	 * @return The Client object.
	 */
	Term createTermFromResultSet(ResultSet result, int index) throws SQLException;

	/**
	 * Email the user that we just added her to a site.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 * @param role
	 *        The user's role in the site.
	 * @param newUserPassword
	 *        if set, the user was just created with this password; if null, the user has an established Etudes account.
	 * @param actor
	 *        The user performing the action.
	 */
	void emailUserAddedToSite(Site site, User user, Role role, String newUserPassword, User actor);

	/**
	 * Get the aggregate active member list for a site based on all the rosters it uses
	 * 
	 * @param site
	 *        The site.
	 * @return The site membership.
	 */
	Membership getActiveSiteMembers(Site site);

	/**
	 * @return A List of all terms that have a site or roster defined.
	 */
	List<Term> getActiveTerms();

	/**
	 * Get the adhoc roster for this site - the roster that the instructor populates for non-registrar members.
	 * 
	 * @param site
	 *        The site.
	 * @return The roster, or null if not found.
	 */
	Roster getAdhocRoster(Site site);

	/**
	 * Find the aggregate member list for a site based on all the rosters it uses. Each member will identify the roster it comes from.
	 * 
	 * @param site
	 *        The site.
	 * @return A Membership containing the aggregate members of the site. May be empty.
	 */
	Membership getAggregateSiteRoster(Site site);

	/**
	 * Find the aggregate member list for a site, based on all the rosters it uses, for users having this role. Each member will identify the roster it comes from.
	 * 
	 * @param site
	 *        The site.
	 * @param role
	 *        The role.
	 * @param activeOnly
	 *        if TRUE, incldude only active / non-blocked members, else include all with the role.
	 * @return A Membership containing the aggregate members of the site with this role. May be empty.
	 */
	Membership getAggregateSiteRosterForRole(Site site, Role role, Boolean activeOnly);

	/**
	 * Get a client.
	 * 
	 * @param id
	 *        The client id.
	 * @return The Client, or null if not found.
	 */
	Client getClient(Long id);

	/**
	 * Get a client from the abbreviation.
	 * 
	 * @param abbreviation
	 *        The client abbreviation.
	 * @return The Client, or null if not found.
	 */
	Client getClient(String abbreviation);

	/**
	 * @return A List of all clients.
	 */
	List<Client> getClients();

	/**
	 * Get the official roster for this site - like a section roster, one that holds the "official" (registrar like) instructors and TAs.
	 * 
	 * @param site
	 *        The site.
	 * @return The roster, or null if not found.
	 */
	Roster getMasterRoster(Site site);

	/**
	 * Get a roster.
	 * 
	 * @param id
	 *        The roster id.
	 * @return The roster, or null if not found.
	 */
	Roster getRoster(Long id);

	/**
	 * Get a roster by name, client and term.
	 * 
	 * @param name
	 *        The name.
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @return The roster, or null if not found.
	 */
	Roster getRoster(String name, Client client, Term term);

	/**
	 * Get the times that automatic file processing will run - if at all on this server.
	 * 
	 * @return The List of RunTime, or empty if none.
	 */
	List<RunTime> getRosterFileProcessingSchedule();

	/**
	 * Get the files ready to be processed.
	 * 
	 * @return The list of <java.io.File>.
	 */
	List<java.io.File> getRosterFiles();

	/**
	 * Get the membership for this roster.
	 * 
	 * @param roster
	 *        The roster.
	 * @return The roster Membership.
	 */
	Membership getRosterMembership(Roster roster);

	/**
	 * Get all the rosters for a client in a term.
	 * 
	 * @param client
	 *        For this client.
	 * @param term
	 *        In this term.
	 * @return The list of Roster, may be empty.
	 */
	List<Roster> getRosters(Client client, Term term);

	/**
	 * Get a term.
	 * 
	 * @param id
	 *        The term id.
	 * @return The Term, or null if not found.
	 */
	Term getTerm(Long id);

	/**
	 * Get a term from the abbreviation.
	 * 
	 * @param id
	 *        The term id.
	 * @param abbreviation
	 *        The term abbreviation.
	 * 
	 * @return The Term, or null if not found.
	 */
	Term getTerm(String abbreviation);

	/**
	 * @return A List of all terms.
	 */
	List<Term> getTerms();

	/**
	 * Start background processing of all ready roster files.
	 */
	void processFiles();

	/**
	 * Process these roster lines.
	 * 
	 * @param text
	 *        The roster lines.
	 * @param client
	 *        The client for these lines - used only for lines that the site name does not identify a client.
	 * @param term
	 *        The term for these lines - used only for lines that the site name does not identify a term.
	 * @param createInstructors
	 *        if TRUE, allow new instructors to be created.
	 * @param addToUG
	 *        if TRUE, add new instructors to the Users Group.
	 * @param createSitesWithoutRosters
	 *        if TRUE, allow sites to be created that have no rosters.
	 */
	void processRosterText(String text, Client client, Term term, Boolean createInstructors, Boolean addToUG, Boolean createSitesWithoutRosters);

	/**
	 * Remove this site's own rosters, it's mapping to any rosters, and any of those rosters that are no longer used.
	 * 
	 * @param site
	 *        The site.
	 */
	void purge(Site site);

	/**
	 * Remove this user from all rosters.
	 * 
	 * @param user
	 *        The user.
	 */
	void remove(User user);

	/**
	 * Remove a mapping between the site and roster. Neither are deleted.
	 * 
	 * @param site
	 *        The site.
	 * @param roster
	 *        The roster.
	 */
	void removeMapping(Site site, Roster roster);

	/**
	 * Remove all this roster mapping from all sites that use it.
	 * 
	 * @param site
	 *        The site.
	 */
	void removeMappings(Roster roster);

	/**
	 * Remove all the mapping for all the site's "official" rosters (not adhoc or master).
	 * 
	 * @param site
	 *        The site.
	 */
	void removeMappings(Site site);

	/**
	 * Remove this roster, it's mappings and membership.
	 * 
	 * @param roster
	 *        The roster.
	 */
	void removeRoster(Roster roster);

	/**
	 * Find all the rosters used by a site.
	 * 
	 * @param site
	 *        The site.
	 * @return The list of Roster used by the site. May be empty.
	 */
	List<Roster> rostersForSite(Site site);

	/**
	 * Find all the sites that use this roster.
	 * 
	 * @param roster
	 *        The roster.
	 * @return The list of Site, may be empty.
	 */
	List<Site> sitesForRoster(Roster roster);

	/**
	 * Find the sites that this user has access to. Ordered by term (DESC) then site name (ASC).
	 * 
	 * @param user
	 *        The user.
	 * @return A List of SiteMembers capturing sites and roles in sites, for this user's sites.
	 */
	List<SiteMember> sitesForUser(User user);

	/**
	 * Return a join statement to add to any item that is stored "by site", to select the items in the user's site(s) in which the user is an active member and the site is published. User id must be provided in the fields (only one field is used by this
	 * SQL).
	 * 
	 * @param joinSiteFrom
	 *        The clause to join site from, such as "A.SITE_ID". Note: Site will be "S"
	 * @return The sql.
	 */
	String sqlJoinItemBySiteFragment(String joinSiteFrom);

	/**
	 * Get a SQL fragment that can be added to any select statement that joins with the CLIENT C table to read in the client.
	 * 
	 * @return The SQL fragment.
	 */
	String sqlSelectClientFragment();

	/**
	 * @return The size of the index increment used up by the sqlSelectClientFragment - i.e. the # fields.
	 */
	Integer sqlSelectClientFragmentNumFields();

	/**
	 * Get a SQL fragment that can be added to any select statement that joins with the TERM T table to read in the term.
	 * 
	 * @return The SQL fragment.
	 */
	String sqlSelectTermFragment();

	/**
	 * @return The size of the index increment used up by the sqlSelectTermFragment - i.e. the # fields.
	 */
	Integer sqlSelectTermFragmentNumFields();

	/**
	 * Unblock the user, restoring access to the site.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 */
	void unblock(Site site, User user);

	/**
	 * Save any membership changes to the roster.
	 * 
	 * @param roster
	 *        The roster.
	 */
	void updateRoster(Roster roster);

	/**
	 * Find the user's membership in the site, based on the site's aggregate roster membership.
	 * 
	 * @param user
	 *        The user
	 * @param site
	 *        THe site.
	 * @return The Member data for the user in the site, or null if not found.
	 */
	Member userMemberInSite(User user, Site site);

	/**
	 * Find the user's role in the site, based on the site's aggregate roster membership.
	 * 
	 * @param user
	 *        The user.
	 * @param site
	 *        The site.
	 * @return The user's role in the site, or Role.none if the user has no role.
	 */
	Role userRoleInSite(User user, Site site);
}
