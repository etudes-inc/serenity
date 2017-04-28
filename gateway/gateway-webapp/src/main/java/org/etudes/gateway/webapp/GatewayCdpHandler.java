/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/gateway/gateway-webapp/src/main/java/org/etudes/gateway/webapp/GatewayCdpHandler.java $
 * $Id: GatewayCdpHandler.java 10386 2015-04-01 20:41:28Z ggolden $
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

package org.etudes.gateway.webapp;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.file.api.FileService;
import org.etudes.gateway.api.GatewayService;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 */
public class GatewayCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(GatewayCdpHandler.class);

	public String getPrefix()
	{
		return "home";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// // if no authenticated user, we reject all requests
		// if (authenticatedUser == null)
		// {
		// Map<String, Object> rv = new HashMap<String, Object>();
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
		// return rv;
		// }
		//
		// else if (requestPath.equals("getSiteBlogs"))
		// {
		// return dispatchGetSiteBlogs(req, res, parameters, path, authenticatedUser);
		// }

		return null;
	}

	// protected Map<String, Object> dispatchAdd(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
	// User authenticatedUser) throws ServletException, IOException
	// {
	// Map<String, Object> rv = new HashMap<String, Object>();
	//
	// // we won't need to get() the site, we can just site() it to encapsulate the id in a Site object.
	// Long siteId = cdpService().readLong(parameters.get("site"));
	// if (siteId == null)
	// {
	// M_log.warn("dispatchAdd: missing site");
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
	// return rv;
	// }
	// Site site = siteService().wrap(siteId);
	//
	// String name = cdpService().readString(parameters.get("name"));
	//
	// // we don't need this user's details - existence will be checked when we check that the user is authorized to have a blog in the site
	// Long ownerId = cdpService().readLong(parameters.get("owner"));
	// if (ownerId == null)
	// {
	// M_log.warn("dispatchAdd: missing owner");
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
	// return rv;
	// }
	// User owner = userService().wrap(ownerId);
	//
	// // security: authenticatedUser must have a role of instructor "or higher" in the site
	// if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
	// {
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
	// return rv;
	// }
	//
	// // the owner must have student "or higher" role in the site
	// if (!rosterService().userRoleInSite(owner, site).ge(Role.student))
	// {
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
	// return rv;
	// }
	//
	// Blog blog = blogService().add(authenticatedUser, site, owner);
	//
	// if (name != null) blog.setName(name);
	//
	// blogService().save(authenticatedUser, blog);
	//
	// // add status parameter
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
	//
	// return rv;
	// }

	/**
	 * @return The registered GatewayService.
	 */
	private GatewayService gatewayService()
	{
		return (GatewayService) Services.get(GatewayService.class);
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
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
