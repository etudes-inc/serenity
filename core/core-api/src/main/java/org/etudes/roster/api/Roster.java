/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/Roster.java $
 * $Id: Roster.java 8535 2014-08-28 22:34:35Z ggolden $
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

package org.etudes.roster.api;

/**
 * RosterIdentity identifies a roster (a Roster is the list of users in the roster).
 */
public interface Roster
{
	/**
	 * @return The roster's client.
	 */
	Client getClient();

	/**
	 * @return The roster id.
	 */
	Long getId();

	/**
	 * @return The roster membership.
	 */
	Membership getMembership();

	/**
	 * @return The roster name.
	 */
	String getName();

	/**
	 * @return The roster's term.
	 */
	Term getTerm();

	/**
	 * Check if the roster is a site's adhoc roster.
	 * 
	 * @return TRUE if the roster is a site's adhoc roster, FALSE if not.
	 */
	Boolean isAdhoc();

	/**
	 * Check if the roster is a site's master roster.
	 * 
	 * @return TRUE if the roster is a site's master roster, FALSE if not.
	 */
	Boolean isMaster();

	/**
	 * @return TRUE if the roster is an official roster, FALSE if adhoc.
	 */
	Boolean isOfficial();
}
