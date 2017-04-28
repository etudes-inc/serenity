/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/roster/roster-webapp/src/main/java/org/etudes/roster/webapp/RosterCdpHandler.java $
 * $Id: RosterCdpHandler.java 12547 2016-01-13 21:34:50Z ggolden $
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

package org.etudes.roster.webapp;

import static org.etudes.util.StringUtil.split;
import static org.etudes.util.StringUtil.splitFirst;
import static org.etudes.util.StringUtil.splitLast;
import static org.etudes.util.StringUtil.trimToNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.EmailValidator;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.cron.api.RunTime;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.Roster;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.Term;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.user.api.Iid;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 */
public class RosterCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(RosterCdpHandler.class);

	public String getPrefix()
	{
		return "roster";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		if (authenticatedUser == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}
		else if (requestPath.equals("schedule"))
		{
			return dispatchSchedule(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("processFiles"))
		{
			return dispatchProcessFiles(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("processLines"))
		{
			return dispatchProcessLines(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("config"))
		{
			return dispatchConfig(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("rosters"))
		{
			return dispatchRosters(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("siteRoster"))
		{
			return dispatchSiteRoster(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("addSiteRoster"))
		{
			return dispatchAddSiteRoster(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("removeSiteRosterMapping"))
		{
			return dispatchRemoveSiteRosterMapping(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("removeSiteRosters"))
		{
			return dispatchRemoveSiteRosters(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("removeRosterSites"))
		{
			return dispatchRemoveRosterSites(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("aggregateRoster"))
		{
			return dispatchAggregateRoster(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("addMembers"))
		{
			return dispatchAddMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("removeMembers"))
		{
			return dispatchRemoveMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("blockMembers"))
		{
			return dispatchBlockMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("unblockMembers"))
		{
			return dispatchUnblockMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("roleMembers"))
		{
			return dispatchRoleMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("addMasterMembers"))
		{
			return dispatchAddMasterMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("removeMasterMembers"))
		{
			return dispatchRemoveMasterMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("groups"))
		{
			return dispatchGroups(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("group"))
		{
			return dispatchGroup(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("groupSave"))
		{
			return dispatchGroupSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("groupRemove"))
		{
			return dispatchGroupRemove(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("accessSave"))
		{
			return dispatchAccessSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("accessDelete"))
		{
			return dispatchAccessDelete(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchAccessDelete(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchAccessDelete: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchAccessDelete: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// the user
		Long userId = cdpService().readLong(parameters.get("user"));
		if (userId == null)
		{
			M_log.warn("dispatchAccessDelete: missing user");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// TODO: remove the global special access for this user

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAccessSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchAccessSave: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchAccessSave: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// the user
		Long userId = cdpService().readLong(parameters.get("user"));
		if (userId == null)
		{
			M_log.warn("dispatchAccessSave: missing user");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String extendTimeOption = cdpService().readString(parameters.get("access_extendTimeOption"));
		Integer extendDue = cdpService().readInt(parameters.get("access_extendDue"));
		Float extendTimeMultiplier = cdpService().readFloat(parameters.get("access_extendTimeMultiplier"));
		String extendTimeValue = cdpService().readString(parameters.get("access_extendTimeValue"));

		// TODO: save the global special access for this user

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAddMasterMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchAddOfficialMembers: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchAddOfficialMembers: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Integer roleLevel = cdpService().readInt(parameters.get("role"));
		if (roleLevel == null)
		{
			M_log.warn("dispatchAddOfficialMembers: missing role");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Role role = Role.valueOf(roleLevel);

		String users = trimToNull(cdpService().readString(parameters.get("users")));
		if (users == null)
		{
			M_log.warn("dispatchAddOfficialMembers: missing users");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] userIdentifiers = split(users, "\r\n");

		// any adds will be to the master roster
		Roster official = rosterService().getMasterRoster(site);
		if (official == null)
		{
			M_log.warn("dispatchAddMembers: missing site official roster: " + site.getId());

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// for detailed results
		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		// for checking current membership
		Membership aggregate = rosterService().getAggregateSiteRoster(site);

		// process each user identifier
		for (String userIdentifier : userIdentifiers)
		{
			User target = null;

			userIdentifier = trimToNull(userIdentifier);
			if (userIdentifier == null) continue;

			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultsList.add(resultMap);
			resultMap.put("ident", userIdentifier);

			// search for existing users ONLY by full IID
			String[] parts = splitLast(userIdentifier, "@");
			if (parts != null)
			{
				Iid iid = userService().makeIid(parts[0], parts[1]);
				target = userService().findByIid(iid);
			}

			// if we don't have a user, we cannot use this line
			if (target == null)
			{
				resultMap.put("status", "notAdded");
				continue;
			}

			// check that the user is not currently in the site
			if (aggregate.findUser(target) != null)
			{
				resultMap.put("name", target.getNameDisplay());
				resultMap.put("status", "alreadyMember");
				continue;
			}

			resultMap.put("name", target.getNameDisplay());
			resultMap.put("status", "userAdded");

			// add the user with the role to the adhoc roster
			official.getMembership().add(target, role, Boolean.TRUE);
		}

		// save
		rosterService().updateRoster(official);

		doSiteRoster(rv, site, true);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAddMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchAddMembers: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchAddMembers: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Integer roleLevel = cdpService().readInt(parameters.get("role"));
		if (roleLevel == null)
		{
			M_log.warn("dispatchAddMembers: missing role");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Role role = Role.valueOf(roleLevel);

		String users = trimToNull(cdpService().readString(parameters.get("users")));
		if (users == null)
		{
			M_log.warn("dispatchAddMembers: missing users");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] userIdentifiers = split(users, "\r\n");

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// any adds will be to the adhoc roster
		Roster adhoc = rosterService().getAdhocRoster(site);
		if (adhoc == null)
		{
			M_log.warn("dispatchAddMembers: missing site adhoc roster: " + site.getId());

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// for detailed results
		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		// for checking current membership
		Membership aggregate = rosterService().getAggregateSiteRoster(site);

		// process each user identifier
		for (String userIdentifier : userIdentifiers)
		{
			User target = null;
			boolean newUser = false;
			String password = null;

			userIdentifier = trimToNull(userIdentifier);
			if (userIdentifier == null) continue;

			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultsList.add(resultMap);
			resultMap.put("ident", userIdentifier);

			// search for existing users first by EID - need a user that is uniquely identified by the EID
			List<User> eidUsers = userService().findByEid(userIdentifier);
			if (eidUsers.size() == 1)
			{
				target = eidUsers.get(0);
			}
			// if there are too many users, we cannot use this line
			else if (eidUsers.size() > 1)
			{
				resultMap.put("status", "conflict");
				continue;
			}

			// search by full iid
			if (target == null)
			{
				String[] parts = splitLast(userIdentifier, "@");
				if (parts != null)
				{
					Iid iid = userService().makeIid(parts[0], parts[1]);
					target = userService().findByIid(iid);
				}
			}

			// search by partial iid - id only, using the site's client's code
			if (target == null)
			{
				Iid iid = userService().makeIid(userIdentifier, site.getClient().getIidCode());
				target = userService().findByIid(iid);
			}

			// search by email
			if (target == null)
			{
				List<User> emailUsers = userService().findByEmail(userIdentifier);
				if (emailUsers.size() == 1)
				{
					target = emailUsers.get(0);
				}
				// if there are too many users, we cannot use this line
				else if (emailUsers.size() > 1)
				{
					resultMap.put("status", "conflict");
					continue;
				}
			}

			// try to add a new user by email
			if (target == null)
			{
				EmailValidator validator = EmailValidator.getInstance();
				if (validator.isValid(userIdentifier))
				{
					target = userService().add(authenticatedUser);
					target.setEid(userIdentifier);
					target.setEmailOfficial(userIdentifier);

					// set password to a random positive number
					Random generator = new Random(System.currentTimeMillis());
					Integer num = new Integer(generator.nextInt(Integer.MAX_VALUE));
					if (num.intValue() < 0) num = new Integer(num.intValue() * -1);
					password = num.toString();
					target.setPassword(password);

					userService().save(authenticatedUser, target);
					newUser = true;
				}
			}

			// if we don't have a user, we cannot use this line
			if (target == null)
			{
				resultMap.put("status", "notAdded");
				continue;
			}

			// check that the user is not currently in the site
			if (aggregate.findUser(target) != null)
			{
				resultMap.put("name", target.getNameDisplay());
				resultMap.put("status", "alreadyMember");
				continue;
			}

			if (newUser)
			{
				resultMap.put("status", "userCreated");
			}
			else
			{
				resultMap.put("name", target.getNameDisplay());
				resultMap.put("status", "userAdded");
			}

			// add the user with the role to the adhoc roster
			adhoc.getMembership().add(target, role, Boolean.TRUE);

			// notify the user by email
			rosterService().emailUserAddedToSite(site, target, role, password, authenticatedUser);
		}

		// save
		rosterService().updateRoster(adhoc);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAddSiteRoster(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		String rosterName = trimToNull((String) parameters.get("roster"));
		String siteName = readSiteNameParam(parameters, "site");
		if ((rosterName == null) || (siteName == null))
		{
			M_log.warn("dispatchAddSiteRoster: incomplete parameters");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Long clientId = cdpService().readLong(parameters.get("client"));
		if (clientId == null)
		{
			M_log.warn("dispatchAddSiteRoster: missing client");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Client client = rosterService().getClient(clientId);
		if (client == null)
		{
			M_log.warn("dispatchAddSiteRoster: missing client: " + clientId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Long termId = cdpService().readLong(parameters.get("term"));
		if (termId == null)
		{
			M_log.warn("dispatchAddSiteRoster: missing term");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Term term = rosterService().getTerm(termId);
		if (term == null)
		{
			M_log.warn("dispatchAddSiteRoster: missing term: " + termId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// find or create the site
		boolean siteCreated = false;
		Site site = siteService().get(siteName);
		if (site == null)
		{
			site = rosterService().createSite(siteName, client, term);
			siteCreated = true;
		}

		// find or create the roster
		boolean rosterCreated = false;
		Roster roster = rosterService().getRoster(rosterName, client, term);
		if (roster == null)
		{
			roster = rosterService().addRoster(rosterName, Boolean.TRUE, client, term);
			rosterCreated = true;
		}

		// if the site does not already have the roster, add it
		boolean mappingAdded = false;
		if (!rosterService().checkMapping(site, roster))
		{
			rosterService().addMapping(site, roster);
			mappingAdded = true;
		}

		Map<String, Object> resultsMap = new HashMap<String, Object>();
		rv.put("results", resultsMap);
		resultsMap.put("site", Boolean.valueOf(siteCreated));
		resultsMap.put("roster", Boolean.valueOf(rosterCreated));
		resultsMap.put("mapping", Boolean.valueOf(mappingAdded));

		// return the rosters response
		doRosters(rv, client, term);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAggregateRoster(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchAggregateRoster: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchAggregateRoster: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> members = new ArrayList<Map<String, Object>>();
		Membership siteRoster = rosterService().getAggregateSiteRoster(site);
		for (Member m : siteRoster.getMembers())
		{
			members.add(m.send(null));
		}
		rv.put("members", members);

		// doSiteRoster(rv, site, false);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchBlockMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchBlockMembers: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchBlockMembers: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String ids = (String) parameters.get("users");
		if (ids == null)
		{
			M_log.warn("dispatchBlockMembers: missing users");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] idStrs = split(ids, "\t");

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// for detailed results
		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			User user = userService().get(id);
			if (user != null)
			{
				Map<String, Object> resultMap = new HashMap<String, Object>();
				resultsList.add(resultMap);
				resultMap.put("name", user.getNameDisplay());

				if (user.equals(authenticatedUser))
				{
					resultMap.put("status", "notBlocked");
				}
				else if (rosterService().checkBlocked(site, user))
				{
					resultMap.put("status", "alreadyBlocked");
				}
				else
				{
					rosterService().block(site, user);
					resultMap.put("status", "blocked");
				}
			}
			else
			{
				M_log.warn("dispatchBlockMembers: user not found: " + id);
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchConfig(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Boolean active = cdpService().readBoolean(parameters.get("active"));

		// clients
		List<Map<String, Object>> clientsMaps = new ArrayList<Map<String, Object>>();
		rv.put("clients", clientsMaps);
		List<Client> clients = rosterService().getClients();
		for (Client client : clients)
		{
			clientsMaps.add(client.send());
		}

		// terms
		List<Map<String, Object>> termsMaps = new ArrayList<Map<String, Object>>();
		rv.put("terms", termsMaps);

		List<Term> terms = null;
		if (active)
		{
			terms = rosterService().getActiveTerms();
		}
		else
		{
			terms = rosterService().getTerms();
		}

		for (Term term : terms)
		{
			termsMaps.add(term.send());
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGroup(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGroup: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchGroup: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long groupId = cdpService().readLong(parameters.get("group"));
		if (groupId == null)
		{
			M_log.warn("dispatchGroup: missing group");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		M_log.warn("dispatchGroup: id: " + groupId);

		// get the group
		// TODO:

		Map<String, Object> groupMap = new HashMap<String, Object>();
		rv.put("group", groupMap);

		groupMap.put("id", Long.valueOf(1));
		groupMap.put("title", "Group One");
		groupMap.put("size", Integer.valueOf(4));

		List<Long> members = new ArrayList<Long>();
		groupMap.put("members", members);

		// fake
		Membership siteRoster = rosterService().getAggregateSiteRoster(site);
		for (Member m : siteRoster.getMembers())
		{
			if ((m.getRole() == Role.student) && (m.isActive() && (!m.isBlocked())))
			{
				members.add(m.getUser().getId());
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGroupRemove(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGroupRemove: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchGroupRemove: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Long> groupIds = cdpService().readIds(parameters.get("groups"));
		if (groupIds == null)
		{
			M_log.warn("dispatchGroupRemove: missing groups");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// TODO:
		M_log.warn("dispatchGroupRemove: " + groupIds);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGroups(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGroups: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchGroups: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
		rv.put("groups", groups);

		Map<String, Object> groupMap = new HashMap<String, Object>();
		groupMap.put("id", Long.valueOf(1));
		groupMap.put("title", "Group One");
		groupMap.put("size", Integer.valueOf(4));
		groups.add(groupMap);

		groupMap = new HashMap<String, Object>();
		groupMap.put("id", Long.valueOf(2));
		groupMap.put("title", "Group Two");
		groupMap.put("size", Integer.valueOf(2));
		groups.add(groupMap);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGroupSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGroupSave: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchGroupSave: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long groupId = cdpService().readLong(parameters.get("group"));
		if (groupId == null)
		{
			M_log.warn("dispatchGroupSave: missing group");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the group

		// TODO:

		String title = cdpService().readString(parameters.get("title"));
		List<Long> memberUserIds = cdpService().readIds(parameters.get("members"));

		// update and save the group
		M_log.warn("dispatchGroupSave:  id: " + groupId + " title: " + title + " members: " + memberUserIds);

		// if the group was new, change the req attribute groupid to reflect the actual group id, so that a subsequent roster_group request will see it. TODO:
		if (groupId == -1)
		{
			parameters.put("group", "22");
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchProcessFiles(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		rosterService().processFiles();

		// TODO: return?

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchProcessLines(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long clientId = cdpService().readLong(parameters.get("client"));
		Client client = null;
		if (clientId != null)
		{
			client = rosterService().getClient(clientId);
		}

		Long termId = cdpService().readLong(parameters.get("term"));
		Term term = null;
		if (termId != null)
		{
			term = rosterService().getTerm(termId);
		}

		String lines = trimToNull(cdpService().readString(parameters.get("lines")));
		Boolean createInstructors = cdpService().readBoolean(parameters.get("createInstructors"));
		Boolean addToUG = cdpService().readBoolean(parameters.get("addToUG"));
		Boolean createSitesWithoutRosters = cdpService().readBoolean(parameters.get("createSitesOnly"));

		rosterService().processRosterText(lines, client, term, createInstructors, addToUG, createSitesWithoutRosters);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemoveMasterMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRemoveMasterMembers: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchRemoveMasterMembers: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String ids = (String) parameters.get("ids");
		if (ids == null)
		{
			M_log.warn("dispatchRemoveMasterMembers: missing ids");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] idStrs = split(ids, "\t");

		// for detailed results
		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		// any removes will be from the master roster
		Roster master = rosterService().getMasterRoster(site);
		if (master == null)
		{
			M_log.warn("dispatchRemoveMasterMembers: missing site master roster: " + site.getId());

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// for checking current membership
		Membership aggregate = rosterService().getAggregateSiteRoster(site);

		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);

			User user = userService().get(id);
			if (user != null)
			{
				Map<String, Object> resultMap = new HashMap<String, Object>();
				resultsList.add(resultMap);
				resultMap.put("name", user.getNameDisplay());

				// find the user in the site's aggregate - to see if the user's in there at all
				if (aggregate.findUser(user) == null)
				{
					resultMap.put("status", "notInSite");
				}
				else
				{
					// is the user in the master roster?
					Member m = master.getMembership().findUser(user);
					if (m != null)
					{
						master.getMembership().remove(user);
						resultMap.put("status", "removed");
					}

					// the user is not part of the master membership
					else
					{
						resultMap.put("status", "notRemoved");
					}
				}
			}
			else
			{
				M_log.warn("dispatchRemoveMasterMembers: user not found: " + id);
			}
		}

		// save
		rosterService().updateRoster(master);

		doSiteRoster(rv, site, true);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemoveMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRemoveMembers: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchRemoveMembers: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String ids = (String) parameters.get("users");
		if (ids == null)
		{
			M_log.warn("dispatchRemoveMembers: missing users");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] idStrs = split(ids, "\t");

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// for detailed results
		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		// any removes will be from the adhoc roster
		Roster adhoc = rosterService().getAdhocRoster(site);
		if (adhoc == null)
		{
			M_log.warn("dispatchRemoveMembers: missing site adhoc roster: " + site.getId());

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// for checking current membership
		Membership aggregate = rosterService().getAggregateSiteRoster(site);

		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);

			User user = userService().get(id);
			if (user != null)
			{
				Map<String, Object> resultMap = new HashMap<String, Object>();
				resultsList.add(resultMap);
				resultMap.put("name", user.getNameDisplay());

				// cannot remove one's self
				if (user.equals(authenticatedUser))
				{
					resultMap.put("status", "notRemoved");
				}

				// find the user in the site's aggregate - to see if the user's in there at all
				else if (aggregate.findUser(user) == null)
				{
					resultMap.put("status", "notInSite");
				}
				else
				{
					// is the user in the adhoc roster?
					Member m = adhoc.getMembership().findUser(user);
					if (m != null)
					{
						adhoc.getMembership().remove(user);
						resultMap.put("status", "removed");
					}

					// the user is not part of the adhoc membership, try a block
					else
					{
						if (!rosterService().checkBlocked(site, user))
						{
							rosterService().block(site, user);
							resultMap.put("status", "blocked");
						}
						else
						{
							resultMap.put("status", "alreadyBlocked");
						}
					}
				}
			}
			else
			{
				M_log.warn("dispatchRemoveMembers: user not found: " + id);
			}
		}

		// save
		rosterService().updateRoster(adhoc);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemoveRosterSites(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long clientId = cdpService().readLong(parameters.get("client"));
		if (clientId == null)
		{
			M_log.warn("dispatchRemoveRosterSites: missing client");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Client client = rosterService().getClient(clientId);
		if (client == null)
		{
			M_log.warn("dispatchRemoveRosterSites: missing client: " + clientId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Long termId = cdpService().readLong(parameters.get("term"));
		if (termId == null)
		{
			M_log.warn("dispatchRemoveRosterSites: missing term");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Term term = rosterService().getTerm(termId);
		if (term == null)
		{
			M_log.warn("dispatchRemoveRosterSites: missing term: " + termId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		String ids = (String) parameters.get("ids");
		String[] idStrs = split(ids, "\t");
		for (String rosterName : idStrs)
		{
			// find the roster
			Roster roster = rosterService().getRoster(rosterName, client, term);

			if (roster != null)
			{
				rosterService().removeMappings(roster);

				// Note: the roster remains: removeRoster(roster);

				Map<String, Object> resultsMap = new HashMap<String, Object>();
				resultsList.add(resultsMap);

				resultsMap.put("roster", roster.getName());
			}
		}

		// return the rosters response
		doRosters(rv, client, term);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemoveSiteRosterMapping(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long clientId = cdpService().readLong(parameters.get("client"));
		if (clientId == null)
		{
			M_log.warn("dispatchRemoveSiteRosterMapping: missing client");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Client client = rosterService().getClient(clientId);
		if (client == null)
		{
			M_log.warn("dispatchRemoveSiteRosterMapping: missing client: " + clientId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Long termId = cdpService().readLong(parameters.get("term"));
		if (termId == null)
		{
			M_log.warn("dispatchRemoveSiteRosterMapping: missing term");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Term term = rosterService().getTerm(termId);
		if (term == null)
		{
			M_log.warn("dispatchRemoveSiteRosterMapping: missing term: " + termId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		String ids = (String) parameters.get("ids");
		String[] idStrs = split(ids, "\t");
		for (String idStr : idStrs)
		{
			try
			{
				// siteId @ rosterName
				String[] params = splitFirst(idStr, "@");
				Long siteId = Long.parseLong(params[0]);
				String rosterName = params[1];

				// find the roster
				Roster roster = rosterService().getRoster(rosterName, client, term);

				// find the site
				Site site = siteService().get(siteId);

				if ((roster != null) && (site != null))
				{
					// check the mapping
					if (rosterService().checkMapping(site, roster))
					{
						rosterService().removeMapping(site, roster);

						Map<String, Object> resultsMap = new HashMap<String, Object>();
						resultsList.add(resultsMap);

						resultsMap.put("site", site.getName());
						resultsMap.put("roster", roster.getName());
					}
				}
			}
			catch (NumberFormatException e)
			{
				M_log.warn("dispatchRemoveSiteRosterMapping: invalid id entry: " + idStr);
			}
		}

		// return the rosters response
		doRosters(rv, client, term);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemoveSiteRosters(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long clientId = cdpService().readLong(parameters.get("client"));
		if (clientId == null)
		{
			M_log.warn("dispatchRemoveSiteRosters: missing client");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Client client = rosterService().getClient(clientId);
		if (client == null)
		{
			M_log.warn("dispatchRemoveSiteRosters: missing client: " + clientId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Long termId = cdpService().readLong(parameters.get("term"));
		if (termId == null)
		{
			M_log.warn("dispatchRemoveSiteRosters: missing term");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Term term = rosterService().getTerm(termId);
		if (term == null)
		{
			M_log.warn("dispatchRemoveSiteRosters: missing term: " + termId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		String ids = (String) parameters.get("ids");
		String[] idStrs = split(ids, "\t");
		for (String siteIdStr : idStrs)
		{
			try
			{
				// siteId
				Long siteId = Long.parseLong(siteIdStr);

				// find the site
				Site site = siteService().get(siteId);

				if (site != null)
				{
					rosterService().removeMappings(site);

					Map<String, Object> resultsMap = new HashMap<String, Object>();
					resultsList.add(resultsMap);

					resultsMap.put("site", site.getName());
				}
			}
			catch (NumberFormatException e)
			{
				M_log.warn("dispatchRemoveSiteRosters: invalid id entry: " + siteIdStr);
			}
		}

		// return the rosters response
		doRosters(rv, client, term);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRoleMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRoleMembers: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchRoleMembers: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String ids = (String) parameters.get("users");
		if (ids == null)
		{
			M_log.warn("dispatchRoleMembers: missing users");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] idStrs = split(ids, "\t");

		Integer roleLevel = cdpService().readInt(parameters.get("role"));
		if (roleLevel == null)
		{
			M_log.warn("dispatchRoleMembers: missing role");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Role role = Role.valueOf(roleLevel);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// any adds will be to the adhoc roster
		Roster adhoc = rosterService().getAdhocRoster(site);
		if (adhoc == null)
		{
			M_log.warn("dispatchAddMembers: missing site adhoc roster: " + site.getId());

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// for detailed results
		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		// for checking current membership
		Membership aggregate = rosterService().getAggregateSiteRoster(site);

		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			User user = userService().get(id);
			if (user == null)
			{
				continue;
			}

			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultsList.add(resultMap);
			resultMap.put("name", user.getNameDisplay());

			// must exist in the site
			if (aggregate.findUser(user) == null)
			{
				resultMap.put("status", "notInSite");
				continue;
			}

			Member m = adhoc.getMembership().findUser(user);
			if (m == null)
			{
				resultMap.put("status", "notAdhoc");
				continue;
			}

			resultMap.put("status", "roleChanged");
			adhoc.getMembership().update(user, role);
		}

		// save
		rosterService().updateRoster(adhoc);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRosters(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long clientId = cdpService().readLong(parameters.get("client"));
		if (clientId == null)
		{
			M_log.warn("dispatchRosters: missing client");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Client client = rosterService().getClient(clientId);
		if (client == null)
		{
			M_log.warn("dispatchRosters: missing client: " + clientId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Long termId = cdpService().readLong(parameters.get("term"));
		if (termId == null)
		{
			M_log.warn("dispatchRosters: missing term");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Term term = rosterService().getTerm(termId);
		if (term == null)
		{
			M_log.warn("dispatchRosters: missing term: " + termId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		doRosters(rv, client, term);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSchedule(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		doSchedule(authenticatedUser, rv);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSiteRoster(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin-only function
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		String title = readSiteNameParam(parameters, "title");
		if (title == null)
		{
			M_log.warn("dispatchSiteRoster: missing title");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// find the site
		Site site = siteService().get(title);
		if (site != null)
		{
			doSiteRoster(rv, site, true);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUnblockMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchUnblockMembers: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchUnblockMembers: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String ids = (String) parameters.get("users");
		if (ids == null)
		{
			M_log.warn("dispatchUnblockMembers: missing users");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] idStrs = split(ids, "\t");

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// for detailed results
		List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
		rv.put("results", resultsList);

		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			User user = userService().get(id);
			if (user != null)
			{
				Map<String, Object> resultMap = new HashMap<String, Object>();
				resultsList.add(resultMap);
				resultMap.put("name", user.getNameDisplay());

				if (rosterService().checkBlocked(site, user))
				{
					rosterService().unblock(site, user);
					resultMap.put("status", "unblocked");
				}
				else
				{
					resultMap.put("status", "alreadyUnblocked");
				}
			}
			else
			{
				M_log.warn("dispatchUnblockMembers: user not fond: " + id);
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected void doRosters(Map<String, Object> rv, Client client, Term term)
	{
		// the sites
		List<Site> clientTermSites = siteService().find(client, term);
		List<Map<String, Object>> clientTermSitesList = new ArrayList<Map<String, Object>>();
		rv.put("sites", clientTermSitesList);
		for (Site site : clientTermSites)
		{
			Map<String, Object> siteMap = new HashMap<String, Object>();
			clientTermSitesList.add(siteMap);

			siteMap.put("name", site.getName());
			siteMap.put("id", site.getId());
			siteMap.put("term", site.getTerm().getName());
			siteMap.put("client", site.getClient().getName());
			siteMap.put("published", site.isPublished());
			siteMap.put("accessStatus", site.getAccessStatus().getId());
			if (site.getPublishOn() != null) siteMap.put("publishOn", cdpService().sendDate(site.getPublishOn()));
			if (site.getUnpublishOn() != null) siteMap.put("unpublishOn", cdpService().sendDate(site.getUnpublishOn()));
			siteMap.put("createdOn", cdpService().sendDate(site.getCreatedOn()));

			Map<String, Object> skinMap = new HashMap<String, Object>();
			siteMap.put("skin", skinMap);
			skinMap.put("id", site.getSkin().getId());
			skinMap.put("name", site.getSkin().getName());
			skinMap.put("color", site.getSkin().getColor());
			skinMap.put("client", site.getSkin().getClient());

			Map<String, Object> clientMap = new HashMap<String, Object>();
			siteMap.put("client", clientMap);
			clientMap.put("id", site.getClient().getId());
			clientMap.put("name", site.getClient().getName());

			// the site's rosters
			List<Roster> rosters = rosterService().rostersForSite(site);
			List<Map<String, Object>> rosterList = new ArrayList<Map<String, Object>>();
			siteMap.put("rosters", rosterList);
			for (Roster roster : rosters)
			{
				// skip adhoc and master
				if (roster.isAdhoc() || roster.isMaster()) continue;

				Map<String, Object> rosterMap = new HashMap<String, Object>();
				rosterList.add(rosterMap);
				rosterMap.put("id", roster.getId());
			}
		}

		// the rosters
		List<Roster> clientTermRosters = rosterService().getRosters(client, term);
		List<Map<String, Object>> clientTermRosterList = new ArrayList<Map<String, Object>>();
		rv.put("rosters", clientTermRosterList);
		for (Roster roster : clientTermRosters)
		{
			// skip adhoc and master
			if (roster.isAdhoc() || roster.isMaster()) continue;

			// the roster
			Map<String, Object> rosterMap = new HashMap<String, Object>();
			clientTermRosterList.add(rosterMap);

			rosterMap.put("name", roster.getName());
			rosterMap.put("id", roster.getId());
			rosterMap.put("client", roster.getClient().getName());
			rosterMap.put("term", roster.getTerm().getName());
			rosterMap.put("adhoc", roster.isAdhoc());
			rosterMap.put("master", roster.isMaster());
			rosterMap.put("official", roster.isOfficial());

			List<Site> sites = rosterService().sitesForRoster(roster);
			List<Map<String, Object>> siteList = new ArrayList<Map<String, Object>>();
			rosterMap.put("sites", siteList);
			for (Site site : sites)
			{
				Map<String, Object> siteMap = new HashMap<String, Object>();
				siteList.add(siteMap);
				siteMap.put("id", site.getId());
			}
		}
	}

	protected void doSchedule(User authenticatedUser, Map<String, Object> rv)
	{
		// the schedule items
		List<Map<String, Object>> scheduleMap = new ArrayList<Map<String, Object>>();
		rv.put("schedule", scheduleMap);

		List<RunTime> schedule = rosterService().getRosterFileProcessingSchedule();
		for (RunTime t : schedule)
		{
			Map<String, Object> scheduleEntryMap = new HashMap<String, Object>();
			scheduleMap.add(scheduleEntryMap);
			scheduleEntryMap.put("time", t.toString());
		}

		// the files
		List<Map<String, Object>> filesMap = new ArrayList<Map<String, Object>>();
		rv.put("files", filesMap);

		List<File> files = rosterService().getRosterFiles();
		for (File file : files)
		{
			Map<String, Object> fileEntryMap = new HashMap<String, Object>();
			filesMap.add(fileEntryMap);
			fileEntryMap.put("name", file.getName());
		}
	}

	protected void doSiteRoster(Map<String, Object> rv, Site site, boolean includeRosters)
	{
		Map<String, Object> siteMap = site.sendForPortal(Role.admin);
		rv.put("site", siteMap);

		// site info
		// siteMap.put("title", site.getName());
		// siteMap.put("id", site.getId());
		siteMap.put("term", site.getTerm().getName());
		siteMap.put("client", site.getClient().getName());
		// siteMap.put("published", site.isPublished());
		// siteMap.put("accessStatus", site.getAccessStatus().getId());
		// if (site.getPublishOn() != null) siteMap.put("publishOn", cdpService().sendDate(site.getPublishOn()));
		// if (site.getUnpublishOn() != null) siteMap.put("unpublishOn", cdpService().sendDate(site.getUnpublishOn()));
		//
		// Map<String, Object> clientMap = new HashMap<String, Object>();
		// siteMap.put("client", clientMap);
		// clientMap.put("id", site.getClient().getId());
		// clientMap.put("name", site.getClient().getName());

		// the site combined roster in "members" - array of membership info
		List<Map<String, Object>> members = new ArrayList<Map<String, Object>>();
		siteMap.put("members", members);

		Membership siteRoster = rosterService().getAggregateSiteRoster(site);
		for (Member m : siteRoster.getMembers())
		{
			members.add(m.send(null));
		}

		if (includeRosters)
		{
			// each roster the site uses
			List<Map<String, Object>> rosterList = new ArrayList<Map<String, Object>>();
			siteMap.put("rosters", rosterList);

			List<Roster> rosters = rosterService().rostersForSite(site);
			for (Roster roster : rosters)
			{
				// record these roster names, but don't include the rosters
				if (roster.isAdhoc())
				{
					continue;
				}
				else if (roster.isMaster())
				{
					continue;
				}

				Map<String, Object> rosterMap = new HashMap<String, Object>();
				rosterList.add(rosterMap);

				rosterMap.put("title", roster.getName());
				rosterMap.put("id", roster.getId());
				rosterMap.put("client", roster.getClient().getName());
				rosterMap.put("term", roster.getTerm().getName());
				rosterMap.put("adhoc", roster.isAdhoc());
				rosterMap.put("master", roster.isMaster());
				rosterMap.put("official", roster.isOfficial());

				// and the roster membership
				members = new ArrayList<Map<String, Object>>();
				rosterMap.put("members", members);

				Membership rosterMembers = roster.getMembership();
				for (Member m : rosterMembers.getMembers())
				{
					members.add(m.send(null));
				}
			}
		}
	}

	/**
	 * Read a site name from the UI, collapsing any multiple internal spaces.
	 * 
	 * @param parameters
	 *        The request parameters.
	 * @return The site name, or null if not found.
	 */
	protected String readSiteNameParam(Map<String, Object> parameters, String paramName)
	{
		String siteName = trimToNull((String) parameters.get(paramName));
		if (siteName != null)
		{
			siteName = siteName.replaceAll("\\s+", " ").toUpperCase();
		}

		return siteName;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
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
