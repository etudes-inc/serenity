/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/social/social-webapp/src/main/java/org/etudes/social/webapp/SocialServiceImpl.java $
 * $Id: SocialServiceImpl.java 11103 2015-06-13 04:19:29Z ggolden $
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

package org.etudes.social.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.file.api.FileService;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.social.api.SocialService;
import org.etudes.sql.api.SqlService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * SocialServiceImpl implements SocialService.
 */
public class SocialServiceImpl implements SocialService, Service, SiteContentHandler
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SocialServiceImpl.class);

	/**
	 * Construct
	 */
	public SocialServiceImpl()
	{
		M_log.info("HomeServiceImpl: construct");
	}

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		// TODO:
		M_log.info("archive");
	}

	@Override
	public void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser)
	{
		// TODO:
		M_log.info("importFromArchive");
	}

	@Override
	public void importFromSite(Site fromSite, Site toSite, User importingUser)
	{
		// TODO:
		M_log.info("importFromSite");
	}

	@Override
	public void purge(Site site)
	{
		// TODO:
		M_log.info("purge");
	}

	@Override
	public boolean start()
	{
		M_log.info("HomeServiceImpl: start");
		return true;
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
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
