/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/tool/api/ToolItemWorkReference.java $
 * $Id: ToolItemWorkReference.java 11569 2015-09-06 20:41:44Z ggolden $
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

import java.util.Date;

import org.etudes.user.api.User;

/**
 * ToolItemWorkReference is a reference to some work submitted to some item of a tool in some site. That item is identified by the embedded ToolItemReference; the remainder hold the id, user and date of the work. Used to refer to user submissions to tool
 * items, such as in ATS and Forum.
 */
public class ToolItemWorkReference
{
	protected ToolItemReference item = null;
	protected Date submittedOn = null;
	protected User user = null;
	protected Long workId = null;

	public ToolItemWorkReference(ToolItemReference item, Long work, User user, Date submittedOn)
	{
		this.item = item;
		this.workId = work;
		this.user = user;
		this.submittedOn = submittedOn;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ToolItemWorkReference)) return false;
		ToolItemWorkReference other = (ToolItemWorkReference) obj;
		if (!this.item.equals(other.item)) return false;
		if (different(workId, other.workId)) return false;
		return true;
	}

	/**
	 * @return The item reference for this work.
	 */
	public ToolItemReference getItem()
	{
		return this.item;
	}

	/**
	 * @return The date the submission was completed.
	 */
	public Date getSubmittedOn()
	{
		return this.submittedOn;
	}

	/**
	 * @return The user submitting the work.
	 */
	public User getUser()
	{
		return this.user;
	}

	/**
	 * @return The work id.
	 */
	public Long getWorkId()
	{
		return this.workId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + item.hashCode() + workId.hashCode();
		return result;
	}
}
