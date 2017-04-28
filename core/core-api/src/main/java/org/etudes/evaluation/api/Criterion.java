/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/Criterion.java $
 * $Id: Criterion.java 11392 2015-07-28 21:20:59Z ggolden $
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

package org.etudes.evaluation.api;

import java.util.List;
import java.util.Map;

public interface Criterion
{
	/**
	 * @return The criterion description.
	 */
	String getDescription();

	/**
	 * @return The criterion id.
	 */
	Long getId();

	/**
	 * @return The % of total score this criterion contributes.
	 */
	Integer getScorePct();

	/**
	 * Find the standard with this id.
	 * 
	 * @param id
	 *        The standard id.
	 * @return The Standard with this id, or null if not found.
	 */
	Standard getStandard(Long id);

	/**
	 * @return The Standards for the rubric's scale levels for this criterion.
	 */
	List<Standard> getStandards();

	/**
	 * @return The criterion title.
	 */
	String getTitle();

	/**
	 * @return true if any changes were made, false if not.
	 */
	boolean isChanged();

	/**
	 * Update from CDP parameters.
	 * 
	 * @param prefix
	 *        The parameter names prefix.
	 * @param parameters
	 *        The parameters.
	 * @param scale
	 *        The rubric's scale, whose levels the criterion standards refer to.
	 */
	void read(String prefix, Map<String, Object> parameters, List<Level> scale);

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the description.
	 * 
	 * @param description
	 *        The description (plain text, 2k limit).
	 */
	void setDescription(String description);

	/**
	 * Set the scorePct.
	 * 
	 * @param pct
	 *        The % of total score this criterion contributes.
	 */
	void setScorePct(Integer pct);

	/**
	 * Set the standards.
	 * 
	 * @param standards
	 *        The Standards for the rubric's scale levels for this criterion.
	 */
	void setStandards(List<Standard> standards);

	/**
	 * Set the title.
	 * 
	 * @param title
	 *        The title (plan text, 255 character limit).
	 */
	void setTitle(String title);
}
