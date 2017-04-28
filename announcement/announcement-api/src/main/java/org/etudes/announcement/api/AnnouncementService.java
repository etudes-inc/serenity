/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/announcement/announcement-api/src/main/java/org/etudes/announcement/api/AnnouncementService.java $
 * $Id: AnnouncementService.java 11458 2015-08-17 02:13:17Z ggolden $
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

package org.etudes.announcement.api;

import java.util.List;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * The E3 AnnouncementService.
 */
public interface AnnouncementService
{
	/**
	 * Create a new announcement in the site
	 * 
	 * @param addedBy
	 *        The user doing the adding.
	 * @param inSite
	 *        The site holding the announcement.
	 * @return The added announcement.
	 */
	Announcement add(User addedBy, Site inSite);

	/**
	 * Check that this id is a valid announcement id. Use check() instead of get() if you don't need the full announcement information loaded.
	 * 
	 * @param id
	 *        The announcement id.
	 * @return An Announcement object with the id (at least) set, or null if not found
	 */
	Announcement check(Long id);

	/**
	 * Count the announcements in the site.
	 * 
	 * @param inSite
	 *        The site.
	 * @return The total number of announcements in the site.
	 */
	Integer countBySite(Site inSite);

	/**
	 * Find the announcements in this site.
	 * 
	 * @param inSite
	 *        The site holding the announcements.
	 * @return The site's announcements.
	 */
	List<Announcement> findBySite(Site inSite);

	/**
	 * Find published announcements, one per site (the top order) for each site in which the user is an active member, the site is published. Order by term desc and site title asc.
	 * 
	 * @param user
	 *        The user.
	 * @return The List of Announcements, may be empty.
	 */
	List<Announcement> findTopByUserSites(User user);

	/**
	 * Find top n published announcements for the site, ordered by "top"
	 * 
	 * @param site
	 *        The site.
	 * @param n
	 *        The number of announcements.
	 * @return The List of Announcements, may be empty.
	 */
	List<Announcement> findTopInSites(Site site, Integer n);

	/**
	 * Get the announcement with this id.
	 * 
	 * @param id
	 *        The announcement id.
	 * @return The announcement with this id, or null if not found.
	 */
	Announcement get(Long id);

	/**
	 * Refresh this Announcement object with a full data load from the database, overwriting any values, setting it to unchanged.
	 * 
	 * @param announcement
	 *        The announcement.
	 */
	void refresh(Announcement announcement);

	/**
	 * Remove this announcement.
	 * 
	 * @param announcement
	 *        The announcement.
	 */
	void remove(Announcement announcement);

	/**
	 * Save any changes made to this announcement.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param announcement
	 *        The announcement to save.
	 */
	void save(User savedBy, Announcement announcement);

	/**
	 * Encapsulate an announcement id into an Announcement object. The id is not checked.
	 * 
	 * @param id
	 *        The announcement id.
	 * @return An Announcement object with this id set.
	 */
	Announcement wrap(Long id);
}
