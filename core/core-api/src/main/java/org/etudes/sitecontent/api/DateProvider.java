/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/DateProvider.java $
 * $Id: DateProvider.java 10509 2015-04-17 21:50:49Z ggolden $
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

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * DateProvider is code that holds site related content with dates.
 */
public interface DateProvider
{
	/**
	 * Adjust all item dates (excluding created / modified) by this factor.
	 * 
	 * @param site
	 *        The site.
	 * @param days
	 *        The number of days to adjust each date (positive or negative).
	 * @param adjustingUser
	 *        The user doing the adjusting.
	 */
	void adjustDatesByDays(Site site, int days, User adjustingUser);

	/**
	 * Figure the date range for content in the site - the earliest and latest for all dates other than created / modified.
	 * 
	 * @param site
	 *        The site.
	 * @return The date range, or null if there are no dates.
	 */
	DateRange getDateRange(Site site);
}
