/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/tool/api/ToolItemReference.java $
 * $Id: ToolItemReference.java 11832 2015-10-13 03:35:37Z ggolden $
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

package org.etudes.tool.api;

import static org.etudes.util.Different.different;

import java.util.HashMap;
import java.util.Map;

import org.etudes.site.api.Site;

/**
 * ToolItemReference is a reference to some item defined in some tool in some site. Used to point at items without details about the item, such as for file references for files used by a tool item, and for grade or course map items pointing at their tool
 * item, of for evaluation design pointing at the tool item to which the design belongs.
 */
public class ToolItemReference
{
	protected Long itemId = null;
	protected Site site = null;
	protected Tool tool = null;

	public ToolItemReference(Site site, Tool tool, Long item)
	{
		this.site = site;
		this.tool = tool;
		this.itemId = item;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ToolItemReference)) return false;
		ToolItemReference other = (ToolItemReference) obj;
		if (different(site, other.site)) return false;
		if (different(tool, other.tool)) return false;
		if (different(itemId, other.itemId)) return false;
		return true;
	}

	/**
	 * @return The id of the item within the tool.
	 */
	public Long getItemId()
	{
		return this.itemId;
	}

	/**
	 * @return the site.
	 */
	public Site getSite()
	{
		return this.site;
	}

	/**
	 * @return The tool id of.
	 */
	public Tool getTool()
	{
		return this.tool;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + site.hashCode() + tool.hashCode() + itemId.hashCode();
		return result;
	}

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("site", getSite().sendIdTitleRole(null));
		rv.put("tool", getTool().getId());  //getTool().send());
		rv.put("itemId", getItemId());

		return rv;
	}
}
