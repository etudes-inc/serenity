/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/EvaluationDesign.java $
 * $Id: EvaluationDesign.java 10972 2015-05-28 02:30:59Z ggolden $
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

import org.etudes.tool.api.ToolItemReference;

public interface EvaluationDesign
{
	/**
	 * Clear the changed flag.
	 */
	void clearChanged();

	/**
	 * @return The actual category (may be null).
	 */
	Category getActualCategory();

	/**
	 * @return The actual point value (may be null).
	 */
	Float getActualPoints();

	/**
	 * @return TRUE if the evaluation is automatically released, FALSE for manual release.
	 */
	Boolean getAutoRelease();

	/**
	 * @return The evaluation (grading) category for this item. If not set, returns a default category.
	 */
	Category getCategory();

	/**
	 * @return The item's position in the category, or null if not set.
	 */
	Integer getCategoryPosition();

	/**
	 * @return TRUE if the evaluation is for a grade, FALSE if not.
	 */
	Boolean getForGrade();

	/**
	 * @return The design id.
	 */
	Long getId();

	/**
	 * @return The evaluation's max points, or 0 if not for points.
	 */
	Float getPoints();

	/**
	 * @return The tool item this is for. TODO: really? it's a back ref, needed?
	 */
	ToolItemReference getRef();

	/**
	 * @return the rubric to use, or null if there is none.
	 */
	Rubric getRubric();

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
	 * Format evaluation design for sending via CDP.
	 * 
	 * @return The map, ready to add as a "design" element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the autoRelease setting.
	 * 
	 * @param autoRelease
	 *        TRUE if the evaluation is automatically released, FALSE for manual release.
	 */
	void setAutoRelease(Boolean autoRelease);

	/**
	 * Set the evaluation category.
	 * 
	 * @param category
	 *        The category, or null to indicate no category.
	 */
	void setCategory(Category category);

	/**
	 * Set the evaluation category item position.
	 * 
	 * @param pos
	 *        The category item position, or null to indicate no position.
	 */
	void setCategoryPosition(Integer pos);

	/**
	 * Set the forGrade setting.
	 * 
	 * @param forGrade
	 *        TRUE if the evaluation is for a grade, FALSE if not.
	 */
	void setForGrade(Boolean forGrade);

	/**
	 * Set the evaluation max points.
	 * 
	 * @param points
	 *        The max points, or null to indicate not for points.
	 */
	void setPoints(Float points);

	/**
	 * Set the rubric to use in the design.
	 * 
	 * @param rubric
	 *        The rubric, or null to clear it.
	 */
	void setRubric(Rubric rubric);
}
