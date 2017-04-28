/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/portal/portal-webapp/src/main/java/org/etudes/portal/webapp/PortalCdpHandler.java $
 * $Id: PortalCdpHandler.java 12060 2015-11-12 03:58:14Z ggolden $
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

package org.etudes.portal.webapp;

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
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.SiteMember;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.tool.api.Tool;
import org.etudes.tracking.api.TrackingService;
import org.etudes.user.api.User;

/**
 */
public class PortalCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(PortalCdpHandler.class);

	public String getPrefix()
	{
		return "portal";
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

		else if (requestPath.equals("info"))
		{
			return dispatchInfo(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("presence"))
		{
			return dispatchPresence(req, res, parameters, path, authenticatedUser);
		}
		else
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
	}

	protected Map<String, Object> dispatchInfo(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// authenticated user info
		rv.put("user", authenticatedUser.send());

		// authenticated user's sites - all sites, published, open, will open or closed, in user order - only those the user has ordered (not the "hidden" ones) - in order // TODO:
		List<Map<String, Object>> sitesMap = new ArrayList<Map<String, Object>>();
		rv.put("sites", sitesMap);

		List<SiteMember> sites = rosterService().sitesForUser(authenticatedUser);

		// an extra site to make sure is in the list - admin only
		Site extraSite = null;
		if (authenticatedUser.isAdmin())
		{
			Long extraSiteId = cdpService().readLong(parameters.get("extraSite"));
			if (extraSiteId != null)
			{
				boolean found = false;
				for (SiteMember s : sites)
				{
					if (s.getSite().getId().equals(extraSiteId))
					{
						found = true;
						break;
					}
				}

				if (!found)
				{
					extraSite = siteService().get(extraSiteId);
				}
			}
		}

		for (SiteMember member : sites)
		{
			// get the reduced "for portal" version, no activity data
			sitesMap.add(member.getSite().sendForPortal(member.getRole()));
		}
		if (extraSite != null)
		{
			sitesMap.add(extraSite.sendForPortal(Role.admin));
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPresence(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long id = cdpService().readLong(parameters.get("site"));
		if (id == null)
		{
			M_log.warn("dispatchPresence: missing site parameter");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// the site
		Site site = siteService().wrap(id);

		// record site presence for this user in this site
		trackingService().registerPresence(authenticatedUser, site, Tool.portal, 0L);

		// if set and true, track a user visit to the site
		Boolean track = cdpService().readBoolean(parameters.get("track"));
		if ((track != null) && (track))
		{
			trackingService().track(authenticatedUser, site);
		}

		List<Map<String, Object>> presenceMap = new ArrayList<Map<String, Object>>();
		rv.put("presence", presenceMap);

		// get the presence for the site
		List<User> presence = trackingService().getPresence(site, Tool.portal, 0L);

		for (User user : presence)
		{
			Map<String, Object> userMap = new HashMap<String, Object>();
			presenceMap.add(userMap);

			userMap.put("id", user.getId().toString());
			userMap.put("nameDisplay", user.getNameDisplay());
			// TODO: see cdpServlet.respondUserInfo for full map
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
	 * @return The registered TrackingService.
	 */
	private TrackingService trackingService()
	{
		return (TrackingService) Services.get(TrackingService.class);
	}
}
