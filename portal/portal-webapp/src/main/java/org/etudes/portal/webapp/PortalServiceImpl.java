/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/portal/portal-webapp/src/main/java/org/etudes/portal/webapp/PortalServiceImpl.java $
 * $Id: PortalServiceImpl.java 8802 2014-09-18 22:33:23Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

package org.etudes.portal.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.portal.api.PortalService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.sql.api.SqlService;
import org.etudes.user.api.UserService;

/**
 * PortalService implements PortalService.
 */
public class PortalServiceImpl implements PortalService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PortalServiceImpl.class);

	/**
	 * Construct
	 */
	public PortalServiceImpl()
	{
		M_log.info("PortalServiceImpl: construct");
	}

	@Override
	public boolean start()
	{
		M_log.info("PortalServiceImpl: start");
		return true;
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}

	/**
	 * @return The registered SqlService.
	 */
	private SqlService sqlService()
	{
		return (SqlService) Services.get(SqlService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
