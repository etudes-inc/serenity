/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sitecontent/webapp/PurgeServiceImpl.java $
 * $Id: PurgeServiceImpl.java 10509 2015-04-17 21:50:49Z ggolden $
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

package org.etudes.sitecontent.webapp;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.authentication.api.AuthenticationService;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.SiteMember;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.PurgeService;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.StudentContentHandler;
import org.etudes.tool.api.Tool;
import org.etudes.tracking.api.TrackingService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * PurgeServiceImpl implements PurgeService.
 */
public class PurgeServiceImpl implements PurgeService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PurgeServiceImpl.class);

	/**
	 * Construct
	 */
	public PurgeServiceImpl()
	{
		M_log.info("PurgeServiceImpl: construct");
	}

	@Override
	public void clear(Site site)
	{
		Map<Tool, Service> handlers = Services.getHandlers(StudentContentHandler.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				if (s.getValue() instanceof StudentContentHandler)
				{
					((StudentContentHandler) s.getValue()).clear(site);
				}
			}
		}

		// remove all non-instructors from site rosters
		rosterService().clear(site);
	}

	@Override
	public void purge(Site site)
	{
		// purge site content for all tools that handle site content
		Map<Tool, Service> handlers = Services.getHandlers(SiteContentHandler.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				if (s.getValue() instanceof SiteContentHandler)
				{
					((SiteContentHandler) s.getValue()).purge(site);
				}
			}
		}

		// remove all site rosters
		rosterService().purge(site);

		// remove all tracking
		trackingService().clear(site);

		// remove the site
		siteService().remove(site);
	}

	@Override
	public void purge(User user)
	{
		// skip purge of protected users
		if (userService().isProtected(user)) return;

		// remove user content from user's sites
		List<SiteMember> sites = rosterService().sitesForUser(user);
		for (SiteMember m : sites)
		{
			Map<Tool, Service> handlers = Services.getHandlers(StudentContentHandler.class);
			if (handlers != null)
			{
				Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
				for (Entry<Tool, Service> s : handlerSet)
				{
					if (s.getValue() instanceof StudentContentHandler)
					{
						((StudentContentHandler) s.getValue()).clear(m.getSite(), user);
					}
				}
			}
		}

		// remove user from all rosters
		rosterService().remove(user);

		// remove all tracking
		trackingService().clear(user);

		// remove all authentication history
		authenticationService().clear(user);

		// remove user
		userService().remove(user);
	}

	@Override
	public boolean start()
	{
		M_log.info("PurgeServiceImpl: start");
		return true;
	}

	/**
	 * @return The registered AuthenticationService.
	 */
	private AuthenticationService authenticationService()
	{
		return (AuthenticationService) Services.get(AuthenticationService.class);
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

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
