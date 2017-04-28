/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/user/user-webapp/src/main/java/org/etudes/user/webapp/UserCdpHandler.java $
 * $Id: UserCdpHandler.java 12056 2015-11-10 23:10:08Z ggolden $
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

package org.etudes.user.webapp;

import static org.etudes.util.StringUtil.splitLast;

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
import org.etudes.service.api.Services;
import org.etudes.sitecontent.api.PurgeService;
import org.etudes.user.api.Iid;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;
import org.etudes.user.api.UserService.Sort;

/**
 */
public class UserCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(UserCdpHandler.class);

	public String getPrefix()
	{
		return "user";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		if (requestPath.equals("resetPw"))
		{
			return dispatchResetPw(req, res, parameters, path, authenticatedUser);
		}

		else
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

			else if (requestPath.equals("purge"))
			{
				return dispatchPurge(req, res, parameters, path, authenticatedUser);
			}

			else if (requestPath.equals("resetPw"))
			{
				return dispatchResetPw(req, res, parameters, path, authenticatedUser);
			}
		}

		return null;
	}

	protected Map<String, Object> dispatchGet(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: Admin only
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		String search = cdpService().readString(parameters.get("search"));
		Integer searchType = cdpService().readInt(parameters.get("searchType"));
		Integer pageNum = cdpService().readInt(parameters.get("pageNum"));
		Integer pageSize = cdpService().readInt(parameters.get("pageSize"));

		if (pageNum == null) pageNum = 1;
		if (pageSize == null) pageSize = 50;

		List<User> users = null;
		if (search == null || searchType == null || searchType == 3)
		{
			users = userService().find(search, Sort.name, pageNum, pageSize);
		}
		else
		{
			// iid
			if (searchType == 0)
			{
				users = new ArrayList<User>();
				String[] parts = splitLast(search, "@");
				if (parts != null)
				{
					Iid iid = userService().makeIid(parts[0], parts[1]);
					User user = userService().findByIid(iid);
					if (user != null)
					{
						users.add(user);
					}
				}
			}

			// eid
			else if (searchType == 1)
			{
				users = userService().findByEid(search);
			}

			// email
			else
			{
				users = userService().findByEmail(search);
			}
		}

		List<Map<String, Object>> usersList = new ArrayList<Map<String, Object>>();
		rv.put("users", usersList);

		for (User user : users)
		{
			usersList.add(user.send());
		}

		rv.put("total", userService().count(search));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPurge(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// security: Admin only
		if (!authenticatedUser.isAdmin())
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Long> ids = cdpService().readIds(parameters.get("ids"));
		for (Long id : ids)
		{
			User user = userService().get(id);
			if (user != null)
			{
				// use the purge service to get a deep delete
				purgeService().purge(user);
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchResetPw(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String email = cdpService().readString(parameters.get("email"));

		rv.put("reset", userService().resetPassword(email));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		boolean admin = authenticatedUser.isAdmin();

		// null for a new user - admin only
		Long id = cdpService().readLong(parameters.get("user"));
		if (id == null)
		{
			M_log.warn("dispatchSave: missing user");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		User user = null;
		if (id == -1)
		{
			if (admin)
			{
				user = userService().add(authenticatedUser);
			}
		}
		else
		{
			user = userService().get(id);
		}

		if (user == null)
		{
			M_log.warn("dispatchSave: null user");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		boolean self = user.equals(authenticatedUser);

		// security: Admin only, if the user being updated is not the authenticated user
		if (!(self || admin))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		user.read("", parameters, admin);

		userService().save(authenticatedUser, user);

		Boolean fetch = cdpService().readBoolean(parameters.get("fetch"));
		if ((fetch != null) && fetch)
		{
			// return the user data
			rv.put("user", user.send());
		}
		else
		{
			// return id of the saved user
			rv.put("id", user.getId());
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
	 * @return The registered PurgeService.
	 */
	private PurgeService purgeService()
	{
		return (PurgeService) Services.get(PurgeService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
