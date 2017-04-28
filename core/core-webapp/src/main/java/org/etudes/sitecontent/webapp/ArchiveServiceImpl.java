/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sitecontent/webapp/ArchiveServiceImpl.java $
 * $Id: ArchiveServiceImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.sitecontent.webapp;

import static org.etudes.util.Different.different;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.Term;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.ArchiveService;
import org.etudes.sitecontent.api.ArchivedSite;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * PurgeServiceImpl implements PurgeService.
 */
public class ArchiveServiceImpl implements ArchiveService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ArchiveServiceImpl.class);

	/** Root location of archive files. */
	protected String fileSystemRoot = null;

	/** The set of tools to read first pass. */
	protected Set<Tool> firstPassTools = new HashSet<Tool>();

	/** A string that identifies where an archive comes from - production, staging, and dev instances will have different settings. */
	protected String service = null;

	/**
	 * Construct
	 */
	public ArchiveServiceImpl()
	{
		M_log.info("ArchiveServiceImpl: construct");

		// setup the tools to read first pass when importing
		this.firstPassTools.add(Tool.myfiles);
		this.firstPassTools.add(Tool.archive);

		// setup to get configured once all services are started
		Services.whenAvailable(ConfigService.class, new Runnable()
		{
			public void run()
			{
				fileSystemRoot = configService().getString("ArchiveService.fileSystemRoot");
				service = configService().getString("service");
				M_log.info("ArchiveServiceImpl: root: " + fileSystemRoot + "  service: " + service);
			}
		});
	}

	@Override
	public boolean archive(final Site site, Boolean authoredContentOnly)
	{
		// see if we already have an archive
		ArchivedSite existingArchived = this.archiveForSite(site);
		if (existingArchived != null)
		{
			// remove the old archive
			this.remove(existingArchived);
		}

		// record the archive
		final ArchivedSite archived = new ArchivedSiteImpl(null, site.getTerm(), site.getClient(), site.getId(), site.getName(), new Date());
		final Membership siteRoster = rosterService().getAggregateSiteRoster(site);
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				insertArchivedTx(archived);
				insertArchivedOwnersTx(archived, siteRoster);
			}
		}, "insertArchive");

		// prepare the archive
		ArchiveImpl archive = new ArchiveImpl();

		// store it here
		String filePath = filePath(new ArchivedSiteImpl(null, site.getTerm(), site.getClient(), site.getId(), site.getName(), null));
		archive.setFilePath(filePath);

		// get the archives ready
		archive.init();

		// archive site information
		archive.archive(newArchiveArtifact(archived, authoredContentOnly, archive));

		// archive site content for all tools that handle site content
		Map<Tool, Service> handlers = Services.getHandlers(SiteContentHandler.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				if (s.getValue() instanceof SiteContentHandler)
				{
					((SiteContentHandler) s.getValue()).archive(site, authoredContentOnly, archive);
				}
			}
		}

		// the artifacts have references - the referenced files need to be included
		Set<File> files = new HashSet<File>();
		for (Artifact a : archive.getArtifacts())
		{
			files.addAll(((ArtifactImpl) a).getReferences());
		}

		// include the files
		for (File f : files)
		{
			archive.archive(newMyfileArtifact(f, archive));
		}

		// TODO: artifacts might reference evaluation criteria, and we may need to do something about that, more than the evaluation service archiving them.

		// complete the archive
		archive.complete();

		return true;
	}

	@Override
	public ArchivedSite archiveForSite(Site site)
	{
		return archiveForSiteTx(site);
	}

	@Override
	public List<ArchivedSite> archivesForUser(User user)
	{
		List<ArchivedSite> rv = archivesForUserTx(user);

		return rv;
	}

	@Override
	public ArchivedSite getArchive(Long id)
	{
		ArchivedSite rv = archiveTx(id);
		return rv;
	}

	@Override
	public boolean importFromArchive(ArchivedSite archivedSite, Set<Tool> fromTools, Site intoSite, Boolean authoredContentOnly, User importingUser)
	{
		// our site content handling tools
		Map<Tool, Service> handlers = Services.getHandlers(SiteContentHandler.class);

		Archive archive = null;
		try
		{
			// open the archive
			archive = openArchive(archivedSite);

			// if not found, return false
			if (archive == null) return false;

			// read the manifest
			((ArchiveImpl) archive).readManifest();

			// collect all the myFiles referenced by any artifact from tools we will be importing - left null if we want them all
			Set<Long> myFilesNeeded = null;
			if (!fromTools.contains(Tool.myfiles))
			{
				myFilesNeeded = new HashSet<Long>();
				for (Artifact artifact : ((ArchiveImpl) archive).getArtifacts())
				{
					if ((fromTools != null) && (!fromTools.contains(artifact.getTool()))) continue;

					// make sure we have a handler
					Service handler = handlers.get(artifact.getTool());
					if ((handler == null) || (!(handler instanceof SiteContentHandler))) continue;

					// collect the files referenced
					myFilesNeeded.addAll(((ArtifactImpl) artifact).getFileIdsReferenced());
				}
			}

			// and for the old style
			Set<String> myFileRefsNeeded = null;
			if (!fromTools.contains(Tool.myfiles))
			{
				myFileRefsNeeded = new HashSet<String>();
				for (Artifact artifact : ((ArchiveImpl) archive).getArtifacts())
				{
					if ((fromTools != null) && (!fromTools.contains(artifact.getTool()))) continue;

					// make sure we have a handler
					Service handler = handlers.get(artifact.getTool());
					if ((handler == null) || (!(handler instanceof SiteContentHandler))) continue;

					// collect the files referenced
					myFileRefsNeeded.addAll(((ArtifactImpl) artifact).getFileRefsReferenced());
				}
			}

			// read the myFiles and archives artifacts
			((ArchiveImpl) archive).readArtifacts(this.firstPassTools, null);

			// will set to true (processing the archive artifact) if the archive came from the same service, false if it was from some other service
			boolean fromHere = false;

			// will collect translations for myFiles, from their archived id to their new imported id
			Map<Long, Long> translations = new HashMap<Long, Long>();

			// will collect translations for myFiles (from old format), from their archived reference string to their new imported id
			Map<String, Long> oldTranslations = new HashMap<String, Long>();

			// will collect files newly imported
			List<File> newFiles = new ArrayList<File>();

			// TODO: evaluation criteria import, creates new criteria (for this site), and importing tool designs need a map

			// process all archive and myFiles artifacts
			for (Artifact artifact : ((ArchiveImpl) archive).getArtifacts())
			{
				if (artifact.getTool() == Tool.archive)
				{
					// get the service
					String archiveService = (String) artifact.getProperties().get("service");
					if (archiveService != null)
					{
						fromHere = archiveService.equals(this.service);
					}
				}

				else if (artifact.getTool() == Tool.myfiles)
				{
					if (artifact.getType().equals("file"))
					{
						Long sourceFileId = (Long) artifact.getProperties().get("id");
						InputStream body = (InputStream) artifact.getProperties().get("body");

						// skip if we won't be needing this one
						if ((myFilesNeeded != null) && (!myFilesNeeded.contains(sourceFileId)))
						{
							try
							{
								body.close();
							}
							catch (IOException e)
							{
								M_log.warn("importFromArchive (closing unused file body): " + e.toString());
							}
							continue;
						}

						// old file id in artifact - new file id may match or be new
						Long newFileId = null;

						Date date = (Date) artifact.getProperties().get("date");
						Date modifiedOn = (Date) artifact.getProperties().get("modifiedon");
						String name = (String) artifact.getProperties().get("name");
						Integer size = (Integer) artifact.getProperties().get("size");
						String type = (String) artifact.getProperties().get("type");
						Boolean addToMyFiles = (Boolean) artifact.getProperties().get("myfiles");
						if (addToMyFiles == null) addToMyFiles = Boolean.FALSE;

						// for archives from here, we can reuse, re-create
						if (fromHere)
						{
							// does this file still exist?
							File existingFile = fileService().getFile(sourceFileId);
							if (existingFile != null)
							{
								newFileId = sourceFileId;

								// if we somehow have a later version of the file, update it.
								if (existingFile.getModifiedOn().before(modifiedOn))
								{
									fileService().replace(existingFile, size, type, body);
								}

								else
								{
									try
									{
										body.close();
									}
									catch (IOException e)
									{
										M_log.warn("importFromArchive (closing unused file body): " + e.toString());
									}
								}
							}

							// file no longer here
							else
							{
								// if we need a myFiles reference, make sure the name is unique ... or use existing file by same name
								if (addToMyFiles)
								{
									// use exiting myFile if title matches
									List<Reference> myFiles = fileService().getReferences(importingUser);
									Reference existingRef = fileService().findReferenceWithName(name, myFiles);
									if (existingRef != null)
									{
										newFileId = existingRef.getFile().getId();
										try
										{
											body.close();
										}
										catch (IOException e)
										{
											M_log.warn("importFromArchive (closing unused file body): " + e.toString());
										}
									}

									// no blocking myfile, bring this file back
									else
									{
										// re-create the file, using the same ID and dates
										File file = fileService().add(name, size, type, body, sourceFileId, date, modifiedOn);
										newFileId = file.getId();
										newFiles.add(file);

										// add a reference to myFiles
										fileService().add(file, importingUser);
									}
								}

								// not going into myfiles, so we don't care about name conflicts
								else
								{
									// re-create the file, using the same ID and dates
									File file = fileService().add(name, size, type, body, sourceFileId, date, modifiedOn);
									newFileId = file.getId();
									newFiles.add(file);
								}
							}
						}

						// for files from some other service, we need to import
						else
						{
							if (addToMyFiles)
							{
								// use exiting myFile if title matches
								List<Reference> myFiles = fileService().getReferences(importingUser);
								Reference existingRef = fileService().findReferenceWithName(name, myFiles);
								if (existingRef != null)
								{
									newFileId = existingRef.getFile().getId();
									try
									{
										body.close();
									}
									catch (IOException e)
									{
										M_log.warn("importFromArchive (closing unused file body): " + e.toString());
									}
								}

								// no blocking myfile, so bring this file in
								else
								{
									File file = fileService().add(name, size, type, body, null, date, modifiedOn);
									newFileId = file.getId();
									newFiles.add(file);

									// add a reference to myfiles
									fileService().add(file, importingUser);
								}
							}

							// not going into myfiles, so import it
							else
							{
								File file = fileService().add(name, size, type, body, null, date, modifiedOn);
								newFileId = file.getId();
								newFiles.add(file);
							}
						}

						translations.put(sourceFileId, newFileId);
					}

					// old format (the resources)
					else if (artifact.getType().startsWith("/content/") && (!artifact.getType().endsWith("/")))
					{
						InputStream body = (InputStream) artifact.getProperties().get("body");
						String ref = (String) artifact.getProperties().get("ref");

						if ((myFileRefsNeeded != null) && (!myFileRefsNeeded.contains(ref)))
						{
							try
							{
								body.close();
							}
							catch (IOException e)
							{
								M_log.warn("importFromArchive (closing unused file body): " + e.toString());
							}
							continue;
						}

						Date date = new Date((Long) artifact.getProperties().get("createdDate"));
						Date modifiedOn = new Date((Long) artifact.getProperties().get("modifiedDate"));
						String name = (String) artifact.getProperties().get("displayName");
						Integer size = (Integer) artifact.getProperties().get("size");
						String type = (String) artifact.getProperties().get("type");

						// // make the file name unique among the user's myfiles
						// List<Reference> myFiles = fileService().getReferences(importingUser);
						// name = fileService().makeUniqueName(name, myFiles);

						// use an existing file if the name matches
						List<Reference> myFiles = fileService().getReferences(importingUser);
						Reference existingRef = fileService().findReferenceWithName(name, myFiles);
						if (existingRef != null)
						{
							oldTranslations.put("/access" + ref, existingRef.getFile().getId());
						}
						else
						{
							// add the file, with a myfiles reference
							File file = fileService().add(name, size, type, body, null, date, modifiedOn);
							fileService().add(file, importingUser);
							newFiles.add(file);

							// map the old reference string to the new file id
							oldTranslations.put("/access" + ref, file.getId());
						}
					}
				}
			}

			((ArchiveImpl) archive).setTranslations(translations, oldTranslations);

			// process the new files for the translations
			for (File f : newFiles)
			{
				if (f.getType().equalsIgnoreCase("text/html"))
				{
					// html files may have "file/nnn" placeholder references
					String content = f.readString();

					String newContent = archive.translateContentBody(content);

					if (different(content, newContent))
					{
						try
						{
							byte[] bytes = newContent.getBytes("UTF-8");
							ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
							fileService().replace(f, content.length(), "text/html", stream);
						}
						catch (UnsupportedEncodingException e)
						{
							M_log.warn("importFromArchive" + e.toString());
						}
					}
				}
			}

			// read the other artifacts that we want
			((ArchiveImpl) archive).readArtifacts(fromTools, this.firstPassTools);

			// TODO: handle site information

			// handle artifacts
			for (Artifact artifact : ((ArchiveImpl) archive).getArtifacts())
			{
				// skip those we already processed
				if (this.firstPassTools.contains(artifact.getTool())) continue;

				// find the tool for this artifact
				Service handler = handlers.get(artifact.getTool());
				if ((handler != null) && (handler instanceof SiteContentHandler))
				{
					try
					{
						((SiteContentHandler) handler).importFromArchive(artifact, authoredContentOnly, intoSite, importingUser);
					}
					catch (Throwable t)
					{
						M_log.warn("importFromArchive: ", t);
						// if (report) this.message("failure in handler: ", handler.toString(), ": ", t.toString());
					}
				}
				else
				{
					M_log.warn("importFromArchive: missing handler for " + artifact.getTool());
					// report artifact with no tool
				}
			}
		}
		finally
		{
			if (archive != null)
			{
				((ArchiveImpl) archive).close();
				archive = null;
			}
		}

		return true;
	}

	@Override
	public boolean remove(final ArchivedSite archived)
	{
		String filePath = filePath(archived);
		String zipName = filePath.substring(0, filePath.length() - 1) + ".zip";

		// delete the file if present
		java.io.File zip = new java.io.File(zipName);
		if (zip.exists())
		{
			zip.delete();
		}
		else
		{
			M_log.warn("remove: missing file: " + zipName);
		}

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeArchiveTx(archived);
			}
		}, "removeSite");

		return true;
	}

	@Override
	public boolean start()
	{
		M_log.info("ArchiveServiceImpl: start");
		return true;
	}

	@Override
	public List<Tool> toolsForArchive(Long id)
	{
		List<Tool> rv = new ArrayList<Tool>();

		// find the archive with this id
		ArchivedSite archivedSite = archiveTx(id);
		if (archivedSite == null) return rv;

		Set<Tool> tools = new HashSet<Tool>();
		Archive archive = null;
		try
		{
			// open the archive
			archive = openArchive(archivedSite);

			// if not found, return false
			if (archive == null) return rv;

			// read the manifest only
			((ArchiveImpl) archive).readManifest();

			// tool for site information, myfiles TODO:

			// our site content handling tools
			Map<Tool, Service> handlers = Services.getHandlers(SiteContentHandler.class);

			// check the artifacts for tool content that will be handled
			for (Artifact artifact : ((ArchiveImpl) archive).getArtifacts())
			{
				Service handler = handlers.get(artifact.getTool());
				if ((handler != null) && (handler instanceof SiteContentHandler))
				{
					tools.add(artifact.getTool());
				}
			}
		}
		finally
		{
			if (archive != null)
			{
				((ArchiveImpl) archive).close();
				archive = null;
			}
		}

		// order the tools
		rv.addAll(tools);
		Collections.sort(rv, new Tool.ToolOrderComparator());

		return rv;
	}

	/**
	 * Transaction code for finding the archive for a site.
	 *
	 * @param site
	 *        The site.
	 * @return The ArchivedSite, or null if not found.
	 */
	protected ArchivedSite archiveForSiteTx(final Site site)
	{
		String sql = "SELECT A.ID, A.ARCHIVED_ON, A.NAME, " + rosterService().sqlSelectClientFragment() + ", "
				+ rosterService().sqlSelectTermFragment()
				+ " FROM ARCHIVE A LEFT OUTER JOIN CLIENT C ON A.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON A.TERM_ID = T.ID"
				+ " WHERE A.SITE_ID = ?";

		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<ArchivedSite> rv = sqlService().select(sql, fields, new SqlService.Reader<ArchivedSite>()
		{
			@Override
			public ArchivedSite read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					Date archivedOn = sqlService().readDate(result, i++);
					String name = sqlService().readString(result, i++);

					Client client = rosterService().createClientFromResultSet(result, i);
					i += rosterService().sqlSelectClientFragmentNumFields();

					Term term = rosterService().createTermFromResultSet(result, i);
					i += rosterService().sqlSelectTermFragmentNumFields();

					ArchivedSiteImpl rv = new ArchivedSiteImpl(id, term, client, site.getId(), name, archivedOn);

					return rv;
				}
				catch (SQLException e)
				{
					M_log.warn("archivesTx(site): " + e);
					return null;
				}
			}
		});

		return (rv.size() == 1) ? rv.get(0) : null;
	}

	/**
	 * Transaction code for finding the archived sites associated with a user.
	 *
	 * @param user
	 *        The user.
	 * @return The ArchivedSite list for the user.
	 */
	protected List<ArchivedSite> archivesForUserTx(final User user)
	{
		String sql = "SELECT A.ID, A.SITE_ID, A.ARCHIVED_ON, A.NAME, "
				+ rosterService().sqlSelectClientFragment()
				+ ", "
				+ rosterService().sqlSelectTermFragment()
				+ " FROM ARCHIVE A JOIN ARCHIVE_OWNER O ON A.ID=O.ARCHIVE_ID LEFT OUTER JOIN CLIENT C ON A.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON A.TERM_ID = T.ID"
				+ " WHERE O.USER_ID = ? ORDER BY T.ID DESC, C.ABBREVIATION ASC, A.NAME ASC";

		Object[] fields = new Object[1];
		fields[0] = user.getId();
		List<ArchivedSite> rv = sqlService().select(sql, fields, new SqlService.Reader<ArchivedSite>()
		{
			@Override
			public ArchivedSite read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					Long siteId = sqlService().readLong(result, i++);
					Date archivedOn = sqlService().readDate(result, i++);
					String name = sqlService().readString(result, i++);

					Client client = rosterService().createClientFromResultSet(result, i);
					i += rosterService().sqlSelectClientFragmentNumFields();

					Term term = rosterService().createTermFromResultSet(result, i);
					i += rosterService().sqlSelectTermFragmentNumFields();

					ArchivedSiteImpl rv = new ArchivedSiteImpl(id, term, client, siteId, name, archivedOn);

					return rv;
				}
				catch (SQLException e)
				{
					M_log.warn("archivesTx(user): " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading an archived site.
	 *
	 * @param id
	 *        The archive id.
	 * @return The ArchivedSite.
	 */
	protected ArchivedSite archiveTx(final Long id)
	{
		String sql = "SELECT A.ID, A.SITE_ID, A.ARCHIVED_ON, A.NAME, " + rosterService().sqlSelectClientFragment() + ", "
				+ rosterService().sqlSelectTermFragment()
				+ " FROM ARCHIVE A LEFT OUTER JOIN CLIENT C ON A.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON A.TERM_ID = T.ID WHERE A.ID = ?";

		Object[] fields = new Object[1];
		fields[0] = id;
		List<ArchivedSite> rv = sqlService().select(sql, fields, new SqlService.Reader<ArchivedSite>()
		{
			@Override
			public ArchivedSite read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					Long siteId = sqlService().readLong(result, i++);
					Date archivedOn = sqlService().readDate(result, i++);
					String name = sqlService().readString(result, i++);

					Client client = rosterService().createClientFromResultSet(result, i);
					i += rosterService().sqlSelectClientFragmentNumFields();

					Term term = rosterService().createTermFromResultSet(result, i);
					i += rosterService().sqlSelectTermFragmentNumFields();

					ArchivedSiteImpl rv = new ArchivedSiteImpl(id, term, client, siteId, name, archivedOn);

					return rv;
				}
				catch (SQLException e)
				{
					M_log.warn("archivesTx(user): " + e);
					return null;
				}
			}
		});

		return (rv.size() == 1) ? rv.get(0) : null;
	}

	/**
	 * Form the folder path to the archive folder for this site.
	 * 
	 * @param siteId
	 *        The site id.
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @return The file path to the archive for this site.
	 */
	protected String filePath(ArchivedSite archivedSite)
	{
		String rv = this.fileSystemRoot + archivedSite.getTerm().getAbbreviation() + "/" + archivedSite.getClient().getAbbreviation() + "/"
				+ archivedSite.getSiteId() + "/";
		return rv;
	}

	/**
	 * Transaction code for inserting archived owner records.
	 * 
	 * @param archived
	 *        The archived site info.
	 */
	protected void insertArchivedOwnersTx(ArchivedSite archived, Membership roster)
	{
		Object[] fields = new Object[2];
		fields[0] = archived.getId();

		// one for each instructor member in the roster
		for (Member m : roster.getMembers())
		{
			if (m.getRole() == Role.instructor)
			{
				String sql = "INSERT INTO ARCHIVE_OWNER (ARCHIVE_ID, USER_ID) VALUES (?,?)";
				fields[1] = m.getUser().getId();
				sqlService().insert(sql, fields, "ID");
			}
		}
	}

	/**
	 * Transaction code for inserting an archived record.
	 * 
	 * @param archived
	 *        The archived site info.
	 */
	protected void insertArchivedTx(ArchivedSite archived)
	{
		String sql = "INSERT INTO ARCHIVE (SITE_ID, CLIENT_ID, TERM_ID, ARCHIVED_ON, NAME) VALUES (?,?,?,?,?)";

		Object[] fields = new Object[5];
		int i = 0;
		fields[i++] = archived.getSiteId();
		fields[i++] = archived.getClient().getId();
		fields[i++] = archived.getTerm().getId();
		fields[i++] = archived.getArchivedOn();
		fields[i++] = archived.getName();

		Long id = sqlService().insert(sql, fields, "ID");
		((ArchivedSiteImpl) archived).initId(id);
	}

	protected Artifact newArchiveArtifact(ArchivedSite site, Boolean authoredContentOnly, Archive archive)
	{
		Artifact a = archive.newArtifact(Tool.archive, "info");
		a.getProperties().put("siteId", site.getSiteId());
		a.getProperties().put("siteName", site.getName());
		a.getProperties().put("term", site.getTerm().getId());
		a.getProperties().put("client", site.getClient().getId());
		a.getProperties().put("id", site.getId());
		a.getProperties().put("archivedOn", site.getArchivedOn());
		a.getProperties().put("authoredContentOnly", authoredContentOnly);
		a.getProperties().put("service", this.service);

		return a;
	}

	protected Artifact newMyfileArtifact(File f, Archive archive)
	{
		Artifact a = archive.newArtifact(Tool.myfiles, "file");
		a.getProperties().put("date", f.getDate());
		a.getProperties().put("modifiedon", f.getModifiedOn());
		a.getProperties().put("id", f.getId());
		a.getProperties().put("name", f.getName());
		a.getProperties().put("size", Integer.valueOf(f.getSize()));
		a.getProperties().put("type", f.getType());
		a.getProperties().put("body", f.readStream());

		// check if there are any myfiles references
		List<Reference> refs = fileService().getReferences(f);
		for (Reference r : refs)
		{
			if (r.getHolder().getTool() == Tool.myfiles)
			{
				a.getProperties().put("myfiles", Boolean.TRUE);
				break;
			}
		}

		return a;
	}

	/**
	 * Read this archive
	 * 
	 * @param archivedSite
	 *        The archive site info.
	 * @return The archive, or null if not found.
	 */
	protected Archive openArchive(ArchivedSite archivedSite)
	{
		ArchiveImpl rv = new ArchiveImpl();

		// find it here
		String filePath = filePath(archivedSite);
		rv.setFilePath(filePath);

		// open it
		rv.open();

		return rv;
	}

	/**
	 * Transaction code for removing record of an archive.
	 * 
	 * @param archived
	 *        The archived site info.
	 */
	protected void removeArchiveTx(ArchivedSite archived)
	{
		Object[] fields = new Object[1];
		fields[0] = archived.getId();

		String sql = "DELETE FROM ARCHIVE_OWNER WHERE ARCHIVE_ID = ?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM ARCHIVE WHERE ID = ?";
		sqlService().update(sql, fields);

		((ArchivedSiteImpl) archived).initId(null);
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
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
	 * @return The registered SqlService.
	 */
	private SqlService sqlService()
	{
		return (SqlService) Services.get(SqlService.class);
	}
}
