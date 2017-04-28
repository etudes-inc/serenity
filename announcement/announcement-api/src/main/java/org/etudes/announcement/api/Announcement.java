/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/announcement/announcement-api/src/main/java/org/etudes/announcement/api/Announcement.java $
 * $Id: Announcement.java 11758 2015-10-03 20:21:30Z ggolden $
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

package org.etudes.announcement.api;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.etudes.file.api.File;
import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * Announcement models an Etudes announcement.
 */
public interface Announcement
{
	/**
	 * @return The date for the byline - either the release date, or, if not set, the modified date
	 */
	Date getBylineDate();

	/**
	 * Get the content (html).
	 * 
	 * @param forDownload
	 *        if true, modify the content to convert embedded references from stored placeholder URLs to download URLs using references for this item.
	 * @return The content.
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
	 * @return The syllabus id.
	 */
	Long getId();

	/**
	 * @return TRUE if the announcement is published and not before the release date (if any), FALSE if not.
	 */
	Boolean getIsReleased();

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
	 * Return the tool item reference to this announcement.
	 * 
	 * @return The ToolItemReference.
	 */
	ToolItemReference getReference();

	/**
	 * Collect all the User objects referenced in the syllabus (created by, modified by ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * @return The release date, or null if not set.
	 */
	Date getReleaseDate();

	/**
	 * @return The site in which the announcement lives.
	 */
	Site getSite();

	/**
	 * @return The subject (plain text).
	 */
	String getSubject();

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
	 * Set the content (html with embedded URLs in "placeholder" format).
	 * 
	 * @param content
	 *        The new content.
	 * @param downloadReferenceFormat
	 *        if true, the content has embedded references in download (not placeholder) format.
	 */
	void setContent(String content, boolean downloadReferenceFormat);

	/**
	 * Set the presentation order.
	 * 
	 * @param order
	 *        The new order.
	 */
	void setOrder(Integer order);

	/**
	 * Update the public status.
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
	 * Set the release date.
	 * 
	 * @param release
	 *        The release date, or null to remove the release date.
	 */
	void setReleaseDate(Date release);

	/**
	 * Set the subject (plain text).
	 * 
	 * @param subject
	 */
	void setSubject(String subject);

	/**
	 * Set the content to share an existing file system file.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void shareContent(File file);
}
