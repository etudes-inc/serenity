/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/SiteMember.java $
 * $Id: SiteMember.java 8472 2014-08-15 22:14:07Z ggolden $
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

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * SiteMember models a user's membership in a roster or site.
 */
public interface SiteMember
{
	/**
	 * @return The user's Role in the site.
	 */
	Role getRole();

	/**
	 * @return The site.
	 */
	Site getSite();

	/**
	 * @return The User.
	 */
	User getUser();
}
