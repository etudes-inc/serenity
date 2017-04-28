/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/portal/portal-webapp/src/main/java/org/etudes/portal/webapp/PortalServlet.java $
 * $Id: PortalServlet.java 10165 2015-02-26 23:24:48Z ggolden $
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

package org.etudes.portal.webapp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.portal.api.PortalService;
import org.etudes.service.api.Services;
import org.etudes.tool.api.Tool;

/**
 * Lifecycle container servlet for E3: portal
 */
@SuppressWarnings("serial")
public class PortalServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PortalServlet.class);

	/** The CDP Handler. */
	protected CdpHandler handler = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		// remove the portal service
		Services.remove(PortalService.class, Tool.portal);
		M_log.info("destroy(): removed PortalService");

		// remove the CDP handler
		if (this.handler != null)
		{
			CdpService cdpService = (CdpService) Services.get(CdpService.class);
			if (cdpService != null)
			{
				cdpService.unregisterCdpHandler(this.handler);
				this.handler = null;
				M_log.info("destroy(): unregistered CDP handler");
			}
		}

		M_log.info("destroy()");
		super.destroy();
	}

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "PortalServlet";
	}

	/**
	 * Initialize the servlet.
	 * 
	 * @param config
	 *        The servlet config.
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		// create the portal service
		PortalServiceImpl portalService = (PortalServiceImpl) Services.get(PortalService.class);
		if (portalService == null)
		{
			portalService = new PortalServiceImpl();
			Services.register(PortalService.class, portalService, null);
			M_log.info("init() - created and registered new PortalService: " + portalService);
		}
		else
		{
			M_log.info("init() - found existing PortalService: " + portalService);
		}

		// create and register the cdp handler - run when Services are available
		final CdpHandler handler = new PortalCdpHandler();
		this.handler = handler;
		Services.whenAvailable(CdpService.class, new Runnable()
		{
			public void run()
			{
				CdpService cdpService = (CdpService) Services.get(CdpService.class);
				cdpService.registerCdpHandler(handler);
				M_log.info("init(): registered CDP handler");
			}
		});
	}
}
