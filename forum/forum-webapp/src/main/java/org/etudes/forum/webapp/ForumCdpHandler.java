/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/forum/forum-webapp/src/main/java/org/etudes/forum/webapp/ForumCdpHandler.java $
 * $Id: ForumCdpHandler.java 10431 2015-04-06 19:16:59Z ggolden $
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

package org.etudes.forum.webapp;

import java.io.IOException;
import java.util.HashMap;
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
import org.etudes.forum.api.ForumService;
import org.etudes.user.api.User;

/**
 */
public class ForumCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(ForumCdpHandler.class);

	public String getPrefix()
	{
		return "forum";
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

		return null;
	}

	/**
	 * @return The registered ForumService.
	 */
	private ForumService forumService()
	{
		return (ForumService) Services.get(ForumService.class);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}
