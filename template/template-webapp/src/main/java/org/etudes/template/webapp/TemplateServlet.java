/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/template/template-webapp/src/main/java/org/etudes/template/webapp/TemplateServlet.java $
 * $Id: TemplateServlet.java 10430 2015-04-06 16:45:07Z ggolden $
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

package org.etudes.template.webapp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;
import org.etudes.template.api.TemplateService;
import org.etudes.tool.api.Tool;

/**
 * Lifecycle container servlet for E3: Template
 */
@SuppressWarnings("serial")
public class TemplateServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(TemplateServlet.class);

	/** The CDP Handler. */
	protected CdpHandler handler = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		// remove the service
		Services.remove(TemplateService.class, Tool.template);
		M_log.info("destroy(): removed TemplateService");

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
		return "TemplateServlet";
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
		TemplateServiceImpl templateService = (TemplateServiceImpl) Services.get(TemplateService.class);
		if (templateService == null)
		{
			templateService = new TemplateServiceImpl();

			// register as the home service, and as also doing SiteContentHandler and ReferenceHolder
			Services.register(TemplateService.class, templateService, Tool.template);
			M_log.info("init() - created and registered new TemplateService: " + templateService);
		}
		else
		{
			M_log.info("init() - found existing TemplateService: " + templateService);
		}

		// create and register the cdp handler - run when Services are available
		final CdpHandler handler = new TemplateCdpHandler();
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
