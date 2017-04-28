/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-api/src/main/java/org/etudes/home/api/HomeService.java $
 * $Id: HomeService.java 11467 2015-08-18 04:20:07Z ggolden $
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

package org.etudes.home.api;

import java.util.List;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * The E3 Home service.
 */
public interface HomeService
{
	/**
	 * Create a new home item in thie site.
	 * 
	 * @param addedBy
	 *        The user adding.
	 * @param inSite
	 *        The site.
	 * @return The item.
	 */
	HomeItem itemAdd(User addedBy, Site inSite);

	/**
	 * Get the site's current item.
	 * 
	 * @param site
	 *        The site.
	 * @return The item, or null if there is no current item.
	 */
	HomeItem itemFindCurrent(Site site);

	/**
	 * Get all the items in the site, ordered by status (current, pending, unpublished, past) and date and title.
	 * 
	 * @param site
	 *        The site.
	 * @return The list of HomeItem, may be empty.
	 */
	List<HomeItem> itemFindInSite(Site site);

	/**
	 * Get this home item.
	 * 
	 * @param id
	 *        The item id.
	 * @return The item, or null if not found.
	 */
	HomeItem itemGet(Long id);

	/**
	 * Refresh the item with a fresh complete read from the db.
	 * 
	 * @param item
	 *        The item, with the id set.
	 */
	void itemRefresh(HomeItem item);

	/**
	 * Remove this item.
	 * 
	 * @param item
	 *        The item.
	 */
	void itemRemove(HomeItem item);

	/**
	 * Save changes in this item.
	 * 
	 * @param savedBy
	 *        The user saving.
	 * @param item
	 *        The item.
	 */
	void itemSave(User savedBy, HomeItem item);

	/**
	 * Get the home options for the site.
	 * 
	 * @param site
	 *        The site.
	 * @return The home options.
	 */
	HomeOptions optionsGet(Site site);

	/**
	 * Save the home options.
	 * 
	 * @param savedBy
	 *        The user saving.
	 * @param options
	 *        The options.
	 */
	void optionsSave(User savedBy, HomeOptions options);
}
