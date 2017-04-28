/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/social/social-webapp/src/main/java/org/etudes/social/webapp/SocialServlet.java $
 * $Id: SocialServlet.java 11103 2015-06-13 04:19:29Z ggolden $
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

package org.etudes.social.webapp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.social.api.SocialService;
import org.etudes.tool.api.Tool;

/**
 * Lifecycle container servlet for E3: home
 */
@SuppressWarnings("serial")
public class SocialServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SocialServlet.class);

	/** The CDP Handler. */
	protected CdpHandler handler = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		// remove the social service
		Services.remove(SocialService.class, Tool.social);
		M_log.info("destroy(): removed SocialService");

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
		return "SocialServlet";
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
		SocialServiceImpl service = (SocialServiceImpl) Services.get(SocialService.class);
		if (service == null)
		{
			service = new SocialServiceImpl();

			// register as the social service, as a SiteContentHandler
			Services.register(SocialService.class, service, Tool.home, SiteContentHandler.class);
			M_log.info("init() - created and registered new SocialService: " + service);
		}
		else
		{
			M_log.info("init() - found existing SocialService: " + service);
		}

		// create and register the cdp handler - run when Services are available
		final CdpHandler handler = new SocialCdpHandler();
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
