/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/java/org/etudes/syllabus/webapp/SyllabusCdpHandler.java $
 * $Id: SyllabusCdpHandler.java 11896 2015-10-21 21:06:34Z ggolden $
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

package org.etudes.syllabus.webapp;

import java.io.IOException;
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
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.syllabus.api.Syllabus;
import org.etudes.syllabus.api.SyllabusAcceptance;
import org.etudes.syllabus.api.SyllabusSection;
import org.etudes.syllabus.api.SyllabusService;
import org.etudes.user.api.User;

/**
 */
public class SyllabusCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(SyllabusCdpHandler.class);

	public String getPrefix()
	{
		return "syllabus";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests TODO: public syllabus access
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

		else if (requestPath.equals("accept"))
		{
			return dispatchAccept(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("save"))
		{
			return dispatchSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("saveSource"))
		{
			return dispatchSaveSource(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("saveSection"))
		{
			return dispatchSaveSection(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("publish"))
		{
			return dispatchPublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("unpublish"))
		{
			return dispatchUnpublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("remove"))
		{
			return dispatchRemove(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("order"))
		{
			return dispatchOrder(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchAccept(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
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

		// security: authenticatedUser must have a role of student in the site
		if (!userRole.equals(Role.student))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Syllabus syllabus = syllabusService().findBySite(site);
		if (syllabus.getId() == null)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// accept the syllabus
		SyllabusAcceptance accepted = syllabusService().accept(syllabus, authenticatedUser);
		rv.put("accepted", accepted.getAcceptedOn());

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
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

		// security: authenticatedUser must have a role of guest "or higher" in the site TODO: public access to so marked syllabus
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Syllabus syllabus = syllabusService().findBySite(site);

		// for students, get their acceptance
		SyllabusAcceptance acceptance = null;
		if ((syllabus != null) && (userRole.equals(Role.student)))
		{
			acceptance = syllabusService().getAccepted(syllabus, authenticatedUser);
		}

		Map<String, Object> syllabusMap = syllabus.send();
		if (acceptance != null) syllabusMap.put("accepted", cdpService().sendDate(acceptance.getAcceptedOn()));
		rv.put("syllabus", syllabusMap);

		rv.put("fs", Integer.valueOf(9)); // 0 - homepage, 1 - CHS/resources, 9 - serenity

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

		// get the syllabus
		Syllabus syllabus = syllabusService().findBySite(site);
		if (syllabus.getId() == null)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// order
		List<Long> orderIds = cdpService().readIds(parameters.get("order"));
		int order = 1;
		for (Long sid : orderIds)
		{
			SyllabusSection section = syllabus.findSectionById(sid);
			if (section != null)
			{
				section.setOrder(order++);
			}
		}

		syllabusService().save(authenticatedUser, syllabus);

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

		// get the syllabus
		Syllabus syllabus = syllabusService().findBySite(site);
		if (syllabus.getId() == null)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// section ids
		List<Long> sids = cdpService().readIds(parameters.get("ids"));
		for (Long sid : sids)
		{
			SyllabusSection section = syllabus.findSectionById(sid);
			if (section != null)
			{
				section.setPublished(Boolean.TRUE);
			}
		}

		syllabusService().save(authenticatedUser, syllabus);

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

		// get the syllabus
		Syllabus syllabus = syllabusService().findBySite(site);
		if (syllabus.getId() == null)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// section ids
		List<Long> sids = cdpService().readIds(parameters.get("ids"));
		for (Long sid : sids)
		{
			SyllabusSection section = syllabus.findSectionById(sid);
			if (section != null)
			{
				syllabus.removeSection(section);
			}
		}

		syllabusService().save(authenticatedUser, syllabus);

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

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Syllabus syllabus = syllabusService().findBySite(site);

		// save external, but not source or sections
		syllabus.getExternal().read("external_", parameters);

		// save
		syllabusService().save(authenticatedUser, syllabus);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSaveSection(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSaveSection: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long sectionId = cdpService().readLong(parameters.get("section"));
		if (sectionId == null)
		{
			M_log.warn("dispatchSaveSection: missing section");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Syllabus syllabus = syllabusService().findBySite(site);
		// if (syllabus.getId() == null)
		// {
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
		// return rv;
		// }

		SyllabusSection section = null;
		if (sectionId == -1)
		{
			section = syllabusService().addSection(authenticatedUser, syllabus);
		}
		else
		{
			section = syllabus.findSectionById(sectionId);
		}

		section.read("", parameters);

		// save
		syllabusService().save(authenticatedUser, syllabus);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSaveSource(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSaveSource: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Syllabus syllabus = syllabusService().findBySite(site);

		// save source only
		syllabus.read("", parameters);

		// save
		syllabusService().save(authenticatedUser, syllabus);

		// add status parameter
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

		// get the syllabus
		Syllabus syllabus = syllabusService().findBySite(site);
		if (syllabus.getId() == null)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// section ids
		List<Long> sids = cdpService().readIds(parameters.get("ids"));
		for (Long sid : sids)
		{
			SyllabusSection section = syllabus.findSectionById(sid);
			if (section != null)
			{
				section.setPublished(Boolean.FALSE);
			}
		}

		syllabusService().save(authenticatedUser, syllabus);

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
	 * @return The registered SyllabusService.
	 */
	private SyllabusService syllabusService()
	{
		return (SyllabusService) Services.get(SyllabusService.class);
	}
}
