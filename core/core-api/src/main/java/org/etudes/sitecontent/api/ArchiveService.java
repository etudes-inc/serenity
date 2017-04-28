/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/ArchiveService.java $
 * $Id: ArchiveService.java 10075 2015-02-13 00:03:01Z ggolden $
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

package org.etudes.sitecontent.api;

import java.util.List;
import java.util.Set;

import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * The ArchiveService service, responsible for dealing with site content archives.
 */
public interface ArchiveService
{
	/**
	 * Archive the site and all of its content.
	 * 
	 * @param site
	 *        The site.
	 * @param authoredContentOnly
	 *        if TRUE, archive only authored content. Otherwise archive all content (submissions, evaluations, tracking, etc).
	 * @return true if successful, false if not.
	 */
	boolean archive(Site site, Boolean authoredContentOnly);

	/**
	 * Find the archive for this site.
	 * 
	 * @param site
	 *        The site.
	 * @return The archive, or null if not found.
	 */
	ArchivedSite archiveForSite(Site site);

	/**
	 * Find all archives the user has access to.
	 * 
	 * @param user
	 *        The user.
	 * @return The archives, ordered by term and site name.
	 */
	List<ArchivedSite> archivesForUser(User user);

	/**
	 * Find this archive.
	 * 
	 * @param id
	 *        The archive id.
	 * @return The archive, or null if not found.
	 */
	ArchivedSite getArchive(Long id);

	/**
	 * Import selected tool information from an archive
	 * 
	 * @param archivedSite
	 *        The archived site information.
	 * @param tools
	 *        A subset of tools to import content from. If null, import all content.
	 * @param intoSite
	 *        The site to get imported content.
	 * @param authoredContentOnly
	 *        if TRUE, import only authored content. Otherwise archive all content (submissions, evaluations, tracking, etc). Not all archives will have more than authored content.
	 * @param importingUser
	 *        The user doing the import.
	 * @return true if successful, false if not.
	 */
	boolean importFromArchive(ArchivedSite archivedSite, Set<Tool> fromTools, Site intoSite, Boolean authoredContentOnly, User importingUser);

	/**
	 * Remove an archive.
	 * 
	 * @param site
	 *        The archived site info.
	 * @return true if successful, false if not.
	 */
	boolean remove(ArchivedSite site);

	/**
	 * Find the tools that have content included in this archive, ordered.
	 * 
	 * @param id
	 *        The archive id.
	 * @return The list of Tools - may be empty.
	 */
	List<Tool> toolsForArchive(Long id);
}
