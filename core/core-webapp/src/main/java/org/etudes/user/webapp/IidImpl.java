/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/user/webapp/IidImpl.java $
 * $Id: IidImpl.java 10421 2015-04-03 20:34:51Z ggolden $
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

package org.etudes.user.webapp;

import static org.etudes.util.Different.different;
import static org.etudes.util.StringUtil.splitLast;

import org.etudes.user.api.Iid;

/**
 * IID implementation.
 */
public class IidImpl implements Iid
{
	/** The client IID code portion of the IID. */
	protected String code = null;

	/** The id portion of the IID. */
	protected String id = null;

	/**
	 * Construct from an "id@code" display format string.
	 * 
	 * @param iidDisplay
	 *        The IID display format string.
	 */
	public IidImpl(String iidDisplay)
	{
		String[] parts = splitLast(iidDisplay, "@");
		if (parts != null)
		{
			this.code = parts[1].toLowerCase();
			this.id = parts[0].toLowerCase();
		}
	}

	/**
	 * Construct from separate id and institution code parts.
	 * 
	 * @param id
	 *        The id.
	 * @param code
	 *        The client iid code.
	 */
	public IidImpl(String id, String code)
	{
		this.id = id.toLowerCase();
		this.code = code.toLowerCase();
	}

	@Override
	public int compareTo(Iid o)
	{
		int rv = this.code.compareTo(o.getCode());
		if (rv == 0)
		{
			rv = this.id.compareTo(o.getId());
		}

		return rv;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof IidImpl)) return false;
		IidImpl other = (IidImpl) obj;
		if (different(this.id, other.id)) return false;
		if (different(this.code, other.code)) return false;
		return true;
	}

	@Override
	public String getCode()
	{
		return this.code;
	}

	@Override
	public String getId()
	{
		return this.id;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
		result = prime * result + ((this.code == null) ? 0 : this.code.hashCode());
		return result;
	}

	@Override
	public String toString()
	{
		return this.id + "@" + this.code;
	}
}
