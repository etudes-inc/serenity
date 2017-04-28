/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/dashboard/dashboard-webapp/src/main/java/org/etudes/dashboard/webapp/DashboardCdpHandler.java $
 * $Id: DashboardCdpHandler.java 11762 2015-10-05 03:38:50Z ggolden $
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

package org.etudes.dashboard.webapp;

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
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.SiteMember;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemType;
import org.etudes.user.api.User;

/**
 */
public class DashboardCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(DashboardCdpHandler.class);

	public String getPrefix()
	{
		return "dash";
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

		else if (requestPath.equals("events"))
		{
			return dispatchEvents(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("eventsDays"))
		{
			return dispatchEventsDays(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("announcements"))
		{
			return dispatchAnnouncements(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected void addMOTD(List<Map<String, Object>> announcementsList)
	{
		// TODO:
		Map<String, Object> announcementMap = new HashMap<String, Object>();

		announcementMap.put("title", "Routine System Maintenance");
		announcementMap
				.put("content",
						"<p>Server maintenance occurs daily from 4:00 am - 4:15 am (Pacific). Please do not log on during this period, as you will be logged out and will lose what you are working on.</p>");

		announcementsList.add(announcementMap);
		announcementsList.add(announcementMap);
	}

	protected Map<String, Object> dispatchActivity(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the user's sites w/ activity
		respondUserSites(rv, authenticated);

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAnnouncements(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// one published announcement from each published site - the one ordered first, or most recent (?)
		respondSiteAnnouncements(rv, authenticated);

		// etudes system message(s)
		respondSystemAnnouncements(rv);

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchEvents(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Date date = cdpService().readDate(parameters.get("date"));
		if (date == null)
		{
			// use today
			Calendar now = Calendar.getInstance();

			// set the user's timezone, to zero out the time
			now.setTimeZone(TimeZone.getTimeZone(authenticatedUser.getTimeZoneDflt()));
			now.set(Calendar.HOUR_OF_DAY, 0);
			now.set(Calendar.MINUTE, 0);
			now.set(Calendar.SECOND, 0);
			now.set(Calendar.MILLISECOND, 0);

			date = now.getTime();
		}

		rv.put("eventsDate", cdpService().sendDate(date));

		List<Map<String, Object>> eventsList = new ArrayList<Map<String, Object>>();
		rv.put("events", eventsList);

		Site s = siteService().get(Long.valueOf(6));
		if (s != null)
		{
			Role userRole = rosterService().userRoleInSite(authenticatedUser, s);

			// event. siteId siteName toolName title
			Map<String, Object> eventMap = new HashMap<String, Object>();
			eventsList.add(eventMap);
			eventsList.add(eventMap);
			eventsList.add(eventMap);
			eventMap.put("id", "0");
			eventMap.put("site", s.sendIdTitleRole(userRole));
			// eventMap.put("type", "Meeting");
			eventMap.put("itemType", ToolItemType.assignment.getId());
			eventMap.put("tool", Tool.assessment.getId());

			eventMap.put("title", "Assessment Opens");
			// eventMap.put("content", "A Meeting has been scheduled ...");
			eventMap.put("dateStart", new Date());
			// eventMap.put("dateEnd", new Date());
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchEventsDays(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

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

	protected Map<String, Object> doAnnouncement(Announcement announcement)
	{
		Map<String, Object> announcementMap = announcement.send();
		if (announcement.getSite() != null) announcementMap.put("site", announcement.getSite().sendIdTitleRole(null));

		return announcementMap;
	}

	protected void respondSiteAnnouncements(Map<String, Object> rv, User user)
	{
		List<Map<String, Object>> announcementsList = new ArrayList<Map<String, Object>>();
		rv.put("announcements", announcementsList);

		List<Announcement> topAnnouncements = announcementService().findTopByUserSites(user);
		for (Announcement a : topAnnouncements)
		{
			announcementsList.add(doAnnouncement(a));
		}
	}

	protected void respondSystemAnnouncements(Map<String, Object> rv)
	{
		List<Map<String, Object>> announcementsList = new ArrayList<Map<String, Object>>();
		rv.put("news", announcementsList);

		// the MOTD
		addMOTD(announcementsList);
	}

	protected void respondUserSites(Map<String, Object> rv, User user)
	{
		List<Map<String, Object>> sitesList = new ArrayList<Map<String, Object>>();
		rv.put("sites", sitesList);

		// get the authenticated user's visible selected sites in order (max 12)
		List<SiteMember> sites = rosterService().sitesForUser(user); // TODO: need only id, name, role, published & dates, access status
		for (SiteMember member : sites)
		{
			sitesList.add(member.getSite().send(member.getRole(), Boolean.TRUE));
		}
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
