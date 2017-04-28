/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/file/webapp/FileServiceImpl.java $
 * $Id: FileServiceImpl.java 11567 2015-09-06 20:22:36Z ggolden $
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

package org.etudes.file.webapp;

import static org.etudes.util.StringUtil.formatFileSize;
import static org.etudes.util.StringUtil.split;
import static org.etudes.util.StringUtil.splitLast;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * FileServiceImpl implements FileService
 */
public class FileServiceImpl implements FileService, Service
{
	protected class FileImpl implements File
	{
		protected Date date;
		protected Long id;
		protected boolean loaded = false;
		protected Date modifiedOn;
		protected String name;
		protected int size;
		protected String type;

		public FileImpl(Long id, Date date, Date modifiedOn, String name, int size, String type)
		{
			this.id = id;
			this.date = date;
			this.modifiedOn = modifiedOn;
			this.name = name;
			this.size = size;
			this.type = ((type == null) ? null : type.toLowerCase());
			this.loaded = true;
			// TODO: if type is "application/octet-stream", guess from the name extension
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) return false;
			if (!(obj instanceof FileImpl)) return false;
			return this.id.equals(((FileImpl) obj).getId());
		}

		@Override
		public Date getDate()
		{
			return this.date;
		}

		@Override
		public Long getId()
		{
			return this.id;
		}

		@Override
		public Date getModifiedOn()
		{
			return this.modifiedOn;
		}

		@Override
		public String getName()
		{
			return this.name;
		}

		@Override
		public int getSize()
		{
			return this.size;
		}

		@Override
		public String getType()
		{
			return this.type;
		}

		@Override
		public int hashCode()
		{
			return this.id.hashCode();
		}

		@Override
		public byte[] readBytes()
		{
			java.io.File file = new java.io.File(getFullPath());
			try
			{
				byte[] body = new byte[getSize()];
				FileInputStream in = new FileInputStream(file);

				in.read(body);
				in.close();

				return body;
			}
			catch (Throwable t)
			{
				M_log.warn("getBytes: " + t);
				return new byte[0];
			}
		}

		@Override
		public InputStream readStream()
		{
			java.io.File file = new java.io.File(getFullPath());
			try
			{
				FileInputStream in = new FileInputStream(file);
				return in;
			}
			catch (FileNotFoundException e)
			{
				M_log.warn("getStream: " + e);
				return null;
			}
		}

		@Override
		public String readString()
		{
			try
			{
				String rv = new String(readBytes(), "UTF-8");
				return rv;
			}
			catch (UnsupportedEncodingException e)
			{
				return "";
			}
		}

		@Override
		public Map<String, Object> send()
		{
			Map<String, Object> rv = new HashMap<String, Object>();

			rv.put("id", getId());
			rv.put("name", getName());
			rv.put("mimeType", getType());
			rv.put("size", formatFileSize(getSize()));
			rv.put("date", getModifiedOn());

			return rv;
		}

		/**
		 * @return The file's file system page & name.
		 */
		protected String getFullPath()
		{
			// form the path based on the upload date: /yyyy/DDD/HH (year, day of year, 24-hour), based on UT
			DateFormat dateFormat = new SimpleDateFormat("yyyy/DDD/HH/", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			// add the file's id, and the file name (as created)
			String path = dateFormat.format(this.date) + Long.toHexString(this.id) + "_" + this.name;

			return fileSystemRoot + path;
		}

		/**
		 * Set the id.
		 * 
		 * @param id
		 *        The file id.
		 */
		protected void setId(Long id)
		{
			this.id = id;
		}

		protected void setModifiedOn(Date date)
		{
			this.modifiedOn = date;
		}

		protected void setName(String name)
		{
			this.name = name;
		}

		protected void setSize(int size)
		{
			this.size = size;
		}

		protected void setType(String type)
		{
			this.type = type;
		}
	}

	protected class ReferenceImpl implements Reference
	{
		protected Long fileId;
		protected ToolItemReference holder;
		protected Long id;
		protected Role security;

		public ReferenceImpl(Long id, Long fileId, ToolItemReference holder, Role security)
		{
			this.fileId = fileId;
			this.id = id;
			this.holder = holder;
			this.security = security;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) return false;
			if (!(obj instanceof ReferenceImpl)) return false;
			return this.id.equals(((ReferenceImpl) obj).getId());
		}

		@Override
		public String getDownloadUrl()
		{
			// TODO: url encoding?
			if (this.security == Role.none) return null;

			String rv = "/download/" + id.toString() + "/" + getFile().getName();
			return rv;
		}

		@Override
		public File getFile()
		{
			return service.getFile(this.fileId);
		}

		@Override
		public ToolItemReference getHolder()
		{
			return this.holder;
		}

		@Override
		public Long getId()
		{
			return this.id;
		}

		@Override
		public String getPlaceholderUrl()
		{
			// TODO: url encoding?
			String rv = "/file/" + getFile().getId().toString();
			return rv;
		}

		@Override
		public Role getSecurity()
		{
			return this.security;
		}

		@Override
		public int hashCode()
		{
			return this.id.hashCode();
		}

		@Override
		public Map<String, Object> send()
		{
			Map<String, Object> rv = new HashMap<String, Object>();

			rv.put("id", getId());
			String downloadUrl = getDownloadUrl();
			if (downloadUrl != null) rv.put("downloadUrl", downloadUrl);

			rv.put("file", getFile().send());

			return rv;
		}

		@Override
		public String toString()
		{
			return "Reference: id: " + this.id + " site: " + this.holder.getSite().getId() + " tool: " + this.holder.getTool() + " item: "
					+ this.holder.getItemId() + " security: " + this.security + " file: " + this.fileId;
		}

		protected Long getFileId()
		{
			return this.fileId;
		}

		/**
		 * Set the id.
		 * 
		 * @param id
		 *        The id.
		 */
		protected void setId(Long id)
		{
			this.id = id;
		}
	}

	/** The chunk size used when streaming (100k). */
	protected static final int STREAM_BUFFER_SIZE = 102400;

	/** Our log. */
	private static Log M_log = LogFactory.getLog(FileServiceImpl.class);

	/** The root path of our files in the file system. */
	protected String fileSystemRoot = null;

	/** Pointer to self. */
	protected FileServiceImpl service = null;

	/**
	 * Construct
	 */
	public FileServiceImpl()
	{
		M_log.info("FileServiceImpl: construct");

		// setup to get configured once all services are started
		Services.whenAvailable(ConfigService.class, new Runnable()
		{
			public void run()
			{
				fileSystemRoot = configService().getString("FileService.fileSystemRoot");
				M_log.info("FileServiceImpl: root: " + fileSystemRoot);
			}
		});

		service = this;
	}

	@Override
	public Reference add(File file, ToolItemReference holder, Role security)
	{
		// make the reference, store it
		final ReferenceImpl rv = new ReferenceImpl(null, file.getId(), holder, security);
		sqlService().transact(new Runnable()
		{
			public void run()
			{
				newReferenceTx(rv);
			}
		}, "add: " + file.getName());

		return rv;
	}

	@Override
	public Reference add(File file, User myFilesUser)
	{
		// create the reference
		Reference rv = add(file, new ToolItemReference(siteService().wrap(0l), Tool.myfiles, myFilesUser.getId()), Role.custom);

		return rv;
	}

	@Override
	public File add(String name, int size, String type, InputStream content, Long id, Date date, Date modifiedOn)
	{
		if ((date == null) || (modifiedOn == null))
		{
			// set the dates to now
			final Date now = new Date();
			if (date == null) date = now;
			if (modifiedOn == null) modifiedOn = now;
		}

		// record the file in the database
		final FileImpl file = new FileImpl(id, date, modifiedOn, name, size, type);

		sqlService().transact(new Runnable()
		{
			public void run()
			{
				if (file.getId() == null)
				{
					newFileTx(file);
				}
				else
				{
					newFileWithIdTx(file);
				}
			}
		}, "add: " + name);

		// write the file
		OutputStream out = null;
		try
		{
			java.io.File resourceFile = new java.io.File(file.getFullPath());

			// make sure all directories are there
			java.io.File container = resourceFile.getParentFile();
			if (container != null)
			{
				container.mkdirs();
			}

			resourceFile.createNewFile();
			out = new FileOutputStream(resourceFile);

			// chunk
			byte[] chunk = new byte[STREAM_BUFFER_SIZE];
			int lenRead;
			while ((lenRead = content.read(chunk)) != -1)
			{
				out.write(chunk, 0, lenRead);
			}
		}
		catch (IOException e)
		{
			M_log.warn("add: " + e.toString());
		}
		finally
		{
			try
			{
				if (out != null) out.close();
				content.close();
			}
			catch (IOException e)
			{
				M_log.warn("add: " + e.toString());
			}
		}

		return file;
	}

	@Override
	public Reference add(String name, int size, String type, InputStream content, ToolItemReference holder, Role security)
	{
		// make the file
		File file = add(name, size, type, content, null, null, null);

		// make the reference
		Reference rv = add(file, holder, security);

		return rv;
	}

	// @Override
	// public Set<Reference> extractReferences(String content)
	// {
	// Set<Reference> rv = new HashSet<Reference>();
	// if (content == null) return rv;
	//
	// // /download/<refid>/<filename> in img src=, a href=
	// Pattern p = Pattern.compile("(src|href)[\\s]*=[\\s]*\"/download/([0-9]*)/([^#\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	// Matcher m = p.matcher(content);
	//
	// while (m.find())
	// {
	// if (m.groupCount() == 3)
	// {
	// String refId = m.group(2);
	// // String fileName = m.group(3);
	//
	// Reference ref = getReference(Long.valueOf(refId));
	// if (ref != null)
	// {
	// rv.add(ref);
	// }
	// }
	// }
	//
	// return rv;
	// }

	@Override
	public Set<Reference> extractReferencesFromPlaceholders(String content, ToolItemReference holder, Role security)
	{
		Set<Reference> rv = new HashSet<Reference>();
		if (content == null) return rv;

		// get the references for this address
		List<Reference> refs = getReferences(holder);

		// find all placeholder URLs in the new content
		Set<String> placeholderUrls = extractPlaceholders(content);
		for (String url : placeholderUrls)
		{
			// use this reference
			Reference ref = findReferenceWithPlaceholderUrl(url, refs);
			if (ref != null)
			{
				rv.add(ref);
			}

			// otherwise, this is new, create a ref
			else
			{
				String[] parts = split(url, "/");
				File file = getFile(Long.valueOf(parts[2]));
				if (file != null)
				{
					Reference newRef = add(file, holder, security);
					rv.add(newRef);
				}
			}
		}

		return rv;
	}

	@Override
	public Reference findReferenceWithFile(File file, List<Reference> refs)
	{
		for (Reference ref : refs)
		{
			if (ref.getFile().equals(file))
			{
				return ref;
			}
		}

		return null;
	}

	@Override
	public Reference findReferenceWithName(String name, List<Reference> refs)
	{
		for (Reference ref : refs)
		{
			if (ref.getFile().getName().equalsIgnoreCase(name))
			{
				return ref;
			}
		}

		return null;
	}

	@Override
	public Reference findReferenceWithPlaceholderUrl(String placeholderUrl, List<Reference> refs)
	{
		for (Reference ref : refs)
		{
			if (ref.getPlaceholderUrl().equalsIgnoreCase(placeholderUrl))
			{
				return ref;
			}
		}

		return null;
	}

	@Override
	public File getFile(final Long id)
	{
		if (id == null) return null;

		// read
		String sql = "SELECT DATE, MODIFIEDON, NAME, SIZE, TYPE FROM FILE WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;

		List<File> files = sqlService().select(sql, fields, new SqlService.Reader<File>()
		{
			public File read(ResultSet result)
			{
				try
				{
					int i = 1;
					Date date = sqlService().readDate(result, i++);
					Date modifiedOn = sqlService().readDate(result, i++);
					String name = sqlService().readString(result, i++);
					int length = sqlService().readLong(result, i++).intValue();
					String mimeType = sqlService().readString(result, i++);
					FileImpl f = new FileImpl(id, date, modifiedOn, name, length, mimeType);

					return f;
				}
				catch (SQLException e)
				{
					M_log.warn("getFile: " + e);
					return null;
				}
			}
		});

		if (files.size() != 1) return null;
		return files.get(0);
	}

	@Override
	public Reference getReference(final Long id)
	{
		if (id == null) return null;

		// read
		String sql = "SELECT SITE_ID, TOOL_ID, ITEM_ID, FILE_ID, SECURITY FROM FILE_REFERENCE WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;

		List<Reference> refs = sqlService().select(sql, fields, new SqlService.Reader<Reference>()
		{
			public Reference read(ResultSet result)
			{
				try
				{
					int i = 1;
					Site site = siteService().wrap(sqlService().readLong(result, i++));
					Tool tool = Tool.valueOf(sqlService().readInteger(result, i++));
					Long itemId = sqlService().readLong(result, i++);
					Long fileId = sqlService().readLong(result, i++);
					Role security = Role.valueOf(sqlService().readInteger(result, i++));
					ReferenceImpl ref = new ReferenceImpl(id, fileId, new ToolItemReference(site, tool, itemId), security);

					return ref;
				}
				catch (SQLException e)
				{
					M_log.warn("getReference: " + e);
					return null;
				}
			}
		});

		if (refs.size() != 1) return null;
		return refs.get(0);
	}

	@Override
	public Reference getReference(Reference other, User myFilesUser)
	{
		List<Reference> rv = getReferences(other, new ToolItemReference(siteService().wrap(0L), Tool.myfiles, myFilesUser.getId()));
		if (rv.isEmpty()) return null;
		return rv.get(0);
	}

	@Override
	public List<Reference> getReferences(File file)
	{
		return getReferencesForFile(file.getId());
	}

	@Override
	public List<Reference> getReferences(Reference referenceRef)
	{
		return getReferencesForFile(((ReferenceImpl) referenceRef).getFileId());
	}

	@Override
	public List<Reference> getReferences(final Reference referenceRef, final ToolItemReference holder)
	{
		String sql = "SELECT ID, SECURITY FROM FILE_REFERENCE WHERE SITE_ID = ? AND TOOL_ID = ? AND ITEM_ID = ? AND FILE_ID = ?";
		Object[] fields = new Object[4];
		fields[0] = holder.getSite().getId();
		fields[1] = holder.getTool().getId();
		fields[2] = holder.getItemId();
		fields[3] = ((ReferenceImpl) referenceRef).getFileId();

		List<Reference> rv = sqlService().select(sql, fields, new SqlService.Reader<Reference>()
		{
			public Reference read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					Role security = Role.valueOf(sqlService().readInteger(result, i++));
					ReferenceImpl ref = new ReferenceImpl(id, ((ReferenceImpl) referenceRef).getFileId(), holder, security);

					return ref;
				}
				catch (SQLException e)
				{
					M_log.warn("getReferences: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	@Override
	public List<Reference> getReferences(final ToolItemReference holder)
	{
		String sql = "SELECT ID, FILE_ID, SECURITY FROM FILE_REFERENCE WHERE SITE_ID = ? AND TOOL_ID = ? AND ITEM_ID = ?";
		Object[] fields = new Object[3];
		fields[0] = holder.getSite().getId();
		fields[1] = holder.getTool().getId();
		fields[2] = holder.getItemId();

		List<Reference> rv = sqlService().select(sql, fields, new SqlService.Reader<Reference>()
		{
			public Reference read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					Long fileId = sqlService().readLong(result, i++);
					Role security = Role.valueOf(sqlService().readInteger(result, i++));
					ReferenceImpl ref = new ReferenceImpl(id, fileId, holder, security);

					return ref;
				}
				catch (SQLException e)
				{
					M_log.warn("getReferences: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	@Override
	public List<Reference> getReferences(User myFilesUser)
	{
		return getReferences(new ToolItemReference(siteService().wrap(0L), Tool.myfiles, myFilesUser.getId()));
	}

	@Override
	public String makeUniqueName(String name, List<Reference> refs)
	{
		// will either return the two parts, or null if there is no dot
		String origName = name;
		String[] parts = splitLast(name, ".");
		int num = 1;

		boolean again = false;
		do
		{
			again = false;
			for (Reference ref : refs)
			{
				if (ref.getFile().getName().equalsIgnoreCase(name))
				{
					if (parts == null)
					{
						name = origName + "-" + num++;
					}
					else
					{
						name = parts[0] + "-" + num++ + "." + parts[1];
					}
					again = true;
					break;
				}
			}
		}
		while (again);

		return name;
	}

	@Override
	public ToolItemReference myFileReference(User user)
	{
		return new ToolItemReference(siteService().wrap(0l), Tool.myfiles, user.getId());
	}

	@Override
	public String processContentDownloadToPlaceholder(String content)
	{
		if (content == null) return content;

		Pattern p = Pattern.compile("(src|href)[\\s]*=[\\s]*\"/download/([U]*[0-9]*)/([^#\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		Matcher m = p.matcher(content);

		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			if (m.groupCount() == 3)
			{
				try
				{
					// use this reference
					Reference ref = null;

					// special "U<userid>" handling (special for CKFinder)
					if (m.group(2).startsWith("U"))
					{
						Long userId = Long.valueOf(m.group(2).substring(1));
						User user = userService().get(userId);

						// find a myFiles reference for the authenticated user matching the file name, the remainder of the path (TODO: folders)
						List<Reference> myFiles = getReferences(user);
						ref = findReferenceWithName(m.group(3), myFiles);
					}

					else
					{
						Long refId = Long.valueOf(m.group(2));
						ref = this.getReference(refId);
					}

					if (ref != null)
					{
						m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "=\"" + ref.getPlaceholderUrl() + "\""));
					}
				}
				catch (NumberFormatException e)
				{
				}
			}
		}
		m.appendTail(sb);

		return sb.toString();
	}

	@Override
	public String processContentPlaceholderToDownload(String content, ToolItemReference holder)
	{
		if (content == null) return content;

		// get the references for this address
		List<Reference> refs = getReferences(holder);

		Pattern p = Pattern.compile("(src|href)[\\s]*=[\\s]*\"/file/([0-9]*)\"", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		Matcher m = p.matcher(content);

		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			if (m.groupCount() == 2)
			{
				try
				{
					// use this reference
					Long fileId = Long.valueOf(m.group(2));
					Reference ref = findReferenceWithFileId(fileId, refs);
					if (ref != null)
					{
						m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "=\"" + ref.getDownloadUrl() + "\""));
					}
				}
				catch (NumberFormatException e)
				{
				}
			}
		}
		m.appendTail(sb);

		return sb.toString();
	}

	@Override
	public String processContentPlaceholderToPlaceholderTranslated(String content, Map<Long, Long> translations)
	{
		if (content == null) return content;

		Pattern p = Pattern.compile("(src|href)[\\s]*=[\\s]*\"/file/([0-9]*)\"", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		Matcher m = p.matcher(content);

		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			if (m.groupCount() == 2)
			{
				try
				{
					// use this reference
					Long fileId = Long.valueOf(m.group(2));
					Long newId = translations.get(fileId);
					if ((newId != null) && (!newId.equals(fileId)))
					{
						m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "=\"/file/" + newId.toString() + "\""));
					}
				}
				catch (NumberFormatException e)
				{
				}
			}
		}
		m.appendTail(sb);

		return sb.toString();
	}

	@Override
	public void remove(final Reference ref)
	{
		// delete the reference record
		sqlService().transact(new Runnable()
		{
			public void run()
			{
				deleteReferenceTx(ref.getId());
			}
		}, "remove: " + ref.getId());

		// TODO: one transaction, and remove the file system file only after the records are gone?

		// if there are no more references to this file, remove the file
		if (!hasReferences(((ReferenceImpl) ref).getFileId()))
		{
			removeFile(ref.getFile());
		}
	}

	@Override
	public void removeExcept(ToolItemReference holder, Set<Reference> keepers)
	{
		// start with all the references with these coordinates
		List<Reference> currentRefs = getReferences(holder);

		// for each one, if not in the keepers set, remove it
		for (Reference r : currentRefs)
		{
			// keep it if it is in keepers
			if ((keepers != null) && (keepers.contains(r))) continue;

			remove(r);
		}
	}

	@Override
	public void rename(User authenticatedUser, Reference ref, String name)
	{
		final File file = ref.getFile();
		String oldName = file.getName();

		// only if there's a change
		if (name.equalsIgnoreCase(oldName)) return;

		// the existing file
		java.io.File osFileOld = new java.io.File(((FileImpl) file).getFullPath());

		// update the file name
		((FileImpl) file).setName(name);

		// the new file
		java.io.File osFileNew = new java.io.File(((FileImpl) file).getFullPath());

		// rename the file system file
		osFileOld.renameTo(osFileNew);

		// update the file record
		sqlService().transact(new Runnable()
		{
			public void run()
			{
				updateFileTx((FileImpl) file);
			}
		}, "rename: " + oldName);
	}

	/**
	 * Replace the body of the file.
	 * 
	 * @param file
	 *        The file to be replaced.
	 * @param size
	 *        The file size (in bytes).
	 * @param type
	 *        The mime type.
	 * @param content
	 *        The file content stream.
	 */
	@Override
	public void replace(final File file, int size, String type, InputStream content)
	{
		// update the file with the new data, and update the database
		((FileImpl) file).setModifiedOn(new Date());
		((FileImpl) file).setSize(size);
		((FileImpl) file).setType(type);

		sqlService().transact(new Runnable()
		{
			public void run()
			{
				updateFileTx((FileImpl) file);
			}
		}, "replace: " + file.getId());

		// write the file, replacing the existing file
		OutputStream out = null;
		try
		{
			java.io.File resourceFile = new java.io.File(((FileImpl) file).getFullPath());

			// make sure all directories are there
			java.io.File container = resourceFile.getParentFile();
			if (container != null)
			{
				container.mkdirs();
			}

			resourceFile.createNewFile();
			out = new FileOutputStream(resourceFile);

			// chunk
			byte[] chunk = new byte[STREAM_BUFFER_SIZE];
			int lenRead;
			while ((lenRead = content.read(chunk)) != -1)
			{
				out.write(chunk, 0, lenRead);
			}
		}
		catch (IOException e)
		{
			M_log.warn("replace: " + e.toString());
		}
		finally
		{
			try
			{
				if (out != null) out.close();
				content.close();
			}
			catch (IOException e)
			{
				M_log.warn("replace: " + e.toString());
			}
		}
	}

	@Override
	public Long saveMyFile(boolean remove, String name, int size, String type, User user, InputStream contents, Reference shareRef,
			Long currentReferenceId, ToolItemReference holder, Role security, Set<Reference> keepers)
	{
		if (remove)
		{
			// remove the old reference
			if (currentReferenceId != null)
			{
				currentReferenceId = null;
			}
		}
		else if (contents != null)
		{
			// add a new file & reference
			Reference ref = add(name, size, type, contents, holder, security);

			// add it to the user's myFiles
			add(ref.getFile(), user);

			// record reference
			currentReferenceId = ref.getId();
		}
		else if (shareRef != null)
		{
			// we might already have a ref to this file
			List<Reference> otherRefs = getReferences(shareRef, holder);
			if (!otherRefs.isEmpty())
			{
				currentReferenceId = otherRefs.get(0).getId();
			}

			// add a new reference to an existing file
			else
			{
				Reference ref = add(shareRef.getFile(), holder, security);
				currentReferenceId = ref.getId();
			}
		}

		if (currentReferenceId != null) keepers.add(getReference(currentReferenceId));
		return currentReferenceId;
	}

	@Override
	public Long savePrivateFile(boolean remove, String newContent, String name, String type, File shareContent, Long currentReferenceId,
			ToolItemReference holder, Role embeddedSecurity, Set<Reference> keepers)
	{
		if (remove)
		{
			// remove the old reference
			if (currentReferenceId != null)
			{
				currentReferenceId = null;
			}
		}

		// changed content
		else if (newContent != null)
		{
			if (holder.getItemId() == null)
			{
				M_log.warn("savePrivateFile: null itemId: site: " + holder.getSite().getId() + " tool: " + holder.getTool());
				if (currentReferenceId != null) keepers.add(getReference(currentReferenceId));
				return currentReferenceId;
			}

			// content is already in placeholder format
			try
			{
				// our references
				if ("text/html".equals(type))
				{
					Set<Reference> refs = extractReferencesFromPlaceholders(newContent, holder, embeddedSecurity);
					keepers.addAll(refs);
				}
				// prepare the content for adding to a file
				byte[] content = newContent.getBytes("UTF-8");
				ByteArrayInputStream stream = new ByteArrayInputStream(content);

				// if there are no other references to the entry's file, we can just update the file, and the ref does not change
				File replace = null;
				if (currentReferenceId != null)
				{
					List<Reference> otherRefs = getReferences(getReference(currentReferenceId));
					if (otherRefs.size() == 1)
					{
						replace = otherRefs.get(0).getFile();
					}
				}

				if (replace != null)
				{
					replace(replace, content.length, type, stream);
				}
				else
				{
					// add the file and a tool reference, but no publication, and myFiles reference (the content is private).
					Reference ref = add(name, content.length, type, stream, holder, Role.none);
					currentReferenceId = ref.getId();
				}
			}
			catch (UnsupportedEncodingException e)
			{
				M_log.warn("savePrivateFile: " + e.toString());
			}
		}

		// or, newly sharing content
		else if (shareContent != null)
		{
			if (holder.getItemId() == null)
			{
				M_log.warn("savePrivateFile: null itemId: site: " + holder.getSite().getId() + " tool: " + holder.getTool());
				if (currentReferenceId != null) keepers.add(getReference(currentReferenceId));
				return currentReferenceId;
			}

			// we need a reference to this shared file
			Reference ref = add(shareContent, holder, Role.none);
			currentReferenceId = ref.getId();

			// and any embedded references (these are placeholder URLs in stored content)
			if ("text/html".equals(ref.getFile().getType()))
			{
				String content = ref.getFile().readString();
				Set<Reference> refs = extractReferencesFromPlaceholders(content, holder, embeddedSecurity);
				keepers.addAll(refs);
			}
		}

		// if we didn't change content, we need to see what references we have in the old content
		else if (currentReferenceId != null)
		{
			if (holder.getItemId() == null)
			{
				M_log.warn("savePrivateFile: null itemId: site: " + holder.getSite().getId() + " tool: " + holder.getTool());
				if (currentReferenceId != null) keepers.add(getReference(currentReferenceId));
				return currentReferenceId;
			}

			Reference ref = getReference(currentReferenceId);
			if ((ref != null) && ("text/html".equals(ref.getFile().getType())))
			{
				// all the references in the current (unchanged) content are keepers (these are placeholder URLs in stored content)
				String content = getReference(currentReferenceId).getFile().readString();
				Set<Reference> refs = extractReferencesFromPlaceholders(content, holder, embeddedSecurity);
				keepers.addAll(refs);
			}
		}

		// the reference we hold (content) is a keeper
		if (currentReferenceId != null) keepers.add(getReference(currentReferenceId));

		return currentReferenceId;
	}

	@Override
	public boolean start()
	{
		M_log.info("FileServiceImpl: start");
		return true;
	}

	@Override
	public boolean validFileName(String name)
	{
		// no path separator like slashes
		return name.indexOf('/') == -1;
	}

	/**
	 * Delete a file record.
	 * 
	 * @param id
	 *        The file id.
	 */
	protected void deleteFileTx(Long id)
	{
		String sql = "DELETE FROM FILE WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		sqlService().update(sql, fields);
	}

	/**
	 * Delete a reference record.
	 * 
	 * @param id
	 *        The reference id.
	 */
	protected void deleteReferenceTx(Long id)
	{
		String sql = "DELETE FROM FILE_REFERENCE WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		sqlService().update(sql, fields);
	}

	/**
	 * Extract all the placeholder URLs from the content.
	 * 
	 * @param content
	 *        The content.
	 * @return The placeholder URLs
	 */
	protected Set<String> extractPlaceholders(String content)
	{
		Set<String> rv = new HashSet<String>();
		if (content == null) return rv;

		// /file/<fileid> in img src=, a href=
		Pattern p = Pattern.compile("(src|href)[\\s]*=[\\s]*\"/file/([0-9]*)\"", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		Matcher m = p.matcher(content);

		while (m.find())
		{
			if (m.groupCount() == 2)
			{
				String fileId = m.group(2);
				rv.add("/file/" + fileId);
			}
		}

		return rv;
	}

	protected Reference findReferenceWithFileId(Long fileId, List<Reference> refs)
	{
		for (Reference ref : refs)
		{
			if (ref.getFile().getId().equals(fileId))
			{
				return ref;
			}
		}

		return null;
	}

	protected List<Reference> getReferencesForFile(final Long fileId)
	{
		String sql = "SELECT ID, SITE_ID, TOOL_ID, ITEM_ID, SECURITY FROM FILE_REFERENCE WHERE FILE_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = fileId;

		List<Reference> rv = sqlService().select(sql, fields, new SqlService.Reader<Reference>()
		{
			public Reference read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					Site site = siteService().wrap(sqlService().readLong(result, i++));
					Tool tool = Tool.valueOf(sqlService().readInteger(result, i++));
					Long itemId = sqlService().readLong(result, i++);
					Role security = Role.valueOf(sqlService().readInteger(result, i++));
					ReferenceImpl ref = new ReferenceImpl(id, fileId, new ToolItemReference(site, tool, itemId), security);

					return ref;
				}
				catch (SQLException e)
				{
					M_log.warn("getReferencesForFile: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	protected boolean hasReferences(Long fileId)
	{
		if (fileId == null) return false;

		// read
		String sql = "SELECT COUNT(1) FROM FILE_REFERENCE WHERE FILE_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = fileId;

		List<Boolean> rv = sqlService().select(sql, fields, new SqlService.Reader<Boolean>()
		{
			public Boolean read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long count = sqlService().readLong(result, i++);

					if (count > 0)
					{
						return Boolean.TRUE;
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("hasReferences: " + e);
					return null;
				}
			}
		});

		if (rv.size() != 1) return false;
		return rv.get(0).booleanValue();
	}

	protected void newFileTx(FileImpl f)
	{
		String sql = "INSERT INTO FILE (DATE, MODIFIEDON, NAME, SIZE, TYPE) values (?,?,?,?,?)";

		Object[] fields = new Object[5];
		fields[0] = f.getDate();
		fields[1] = f.getModifiedOn();
		fields[2] = f.getName();
		fields[3] = Long.valueOf(f.getSize());
		fields[4] = f.getType();

		Long id = sqlService().insert(sql, fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("newFileTx: dbInsert failed");
		}

		f.setId(id);
	}

	protected void newFileWithIdTx(FileImpl f)
	{
		String sql = "INSERT INTO FILE (ID, DATE, MODIFIEDON, NAME, SIZE, TYPE) values (?,?,?,?,?,?)";

		Object[] fields = new Object[6];
		int i = 0;
		fields[i++] = f.getId();
		fields[i++] = f.getDate();
		fields[i++] = f.getModifiedOn();
		fields[i++] = f.getName();
		fields[i++] = Long.valueOf(f.getSize());
		fields[i++] = f.getType();

		Long id = sqlService().insert(sql, fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("newFileTx: dbInsert failed");
		}
	}

	protected void newReferenceTx(ReferenceImpl ref)
	{
		String sql = "INSERT INTO FILE_REFERENCE (SITE_ID, TOOL_ID, ITEM_ID, FILE_ID, SECURITY) values (?,?,?,?,?)";

		Object[] fields = new Object[5];
		fields[0] = ref.getHolder().getSite().getId();
		fields[1] = ref.getHolder().getTool().getId();
		fields[2] = ref.getHolder().getItemId();
		fields[3] = ref.getFileId();
		fields[4] = ref.getSecurity().getLevel();

		Long id = sqlService().insert(sql, fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("newReferenceTx: dbInsert failed");
		}

		ref.setId(id);
	}

	/**
	 * Remove the file from storage.
	 * 
	 * @param file
	 *        The file to remove.
	 */
	protected void removeFile(final File file)
	{
		// delete the file
		String fullPath = ((FileImpl) file).getFullPath();
		java.io.File osFile = new java.io.File(fullPath);
		osFile.delete();

		// delete the file record
		sqlService().transact(new Runnable()
		{
			public void run()
			{
				deleteFileTx(file.getId());
			}
		}, "removeFile: " + fullPath);
	}

	protected void updateFileTx(FileImpl f)
	{
		String sql = "UPDATE FILE SET MODIFIEDON=?, SIZE=?, TYPE=?, NAME=? WHERE ID=?";

		Object[] fields = new Object[5];
		fields[0] = f.getModifiedOn();
		fields[1] = Long.valueOf(f.getSize());
		fields[2] = f.getType();
		fields[3] = f.getName();
		fields[4] = f.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
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
