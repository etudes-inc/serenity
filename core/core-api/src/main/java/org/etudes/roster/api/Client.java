/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/roster/api/Client.java $
 * $Id: Client.java 12060 2015-11-12 03:58:14Z ggolden $
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
 * Client models an Etudes client school.
 */
public interface Client
{
	/**
	 * @return The client abbreviation.
	 */
	String getAbbreviation();

	/**
	 * @return The client id.
	 */
	Long getId();

	/**
	 * @return The IID code for users from this client.
	 */
	String getIidCode();

	/**
	 * @return The client's name.
	 */
	String getName();

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the client abbreviation.
	 * 
	 * @param abbreviation
	 *        The abbreviation.
	 */
	void setAbbreviation(String abbreviation);

	/**
	 * Set the client iid code.
	 * 
	 * @param iidCode
	 *        The iidCode.
	 */
	void setIidCode(String iidCode);

	/**
	 * Set the client name.
	 * 
	 * @param name
	 *        The name.
	 */
	void setName(String name);
}
