/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/site/webapp/LinkImpl.java $
 * $Id: LinkImpl.java 10410 2015-04-02 22:09:28Z ggolden $
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

package org.etudes.site.webapp;

import static org.etudes.util.Different.different;

import org.etudes.site.api.Link;

public class LinkImpl implements Link
{
	protected Long id = null;
	protected Integer position = null;
	protected String title = null;
	protected String url = null;

	public LinkImpl(Long id, String title, String url, Integer position)
	{
		this.id = id;
		this.title = title;
		this.url = url;
		this.position = position;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof LinkImpl)) return false;
		LinkImpl other = (LinkImpl) obj;
		if (different(this.id, other.id)) return false;
		return true;
	}

	@Override
	public boolean exactlyEqual(Link other)
	{
		if (this == other) return true;
		if (other == null) return false;
		if (!(other instanceof LinkImpl)) return false;
		LinkImpl otherLink = (LinkImpl) other;
		if (different(this.id, otherLink.id)) return false;
		if (different(this.title, otherLink.title)) return false;
		if (different(this.url, otherLink.url)) return false;
		if (different(this.position, otherLink.position)) return false;

		return true;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Integer getPosition()
	{
		return this.position;
	}

	@Override
	public String getTitle()
	{
		return title;
	}

	@Override
	public String getUrl()
	{
		return this.url;
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
