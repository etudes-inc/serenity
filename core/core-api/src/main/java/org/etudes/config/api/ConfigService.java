/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/config/api/ConfigService.java $
 * $Id: ConfigService.java 11537 2015-09-01 21:12:54Z ggolden $
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

package org.etudes.config.api;

/**
 * E3 Configuration service.
 */
public interface ConfigService
{
	/**
	 * Find the value set for this named configuration item.
	 * 
	 * @param name
	 *        The configuration item name.
	 * @param valueIfMissing
	 *        The value to return instead of null if the item is not set.
	 * @return The value of the configuration item, or valueIfMissing if not set.
	 */
	boolean getBoolean(String name, boolean valueIfMissing);

	/**
	 * Find the value set for this named configuration item.
	 * 
	 * @param name
	 *        The configuration item name.
	 * @param valueIfMissing
	 *        The value to return instead of null if the item is not set.
	 * @return The value of the configuration item, or valueIfMissing if not set.
	 */
	int getInt(String name, int valueIfMissing);

	/**
	 * Find the value set for this named configuration item.
	 * 
	 * @param name
	 *        The configuration item name.
	 * @param valueIfMissing
	 *        The value to return instead of null if the item is not set.
	 * @return The value of the configuration item, or valueIfMissing if not set.
	 */
	long getLong(String name, long valueIfMissing);

	/**
	 * Find the value set for this named configuration item.
	 * 
	 * @param name
	 *        The configuration item name.
	 * @return The value of the configuration item, or null if not set.
	 */
	String getString(String name);

	/**
	 * Find the value set for this named configuration item.
	 * 
	 * @param name
	 *        The configuration item name.
	 * @param valueIfMissing
	 *        The value to return instead of null if the item is not set.
	 * @return The value of the configuration item, or valueIfMissing if not set.
	 */
	String getString(String name, String valueIfMissing);

	/**
	 * Find the value set for this named configuration item array.
	 * 
	 * @param name
	 *        The configuration item name.
	 * @return The value of the configuration item, as an array, or null if not set.
	 */
	String[] getStrings(String name);
}
