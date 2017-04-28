/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-api/src/main/java/org/etudes/blog/api/BlogService.java $
 * $Id: BlogService.java 9906 2015-01-23 22:42:08Z ggolden $
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

import java.util.List;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * The E3 Blog service.
 */
public interface BlogService
{
	/**
	 * Create a new blog.
	 * 
	 * @param addedBy
	 *        The user doing the adding.
	 * @param inSite
	 *        The site holding the blog.
	 * @param forUser
	 *        The user owning the blog.
	 * @return The added Blog.
	 */
	Blog add(User addedBy, Site inSite, User forUser);

	/**
	 * Add an entry to the blog.
	 * 
	 * @param addedBy
	 *        The user doing the adding.
	 * @param blog
	 *        The blog.
	 * @return The new blog entry.
	 */
	BlogEntry addEntry(User addedBy, Blog blog);

	/**
	 * Find the blog(s) in this site.
	 * 
	 * @param inSite
	 *        The site holding the blog.
	 * @return The List of Blogs in this site, possibly empty.
	 */
	List<Blog> findBySite(Site inSite);

	/**
	 * Find the blog(s) owned by this user in this site.
	 * 
	 * @param inSite
	 *        The site holding the blog.
	 * @param user
	 *        The owning user.
	 * @return The List of Blogs owned by this user, possibly empty.
	 */
	List<Blog> findByUser(Site inSite, User user);

	/**
	 * Get the Blog with this id.
	 * 
	 * @param id
	 *        The blog id.
	 * @return The Blog with this id, or null if not found.
	 */
	Blog get(Long id);

	/**
	 * Get the entries from this blog. Can also call blog.getEntries().
	 * 
	 * @param blog
	 *        The blog.
	 * @return The List of BlogEntry, possibly empty.
	 */
	List<BlogEntry> getEntries(Blog blog);

	/**
	 * Get a blog entry.
	 * 
	 * @param id
	 *        The blog entry id.
	 * @return The BlogEntry, or null if not found.
	 */
	BlogEntry getEntry(Long id);

	/**
	 * Remove this blog.
	 * 
	 * @param blog
	 *        The blog.
	 */
	void remove(Blog blog);

	/**
	 * Remove this entry from it's blog
	 * 
	 * @param removedBy
	 *        The user doing the removing.
	 * @param entry
	 *        The entry to remove.
	 */
	void remove(User removedBy, BlogEntry entry);

	/**
	 * Save any changes made to this blog. Be sure to separately save any changes made to blog entries using save( ... BlogEntry).
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param blog
	 *        The blog to save.
	 */
	void save(User savedBy, Blog blog);

	/**
	 * Save any changes made to this blog entry.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param entry
	 *        The blog entry to save.
	 */
	void save(User savedBy, BlogEntry entry);
}
