/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/Category.java $
 * $Id: Category.java 11587 2015-09-10 03:14:52Z ggolden $
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

package org.etudes.evaluation.api;

import java.util.Map;

import org.etudes.entity.api.Entity;
import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemType;

public interface Category extends Entity
{
	/**
	 * @return The number of lowest scoring items in this category to drop.
	 */
	Integer getNumberLowestToDrop();

	/**
	 * @return The site order (1 based).
	 */
	Integer getOrder();

	/**
	 * @return the Site in which this category resides.
	 */
	Site getSite();

	/**
	 * @return The category title.
	 */
	String getTitle();

	/**
	 * @return The category type.
	 */
	ToolItemType getType();

	/**
	 * @return true if any changes were made, false if not.
	 */
	boolean isChanged();

	/**
	 * Update from CDP parameters.
	 * 
	 * @param prefix
	 *        The parameter names prefix.
	 * @param parameters
	 *        The parameters.
	 */
	void read(String prefix, Map<String, Object> parameters);

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the NumvberLowestToDrop value
	 * 
	 * @param value
	 *        The number of lowest scoring items to drop from this category.
	 */
	void setNumberLowestToDrop(Integer value);

	/**
	 * Set the site order (1 based).
	 * 
	 * @param order
	 *        The new order.
	 */
	void setOrder(Integer order);

	/**
	 * Set the title.
	 * 
	 * @param title
	 *        The title.
	 */
	void setTitle(String title);

	/**
	 * Set the category type.
	 * 
	 * @param type
	 *        The type.
	 */
	void setType(ToolItemType type);
}
