/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/LevelImpl.java $
 * $Id: LevelImpl.java 11392 2015-07-28 21:20:59Z ggolden $
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
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.Level;
import org.etudes.service.api.Services;

class LevelImpl implements Level
{
	protected boolean changed = false;

	protected String description = null; // TODO: remove description

	protected Long id = null;

	protected boolean loaded = false;
	protected Integer number = null;

	protected String title = null;

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof LevelImpl)) return false;
		LevelImpl other = (LevelImpl) obj;
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
	public Integer getNumber()
	{
		load();
		return this.number;
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

		return this.changed;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		setTitle(cdpService().readString(parameters.get(prefix + "scale_title")));
		// setDescription(cdpService().readString(parameters.get(prefix + "scale_description")));
		setNumber(cdpService().readInt(parameters.get(prefix + "scale_number")));
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());
		rv.put("number", getNumber());
		rv.put("title", getTitle());
		rv.put("description", getDescription());

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
	public void setNumber(Integer number)
	{
		load();
		if (different(number, this.number))
		{
			this.number = number;
			this.changed = true;
		}
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
	}

	protected void initDescription(String description)
	{
		this.description = description;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initNumber(Integer number)
	{
		this.number = number;
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

		evaluationService().levelRefresh(this);
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
