/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/StandardImpl.java $
 * $Id: StandardImpl.java 11392 2015-07-28 21:20:59Z ggolden $
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
import java.util.List;
import java.util.Map;

import org.etudes.cdp.api.CdpService;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.Level;
import org.etudes.evaluation.api.Standard;
import org.etudes.service.api.Services;

class StandardImpl implements Standard
{
	protected boolean changed = false;

	protected String description = null;

	protected Long id = null;

	protected Level level = null;

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof StandardImpl)) return false;
		StandardImpl other = (StandardImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public String getDescription()
	{
		return this.description;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Level getLevel()
	{
		return this.level;
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
		return this.changed;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters, List<Level> scale)
	{
		setLevel(evaluationService().levelFindByNumber(cdpService().readInt(parameters.get(prefix + "level")), scale));
		setDescription(cdpService().readString(parameters.get(prefix + "description")));
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());
		rv.put("level", getLevel().getNumber());
		rv.put("description", getDescription());

		return rv;
	}

	@Override
	public void setDescription(String description)
	{
		if (different(description, this.description))
		{
			this.description = description;
			this.changed = true;
		}
	}

	@Override
	public void setLevel(Level level)
	{
		if (different(level, this.level))
		{
			this.level = level;
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

	protected void initLevel(Level level)
	{
		this.level = level;
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
