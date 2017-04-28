/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/announcement/announcement-webapp/src/main/java/org/etudes/announcement/webapp/AnnouncementCdpHandler.java $
 * $Id: AnnouncementCdpHandler.java 11887 2015-10-21 02:49:14Z ggolden $
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

package org.etudes.announcement.webapp;

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
import org.etudes.announcement.api.Announcement;
import org.etudes.announcement.api.AnnouncementService;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.user.api.User;

/**
 */
public class AnnouncementCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(AnnouncementCdpHandler.class);

	public String getPrefix()
	{
		return "announcement";
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

		else if (requestPath.equals("get"))
		{
			return dispatchGet(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("save"))
		{
			return dispatchSave(req, res, parameters, path, authenticatedUser);
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

		else if (requestPath.equals("order"))
		{
			return dispatchOrder(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchGet(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
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

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of guest "or higher" in the site TODO: public access to announcements?
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// get the site's announcements
		List<Announcement> announcements = announcementService().findBySite(site);
		// rv.put("mayEdit", userRole.ge(Role.instructor));

		List<Map<String, Object>> announcementsList = new ArrayList<Map<String, Object>>();
		rv.put("announcements", announcementsList);

		for (Announcement announcement : announcements)
		{
			announcementsList.add(announcement.send());
		}

		rv.put("fs", Integer.valueOf(9)); // 0 - homepage, 1 - CHS/resources, 9 - serenity

		// add status parameter
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

		// announcement ids
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Announcement announcement = announcementService().get(aid);
			if (announcement != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (announcement.getSite().equals(site))
				{
					announcement.setPublished(Boolean.TRUE);
					announcementService().save(authenticatedUser, announcement);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchOrder(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchOrder: missing site");
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

		// order
		List<Long> orderIds = cdpService().readIds(parameters.get("order"));
		int order = 1;
		for (Long aid : orderIds)
		{
			Announcement a = announcementService().get(aid);
			if (a != null)
			{
				a.setOrder(order++);
				announcementService().save(authenticatedUser, a);
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

		// announcement ids
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Announcement announcement = announcementService().get(aid);
			if (announcement != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (announcement.getSite().equals(site))
				{
					announcementService().remove(announcement);
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

		// id < 0 for a new announcement
		Long id = cdpService().readLong(parameters.get("announcement"));
		if (id == null)
		{
			M_log.warn("dispatchSave: missing announcement (id)");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Announcement announcement = null;
		if (id < 0)
		{
			// make a new announcement
			announcement = announcementService().add(authenticatedUser, site);
		}
		else
		{
			// find the announcement, assure it is in the site
			announcement = announcementService().get(id);
			if ((announcement != null) && (!announcement.getSite().equals(site)))
			{
				M_log.warn("dispatchSave: announcement from different site");
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		if (announcement == null)
		{
			M_log.warn("dispatchSave: null announcement");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		announcement.read("", parameters);
		announcementService().save(authenticatedUser, announcement);

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

		// announcement ids
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Announcement announcement = announcementService().get(aid);
			if (announcement != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (announcement.getSite().equals(site))
				{
					announcement.setPublished(Boolean.FALSE);
					announcementService().save(authenticatedUser, announcement);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	// protected Map<String, Object> dispatchSaveAll(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
	// User authenticatedUser) throws ServletException, IOException
	// {
	// Map<String, Object> rv = new HashMap<String, Object>();
	//
	// // site
	// Long siteId = cdpService().readLong(parameters.get("site"));
	// if (siteId == null)
	// {
	// M_log.warn("dispatchSave: missing site");
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
	// return rv;
	// }
	// Site site = siteService().wrap(siteId);
	//
	// Role userRole = rosterService().userRoleInSite(authenticatedUser, site);
	//
	// // security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
	// if (!userRole.ge(Role.instructor))
	// {
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
	// return rv;
	// }
	//
	// List<Long> order = cdpService().readIds(parameters.get("order"));
	// List<Long> updated = cdpService().readIds(parameters.get("updated"));
	// List<Long> added = cdpService().readIds(parameters.get("added"));
	// List<Long> deleted = cdpService().readIds(parameters.get("deleted"));
	//
	// Map<Long, Long> addedIds = new HashMap<Long, Long>();
	//
	// // add
	// for (Long id : added)
	// {
	// Boolean published = cdpService().readBoolean(parameters.get("published" + id));
	// Date releaseDate = cdpService().readDate(parameters.get("releaseDate" + id));
	// String subject = cdpService().readString(parameters.get("subject" + id));
	// String content = cdpService().readString(parameters.get("content" + id));
	//
	// Announcement a = announcementService().add(authenticatedUser, site);
	// a.setPublished(published);
	// a.setReleaseDate(releaseDate);
	// a.setSubject(subject);
	// a.setContent(content, true);
	//
	// announcementService().save(authenticatedUser, a);
	//
	// // map the id
	// addedIds.put(id, a.getId());
	// }
	//
	// // delete
	// for (Long id : deleted)
	// {
	// Announcement a = announcementService().check(id);
	// if (a != null) announcementService().remove(a);
	// }
	//
	// // update
	// for (Long id : updated)
	// {
	// Boolean published = cdpService().readBoolean(parameters.get("published" + id));
	// Date releaseDate = cdpService().readDate(parameters.get("releaseDate" + id));
	// String subject = cdpService().readString(parameters.get("subject" + id));
	// String content = cdpService().readString(parameters.get("content" + id));
	//
	// Announcement a = announcementService().get(id);
	// if (a != null)
	// {
	// a.setPublished(published);
	// a.setReleaseDate(releaseDate);
	// a.setSubject(subject);
	// a.setContent(content, true);
	//
	// announcementService().save(authenticatedUser, a);
	// }
	// }
	//
	// // apply the order to each section (Note: the order id, if <0, represents an added section, and needs to be mapped to the section's new id
	// int pos = 1;
	// for (Long id : order)
	// {
	// if (id < 0)
	// {
	// id = addedIds.get(id);
	// }
	//
	// Announcement a = announcementService().get(id);
	// if (a != null)
	// {
	// a.setOrder(pos++);
	// announcementService().save(authenticatedUser, a);
	// }
	// }
	//
	// // get the site's announcements
	// List<Announcement> announcements = announcementService().findBySite(site);
	// respondGet(rv, userRole, announcements, site);
	//
	// // add status parameter
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
	//
	// return rv;
	// }

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
