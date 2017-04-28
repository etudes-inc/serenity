/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/entity/api/Entity.java $
 * $Id: Entity.java 10634 2015-04-28 02:18:05Z ggolden $
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

package org.etudes.entity.api;

import java.util.Date;

import org.etudes.user.api.User;

public interface Entity
{
	/**
	 * @return the user who created this.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * @return The id.
	 */
	Long getId();

	/**
	 * @return the user who last modified this.
	 */
	User getModifiedBy();

	/**
	 * @return the date last modified.
	 */
	Date getModifiedOn();
}
