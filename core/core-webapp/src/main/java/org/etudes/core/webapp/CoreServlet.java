/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/core/webapp/CoreServlet.java $
 * $Id: CoreServlet.java 10509 2015-04-17 21:50:49Z ggolden $
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

package org.etudes.core.webapp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.authentication.api.AuthenticationService;
import org.etudes.authentication.webapp.AuthenticationServiceImpl;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.webapp.CdpServiceImpl;
import org.etudes.config.api.ConfigService;
import org.etudes.config.webapp.ConfigServiceImpl;
import org.etudes.cron.api.CronHandler;
import org.etudes.cron.api.CronService;
import org.etudes.cron.webapp.CronServiceImpl;
import org.etudes.download.api.DownloadHandler;
import org.etudes.email.api.EmailService;
import org.etudes.email.webapp.EmailServiceImpl;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.webapp.EvaluationServiceImpl;
import org.etudes.file.api.FileService;
import org.etudes.file.webapp.FileServiceImpl;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.webapp.RosterServiceImpl;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.site.webapp.SiteServiceImpl;
import org.etudes.sitecontent.api.ArchiveService;
import org.etudes.sitecontent.api.BaseDateService;
import org.etudes.sitecontent.api.DateProvider;
import org.etudes.sitecontent.api.PurgeService;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.SiteImportService;
import org.etudes.sitecontent.api.StudentContentHandler;
import org.etudes.sitecontent.webapp.ArchiveServiceImpl;
import org.etudes.sitecontent.webapp.BaseDateServiceImpl;
import org.etudes.sitecontent.webapp.PurgeServiceImpl;
import org.etudes.sitecontent.webapp.SiteImportServiceImpl;
import org.etudes.sql.api.SqlService;
import org.etudes.sql.webapp.SqlServiceImpl;
import org.etudes.threadlocal.api.ThreadLocalService;
import org.etudes.threadlocal.webapp.ThreadLocalServiceImpl;
import org.etudes.tool.api.Tool;
import org.etudes.tracking.api.TrackingService;
import org.etudes.tracking.webapp.TrackingServiceImpl;
import org.etudes.user.api.UserService;
import org.etudes.user.webapp.UserServiceImpl;

/**
 * Lifecycle container servlet for E3: storage
 */
@SuppressWarnings("serial")
public class CoreServlet extends HttpServlet
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(CoreServlet.class);

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		// remove the evaluation service
		Services.remove(EvaluationService.class, Tool.evaluation);
		M_log.info("destroy(): removed EvaluationService");

		// remove the email service
		Services.remove(EmailService.class, null);
		M_log.info("destroy(): removed EmailService");

		// remove the tracking service
		Services.remove(TrackingService.class, null);
		M_log.info("destroy(): removed TrackingService");

		// remove the authentication service
		Services.remove(AuthenticationService.class, null);
		M_log.info("destroy(): removed AuthenticationService");

		// TODO: ?? do NOT remove the cdp service, as it has registered handlers.
		Services.remove(CdpService.class, null);
		M_log.info("destroy(): removed CdpService");

		// remove the archive service
		Services.remove(ArchiveService.class, null);
		M_log.info("destroy(): removed ArchiveService");

		// remove the purge service
		Services.remove(PurgeService.class, null);
		M_log.info("destroy(): removed PurgeService");

		// remove the base date service
		Services.remove(BaseDateService.class, null);
		M_log.info("destroy(): removed BaseDateService");

		// remove the importer service
		Services.remove(SiteImportService.class, null);
		M_log.info("destroy(): removed SiteImportService");

		// remove the site service
		Services.remove(SiteService.class, null);
		M_log.info("destroy(): removed SiteService");

		// remove the roster service
		Services.remove(RosterService.class, Tool.roster);
		M_log.info("destroy(): removed RosterService");

		// remove the user service
		Services.remove(UserService.class, Tool.user);
		M_log.info("destroy(): removed UserService");

		// remove the file service
		Services.remove(FileService.class, null);
		M_log.info("destroy(): removed FileService");

		// remove the sql service
		Services.remove(SqlService.class, null);
		M_log.info("destroy(): removed SqlService");

		// remove the cron service
		Services.remove(CronService.class, null);
		M_log.info("destroy(): removed CronService");

		// remove the config service
		Services.remove(ConfigService.class, null);
		M_log.info("destroy(): removed ConfigService");

		// remove the threadlocal service
		Services.remove(ThreadLocalService.class, null);
		M_log.info("destroy(): removed TestService");

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
		return "CoreServlet";
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

		// create the threadlocal service
		ThreadLocalService threadLocalService = (ThreadLocalService) Services.get(ThreadLocalService.class);
		if (threadLocalService == null)
		{
			threadLocalService = new ThreadLocalServiceImpl();
			Services.register(ThreadLocalService.class, (ThreadLocalServiceImpl) threadLocalService, null);
			M_log.info("init() - created and registered new ThreadLocalService: " + threadLocalService);
		}
		else
		{
			M_log.info("init() - found existing ThreadLocalService: " + threadLocalService);
		}

		// create the config service
		ConfigService configService = (ConfigService) Services.get(ConfigService.class);
		if (configService == null)
		{
			configService = new ConfigServiceImpl();
			Services.register(ConfigService.class, (ConfigServiceImpl) configService, null);
			M_log.info("init() - created and registered new ConfigService: " + configService);
		}
		else
		{
			M_log.info("init() - found existing configService: " + configService);
		}

		// create the cron service
		CronService cronService = (CronService) Services.get(CronService.class);
		if (cronService == null)
		{
			cronService = new CronServiceImpl();
			Services.register(CronService.class, (CronServiceImpl) cronService, null);
			M_log.info("init() - created and registered new CronService: " + cronService);
		}
		else
		{
			M_log.info("init() - found existing CronService: " + cronService);
		}

		// create the sql service
		SqlService sqlService = (SqlService) Services.get(SqlService.class);
		if (sqlService == null)
		{
			sqlService = new SqlServiceImpl();
			Services.register(SqlService.class, (SqlServiceImpl) sqlService, null);
			M_log.info("init() - created and registered new SqlService: " + sqlService);
		}
		else
		{
			M_log.info("init() - found existing SqlService: " + sqlService);
		}

		// create the file service
		FileService fileService = (FileService) Services.get(FileService.class);
		if (fileService == null)
		{
			fileService = new FileServiceImpl();
			Services.register(FileService.class, (FileServiceImpl) fileService, null);
			M_log.info("init() - created and registered new FileService: " + fileService);
		}
		else
		{
			M_log.info("init() - found existing FileService: " + fileService);
		}

		// create the user service - the User tool holds references
		UserService userService = (UserService) Services.get(UserService.class);
		if (userService == null)
		{
			userService = new UserServiceImpl();
			Services.register(UserService.class, (UserServiceImpl) userService, Tool.user);
			M_log.info("init() - created and registered new UserService: " + userService);
		}
		else
		{
			M_log.info("init() - found existing UserService: " + userService);
		}

		// create the roster service
		RosterService rosterService = (RosterService) Services.get(RosterService.class);
		if (rosterService == null)
		{
			rosterService = new RosterServiceImpl();
			Services.register(RosterService.class, (RosterServiceImpl) rosterService, Tool.roster, CronHandler.class);
			M_log.info("init() - created and registered new RosterService: " + rosterService);
		}
		else
		{
			M_log.info("init() - found existing RosterService: " + rosterService);
		}

		// create the site service
		SiteService siteService = (SiteService) Services.get(SiteService.class);
		if (siteService == null)
		{
			siteService = new SiteServiceImpl();
			Services.register(SiteService.class, (SiteServiceImpl) siteService, Tool.site, SiteContentHandler.class, DateProvider.class,
					StudentContentHandler.class);
			M_log.info("init() - created and registered new SiteService: " + siteService);
		}
		else
		{
			M_log.info("init() - found existing SiteService: " + siteService);
		}

		// create the site import service
		SiteImportService siteImportService = (SiteImportService) Services.get(SiteImportService.class);
		if (siteImportService == null)
		{
			siteImportService = new SiteImportServiceImpl();
			Services.register(SiteImportService.class, (SiteImportServiceImpl) siteImportService, null);
			M_log.info("init() - created and registered new SiteImportService: " + siteImportService);
		}
		else
		{
			M_log.info("init() - found existing SiteImportService: " + siteImportService);
		}

		// create the base date service
		BaseDateService baseDateService = (BaseDateService) Services.get(BaseDateService.class);
		if (baseDateService == null)
		{
			baseDateService = new BaseDateServiceImpl();
			Services.register(BaseDateService.class, (BaseDateServiceImpl) baseDateService, null);
			M_log.info("init() - created and registered new BaseDateService: " + baseDateService);
		}
		else
		{
			M_log.info("init() - found existing BaseDateService: " + baseDateService);
		}

		// create the purge service
		PurgeService purgeService = (PurgeService) Services.get(PurgeService.class);
		if (purgeService == null)
		{
			purgeService = new PurgeServiceImpl();
			Services.register(PurgeService.class, (PurgeServiceImpl) purgeService, null);
			M_log.info("init() - created and registered new PurgeService: " + purgeService);
		}
		else
		{
			M_log.info("init() - found existing PurgeService: " + purgeService);
		}

		// create the archive service
		ArchiveService archiveService = (ArchiveService) Services.get(ArchiveService.class);
		if (archiveService == null)
		{
			archiveService = new ArchiveServiceImpl();
			Services.register(ArchiveService.class, (ArchiveServiceImpl) archiveService, null);
			M_log.info("init() - created and registered new ArchiveService: " + purgeService);
		}
		else
		{
			M_log.info("init() - found existing ArchiveService: " + archiveService);
		}

		// create the cdp service
		CdpService cdpService = (CdpService) Services.get(CdpService.class);
		if (cdpService == null)
		{
			cdpService = new CdpServiceImpl();
			Services.register(CdpService.class, (CdpServiceImpl) cdpService, null);
			M_log.info("init() - created and registered new CdpService: " + cdpService);
		}
		else
		{
			M_log.info("init() - found existing CdpService: " + cdpService);
		}

		// create the authentication service
		AuthenticationService authenticationService = (AuthenticationService) Services.get(AuthenticationService.class);
		if (authenticationService == null)
		{
			authenticationService = new AuthenticationServiceImpl();
			Services.register(AuthenticationService.class, (AuthenticationServiceImpl) authenticationService, null);
			M_log.info("init() - created and registered new AuthenticationService: " + authenticationService);
		}
		else
		{
			M_log.info("init() - found existing AuthenticationService: " + authenticationService);
		}

		// create the tracking service
		TrackingServiceImpl trackingService = (TrackingServiceImpl) Services.get(TrackingService.class);
		if (trackingService == null)
		{
			trackingService = new TrackingServiceImpl();

			// register as the tracking service
			Services.register(TrackingService.class, trackingService, Tool.presence, CronHandler.class);
			M_log.info("init() - created and registered new TrackingService: " + trackingService);
		}
		else
		{
			M_log.info("init() - found existing TrackingService: " + trackingService);
		}

		// create the email service
		EmailServiceImpl emailService = (EmailServiceImpl) Services.get(EmailService.class);
		if (emailService == null)
		{
			emailService = new EmailServiceImpl();

			// register as the email service
			Services.register(EmailService.class, emailService, null);
			M_log.info("init() - created and registered new EmailService: " + emailService);
		}
		else
		{
			M_log.info("init() - found existing EmailService: " + emailService);
		}

		// create the evaluation service
		EvaluationServiceImpl evaluationService = (EvaluationServiceImpl) Services.get(EvaluationService.class);
		if (evaluationService == null)
		{
			evaluationService = new EvaluationServiceImpl();

			// register as the evaluation service
			Services.register(EvaluationService.class, evaluationService, Tool.evaluation, SiteContentHandler.class, DownloadHandler.class,
					StudentContentHandler.class);
			M_log.info("init() - created and registered new EvaluationService: " + evaluationService);
		}
		else
		{
			M_log.info("init() - found existing EvaluationService: " + evaluationService);
		}
	}
}
