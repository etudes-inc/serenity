/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/Membership.java $
 * $Id: Membership.java 12506 2016-01-10 01:58:40Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015, 2016 Etudes, Inc.
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

import java.util.Collection;
import java.util.List;

import org.etudes.user.api.User;

/**
 * Membership is a list of Members for a roster or for the aggregate site membership.
 */
public interface Membership
{
	/**
	 * Add a membership
	 * 
	 * @param user
	 *        The user.
	 * @param role
	 *        The role.
	 * @param active
	 *        The active flag.
	 */
	void add(User user, Role role, Boolean active);

	/**
	 * Assure that this user has this role and active flag in the membership.
	 * 
	 * @param user
	 *        The user.
	 * @param role
	 *        The role.
	 * @param active
	 *        The active flag.
	 */
	void assure(User user, Role role, Boolean active);

	/**
	 * Find the members that use this role.
	 * 
	 * @param role
	 *        The Role to find.
	 * @return A List of memberships that use this role - may be empty.
	 */
	List<Member> findRole(Role role);

	/**
	 * Find a member for the user.
	 * 
	 * @param user
	 *        The user to find.
	 * @return The Membership for the user, or null if not found.
	 */
	Member findUser(User user);

	/**
	 * @return The members making up the roster.
	 */
	Collection<Member> getMembers();

	/**
	 * @return The users making up the roster.
	 */
	Collection<User> getUsers();

	/**
	 * Remove this user from the membership.
	 * 
	 * @param user
	 *        The user.
	 */
	void remove(User user);

	/**
	 * Update the active flag for this member.
	 * 
	 * @param user
	 *        The user.
	 * @param active
	 *        The new active flag.
	 */
	void update(User user, Boolean active);

	/**
	 * Update the role for this member.
	 * 
	 * @param user
	 *        The user.
	 * @param role
	 *        The new role.
	 */
	void update(User user, Role role);
}
