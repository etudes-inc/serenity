/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/CriterionImpl.java $
 * $Id: CriterionImpl.java 11392 2015-07-28 21:20:59Z ggolden $
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
import org.etudes.evaluation.api.Criterion;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.Level;
import org.etudes.evaluation.api.Standard;
import org.etudes.service.api.Services;

class CriterionImpl implements Criterion
{
	protected boolean changed = false;

	protected String description = null;

	/** The criterion id. */
	protected Long id = null;

	protected boolean loaded = false;

	protected List<Standard> origStandards = new ArrayList<Standard>();

	protected Integer scorePct = null;

	protected List<Standard> standards = new ArrayList<Standard>();

	protected String title = null;

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof CriterionImpl)) return false;
		CriterionImpl other = (CriterionImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public String getDescription()
	{
		load();
		return this.description;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Integer getScorePct()
	{
		load();
		return this.scorePct;
	}

	@Override
	public Standard getStandard(Long id)
	{
		load();
		for (Standard s : this.standards)
		{
			if (s.getId().equals(id)) return s;
		}

		return null;
	}

	@Override
	public List<Standard> getStandards()
	{
		load();
		return this.standards;
	}

	@Override
	public String getTitle()
	{
		load();
		return this.title;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean isChanged()
	{
		if (!loaded) return false;

		if (this.changed) return true;

		// if there was a change in the standards list
		if (this.standards.size() != this.origStandards.size()) return true;

		// if any standards have changed
		for (Standard s : this.standards)
		{
			if (s.isChanged()) return true;
		}

		return false;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters, List<Level> scale)
	{
		setTitle(cdpService().readString(parameters.get(prefix + "criteria_title")));
		setDescription(cdpService().readString(parameters.get(prefix + "criteria_description")));
		setScorePct(cdpService().readInt(parameters.get(prefix + "criteria_scorePct")));

		// TODO: standards
		List<Long> added = cdpService().readIds(parameters.get(prefix + "criteria_added_standards"));
		// List<Long> removed = cdpService().readIds(parameters.get(prefix + "removed_standards")); TODO: ??? do we need this?
		List<Standard> standards = new ArrayList<Standard>();
		int numStandards = cdpService().readInt(parameters.get(prefix + "criteria_count_standards"));
		for (int index = 0; index < numStandards; index++)
		{
			Long id = cdpService().readLong(parameters.get(prefix + "criteria_" + index + "_standards_id"));

			Standard s = null;
			// if added
			if ((added != null) && added.contains(id))
			{
				s = new StandardImpl();
			}
			else
			{
				s = getStandard(id);
			}

			s.read(prefix + "criteria_" + index + "_standards_", parameters, scale);

			standards.add(s);
		}

		// the new standards
		setStandards(standards);
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());
		rv.put("title", getTitle());
		rv.put("description", getDescription());
		rv.put("scorePct", getScorePct());

		List<Map<String, Object>> standardsList = new ArrayList<Map<String, Object>>();
		rv.put("standards", standardsList);

		for (Standard s : getStandards())
		{
			standardsList.add(s.send());
		}

		return rv;
	}

	@Override
	public void setDescription(String description)
	{
		load();
		if (different(description, this.description))
		{
			this.description = description;
			this.changed = true;
		}
	}

	@Override
	public void setScorePct(Integer pct)
	{
		load();
		if (different(pct, this.scorePct))
		{
			this.scorePct = pct;
			this.changed = true;
		}
	}

	@Override
	public void setStandards(List<Standard> standards)
	{
		load();
		this.standards = standards;
	}

	@Override
	public void setTitle(String title)
	{
		load();
		if (different(title, this.title))
		{
			this.title = title;
			this.changed = true;
		}
	}

	/**
	 * Mark as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;

		for (Standard s : this.standards)
		{
			((StandardImpl) s).clearChanged();
		}

		this.origStandards.clear();
		this.origStandards.addAll(this.standards);
	}

	protected void initDescription(String description)
	{
		this.description = description;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initScorePct(Integer scorePct)
	{
		this.scorePct = scorePct;
	}

	protected void initStandards(List<Standard> standards)
	{
		this.standards.clear();
		this.origStandards.clear();

		if (standards != null)
		{
			this.standards.addAll(standards);
			this.origStandards.addAll(standards);
		}
	}

	protected void initTitle(String title)
	{
		this.title = title;
	}

	/**
	 * If not fully loaded, load.
	 */
	protected void load()
	{
		if (this.loaded) return;
		if (this.id == null) return;

		evaluationService().criterionRefresh(this);
	}

	/**
	 * Set that the full information has been loaded.
	 */
	protected void setLoaded()
	{
		this.loaded = true;
	}

	protected List<Long> standardsToRemove()
	{
		List<Long> rv = new ArrayList<Long>();

		// find any level in the original not now in the scale
		for (Standard orig : this.origStandards)
		{
			boolean foundInCurrent = false;
			for (Standard curr : this.standards)
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
