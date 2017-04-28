/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-api/src/main/java/org/etudes/home/api/HomeItem.java $
 * $Id: HomeItem.java 11469 2015-08-18 22:57:25Z ggolden $
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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.etudes.file.api.File;
import org.etudes.file.api.Reference;
import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * The E3 Home service home content item.
 */
public interface HomeItem
{
	enum Source
	{
		authored("A"), file("F"), web("W"), youtube("Y");

		public static Source fromCode(String c)
		{
			for (Source s : Source.values())
			{
				if (s.code.equals(c)) return s;
			}
			return web;
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

	enum Status
	{
		current(0), past(3), pending(1), unknown(-1), unpublished(2);

		public static Status fromCode(Integer i)
		{
			for (Status s : Status.values())
			{
				if (s.code.equals(i)) return s;
			}
			return unpublished;
		}

		private Integer code;

		private Status(Integer code)
		{
			this.code = code;
		}

		public Integer getCode()
		{
			return this.code;
		}
	};

	/**
	 * @return The alt text.
	 */
	String getAlt();

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
	 * @return The dimensions.
	 */
	String getDimensions();

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
	 * @return TRUE if published, FALSE if not.
	 */
	Boolean getPublished();

	/**
	 * Return the tool item reference to this item.
	 * 
	 * @return The ToolItemReference.
	 */
	ToolItemReference getReference();

	/**
	 * Collect all the User objects referenced in the item (created by, modified by ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * @return The release date, or null if not set.
	 */
	Date getReleaseDate();

	/**
	 * @return The site in which the item lives.
	 */
	Site getSite();

	/**
	 * @return The item source.
	 */
	Source getSource();

	/**
	 * @return The item status.
	 */
	Status getStatus();

	/**
	 * @return The title.
	 */
	String getTitle();

	/**
	 * @return The url.
	 */
	String getUrl();

	/**
	 * @return TRUE if the item has the required definitions for the source, FALSE if not.
	 */
	Boolean isValid();

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
	 * @param user
	 *        The user editing the home page (or null for non-edit purposes)
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send(User user);

	/**
	 * Set the alt text (for web or file).
	 * 
	 * @param alt
	 *        The alt text.
	 */
	void setAlt(String alt);

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
	 * Set the dimensions (web, height in pixels, youtube, ratio as "16", "4" or "1")
	 * 
	 * @param dimensions
	 *        The dimensions.
	 */
	void setDimensions(String dimensions);

	/**
	 * Update the published status.
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
	 * Set the source.
	 * 
	 * @param source
	 *        The source.
	 */
	void setSource(Source source);

	/**
	 * Set the title.
	 * 
	 * @param title
	 *        The title.
	 */
	void setTitle(String title);

	/**
	 * Set the url (web url, youtube id)
	 * 
	 * @param url
	 *        The url.
	 */
	void setUrl(String url);

	/**
	 * Set the content to share an existing file system file.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void shareContent(File file);

	/**
	 * Set the (file type) item to use this myFiles reference.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void useMyFile(Reference ref);
}
