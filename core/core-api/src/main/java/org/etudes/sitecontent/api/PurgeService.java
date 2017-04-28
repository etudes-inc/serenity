/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/PurgeService.java $
 * $Id: PurgeService.java 10165 2015-02-26 23:24:48Z ggolden $
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
 * The Purge service, responsible for purging sites and their content.
 */
public interface PurgeService
{
	/**
	 * Remove all non-instructor users and their contributions in the site - Instructors and their content remain.
	 * 
	 * @param site
	 *        The site.
	 */
	void clear(Site site);

	/**
	 * Purge the site and all of its content.
	 * 
	 * @param site
	 *        The site.
	 */
	void purge(Site site);

	/**
	 * Remove this users and contributions.
	 * 
	 * @param user
	 *        The user.
	 */
	void purge(User user);
}
