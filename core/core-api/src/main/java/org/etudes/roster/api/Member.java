/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/Member.java $
 * $Id: Member.java 10921 2015-05-21 02:48:48Z ggolden $
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

import java.util.Comparator;
import java.util.Map;

import org.etudes.user.api.User;

/**
 * Member models a user's membership in a roster or site.
 */
public interface Member
{
	/**
	 * Sort comparator based on the member's user's sort display
	 */
	public static class NameSortComparator implements Comparator<Member>
	{
		public int compare(Member o1, Member o2)
		{
			return o1.getUser().getNameSort().compareTo(o2.getUser().getNameSort());
		}
	}

	/**
	 * @return The member's Role.
	 */
	Role getRole();

	/**
	 * @return For aggregate membership, the roster responsible for including this member..
	 */
	Roster getRoster();

	/**
	 * @return The User.
	 */
	User getUser();

	/**
	 * @return TRUE if the user is active, FALSE if not.
	 */
	Boolean isActive();

	/**
	 * @return TRUE if the user is blocked (from the site), FALSE if not, or null if it is not applicable (i.e. for an individual roster, not an aggregate one)
	 */
	Boolean isBlocked();

	/**
	 * Format for sending via CDP.
	 * 
	 * @param client
	 *        If set, iid will be formatted for display in the context of sites for this client (iidFull will always show the full iid).
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send(Client client);
}
