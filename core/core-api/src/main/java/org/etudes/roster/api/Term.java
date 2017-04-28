/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/Term.java $
 * $Id: Term.java 12060 2015-11-12 03:58:14Z ggolden $
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

import java.util.Map;

/**
 * Site models an Etudes (academic) term.
 */
public interface Term
{
	/**
	 * @return The term abbreviation.
	 */
	String getAbbreviation();

	/**
	 * @return The site id.
	 */
	Long getId();

	/**
	 * @return The term name.
	 */
	String getName();

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the term abbreviation.
	 * 
	 * @param abbreviation
	 *        The abbreviation.
	 */
	void setAbbreviation(String abbreviation);

	/**
	 * Set the term name.
	 * 
	 * @param name
	 *        The name.
	 */
	void setName(String name);

}
