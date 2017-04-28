/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/java/org/etudes/syllabus/webapp/SyllabusServiceImpl.java $
 * $Id: SyllabusServiceImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.syllabus.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.StudentContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.syllabus.api.Syllabus;
import org.etudes.syllabus.api.SyllabusAcceptance;
import org.etudes.syllabus.api.SyllabusExternal;
import org.etudes.syllabus.api.SyllabusSection;
import org.etudes.syllabus.api.SyllabusService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * SyllabusServiceImpl implements SyllabusService.
 */
public class SyllabusServiceImpl implements SyllabusService, Service, SiteContentHandler, StudentContentHandler
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SyllabusServiceImpl.class);

	/**
	 * Construct
	 */
	public SyllabusServiceImpl()
	{
		M_log.info("SyllabusServiceImpl: construct");
	}

	@Override
	public SyllabusAcceptance accept(Syllabus syllabus, User user)
	{
		final SyllabusAcceptanceImpl rv = new SyllabusAcceptanceImpl(syllabus, user, new Date());
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				insertSyllabusAcceptanceTx(rv);
			}
		}, "save(insert syllabus)");

		// will be marked as not valid if it was already accepted
		if (rv.isValid()) return rv;

		// if already accepted, return that
		return getAccepted(syllabus, user);
	}

	@Override
	public Syllabus add(User addedBy, Site inSite)
	{
		// find existing
		Syllabus existing = findBySite(inSite);
		if (existing.getId() != null) return existing;

		// else create new
		SyllabusImpl rv = new SyllabusImpl();
		rv.initSite(inSite);
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());

		save(addedBy, rv);

		return rv;
	}

	@Override
	public SyllabusSection addSection(User addedBy, Syllabus syllabus)
	{
		// make sure the syllabus is really there
		if (syllabus.getId() == null) this.save(addedBy, syllabus);

		// make sure the syllabus has loaded sections so far
		syllabus.getSections();

		SyllabusSectionImpl rv = new SyllabusSectionImpl();
		rv.initSyllabusId(syllabus.getId());
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());
		rv.initOrder(((SyllabusImpl) syllabus).sections.size() + 1);
		this.save(addedBy, rv);

		// add this very section object to the syllabus's sections
		((SyllabusImpl) syllabus).sections.add(rv);

		return rv;
	}

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		// read the syllabus data for the site
		Syllabus syllabus = findBySite(fromSite);
		if (syllabus.getId() != null)
		{
			// make an artifact for the syllabus information
			Artifact artifact = toArchive.newArtifact(Tool.syllabus, "info"); // "/syllabus/0" - which was only there when the syllabus was external

			// source
			artifact.getProperties().put("source", syllabus.getSource().getCode());

			artifact.getProperties().put("createdbyId", syllabus.getCreatedBy().getId());
			artifact.getProperties().put("createdon", syllabus.getCreatedOn());
			artifact.getProperties().put("modifiedbyId", syllabus.getModifiedBy().getId());
			artifact.getProperties().put("modifiedon", syllabus.getModifiedOn());
			artifact.getProperties().put("id", syllabus.getId());

			toArchive.archive(artifact);

			// external definition
			SyllabusExternal external = syllabus.getExternal();
			if (external.getUrl() != null)
			{
				artifact = toArchive.newArtifact(Tool.syllabus, "external");
				artifact.getProperties().put("url", external.getUrl()); // was "redirect" in /syllabus/0
				artifact.getProperties().put("height", external.getHeight()); // was not
				artifact.getProperties().put("newWindow", external.getNewWindow()); // was "openWindow" in /syllabus/0
				toArchive.archive(artifact);
			}

			// sections
			for (SyllabusSection section : syllabus.getSections())
			{
				artifact = toArchive.newArtifact(Tool.syllabus, "section");

				artifact.getProperties().put("position", section.getOrder()); // was "position" in /syllabus/<position>
				artifact.getProperties().put("content", ((SyllabusSectionImpl) section).getContentReference().getFile()); // was "body", the actual text
				artifact.getProperties().put("title", section.getTitle());
				artifact.getProperties().put("public", section.getPublic()); // was "pubview" "status" was ??? attachments was ???
				artifact.getProperties().put("published", section.getPublished());
				artifact.getProperties().put("createdbyId", section.getCreatedBy().getId());
				artifact.getProperties().put("createdon", section.getCreatedOn());
				artifact.getProperties().put("modifiedbyId", section.getModifiedBy().getId());
				artifact.getProperties().put("modifiedon", section.getModifiedOn());
				artifact.getProperties().put("id", section.getId());

				List<Reference> refs = fileService().getReferences(section.getReference());
				artifact.addReferences(refs);

				toArchive.archive(artifact);
			}
		}
	}

	@Override
	public void clear(Site site)
	{
		final Syllabus syllabus = findBySite(site);
		// remove acceptance records
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeSyllabusAcceptanceTx(syllabus);
			}
		}, "clear(site)");
	}

	@Override
	public void clear(Site site, final User user)
	{
		final Syllabus syllabus = findBySite(site);
		// remove acceptance record for user
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeSyllabusAcceptanceTx(syllabus, user);
			}
		}, "clear(site, user)");
	}

	@Override
	public Syllabus findBySite(Site inSite)
	{
		Syllabus rv = findSyllabusBySiteTx(inSite);

		if (rv == null)
		{
			SyllabusImpl syllabus = new SyllabusImpl();
			syllabus.initSite(inSite);
			rv = syllabus;
		}

		return rv;
	}

	@Override
	public Syllabus get(Long id)
	{
		Syllabus rv = readSyllabusTx(id);
		return rv;
	}

	@Override
	public Map<User, SyllabusAcceptance> getAccepted(Syllabus syllabus)
	{
		Map<User, SyllabusAcceptance> rv = readSyllabusAcceptanceTx(syllabus);
		return rv;
	}

	@Override
	public SyllabusAcceptance getAccepted(Syllabus syllabus, User user)
	{
		SyllabusAcceptance rv = readSyllabusAcceptanceTx(syllabus, user);
		return rv;
	}

	@Override
	public SyllabusSection getSection(Long id)
	{
		SyllabusSection rv = readSectionTx(id);
		return rv;
	}

	@Override
	public List<SyllabusSection> getSections(Syllabus syllabus)
	{
		List<SyllabusSection> rv = readSectionsTx(syllabus.getId());
		return rv;
	}

	@Override
	public void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser)
	{
		// get or create the site syllabus
		Syllabus syllabus = findBySite(intoSite);
		boolean newSyllabus = (syllabus.getId() == null);
		if (fromArtifact.getType().equals("info"))
		{
			if (newSyllabus)
			{
				Syllabus.Source source = Syllabus.Source.fromCode((String) fromArtifact.getProperties().get("source"));
				syllabus.setSource(source);
				// created / modified info?
			}
		}
		else if (fromArtifact.getType().equals("external"))
		{
			// only import external info if we have no external URL
			if (newSyllabus || (syllabus.getExternal().getUrl() == null))
			{
				syllabus.getExternal().setUrl((String) fromArtifact.getProperties().get("url"));
				Integer height = (Integer) fromArtifact.getProperties().get("height");
				if (height != null) syllabus.getExternal().setHeight(height);
				Boolean newWindow = (Boolean) fromArtifact.getProperties().get("newWindow");
				if (newWindow != null) syllabus.getExternal().setNewWindow(newWindow);
			}
		}
		else if (fromArtifact.getType().equals("section"))
		{
			// only import a section if we have no section with the same title
			String title = (String) fromArtifact.getProperties().get("title");
			if (newSyllabus || (syllabus.findSectionByTitle(title) == null))
			{
				SyllabusSection section = this.addSection(importingUser, syllabus);
				section.setTitle(title);

				File content = (File) fromArtifact.getProperties().get("content");
				section.shareContent(content);

				section.setPublic((Boolean) fromArtifact.getProperties().get("public"));
				// leave draft section.setPublished((Boolean) fromArtifact.getProperties().get("published"));

				// auto position at the end, assuming we get the artifacts in position order, and we add after existing sections
				// created / modified info?
			}
		}

		// old format types
		else if (fromArtifact.getType().equals("/syllabus/0"))
		{
			// only import external info if we have no external URL
			if (newSyllabus || (syllabus.getExternal().getUrl() == null))
			{
				syllabus.setSource(Syllabus.Source.external);
				syllabus.getExternal().setUrl((String) fromArtifact.getProperties().get("redirect"));
				syllabus.getExternal().setNewWindow(((String) fromArtifact.getProperties().get("openWindow")).equals("true"));
			}
		}

		else if (fromArtifact.getType().startsWith("/syllabus/"))
		{
			// only import a section if we have no section with the same title
			String title = (String) fromArtifact.getProperties().get("title");
			if (newSyllabus || (syllabus.findSectionByTitle(title) == null))
			{
				SyllabusSection section = this.addSection(importingUser, syllabus);
				section.setTitle(title);

				String content = (String) fromArtifact.getProperties().get("body");

				// translate old style CHS references
				content = fromArtifact.getArchive().translateContentBody(content);

				// and set it into the section
				section.setContent(content, false);

				section.setPublic(((String) fromArtifact.getProperties().get("pubview")).equals("yes"));
				// leave draft section.setPublished( ((String) fromArtifact.getProperties().get("status")).equals("Posted") or "Draft");

				// auto position at the end, assuming we get the artifacts in position order, and we add after existing sections
				// created / modified info?
			}
		}

		save(importingUser, syllabus);
	}

	@Override
	public void importFromSite(Site fromSite, Site toSite, User importingUser)
	{
		Syllabus source = findBySite(fromSite);
		if (source.getId() == null) return;

		// create or find the syllabus for the toSite
		Syllabus dest = add(importingUser, toSite);

		// set the url from the source, unless we already have one
		if ((dest.getExternal().getUrl() == null) && (source.getExternal().getUrl() != null))
		{
			dest.getExternal().setUrl(source.getExternal().getUrl());
			dest.getExternal().setHeight(source.getExternal().getHeight());
			dest.getExternal().setNewWindow(source.getExternal().getNewWindow());
		}

		for (SyllabusSection section : source.getSections())
		{
			// skip if we have a section with this title
			if (dest.findSectionByTitle(section.getTitle()) != null) continue;

			// make a new section
			SyllabusSection newSection = addSection(importingUser, dest);
			newSection.setTitle(section.getTitle());
			newSection.setPublic(section.getPublic());
			// TODO: order
			newSection.setPublished(Boolean.FALSE);

			// for content, make a new reference to the same file
			Reference ref = ((SyllabusSectionImpl) section).getContentReference();
			if (ref != null)
			{
				newSection.shareContent(ref.getFile());
			}

			save(importingUser, newSection);
		}

		save(importingUser, dest);
	}

	@Override
	public void purge(Site site)
	{
		Syllabus syllabus = findBySite(site);
		if (syllabus.getId() != null) remove(syllabus);
	}

	@Override
	public void remove(final Syllabus syllabus)
	{
		// first the sections
		List<SyllabusSection> sections = syllabus.getSections();
		for (SyllabusSection section : sections)
		{
			removeSection(section);
		}

		// next the acceptance records
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeSyllabusAcceptanceTx(syllabus);
			}
		}, "remove(syllabus)");

		// now the syllabus
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeSyllabusTx(syllabus);
			}
		}, "remove(syllabus)");
	}

	@Override
	public void remove(User removedBy, SyllabusSection section)
	{
		// update the syllabus modified by/on
		SyllabusImpl syllabus = (SyllabusImpl) section.getSyllabus();
		syllabus.initModifiedBy(removedBy);
		syllabus.initModifiedOn(new Date());
		save(removedBy, syllabus);

		removeSection(section);
	}

	@Override
	public void save(User savedBy, final Syllabus syllabus)
	{
		if (((SyllabusImpl) syllabus).isChanged() || syllabus.getId() == null)
		{
			Date now = new Date();

			// for new syllabus, set created by/on
			if (syllabus.getId() == null)
			{
				((SyllabusImpl) syllabus).initCreatedBy(savedBy);
				((SyllabusImpl) syllabus).initCreatedOn(now);
			}

			// set modified by/on
			((SyllabusImpl) syllabus).initModifiedBy(savedBy);
			((SyllabusImpl) syllabus).initModifiedOn(now);

			// insert or update
			if (syllabus.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertSyllabusTx((SyllabusImpl) syllabus);

					}
				}, "save(insert syllabus)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						updateSyllabusTx((SyllabusImpl) syllabus);

					}
				}, "save(update syllabus)");
			}

			for (SyllabusSection section : syllabus.getSections())
			{
				save(savedBy, section);
			}

			for (SyllabusSection section : ((SyllabusImpl) syllabus).getRemovedSections())
			{
				remove(savedBy, section);
			}
			((SyllabusImpl) syllabus).clearRemovedSections();

			((SyllabusImpl) syllabus).clearChanged();
		}
	}

	@Override
	public void save(User savedBy, final SyllabusSection section)
	{
		if (((SyllabusSectionImpl) section).isChanged() || section.getId() == null)
		{
			if (((SyllabusSectionImpl) section).isChanged())
			{
				// set modified by/on
				((SyllabusSectionImpl) section).initModifiedBy(savedBy);
				((SyllabusSectionImpl) section).initModifiedOn(new Date());

				// deal with the content
				((SyllabusSectionImpl) section).saveContent(savedBy);
			}

			// insert or update
			if (section.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertSectionTx((SyllabusSectionImpl) section);
					}
				}, "save(insert section)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						updateSectionTx((SyllabusSectionImpl) section);

					}
				}, "save(update section)");
			}

			((SyllabusSectionImpl) section).clearChanged();
		}
	}

	@Override
	public boolean start()
	{
		M_log.info("SyllabusServiceImpl: start");
		return true;
	}

	/**
	 * Transaction code for reading the syllabus for a site.
	 * 
	 * @param siteId
	 *        the site id.
	 */
	protected Syllabus findSyllabusBySiteTx(final Site site)
	{
		String sql = "SELECT ID, SOURCE, URL, HEIGHT, NEWWINDOW, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM SYLLABUS WHERE SITE = ?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<Syllabus> rv = sqlService().select(sql, fields, new SqlService.Reader<Syllabus>()
		{
			@Override
			public Syllabus read(ResultSet result)
			{
				SyllabusImpl syllabus = new SyllabusImpl();
				syllabus.initSite(site);
				try
				{
					int i = 1;
					syllabus.initId(sqlService().readLong(result, i++));
					syllabus.initSource(Syllabus.Source.fromCode(sqlService().readString(result, i++)));
					((SyllabusExternalImpl) syllabus.getExternal()).initUrl(sqlService().readString(result, i++));
					((SyllabusExternalImpl) syllabus.getExternal()).initHeight(sqlService().readInteger(result, i++));
					((SyllabusExternalImpl) syllabus.getExternal()).initNewWindow(sqlService().readBoolean(result, i++));
					syllabus.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					syllabus.initCreatedOn(sqlService().readDate(result, i++));
					syllabus.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					syllabus.initModifiedOn(sqlService().readDate(result, i++));
					return syllabus;
				}
				catch (SQLException e)
				{
					M_log.warn("findSyllabusBySiteTx: " + e);
					return null;
				}
			}
		});

		// should find 0 or 1
		if (rv.size() > 1) M_log.warn("too many syllabus records for site: " + site.getId() + " = " + rv.size());
		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for inserting a section.
	 * 
	 * @param section
	 *        The section.
	 */
	protected void insertSectionTx(SyllabusSectionImpl section)
	{
		String sql = "INSERT INTO SYLLABUS_SECTION (SYLLABUS_ID, TITLE, CONTENT, PUBLISHED, ISPUBLIC, SECTIONORDER, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[10];
		int i = 0;
		fields[i++] = section.getSyllabusId();
		fields[i++] = section.getTitle();
		fields[i++] = section.getContentReferenceId();
		fields[i++] = section.getPublished();
		fields[i++] = section.getPublic();
		fields[i++] = section.getOrder();
		fields[i++] = section.getCreatedBy().getId();
		fields[i++] = section.getCreatedOn();
		fields[i++] = section.getModifiedBy().getId();
		fields[i++] = section.getModifiedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		section.initId(id);
	}

	/**
	 * Transaction code for inserting a syllabus acceptance.
	 * 
	 * @param syllabusAcceptance
	 *        The syllabus acceptance.
	 * @return null if the insert failed due to constraint violation (i.e. the user already accepted), 0L if successful.
	 */
	protected void insertSyllabusAcceptanceTx(SyllabusAcceptanceImpl syllabusAcceptance)
	{
		String sql = "INSERT INTO SYLLABUS_ACCEPTANCE (SYLLABUS_ID, ACCEPTED_BY, ACCEPTED_ON) VALUES (?,?,?)";

		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = syllabusAcceptance.getSyllabusId();
		fields[i++] = syllabusAcceptance.getAcceptedBy().getId();
		fields[i++] = syllabusAcceptance.getAcceptedOn();

		Long rv = sqlService().insert(sql, fields, null);
		syllabusAcceptance.initValid(rv != null);
	}

	/**
	 * Transaction code for inserting a syllabus.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 */
	protected void insertSyllabusTx(SyllabusImpl syllabus)
	{
		String sql = "INSERT INTO SYLLABUS (SITE, SOURCE, URL, HEIGHT, NEWWINDOW, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
				+ " VALUES (?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[9];
		int i = 0;
		fields[i++] = syllabus.getSite().getId();
		fields[i++] = syllabus.getSource().getCode();
		fields[i++] = syllabus.getExternal().getUrl();
		fields[i++] = syllabus.getExternal().getHeight();
		fields[i++] = syllabus.getExternal().getNewWindow();
		fields[i++] = syllabus.getCreatedBy().getId();
		fields[i++] = syllabus.getCreatedOn();
		fields[i++] = syllabus.getModifiedBy().getId();
		fields[i++] = syllabus.getModifiedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		syllabus.initId(id);
	}

	protected List<SyllabusSection> readSectionsTx(final Long syllabusId)
	{
		String sql = "SELECT ID, TITLE, CONTENT, PUBLISHED, ISPUBLIC, SECTIONORDER, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM SYLLABUS_SECTION WHERE SYLLABUS_ID=? ORDER BY SECTIONORDER ASC";
		Object[] fields = new Object[1];
		fields[0] = syllabusId;

		List<SyllabusSection> rv = sqlService().select(sql, fields, new SqlService.Reader<SyllabusSection>()
		{
			@Override
			public SyllabusSection read(ResultSet result)
			{
				SyllabusSectionImpl entry = new SyllabusSectionImpl();
				entry.initSyllabusId(syllabusId);
				try
				{
					int i = 1;

					entry.initId(sqlService().readLong(result, i++));
					entry.initTitle(sqlService().readString(result, i++));
					entry.initContentReferenceId(sqlService().readLong(result, i++));
					entry.initPublished(sqlService().readBoolean(result, i++));
					entry.initPublic(sqlService().readBoolean(result, i++));
					entry.initOrder(sqlService().readInteger(result, i++));
					entry.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initCreatedOn(sqlService().readDate(result, i++));
					entry.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initModifiedOn(sqlService().readDate(result, i++));

					return entry;
				}
				catch (SQLException e)
				{
					M_log.warn("readSectionsTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a section.
	 * 
	 * @param id
	 *        The section id.
	 */
	protected SyllabusSection readSectionTx(final Long id)
	{
		String sql = "SELECT SYLLABUS_ID, TITLE, CONTENT, PUBLISHED, ISPUBLIC, SECTIONORDER, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM SYLLABUS_SECTION WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<SyllabusSection> rv = sqlService().select(sql, fields, new SqlService.Reader<SyllabusSection>()
		{
			@Override
			public SyllabusSection read(ResultSet result)
			{
				SyllabusSectionImpl entry = new SyllabusSectionImpl();
				entry.initId(id);
				try
				{
					int i = 1;

					entry.initSyllabusId(sqlService().readLong(result, i++));
					entry.initTitle(sqlService().readString(result, i++));
					entry.initContentReferenceId(sqlService().readLong(result, i++));
					entry.initPublished(sqlService().readBoolean(result, i++));
					entry.initPublic(sqlService().readBoolean(result, i++));
					entry.initOrder(sqlService().readInteger(result, i++));
					entry.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initCreatedOn(sqlService().readDate(result, i++));
					entry.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					entry.initModifiedOn(sqlService().readDate(result, i++));

					return entry;
				}
				catch (SQLException e)
				{
					M_log.warn("readSectionTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Read all the syllabus acceptance records for this syllabus.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 * @param user
	 *        The user.
	 * @return A map of SyllabusAcceptance object to the user accepting, may be empty.
	 */
	protected Map<User, SyllabusAcceptance> readSyllabusAcceptanceTx(final Syllabus syllabus)
	{
		String sql = "SELECT ACCEPTED_BY, ACCEPTED_ON FROM SYLLABUS_ACCEPTANCE WHERE SYLLABUS_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = syllabus.getId();
		final Map<User, SyllabusAcceptance> rv = new HashMap<User, SyllabusAcceptance>();
		sqlService().select(sql, fields, new SqlService.Reader<SyllabusAcceptance>()
		{
			@Override
			public SyllabusAcceptance read(ResultSet result)
			{
				try
				{
					int i = 1;
					User acceptedBy = userService().wrap(sqlService().readLong(result, i++));
					Date acceptedOn = sqlService().readDate(result, i++);
					SyllabusAcceptanceImpl accepted = new SyllabusAcceptanceImpl(syllabus, acceptedBy, acceptedOn);

					rv.put(acceptedBy, accepted);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSyllabusAcceptanceTx(map): " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Read the syllabus acceptance record for this syllabus and user.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 * @param user
	 *        The user.
	 * @return The acceptance record, or null if not found.
	 */
	protected SyllabusAcceptance readSyllabusAcceptanceTx(final Syllabus syllabus, final User user)
	{
		String sql = "SELECT ACCEPTED_ON FROM SYLLABUS_ACCEPTANCE WHERE SYLLABUS_ID = ? AND ACCEPTED_BY = ?";
		Object[] fields = new Object[2];
		fields[0] = syllabus.getId();
		fields[1] = user.getId();
		List<SyllabusAcceptance> rv = sqlService().select(sql, fields, new SqlService.Reader<SyllabusAcceptance>()
		{
			@Override
			public SyllabusAcceptance read(ResultSet result)
			{
				try
				{
					int i = 1;
					Date acceptedOn = sqlService().readDate(result, i++);
					SyllabusAcceptanceImpl rv = new SyllabusAcceptanceImpl(syllabus, user, acceptedOn);

					return rv;
				}
				catch (SQLException e)
				{
					M_log.warn("readSyllabusAcceptanceTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading a syllabus.
	 * 
	 * @param id
	 *        The syllabus id.
	 */
	protected Syllabus readSyllabusTx(final Long id)
	{
		String sql = "SELECT SITE, SOURCE, URL, HEIGHT, NEWWINDOW, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM SYLLABUS WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<Syllabus> rv = sqlService().select(sql, fields, new SqlService.Reader<Syllabus>()
		{
			@Override
			public Syllabus read(ResultSet result)
			{
				SyllabusImpl syllabus = new SyllabusImpl();
				syllabus.initId(id);
				try
				{
					int i = 1;
					syllabus.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					syllabus.initSource(Syllabus.Source.fromCode(sqlService().readString(result, i++)));
					((SyllabusExternalImpl) syllabus.getExternal()).initUrl(sqlService().readString(result, i++));
					((SyllabusExternalImpl) syllabus.getExternal()).initHeight(sqlService().readInteger(result, i++));
					((SyllabusExternalImpl) syllabus.getExternal()).initNewWindow(sqlService().readBoolean(result, i++));
					syllabus.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					syllabus.initCreatedOn(sqlService().readDate(result, i++));
					syllabus.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					syllabus.initModifiedOn(sqlService().readDate(result, i++));
					return syllabus;
				}
				catch (SQLException e)
				{
					M_log.warn("readSyllabusTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Do the work of removing a section.
	 * 
	 * @param section
	 *        The section to remove.
	 */
	protected void removeSection(final SyllabusSection section)
	{
		// deal with the content - remove all our references
		fileService().removeExcept(section.getReference(), null);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeSectionTx(section);
			}
		}, "removeSection");
	}

	/**
	 * Transaction code for removing a section.
	 * 
	 * @param section
	 *        The section to remove.
	 */
	protected void removeSectionTx(SyllabusSection section)
	{
		String sql = "DELETE FROM SYLLABUS_SECTION WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = section.getId();
		sqlService().update(sql, fields);
		((SyllabusSectionImpl) section).initId(null);
	}

	/**
	 * Transaction code for removing a syllabus's acceptance records.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 */
	protected void removeSyllabusAcceptanceTx(Syllabus syllabus)
	{
		String sql = "DELETE FROM SYLLABUS_ACCEPTANCE WHERE SYLLABUS_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = syllabus.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a syllabus's acceptance records from this user.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 * @param user
	 *        The user.
	 */
	protected void removeSyllabusAcceptanceTx(Syllabus syllabus, User user)
	{
		String sql = "DELETE FROM SYLLABUS_ACCEPTANCE WHERE SYLLABUS_ID = ? AND ACCEPTED_BY = ?";
		Object[] fields = new Object[2];
		fields[0] = syllabus.getId();
		fields[1] = user.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a syllabus.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 */
	protected void removeSyllabusTx(Syllabus syllabus)
	{
		String sql = "DELETE FROM SYLLABUS WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = syllabus.getId();
		sqlService().update(sql, fields);
		((SyllabusImpl) syllabus).initId(null);
	}

	/**
	 * Transaction code for updating an existing syllabus section.
	 * 
	 * @param section
	 *        The section.
	 */
	protected void updateSectionTx(SyllabusSectionImpl section)
	{
		String sql = "UPDATE SYLLABUS_SECTION SET SYLLABUS_ID=?, TITLE=?, CONTENT=?, PUBLISHED=?, ISPUBLIC=?, SECTIONORDER=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";

		Object[] fields = new Object[11];
		int i = 0;
		fields[i++] = section.getSyllabusId();
		fields[i++] = section.getTitle();
		fields[i++] = section.getContentReferenceId();
		fields[i++] = section.getPublished();
		fields[i++] = section.getPublic();
		fields[i++] = section.getOrder();
		fields[i++] = section.getCreatedBy().getId();
		fields[i++] = section.getCreatedOn();
		fields[i++] = section.getModifiedBy().getId();
		fields[i++] = section.getModifiedOn();

		fields[i++] = section.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for updating an existing syllabus.
	 * 
	 * @param syllabus
	 *        The syllabus.
	 */
	protected void updateSyllabusTx(SyllabusImpl syllabus)
	{
		String sql = "UPDATE SYLLABUS SET SITE=?, SOURCE=?, URL=?, HEIGHT=?, NEWWINDOW=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";

		Object[] fields = new Object[10];
		int i = 0;
		fields[i++] = syllabus.getSite().getId();
		fields[i++] = syllabus.getSource().getCode();
		fields[i++] = syllabus.getExternal().getUrl();
		fields[i++] = syllabus.getExternal().getHeight();
		fields[i++] = syllabus.getExternal().getNewWindow();
		fields[i++] = syllabus.getCreatedBy().getId();
		fields[i++] = syllabus.getCreatedOn();
		fields[i++] = syllabus.getModifiedBy().getId();
		fields[i++] = syllabus.getModifiedOn();

		fields[i++] = syllabus.getId();

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
