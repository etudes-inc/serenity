/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-api/src/main/java/org/etudes/syllabus/api/SyllabusExternal.java $
 * $Id: SyllabusExternal.java 11454 2015-08-15 04:13:37Z ggolden $
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

package org.etudes.syllabus.api;

import java.util.Map;

/**
 * SyllabusExternal models information for an external syllabus (part of a Syllabus)
 */
public interface SyllabusExternal
{
	/**
	 * @return The height (pixels) for use in-place (ignored if newWindow)
	 */
	Integer getHeight();

	/**
	 * @return TRUE if the external URL is to be opened in a new window / tab, FALSE if it it is to be opened in-place.
	 */
	Boolean getNewWindow();

	/**
	 * @return The external syllabus URL.
	 */
	String getUrl();

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
	 * Update the height.
	 * 
	 * @param height
	 *        The height.
	 */
	void setHeight(Integer height);

	/**
	 * Update the newWindow flag.
	 * 
	 * @param newWindow
	 *        The newWindow flag.
	 * 
	 */
	void setNewWindow(Boolean newWindow);

	/**
	 * Set the external syllabus URL.
	 * 
	 * @param url
	 *        The new url.
	 */
	void setUrl(String url);
}
