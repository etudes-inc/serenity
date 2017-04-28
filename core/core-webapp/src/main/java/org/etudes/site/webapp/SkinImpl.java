/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/site/webapp/SkinImpl.java $
 * $Id: SkinImpl.java 8641 2014-09-04 02:35:12Z ggolden $
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

package org.etudes.site.webapp;

import static org.etudes.util.Different.different;

import org.etudes.site.api.Skin;

public class SkinImpl implements Skin
{
	protected Long client = null;
	protected String color = null;
	protected Long id = null;
	protected String name = null;

	public SkinImpl(Long id, String name, String color, Long client)
	{
		this.id = id;
		this.name = name;
		this.color = color;
		this.client = client;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SkinImpl)) return false;
		SkinImpl other = (SkinImpl) obj;
		if (different(this.id, other.id)) return false;
		return true;
	}

	@Override
	public Long getClient()
	{
		return this.client;
	}

	@Override
	public String getColor()
	{
		// TODO Auto-generated method stub
		return this.color;
	}

	@Override
	public Long getId()
	{
		// TODO Auto-generated method stub
		return this.id;
	}

	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return this.name;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
		return result;
	}
}
