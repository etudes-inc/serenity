/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/GradeProvider.java $
 * $Id: GradeProvider.java 10871 2015-05-18 02:21:08Z ggolden $
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

import org.etudes.evaluation.api.GradingItem;
import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemReference;

/**
 * GradeProvider is code that holds site related content with student submissions which are evaluated for part of the class grade.
 */
public interface GradeProvider
{
	/**
	 * Get one item for grading
	 * 
	 * @param item
	 *        The item reference.
	 * @return The grading item, or null if the item is not found.
	 */
	GradingItem getGradingItem(ToolItemReference item);

	/**
	 * Get the items for grading.
	 * 
	 * @param site
	 *        The site.
	 * @return The list of GradingItem, may be empty.
	 */
	List<GradingItem> getGradingItems(Site site);
}
