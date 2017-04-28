/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-webapp/src/main/java/org/etudes/blog/webapp/BlogCdpHandler.java $
 * $Id: BlogCdpHandler.java 10060 2015-02-11 22:02:24Z ggolden $
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

package org.etudes.blog.webapp;

import static org.etudes.util.StringUtil.split;
import static org.etudes.util.StringUtil.trimToNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.blog.api.Blog;
import org.etudes.blog.api.BlogEntry;
import org.etudes.blog.api.BlogService;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 */
public class BlogCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(BlogCdpHandler.class);

	public String getPrefix()
	{
		return "blog";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		if (authenticatedUser == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}

		else if (requestPath.equals("getSiteBlogs"))
		{
			return dispatchGetSiteBlogs(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("add"))
		{
			return dispatchAdd(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("update"))
		{
			return dispatchUpdate(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("remove"))
		{
			return dispatchRemove(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("getBlog"))
		{
			return dispatchGetBlog(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("addEntry"))
		{
			return dispatchAddEntry(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("updateEntry"))
		{
			return dispatchUpdateEntry(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("removeEntry"))
		{
			return dispatchRemoveEntry(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("getEntry"))
		{
			return dispatchGetBlogEntry(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchAdd(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// we won't need to get() the site, we can just wrap() it to encapsulate the id in a Site object.
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchAdd: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		String name = cdpService().readString(parameters.get("name"));

		// we don't need this user's details - existence will be checked when we check that the user is authorized to have a blog in the site
		Long ownerId = cdpService().readLong(parameters.get("owner"));
		if (ownerId == null)
		{
			M_log.warn("dispatchAdd: missing owner");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		User owner = userService().wrap(ownerId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// the owner must have student "or higher" role in the site
		if (!rosterService().userRoleInSite(owner, site).ge(Role.student))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Blog blog = blogService().add(authenticatedUser, site, owner);

		if (name != null) blog.setName(name);

		blogService().save(authenticatedUser, blog);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchAddEntry(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long blogId = cdpService().readLong(parameters.get("blog"));
		if (blogId == null)
		{
			M_log.warn("dispatchAddEntry: missing blogId");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Blog blog = blogService().get(blogId);
		if (blog == null)
		{
			M_log.warn("dispatchAddEntry: missing blog: " + blogId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		FileItem image = null;
		Object o = parameters.get("image");
		if ((o != null) && (o instanceof FileItem))
		{
			image = (FileItem) o;
		}
		if ((image != null) && (image.getSize() > 0))
		{
			// make sure we got an image file
			if (!image.getContentType().startsWith("image/"))
			{
				M_log.warn("dispatchAddEntry: image file not image: " + image.getContentType());
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		Long galleryImageRefId = cdpService().readLong(parameters.get("galleryImage"));
		Reference galleryImage = null;
		if (galleryImageRefId != null)
		{
			galleryImage = fileService().getReference(galleryImageRefId);
		}

		Boolean imageRemove = cdpService().readBoolean(parameters.get("imageRemove"));
		Boolean published = cdpService().readBoolean(parameters.get("published"));

		// security: the authenticated user must match the owner of the blog
		if (!blog.getOwner().equals(authenticatedUser))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		BlogEntry entry = blogService().addEntry(authenticatedUser, blog);

		String title = cdpService().readString(parameters.get("title"));
		String content = cdpService().readString(parameters.get("content"));

		if ((imageRemove != null) && imageRemove)
		{
			entry.removeImage();
		}
		else if (image != null)
		{
			entry.setImage(image.getName(), (int) image.getSize(), image.getContentType(), image.getInputStream());
		}
		else if (galleryImage != null)
		{
			entry.setImage(galleryImage);
		}

		if (title != null) entry.setTitle(trimToNull(title));
		if (content != null) entry.setContent(trimToNull(content), true);
		if (published != null) entry.setPublished(published);

		blogService().save(authenticatedUser, entry);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGetBlog(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long blogId = cdpService().readLong(parameters.get("id"));
		if (blogId == null)
		{
			M_log.warn("dispatchGetBlog: missing id");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Blog blog = blogService().get(blogId);
		if (blog == null)
		{
			M_log.warn("dispatchGetBlog: missing blog: " + blogId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: the authenticated user must have some role (guest or higher) in the site.
		Role authenticatedRole = rosterService().userRoleInSite(authenticatedUser, blog.getSite());
		if (!authenticatedRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// we will need the users from the blog and the entries
		Set<User> users = new HashSet<User>();
		users.addAll(blog.getReferencedUsers());
		for (BlogEntry entry : blog.getEntries())
		{
			users.addAll(entry.getReferencedUsers());
		}
		userService().willNeed(users);

		Map<String, Object> blogMap = new HashMap<String, Object>();
		rv.put("blog", blogMap);

		blogMap.put("id", blog.getId().toString());
		blogMap.put("site", blog.getSite().getName());
		blogMap.put("createdBy", blog.getCreatedBy() == null ? "SYSTEM" : blog.getCreatedBy().getNameDisplay());
		if (blog.getCreatedOn() != null) blogMap.put("createdOn", cdpService().sendDate(blog.getCreatedOn()));
		blogMap.put("name", blog.getName());
		blogMap.put("owner_id", blog.getOwner().getId().toString());
		blogMap.put("owner_nameDisplay", blog.getOwner().getNameDisplay());
		blogMap.put("owner_nameSort", blog.getOwner().getNameSort());
		blogMap.put("modifiedBy", blog.getModifiedBy() == null ? "SYSTEM" : blog.getModifiedBy().getNameDisplay());
		if (blog.getModifiedOn() != null) blogMap.put("modifiedOn", cdpService().sendDate(blog.getModifiedOn()));

		Reference avatar = blog.getOwner().getAvatar();
		if (avatar != null)
		{
			String downloadUrl = avatar.getDownloadUrl();
			if (downloadUrl != null)
			{
				blogMap.put("owner_avatar", downloadUrl);
			}
		}

		// the blog entries
		List<Map<String, Object>> entriesMap = new ArrayList<Map<String, Object>>();
		blogMap.put("entries", entriesMap);

		for (BlogEntry entry : blog.getEntries())
		{
			Map<String, Object> entryMap = new HashMap<String, Object>();
			entriesMap.add(entryMap);

			entryMap.put("id", entry.getId().toString());
			entryMap.put("title", entry.getTitle());
			entryMap.put("createdBy", entry.getCreatedBy() == null ? "SYSTEM" : entry.getCreatedBy().getNameDisplay());
			if (entry.getCreatedOn() != null) entryMap.put("createdOn", cdpService().sendDate(entry.getCreatedOn()));
			entryMap.put("modifiedBy", entry.getModifiedBy() == null ? "SYSTEM" : entry.getModifiedBy().getNameDisplay());
			if (entry.getModifiedOn() != null) entryMap.put("modifiedOn", cdpService().sendDate(entry.getModifiedOn()));

			// content - no

			Reference image = entry.getImage();
			if (image != null)
			{
				String downloadUrl = image.getDownloadUrl();
				if (downloadUrl != null)
				{
					entryMap.put("image", downloadUrl);
				}
			}
		}

		// permissions - the owner or user with instructor or better
		Map<String, Object> permissionsMap = new HashMap<String, Object>();
		rv.put("permissions", permissionsMap);
		permissionsMap.put("mayEdit", blog.getOwner().equals(authenticatedUser) || authenticatedRole.ge(Role.instructor));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGetBlogEntry(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long blogEntryId = cdpService().readLong(parameters.get("id"));
		if (blogEntryId == null)
		{
			M_log.warn("dispatchGetBlogEntry: missing id");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		BlogEntry entry = blogService().getEntry(blogEntryId);
		if (entry == null)
		{
			M_log.warn("dispatchGetBlogEntry: missing entry: " + blogEntryId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: the authenticated user must have some role (guest or higher) in the site.
		if (!rosterService().userRoleInSite(authenticatedUser, entry.getBlog().getSite()).ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// we will need the users from the blog and the entries
		userService().willNeed(entry.getReferencedUsers());

		Map<String, Object> entryMap = new HashMap<String, Object>();
		rv.put("entry", entryMap);

		entryMap.put("id", entry.getId().toString());
		entryMap.put("title", entry.getTitle());
		entryMap.put("createdBy", entry.getCreatedBy() == null ? "SYSTEM" : entry.getCreatedBy().getNameDisplay());
		if (entry.getCreatedOn() != null) entryMap.put("createdOn", cdpService().sendDate(entry.getCreatedOn()));
		entryMap.put("modifiedBy", entry.getModifiedBy() == null ? "SYSTEM" : entry.getModifiedBy().getNameDisplay());
		if (entry.getModifiedOn() != null) entryMap.put("modifiedOn", cdpService().sendDate(entry.getModifiedOn()));

		Reference image = entry.getImage();
		if (image != null)
		{
			String downloadUrl = image.getDownloadUrl();
			if (downloadUrl != null)
			{
				entryMap.put("image", downloadUrl);
			}
		}

		entryMap.put("content", entry.getContent(true));

		// rv.put("myfiles", myfilesService().respondMyFiles(authenticatedUser));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGetSiteBlogs(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		Site site = siteService().wrap(siteId);
		if (siteId == null)
		{
			M_log.warn("dispatchGetSiteBlogs: missing site: " + parameters.get("site"));
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: the authenticated user must have some role (guest or higher) in the site.
		Role authenticatedRole = rosterService().userRoleInSite(authenticatedUser, site);
		if (!authenticatedRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> blogsMap = new ArrayList<Map<String, Object>>();
		rv.put("blogs", blogsMap);

		List<Blog> blogs = blogService().findBySite(site);

		// we will need the users from all these blogs
		Set<User> users = new HashSet<User>();
		for (Blog blog : blogs)
		{
			users.addAll(blog.getReferencedUsers());
		}
		userService().willNeed(users);

		for (Blog blog : blogs)
		{
			Map<String, Object> blogMap = new HashMap<String, Object>();
			blogsMap.add(blogMap);

			blogMap.put("id", blog.getId());
			blogMap.put("site", blog.getSite().getName());
			blogMap.put("createdBy", blog.getCreatedBy() == null ? "SYSTEM" : blog.getCreatedBy().getNameDisplay());
			if (blog.getCreatedOn() != null) blogMap.put("createdOn", cdpService().sendDate(blog.getCreatedOn()));
			blogMap.put("name", blog.getName());
			blogMap.put("owner_id", blog.getOwner().getId());
			blogMap.put("owner_nameDisplay", blog.getOwner().getNameDisplay());
			blogMap.put("owner_nameSort", blog.getOwner().getNameSort());
			blogMap.put("modifiedBy", blog.getModifiedBy() == null ? "SYSTEM" : blog.getModifiedBy().getNameDisplay());
			if (blog.getModifiedOn() != null) blogMap.put("modifiedOn", cdpService().sendDate(blog.getModifiedOn()));

			Reference avatar = blog.getOwner().getAvatar();
			if (avatar != null)
			{
				String downloadUrl = avatar.getDownloadUrl();
				if (downloadUrl != null)
				{
					blogMap.put("owner_avatar", downloadUrl);
				}
			}
		}

		// we also need the site roster - user id and name(s) for all active users in the site
		// TODO: sort by sort name
		List<Map<String, Object>> membersMap = new ArrayList<Map<String, Object>>();
		rv.put("members", membersMap);
		Membership members = rosterService().getActiveSiteMembers(site);
		for (Member membership : members.getMembers())
		{
			Map<String, Object> memberMap = new HashMap<String, Object>();
			membersMap.add(memberMap);

			memberMap.put("userId", membership.getUser().getId());
			memberMap.put("nameSort", membership.getUser().getNameSort());
			memberMap.put("role", membership.getRole().name());
		}

		// and permissions based on the authenticated user
		Map<String, Object> permissionsMap = new HashMap<String, Object>();
		rv.put("permissions", permissionsMap);
		permissionsMap.put("mayEdit", authenticatedRole.ge(Role.instructor));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemove(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String ids = (String) parameters.get("ids");
		String[] idStrs = split(ids, "\t");

		List<Blog> blogsToRemove = new ArrayList<Blog>();
		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			Blog blog = blogService().get(id);

			// security: authenticatedUser must have a role of instructor "or higher" in the site
			if (!rosterService().userRoleInSite(authenticatedUser, blog.getSite()).ge(Role.instructor))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
			blogsToRemove.add(blog);
		}

		for (Blog blog : blogsToRemove)
		{
			blogService().remove(blog);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemoveEntry(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		String ids = (String) parameters.get("ids");
		if (ids == null)
		{
			M_log.warn("dispatchRemoveEntry: missing ids");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		String[] idStrs = split(ids, "\t");

		List<BlogEntry> entriesToRemove = new ArrayList<BlogEntry>();
		for (String idStr : idStrs)
		{
			Long id = Long.valueOf(idStr);
			BlogEntry entry = blogService().getEntry(id);

			// security: the authenticated user must match the owner of the blog
			if (!entry.getBlog().getOwner().equals(authenticatedUser))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}

			entriesToRemove.add(entry);
		}

		for (BlogEntry entry : entriesToRemove)
		{
			blogService().remove(authenticatedUser, entry);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUpdate(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long id = cdpService().readLong(parameters.get("id"));
		if (id == null)
		{
			M_log.warn("dispatchUpdate: missing id");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Blog blog = blogService().get(id);
		if (blog == null)
		{
			M_log.warn("dispatchUpdate: missing blog: " + id);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: the authenticated user must match the owner of the blog, or have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, blog.getSite()).ge(Role.instructor)) if (!blog.getOwner().equals(authenticatedUser))
		{
			if (!rosterService().userRoleInSite(authenticatedUser, blog.getSite()).ge(Role.instructor))
			{
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
		}

		// we don't need this user's details - existence will be checked when we check that the user is authorized to have a blog in the site
		Long ownerId = cdpService().readLong(parameters.get("owner"));
		if (ownerId == null)
		{
			M_log.warn("dispatchAdd: missing owner");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		User owner = userService().wrap(ownerId);

		// the owner must have student "or higher" role in the site
		if (!rosterService().userRoleInSite(owner, blog.getSite()).ge(Role.student))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		String name = cdpService().readString(parameters.get("name"));

		if (name != null) blog.setName(trimToNull(name));
		if (owner != null) blog.setOwner(owner);

		blogService().save(authenticatedUser, blog);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUpdateEntry(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long id = cdpService().readLong(parameters.get("id"));
		if (id == null)
		{
			M_log.warn("dispatchUpdateEntry: missing id");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		BlogEntry entry = blogService().getEntry(id);
		if (entry == null)
		{
			M_log.warn("dispatchUpdateEntry: missing entry: " + id);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// security: the authenticated user must match the owner of the blog
		if (!entry.getBlog().getOwner().equals(authenticatedUser))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		String title = cdpService().readString(parameters.get("title"));
		String content = cdpService().readString(parameters.get("content"));

		FileItem image = null;
		Object o = parameters.get("image");
		if ((o != null) && (o instanceof FileItem))
		{
			image = (FileItem) o;
		}
		if ((image != null) && (image.getSize() > 0))
		{
			// make sure we got an image file
			if (!image.getContentType().startsWith("image/"))
			{
				M_log.warn("dispatchAddEntry: image file not image: " + image.getContentType());
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		Boolean imageRemove = cdpService().readBoolean(parameters.get("imageRemove"));
		Boolean published = cdpService().readBoolean(parameters.get("published"));

		Long galleryImageRefId = cdpService().readLong(parameters.get("galleryImage"));
		Reference galleryImage = null;
		if (galleryImageRefId != null)
		{
			galleryImage = fileService().getReference(galleryImageRefId);
		}

		if (title != null) entry.setTitle(trimToNull(title));
		if (content != null) entry.setContent(trimToNull(content), true);
		if (published != null) entry.setPublished(published);

		if ((imageRemove != null) && imageRemove)
		{
			entry.removeImage();
		}
		else if (image != null)
		{
			entry.setImage(image.getName(), (int) image.getSize(), image.getContentType(), image.getInputStream());
		}
		else if (galleryImage != null)
		{
			entry.setImage(galleryImage);
		}

		blogService().save(authenticatedUser, entry);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	/**
	 * @return The registered BlogService.
	 */
	private BlogService blogService()
	{
		return (BlogService) Services.get(BlogService.class);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
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
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
