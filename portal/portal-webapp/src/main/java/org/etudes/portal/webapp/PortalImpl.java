/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/portal/portal-webapp/src/main/java/org/etudes/portal/webapp/PortalImpl.java $
 * $Id: PortalImpl.java 8313 2014-06-25 01:23:41Z ggolden $
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

import org.etudes.portal.api.Portal;
import org.etudes.portal.api.PortalService;
import org.etudes.service.api.Services;

/**
 * User implementation.
 */
public class PortalImpl implements Portal
{
	/** Our log. */
	// private static Log M_log = LogFactory.getLog(BlogImpl.class);

	/**
	 * @return The registered PortalService.
	 */
	private PortalService portalService()
	{
		return (PortalService) Services.get(PortalService.class);
	}
}
