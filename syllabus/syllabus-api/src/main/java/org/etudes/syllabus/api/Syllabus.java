/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-api/src/main/java/org/etudes/syllabus/api/Syllabus.java $
 * $Id: Syllabus.java 11454 2015-08-15 04:13:37Z ggolden $
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * Syllabus models an Etudes class syllabus.
 */
public interface Syllabus
{
	enum Source
	{
		external("E"), sections("S");

		public static Source fromCode(String c)
		{
			for (Source s : Source.values())
			{
				if (s.code.equals(c)) return s;
			}
			return sections;
		}

		private String code;

		private Source(String code)
		{
			this.code = code;
		}

		public String getCode()
		{
			return this.code;
		}
	};

	/**
	 * Find the section in the syllabus with this id.
	 * 
	 * @param id
	 * @return The SyllabusSection found, or null if not.
	 */
	SyllabusSection findSectionById(Long id);

	/**
	 * Find a section in the syllabus by exact title match.
	 * 
	 * @param title
	 *        The title to search for.
	 * @return The SyllabusSection if found, or null if not.
	 */
	SyllabusSection findSectionByTitle(String title);

	/**
	 * @return the user who created this.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * @return The syllabus's external information.
	 */
	SyllabusExternal getExternal();

	/**
	 * @return The syllabus id.
	 */
	Long getId();

	/**
	 * @return the user who last modified this.
	 */
	User getModifiedBy();

	/**
	 * @return the date last modified.
	 */
	Date getModifiedOn();

	/**
	 * Collect all the User objects referenced in the syllabus (created by, modified by ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * Get all the syllabus sections.
	 * 
	 * @return The List of sections, may be empty;
	 */
	List<SyllabusSection> getSections();

	/**
	 * @return The site in which the syllabus lives.
	 */
	Site getSite();

	/**
	 * @return The syllabus source.
	 */
	Source getSource();

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
	 * Remove this section from the syllabus - will commit when the syllabus is saved.
	 * 
	 * @param section
	 *        The section to remove.
	 */
	void removeSection(SyllabusSection section);

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the source.
	 * 
	 * @param source
	 *        The syllabus source.
	 */
	void setSource(Source source);
}
