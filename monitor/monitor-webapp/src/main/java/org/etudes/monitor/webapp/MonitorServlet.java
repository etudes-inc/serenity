/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-webapp/src/main/java/org/etudes/monitor/webapp/MonitorServlet.java $
 * $Id: MonitorServlet.java 11992 2015-11-03 23:07:05Z ggolden $
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

package org.etudes.monitor.webapp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cron.api.CronHandler;
import org.etudes.monitor.api.MonitorService;
import org.etudes.service.api.Services;
import org.etudes.tool.api.Tool;

/**
 * Lifecycle container servlet for Serenity's monitor tool
 */
@SuppressWarnings("serial")
public class MonitorServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(MonitorServlet.class);

	/** The CDP Handler. */
	protected CdpHandler handler = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		// remove the home service
		Services.remove(MonitorService.class, Tool.syllabus);
		M_log.info("destroy(): removed MonitorService");

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
		return "MonitorServlet";
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

		// create the monitor service
		MonitorServiceImpl MonitorService = (MonitorServiceImpl) Services.get(MonitorService.class);
		if (MonitorService == null)
		{
			MonitorService = new MonitorServiceImpl();

			// register as the monitor service, and as also doing CronHandler
			Services.register(MonitorService.class, MonitorService, Tool.monitor, CronHandler.class);
			M_log.info("init() - created and registered new MonitorService: " + MonitorService);
		}
		else
		{
			M_log.info("init() - found existing MonitorService: " + MonitorService);
		}

		// create and register the cdp handler - run when Services are available
		final CdpHandler handler = new MonitorCdpHandler();
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
