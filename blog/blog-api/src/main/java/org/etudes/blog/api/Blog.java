/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-api/src/main/java/org/etudes/blog/api/Blog.java $
 * $Id: Blog.java 10088 2015-02-18 23:22:59Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * Blog models an Etudes blog.
 */
public interface Blog
{
	/**
	 * Find the entry with this title.
	 * 
	 * @param title
	 *        The title.
	 * @return The entry, or null if not found.
	 */
	BlogEntry findEntryByTitle(String title);

	/**
	 * @return the user who created this.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * Get the blog entries.
	 * 
	 * @return The List of blog entries.
	 */
	List<BlogEntry> getEntries();

	/**
	 * @return The blog id.
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
	 * @return The name of the blog.
	 */
	String getName();

	/**
	 * @return The user owning the blog.
	 */
	User getOwner();

	/**
	 * Collect all the User objects referenced in the blog (created by, modified by, owner ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * @return The site in which the blog lives.
	 */
	Site getSite();

	/**
	 * Set the blog's name.
	 * 
	 * @param name
	 *        The new name.
	 */
	void setName(String name);

	/**
	 * Set the blog's owner.
	 * 
	 * @param owner
	 *        The new owner.
	 */
	void setOwner(User owner);
}
