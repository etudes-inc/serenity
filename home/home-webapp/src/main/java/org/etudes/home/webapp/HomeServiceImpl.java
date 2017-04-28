/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-webapp/src/main/java/org/etudes/home/webapp/HomeServiceImpl.java $
 * $Id: HomeServiceImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.home.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.file.api.FileService;
import org.etudes.home.api.HomeItem;
import org.etudes.home.api.HomeItem.Status;
import org.etudes.home.api.HomeOptions;
import org.etudes.home.api.HomeService;
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
 * HomeServiceImpl implements HomeService.
 */
public class HomeServiceImpl implements HomeService, Service, SiteContentHandler, DateProvider
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(HomeServiceImpl.class);

	/**
	 * Construct
	 */
	public HomeServiceImpl()
	{
		M_log.info("HomeServiceImpl: construct");
	}

	@Override
	public void adjustDatesByDays(Site site, int days, User adjustingUser)
	{
		// TODO: release dates
		M_log.info("adjustDatesByDays");
	}

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		// TODO:
		M_log.info("archive");
	}

	@Override
	public DateRange getDateRange(Site site)
	{
		// TODO:
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
	public HomeItem itemAdd(User addedBy, Site inSite)
	{
		HomeItemImpl rv = new HomeItemImpl();
		rv.initSite(inSite);
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());

		itemSave(addedBy, rv);

		return rv;
	}

	@Override
	public HomeItem itemFindCurrent(Site site)
	{
		HomeItem rv = itemFindCurrentTx(site);
		return rv;
	}

	@Override
	public List<HomeItem> itemFindInSite(Site site)
	{
		// this orders published, by release date asc, then unpublished
		List<HomeItem> rv = itemFindBySiteTx(site);

		// put in order: current, pending by date asc, unpublished, past by date asc
		Date now = new Date();
		List<HomeItem> published = new ArrayList<HomeItem>();
		List<HomeItem> unpublished = new ArrayList<HomeItem>();
		List<HomeItem> pending = new ArrayList<HomeItem>();
		List<HomeItem> past = new ArrayList<HomeItem>();
		HomeItem current = null;
		for (HomeItem item : rv)
		{
			if (item.getPublished())
			{
				published.add(item);
			}
			else
			{
				unpublished.add(item);
			}
		}

		// find the current in the published
		for (HomeItem item : published)
		{
			if (!(item.getReleaseDate().after(now)))
			{
				past.add(item);
			}
			else
			{
				pending.add(item);
			}
		}

		// the last "past" is the current
		if (!past.isEmpty())
		{
			current = past.get(past.size() - 1);
			past.remove(past.size() - 1);
		}

		// mark the status, put in final order
		List<HomeItem> ordered = new ArrayList<HomeItem>();
		if (current != null)
		{
			((HomeItemImpl) current).initStatus(Status.current);
			ordered.add(current);
		}
		for (HomeItem item : pending)
		{
			((HomeItemImpl) item).initStatus(Status.pending);
			ordered.add(item);
		}
		for (HomeItem item : unpublished)
		{
			((HomeItemImpl) item).initStatus(Status.unpublished);
			ordered.add(item);
		}
		for (HomeItem item : past)
		{
			((HomeItemImpl) item).initStatus(Status.past);
			ordered.add(item);
		}

		return ordered;
	}

	@Override
	public HomeItem itemGet(Long id)
	{
		HomeItemImpl rv = new HomeItemImpl(id);
		HomeItem found = itemReadTx(rv);
		return found;
	}

	@Override
	public void itemRefresh(HomeItem item)
	{
		itemReadTx((HomeItemImpl) item);
	}

	@Override
	public void itemRemove(final HomeItem item)
	{
		// deal with the content - remove all our references
		fileService().removeExcept(item.getReference(), null);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				itemRemoveTx(item);
			}
		}, "remove");
	}

	@Override
	public void itemSave(User savedBy, final HomeItem item)
	{
		// enforce - nothing gets published without being valid
		if (item.getPublished() && !item.isValid())
		{
			item.setPublished(Boolean.FALSE);
		}

		if (((HomeItemImpl) item).isChanged() || item.getId() == null)
		{
			if (((HomeItemImpl) item).isChanged())
			{
				// set modified by/on
				((HomeItemImpl) item).initModifiedBy(savedBy);
				((HomeItemImpl) item).initModifiedOn(new Date());

				// deal with the content
				((HomeItemImpl) item).saveContent(savedBy);
			}

			// insert or update
			if (item.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertItemTx((HomeItemImpl) item);

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
						updateItemTx((HomeItemImpl) item);

					}
				}, "save(update)");
			}

			((HomeItemImpl) item).clearChanged();
		}
	}

	@Override
	public HomeOptions optionsGet(Site site)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void optionsSave(User savedBy, HomeOptions options)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void purge(Site site)
	{
		// TODO:
		M_log.info("purge");
	}

	@Override
	public boolean start()
	{
		M_log.info("HomeServiceImpl: start");
		return true;
	}

	/**
	 * Transaction code for inserting an item.
	 * 
	 * @param item
	 *        The item.
	 */
	protected void insertItemTx(HomeItemImpl item)
	{
		String sql = "INSERT INTO HOME_ITEM (SITE, PUBLISHED, RELEASE_DATE, SOURCE, TITLE, CONTENT, URL, DIMENSIONS, ALT, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[13];
		int i = 0;
		fields[i++] = item.getSite().getId();
		fields[i++] = item.getPublished();
		fields[i++] = item.getReleaseDate();
		fields[i++] = item.getSource().getCode();
		fields[i++] = item.getTitle();
		fields[i++] = item.getContentReferenceId();
		fields[i++] = item.getUrl();
		fields[i++] = item.getDimensions();
		fields[i++] = item.getAlt();
		fields[i++] = item.getCreatedBy().getId();
		fields[i++] = item.getCreatedOn();
		fields[i++] = item.getModifiedBy().getId();
		fields[i++] = item.getModifiedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		item.initId(id);
	}

	/**
	 * Transaction code for reading the items for a site.
	 * 
	 * @param site
	 *        The site.
	 */
	protected List<HomeItem> itemFindBySiteTx(final Site site)
	{
		String sql = "SELECT ID, PUBLISHED, RELEASE_DATE, SOURCE, TITLE, CONTENT, URL, DIMENSIONS, ALT, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM HOME_ITEM WHERE SITE=?"
				+ " ORDER BY PUBLISHED DESC, RELEASE_DATE ASC, TITLE ASC";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<HomeItem> rv = sqlService().select(sql, fields, new SqlService.Reader<HomeItem>()
		{
			@Override
			public HomeItem read(ResultSet result)
			{
				HomeItemImpl item = new HomeItemImpl();
				item.initSite(site);
				try
				{
					int i = 1;
					item.initId(sqlService().readLong(result, i++));
					item.initPublished(sqlService().readBoolean(result, i++));
					item.initReleaseDate(sqlService().readDate(result, i++));
					item.initSource(HomeItem.Source.fromCode(sqlService().readString(result, i++)));
					item.initTitle(sqlService().readString(result, i++));
					item.initContentReferenceId(sqlService().readLong(result, i++));
					item.initUrl(sqlService().readString(result, i++));
					item.initDimensions(sqlService().readString(result, i++));
					item.initAlt(sqlService().readString(result, i++));
					item.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					item.initCreatedOn(sqlService().readDate(result, i++));
					item.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					item.initModifiedOn(sqlService().readDate(result, i++));
					item.setLoaded();

					return item;
				}
				catch (SQLException e)
				{
					M_log.warn("itemFindBySiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading the current item for a site.
	 * 
	 * @param site
	 *        The site.
	 */
	protected HomeItem itemFindCurrentTx(final Site site)
	{
		String sql = "SELECT ID, PUBLISHED, RELEASE_DATE, SOURCE, TITLE, CONTENT, URL, DIMENSIONS, ALT, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM HOME_ITEM WHERE SITE=?"
				+ " AND PUBLISHED=1 AND RELEASE_DATE < ? ORDER BY RELEASE_DATE DESC LIMIT 1";
		Object[] fields = new Object[2];
		fields[0] = site.getId();
		fields[1] = new Date();
		List<HomeItem> rv = sqlService().select(sql, fields, new SqlService.Reader<HomeItem>()
		{
			@Override
			public HomeItem read(ResultSet result)
			{
				HomeItemImpl item = new HomeItemImpl();
				item.initSite(site);
				try
				{
					int i = 1;
					item.initId(sqlService().readLong(result, i++));
					item.initPublished(sqlService().readBoolean(result, i++));
					item.initReleaseDate(sqlService().readDate(result, i++));
					item.initSource(HomeItem.Source.fromCode(sqlService().readString(result, i++)));
					item.initTitle(sqlService().readString(result, i++));
					item.initContentReferenceId(sqlService().readLong(result, i++));
					item.initUrl(sqlService().readString(result, i++));
					item.initDimensions(sqlService().readString(result, i++));
					item.initAlt(sqlService().readString(result, i++));
					item.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					item.initCreatedOn(sqlService().readDate(result, i++));
					item.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					item.initModifiedOn(sqlService().readDate(result, i++));

					item.initStatus(HomeItem.Status.current);

					item.setLoaded();

					return item;
				}
				catch (SQLException e)
				{
					M_log.warn("itemFindCurrentTx: " + e);
					return null;
				}
			}
		});

		if (rv.size() > 0)
		{
			return rv.get(0);
		}

		return null;
	}

	/**
	 * Transaction code for reading an item.
	 * 
	 * @param item
	 *        the item (with id set) to read and fill in.
	 */
	protected HomeItem itemReadTx(final HomeItemImpl item)
	{
		String sql = "SELECT SITE, PUBLISHED, RELEASE_DATE, SOURCE, TITLE, CONTENT, URL, DIMENSIONS, ALT, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM HOME_ITEM WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = item.getId();
		List<HomeItem> rv = sqlService().select(sql, fields, new SqlService.Reader<HomeItem>()
		{
			@Override
			public HomeItem read(ResultSet result)
			{
				try
				{
					int i = 1;
					item.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					item.initPublished(sqlService().readBoolean(result, i++));
					item.initReleaseDate(sqlService().readDate(result, i++));
					item.initSource(HomeItem.Source.fromCode(sqlService().readString(result, i++)));
					item.initTitle(sqlService().readString(result, i++));
					item.initContentReferenceId(sqlService().readLong(result, i++));
					item.initUrl(sqlService().readString(result, i++));
					item.initDimensions(sqlService().readString(result, i++));
					item.initAlt(sqlService().readString(result, i++));
					item.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					item.initCreatedOn(sqlService().readDate(result, i++));
					item.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					item.initModifiedOn(sqlService().readDate(result, i++));
					item.setLoaded();

					return item;
				}
				catch (SQLException e)
				{
					M_log.warn("itemReadTx: " + e);
					return null;
				}
			}
		});

		if (rv.size() > 0)
		{
			return item;
		}
		return null;
	}

	/**
	 * Transaction code for removing a home item.
	 * 
	 * @param item
	 *        The item.
	 */
	protected void itemRemoveTx(HomeItem item)
	{
		String sql = "DELETE FROM HOME_ITEM WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = item.getId();
		sqlService().update(sql, fields);
		((HomeItemImpl) item).initId(null);
	}

	/**
	 * Transaction code for updating an existing item.
	 * 
	 * @param item
	 *        The item.
	 */
	protected void updateItemTx(HomeItemImpl item)
	{
		String sql = "UPDATE HOME_ITEM SET SITE=?, PUBLISHED=?, RELEASE_DATE=?, SOURCE=?, TITLE=?, CONTENT=?, URL=?, DIMENSIONS=?, ALT=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";

		Object[] fields = new Object[14];
		int i = 0;
		fields[i++] = item.getSite().getId();
		fields[i++] = item.getPublished();
		fields[i++] = item.getReleaseDate();
		fields[i++] = item.getSource().getCode();
		fields[i++] = item.getTitle();
		fields[i++] = item.getContentReferenceId();
		fields[i++] = item.getUrl();
		fields[i++] = item.getDimensions();
		fields[i++] = item.getAlt();
		fields[i++] = item.getCreatedBy().getId();
		fields[i++] = item.getCreatedOn();
		fields[i++] = item.getModifiedBy().getId();
		fields[i++] = item.getModifiedOn();

		fields[i++] = item.getId();

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
