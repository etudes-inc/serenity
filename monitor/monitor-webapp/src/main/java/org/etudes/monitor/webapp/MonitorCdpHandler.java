/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-webapp/src/main/java/org/etudes/monitor/webapp/MonitorCdpHandler.java $
 * $Id: MonitorCdpHandler.java 12553 2016-01-14 20:03:28Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015, 2016 Etudes, Inc.
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

package org.etudes.monitor.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import org.etudes.monitor.api.Alert;
import org.etudes.monitor.api.MonitorService;
import org.etudes.monitor.api.Options;
import org.etudes.monitor.api.Sample;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.user.api.User;

/**
 */
public class MonitorCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(MonitorCdpHandler.class);

	public String getPrefix()
	{
		return "monitor";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		// if (authenticatedUser == null)
		// {
		// Map<String, Object> rv = new HashMap<String, Object>();
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
		// return rv;
		// }
		//
		// else

		if (requestPath.equals("sample"))
		{
			return dispatchSample(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("status"))
		{
			return dispatchStatus(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("options"))
		{
			return dispatchOptions(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("optionsSave"))
		{
			return dispatchOptionsSave(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchOptions(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: ??? admin only
		// if (!authenticatedUser.isAdmin())
		// {
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
		// return rv;
		// }

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGet: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security
		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// get the current samples from each source
		Options options = monitorService().getOptions();

		rv.put("options", options.send());

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchOptionsSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
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

		// security
		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);
		if (!userRole.ge(Role.student))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// get the current samples from each source
		Options options = monitorService().getOptions();

		options.read("", parameters);

		monitorService().setOptions(options);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSample(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: the source name must match the sending IP address
		String source = cdpService().readString(parameters.get("source"));
		if (!monitorService().validSource(source, req.getRemoteAddr()))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Sample s = new Sample();
		s.read("", parameters);

		// Note: use server time, not request provided time stamp
		s.setSampledOn(new Date());

		monitorService().sample(s);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchStatus(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
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

		// security
		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// get the current samples from each source
		List<Sample> samples = monitorService().getSamples();

		List<Map<String, Object>> sampleList = new ArrayList<Map<String, Object>>();
		rv.put("samples", sampleList);

		for (Sample s : samples)
		{
			Map<String, Object> sampleMap = s.send();

			// get this source's alerts
			List<Integer> alertCodes = new ArrayList<Integer>();
			sampleMap.put("alerts", alertCodes);

			List<Alert> alerts = monitorService().getAlerts(s.getSource());
			for (Alert a : alerts)
			{
				alertCodes.add(a.getType().getCode());
			}

			sampleList.add(sampleMap);
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
	 * @return The registered MonitorService.
	 */
	private MonitorService monitorService()
	{
		return (MonitorService) Services.get(MonitorService.class);
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
