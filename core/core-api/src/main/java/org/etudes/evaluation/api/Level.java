/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/Level.java $
 * $Id: Level.java 11389 2015-07-27 04:32:19Z ggolden $
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

import java.util.Map;

/**
 * One level in a rubric's rating scale.
 */
public interface Level
{
	/**
	 * @return The level description (plain text).
	 */
	String getDescription();

	/**
	 * @return The level id.
	 */
	Long getId();

	/**
	 * @return The level number, both the order (0 .. max) and the weight, % that evaluations at this level earn towards total score for criterion.
	 */
	Integer getNumber();

	/**
	 * @return The level title (plain text).
	 */
	String getTitle();

	/**
	 * @return true if it has changes.
	 */
	boolean isChanged();

	/**
	 * Update from CDP parameters.
	 * 
	 * @param prefix
	 *        The parameter names prefix.
	 * @param parameters
	 *        The parameters.
	 */
	void read(String prefix, Map<String, Object> parameters);

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
	 * Set the level number.
	 * 
	 * @param number
	 *        The level number.
	 */
	void setNumber(Integer number);

	/**
	 * Set the title.
	 * 
	 * @param title
	 *        The title (plan text, 255 character limit).
	 */
	void setTitle(String title);
}
