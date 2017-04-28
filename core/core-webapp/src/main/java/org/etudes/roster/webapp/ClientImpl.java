/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/roster/webapp/ClientImpl.java $
 * $Id: ClientImpl.java 12060 2015-11-12 03:58:14Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015 Etudes, Inc.
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
import org.etudes.roster.api.Client;

/**
 * Client implementation.
 */
public class ClientImpl implements Client
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ClientImpl.class);

	protected String abbreviation = null;

	protected boolean changed = false;

	protected Long id = null;

	protected String iidCode = null;

	protected String name = null;

	/**
	 * Construct.
	 * 
	 * @param userServiceImpl
	 */
	public ClientImpl()
	{
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ClientImpl)) return false;
		ClientImpl other = (ClientImpl) obj;
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
	public String getIidCode()
	{
		return this.iidCode;
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
		Map<String, Object> clientMap = new HashMap<String, Object>();
		clientMap = new HashMap<String, Object>();
		clientMap.put("id", getId().toString());
		clientMap.put("name", getName());
		clientMap.put("abbreviation", getAbbreviation());
		clientMap.put("iid", getIidCode());

		return clientMap;
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
	public void setIidCode(String iidCode)
	{
		if (different(iidCode, this.iidCode))
		{
			this.iidCode = iidCode;
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

	protected void initIidCode(String code)
	{
		this.iidCode = code;
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
