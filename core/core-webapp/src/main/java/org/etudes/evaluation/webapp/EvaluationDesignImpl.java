/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/EvaluationDesignImpl.java $
 * $Id: EvaluationDesignImpl.java 11954 2015-10-29 23:30:28Z ggolden $
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

import java.util.HashMap;
import java.util.Map;

import org.etudes.cdp.api.CdpService;
import org.etudes.evaluation.api.Category;
import org.etudes.evaluation.api.EvaluationDesign;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.Rubric;
import org.etudes.service.api.Services;
import org.etudes.tool.api.ToolItemReference;

class EvaluationDesignImpl implements EvaluationDesign
{
	protected Boolean autoRelease = Boolean.FALSE;
	protected Category category = null;
	protected Integer categoryPos = null;
	protected boolean changed = false;
	protected Category defaultCategory = null;
	protected Boolean forGrade = Boolean.TRUE;
	protected Long id = null;
	protected Float points = null;
	protected ToolItemReference ref = null;
	protected Rubric rubric = null;

	@Override
	public void clearChanged()
	{
		this.changed = false;

		if (this.rubric != null) ((RubricImpl) this.rubric).clearChanged();
	}

	@Override
	public Category getActualCategory()
	{
		return this.category;
	}

	@Override
	public Float getActualPoints()
	{
		return this.points;
	}

	@Override
	public Boolean getAutoRelease()
	{
		return this.autoRelease;
	}

	@Override
	public Category getCategory()
	{
		return this.category == null ? this.defaultCategory : this.category;
	}

	@Override
	public Integer getCategoryPosition()
	{
		return this.categoryPos;
	}

	@Override
	public Boolean getForGrade()
	{
		return this.forGrade;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Float getPoints()
	{
		if (this.points == null) return Float.valueOf(0f);
		return this.points;
	}

	@Override
	public ToolItemReference getRef()
	{
		return this.ref;
	}

	@Override
	public Rubric getRubric()
	{
		return this.rubric;
	}

	@Override
	public boolean isChanged()
	{
		if (this.changed) return true;
		if ((this.rubric != null) && (this.rubric.isChanged())) return true;

		return false;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		setAutoRelease(cdpService().readBoolean(parameters.get(prefix + "autoRelease")));
		setForGrade(cdpService().readBoolean(parameters.get(prefix + "forGrade")));
		setPoints(cdpService().readFloat(parameters.get(prefix + "actualPoints")));
		setRubric(evaluationService().rubricWrap(cdpService().readLong(parameters.get(prefix + "rubricSelected"))));
		setCategory(evaluationService().categoryWrap(cdpService().readLong(parameters.get(prefix + "actualCategory"))));
		setCategoryPosition(cdpService().readInt(parameters.get(prefix + "categoryPosition")));
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (this.rubric != null)
		{
			rv.put("rubricSelected", this.rubric.getId());
			// rv.put("rubric", rubric.send());
		}
		else
		{
			rv.put("rubricSelected", Long.valueOf(0L));
		}

		rv.put("autoRelease", getAutoRelease());
		rv.put("forGrade", getForGrade());
		rv.put("points", getPoints());
		if (getActualPoints() != null) rv.put("actualPoints", getActualPoints());
		rv.put("actualCategory", (getActualCategory() == null) ? Integer.valueOf(0) : getActualCategory().getId());
		rv.put("category", (getCategory() == null) ? Integer.valueOf(0) : getCategory().getId());
		if (getCategoryPosition() != null) rv.put("categoryPosition", getCategoryPosition());

		return rv;
	}

	@Override
	public void setAutoRelease(Boolean autoRelease)
	{
		if (autoRelease == null) autoRelease = Boolean.FALSE;
		if (different(autoRelease, this.autoRelease))
		{
			this.changed = true;
			this.autoRelease = autoRelease;
		}
	}

	@Override
	public void setCategory(Category category)
	{
		if (different(category, this.category))
		{
			this.category = category;
			this.defaultCategory = null;
			this.changed = true;
		}
	}

	@Override
	public void setCategoryPosition(Integer pos)
	{
		if (different(pos, this.categoryPos))
		{
			this.categoryPos = pos;
			this.changed = true;
		}
	}

	@Override
	public void setForGrade(Boolean forGrade)
	{
		if (forGrade == null) forGrade = Boolean.FALSE;
		if (different(forGrade, this.forGrade))
		{
			this.changed = true;
			this.forGrade = forGrade;
		}
	}

	@Override
	public void setPoints(Float points)
	{
		if (different(points, this.points))
		{
			this.changed = true;
			this.points = points;
		}
	}

	@Override
	public void setRubric(Rubric rubric)
	{
		if (different(rubric, this.rubric))
		{
			this.rubric = rubric;
			this.changed = true;
		}
	}

	protected Category getDefaultCategory()
	{
		return this.defaultCategory;
	}

	protected void initAutoRelease(Boolean autoRelease)
	{
		if (autoRelease == null) autoRelease = Boolean.FALSE;
		this.autoRelease = autoRelease;
	}

	protected void initCategory(Category category)
	{
		this.category = category;
	}

	protected void initCategoryPos(Integer pos)
	{
		this.categoryPos = pos;
	}

	protected void initDefaultCategory(Category category)
	{
		this.defaultCategory = category;
	}

	protected void initForGrade(Boolean forGrade)
	{
		if (forGrade == null) forGrade = Boolean.FALSE;
		this.forGrade = forGrade;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initPoints(Float points)
	{
		this.points = points;
	}

	protected void initRef(ToolItemReference ref)
	{
		this.ref = ref;
	}

	protected void initRubric(Rubric rubric)
	{
		this.rubric = rubric;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered EvaluationService.
	 */
	private EvaluationService evaluationService()
	{
		return (EvaluationService) Services.get(EvaluationService.class);
	}
}
