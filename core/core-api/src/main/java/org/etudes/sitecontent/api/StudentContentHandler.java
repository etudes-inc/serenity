/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/StudentContentHandler.java $
 * $Id: StudentContentHandler.java 10509 2015-04-17 21:50:49Z ggolden $
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

package org.etudes.sitecontent.api;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * StudentContentHandler is code that holds site related content, contributed by students.
 */
public interface StudentContentHandler
{
	/**
	 * Clear all non-instructor roster entries and contributions from a site - Instructors and authored content remain.
	 */
	void clear(Site site);

	/**
	 * Clear this user and contributions from the site.
	 */
	void clear(Site site, User user);
}
