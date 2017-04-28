/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/entity/webapp/EntityImpl.java $
 * $Id: EntityImpl.java 10634 2015-04-28 02:18:05Z ggolden $
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

package org.etudes.entity.webapp;

import java.util.Date;

import org.etudes.entity.api.Entity;
import org.etudes.user.api.User;

public class EntityImpl implements Entity
{
	protected User createdBy = null;
	protected Date createdOn = null;
	protected Long id = null;
	protected User modifiedBy = null;
	protected Date modifiedOn = null;

	@Override
	public User getCreatedBy()
	{
		return this.createdBy;
	}

	@Override
	public Date getCreatedOn()
	{
		return this.createdOn;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public User getModifiedBy()
	{
		return this.modifiedBy;
	}

	@Override
	public Date getModifiedOn()
	{
		return this.modifiedOn;
	}

	public void initCreatedBy(User user)
	{
		this.createdBy = user;
	}

	public void initCreatedOn(Date createdOn)
	{
		this.createdOn = createdOn;
	}

	public void initEntity(Entity entity)
	{
		this.id = entity.getId();
		this.createdBy = entity.getCreatedBy();
		this.createdOn = entity.getCreatedOn();
		this.modifiedBy = entity.getModifiedBy();
		this.modifiedOn = entity.getModifiedOn();
	}

	public void initId(Long id)
	{
		this.id = id;
	}

	public void initModifiedBy(User user)
	{
		this.modifiedBy = user;
	}

	public void initModifiedOn(Date modifiedOn)
	{
		this.modifiedOn = modifiedOn;
	}
}
