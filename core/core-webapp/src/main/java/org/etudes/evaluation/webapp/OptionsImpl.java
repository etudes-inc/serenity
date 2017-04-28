/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/OptionsImpl.java $
 * $Id: OptionsImpl.java 11807 2015-10-10 00:00:25Z ggolden $
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

package org.etudes.evaluation.webapp;

import static org.etudes.util.Different.different;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.etudes.cdp.api.CdpService;
import org.etudes.evaluation.api.GradeThreshold;
import org.etudes.evaluation.api.Options;
import org.etudes.service.api.Services;

class OptionsImpl implements Options
{
	protected boolean changed = false;
	protected Boolean dropLowestActive = Boolean.FALSE;
	protected GradingScale gradingScale = GradingScale.lettersPM;
	protected Boolean includeAll = Boolean.FALSE;
	protected Map<GradingScale, List<GradeThreshold>> scaleThresholds = new HashMap<GradingScale, List<GradeThreshold>>();
	protected Boolean showLetterGrade = Boolean.TRUE;

	public OptionsImpl()
	{
		// establish defaults TODO: ???
		List<GradeThreshold> thresholds = new ArrayList<GradeThreshold>();
		thresholds.add(new GradeThreshold("A+", 100));
		thresholds.add(new GradeThreshold("A", 95));
		thresholds.add(new GradeThreshold("A-", 90));
		thresholds.add(new GradeThreshold("B+", 87));
		thresholds.add(new GradeThreshold("B", 83));
		thresholds.add(new GradeThreshold("B-", 80));
		thresholds.add(new GradeThreshold("C+", 77));
		thresholds.add(new GradeThreshold("C", 73));
		thresholds.add(new GradeThreshold("C-", 70));
		thresholds.add(new GradeThreshold("D+", 67));
		thresholds.add(new GradeThreshold("D", 63));
		thresholds.add(new GradeThreshold("D-", 60));
		thresholds.add(new GradeThreshold("F", 0));
		thresholds.add(new GradeThreshold("I", 0));
		this.scaleThresholds.put(GradingScale.lettersPM, thresholds);

		thresholds = new ArrayList<GradeThreshold>();
		thresholds.add(new GradeThreshold("A", 90));
		thresholds.add(new GradeThreshold("B", 80));
		thresholds.add(new GradeThreshold("C", 70));
		thresholds.add(new GradeThreshold("D", 60));
		thresholds.add(new GradeThreshold("F", 0));
		thresholds.add(new GradeThreshold("I", 0));
		this.scaleThresholds.put(GradingScale.letters, thresholds);

		thresholds = new ArrayList<GradeThreshold>();
		thresholds.add(new GradeThreshold("P", 75));
		thresholds.add(new GradeThreshold("NP", 0));
		thresholds.add(new GradeThreshold("I", 0));
		this.scaleThresholds.put(GradingScale.pass, thresholds);
	}

	@Override
	public Boolean getDopLowestActive()
	{
		return this.dropLowestActive;
	}

	@Override
	public GradingScale getGradingScale()
	{
		return this.gradingScale;
	}

	@Override
	public List<GradeThreshold> getGradingScaleThresholds(GradingScale scale)
	{
		if (scale == null) scale = getGradingScale();
		return this.scaleThresholds.get(scale);
	}

	@Override
	public Boolean getIncludeAllInGrade()
	{
		return this.includeAll;
	}

	@Override
	public Boolean getShowLetterGrades()
	{
		return this.showLetterGrade;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		setDropLowestActive(cdpService().readBoolean(parameters.get(prefix + "dropLowestActive")));
		setGradingScale(GradingScale.fromCode(cdpService().readInt(parameters.get(prefix + "gradingScale"))));
		setIncludeAllInGrade(cdpService().readBoolean(parameters.get(prefix + "includeAll")));
		setShowLetterGrades(cdpService().readBoolean(parameters.get(prefix + "showLetterGrades")));

		for (GradingScale scale : GradingScale.values())
		{
			if (scale == GradingScale.unknown) continue;

			List<GradeThreshold> thresholds = getGradingScaleThresholds(scale);
			for (int i = 0; i < thresholds.size(); i++)
			{
				GradeThreshold threshold = thresholds.get(i);
				threshold.read(prefix + i + "_gradingScaleThresholds_" + scale.getId() + "_", parameters);
			}
		}
	}

	@Override
	public String scaleGradeForScore(Float score, Float points)
	{
		List<GradeThreshold> scale = getGradingScaleThresholds(null);

		// no points means no grade
		if ((points == null) | (points == 0)) return null;

		if (score == null) return "I";
		for (GradeThreshold t : scale)
		{
			String grade = t.grade(score, points);
			if (grade != null) return grade;
		}
		return "I";
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("dropLowestActive", getDopLowestActive());
		rv.put("gradingScale", getGradingScale().getId());
		rv.put("includeAll", getIncludeAllInGrade());
		rv.put("showLetterGrades", getShowLetterGrades());

		rv.put("boostBy", Float.valueOf(2)); // TODO:
		rv.put("boostType", Integer.valueOf(0)); // TODO: 0- percentage points, 1- points

		for (GradingScale scale : GradingScale.values())
		{
			if (scale == GradingScale.unknown) continue;

			List<Map<String, Object>> gst = new ArrayList<Map<String, Object>>();
			rv.put("gradingScaleThresholds_" + scale.getId(), gst);
			for (GradeThreshold gt : getGradingScaleThresholds(scale))
			{
				gst.add(gt.send());
			}
		}
		return rv;
	}

	@Override
	public void setDropLowestActive(Boolean value)
	{
		if (different(value, this.dropLowestActive))
		{
			this.dropLowestActive = value;
			this.changed = true;
		}
	}

	@Override
	public void setGradingScale(GradingScale scale)
	{
		if (different(scale, this.gradingScale))
		{
			this.gradingScale = scale;
			this.changed = true;
		}
	}

	@Override
	public void setIncludeAllInGrade(Boolean value)
	{
		if (different(value, this.includeAll))
		{
			this.includeAll = value;
			this.changed = true;
		}
	}

	@Override
	public void setShowLetterGrades(Boolean value)
	{
		if (different(value, this.showLetterGrade))
		{
			this.showLetterGrade = value;
			this.changed = true;
		}
	}

	/**
	 * Clear the changed flag.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected void initDropLowestActive(Boolean value)
	{
		this.dropLowestActive = value;
	}

	protected void initGradingScale(GradingScale value)
	{
		this.gradingScale = value;
	}

	protected void initIncludeAll(Boolean value)
	{
		this.includeAll = value;
	}

	protected void initShowLetterGrade(Boolean value)
	{
		this.showLetterGrade = value;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}
