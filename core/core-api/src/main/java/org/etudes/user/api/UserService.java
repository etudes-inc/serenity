/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/user/api/UserService.java $
 * $Id: UserService.java 12054 2015-11-10 22:01:23Z ggolden $
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

package org.etudes.user.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * The E3 User service.
 */
public interface UserService
{
	/** Sort criteria for find */
	enum Sort
	{
		eid, email, id, iid, name
	}

	/** Some well known users */
	final Long ADMIN = 1L;
	final Long HELPDESK = 2L;
	final Long SYSTEM = 3L;

	/**
	 * Create a new user, assigning a new user id.
	 * 
	 * @param addedBy
	 *        The user doing the adding.
	 * @return The added User.
	 */
	User add(User addedBy);

	/**
	 * Check that this id is a valid user id. Use check() instead of get() if you don't need the full user information loaded.
	 * 
	 * @param id
	 *        The user id.
	 * @return A User object with the id (at least) set, or null if not found
	 */
	User check(Long id);

	/**
	 * As a companion to the paging find(), count the total users across all pages with this criteria.
	 * 
	 * @param search
	 *        The search string - partial match against name if specified.
	 * @return The count of users meeting the criteria.
	 */
	Integer count(String search);

	/**
	 * Create an User object, populated from the result set, in the field order of our "sqlSelectFragment" SQL code, starting at index.
	 * 
	 * @param result
	 *        The SQL ResultSet
	 * @param index
	 *        The starting index
	 * @return The User object.
	 */
	User createFromResultSet(ResultSet result, int index) throws SQLException;

	/**
	 * find a page of users meeting this criteria. For finding by eid or IID or email, use findByEid or findByIid or findByEmail.
	 * 
	 * @param search
	 *        The search string - partial match against name if specified.
	 * @param sort
	 *        sort criteria.
	 * @param pageNum
	 *        The 1 based page number to get.
	 * @param pageSize
	 *        The number of sites per page.
	 * @return The List of users meeting the criteria.
	 */
	List<User> find(String search, Sort sort, Integer pageNum, Integer pageSize);

	/**
	 * Find the user(s) who use this EID.
	 * 
	 * @param eid
	 *        The EID. EID is case insensitive.
	 * @return The List of Users who use this EID, possibly empty.
	 */
	List<User> findByEid(String eid);

	/**
	 * Find the user(s) who use this email, for any of their registered email accounts.
	 * 
	 * @param email
	 *        The email.
	 * @return The List of Users who use this email, possibly empty.
	 */
	List<User> findByEmail(String eid);

	/**
	 * Find a user by IID.
	 * 
	 * @param iid
	 *        The IID.
	 * @return The user found, or null if we don't have a user by this IID.
	 */
	User findByIid(Iid iid);

	/**
	 * Get the User with this id.
	 * 
	 * @param id
	 *        The user id.
	 * @return The User with this id, or null if not found.
	 */
	User get(Long id);;

	/**
	 * Get the iids for this user.
	 * 
	 * @param user
	 *        The user.
	 * @return The user Iid list, may be empty.
	 */
	List<Iid> getUserIid(User user);

	/**
	 * Check if the user is a protected user, not to be removed (Admin, Helpdesk).
	 * 
	 * @param user
	 *        The user.
	 * @return TRUE if protected, FALSE if not.
	 */
	Boolean isProtected(User user);

	/**
	 * Construct an IID
	 * 
	 * @param display
	 *        The combined IID@clientcode value.
	 * @return The IID.
	 */
	Iid makeIid(String display);

	/**
	 * Construct an IID
	 * 
	 * @param iid
	 *        The IID id portion.
	 * @param code
	 *        The client IID code portion.
	 * @return The IID.
	 */
	Iid makeIid(String iid, String code);

	/**
	 * Refresh this User object with a full data load from the database, overwriting any values in the User, setting it to unchanged.
	 * 
	 * @param user
	 *        The user.
	 */
	void refresh(User user);

	/**
	 * Remove this user.
	 * 
	 * @param user
	 *        The user.
	 */
	void remove(User user);

	/**
	 * Reset the password for the user uniquely identified by this email address.
	 * 
	 * @param email
	 *        The email address.
	 * @return TRUE if a single user is found and the password reset, FALSE if not.
	 */
	Boolean resetPassword(String email);

	/**
	 * Save any changes made to this user.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param user
	 *        The user to save.
	 */
	void save(User savedBy, User user);

	/**
	 * @return a SQL fragment for the GROUP BY clause to use with sqlSelectFragment().
	 */
	String sqlGroupFragment();

	/**
	 * Get a SQL fragment for the JOIN clause to use with sqlSelectFragment().
	 * 
	 * @param on
	 *        The value to join against the user id.
	 * @return The SQL fragment.
	 */
	String sqlJoinFragment(String on);

	/**
	 * Get a SQL fragment that can be added to any select statement that joins with the USER table. Make sure to use sqlJoinFragment() for a JOIN clause, and make sure to GROUP BY your primary ID.
	 * 
	 * @return The SQL fragment.
	 */
	String sqlSelectFragment();

	/**
	 * @return The size of the index increment used up by the sqlSelectTermFragment - i.e. the # fields.
	 */
	Integer sqlSelectFragmentNumFields();

	/**
	 * Check the password for strong password rule adherence.
	 * 
	 * @param pw
	 *        The unencoded password.
	 * @return TRUE if the password meets strong password rules, FALSE if not.
	 */
	Boolean strongPassword(String pw);

	/**
	 * Prepare for the imminent use of these users. The User objects are likely unchecked.
	 * 
	 * @param users
	 *        A Set of user object.
	 */
	void willNeed(Set<User> users);

	/**
	 * Encapsulate a user id into a User object. The user id is not checked.
	 * 
	 * @param id
	 *        The user id.
	 * @return A User object with this id set.
	 */
	User wrap(Long id);
}
