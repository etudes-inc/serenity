/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-api/src/main/java/org/etudes/blog/api/BlogEntry.java $
 * $Id: BlogEntry.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.blog.api;

import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import org.etudes.file.api.File;
import org.etudes.file.api.Reference;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * BlogEntry models an entry in an Etudes blog.
 */
public interface BlogEntry
{
	/**
	 * @return The blog in which this entry lives.
	 */
	Blog getBlog();

	/**
	 * Get the blog content.
	 * 
	 * @param forDownload
	 *        if true, modify the content to convert embedded references from stored placeholder URLs to download URLs using references for this item.
	 * @return The blog content.
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
	 * @return The blog entry's image file reference, or null if none set.
	 */
	Reference getImage();

	/**
	 * @return the user who last modified this.
	 */
	User getModifiedBy();

	/**
	 * @return the date last modified.
	 */
	Date getModifiedOn();

	/**
	 * @return TRUE if the entry is published, FALSE if not.
	 */
	Boolean getPublished();

	/**
	 * @return The ToolItemReference for this blog entry.
	 */
	ToolItemReference getReference();

	/**
	 * Collect all the User objects referenced in the blog (created by, modified by ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * @return The blog entry title (plain text).
	 */
	String getTitle();

	/**
	 * Remove any set image.
	 */
	void removeImage();

	/**
	 * Set the blog entry content.
	 * 
	 * @param content
	 *        The new content.
	 * @param downloadReferenceFormat
	 *        if true, the content has embedded references in download (not placeholder) format.
	 */
	void setContent(String content, boolean downloadReferenceFormat);

	/**
	 * Set the blog entry's image to an existing image (by Reference).
	 * 
	 * @param ref
	 *        The existing image file Reference.
	 */
	void setImage(Reference ref);

	/**
	 * Set the blog entry's image.
	 * 
	 * @param name
	 *        The upload file name.
	 * @param size
	 *        The file size.
	 * @param type
	 *        The file mime type.
	 * @param content
	 *        The image file's content.
	 */
	void setImage(String name, int size, String type, InputStream content);

	/**
	 * Update the entry's publish status.
	 * 
	 * @param published
	 *        The new published value.
	 * 
	 */
	void setPublished(Boolean published);

	/**
	 * Set the blog entry title (plain text).
	 * 
	 * @param title
	 */
	void setTitle(String title);

	/**
	 * Set the blog entry content to share an existing file system file.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void shareContent(File file);

	/**
	 * Set the section entry image to share an existing file system file.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void shareImage(File file);
}
