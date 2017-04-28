/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-api/src/main/java/org/etudes/syllabus/api/SyllabusService.java $
 * $Id: SyllabusService.java 11454 2015-08-15 04:13:37Z ggolden $
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

import java.util.List;
import java.util.Map;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * The Serenity Syllabus service.
 */
public interface SyllabusService
{
	/**
	 * Record that this user has accepted this syllabus.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 * @param user
	 *        The user.
	 * @return The date of acceptance.
	 */
	SyllabusAcceptance accept(Syllabus syllabus, User user);

	/**
	 * Create a new syllabus for the site. If the syllabus exists already, returns the existing syllabus unchanged.
	 * 
	 * @param addedBy
	 *        The user doing the adding.
	 * @param inSite
	 *        The site holding the syllabus.
	 * @return The added (or found) syllabus.
	 */
	Syllabus add(User addedBy, Site inSite);

	/**
	 * Add a section to a syllabus. It goes at the end of the ordering.
	 * 
	 * @param addedBy
	 *        The user doing the adding.
	 * @param syllabus
	 *        The syllabus.
	 * @return The new syllabus section.
	 */
	SyllabusSection addSection(User addedBy, Syllabus syllabus);

	/**
	 * Find the syllabus for this site.
	 * 
	 * @param inSite
	 *        The site holding the syllabus.
	 * @return The site's syllabus. If none established, a new syllabus.
	 */
	Syllabus findBySite(Site inSite);

	/**
	 * Get the syllabus with this id.
	 * 
	 * @param id
	 *        The syllabus id.
	 * @return The Syllabus with this id, or null if not found.
	 */
	Syllabus get(Long id);

	/**
	 * Get all the acceptance records for this syllabus.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 * @return The acceptance records, mapped to the accepting user.
	 */
	Map<User, SyllabusAcceptance> getAccepted(Syllabus syllabus);

	/**
	 * Get the acceptance for this syllabus from this user.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 * @param user
	 *        The user.
	 * @return The SyllabusAcceptance, if found, or null if not.
	 */
	SyllabusAcceptance getAccepted(Syllabus syllabus, User user);

	/**
	 * Get a section.
	 * 
	 * @param id
	 *        The section id.
	 * @return The section, or null if not found.
	 */
	SyllabusSection getSection(Long id);

	/**
	 * Get the ordered sections for this syllabus. Can also call syllabus.getSections().
	 * 
	 * @param syllabus
	 *        The syllabus.
	 * @return The List of SyllabusSection, possibly empty.
	 */
	List<SyllabusSection> getSections(Syllabus syllabus);

	/**
	 * Remove this syllabus.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 */
	void remove(Syllabus syllabus);

	/**
	 * Remove this section
	 * 
	 * @param removedBy
	 *        The user doing the removing.
	 * @param section
	 *        The section to remove.
	 */
	void remove(User removedBy, SyllabusSection section);

	/**
	 * Save any changes made to this syllabus or any of its sections.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param syllabus
	 *        The syllabus to save.
	 */
	void save(User savedBy, Syllabus syllabus);

	/**
	 * Save any changes made to this section.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param section
	 *        The section to save.
	 */
	void save(User savedBy, SyllabusSection section);
}
