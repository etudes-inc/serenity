/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sitecontent/webapp/SiteImportServiceImpl.java $
 * $Id: SiteImportServiceImpl.java 10384 2015-04-01 18:36:05Z ggolden $
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

package org.etudes.sitecontent.webapp;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.SiteImportService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * SiteImportServiceImpl implements SiteImportService.
 */
public class SiteImportServiceImpl implements SiteImportService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SiteImportServiceImpl.class);

	/**
	 * Construct
	 */
	public SiteImportServiceImpl()
	{
		M_log.info("SiteImportServiceImpl: construct");
	}

	@Override
	public boolean importFromSite(Site fromSite, Set<Tool> tools, Site intoSite, User importingUser)
	{
		// assure that the site has these tools
		intoSite.assureTools(tools);
		siteService().save(importingUser, intoSite);

		// import content for these tools
		Map<Tool, Service> handlers = Services.getHandlers(SiteContentHandler.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				// only for the requested tools
				if (tools.contains(s.getKey()))
				{
					if (s.getValue() instanceof SiteContentHandler)
					{
						((SiteContentHandler) s.getValue()).importFromSite(fromSite, intoSite, importingUser);
					}
				}
			}
		}

		// TODO: site skin, links, services, publish dates
		
		// TODO: groups?
		
		// TODO: evaluation criteria import, creates new criteria (for this site), and importing tool designs need a map

		return true;
	}

	@Override
	public boolean imports(Tool tool)
	{
		boolean rv = false;
		Map<Tool, Service> handlers = Services.getHandlers(SiteContentHandler.class);
		if (handlers != null)
		{
			Set<Tool> importTools = handlers.keySet();
			if (importTools.contains(tool)) rv = true;
		}

		return rv;
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}

	@Override
	public boolean start()
	{
		M_log.info("SiteImportServiceImpl: start");
		return true;
	}
}
