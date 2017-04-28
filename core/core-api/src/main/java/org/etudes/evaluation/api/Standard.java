/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/Standard.java $
 * $Id: Standard.java 11392 2015-07-28 21:20:59Z ggolden $
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

/**
 * The standard for achieving an evaluation level in a criterion in a rubric.
 */
public interface Standard
{
	/**
	 * @return The standard description (plain text).
	 */
	String getDescription();

	/**
	 * @return The standard id.
	 */
	Long getId();

	/**
	 * @return The Level this standard is in reference to.
	 */
	Level getLevel();

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
	 * @param scale
	 *        The rubric's scale, whose levels the standards refer to.
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
	 * Set the Level
	 * 
	 * @param level
	 *        The level.
	 */
	void setLevel(Level level);
}
