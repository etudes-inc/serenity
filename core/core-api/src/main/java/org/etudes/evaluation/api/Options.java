/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/Options.java $
 * $Id: Options.java 11588 2015-09-10 16:58:55Z ggolden $
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

public interface Options
{
	enum GradingScale
	{
		letters(1), lettersPM(2), pass(3), unknown(0);

		public static GradingScale fromCode(Integer id)
		{
			for (GradingScale scale : GradingScale.values())
			{
				if (scale.id.equals(id)) return scale;
			}
			return unknown;
		}

		private final Integer id;

		private GradingScale(int id)
		{
			this.id = Integer.valueOf(id);
		}

		public Integer getId()
		{
			return this.id;
		}
	}

	/**
	 * @return TRUE if we are dropping lowest grades, FALSE if not. The # to drop is set in each category.
	 */
	Boolean getDopLowestActive();

	/**
	 * @return The grading scale.
	 */
	GradingScale getGradingScale();

	/**
	 * @return The grading scale thresholds for the grading scale.
	 * @param scale
	 *        The grading scale. If null, use the selected scale.
	 */
	List<GradeThreshold> getGradingScaleThresholds(GradingScale scale);

	/**
	 * @return TRUE to include all gradable items in grade calculations (counting 0 for non-submit and non-release), FALSE to include only "to date" released items.
	 */
	Boolean getIncludeAllInGrade();

	/**
	 * @return TRUE to show letter grades, FALSE to not.
	 */
	Boolean getShowLetterGrades();

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
	 * Compute a grade from a score against points, using our grading scale and thresholds.
	 * 
	 * @param score
	 *        The score.
	 * @param points
	 *        The points.
	 * @return The grade.
	 */
	String scaleGradeForScore(Float score, Float points);

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the DropLowestActive option.
	 * 
	 * @param value
	 *        TRUE if we are dropping lowest grades, FALSE if not.
	 */
	void setDropLowestActive(Boolean value);

	/**
	 * Set the GradingScale
	 * 
	 * @param scale
	 *        The grading scale.
	 */
	void setGradingScale(GradingScale scale);

	/**
	 * Set the IncludeAllInGrade option
	 * 
	 * @param value
	 *        TRUE to include all gradable items in grade calculations (counting 0 for non-submit and non-release), FALSE to include only "to date" released items.
	 */
	void setIncludeAllInGrade(Boolean value);

	/**
	 * Set the ShowLetterGrades option.
	 * 
	 * @param value
	 *        TRUE to show letter grades, FALSE to not.
	 */
	void setShowLetterGrades(Boolean value);
}
