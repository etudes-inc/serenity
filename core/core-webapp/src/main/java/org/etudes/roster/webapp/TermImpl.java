/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/roster/webapp/TermImpl.java $
 * $Id: TermImpl.java 12060 2015-11-12 03:58:14Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

package org.etudes.roster.webapp;

import static org.etudes.util.Different.different;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.roster.api.Term;

/**
 * Term implementation.
 */
public class TermImpl implements Term
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(TermImpl.class);

	protected String abbreviation = null;

	protected boolean changed = false;

	protected Long id = null;

	protected String name = null;

	/**
	 * Construct.
	 * 
	 * @param userServiceImpl
	 */
	public TermImpl()
	{
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof TermImpl)) return false;
		TermImpl other = (TermImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public String getAbbreviation()
	{
		return this.abbreviation;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public String getName()
	{
		return this.name;
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
	public Map<String, Object> send()
	{
		Map<String, Object> termMap = new HashMap<String, Object>();
		termMap.put("id", getId().toString());
		termMap.put("name", getName());
		termMap.put("abbreviation", getAbbreviation());

		return termMap;
	}

	@Override
	public void setAbbreviation(String abbreviation)
	{
		if (different(abbreviation, this.abbreviation))
		{
			this.abbreviation = abbreviation;
			this.changed = true;
		}
	}

	@Override
	public void setName(String name)
	{
		if (different(name, this.name))
		{
			this.name = name;
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

	protected void initAbbreviation(String abbreviation)
	{
		this.abbreviation = abbreviation;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initName(String name)
	{
		this.name = name;
	}

	/**
	 * Check if there are any changes.
	 * 
	 * @return true if changed, false if not.
	 */
	protected boolean isChanged()
	{
		return this.changed;
	}
}
