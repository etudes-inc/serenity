/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/tracking/webapp/TrackingServiceImpl.java $
 * $Id: TrackingServiceImpl.java 11178 2015-06-30 19:46:08Z ggolden $
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

package org.etudes.tracking.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cron.api.CronFrequency;
import org.etudes.cron.api.CronHandler;
import org.etudes.cron.api.RunTime;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.tracking.api.TrackingService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

public class TrackingServiceImpl implements TrackingService, CronHandler, Service
{
	/** Presence is good for this long (ms) */
	protected static long MAX_PRESENCE_DURATION = 30 * 1000; // TODO: need some padding?

	/** Our log. */
	private static Log M_log = LogFactory.getLog(TrackingServiceImpl.class);

	@Override
	public void clear(final Site site)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeSiteTrackingTx(site);
				removeSitePresenceTx(site);
			}
		}, "clear(site)");
	}

	@Override
	public void clear(final User user)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeUserTrackingTx(user);
				removeUserPresenceTx(user);
			}
		}, "clear(user)");
	}

	@Override
	public Long countPresence(Site site, Tool tool, Long itemId)
	{
		Long rv = countPresenceTx(site, tool, itemId);

		return rv;
	}

	@Override
	public CronFrequency cronGetFrequency()
	{
		return CronFrequency.fiveMins;
	}

	@Override
	public RunTime[] cronGetRunTimes()
	{
		return null;
	}

	@Override
	public void cronRun()
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeExpiredPresenceTx();

			}
		}, "removeExpiredPresenceTx");
	}

	@Override
	public List<User> getPresence(Site site, Tool tool, Long itemId)
	{
		List<User> rv = getPresenceTx(site, tool, itemId);

		// sort by display name
		Collections.sort(rv, new User.NameDisplayComparator());

		return rv;
	}

	@Override
	public void registerPresence(final User user, final Site site, final Tool tool, final Long itemId)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				insertPresenceTx(user, site, tool, itemId);
			}
		}, "registerPresence");
	}

	@Override
	public boolean start()
	{
		M_log.info("TrackingServiceImpl: start");
		return true;
	}

	@Override
	public void track(User user)
	{
		// track at site 0
		track(user, siteService().wrap(0L));
	}

	@Override
	public void track(final User user, final Site site)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				insertTrackingTx(user, site, new Date());
			}
		}, "track");
	}

	/**
	 * Transaction code for counting presence in a site.
	 * 
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 * @param itemId
	 *        The item ID.
	 */
	protected Long countPresenceTx(final Site site, Tool tool, Long itemId)
	{
		// ignore expired entries
		String sql = "SELECT COUNT(*) FROM PRESENCE P WHERE P.SITE_ID = ? AND P.TOOL_ID = ? AND P.ITEM_ID = ? AND P.ASOF > ?";
		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = tool.getId();
		fields[i++] = itemId;
		fields[i++] = expireCutoff();
		List<Long> rv = sqlService().select(sql, fields, new SqlService.Reader<Long>()
		{
			@Override
			public Long read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long rv = sqlService().readLong(result, i++);

					return rv;
				}
				catch (SQLException e)
				{
					M_log.warn("countPresenceTx: " + e);
					return null;
				}
			}
		});

		return (rv.size() == 1) ? rv.get(0) : Long.valueOf(0);
	}

	/**
	 * Compute the cutoff date, on and before which any presence has expired, based on now.
	 * 
	 * @return The cutoff date.
	 */
	protected Date expireCutoff()
	{
		Date rv = new Date(new Date().getTime() - MAX_PRESENCE_DURATION);
		return rv;
	}

	/**
	 * Transaction code for reading presence in a site.
	 * 
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 * @param itemId
	 *        The item ID.
	 */
	protected List<User> getPresenceTx(final Site site, Tool tool, Long itemId)
	{
		// ignore expired entries; each user shows only once per location
		String sql = "SELECT " + userService().sqlSelectFragment() + " FROM PRESENCE P " + userService().sqlJoinFragment("P.USER_ID")
				+ " WHERE P.SITE_ID = ? AND P.TOOL_ID = ? AND P.ITEM_ID = ? AND P.ASOF > ? GROUP BY " + userService().sqlGroupFragment();
		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = tool.getId();
		fields[i++] = itemId;
		fields[i++] = expireCutoff();
		List<User> rv = sqlService().select(sql, fields, new SqlService.Reader<User>()
		{
			@Override
			public User read(ResultSet result)
			{
				try
				{
					int i = 1;
					User user = userService().createFromResultSet(result, i);

					return user;
				}
				catch (SQLException e)
				{
					M_log.warn("getPresenceTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for inserting presence, or updating the ASOF of existing presence.
	 * 
	 * @param user
	 *        The user.
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 * @param itemId
	 *        The item ID.
	 */
	protected void insertPresenceTx(User user, Site site, Tool tool, Long itemId)
	{
		String sql = "INSERT INTO PRESENCE (SITE_ID, TOOL_ID, ITEM_ID, USER_ID, ASOF) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE ASOF=VALUES(ASOF)";

		Object[] fields = new Object[5];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = tool.getId();
		fields[i++] = itemId;
		fields[i++] = user.getId();
		fields[i++] = new Date();

		sqlService().insert(sql, fields, null);
	}

	/**
	 * Transaction code for inserting presence, or updating the ASOF of existing presence.
	 * 
	 * @param user
	 *        The user.
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 * @param itemId
	 *        The item ID.
	 */
	protected void insertTrackingTx(User user, Site site, Date date)
	{
		String sql = "INSERT INTO TRACKING (SITE_ID, USER_ID, FIRST_VISIT, LAST_VISIT, VISITS) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE VISITS=VISITS+1, LAST_VISIT=VALUES(LAST_VISIT)";

		Object[] fields = new Object[5];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = user.getId();
		fields[i++] = date;
		fields[i++] = date;
		fields[i++] = 1L;

		sqlService().insert(sql, fields, null);
	}

	/**
	 * Transaction code for removing expired presence records.
	 */
	protected void removeExpiredPresenceTx()
	{
		String sql = "DELETE FROM PRESENCE WHERE ASOF < ?";

		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = expireCutoff();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing presence for a site.
	 * 
	 * @param site
	 *        The site.
	 */
	protected void removeSitePresenceTx(Site site)
	{
		String sql = "DELETE FROM PRESENCE WHERE SITE_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing tracking for a site.
	 * 
	 * @param site
	 *        The site.
	 */
	protected void removeSiteTrackingTx(Site site)
	{
		String sql = "DELETE FROM TRACKING WHERE SITE_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing presence for a user.
	 * 
	 * @param user
	 *        The user.
	 */
	protected void removeUserPresenceTx(User user)
	{
		String sql = "DELETE FROM PRESENCE WHERE USER_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing tracking for a user.
	 * 
	 * @param user
	 *        The user.
	 */
	protected void removeUserTrackingTx(User user)
	{
		String sql = "DELETE FROM TRACKING WHERE USER_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		sqlService().update(sql, fields);
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
