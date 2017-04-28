/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-webapp/src/main/java/org/etudes/blog/webapp/BlogEntryImpl.java $
 * $Id: BlogEntryImpl.java 11578 2015-09-07 00:43:58Z ggolden $
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

import static org.etudes.util.Different.different;
import static org.etudes.util.StringUtil.trimToNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.blog.api.Blog;
import org.etudes.blog.api.BlogEntry;
import org.etudes.blog.api.BlogService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * BlogEntry implementation.
 */
public class BlogEntryImpl implements BlogEntry
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(BlogEntryImpl.class);

	protected Blog blog = null;

	protected Long blogId = null;

	protected boolean changed = false;

	protected Long contentReferenceId = null;

	protected User createdBy = null;

	protected Date createdOn = null;

	protected Long id = null;

	protected InputStream imageContent = null;

	protected Reference imageFile = null;

	protected String imageName = null;

	protected Long imageReferenceId = null;

	protected boolean imageRemove = false;

	protected int imageSize = 0;

	protected String imageType = null;

	protected User modifiedBy = null;

	protected Date modifiedOn = null;

	protected String newContent = null;

	protected Boolean published = Boolean.FALSE;

	protected boolean removeContent = false;

	protected File sharedContent = null;

	// TODO: sharedImage and imageFile? need both?

	protected File sharedImage = null;

	protected String title = null;

	/**
	 * Construct
	 */
	public BlogEntryImpl()
	{
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof BlogEntryImpl)) return false;
		BlogEntryImpl other = (BlogEntryImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public Blog getBlog()
	{
		if (this.blog == null)
		{
			this.blog = blogService().get(this.blogId);
		}

		return this.blog;
	}

	@Override
	public String getContent(boolean forDownload)
	{
		Reference ref = getContentReference();
		if (ref != null)
		{
			String content = ref.getFile().readString();
			if (forDownload) content = fileService().processContentPlaceholderToDownload(content, getReference());

			return content;
		}

		return null;
	}

	@Override
	public User getCreatedBy()
	{
		return this.createdBy;
	}

	@Override
	public Date getCreatedOn()
	{
		return this.createdOn;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Reference getImage()
	{
		return fileService().getReference(this.imageReferenceId);
	}

	@Override
	public User getModifiedBy()
	{
		return this.modifiedBy;
	}

	@Override
	public Date getModifiedOn()
	{
		return this.modifiedOn;
	}

	@Override
	public Boolean getPublished()
	{
		return this.published;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(getBlog().getSite(), Tool.blog, this.id);
	}

	@Override
	public Set<User> getReferencedUsers()
	{
		Set<User> rv = new HashSet<User>();
		rv.add(getCreatedBy());
		rv.add(getModifiedBy());

		return rv;
	}

	@Override
	public String getTitle()
	{
		return this.title;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public void removeImage()
	{
		setImage(null, 0, null, null);
		imageRemove = true;
	}

	@Override
	public void setContent(String content, boolean downloadReferenceFormat)
	{
		content = trimToNull(content);

		// make sure signature ends up with embedded reference URLs in placeholder format
		if (downloadReferenceFormat) content = fileService().processContentDownloadToPlaceholder(content);

		if (different(content, this.getContent(false)))
		{
			this.newContent = content;
			this.removeContent = (content == null);
			this.sharedContent = null;
			this.changed = true;
		}
	}

	@Override
	public void setImage(Reference ref)
	{
		if (this.imageContent != null)
		{
			try
			{
				this.imageContent.close();
			}
			catch (IOException e)
			{
				M_log.warn("setImage: closing prior stream: " + e.toString());
			}
		}

		this.imageContent = null;
		this.imageSize = 0;
		this.imageName = null;
		this.imageType = null;
		this.imageRemove = false;
		this.sharedImage = null;

		this.imageFile = ref;

		this.changed = true;
	}

	@Override
	public void setImage(String name, int size, String type, InputStream content)
	{
		if (this.imageContent != null)
		{
			try
			{
				this.imageContent.close();
			}
			catch (IOException e)
			{
				M_log.warn("setImage: closing prior stream: " + e.toString());
			}
		}

		this.imageContent = content;
		this.imageSize = size;
		this.imageName = name;
		this.imageType = type;
		this.imageRemove = false;
		this.sharedImage = null;
		this.imageFile = null;

		this.changed = true;
	}

	@Override
	public void setPublished(Boolean published)
	{
		if (different(published, this.published))
		{
			this.changed = true;
			this.published = published;
		}
	}

	@Override
	public void setTitle(String title)
	{
		if (different(title, this.title))
		{
			this.changed = true;
			this.title = title;
		}
	}

	@Override
	public void shareContent(File file)
	{
		if (different(file, this.sharedContent))
		{
			this.sharedContent = file;
			this.newContent = null;
			this.removeContent = false;
			this.changed = true;
		}
	}

	@Override
	public void shareImage(File file)
	{
		if (different(file, this.sharedImage))
		{
			this.sharedImage = file;

			if (this.imageContent != null)
			{
				try
				{
					this.imageContent.close();
				}
				catch (IOException e)
				{
					M_log.warn("shareImage: closing prior stream: " + e.toString());
				}
			}

			this.imageContent = null;
			this.imageSize = 0;
			this.imageName = null;
			this.imageType = null;
			this.imageRemove = false;
			this.imageFile = null;

			this.changed = true;
		}
	}

	/**
	 * Mark the blog entry as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected Long getBlogId()
	{
		return this.blogId;
	}

	protected Reference getContentReference()
	{
		return fileService().getReference(this.contentReferenceId);
	}

	protected Long getContentReferenceId()
	{
		return this.contentReferenceId;
	}

	protected Long getImageReferenceId()
	{
		return this.imageReferenceId;
	}

	protected void initBlogId(Long id)
	{
		this.blogId = id;
	}

	protected void initContentReferenceId(Long id)
	{
		this.contentReferenceId = id;
	}

	protected void initCreatedBy(User user)
	{
		this.createdBy = user;
	}

	protected void initCreatedOn(Date createdOn)
	{
		this.createdOn = createdOn;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initImageReferenceId(Long id)
	{
		this.imageReferenceId = id;
	}

	protected void initModifiedBy(User user)
	{
		this.modifiedBy = user;
	}

	protected void initModifiedOn(Date modifiedOn)
	{
		this.modifiedOn = modifiedOn;
	}

	protected void initPublished(Boolean published)
	{
		this.published = published;
	}

	protected void initTitle(String title)
	{
		this.title = title;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	protected void saveContentAndImage(User savedBy)
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		// update the content
		this.contentReferenceId = fileService().savePrivateFile(this.removeContent, this.newContent, "blog_entry.html", "text/html",
				this.sharedContent, this.contentReferenceId, getReference(), Role.guest, keepers);

		this.imageReferenceId = fileService().saveMyFile(this.imageRemove, this.imageName, this.imageSize, this.imageType, savedBy,
				this.imageContent, this.imageFile, this.imageReferenceId, getReference(), Role.guest, keepers);

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		// clean up in case we get called to save again
		this.newContent = null;
		this.removeContent = false;
		this.sharedContent = null;
		this.imageContent = null;
		this.imageSize = 0;
		this.imageName = null;
		this.imageType = null;
		this.imageRemove = false;
		this.sharedImage = null;
		this.imageFile = null;
	}

	/**
	 * @return The registered BlogService.
	 */
	private BlogService blogService()
	{
		return (BlogService) Services.get(BlogService.class);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}
}
