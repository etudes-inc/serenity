/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/site/site-webapp/src/main/java/org/etudes/site/webapp/SiteCdpHandler.java $
 * $Id: SiteCdpHandler.java 11750 2015-10-02 04:11:38Z ggolden $
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

package org.etudes.site.webapp;

import static org.etudes.util.StringUtil.split;
import static org.etudes.util.StringUtil.trimToNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.SiteMember;
import org.etudes.roster.api.Term;
import org.etudes.service.api.Services;
import org.etudes.site.api.Link;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.site.api.Skin;
import org.etudes.sitecontent.api.ArchiveService;
import org.etudes.sitecontent.api.ArchivedSite;
import org.etudes.sitecontent.api.BaseDateService;
import org.etudes.sitecontent.api.DateRange;
import org.etudes.sitecontent.api.PurgeService;
import org.etudes.sitecontent.api.SiteImportService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 */
public class SiteCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(SiteCdpHandler.class);

	public String getPrefix()
	{
		return "site";
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

		else if (requestPath.equals("config"))
		{
			return dispatchConfig(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("allSites"))
		{
			return dispatchAllSites(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("update"))
		{
			return dispatchUpdate(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("importSites"))
		{
			return dispatchImportSites(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("importTools"))
		{
			return dispatchImportTools(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("import"))
		{
			return dispatchImport(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("publish"))
		{
			return dispatchPublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("unpublish"))
		{
			return dispatchUnpublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("purge"))
		{
			return dispatchPurge(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("archive"))
		{
			return dispatchArchive(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("clear"))
		{
			return dispatchClear(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("sites"))
		{
			return dispatchSites(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchAllSites(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: admin only
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Client client = null;
		Term term = null;
		Boolean byTerm = cdpService().readBoolean(parameters.get("byTerm"));
		String search = cdpService().readString(parameters.get("search"));
		Integer pageNum = cdpService().readInt(parameters.get("pageNum"));
		Integer pageSize = cdpService().readInt(parameters.get("pageSize"));

		respondAllSites(rv, client, term, search, byTerm, pageNum, pageSize);

		// and the total count, disregarding paging
		Integer total = siteService().count(client, term, search);
		rv.put("total", total);

		// status
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchArchive(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String ids = (String) parameters.get("ids");
		if (ids == null)
		{
			M_log.warn("dispatchArchive: missing ids");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] idStrs = split(ids, "\t");

		List<Site> sitesToArchive = new ArrayList<Site>();
		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			Site site = siteService().get(id);

			// security: authenticatedUser must have a role of instructor "or higher" in the site
			if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
			sitesToArchive.add(site);
		}

		// archive the sites
		for (Site site : sitesToArchive)
		{
			archiveService().archive(site, Boolean.TRUE);
		}

		// return the sites
		Client client = null;
		Term term = null;
		Boolean byTerm = cdpService().readBoolean(parameters.get("byTerm"));
		String search = cdpService().readString(parameters.get("search"));
		Integer pageNum = cdpService().readInt(parameters.get("pageNum"));
		Integer pageSize = cdpService().readInt(parameters.get("pageSize"));

		respondAllSites(rv, client, term, search, byTerm, pageNum, pageSize);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchClear(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String ids = (String) parameters.get("ids");
		if (ids == null)
		{
			M_log.warn("dispatchClear: missing ids");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] idStrs = split(ids, "\t");

		List<Site> sitesToArchive = new ArrayList<Site>();
		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			Site site = siteService().get(id);

			// security: authenticatedUser must have a role of instructor "or higher" in the site
			if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
			sitesToArchive.add(site);
		}

		// clear the sites
		for (Site site : sitesToArchive)
		{
			purgeService().clear(site);
		}

		// return the sites
		Client client = null;
		Term term = null;
		Boolean byTerm = cdpService().readBoolean(parameters.get("byTerm"));
		String search = cdpService().readString(parameters.get("search"));
		Integer pageNum = cdpService().readInt(parameters.get("pageNum"));
		Integer pageSize = cdpService().readInt(parameters.get("pageSize"));

		respondAllSites(rv, client, term, search, byTerm, pageNum, pageSize);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchConfig(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchUpdate: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchConfig: missing site: " + siteId);
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

		// skins
		List<Skin> skins = siteService().getSkins();
		List<Map<String, Object>> skinsList = new ArrayList<Map<String, Object>>();
		rv.put("skins", skinsList);
		for (Skin skin : skins)
		{
			Map<String, Object> skinMap = new HashMap<String, Object>();
			skinsList.add(skinMap);
			skinMap.put("id", skin.getId());
			skinMap.put("name", skin.getName());
			skinMap.put("color", skin.getColor());
			skinMap.put("client", skin.getClient());
		}

		// base date info
		List<DateRange> ranges = baseDateService().getRanges(site);
		Date baseDate = baseDateService().computeBaseDate(ranges);

		Map<String, Object> baseDateMap = new HashMap<String, Object>();
		rv.put("baseDate", baseDateMap);

		if (baseDate != null) baseDateMap.put("baseDate", cdpService().sendDate(baseDate));

		List<Map<String, Object>> rangesList = new ArrayList<Map<String, Object>>();
		baseDateMap.put("ranges", rangesList);

		for (DateRange range : ranges)
		{
			Map<String, Object> rangeMap = new HashMap<String, Object>();
			rangesList.add(rangeMap);
			rangeMap.put("tool", range.getTool().getTitle());
			rangeMap.put("min", cdpService().sendDate(range.getMin()));
			rangeMap.put("max", cdpService().sendDate(range.getMax()));
			// TODO: out of range indicator
		}

		// status
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchImport(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchImport: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchImport: missing site: " + siteId);
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

		// the site being imported TODO: or archive
		Long importId = cdpService().readLong(parameters.get("import"));
		if (importId == null)
		{
			M_log.warn("dispatchImport: missing import");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// "S" for site or "A" for archive
		String type = cdpService().readString(parameters.get("type"));

		// tools to import
		String tools = cdpService().readString(parameters.get("tools"));
		Set<Tool> desiredTools = new HashSet<Tool>();
		if (tools != null)
		{
			String[] toolIdStrs = split(tools, ",");
			for (String idStr : toolIdStrs)
			{
				Integer toolId = Integer.valueOf(idStr);
				Tool tool = Tool.valueOf(toolId);
				desiredTools.add(tool);
			}
		}

		if ("S".equals(type))
		{
			Site importSite = siteService().get(importId);
			if (importSite == null)
			{
				M_log.warn("dispatchImport: missing import site: " + importId);
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}

			siteImportService().importFromSite(importSite, desiredTools, site, authenticatedUser);
		}
		else
		{
			// get the archive
			ArchivedSite archive = archiveService().getArchive(importId);
			if (archive == null)
			{
				M_log.warn("dispatchImport: missing import archive: " + importId);
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}

			archiveService().importFromArchive(archive, desiredTools, site, Boolean.TRUE, authenticatedUser);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchImportSites(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchImportSites: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchImportSites: missing site: " + siteId);
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

		List<Map<String, Object>> sitesMap = new ArrayList<Map<String, Object>>();
		rv.put("sites", sitesMap);

		// get the authenticated user's sites, ordered by term, then name
		List<SiteMember> sites = rosterService().sitesForUser(authenticatedUser);
		for (SiteMember member : sites)
		{
			// filter out this site
			if (member.getSite().equals(site)) continue;

			// filter out those the instructor does not have instructor or higher role in
			if (!member.getRole().ge(Role.instructor)) continue;

			Map<String, Object> siteMap = new HashMap<String, Object>();
			sitesMap.add(siteMap);
			siteMap.put("name", member.getSite().getName());
			siteMap.put("id", member.getSite().getId());
			siteMap.put("type", "S");
			siteMap.put("term", member.getSite().getTerm().getName());
		}

		// add the authenticated user's archives
		List<Map<String, Object>> archivesMap = new ArrayList<Map<String, Object>>();
		rv.put("archives", archivesMap);

		List<ArchivedSite> archives = archiveService().archivesForUser(authenticatedUser);
		for (ArchivedSite archived : archives)
		{
			Map<String, Object> siteMap = new HashMap<String, Object>();
			archivesMap.add(siteMap);
			siteMap.put("name", archived.getName());
			siteMap.put("id", archived.getId());
			siteMap.put("type", "A");
			siteMap.put("term", archived.getTerm().getName());
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchImportTools(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchImportTools: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchImportTools: missing site: " + siteId);
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

		// "S" for site or "A" for archive
		String type = cdpService().readString(parameters.get("type"));

		// the site being imported TODO: or archive
		Long importId = cdpService().readLong(parameters.get("import"));
		if (importId == null)
		{
			M_log.warn("dispatchImportTools: missing import");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// find tools
		List<Tool> tools = null;

		if ("S".equals(type))
		{
			Site importSite = siteService().get(importId);
			if (importSite == null)
			{
				M_log.warn("dispatchImportTools: missing import: " + importSite);
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}

			// find importSite's tools
			tools = importSite.getOrderedTools();
		}
		else
		{
			// get the tool list from the archive with this id
			tools = archiveService().toolsForArchive(importId);
		}

		List<Map<String, Object>> toolsMap = new ArrayList<Map<String, Object>>();
		rv.put("tools", toolsMap);

		for (Tool tool : tools)
		{
			// filter out if it does not do import
			if (!siteImportService().imports(tool)) continue;

			Map<String, Object> toolMap = new HashMap<String, Object>();
			toolsMap.add(toolMap);
			toolMap.put("title", tool.getTitle());
			toolMap.put("id", tool.getId());
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPublish(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String ids = (String) parameters.get("ids");
		if (ids == null)
		{
			M_log.warn("dispatchPublish: missing ids");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] idStrs = split(ids, "\t");

		List<Site> sitesToPublish = new ArrayList<Site>();
		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			Site site = siteService().get(id);

			// security: authenticatedUser must have a role of instructor "or higher" in the site
			if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
			sitesToPublish.add(site);
		}

		// publish the sites
		for (Site site : sitesToPublish)
		{
			site.setPublished(Boolean.TRUE);
			siteService().save(authenticatedUser, site);
		}

		// return the sites
		Client client = null;
		Term term = null;
		Boolean byTerm = cdpService().readBoolean(parameters.get("byTerm"));
		String search = cdpService().readString(parameters.get("search"));
		Integer pageNum = cdpService().readInt(parameters.get("pageNum"));
		Integer pageSize = cdpService().readInt(parameters.get("pageSize"));

		if (byTerm != null)
		{
			respondAllSites(rv, client, term, search, byTerm, pageNum, pageSize);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPurge(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String ids = (String) parameters.get("ids");
		if (ids == null)
		{
			M_log.warn("dispatchPurge: missing ids");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] idStrs = split(ids, "\t");

		List<Site> sitesToPurge = new ArrayList<Site>();
		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			Site site = siteService().get(id);

			// security: authenticatedUser must have a role of instructor "or higher" in the site
			if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
			sitesToPurge.add(site);
		}

		// purge the sites
		for (Site site : sitesToPurge)
		{
			purgeService().purge(site);
		}

		// return the sites
		Client client = null;
		Term term = null;
		Boolean byTerm = cdpService().readBoolean(parameters.get("byTerm"));
		String search = cdpService().readString(parameters.get("search"));
		Integer pageNum = cdpService().readInt(parameters.get("pageNum"));
		Integer pageSize = cdpService().readInt(parameters.get("pageSize"));

		respondAllSites(rv, client, term, search, byTerm, pageNum, pageSize);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSites(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		List<Map<String, Object>> sitesMap = new ArrayList<Map<String, Object>>();
		rv.put("sites", sitesMap);

		// get ALL the authenticated user's sites, ordered by term, then name
		// TODO: what subset of site info is needed by mysites?
		List<SiteMember> sites = rosterService().sitesForUser(authenticatedUser);
		for (SiteMember member : sites)
		{
			sitesMap.add(member.getSite().send(member.getRole(), Boolean.FALSE));
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUnpublish(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String ids = (String) parameters.get("ids");
		if (ids == null)
		{
			M_log.warn("dispatchUnpublish: missing ids");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] idStrs = split(ids, "\t");

		List<Site> sitesToUnpublish = new ArrayList<Site>();
		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			Site site = siteService().get(id);

			// security: authenticatedUser must have a role of instructor "or higher" in the site
			if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
			sitesToUnpublish.add(site);
		}

		// unpublish the sites
		for (Site site : sitesToUnpublish)
		{
			site.setPublished(Boolean.FALSE);
			siteService().save(authenticatedUser, site);
		}

		// return the sites
		Client client = null;
		Term term = null;
		Boolean byTerm = cdpService().readBoolean(parameters.get("byTerm"));
		String search = cdpService().readString(parameters.get("search"));
		Integer pageNum = cdpService().readInt(parameters.get("pageNum"));
		Integer pageSize = cdpService().readInt(parameters.get("pageSize"));

		if (byTerm != null)
		{
			respondAllSites(rv, client, term, search, byTerm, pageNum, pageSize);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUpdate(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// the site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchUpdate: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().get(siteId);
		if (site == null)
		{
			M_log.warn("dispatchUpdate: missing site: " + siteId);
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

		// tools
		String tools = cdpService().readString(parameters.get("tools"));
		if (tools != null)
		{
			Set<Tool> desiredTools = new HashSet<Tool>();

			String[] toolIdStrs = split(tools, ",");
			for (String idStr : toolIdStrs)
			{
				Integer toolId = Integer.valueOf(idStr);
				Tool tool = Tool.valueOf(toolId);
				desiredTools.add(tool);
			}

			site.setTools(desiredTools);
		}

		// skin
		Long skinId = cdpService().readLong(parameters.get("skin"));
		if (skinId != null)
		{
			Skin skin = siteService().getSkin(skinId);
			site.setSkin(skin);
		}

		// links
		Integer linkCount = cdpService().readInt(parameters.get("links"));
		if (linkCount != null)
		{
			List<Link> links = new ArrayList<Link>();
			for (int i = 0; i < linkCount; i++)
			{
				String title = trimToNull(cdpService().readString(parameters.get("title" + i)));
				String url = trimToNull(cdpService().readString(parameters.get("url" + i)));
				if ((title != null) && (url != null))
				{
					Long id = cdpService().readLong(parameters.get("id" + i));

					Link link = site.wrap(id, title, url, i + 1);
					links.add(link);
				}
			}

			site.setLinks(links);
		}

		// publication
		Integer publishOption = cdpService().readInt(parameters.get("publishOption"));
		if (publishOption != null)
		{
			Date publishDate = cdpService().readDate(parameters.get("publishDate"));
			Date unpublishDate = cdpService().readDate(parameters.get("unpublishDate"));

			// option 1, publish now - clear any dates
			if (publishOption == 1)
			{
				site.setPublished(Boolean.TRUE);
			}
			// option 2, unpublish now - clear any dates
			else if (publishOption == 2)
			{
				site.setPublished(Boolean.FALSE);
			}
			// option 3, dates - set the dates
			else
			{
				site.setPublishOn(publishDate);
				site.setUnpublishOn(unpublishDate);
			}
		}

		siteService().save(authenticatedUser, site);

		// baseDate
		Date newBaseDate = cdpService().readDate(parameters.get("newBaseDate"));
		if (newBaseDate != null)
		{
			Date baseDate = cdpService().readDate(parameters.get("baseDate"));
			int days = baseDateService().computeDayDifference(baseDate, newBaseDate);
			baseDateService().adjustDatesByDays(site, days, authenticatedUser);
		}

		// get the updated site for return
		site = siteService().get(siteId);
		rv.put("site", site.send(rosterService().userRoleInSite(authenticatedUser, site), Boolean.FALSE));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected void respondAllSites(Map<String, Object> rv, Client client, Term term, String search, Boolean byTerm, Integer pageNum, Integer pageSize)
	{
		List<Map<String, Object>> sitesMap = new ArrayList<Map<String, Object>>();
		rv.put("sites", sitesMap);

		List<Site> sites = siteService().find(null, null, search, byTerm, pageNum, pageSize);
		for (Site site : sites)
		{
			Map<String, Object> siteMap = site.send(Role.admin, Boolean.FALSE);

			Membership members = rosterService().getActiveSiteMembers(site);
			List<Member> instructors = members.findRole(Role.instructor);
			if (!instructors.isEmpty())
			{
				// TODO: many?
				Member instructor = instructors.get(0);
				siteMap.put("instructor", instructor.getUser().getNameDisplay() + " (" + instructor.getUser().getIidDisplay() + ")");
				// TODO: official? user?
				if (instructor.getUser().getEmailOfficial() != null) siteMap.put("instructorEmail", instructor.getUser().getEmailOfficial());
			}

			sitesMap.add(siteMap);
		}
	}

	/**
	 * @return The registered ArchiveService.
	 */
	private ArchiveService archiveService()
	{
		return (ArchiveService) Services.get(ArchiveService.class);
	}

	/**
	 * @return The registered BaseDateService.
	 */
	private BaseDateService baseDateService()
	{
		return (BaseDateService) Services.get(BaseDateService.class);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered PurgeService.
	 */
	private PurgeService purgeService()
	{
		return (PurgeService) Services.get(PurgeService.class);
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
	}

	/**
	 * @return The registered SiteImportService.
	 */
	private SiteImportService siteImportService()
	{
		return (SiteImportService) Services.get(SiteImportService.class);
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}
}
