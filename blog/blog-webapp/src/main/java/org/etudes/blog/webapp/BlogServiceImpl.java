/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-webapp/src/main/java/org/etudes/blog/webapp/BlogServiceImpl.java $
 * $Id: BlogServiceImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.blog.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.blog.api.Blog;
import org.etudes.blog.api.BlogEntry;
import org.etudes.blog.api.BlogService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.StudentContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * BlogServiceImpl implements BlogService.
 */
public class BlogServiceImpl implements BlogService, Service, SiteContentHandler, StudentContentHandler
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(BlogServiceImpl.class);

	/**
	 * Construct
	 */
	public BlogServiceImpl()
	{
		M_log.info("BlogServiceImpl: construct");
	}

	@Override
	public Blog add(User addedBy, Site inSite, User forUser)
	{
		BlogImpl rv = new BlogImpl();
		rv.initSite(inSite);
		rv.initOwner(forUser);
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());

		save(addedBy, rv);

		return rv;
	}

	@Override
	public BlogEntry addEntry(User addedBy, Blog blog)
	{
		BlogEntryImpl rv = new BlogEntryImpl();
		rv.initBlogId(blog.getId());
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());

		save(addedBy, rv);

		return rv;
	}

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		// the blogs
		List<Blog> source = findBySite(fromSite);
		for (Blog blog : source)
		{
			// make an artifact for the blog
			Artifact artifact = toArchive.newArtifact(Tool.blog, "blog");
			artifact.getProperties().put("createdbyId", blog.getCreatedBy().getId());
			artifact.getProperties().put("createdon", blog.getCreatedOn());
			artifact.getProperties().put("modifiedbyId", blog.getModifiedBy().getId());
			artifact.getProperties().put("modifiedon", blog.getModifiedOn());
			artifact.getProperties().put("id", blog.getId());
			artifact.getProperties().put("owner", blog.getOwner());
			artifact.getProperties().put("name", blog.getName());

			// Note: all file system references are to blog entries, not the blog.

			toArchive.archive(artifact);

			// the entries
			List<BlogEntry> entries = blog.getEntries();
			for (BlogEntry entry : entries)
			{
				artifact = toArchive.newArtifact(Tool.blog, "entry");
				artifact.getProperties().put("createdbyId", entry.getCreatedBy().getId());
				artifact.getProperties().put("createdon", entry.getCreatedOn());
				artifact.getProperties().put("modifiedbyId", entry.getModifiedBy().getId());
				artifact.getProperties().put("modifiedon", entry.getModifiedOn());
				artifact.getProperties().put("id", entry.getId());
				artifact.getProperties().put("title", entry.getTitle());
				artifact.getProperties().put("published", entry.getPublished());

				artifact.getProperties().put("blogId", blog.getId());
				artifact.getProperties().put("blogName", blog.getName());

				Reference ref = ((BlogEntryImpl) entry).getContentReference();
				if (ref != null) artifact.getProperties().put("content", ref.getFile());

				ref = entry.getImage();
				if (ref != null) artifact.getProperties().put("image", ref.getFile());

				List<Reference> refs = fileService().getReferences(new ToolItemReference(fromSite, Tool.blog, entry.getId()));
				artifact.addReferences(refs);

				toArchive.archive(artifact);
			}
		}
	}

	@Override
	public void clear(Site site)
	{
		// TODO: remove blogs and entries for non instructors
	}

	@Override
	public void clear(Site site, User user)
	{
		// TODO: remove blogs and entries for user
	}

	@Override
	public List<Blog> findBySite(Site inSite)
	{
		List<Blog> rv = findBySiteTx(inSite);
		return rv;
	}

	@Override
	public List<Blog> findByUser(Site inSite, User user)
	{
		List<Blog> rv = findByUserTx(inSite, user);
		return rv;
	}

	@Override
	public Blog get(Long id)
	{
		Blog rv = readBlogTx(id);
		return rv;
	}

	@Override
	public List<BlogEntry> getEntries(Blog blog)
	{
		List<BlogEntry> rv = readEntriesTx(blog.getId());
		return rv;
	}

	@Override
	public BlogEntry getEntry(Long id)
	{
		BlogEntry rv = readEntryTx(id);
		return rv;
	}

	@Override
	public void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser)
	{
		// see what we already have
		List<Blog> dest = findBySite(intoSite);

		Membership members = rosterService().getActiveSiteMembers(intoSite);
		List<Member> instructors = members.findRole(Role.instructor);
		Member owner = null;
		if (!instructors.isEmpty()) owner = instructors.get(0);

		if (fromArtifact.getType().equals("blog"))
		{
			// skip any that have a name conflict
			String name = (String) fromArtifact.getProperties().get("name");
			if (findByName(dest, name) == null)
			{
				// create
				BlogImpl newBlog = new BlogImpl();
				newBlog.initSite(intoSite);
				newBlog.initOwner(owner.getUser());
				newBlog.initName(name);
				// TODO: preserve, or make new?
				newBlog.initCreatedBy(importingUser);
				newBlog.initCreatedOn(new Date());
				newBlog.initModifiedBy(importingUser);
				newBlog.initModifiedOn(newBlog.getCreatedOn());

				// the source id
				// Long id = (Long) fromArtifact.getProperties().get("id");

				// save the blog
				save(importingUser, newBlog);
			}
		}
		else if (fromArtifact.getType().equals("entry"))
		{
			// check the named blog
			// Long blogId = (Long) fromArtifact.getProperties().get("blogId");
			String blogName = (String) fromArtifact.getProperties().get("blogName");
			Blog blog = findByName(dest, blogName);
			if (blog != null)
			{
				// check for title conflict
				String title = (String) fromArtifact.getProperties().get("title");
				BlogEntry entry = blog.findEntryByTitle(title);
				if (entry == null)
				{
					BlogEntryImpl newEntry = new BlogEntryImpl();

					newEntry.initBlogId(blog.getId());
					// TODO: preserve, or make new?
					newEntry.initCreatedBy(importingUser);
					newEntry.initCreatedOn(new Date());
					newEntry.initModifiedBy(importingUser);
					newEntry.initModifiedOn(newEntry.getCreatedOn());
					newEntry.initTitle(title);
					newEntry.setPublished((Boolean) fromArtifact.getProperties().get("published"));

					doSave(importingUser, newEntry);

					File content = (File) fromArtifact.getProperties().get("content");
					if (content != null) newEntry.shareContent(content);

					File image = (File) fromArtifact.getProperties().get("image");
					if (image != null) newEntry.shareImage(image);

					doSave(importingUser, newEntry);
				}
			}
		}
	}

	@Override
	public void importFromSite(Site fromSite, Site toSite, User importingUser)
	{
		// create blogs in the toSite to match the titles of the blogs in the fromSite. If the user is available, keep it, otherwise change it to the toSite's Instructor role user. Transfer entries, too.

		List<Blog> source = findBySite(fromSite);

		List<Blog> dest = findBySite(toSite);
		Membership members = rosterService().getActiveSiteMembers(toSite);
		List<Member> instructors = members.findRole(Role.instructor);
		if (instructors.isEmpty()) instructors = null;

		for (Blog blog : source)
		{
			// if the to site has a blog with this name, skip
			if (findBlogNamed(dest, blog.getName()) != null) continue;

			// for owner?
			User owner = null;
			Member membership = members.findUser(blog.getOwner());
			if (membership != null)
			{
				if (membership.getRole().ge(Role.student))
				{
					owner = membership.getUser();
				}
			}
			else if (instructors != null)
			{
				owner = instructors.get(0).getUser();
			}
			if (owner == null) continue;

			// create
			BlogImpl newBlog = new BlogImpl();
			newBlog.initSite(toSite);
			newBlog.initOwner(owner);
			newBlog.initName(blog.getName());
			// TODO: preserve, or make new?
			newBlog.initCreatedBy(importingUser);
			newBlog.initCreatedOn(new Date());
			newBlog.initModifiedBy(importingUser);
			newBlog.initModifiedOn(newBlog.getCreatedOn());

			save(importingUser, newBlog);

			// add to the toSite's blogs
			dest.add(newBlog);

			// TODO: entries - we may NOT want to bring over entries
			List<BlogEntry> entries = blog.getEntries();
			for (BlogEntry entry : entries)
			{
				// Note: the blog in dest is new, so we can bring over all entries
				// TODO: merge entries for existing blogs?
				BlogEntryImpl newEntry = new BlogEntryImpl();
				newEntry.initBlogId(newBlog.getId());
				// TODO: preserve, or make new?
				newEntry.initCreatedBy(entry.getCreatedBy());
				newEntry.initCreatedOn(entry.getCreatedOn());
				newEntry.initModifiedBy(entry.getModifiedBy());
				newEntry.initModifiedOn(entry.getModifiedOn());
				newEntry.initPublished(entry.getPublished());
				newEntry.initTitle(entry.getTitle());

				// save once, to get the id for references to be set with content and image
				doSave(importingUser, newEntry);

				// for content, make a new reference to the same file
				Reference ref = ((BlogEntryImpl) entry).getContentReference();
				if (ref != null)
				{
					ref = fileService().add(ref.getFile(), newEntry.getReference(), Role.none);
					newEntry.initContentReferenceId(ref.getId());
				}

				// for the image, make a new reference to the same file
				ref = entry.getImage();
				if (ref != null)
				{
					ref = fileService().add(ref.getFile(), newEntry.getReference(), Role.guest);
					newEntry.initImageReferenceId(ref.getId());
				}

				doSave(importingUser, newEntry);
			}
		}
	}

	@Override
	public void purge(Site site)
	{
		List<Blog> blogs = findBySite(site);
		for (Blog blog : blogs)
		{
			remove(blog);
		}
	}

	@Override
	public void remove(final Blog blog)
	{
		// first the entries
		List<BlogEntry> entries = blog.getEntries();
		for (BlogEntry entry : entries)
		{
			removeEntry(entry);
		}

		// now the blog
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeBlogTx(blog);
			}
		}, "remove(blog)");
	}

	@Override
	public void remove(User removedBy, BlogEntry blogEntry)
	{
		// update the blog's modified by/on
		BlogImpl blog = (BlogImpl) blogEntry.getBlog();
		blog.initModifiedBy(removedBy);
		blog.initModifiedOn(new Date());
		save(removedBy, blog);

		removeEntry(blogEntry);
	}

	@Override
	public void save(User savedBy, Blog blog)
	{
		if (((BlogImpl) blog).isChanged() || blog.getId() == null)
		{
			if (((BlogImpl) blog).isChanged())
			{
				// set modified by/on
				((BlogImpl) blog).initModifiedBy(savedBy);
				((BlogImpl) blog).initModifiedOn(new Date());
			}

			doSave(blog);
		}
	}

	@Override
	public void save(User savedBy, BlogEntry entry)
	{
		if (((BlogEntryImpl) entry).isChanged() || entry.getId() == null)
		{
			if (((BlogEntryImpl) entry).isChanged())
			{
				// set modified by/on
				((BlogEntryImpl) entry).initModifiedBy(savedBy);
				((BlogEntryImpl) entry).initModifiedOn(new Date());
			}

			doSave(savedBy, entry);
		}
	}

	@Override
	public boolean start()
	{
		M_log.info("BlogServiceImpl: start");
		return true;
	}

	/**
	 * Do the save.
	 * 
	 * @param blog
	 *        The blog to save.
	 */
	protected void doSave(final Blog blog)
	{
		if (((BlogImpl) blog).isChanged() || blog.getId() == null)
		{
			// insert or update
			if (blog.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertBlogTx((BlogImpl) blog);

					}
				}, "doSave(insert blog)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						updateBlogTx((BlogImpl) blog);

					}
				}, "doSave(update blog)");
			}

			((BlogImpl) blog).clearChanged();
		}
	}

	protected void doSave(User savedBy, final BlogEntry entry)
	{
		if (((BlogEntryImpl) entry).isChanged() || entry.getId() == null)
		{
			if (((BlogEntryImpl) entry).isChanged())
			{
				// deal with the content and image
				((BlogEntryImpl) entry).saveContentAndImage(savedBy);
			}

			// insert or update
			if (entry.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertEntryTx((BlogEntryImpl) entry);

					}
				}, "save(insert entry)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						updateEntryTx((BlogEntryImpl) entry);

					}
				}, "save(update entry)");
			}

			((BlogEntryImpl) entry).clearChanged();
		}
	}

	/**
	 * Find a blog with this name in the list provided. Case insensitive.
	 * 
	 * @param blogs
	 *        The list of blogs to search.
	 * @param name
	 *        The name to search for.
	 * @return The blog found that matches the name, or null if none found.
	 */
	protected Blog findBlogNamed(List<Blog> blogs, String name)
	{
		for (Blog blog : blogs)
		{
			if (blog.getName().equalsIgnoreCase(name)) return blog;
		}

		return null;
	}

	protected Blog findByName(List<Blog> blogs, String name)
	{
		for (Blog b : blogs)
		{
			if (b.getName().equals(name)) return b;
		}

		return null;
	}

	/**
	 * Transaction code for reading the blogs for a site.
	 * 
	 * @param siteId
	 *        the site id.
	 */
	protected List<Blog> findBySiteTx(final Site site)
	{
		String sql = "SELECT ID, OWNER, NAME, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM BLOG WHERE SITE = ?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<Blog> rv = sqlService().select(sql, fields, new SqlService.Reader<Blog>()
		{
			@Override
			public Blog read(ResultSet result)
			{
				BlogImpl blog = new BlogImpl();
				blog.initSite(site);
				try
				{
					int i = 1;
					blog.initId(sqlService().readLong(result, i++));
					blog.initOwner(userService().wrap(sqlService().readLong(result, i++)));
					blog.initName(sqlService().readString(result, i++));
					blog.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					blog.initCreatedOn(sqlService().readDate(result, i++));
					blog.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					blog.initModifiedOn(sqlService().readDate(result, i++));

					return blog;
				}
				catch (SQLException e)
				{
					M_log.warn("findBySiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading the blogs for a user in a site.
	 * 
	 * @param siteId
	 *        the site id.
	 * @param userId
	 *        The user id.
	 */
	protected List<Blog> findByUserTx(final Site site, final User user)
	{
		String sql = "SELECT ID, NAME, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM BLOG WHERE SITE = ? AND OWNER = ?";
		Object[] fields = new Object[2];
		fields[0] = site.getId();
		fields[1] = user.getId();
		List<Blog> rv = sqlService().select(sql, fields, new SqlService.Reader<Blog>()
		{
			@Override
			public Blog read(ResultSet result)
			{
				BlogImpl blog = new BlogImpl();
				blog.initSite(site);
				blog.initOwner(user);
				try
				{
					int i = 1;
					blog.initId(sqlService().readLong(result, i++));
					blog.initName(sqlService().readString(result, i++));
					blog.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					blog.initCreatedOn(sqlService().readDate(result, i++));
					blog.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					blog.initModifiedOn(sqlService().readDate(result, i++));

					return blog;
				}
				catch (SQLException e)
				{
					M_log.warn("findByUserTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for inserting a blog.
	 * 
	 * @param blog
	 *        The blog.
	 */
	protected void insertBlogTx(BlogImpl blog)
	{
		String sql = "INSERT INTO BLOG (SITE, OWNER, NAME, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)" + " VALUES (?,?,?,?,?,?,?)";

		Object[] fields = new Object[7];
		int i = 0;
		fields[i++] = blog.getSite().getId();
		fields[i++] = blog.getOwner().getId();
		fields[i++] = blog.getName();
		fields[i++] = blog.getCreatedBy().getId();
		fields[i++] = blog.getCreatedOn();
		fields[i++] = blog.getModifiedBy().getId();
		fields[i++] = blog.getModifiedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		blog.initId(id);
	}

	/**
	 * Transaction code for inserting a blog entry.
	 * 
	 * @param entry
	 *        The blog entry.
	 */
	protected void insertEntryTx(BlogEntryImpl entry)
	{
		String sql = "INSERT INTO BLOG_ENTRY (BLOG_ID, TITLE, CONTENT, IMAGE, PUBLISHED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
				+ " VALUES (?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[9];
		int i = 0;
		fields[i++] = entry.getBlogId();
		fields[i++] = entry.getTitle();
		fields[i++] = entry.getContentReferenceId();
		fields[i++] = entry.getImageReferenceId();
		fields[i++] = entry.getPublished();
		fields[i++] = entry.getCreatedBy().getId();
		fields[i++] = entry.getCreatedOn();
		fields[i++] = entry.getModifiedBy().getId();
		fields[i++] = entry.getModifiedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		entry.initId(id);
	}

	/**
	 * Transaction code for reading a blog.
	 * 
	 * @param id
	 *        The blog id.
	 */
	protected Blog readBlogTx(final Long id)
	{
		String sql = "SELECT SITE, OWNER, NAME, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM BLOG WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<Blog> rv = sqlService().select(sql, fields, new SqlService.Reader<Blog>()
		{
			@Override
			public Blog read(ResultSet result)
			{
				BlogImpl blog = new BlogImpl();
				blog.initId(id);
				try
				{
					int i = 1;
					blog.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					blog.initOwner(userService().wrap(sqlService().readLong(result, i++)));
					blog.initName(sqlService().readString(result, i++));
					blog.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					blog.initCreatedOn(sqlService().readDate(result, i++));
					blog.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					blog.initModifiedOn(sqlService().readDate(result, i++));

					return blog;
				}
				catch (SQLException e)
				{
					M_log.warn("readBlogTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	protected List<BlogEntry> readEntriesTx(final Long blogId)
	{
		// TODO: sort
		String sql = "SELECT ID, TITLE, CONTENT, IMAGE, PUBLISHED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM BLOG_ENTRY WHERE BLOG_ID=?";
		Object[] fields = new Object[1];
		fields[0] = blogId;
		List<BlogEntry> rv = sqlService().select(sql, fields, new SqlService.Reader<BlogEntry>()
		{
			@Override
			public BlogEntry read(ResultSet result)
			{
				BlogEntryImpl entry = new BlogEntryImpl();
				entry.initBlogId(blogId);
				try
				{
					int i = 1;

					entry.initId(sqlService().readLong(result, i++));
					entry.initTitle(sqlService().readString(result, i++));
					entry.initContentReferenceId(sqlService().readLong(result, i++));
					entry.initImageReferenceId(sqlService().readLong(result, i++));
					entry.initPublished(sqlService().readBoolean(result, i++));
					entry.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initCreatedOn(sqlService().readDate(result, i++));
					entry.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initModifiedOn(sqlService().readDate(result, i++));

					return entry;
				}
				catch (SQLException e)
				{
					M_log.warn("readBlogEntriesTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a blog entry.
	 * 
	 * @param id
	 *        The blog entry id.
	 */
	protected BlogEntry readEntryTx(final Long id)
	{
		String sql = "SELECT BLOG_ID, TITLE, CONTENT, IMAGE, PUBLISHED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM BLOG_ENTRY WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<BlogEntry> rv = sqlService().select(sql, fields, new SqlService.Reader<BlogEntry>()
		{
			@Override
			public BlogEntry read(ResultSet result)
			{
				BlogEntryImpl entry = new BlogEntryImpl();
				entry.initId(id);
				try
				{
					int i = 1;

					entry.initBlogId(sqlService().readLong(result, i++));
					entry.initTitle(sqlService().readString(result, i++));
					entry.initContentReferenceId(sqlService().readLong(result, i++));
					entry.initImageReferenceId(sqlService().readLong(result, i++));
					entry.initPublished(sqlService().readBoolean(result, i++));
					entry.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initCreatedOn(sqlService().readDate(result, i++));
					entry.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initModifiedOn(sqlService().readDate(result, i++));

					return entry;
				}
				catch (SQLException e)
				{
					M_log.warn("readEntryTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for removing a blog.
	 * 
	 * @param blog
	 *        The blog.
	 */
	protected void removeBlogTx(Blog blog)
	{
		String sql = "DELETE FROM BLOG WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = blog.getId();
		sqlService().update(sql, fields);
		((BlogImpl) blog).initId(null);
	}

	/**
	 * Do the work of removing a blog entry.
	 * 
	 * @param blogEntry
	 *        The blog entry to remove.
	 */
	protected void removeEntry(final BlogEntry blogEntry)
	{
		// deal with the image and content - remove all our references
		fileService().removeExcept(blogEntry.getReference(), null);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeEntryTx(blogEntry);
			}
		}, "removeEntry");
	}

	/**
	 * Transaction code for removing a blog entry.
	 * 
	 * @param entry
	 *        The blog entry.
	 */
	protected void removeEntryTx(BlogEntry entry)
	{
		String sql = "DELETE FROM BLOG_ENTRY WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = entry.getId();
		sqlService().update(sql, fields);
		((BlogEntryImpl) entry).initId(null);
	}

	/**
	 * Transaction code for updating an existing blog.
	 * 
	 * @param blog
	 *        The blog.
	 */
	protected void updateBlogTx(BlogImpl blog)
	{
		String sql = "UPDATE BLOG SET SITE=?, OWNER=?, NAME=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";

		Object[] fields = new Object[8];
		int i = 0;
		fields[i++] = blog.getSite().getId();
		fields[i++] = blog.getOwner().getId();
		fields[i++] = blog.getName();
		fields[i++] = blog.getCreatedBy().getId();
		fields[i++] = blog.getCreatedOn();
		fields[i++] = blog.getModifiedBy().getId();
		fields[i++] = blog.getModifiedOn();

		fields[i++] = blog.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for updating an existing blog entry.
	 * 
	 * @param entry
	 *        The blog entry.
	 */
	protected void updateEntryTx(BlogEntryImpl entry)
	{
		String sql = "UPDATE BLOG_ENTRY SET BLOG_ID=?, TITLE=?, CONTENT=?, IMAGE=?, PUBLISHED=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";

		Object[] fields = new Object[10];
		int i = 0;
		fields[i++] = entry.getBlogId();
		fields[i++] = entry.getTitle();
		fields[i++] = entry.getContentReferenceId();
		fields[i++] = entry.getImageReferenceId();
		fields[i++] = entry.getPublished();
		fields[i++] = entry.getCreatedBy().getId();
		fields[i++] = entry.getCreatedOn();
		fields[i++] = entry.getModifiedBy().getId();
		fields[i++] = entry.getModifiedOn();

		fields[i++] = entry.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}

	/**
	 * @return The registered SqlService.
	 */
	private SqlService sqlService()
	{
		return (SqlService) Services.get(SqlService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
