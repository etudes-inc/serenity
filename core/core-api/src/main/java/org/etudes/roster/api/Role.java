/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/Role.java $
 * $Id: Role.java 10426 2015-04-05 19:03:03Z ggolden $
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

package org.etudes.roster.api;

/**
 * Role models a user's role in a roster (site).
 */
public enum Role
{
	// Note: role values are hard coded in both roster tools, for the role dialogs
	// "anonymous", "authenticated" and "custom" are not intended to be used for user role in site, but to describe role based security for things like publication of files
	admin(6), anonymous(-1), authenticated(0), custom(-2), guest(1), observer(2), instructor(5), none(0), student(3), ta(4);

	// NOTE: Role values are modeled on the DB as TINYINT, 1 byte signed

	public static Role valueOf(Integer i)
	{
		for (Role r : Role.values())
		{
			if (r.level.equals(i)) return r;
		}
		return custom;
	}

	private final Integer level;

	private Role(int level)
	{
		this.level = Integer.valueOf(level);
	}

	public Boolean ge(Role other)
	{
		return this.level >= other.level;
	}

	public Integer getLevel()
	{
		return this.level;
	}
}
