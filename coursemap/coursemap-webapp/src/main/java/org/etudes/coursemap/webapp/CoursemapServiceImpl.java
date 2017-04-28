/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/coursemap/coursemap-webapp/src/main/java/org/etudes/coursemap/webapp/CoursemapServiceImpl.java $
 * $Id: CoursemapServiceImpl.java 10509 2015-04-17 21:50:49Z ggolden $
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

package org.etudes.coursemap.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.coursemap.api.CoursemapService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.user.api.User;

/**
 * CoursemapServiceImpl implements CoursemapService.
 */
public class CoursemapServiceImpl implements CoursemapService, Service, SiteContentHandler
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(CoursemapService.class);

	/**
	 * Construct
	 */
	public CoursemapServiceImpl()
	{
		M_log.info("CoursemapServiceImpl: construct");
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
		M_log.info("CoursemapServiceImpl: start");
		return true;
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
}
