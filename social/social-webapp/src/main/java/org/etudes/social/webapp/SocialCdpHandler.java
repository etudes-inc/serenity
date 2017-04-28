/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/social/social-webapp/src/main/java/org/etudes/social/webapp/SocialCdpHandler.java $
 * $Id: SocialCdpHandler.java 11104 2015-06-14 02:28:32Z ggolden $
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

package org.etudes.social.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.file.api.FileService;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.social.api.SocialService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 */
public class SocialCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(SocialCdpHandler.class);

	public String getPrefix()
	{
		return "social";
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

		else if (requestPath.equals("roster"))
		{
			return dispatchRoster(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchRoster(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRoster: missing site");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchRoster: missing site: " + siteId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: authenticatedUser must have a role of guest "or higher" in the site
		// TODO: allow system admin, helpdesk, too?
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// the site combined roster in "members" - array of user info enhance with membership info
		List<Map<String, Object>> roster = new ArrayList<Map<String, Object>>();
		rv.put("roster", roster);

		Membership siteRoster = rosterService().getAggregateSiteRoster(site);
		for (Member m : siteRoster.getMembers())
		{
			Map<String, Object> userMap = m.getUser().send();

			// enhance with membership info TODO: work into Membership.send
			userMap.put("role", m.getRole().getLevel());
			userMap.put("active", m.isActive());
			userMap.put("blocked", (m.isBlocked() == null) ? Boolean.FALSE : m.isBlocked());
			userMap.put("rosterName", m.getRoster().getName());
			userMap.put("rosterId", m.getRoster().getId());
			userMap.put("official", m.getRoster().isOfficial());
			userMap.put("adhoc", m.getRoster().isAdhoc());
			userMap.put("master", m.getRoster().isMaster());
			userMap.put("clientIid", m.getUser().getIidDisplay(site.getClient()));

			roster.add(userMap);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
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
	 * @return The registered SocialService.
	 */
	private SocialService socialService()
	{
		return (SocialService) Services.get(SocialService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
