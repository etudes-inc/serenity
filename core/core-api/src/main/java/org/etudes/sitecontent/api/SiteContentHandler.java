/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/SiteContentHandler.java $
 * $Id: SiteContentHandler.java 10979 2015-05-29 20:31:34Z ggolden $
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

package org.etudes.sitecontent.api;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * SiteContentHandler is code that holds site related content, so it participates in site import, archive and purge.
 */
public interface SiteContentHandler
{
	/**
	 * Archive content.
	 * 
	 * @param fromSite
	 *        The site to archive.
	 * @param authoredContentOnly
	 *        if TRUE, include only authored content, otherwise include everything.
	 * @param toArchive
	 *        The archive.
	 */
	void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive);

	/**
	 * Import content from an archive artifact.
	 * 
	 * @param fromArtifact
	 *        The artifact to import.
	 * @param authoredContentOnly
	 *        if TRUE, include only authored content, otherwise include everything.
	 * @param intoSite
	 *        The destination site.
	 * @param importingUser
	 *        The user doing the importing.
	 */
	void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser);

	/**
	 * Import content from one site to another.
	 * 
	 * @param fromSite
	 *        The source site.
	 * @param intoSite
	 *        The destination site.
	 * @param importingUser
	 *        The user doing the importing.
	 */
	void importFromSite(Site fromSite, Site intoSite, User importingUser);

	/**
	 * Purge content related to this site.
	 * 
	 * @param site
	 *        The site
	 * @return true if successful, false if not.
	 */
	void purge(Site site);
}
