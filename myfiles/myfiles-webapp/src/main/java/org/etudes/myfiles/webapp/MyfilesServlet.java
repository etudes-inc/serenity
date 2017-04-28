/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/myfiles/myfiles-webapp/src/main/java/org/etudes/myfiles/webapp/MyfilesServlet.java $
 * $Id: MyfilesServlet.java 9482 2014-12-07 23:58:49Z ggolden $
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

package org.etudes.myfiles.webapp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.download.api.DownloadHandler;
import org.etudes.myfiles.api.MyfilesService;
import org.etudes.service.api.Services;
import org.etudes.tool.api.Tool;

/**
 * Lifecycle container servlet for E3: myFiles
 */
@SuppressWarnings("serial")
public class MyfilesServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(MyfilesServlet.class);

	/** The CDP Handler. */
	protected CdpHandler handler = null;

	/** The download handler. */
	protected DownloadHandler downloadHandler = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
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

		// remove the myFiles service
		Services.remove(MyfilesService.class, Tool.myfiles);
		M_log.info("destroy(): removed MyfilesService");

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
		return "MyfilesServlet";
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

		// create the myFiles service
		MyfilesServiceImpl myfilesService = (MyfilesServiceImpl) Services.get(MyfilesService.class);
		if (myfilesService == null)
		{
			myfilesService = new MyfilesServiceImpl();

			// register as the myFiles service, and as also doing DownloadHandler
			Services.register(MyfilesService.class, myfilesService, Tool.myfiles, DownloadHandler.class);
			M_log.info("init() - created and registered new MyfilesService: " + myfilesService);
		}
		else
		{
			M_log.info("init() - found existing MyfilesService: " + myfilesService);
		}

		// create and register the cdp handler - run when Services are available
		final CdpHandler handler = new MyfilesCdpHandler();
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
