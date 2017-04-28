/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/site/api/Link.java $
 * $Id: Link.java 10410 2015-04-02 22:09:28Z ggolden $
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

package org.etudes.site.api;

public interface Link
{
	/**
	 * Check for exact equality between this and the other. equals() just goes on id match.
	 * 
	 * @param other
	 *        The other link.
	 * @return true if exactly equal, false if not.
	 */
	boolean exactlyEqual(Link other);

	/**
	 * @return The link id.
	 */
	Long getId();

	/**
	 * @return The order position for the link.
	 */
	Integer getPosition();

	/**
	 * @return The link title.
	 */
	String getTitle();

	/**
	 * @return The link's URL.
	 */
	String getUrl();
}
