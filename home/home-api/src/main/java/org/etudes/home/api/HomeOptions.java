/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-api/src/main/java/org/etudes/home/api/HomeOptions.java $
 * $Id: HomeOptions.java 11467 2015-08-18 04:20:07Z ggolden $
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

package org.etudes.home.api;

import java.util.Map;

import org.etudes.site.api.Site;

/**
 * The E3 Home service home options.
 */
public interface HomeOptions
{
	/**
	 * @return The site to which these options apply.
	 */
	Site getSite();

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
}
