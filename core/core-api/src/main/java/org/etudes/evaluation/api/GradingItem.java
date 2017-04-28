/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/GradingItem.java $
 * $Id: GradingItem.java 11796 2015-10-08 20:27:22Z ggolden $
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

import static org.etudes.util.Different.different;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.etudes.entity.api.Schedule;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemType;

public class GradingItem
{
	/** The item's evaluation design. */
	protected EvaluationDesign design = null;

	/** Best evaluations per user of submissions to this item */
	protected List<Evaluation> evaluations = new ArrayList<Evaluation>();

	/** Reference to the tool item. */
	protected ToolItemReference item = null;

	/** The item's open / due / allow until schedule. */
	protected Schedule schedule = null;

	/** The item's title. */
	protected String title = null;

	/** The item's type. */
	protected ToolItemType type = null;

	public GradingItem(ToolItemReference item, ToolItemType type, String title, Schedule schedule, EvaluationDesign design)
	{
		this.item = item;
		this.type = type;
		this.title = title;
		this.schedule = schedule;
		this.design = design;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof GradingItem)) return false;
		GradingItem other = (GradingItem) obj;
		if (different(item, other.item)) return false;
		return true;
	}

	/**
	 * @return The item's evaluation design.
	 */
	public EvaluationDesign getDesign()
	{
		return this.design;
	}

	/**
	 * @return The best evaluations, one for each qualified user.
	 */
	public List<Evaluation> getEvaluations()
	{
		return this.evaluations;
	}

	/**
	 * @return A reference to the item being graded.
	 */
	public ToolItemReference getItemReference()
	{
		return this.item;
	}

	/**
	 * @return The item's schedule.
	 */
	public Schedule getSchedule()
	{
		return this.schedule;
	}

	/**
	 * @return The item's type.
	 */
	public ToolItemType getType()
	{
		return this.type;
	}

	/**
	 * @return The item's title.
	 */
	public String getTitle()
	{
		return this.title;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + item.hashCode();
		return result;
	}

	/**
	 * Format evaluation for sending via CDP.
	 * 
	 * @return The map, ready to add as an "evaluation" element to the return map.
	 */
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// referenced tool item
		rv.put("toolItem", this.item.send());

		// title
		rv.put("title", this.title);

		// type
		rv.put("type", this.type.getId()); // this.type.send());

		// schedule
		rv.put("schedule", this.schedule.send());

		// evaluation design
		rv.put("design", this.design.send());

		// evaluation summary - average score for the collected evaluations, count of that average
		double totalScore = 0f;
		for (Evaluation e : this.evaluations)
		{
			totalScore += e.getScore().floatValue();
		}
		Map<String, Object> summaryMap = new HashMap<String, Object>();
		rv.put("summary", summaryMap);
		summaryMap.put("avgScore", Float.valueOf((float) (Math.floor((totalScore / (double) this.evaluations.size()) * 100d) / 100d)));
		summaryMap.put("avgCount", Integer.valueOf(this.evaluations.size()));

		return rv;
	}
}
