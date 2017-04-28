/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/tracking/api/TrackingService.java $
 * $Id: TrackingService.java 10451 2015-04-12 00:38:09Z ggolden $
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

package org.etudes.tracking.api;

import java.util.List;

import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

public interface TrackingService
{
	/**
	 * Remove the tracking for this site.
	 * 
	 * @param site
	 *        The site.
	 */
	void clear(Site site);

	/**
	 * Remove the tracking for this user.
	 * 
	 * @param user
	 *        The user.
	 */
	void clear(User user);

	/**
	 * Get the presence count (unique users, non expired) for the coordinate (site, tool, item).
	 * 
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 * @param itemId
	 *        The item ID.
	 * @return The count of unique users at the location.
	 */
	Long countPresence(Site site, Tool tool, Long itemId);

	/**
	 * Get the presence for the coordinate (site, tool, item). Each user shows up at most once. Sorted by user display name. Ignore any that have expired presence.
	 * 
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 * @param itemId
	 *        The item ID.
	 * @return The list of users currently present in the site. May be empty.
	 */
	List<User> getPresence(Site site, Tool tool, Long itemId);

	/**
	 * Register presence for this user in this site.
	 * 
	 * @param user
	 *        The user.
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 * @param itemId
	 *        The item ID.
	 */
	void registerPresence(User user, Site site, Tool tool, Long itemId);

	/**
	 * Track a user visit to Etudes, setting the dates and incrementing the count.
	 * 
	 * @param user
	 *        The user.
	 */
	void track(User user);

	/**
	 * Track a user visit to a site, setting the dates and incrementing the count.
	 * 
	 * @param user
	 *        The user.
	 * @param site
	 *        The site.
	 */
	void track(User user, Site site);
}
