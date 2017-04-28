/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/announcement/announcement-webapp/src/main/java/org/etudes/announcement/webapp/AnnouncementServiceImpl.java $
 * $Id: AnnouncementServiceImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.announcement.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.announcement.api.Announcement;
import org.etudes.announcement.api.AnnouncementService;
import org.etudes.file.api.FileService;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.DateProvider;
import org.etudes.sitecontent.api.DateRange;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * AnnouncementServiceImpl implements AnnouncementService.
 */
public class AnnouncementServiceImpl implements AnnouncementService, Service, SiteContentHandler, DateProvider
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AnnouncementServiceImpl.class);

	/**
	 * Construct
	 */
	public AnnouncementServiceImpl()
	{
		M_log.info("AnnouncementServiceImpl: construct");
	}

	@Override
	public Announcement add(User addedBy, Site inSite)
	{
		// get the count of existing announcements, so this can be ordered to follow (1 based)
		Integer count = countBySite(inSite);

		AnnouncementImpl rv = new AnnouncementImpl();
		rv.initSite(inSite);
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());

		rv.initOrder(count.intValue() + 1);

		save(addedBy, rv);

		return rv;
	}

	@Override
	public void adjustDatesByDays(Site site, int days, User adjustingUser)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		// TODO:
		M_log.info("archive");
	}

	@Override
	public Announcement check(Long id)
	{
		AnnouncementImpl rv = announcementCheckTx(id);
		return rv;
	}

	@Override
	public Integer countBySite(Site inSite)
	{
		return announcementCountBySiteTx(inSite);
	}

	@Override
	public List<Announcement> findBySite(Site inSite)
	{
		List<Announcement> rv = announcementFindBySiteTx(inSite);
		return rv;
	}

	@Override
	public List<Announcement> findTopByUserSites(User user)
	{
		List<Announcement> rv = announcementFindTopByUserSitesTx(user);
		return rv;
	}

	@Override
	public List<Announcement> findTopInSites(Site site, Integer n)
	{
		List<Announcement> rv = announcementFindTopBySiteTx(site, n);
		return rv;
	}

	@Override
	public Announcement get(Long id)
	{
		AnnouncementImpl rv = new AnnouncementImpl(id);
		announcementReadTx(rv);
		return rv;
	}

	@Override
	public DateRange getDateRange(Site site)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser)
	{
		// TODO:
		M_log.info("importFromArchive");
	}

	@Override
	public void importFromSite(Site fromSite, Site toSite, User importingUser)
	{
		// TODO:
		M_log.info("importFromSite");
	}

	@Override
	public void purge(Site site)
	{
		List<Announcement> announcements = findBySite(site);
		for (Announcement a : announcements)
		{
			remove(a);
		}
	}

	@Override
	public void refresh(Announcement announcement)
	{
		announcementReadTx((AnnouncementImpl) announcement);
	}

	@Override
	public void remove(final Announcement announcement)
	{
		// deal with the content - remove all our references
		fileService().removeExcept(announcement.getReference(), null);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				announcementRemoveTx(announcement);
			}
		}, "remove");
	}

	@Override
	public void save(User savedBy, final Announcement announcement)
	{
		if (((AnnouncementImpl) announcement).isChanged() || announcement.getId() == null)
		{
			if (((AnnouncementImpl) announcement).isChanged())
			{
				// set modified by/on
				((AnnouncementImpl) announcement).initModifiedBy(savedBy);
				((AnnouncementImpl) announcement).initModifiedOn(new Date());

				// deal with the content
				((AnnouncementImpl) announcement).saveContent(savedBy);
			}

			// insert or update
			if (announcement.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertAnnouncementTx((AnnouncementImpl) announcement);

					}
				}, "save(insert)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						updateAnnouncementTx((AnnouncementImpl) announcement);

					}
				}, "save(update)");
			}

			((AnnouncementImpl) announcement).clearChanged();
		}
	}

	@Override
	public boolean start()
	{
		M_log.info("AnnouncementServiceImpl: start");
		return true;
	}

	@Override
	public Announcement wrap(Long id)
	{
		Announcement rv = new AnnouncementImpl(id);
		return rv;
	}

	/**
	 * Transaction code for checking an announcement.
	 * 
	 * @param id
	 *        The announcement id.
	 * @return a wrapped Announcement object if found, null if not.
	 */
	protected AnnouncementImpl announcementCheckTx(final Long id)
	{
		String sql = "SELECT ID FROM ANNOUNCEMENT WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<AnnouncementImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<AnnouncementImpl>()
		{
			@Override
			public AnnouncementImpl read(ResultSet result)
			{
				AnnouncementImpl assessment = new AnnouncementImpl(id);
				return assessment;
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for counting the announcements for a site.
	 * 
	 * @param siteId
	 *        the site id.
	 */
	protected Integer announcementCountBySiteTx(final Site site)
	{
		String sql = "SELECT COUNT(ID) FROM ANNOUNCEMENT WHERE SITE=?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<Integer> rv = sqlService().select(sql, fields, new SqlService.Reader<Integer>()
		{
			@Override
			public Integer read(ResultSet result)
			{
				try
				{
					int i = 1;
					Integer rv = sqlService().readInteger(result, i++);
					return rv;
				}
				catch (SQLException e)
				{
					M_log.warn("announcementCountBySiteTx: " + e);
					return null;
				}
			}
		});

		return rv.get(0);
	}

	/**
	 * Transaction code for reading the announcement for a site.
	 * 
	 * @param siteId
	 *        the site id.
	 */
	protected List<Announcement> announcementFindBySiteTx(final Site site)
	{
		String sql = "SELECT ID, SUBJECT, CONTENT, RELEASE_ON, PUBLISHED, ISPUBLIC, SITEORDER, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM ANNOUNCEMENT WHERE SITE=? ORDER BY SITEORDER ASC";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<Announcement> rv = sqlService().select(sql, fields, new SqlService.Reader<Announcement>()
		{
			@Override
			public Announcement read(ResultSet result)
			{
				AnnouncementImpl announcement = new AnnouncementImpl();
				announcement.initSite(site);
				try
				{
					int i = 1;
					announcement.initId(sqlService().readLong(result, i++));
					announcement.initSubject(sqlService().readString(result, i++));
					announcement.initContentReferenceId(sqlService().readLong(result, i++));
					announcement.initReleaseDate(sqlService().readDate(result, i++));
					announcement.initPublished(sqlService().readBoolean(result, i++));
					announcement.initPublic(sqlService().readBoolean(result, i++));
					announcement.initOrder(sqlService().readInteger(result, i++));
					announcement.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initCreatedOn(sqlService().readDate(result, i++));
					announcement.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initModifiedOn(sqlService().readDate(result, i++));
					announcement.setLoaded();

					return announcement;
				}
				catch (SQLException e)
				{
					M_log.warn("announcementFindBySiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading the top n released announcement for a site.
	 * 
	 * @param siteId
	 *        the site id.
	 */
	protected List<Announcement> announcementFindTopBySiteTx(final Site site, Integer n)
	{
		String sql = "SELECT ID, SUBJECT, CONTENT, RELEASE_ON, PUBLISHED, ISPUBLIC, SITEORDER, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM ANNOUNCEMENT"
				+ " WHERE SITE=? AND PUBLISHED=1 AND ((RELEASE_ON IS NULL) OR (RELEASE_ON < ?))" + " ORDER BY SITEORDER ASC LIMIT ?";
		Object[] fields = new Object[3];
		fields[0] = site.getId();
		fields[1] = new Date();
		fields[2] = n;
		List<Announcement> rv = sqlService().select(sql, fields, new SqlService.Reader<Announcement>()
		{
			@Override
			public Announcement read(ResultSet result)
			{
				AnnouncementImpl announcement = new AnnouncementImpl();
				announcement.initSite(site);
				try
				{
					int i = 1;
					announcement.initId(sqlService().readLong(result, i++));
					announcement.initSubject(sqlService().readString(result, i++));
					announcement.initContentReferenceId(sqlService().readLong(result, i++));
					announcement.initReleaseDate(sqlService().readDate(result, i++));
					announcement.initPublished(sqlService().readBoolean(result, i++));
					announcement.initPublic(sqlService().readBoolean(result, i++));
					announcement.initOrder(sqlService().readInteger(result, i++));
					announcement.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initCreatedOn(sqlService().readDate(result, i++));
					announcement.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initModifiedOn(sqlService().readDate(result, i++));
					announcement.setLoaded();

					return announcement;
				}
				catch (SQLException e)
				{
					M_log.warn("announcementFindTopBySiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading the top 1 announcement for a user's sites.
	 * 
	 * @param user
	 *        the user.
	 */
	protected List<Announcement> announcementFindTopByUserSitesTx(User user)
	{
		String sql = "SELECT A.ID, A.SITE, S.NAME, A.SUBJECT, A.CONTENT, A.RELEASE_ON, A.PUBLISHED, A.ISPUBLIC, A.SITEORDER, A.CREATED_BY, A.CREATED_ON, A.MODIFIED_BY, A.MODIFIED_ON FROM ANNOUNCEMENT A "
				+ rosterService().sqlJoinItemBySiteFragment("A.SITE")
				+ " WHERE A.PUBLISHED = '1' AND A.SITEORDER = 1 ORDER BY S.TERM_ID DESC, S.NAME ASC";

		Object[] fields = new Object[1];
		fields[0] = user.getId();
		List<Announcement> rv = sqlService().select(sql, fields, new SqlService.Reader<Announcement>()
		{
			@Override
			public Announcement read(ResultSet result)
			{
				AnnouncementImpl announcement = new AnnouncementImpl();
				try
				{
					int i = 1;
					announcement.initId(sqlService().readLong(result, i++));
					announcement.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					announcement.initSiteName(sqlService().readString(result, i++));
					announcement.initSubject(sqlService().readString(result, i++));
					announcement.initContentReferenceId(sqlService().readLong(result, i++));
					announcement.initReleaseDate(sqlService().readDate(result, i++));
					announcement.initPublished(sqlService().readBoolean(result, i++));
					announcement.initPublic(sqlService().readBoolean(result, i++));
					announcement.initOrder(sqlService().readInteger(result, i++));
					announcement.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initCreatedOn(sqlService().readDate(result, i++));
					announcement.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initModifiedOn(sqlService().readDate(result, i++));
					announcement.setLoaded();

					return announcement;
				}
				catch (SQLException e)
				{
					M_log.warn("announcementFindTopByUserSitesTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading an announcement.
	 * 
	 * @param announcement
	 *        the announcement (with id set) to read and fill in.
	 */
	protected void announcementReadTx(final AnnouncementImpl announcement)
	{
		String sql = "SELECT SITE, SUBJECT, CONTENT, RELEASE_ON, PUBLISHED, ISPUBLIC, SITEORDER, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM ANNOUNCEMENT WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = announcement.getId();
		List<Announcement> rv = sqlService().select(sql, fields, new SqlService.Reader<Announcement>()
		{
			@Override
			public Announcement read(ResultSet result)
			{
				try
				{
					int i = 1;
					announcement.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					announcement.initSubject(sqlService().readString(result, i++));
					announcement.initContentReferenceId(sqlService().readLong(result, i++));
					announcement.initReleaseDate(sqlService().readDate(result, i++));
					announcement.initPublished(sqlService().readBoolean(result, i++));
					announcement.initPublic(sqlService().readBoolean(result, i++));
					announcement.initOrder(sqlService().readInteger(result, i++));
					announcement.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initCreatedOn(sqlService().readDate(result, i++));
					announcement.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					announcement.initModifiedOn(sqlService().readDate(result, i++));
					announcement.setLoaded();

					return announcement;
				}
				catch (SQLException e)
				{
					M_log.warn("announcementReadTx: " + e);
					return null;
				}
			}
		});
	}

	/**
	 * Transaction code for removing an announcement.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void announcementRemoveTx(Announcement announcement)
	{
		String sql = "DELETE FROM ANNOUNCEMENT WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = announcement.getId();
		sqlService().update(sql, fields);
		((AnnouncementImpl) announcement).initId(null);
	}

	/**
	 * Transaction code for inserting an announcement.
	 * 
	 * @param announcement
	 *        The announcement.
	 */
	protected void insertAnnouncementTx(AnnouncementImpl announcement)
	{
		String sql = "INSERT INTO ANNOUNCEMENT (SITE, SUBJECT, CONTENT, RELEASE_ON, PUBLISHED, ISPUBLIC, SITEORDER, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[11];
		int i = 0;
		fields[i++] = announcement.getSite().getId();
		fields[i++] = announcement.getSubject();
		fields[i++] = announcement.getContentReferenceId();
		fields[i++] = announcement.getReleaseDate();
		fields[i++] = announcement.getPublished();
		fields[i++] = announcement.getPublic();
		fields[i++] = announcement.getOrder();
		fields[i++] = announcement.getCreatedBy().getId();
		fields[i++] = announcement.getCreatedOn();
		fields[i++] = announcement.getModifiedBy().getId();
		fields[i++] = announcement.getModifiedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		announcement.initId(id);
	}

	/**
	 * Transaction code for updating an existing announcement.
	 * 
	 * @param announcement
	 *        The announcement.
	 */
	protected void updateAnnouncementTx(AnnouncementImpl announcement)
	{
		String sql = "UPDATE ANNOUNCEMENT SET SITE=?, SUBJECT=?, CONTENT=?, RELEASE_ON=?, PUBLISHED=?, ISPUBLIC=?, SITEORDER=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";

		Object[] fields = new Object[12];
		int i = 0;
		fields[i++] = announcement.getSite().getId();
		fields[i++] = announcement.getSubject();
		fields[i++] = announcement.getContentReferenceId();
		fields[i++] = announcement.getReleaseDate();
		fields[i++] = announcement.getPublished();
		fields[i++] = announcement.getPublic();
		fields[i++] = announcement.getOrder();
		fields[i++] = announcement.getCreatedBy().getId();
		fields[i++] = announcement.getCreatedOn();
		fields[i++] = announcement.getModifiedBy().getId();
		fields[i++] = announcement.getModifiedOn();

		fields[i++] = announcement.getId();

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
