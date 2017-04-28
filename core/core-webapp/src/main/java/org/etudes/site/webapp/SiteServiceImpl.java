/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/site/webapp/SiteServiceImpl.java $
 * $Id: SiteServiceImpl.java 10509 2015-04-17 21:50:49Z ggolden $
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

package org.etudes.site.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.Term;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Link;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.site.api.Skin;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.BaseDateService;
import org.etudes.sitecontent.api.DateProvider;
import org.etudes.sitecontent.api.DateRange;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.StudentContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * SiteServiceImpl implements SiteService.
 */
public class SiteServiceImpl implements SiteService, SiteContentHandler, DateProvider, StudentContentHandler, Service
{
	/** The skin id of the default skin. */
	protected final static Long SKIN_DEFAULT_ID = 1L;

	/** Our log. */
	private static Log M_log = LogFactory.getLog(SiteServiceImpl.class);

	/**
	 * Construct
	 */
	public SiteServiceImpl()
	{
		M_log.info("SiteServiceImpl: construct");
	}

	@Override
	public Site add(String name, Client client, Term term, User addedBy)
	{
		SiteImpl rv = new SiteImpl();

		rv.setLoaded();
		rv.initCreatedByUserId(addedBy.getId());
		rv.initCreatedOn(new Date());
		rv.setName(name);
		rv.setClient(client);
		rv.setTerm(term);

		save(addedBy, rv);

		return rv;
	}

	@Override
	public void adjustDatesByDays(Site site, int days, User adjustingUser)
	{
		site.setPublishOn(baseDateService().adjustDateByDays(site.getPublishOn(), days));
		site.setUnpublishOn(baseDateService().adjustDateByDays(site.getUnpublishOn(), days));

		save(adjustingUser, site);
	}

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public Site check(Long id)
	{
		Site rv = checkSiteTx(id);
		return rv;
	}

	@Override
	public void clear(Site site)
	{
		// TODO: clear site visits ?
	}

	@Override
	public void clear(Site site, User user)
	{
		// TODO: clear site visits ?
	}

	@Override
	public Integer count(Client client, Term term, String search)
	{
		Integer rv = countSitesTx(client, term, search);
		return rv;
	}

	@Override
	public Site createFromResultSet(ResultSet result, int i) throws SQLException
	{
		return createFromResultSet(result, i, null);
	}

	@Override
	public List<Site> find(Client client, Term term)
	{
		List<Site> rv = readSitesClientTermTx(client, term);
		return rv;
	}

	@Override
	public List<Site> find(Client client, Term term, String search, Boolean byTerm, Integer pageNum, Integer pageSize)
	{
		List<Site> rv = readSitesPageTx(client, term, search, byTerm, pageNum, pageSize);
		return rv;
	}

	@Override
	public Site get(Long id)
	{
		SiteImpl site = new SiteImpl();
		site.initId(id);

		Site rv = readSiteTx(site);
		return rv;
	}

	@Override
	public Site get(String name)
	{
		Site rv = readSiteByNameTx(name);
		return rv;
	}

	@Override
	public DateRange getDateRange(Site site)
	{
		Date[] minMax = new Date[2];
		baseDateService().computeMinMax(minMax, site.getPublishOn());
		baseDateService().computeMinMax(minMax, site.getUnpublishOn());

		if (minMax[0] == null) return null;
		return baseDateService().newDateRange(Tool.site, minMax);
	}

	@Override
	public Skin getSkin(Client client)
	{
		Skin rv = readSkinByClientTx(client);
		if (rv == null) rv = readSkinTx(SKIN_DEFAULT_ID);
		return rv;
	}

	@Override
	public Skin getSkin(Long id)
	{
		Skin rv = readSkinTx(id);
		if (rv == null) rv = readSkinTx(SKIN_DEFAULT_ID);
		return rv;
	}

	@Override
	public List<Skin> getSkins()
	{
		List<Skin> rv = readSkinsTx();
		return rv;
	}

	@Override
	public void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void importFromSite(Site fromSite, Site intoSite, User importingUser)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void purge(Site site)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void remove(final Site site)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeSiteTx(site);
			}
		}, "removeSite");
	}

	@Override
	public void save(User savedBy, final Site site)
	{
		if (((SiteImpl) site).isChanged() || site.getId() == null)
		{
			if (((SiteImpl) site).isChanged())
			{
				// set modified by/on
				((SiteImpl) site).initModifiedByUserId(savedBy.getId());
				((SiteImpl) site).initModifiedOn(new Date());
			}

			// insert or update
			if (site.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertSiteTx((SiteImpl) site);
						updateTools((SiteImpl) site);
						updateLinks((SiteImpl) site);
					}
				}, "insertSite");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						updateSiteTx((SiteImpl) site);
						updateTools((SiteImpl) site);
						updateLinks((SiteImpl) site);
					}
				}, "updateSite");
			}

			((SiteImpl) site).clearChanged();
		}
	}

	@Override
	public String sqlJoinFragment(String on)
	{
		return "JOIN SITE S ON "
				+ on
				+ " = S.ID LEFT OUTER JOIN CLIENT C ON S.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON S.TERM_ID = T.ID LEFT OUTER JOIN SKIN SK ON S.SKIN_ID = SK.ID LEFT OUTER JOIN SITE_TOOL SITE_T ON S.ID = SITE_T.SITE_ID";
	}

	@Override
	public String sqlSelectFragment()
	{
		return "S.ID, S.NAME, S.PUBLISHED, S.CREATED_BY, S.CREATED_ON, S.MODIFIED_BY, S.MODIFIED_ON, S.PUBLISH_ON, S.UNPUBLISH_ON, "
				+ rosterService().sqlSelectClientFragment() + ", " + rosterService().sqlSelectTermFragment()
				+ ", SK.ID, SK.NAME, SK.COLOR, SK.CLIENT_ID, GROUP_CONCAT(SITE_T.TOOL_ID)";
	}

	@Override
	public Integer sqlSelectFragmentNumFields()
	{
		return 9 + rosterService().sqlSelectClientFragmentNumFields() + rosterService().sqlSelectTermFragmentNumFields() + 5;
	}

	@Override
	public boolean start()
	{
		M_log.info("SiteServiceImpl: start");
		return true;
	}

	@Override
	public Site wrap(Long id)
	{
		SiteImpl site = new SiteImpl();
		site.initId(id);
		return site;
	}

	/**
	 * Transaction code for checking a site.
	 * 
	 * @param id
	 *        The site id.
	 */
	protected Site checkSiteTx(final Long id)
	{
		String sql = "SELECT ID FROM SITE WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<Site> rv = sqlService().select(sql, fields, new SqlService.Reader<Site>()
		{
			@Override
			public Site read(ResultSet result)
			{
				SiteImpl site = new SiteImpl();
				site.initId(id);
				return site;
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for counting the total sites across all pages with this criteria.
	 * 
	 * @param client
	 *        Client criteria - if specified, return sites only from this client.
	 * @param term
	 *        The term criteria - if specified, return sites only from this term.
	 * @param search
	 *        The search string - partial match against title if specified.
	 * @return The count of sites meeting the criteria.
	 */
	protected Integer countSitesTx(Client client, Term term, String search)
	{
		String sql = "SELECT COUNT(1) FROM SITE S LEFT OUTER JOIN CLIENT C ON S.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON S.TERM_ID = T.ID";

		int numFields = 0;
		String where = null;
		if (client != null)
		{
			where = " WHERE S.CLIENT_ID = ?";
			numFields++;
		}
		if (term != null)
		{
			if (where == null)
			{
				where = " WHERE S.TERM_ID = ?";
			}
			else
			{
				where += " AND S.TERM_ID = ?";
			}
			numFields++;
		}
		if (search != null)
		{
			if (where == null)
			{
				where = " WHERE S.NAME LIKE ?";
			}
			else
			{
				where += " AND S.NAME LIKE ?";
			}
			numFields++;
		}
		if (where != null) sql += where;

		Object[] fields = new Object[numFields];
		int i = 0;

		if (client != null)
		{
			fields[i++] = client.getId();
		}
		if (term != null)
		{
			fields[i++] = term.getId();
		}
		if (search != null)
		{
			fields[i++] = "%" + search + "%";
		}

		List<Integer> rv = sqlService().select(sql, fields, new SqlService.Reader<Integer>()
		{
			@Override
			public Integer read(ResultSet result)
			{
				try
				{
					int i = 1;
					Integer count = sqlService().readInteger(result, i++);
					return count;
				}
				catch (SQLException e)
				{
					M_log.warn("countSitesTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? Integer.valueOf(0) : rv.get(0);
	}

	protected Site createFromResultSet(ResultSet result, int i, SiteImpl site) throws SQLException
	{
		if (site == null) site = new SiteImpl();

		site.setLoaded();
		site.initId(sqlService().readLong(result, i++));
		site.initName(sqlService().readString(result, i++));
		site.initPublished(sqlService().readBoolean(result, i++));
		site.initCreatedByUserId(sqlService().readLong(result, i++));
		site.initCreatedOn(sqlService().readDate(result, i++));
		site.initModifiedByUserId(sqlService().readLong(result, i++));
		site.initModifiedOn(sqlService().readDate(result, i++));
		site.initPublishOn(sqlService().readDate(result, i++));
		site.initUnpublishOn(sqlService().readDate(result, i++));

		site.initClient(rosterService().createClientFromResultSet(result, i));
		i += rosterService().sqlSelectClientFragmentNumFields();

		site.initTerm(rosterService().createTermFromResultSet(result, i));
		i += rosterService().sqlSelectTermFragmentNumFields();

		Long id = sqlService().readLong(result, i++);
		String name = sqlService().readString(result, i++);
		String color = sqlService().readString(result, i++);
		Long client = sqlService().readLong(result, i++);
		SkinImpl skin = new SkinImpl(id, name, color, client);
		site.initSkin(skin);

		String toolIds = sqlService().readString(result, i++);
		site.initTools(toolIds);

		return site;
	}

	/**
	 * Transaction code for inserting a link in site.
	 * 
	 * @param site
	 *        The site.
	 * @param link
	 *        The link.
	 */
	protected void insertLinkTx(Site site, Link link)
	{
		String sql = "INSERT INTO SITE_LINK (SITE_ID, TITLE, URL, POSITION) VALUES (?,?,?,?)";

		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = link.getTitle();
		fields[i++] = link.getUrl();
		fields[i++] = link.getPosition();

		sqlService().insert(sql, fields, "ID");
	}

	/**
	 * Transaction code for inserting a site.
	 * 
	 * @param site
	 *        The site.
	 */
	protected void insertSiteTx(SiteImpl site)
	{
		String sql = "INSERT INTO SITE (NAME, CLIENT_ID, TERM_ID, PUBLISHED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON, PUBLISH_ON, UNPUBLISH_ON) VALUES (?,?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[10];
		int i = 0;
		fields[i++] = site.getName();
		fields[i++] = site.getClientId();
		fields[i++] = site.getTermId();
		fields[i++] = site.getPublished();
		fields[i++] = site.getCreatedByUserId();
		fields[i++] = site.getCreatedOn();
		fields[i++] = site.getModifiedByUserId();
		fields[i++] = site.getModifiedOn();
		fields[i++] = site.getPublishOn();
		fields[i++] = site.getUnpublishOn();

		Long id = sqlService().insert(sql, fields, "ID");
		site.initId(id);
		site.clearChanged();
	}

	/**
	 * Transaction code for inserting a tool in site.
	 * 
	 * @param site
	 *        The site.
	 * @param tool
	 *        The tool.
	 */
	protected void insertToolTx(Site site, Tool tool)
	{
		String sql = "INSERT INTO SITE_TOOL (SITE_ID, TOOL_ID) VALUES (?,?)";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = tool.getId();

		sqlService().insert(sql, fields, "ID");
	}

	/**
	 * Load up the full site data (all but tools).
	 * 
	 * @param site
	 *        The site to load.
	 */
	protected void load(SiteImpl site)
	{
		readSiteTx(site);
	}

	/**
	 * Load up the site's links.
	 * 
	 * @param site
	 *        The site.
	 */
	protected List<Link> loadLinks(SiteImpl site)
	{
		return readSiteLinksTx(site);
	}

	/**
	 * Load up the site's tools.
	 * 
	 * @param site
	 *        The site.
	 * @return The site's tool set.
	 */
	protected Set<Tool> loadTools(SiteImpl site)
	{
		return readSiteToolsTx(site);
	}

	/**
	 * Transaction code for reading a site by name.
	 * 
	 * @return The site read, or null if the site id is not found.
	 */
	protected SiteImpl readSiteByNameTx(final String name)
	{
		String sql = "SELECT "
				+ sqlSelectFragment()
				+ " FROM SITE S LEFT OUTER JOIN CLIENT C ON S.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON S.TERM_ID = T.ID LEFT OUTER JOIN SKIN SK ON S.SKIN_ID = SK.ID LEFT OUTER JOIN SITE_TOOL SITE_T ON S.ID = SITE_T.SITE_ID "
				+ "WHERE S.NAME = ? GROUP BY S.ID";
		Object[] fields = new Object[1];
		fields[0] = name;
		List<SiteImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<SiteImpl>()
		{
			@Override
			public SiteImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					SiteImpl site = (SiteImpl) createFromResultSet(result, i);
					return site;
				}
				catch (SQLException e)
				{
					M_log.warn("readSiteByNameTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading a site's links.
	 * 
	 * @param site
	 *        The site.
	 * @return The List of Link.
	 */
	protected List<Link> readSiteLinksTx(Site site)
	{
		String sql = "SELECT ID, TITLE, URL, POSITION FROM SITE_LINK WHERE SITE_ID = ? ORDER BY POSITION ASC";
		Object[] fields = new Object[1];
		fields[0] = site.getId();

		List<Link> rv = sqlService().select(sql, fields, new SqlService.Reader<Link>()
		{
			@Override
			public Link read(ResultSet result)
			{
				try
				{
					int i = 1;

					Long id = sqlService().readLong(result, i++);
					String title = sqlService().readString(result, i++);
					String url = sqlService().readString(result, i++);
					Integer position = sqlService().readInteger(result, i++);

					Link link = new LinkImpl(id, title, url, position);
					return link;
				}
				catch (SQLException e)
				{
					M_log.warn("readSiteLinksTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading all sites for this client in this term.
	 * 
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @return A List of Site, may be empty.
	 */
	protected List<Site> readSitesClientTermTx(final Client client, final Term term)
	{
		String sql = "SELECT "
				+ sqlSelectFragment()
				+ " FROM SITE S LEFT OUTER JOIN CLIENT C ON S.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON S.TERM_ID = T.ID LEFT OUTER JOIN SKIN SK ON S.SKIN_ID = SK.ID LEFT OUTER JOIN SITE_TOOL SITE_T ON S.ID = SITE_T.SITE_ID "
				+ " WHERE S.TERM_ID = ? AND S.CLIENT_ID = ? GROUP BY S.ID";
		Object[] fields = new Object[2];
		fields[0] = term.getId();
		fields[1] = client.getId();
		List<Site> rv = sqlService().select(sql, fields, new SqlService.Reader<Site>()
		{
			@Override
			public Site read(ResultSet result)
			{
				try
				{
					int i = 1;
					SiteImpl site = (SiteImpl) createFromResultSet(result, i);
					return site;
				}
				catch (SQLException e)
				{
					M_log.warn("readSitesClientTermTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading all sites for this client in this term.
	 * 
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @return A List of Site, may be empty.
	 */
	protected List<Site> readSitesPageTx(Client client, Term term, String search, Boolean byTerm, Integer pageNum, Integer pageSize)
	{
		String sql = "SELECT "
				+ sqlSelectFragment()
				+ " FROM SITE S LEFT OUTER JOIN CLIENT C ON S.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON S.TERM_ID = T.ID LEFT OUTER JOIN SKIN SK ON S.SKIN_ID = SK.ID LEFT OUTER JOIN SITE_TOOL SITE_T ON S.ID = SITE_T.SITE_ID";

		int numFields = 0;
		String where = null;
		if (client != null)
		{
			where = " WHERE S.CLIENT_ID = ?";
			numFields++;
		}
		if (term != null)
		{
			if (where == null)
			{
				where = " WHERE S.TERM_ID = ?";
			}
			else
			{
				where += " AND S.TERM_ID = ?";
			}
			numFields++;
		}
		if (search != null)
		{
			if (where == null)
			{
				where = " WHERE S.NAME LIKE ?";
			}
			else
			{
				where += " AND S.NAME LIKE ?";
			}
			numFields++;
		}
		if (where != null) sql += where;

		sql += " GROUP BY S.ID ";

		if (byTerm)
		{
			sql += " ORDER BY S.TERM_ID DESC, S.NAME ASC";
		}
		else
		{
			sql += " ORDER BY S.CREATED_ON DESC, S.NAME ASC";
		}

		sql += " LIMIT ?, ?";
		numFields += 2;

		Object[] fields = new Object[numFields];
		int i = 0;

		if (client != null)
		{
			fields[i++] = client.getId();
		}
		if (term != null)
		{
			fields[i++] = term.getId();
		}
		if (search != null)
		{
			fields[i++] = "%" + search + "%";
		}

		fields[i++] = Integer.valueOf((pageNum - 1) * pageSize);
		fields[i++] = Integer.valueOf(pageSize);

		List<Site> rv = sqlService().select(sql, fields, new SqlService.Reader<Site>()
		{
			@Override
			public SiteImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					SiteImpl site = (SiteImpl) createFromResultSet(result, i);
					return site;
				}
				catch (SQLException e)
				{
					M_log.warn("readSitesPageTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a site's tools.
	 * 
	 * @param site
	 *        The site.
	 * @return The set of Tool.
	 */
	protected Set<Tool> readSiteToolsTx(Site site)
	{
		String sql = "SELECT TOOL_ID FROM SITE_TOOL WHERE SITE_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();

		List<Tool> rv = sqlService().select(sql, fields, new SqlService.Reader<Tool>()
		{
			@Override
			public Tool read(ResultSet result)
			{
				try
				{
					int i = 1;

					Integer toolId = sqlService().readInteger(result, i++);
					Tool tool = Tool.valueOf(toolId);
					return tool;
				}
				catch (SQLException e)
				{
					M_log.warn("readSiteToolsTx: " + e);
					return null;
				}
			}
		});

		return new HashSet<Tool>(rv);
	}

	/**
	 * Transaction code for reading a site.
	 * 
	 * @param site
	 *        The site with id set, to fill in.
	 * @return The site read, or null if the site id is not found.
	 */
	protected SiteImpl readSiteTx(final SiteImpl site)
	{

		String sql = "SELECT "
				+ sqlSelectFragment()
				+ " FROM SITE S LEFT OUTER JOIN CLIENT C ON S.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON S.TERM_ID = T.ID LEFT OUTER JOIN SKIN SK ON S.SKIN_ID = SK.ID LEFT OUTER JOIN SITE_TOOL SITE_T ON S.ID = SITE_T.SITE_ID "
				+ " WHERE S.ID = ? GROUP BY S.ID";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<SiteImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<SiteImpl>()
		{
			@Override
			public SiteImpl read(ResultSet result)
			{
				site.setLoaded();
				try
				{
					int i = 1;
					createFromResultSet(result, i, site);
					return site;
				}
				catch (SQLException e)
				{
					M_log.warn("readSiteTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading a skin to use for this client.
	 * 
	 * @param client
	 *        The client.
	 * @return The skin for this client.
	 */
	protected SkinImpl readSkinByClientTx(Client client)
	{
		String sql = "SELECT ID, NAME, COLOR, CLIENT_ID FROM SKIN WHERE CLIENT_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = client.getId();
		List<SkinImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<SkinImpl>()
		{
			@Override
			public SkinImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					String color = sqlService().readString(result, i++);
					Long client = sqlService().readLong(result, i++);
					SkinImpl skin = new SkinImpl(id, name, color, client);
					return skin;
				}
				catch (SQLException e)
				{
					M_log.warn("readSkinByClientTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading all skins.
	 * 
	 * @param id
	 *        The skin id.
	 * @return The skin for this id.
	 */
	protected List<Skin> readSkinsTx()
	{
		String sql = "SELECT ID, NAME, COLOR, CLIENT_ID FROM SKIN ORDER BY ID ASC";
		List<Skin> rv = sqlService().select(sql, null, new SqlService.Reader<Skin>()
		{
			@Override
			public Skin read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					String color = sqlService().readString(result, i++);
					Long client = sqlService().readLong(result, i++);
					SkinImpl skin = new SkinImpl(id, name, color, client);
					return skin;
				}
				catch (SQLException e)
				{
					M_log.warn("readSkinTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a skin.
	 * 
	 * @param id
	 *        The skin id.
	 * @return The skin for this id.
	 */
	protected SkinImpl readSkinTx(Long id)
	{
		String sql = "SELECT ID, NAME, COLOR, CLIENT_ID FROM SKIN WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<SkinImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<SkinImpl>()
		{
			@Override
			public SkinImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					String color = sqlService().readString(result, i++);
					Long client = sqlService().readLong(result, i++);
					SkinImpl skin = new SkinImpl(id, name, color, client);
					return skin;
				}
				catch (SQLException e)
				{
					M_log.warn("readSkinTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for removing a link in site.
	 * 
	 * @param Link
	 *        The link.
	 */
	protected void removeLinkTx(Link link)
	{
		String sql = "DELETE FROM SITE_LINK WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = link.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a site.
	 * 
	 * @param site
	 *        The site.
	 */
	protected void removeSiteTx(Site site)
	{
		Object[] fields = new Object[1];
		fields[0] = site.getId();

		String sql = "DELETE FROM SITE_TOOL WHERE SITE_ID = ?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM SITE_LINK WHERE SITE_ID = ?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM SITE WHERE ID = ?";
		sqlService().update(sql, fields);

		((SiteImpl) site).initId(null);
	}

	/**
	 * Transaction code for removing a tool in site.
	 * 
	 * @param site
	 *        The site.
	 * @param Tool
	 *        The tool.
	 */
	protected void removeToolTx(Site site, Tool tool)
	{
		String sql = "DELETE FROM SITE_TOOL WHERE SITE_ID = ? AND TOOL_ID = ?";
		Object[] fields = new Object[2];
		fields[0] = site.getId();
		fields[1] = tool.getId();
		sqlService().update(sql, fields);
	}

	protected void updateLinks(SiteImpl site)
	{
		// don't force the load if it's not already loaded
		if (site.links == null) return;

		// what links to remove - entries in orig not in modified
		for (Link l : site.origLinks)
		{
			if (!site.links.contains(l))
			{
				removeLinkTx(l);
			}
		}

		// what to update - entries in orig and modified that are changed
		for (Link l : site.links)
		{
			if (site.origLinks.contains(l))
			{
				Link orig = site.origLinks.get(site.origLinks.indexOf(l));
				if (!l.exactlyEqual(orig))
				{
					updateLinkTx(l);
				}
			}
		}

		// what links to add - entries with id=0
		for (Link l : site.links)
		{
			if (l.getId() == 0)
			{
				insertLinkTx(site, l);
			}
		}
	}

	/**
	 * Transaction code for updating a link.
	 * 
	 * @param link
	 *        The link.
	 */
	protected void updateLinkTx(Link link)
	{
		String sql = "UPDATE SITE_LINK SET TITLE=?, URL=?, POSITION=? WHERE ID=?";

		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = link.getTitle();
		fields[i++] = link.getUrl();
		fields[i++] = link.getPosition();
		fields[i++] = link.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for updating an existing site.
	 * 
	 * @param site
	 *        The site.
	 */
	protected void updateSiteTx(SiteImpl site)
	{
		String sql = "UPDATE SITE SET NAME=?, CLIENT_ID=?, TERM_ID=?, PUBLISHED=?, SKIN_ID=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=?, PUBLISH_ON=?, UNPUBLISH_ON=? WHERE ID=?";

		Object[] fields = new Object[12];
		int i = 0;
		fields[i++] = site.getName();
		fields[i++] = site.getClientId();
		fields[i++] = site.getTermId();
		fields[i++] = site.getPublished();
		fields[i++] = site.getSkin().getId();
		fields[i++] = site.getCreatedByUserId();
		fields[i++] = site.getCreatedOn();
		fields[i++] = site.getModifiedByUserId();
		fields[i++] = site.getModifiedOn();
		fields[i++] = site.getPublishOn();
		fields[i++] = site.getUnpublishOn();

		fields[i++] = site.getId();

		sqlService().update(sql, fields);
		site.clearChanged();
	}

	protected void updateTools(SiteImpl site)
	{
		// don't force the load if it's not already loaded
		if (site.tools == null) return;

		// what tools to remove - entries in orig not in modified
		for (Tool t : site.origTools)
		{
			if (!site.tools.contains(t))
			{
				removeToolTx(site, t);
			}
		}

		// what tools to add - entries in modified not in orig
		for (Tool t : site.tools)
		{
			if (!site.origTools.contains(t))
			{
				insertToolTx(site, t);
			}
		}
	}

	/**
	 * @return The registered BaseDateService.
	 */
	private BaseDateService baseDateService()
	{
		return (BaseDateService) Services.get(BaseDateService.class);
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
