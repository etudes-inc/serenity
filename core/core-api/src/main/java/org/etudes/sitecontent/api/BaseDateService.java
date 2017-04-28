/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/BaseDateService.java $
 * $Id: BaseDateService.java 10143 2015-02-25 03:37:59Z ggolden $
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

import java.util.Date;
import java.util.List;

import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * The Purge service, responsible for purging sites and their content.
 */
public interface BaseDateService
{
	/**
	 * Compute the date offset by days.
	 * 
	 * @param date
	 *        The starting date.
	 * @param days
	 *        The days offset (positive or negative).
	 * @return The adjusted date.
	 */
	Date adjustDateByDays(Date date, int days);

	/**
	 * Adjust all content items in a site shifting the dates (other than created / modified) by this number of days.
	 * 
	 * @param site
	 *        The site.
	 * @param days
	 *        The number of days (positive or negative).
	 * @param adjustingUser
	 *        The user doing the adjusting.
	 */
	void adjustDatesByDays(Site site, int days, User adjustingUser);

	/**
	 * Find the base date (the minimum date) from a set of ranges (such as is returned from getRanges()).
	 * 
	 * @param ranges
	 *        The list of ranges.
	 * @return The base date.
	 */
	Date computeBaseDate(List<DateRange> ranges);

	/**
	 * Compute the offset in days between the two dates - positive if d2 > d1, negative if d1 > d2.
	 * 
	 * @param d1
	 *        One Date.
	 * @param d2
	 *        The other Date.
	 * @return The difference between the dates in days.
	 */
	int computeDayDifference(Date d1, Date d2);

	/**
	 * Adjust min and max (in the minMax parameter) with this candidate date - extending either in their natural direction if the candidate is the new min or max.
	 * 
	 * @param min
	 *        The current min (may be null).
	 * @param max
	 *        The current max (may be null).
	 * @param candidate
	 *        The candidate date (may be null).
	 */
	void computeMinMax(Date[] minMax, Date candidate);

	/**
	 * Find the date ranges for all tools in the site that have dates in items.
	 * 
	 * @param site
	 *        The site.
	 * @return The list of DateRanges found.
	 */
	List<DateRange> getRanges(Site site);

	/**
	 * Create a DateRange.
	 * 
	 * @param tool
	 *        The tool.
	 * @param dates
	 *        The dates (min, max).
	 * @return The DateRange.
	 */
	DateRange newDateRange(Tool tool, Date[] dates);

}
