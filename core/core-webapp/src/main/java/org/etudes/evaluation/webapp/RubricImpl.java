/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/RubricImpl.java $
 * $Id: RubricImpl.java 11392 2015-07-28 21:20:59Z ggolden $
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
import org.etudes.entity.webapp.EntityImpl;
import org.etudes.evaluation.api.Criterion;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.Level;
import org.etudes.evaluation.api.Rubric;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;

class RubricImpl extends EntityImpl implements Rubric
{
	protected boolean changed = false;
	protected List<Criterion> criteria = new ArrayList<Criterion>();
	protected boolean loaded = false;
	protected List<Criterion> origCriteria = new ArrayList<Criterion>();
	protected List<Level> origScale = new ArrayList<Level>();
	protected List<Level> scale = new ArrayList<Level>();

	protected Site site = null;

	protected String title = null;

	@Override
	public List<Criterion> getCriteria()
	{
		load();
		return this.criteria;
	}

	@Override
	public Criterion getCriterion(Long id)
	{
		load();
		for (Criterion c : this.criteria)
		{
			if (c.getId().equals(id)) return c;
		}

		return null;
	}

	@Override
	public List<Level> getScale()
	{
		load();
		return this.scale;
	}

	@Override
	public Level getScaleLevel(Long id)
	{
		for (Level l : this.scale)
		{
			if (l.getId().equals(id)) return l;
		}
		return null;
	}

	@Override
	public Site getSite()
	{
		load();
		return this.site;
	}

	@Override
	public String getTitle()
	{
		load();
		return this.title;
	}

	@Override
	public boolean isChanged()
	{
		if (!loaded) return false;

		if (this.changed) return true;

		// if any scale level has changed
		for (Level l : this.scale)
		{
			if (l.isChanged()) return true;
		}

		// if there was a change in the scale level list
		if (this.scale.size() != this.origScale.size()) return true;

		// if any criteria have changed
		for (Criterion c : this.criteria)
		{
			if (c.isChanged()) return true;
		}

		// if there has been any change to the criteria list
		if (this.criteria.size() != this.origCriteria.size()) return true;

		return false;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		setTitle(cdpService().readString(parameters.get(prefix + "title")));

		// scale levels added and removed
		List<Long> levelsAdded = cdpService().readIds(parameters.get(prefix + "added_scale"));
		List<Level> newScale = new ArrayList<Level>();

		int numLevels = cdpService().readInt(parameters.get(prefix + "count_scale"));
		for (int levelIndex = 0; levelIndex < numLevels; levelIndex++)
		{
			Long levelId = cdpService().readLong(parameters.get(prefix + levelIndex + "_scale_id"));

			Level l = null;

			// if added
			if ((levelId < 0) || (levelsAdded != null) && levelsAdded.contains(levelId))
			{
				l = new LevelImpl();
			}
			else
			{
				l = getScaleLevel(levelId);
			}

			l.read(prefix + levelIndex + "_", parameters);

			newScale.add(l);
		}

		// the new scale
		setScale(newScale);

		List<Long> added = cdpService().readIds(parameters.get(prefix + "added_criteria"));
		// List<Long> removed = cdpService().readIds(parameters.get(prefix + "removed_criteria")); TODO: ??? do we need this?
		List<Criterion> order = new ArrayList<Criterion>();
		int numCriteria = cdpService().readInt(parameters.get(prefix + "count_criteria"));
		for (int index = 0; index < numCriteria; index++)
		{
			Long id = cdpService().readLong(parameters.get(prefix + index + "_criteria_id"));

			Criterion c = null;
			// if added
			if ((added != null) && added.contains(id))
			{
				c = new CriterionImpl();
				((CriterionImpl) c).setLoaded();
			}
			else
			{
				c = getCriterion(id);
			}

			c.read(prefix + index + "_", parameters, newScale);

			order.add(c);
		}

		// the new criteria
		setCriteria(order);
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		List<Map<String, Object>> levelList = new ArrayList<Map<String, Object>>();
		rv.put("scale", levelList);
		for (Level level : getScale())
		{
			levelList.add(level.send());
		}

		List<Map<String, Object>> criteriaList = new ArrayList<Map<String, Object>>();
		rv.put("criteria", criteriaList);

		int order = 1;
		for (Criterion criterion : getCriteria())
		{
			Map<String, Object> criterionMap = criterion.send();
			criteriaList.add(criterionMap);

			// add the criterion's order within the rubric
			criterionMap.put("order", Integer.valueOf(order++));
		}

		rv.put("id", getId());
		rv.put("title", getTitle());
		rv.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) rv.put("createdOn", cdpService().sendDate(getCreatedOn()));
		rv.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) rv.put("modifiedOn", cdpService().sendDate(getModifiedOn()));

		return rv;
	}

	@Override
	public void setCriteria(List<Criterion> criteria)
	{
		load();
		this.criteria = criteria;
	}

	@Override
	public void setScale(List<Level> scale)
	{
		load();
		this.scale = scale;
	}

	@Override
	public void setTitle(String title)
	{
		load();
		if (different(title, this.title))
		{
			this.changed = true;
			this.title = title;
		}
	}

	/**
	 * Clear the changed flag.
	 */
	protected void clearChanged()
	{
		this.changed = false;

		for (Criterion c : this.criteria)
		{
			((CriterionImpl) c).clearChanged();
		}

		for (Level l : this.scale)
		{
			((LevelImpl) l).clearChanged();
		}
		this.origScale.clear();
		this.origScale.addAll(this.scale);

		this.origCriteria.clear();
		this.origCriteria.addAll(this.criteria);
	}

	protected List<Long> criteriaToRemove()
	{
		List<Long> rv = new ArrayList<Long>();

		// find any criterion in the original not now in criteria
		for (Criterion orig : this.origCriteria)
		{
			boolean foundInCurrent = false;
			for (Criterion curr : this.criteria)
			{
				if (!different(curr.getId(), orig.getId()))
				{
					foundInCurrent = true;
					break;
				}
			}
			if (!foundInCurrent) rv.add(orig.getId());
		}

		return rv;
	}

	protected void initCriteria(List<Criterion> criteria)
	{
		this.criteria.clear();
		this.origCriteria.clear();

		if (criteria != null)
		{
			this.criteria.addAll(criteria);
			this.origCriteria.addAll(criteria);
		}
	}

	protected void initScale(List<Level> scale)
	{
		this.scale.clear();
		this.origScale.clear();

		if (scale != null)
		{
			this.scale.addAll(scale);
			this.origScale.addAll(scale);
		}
	}

	protected void initSite(Site site)
	{
		this.site = site;
	}

	protected void initTitle(String title)
	{
		this.title = title;
	}

	/**
	 * @return true if loaded, false if not.
	 */
	protected boolean isLoaded()
	{
		return this.loaded;
	}

	protected List<Long> levelsToRemove()
	{
		List<Long> rv = new ArrayList<Long>();

		// find any level in the original not now in the scale
		for (Level orig : this.origScale)
		{
			boolean foundInCurrent = false;
			for (Level curr : this.scale)
			{
				if (!different(curr.getId(), orig.getId()))
				{
					foundInCurrent = true;
					break;
				}
			}
			if (!foundInCurrent) rv.add(orig.getId());
		}

		return rv;
	}

	/**
	 * If not fully loaded, load.
	 */
	protected void load()
	{
		if (this.loaded) return;
		if (this.id == null) return;

		evaluationService().rubricRefresh(this);
	}

	/**
	 * Set that the full information has been loaded.
	 */
	protected void setLoaded()
	{
		this.loaded = true;
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
