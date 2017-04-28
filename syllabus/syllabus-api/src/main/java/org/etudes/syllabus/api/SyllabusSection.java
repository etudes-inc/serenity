/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-api/src/main/java/org/etudes/syllabus/api/SyllabusSection.java $
 * $Id: SyllabusSection.java 11561 2015-09-06 00:45:58Z ggolden $
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
import java.util.Map;
import java.util.Set;

import org.etudes.file.api.File;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * SyllabusSection models one section in an Etudes syllabus.
 */
public interface SyllabusSection
{
	/**
	 * Get the section content (html).
	 * 
	 * @param forDownload
	 *        if true, modify the content to convert embedded references from stored placeholder URLs to download URLs using references for this item.
	 * @return The section content.
	 */
	String getContent(boolean forDownload);

	/**
	 * @return the user who created this.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * @return the entry id.
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
	 * @return The presentation order for this section.
	 */
	Integer getOrder();

	/**
	 * @return TRUE if this section is publicly viewable, FALSE if it is limited to the site members.
	 */
	Boolean getPublic();

	/**
	 * @return TRUE if the entry is published, FALSE if not.
	 */
	Boolean getPublished();

	/**
	 * @return The ToolItemReference to this syllabus section.
	 */
	ToolItemReference getReference();

	/**
	 * Collect all the User objects referenced in the section (created by, modified by ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * @return The Syllabus in which this section lives.
	 */
	Syllabus getSyllabus();

	/**
	 * @return The section title (plain text).
	 */
	String getTitle();

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
	 * Set the section entry content (html with embedded URLs in "placeholder" format).
	 * 
	 * @param content
	 *        The new content.
	 * @param downloadReferenceFormat
	 *        if true, the content has embedded references in download (not placeholder) format.
	 */
	void setContent(String content, boolean downloadReferenceFormat);

	/**
	 * Set the section presentation order.
	 * 
	 * @param order
	 *        The new order.
	 */
	void setOrder(Integer order);

	/**
	 * Update the section's public status.
	 * 
	 * @param isPublic
	 *        The new public status.
	 * 
	 */
	void setPublic(Boolean isPublic);

	/**
	 * Update the section's publish status.
	 * 
	 * @param published
	 *        The new published status.
	 * 
	 */
	void setPublished(Boolean published);

	/**
	 * Set the section's title (plain text).
	 * 
	 * @param title
	 */
	void setTitle(String title);

	/**
	 * Set the section entry content to share an existing file system file.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void shareContent(File file);
}
