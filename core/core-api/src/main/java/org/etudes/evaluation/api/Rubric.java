/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/Rubric.java $
 * $Id: Rubric.java 11389 2015-07-27 04:32:19Z ggolden $
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

import org.etudes.entity.api.Entity;
import org.etudes.site.api.Site;

public interface Rubric extends Entity
{
	/**
	 * @return The List of criteria.
	 */
	List<Criterion> getCriteria();

	/**
	 * Find the criterion with this id.
	 * 
	 * @param id
	 *        The criterion id.
	 * @return The criterion with this id, or null if not found.
	 */
	Criterion getCriterion(Long id);

	/**
	 * @return The levels that make up the rating scale for the rubric.
	 */
	List<Level> getScale();

	/**
	 * Find the level in the scale with this id.
	 * 
	 * @param id
	 *        The scale level id.
	 * @return The level if found, null if not.
	 */
	Level getScaleLevel(Long id);

	/**
	 * @return the Site in which this rubric resides.
	 */
	Site getSite();

	/**
	 * @return The rubric title.
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
	 */
	void read(String prefix, Map<String, Object> parameters);

	/**
	 * Format rubric for sending via CDP.
	 * 
	 * @return The map, ready to add as a "rubric" element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the criteria list.
	 * 
	 * @param criteria
	 *        The criteria for the design.
	 */
	void setCriteria(List<Criterion> criteria);

	/**
	 * Set the rubric's scale.
	 * 
	 * @param scale
	 *        The scale levels.
	 */
	void setScale(List<Level> scale);

	/**
	 * Set the title.
	 * 
	 * @param title
	 *        The title.
	 */
	void setTitle(String title);
}
