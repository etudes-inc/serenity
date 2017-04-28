/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-webapp/src/main/java/org/etudes/home/webapp/HomeServlet.java $
 * $Id: HomeServlet.java 10509 2015-04-17 21:50:49Z ggolden $
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

package org.etudes.home.webapp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.home.api.HomeService;
import org.etudes.service.api.Services;
import org.etudes.sitecontent.api.DateProvider;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.tool.api.Tool;

/**
 * Lifecycle container servlet for E3: home
 */
@SuppressWarnings("serial")
public class HomeServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(HomeServlet.class);

	/** The CDP Handler. */
	protected CdpHandler handler = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		// remove the home service
		Services.remove(HomeService.class, Tool.home);
		M_log.info("destroy(): removed HomeService");

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
		return "HomeServlet";
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

		// create the home service
		HomeServiceImpl HomeService = (HomeServiceImpl) Services.get(HomeService.class);
		if (HomeService == null)
		{
			HomeService = new HomeServiceImpl();

			// register as the home service, and as also doing SiteContentHandler and ReferenceHolder
			Services.register(HomeService.class, HomeService, Tool.home, SiteContentHandler.class, DateProvider.class);
			M_log.info("init() - created and registered new HomeService: " + HomeService);
		}
		else
		{
			M_log.info("init() - found existing HomeService: " + HomeService);
		}

		// create and register the cdp handler - run when Services are available
		final CdpHandler handler = new HomeCdpHandler();
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
