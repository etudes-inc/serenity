/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-webapp/src/main/java/org/etudes/home/webapp/HomeCdpHandler.java $
 * $Id: HomeCdpHandler.java 11778 2015-10-06 21:24:54Z ggolden $
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

package org.etudes.home.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.announcement.api.Announcement;
import org.etudes.announcement.api.AnnouncementService;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.home.api.HomeItem;
import org.etudes.home.api.HomeOptions;
import org.etudes.home.api.HomeService;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemType;
import org.etudes.user.api.User;

/**
 */
public class HomeCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(HomeCdpHandler.class);

	public String getPrefix()
	{
		return "home";
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

		else if (requestPath.equals("activity"))
		{
			return dispatchActivity(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("options"))
		{
			return dispatchOptions(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("current"))
		{
			return dispatchCurrent(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("announcements"))
		{
			return dispatchAnnouncements(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("events"))
		{
			return dispatchEvents(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("eventsDays"))
		{
			return dispatchEventsDays(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("get"))
		{
			return dispatchGet(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("remove"))
		{
			return dispatchRemove(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("publish"))
		{
			return dispatchPublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("unpublish"))
		{
			return dispatchUnpublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("save"))
		{
			return dispatchSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("saveOptions"))
		{
			return dispatchSaveOptions(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchActivity(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchActivity: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticated, site);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		rv.put("activity", site.sendActivity());

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAnnouncements(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchAnnouncements: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticated, site);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// get the top n announcements
		List<Announcement> announcements = announcementService().findTopInSites(site, Integer.valueOf(3)); // TODO: from options
		List<Map<String, Object>> announcementsList = new ArrayList<Map<String, Object>>();
		rv.put("announcements", announcementsList);
		for (Announcement announcement : announcements)
		{
			announcementsList.add(announcement.send());
		}

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchCurrent(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchActivity: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticated, site);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		HomeItem current = homeService().itemFindCurrent(site);
		if (current != null) rv.put("current", current.send(null));

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchEvents(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchEvents: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticated, site);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Date date = cdpService().readDate(parameters.get("date"));
		if (date == null)
		{
			// use today
			Calendar now = Calendar.getInstance();

			// set the user's timezone, to zero out the time
			now.setTimeZone(TimeZone.getTimeZone(authenticated.getTimeZoneDflt()));
			now.set(Calendar.HOUR_OF_DAY, 0);
			now.set(Calendar.MINUTE, 0);
			now.set(Calendar.SECOND, 0);
			now.set(Calendar.MILLISECOND, 0);

			date = now.getTime();
		}

		rv.put("eventsDate", cdpService().sendDate(date));

		List<Map<String, Object>> eventsList = new ArrayList<Map<String, Object>>();
		rv.put("events", eventsList);

		// event. siteId siteName toolName title
		Map<String, Object> eventMap = new HashMap<String, Object>();
		eventsList.add(eventMap);
		eventsList.add(eventMap);
		eventsList.add(eventMap);
		eventMap.put("id", "0");
		// eventMap.put("type", "Meeting");
		eventMap.put("itemType", ToolItemType.assignment.getId());
		eventMap.put("tool", Tool.assessment.getId());

		eventMap.put("title", "Assessment Opens");
		// eventMap.put("content", "A Meeting has been scheduled ...");
		eventMap.put("dateStart", new Date());
		// eventMap.put("dateEnd", new Date());

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchEventsDays(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchEvents: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Integer year = cdpService().readInt(parameters.get("year")); // i.e. 2015
		Integer month = cdpService().readInt(parameters.get("month")); // 1 .. 12

		if ((year == null) || (month == null))
		{
			// use today
			Calendar now = Calendar.getInstance();

			// set the user's timezone, to zero out the time
			now.setTimeZone(TimeZone.getTimeZone(authenticatedUser.getTimeZoneDflt()));
			now.set(Calendar.HOUR_OF_DAY, 0);
			now.set(Calendar.MINUTE, 0);
			now.set(Calendar.SECOND, 0);
			now.set(Calendar.MILLISECOND, 0);

			year = now.get(Calendar.YEAR);
			month = now.get(Calendar.MONTH) + 1;
		}

		// build up a map to return
		Map<String, Object> map = new HashMap<String, Object>();
		rv.put("eventsDays", map);

		map.put("year", year);
		map.put("month", month);

		List<Integer> days = new ArrayList<Integer>();
		map.put("days", days);

		days.add(Integer.valueOf(2));
		days.add(Integer.valueOf(22));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGet(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGet: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticated, site);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
		rv.put("items", itemList);

		List<HomeItem> items = homeService().itemFindInSite(site);
		for (HomeItem item : items)
		{
			itemList.add(item.send(authenticated));
		}

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchOptions(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchOptions: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticated, site);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		HomeOptions options = homeService().optionsGet(site);
		// rv.put("options", options.send());

		Map<String, Object> optionsMap = new HashMap<String, Object>();
		rv.put("options", optionsMap);

		optionsMap.put("format", Integer.valueOf(0));
		optionsMap.put("numAnnc", Integer.valueOf(3));
		optionsMap.put("fullAnnc", Boolean.TRUE);

		List<Map<String, Object>> componentsList = new ArrayList<Map<String, Object>>();
		optionsMap.put("components", componentsList);

		Map<String, Object> component = new HashMap<String, Object>();
		componentsList.add(component);
		component.put("id", Integer.valueOf(1));
		component.put("title", "Announcements");
		component.put("enabled", Boolean.TRUE);
		component.put("order", Integer.valueOf(1));

		component = new HashMap<String, Object>();
		componentsList.add(component);
		component.put("id", Integer.valueOf(2));
		component.put("title", "Calendar");
		component.put("enabled", Boolean.TRUE);
		component.put("order", Integer.valueOf(2));

		component = new HashMap<String, Object>();
		componentsList.add(component);
		component.put("id", Integer.valueOf(3));
		component.put("title", "Chat");
		component.put("enabled", Boolean.FALSE);
		component.put("order", Integer.valueOf(3));

		optionsMap.put("fs", Integer.valueOf(9)); // 0 - homepage, 1 - CHS/resources, 9 - serenity

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPublish(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchPublish: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// item ids
		List<Long> ids = cdpService().readIds(parameters.get("ids"));
		for (Long id : ids)
		{
			// get item
			HomeItem item = homeService().itemGet(id);
			if (item != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (item.getSite().equals(site))
				{
					// publish
					item.setPublished(Boolean.TRUE);
					homeService().itemSave(authenticatedUser, item);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemove(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRemove: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// item ids
		List<Long> ids = cdpService().readIds(parameters.get("ids"));
		for (Long id : ids)
		{
			// get item
			HomeItem item = homeService().itemGet(id);
			if (item != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (item.getSite().equals(site))
				{
					// remove
					homeService().itemRemove(item);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSave: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// id < 0 for a new item
		Long id = cdpService().readLong(parameters.get("item"));
		if (id == null)
		{
			M_log.warn("dispatchSave: missing item (id)");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		HomeItem item = null;
		if (id < 0)
		{
			// make a new item
			item = homeService().itemAdd(authenticatedUser, site);
		}
		else
		{
			// find the item, assure it is in the site
			item = homeService().itemGet(id);
			if ((item != null) && (!item.getSite().equals(site)))
			{
				M_log.warn("dispatchSave: item from different site");
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		if (item == null)
		{
			M_log.warn("dispatchSave: null item");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		item.read("", parameters);
		homeService().itemSave(authenticatedUser, item);

		// report the id for the item just saved
		rv.put("savedId", item.getId());

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		return rv;
	}

	protected Map<String, Object> dispatchSaveOptions(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSave: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// TODO: unimplemented
		// HomeOptions options = homeService().optionsGet(site);
		// options.read("", parameters);
		// homeService().optionsSave(authenticatedUser, options);

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		return rv;
	}

	protected Map<String, Object> dispatchUnpublish(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchUnpublish: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// item ids
		List<Long> ids = cdpService().readIds(parameters.get("ids"));
		for (Long id : ids)
		{
			// get item
			HomeItem item = homeService().itemGet(id);
			if (item != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (item.getSite().equals(site))
				{
					// publish
					item.setPublished(Boolean.FALSE);
					homeService().itemSave(authenticatedUser, item);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	/**
	 * @return The registered AnnouncementService.
	 */
	private AnnouncementService announcementService()
	{
		return (AnnouncementService) Services.get(AnnouncementService.class);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered HomeService.
	 */
	private HomeService homeService()
	{
		return (HomeService) Services.get(HomeService.class);
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
}
