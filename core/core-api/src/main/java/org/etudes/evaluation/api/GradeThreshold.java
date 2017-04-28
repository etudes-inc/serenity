/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/GradeThreshold.java $
 * $Id: GradeThreshold.java 11826 2015-10-12 03:31:58Z ggolden $
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

import java.util.HashMap;
import java.util.Map;

import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;

public class GradeThreshold
{
	protected String grade = null;
	protected float threshold = 0f;

	public GradeThreshold(String grade, Integer threshold100)
	{
		this.grade = grade;
		setThreshold(threshold100);
	}

	/**
	 * @return The grade.
	 */
	public String getGrade()
	{
		return this.grade;
	}

	/**
	 * @return The threshold percent (float 0..1) for this grade.
	 */
	public Float getThreshold()
	{
		return this.threshold;
	}

	/**
	 * @return The threshold percent (float 0 .. 100, 2 decimal places) for this grade.
	 */
	public Float getThresholdPct()
	{
		return Float.valueOf((float) (Math.floor(((double) this.threshold) * 10000.0d) / 100d));
	}

	/**
	 * If the score against points exceeds this threshold, return the grade, else return null.
	 * 
	 * @param score
	 *        The score.
	 * @param points
	 *        The points.
	 * @return The grade, if the score against points exceeds this threshold, else null.
	 */
	public String grade(Float score, Float points)
	{
		if ((score / points) >= this.threshold) return this.grade;
		return null;
	}

	/**
	 * Update from CDP parameters.
	 * 
	 * @param prefix
	 *        The parameter names prefix.
	 * @param parameters
	 *        The parameters.
	 */
	public void read(String prefix, Map<String, Object> parameters)
	{
		setThreshold(cdpService().readInt(parameters.get(prefix + "threshold100")));
	}

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("grade", getGrade());
		rv.put("threshold", getThreshold());
		rv.put("thresholdPct", getThresholdPct());

		return rv;
	}

	/**
	 * Set a new threshold level for this grade.
	 * 
	 * @param threshold100
	 *        (int 0 .. 100) for this grade.
	 */
	public void setThreshold(Integer threshold100)
	{
		this.threshold = Float.valueOf(threshold100.floatValue() / 100f);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}
