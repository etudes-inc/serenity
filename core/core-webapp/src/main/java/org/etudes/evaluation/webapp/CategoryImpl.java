/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/CategoryImpl.java $
 * $Id: CategoryImpl.java 11600 2015-09-14 00:29:05Z ggolden $
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
import org.etudes.entity.webapp.EntityImpl;
import org.etudes.evaluation.api.Category;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemType;

class CategoryImpl extends EntityImpl implements Category
{
	protected boolean changed = false;
	protected boolean loaded = false;
	protected Integer order = new Integer(0);
	protected Site site = null;
	protected String title = null;
	protected Integer toDrop = Integer.valueOf(0);
	protected ToolItemType type = ToolItemType.none;

	@Override
	public Integer getNumberLowestToDrop()
	{
		load();
		return this.toDrop;
	}

	@Override
	public Integer getOrder()
	{
		return this.order;
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
	public ToolItemType getType()
	{
		load();
		return this.type;
	}

	@Override
	public boolean isChanged()
	{
		if (!this.loaded) return false;

		if (this.changed) return true;

		return false;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		String title = cdpService().readString(parameters.get(prefix + "title"));
		Integer order = cdpService().readInt(parameters.get(prefix + "order"));
		Integer drop = cdpService().readInt(parameters.get(prefix + "drop"));
		Integer typeId = cdpService().readInt(parameters.get(prefix + "type"));

		if (title != null) setTitle(title);
		if (order != null) setOrder(order);
		if (drop != null) setNumberLowestToDrop(drop);
		if (typeId != null) setType(ToolItemType.valueOf(typeId));
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());
		rv.put("title", getTitle());
		rv.put("type", getType().getId());
		rv.put("order", getOrder());
		rv.put("drop", getNumberLowestToDrop());
		rv.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) rv.put("createdOn", cdpService().sendDate(getCreatedOn()));
		rv.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) rv.put("modifiedOn", cdpService().sendDate(getModifiedOn()));
		rv.put("equalDistribution", Boolean.FALSE); // 0 - not equal distribution, 1 - equal distribution,
		// weight
		return rv;
	}

	@Override
	public void setNumberLowestToDrop(Integer value)
	{
		load();
		if (different(value, this.toDrop))
		{
			this.toDrop = value;
			this.changed = true;
		}
	}

	@Override
	public void setOrder(Integer order)
	{
		load();
		if (different(order, this.order))
		{
			this.changed = true;
			this.order = order;
		}
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

	@Override
	public void setType(ToolItemType type)
	{
		load();
		if (different(type, this.type))
		{
			this.type = type;
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

	protected void initDrop(Integer drop)
	{
		this.toDrop = drop;
	}

	protected void initOrder(Integer order)
	{
		this.order = order;
	}

	protected void initSite(Site site)
	{
		this.site = site;
	}

	protected void initTitle(String title)
	{
		this.title = title;
	}

	protected void initType(ToolItemType type)
	{
		this.type = type;
	}

	/**
	 * @return true if loaded, false if not.
	 */
	protected boolean isLoaded()
	{
		return this.loaded;
	}

	/**
	 * If not fully loaded, load.
	 */
	protected void load()
	{
		if (this.loaded) return;
		if (this.id == null) return;

		evaluationService().categoryRefresh(this);
	}

	/**
	 * Set that the full assessment information has been loaded.
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
