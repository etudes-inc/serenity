/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/roster/webapp/RosterServiceImpl.java $
 * $Id: RosterServiceImpl.java 11450 2015-08-13 20:03:01Z ggolden $
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

package org.etudes.roster.webapp;

import static org.etudes.util.Different.differentIgnoreCase;
import static org.etudes.util.StringUtil.split;
import static org.etudes.util.StringUtil.trimToNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.cron.api.CronFrequency;
import org.etudes.cron.api.CronHandler;
import org.etudes.cron.api.CronService;
import org.etudes.cron.api.RunTime;
import org.etudes.email.api.EmailService;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.Roster;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.SiteMember;
import org.etudes.roster.api.Term;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Link;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.site.webapp.SiteImpl;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.Iid;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * RosterServiceImpl implements RosterService.
 */
public class RosterServiceImpl implements RosterService, Service, CronHandler
{
	protected class RosterCache
	{
		public Map<String, Roster> rosters = new HashMap<String, Roster>();

		public Roster get(String name, Client client, Term term)
		{
			String key = name + "_" + client.getId().toString() + "_" + term.getId().toString();
			Roster rv = this.rosters.get(key);
			if (rv == null)
			{
				rv = getRoster(name, client, term);
				if (rv != null) this.rosters.put(key, rv);
			}

			return rv;
		}
	}

	protected class RosterLine
	{
		Boolean active;
		Client client;
		String eid;
		String email;
		String firstName;
		String iid;
		String lastName;
		String line;
		String pw;
		Role role;
		String rosterName;
		String siteName;
		Term term;
		String userType;
	}

	protected final static String ADHOC_SOURCE = "Adhoc Lines";

	// acceptable strings in roster lines for roles (case insensitive)
	// protected final static String TYPE_EVALUATOR = "evaluator";

	protected final static String TYPE_INSTRUCTOR = "instructor";

	protected final static String TYPE_STUDENT = "student";

	protected final static String TYPE_TA = "teachingassistant";

	/** Our log. */
	private static Log M_log = LogFactory.getLog(RosterServiceImpl.class);

	/** Is this the app server to run roster files? */
	protected boolean processRosterFiles = false;

	/** The file system root for roster files. */
	protected String processRosterFilesFileSystemRoot = null;

	/** The schedule for file processing. */
	protected RunTime[] processRosterFilesSchedule = null;

	/**
	 * Construct
	 */
	public RosterServiceImpl()
	{
		// find out if this is the server to process roster files
		Services.whenStarted(new Runnable()
		{
			public void run()
			{
				String rosterServer = configService().getString("RosterService.server");
				String serverName = configService().getString("server");
				if (serverName.equals(rosterServer))
				{
					String[] runTimes = configService().getStrings("RosterService.schedule");
					if ((runTimes != null) && (runTimes.length > 0))
					{
						processRosterFilesFileSystemRoot = configService().getString("RosterService.fileSystemRoot");

						StringBuilder buf = new StringBuilder();
						buf.append("RosterServiceImpl()[whenStarted]: automatic roster file processing: fs:");
						buf.append(processRosterFilesFileSystemRoot);
						buf.append(" - ");

						processRosterFiles = true;
						processRosterFilesSchedule = new RunTime[runTimes.length];
						for (int i = 0; i < runTimes.length; i++)
						{
							String time = runTimes[i];
							processRosterFilesSchedule[i] = cronService().runTime(time);
							buf.append(processRosterFilesSchedule[i].toString());
							buf.append(" ");
						}

						M_log.info(buf.toString());
					}
					else
					{
						M_log.warn("RosterServiceImpl()[whenStarted]: no RosterService.schedule found");
					}
				}
			}
		});

		M_log.info("RosterServiceImpl: construct");
	}

	@Override
	public void addMapping(final Site site, final Roster roster)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				addMappingTx(site, roster);

			}
		}, "addMapping");
	}

	@Override
	public Roster addRoster(String name, Boolean official, Client client, Term term)
	{
		final RosterImpl roster = new RosterImpl(null, name, official, client, term);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				insertRosterTx(roster);

			}
		}, "addRoster");

		return roster;
	}

	@Override
	public void block(Site site, User user)
	{
		addBlockedTx(site, user);
	}

	@Override
	public Boolean checkBlocked(Site site, User user)
	{
		Boolean rv = checkBlockedTx(site, user);
		return rv;
	}

	@Override
	public Boolean checkMapping(Site site, Roster roster)
	{
		Boolean rv = checkMapingTx(site, roster);
		return rv;
	}

	@Override
	public void clear(Site site)
	{
		clearSiteRosters(site, false);
	}

	@Override
	public Client createClientFromResultSet(ResultSet result, int i) throws SQLException
	{
		ClientImpl client = new ClientImpl();

		client.initId(sqlService().readLong(result, i++));
		client.initName(sqlService().readString(result, i++));
		client.initAbbreviation(sqlService().readString(result, i++));
		client.initIidCode(sqlService().readString(result, i++));

		return client;
	}

	@Override
	public Site createSite(String name, Client client, Term term)
	{
		Site rv = null;

		// if we have a template for the client, create the site based on the template
		String templateName = client.getAbbreviation() + " TEMPLATE DEV";
		Site template = siteService().get(templateName);
		if (template != null)
		{
			// TODO:
			rv = createDefaultSite(name, client, term);
		}

		// otherwise, create it from defaults
		else
		{
			rv = createDefaultSite(name, client, term);
		}

		// create the adhoc roster, mapped to the site
		Roster adhoc = addRoster(adhocRosterName(rv), Boolean.FALSE, client, term);
		addMapping(rv, adhoc);

		// create the master roster, mapped to the site
		Roster master = addRoster(masterRosterName(rv), Boolean.TRUE, client, term);
		addMapping(rv, master);

		return rv;
	}

	@Override
	public Term createTermFromResultSet(ResultSet result, int i) throws SQLException
	{
		TermImpl term = new TermImpl();
		term.initId(sqlService().readLong(result, i++));
		term.initName(sqlService().readString(result, i++));
		term.initAbbreviation(sqlService().readString(result, i++));

		return term;
	}

	@Override
	public CronFrequency cronGetFrequency()
	{
		// if we are the server processing adhoc files, we will provide a schedule, else we don't do cron
		if (this.processRosterFiles) return CronFrequency.runTime;

		return CronFrequency.never;
	}

	@Override
	public RunTime[] cronGetRunTimes()
	{
		return this.processRosterFilesSchedule;
	}

	@Override
	public void cronRun()
	{
		if (!this.processRosterFiles) return;

		M_log.info("cronRun: processing files");
		doProcessFiles();
	}

	@Override
	public void emailUserAddedToSite(Site site, User user, Role role, String newUserPassword, User actor)
	{
		StringBuilder textMessage = new StringBuilder();
		// StringBuilder htmlMessage = new StringBuilder();

		String subject = (newUserPassword == null) ? ("Added to Etudes site " + site.getName()) : "New Etudes User Account";

		textMessage.append("Dear " + user.getNameDisplay() + ":\n\n" + actor.getNameDisplay() + " has added you to the Etudes site " + site.getName()
				+ ".\n\nTo access this site, log on at: " + configService().getString("serviceUrl") + "\n\n");

		if (newUserPassword != null)
		{
			textMessage.append("Your Etudes user id is: " + user.getEid() + "\n\n");
			textMessage.append("Your temporary password is: " + newUserPassword + "\n\n");
			textMessage.append("Upon logging on, you will be asked to establish a new, \"strong\" password.\n\n");
			textMessage.append("Once you change your password, please enter your first and last name using the Account link up top.  "
					+ "Click on the \"Edit\" link in the \"Name\" section of the Account screen.  Enter your name and press \"Save\".\n\n");
		}
		else
		{
			textMessage.append("Log in using your user id \"" + user.getEid() + "\" and the password you have set for this account.\n\n");
			textMessage.append("If you don't remember your password, use \"Reset Password\" on the left of Etudes.\n\n");
		}

		textMessage.append("----------------------\n\nThis is an automatic notification.  Do not reply to this email.");

		List<User> toUsers = new ArrayList<User>();
		toUsers.add(user);

		emailService().send(textMessage.toString(), textMessage.toString(), subject, toUsers);
	}

	@Override
	public Membership getActiveSiteMembers(Site site)
	{
		MembershipImpl rv = new MembershipImpl(false);
		List<Member> members = membershipTx(site);
		rv.initMembers(members);

		return rv;
	}

	@Override
	public List<Term> getActiveTerms()
	{
		List<Term> rv = readActiveTermsTx();
		return rv;
	}

	@Override
	public Roster getAdhocRoster(Site site)
	{
		String name = adhocRosterName(site);
		return this.getRoster(name, site.getClient(), site.getTerm());
	}

	@Override
	public Membership getAggregateSiteRoster(Site site)
	{
		MembershipImpl rv = new MembershipImpl(false);
		List<Member> members = membershipFullTx(site);
		rv.initMembers(members);

		return rv;
	}

	@Override
	public Membership getAggregateSiteRosterForRole(Site site, Role role, Boolean activeOnly)
	{
		MembershipImpl rv = new MembershipImpl(false);
		List<Member> members = membershipFullTx(site);
		List<Member> roleMembers = new ArrayList<Member>();
		for (Member m : members)
		{
			if (m.getRole().equals(role) && (!activeOnly || (m.isActive() && !m.isBlocked())))
			{
				roleMembers.add(m);
			}
		}

		rv.initMembers(roleMembers);

		return rv;
	}

	@Override
	public Client getClient(Long id)
	{
		Client rv = readClientTx(id);
		return rv;
	}

	@Override
	public Client getClient(String abbreviation)
	{
		Client rv = readClientAbbrevTx(abbreviation);
		return rv;
	}

	@Override
	public List<Client> getClients()
	{
		List<Client> rv = readClientsTx();
		return rv;
	}

	@Override
	public Roster getMasterRoster(Site site)
	{
		String name = masterRosterName(site);
		return this.getRoster(name, site.getClient(), site.getTerm());
	};

	@Override
	public Roster getRoster(Long id)
	{
		Roster rv = rosterTx(id);
		return rv;
	}

	@Override
	public Roster getRoster(String name, Client client, Term term)
	{
		Roster rv = rosterByNameTx(name, client, term);
		return rv;
	};

	@Override
	public List<RunTime> getRosterFileProcessingSchedule()
	{
		List<RunTime> times = new ArrayList<RunTime>();

		if (this.processRosterFiles)
		{
			for (RunTime t : this.processRosterFilesSchedule)
			{
				times.add(t);
			}
		}

		return times;
	}

	@Override
	public List<File> getRosterFiles()
	{
		List<File> rv = new ArrayList<File>();
		if (this.processRosterFilesFileSystemRoot == null) return rv;

		// get the directory
		File dir = new File(this.processRosterFilesFileSystemRoot);
		if ((dir == null) || (!dir.isDirectory()))
		{
			M_log.warn("getRosterFiles: configured directory missing or not directory: " + this.processRosterFilesFileSystemRoot);
			return rv;
		}

		// get the file list
		File files[] = dir.listFiles();
		if (files == null)
		{
			// nothing to do
			return rv;
		}

		// process each file found
		for (File file : files)
		{
			// skip "." files
			if (file.getName().startsWith(".")) continue;

			rv.add(file);
		}

		return rv;
	}

	@Override
	public Membership getRosterMembership(Roster roster)
	{
		MembershipImpl rv = new MembershipImpl(true);
		List<Member> members = rosterMembershipTx(roster);
		rv.initMembers(members);

		return rv;
	}

	@Override
	public List<Roster> getRosters(Client client, Term term)
	{
		List<Roster> rv = rosterByClientTermTx(client, term);
		return rv;
	}

	@Override
	public Term getTerm(Long id)
	{
		Term rv = readTermTx(id);
		return rv;
	}

	@Override
	public Term getTerm(String abbreviation)
	{
		Term rv = readTermAbbrevTx(abbreviation);
		return rv;
	}

	@Override
	public List<Term> getTerms()
	{
		List<Term> rv = readTermsTx();
		return rv;
	}

	@Override
	public void processFiles()
	{
		// run in the background
		cronService().runJob(new Runnable()
		{
			@Override
			public void run()
			{
				doProcessFiles();
			}
		});
	}

	@Override
	public void processRosterText(final String text, final Client client, final Term term, final Boolean createInstructors, final Boolean addToUG,
			final Boolean createSitesWithoutRosters)
	{
		if (text == null) return;

		// run in the background
		cronService().runJob(new Runnable()
		{
			@Override
			public void run()
			{
				BufferedReader br = null;

				try
				{
					br = new BufferedReader(new StringReader(text));
					processRosterBuffer(br, client, term, createInstructors, addToUG, createSitesWithoutRosters, ADHOC_SOURCE);
				}
				finally
				{
					if (br != null)
					{
						try
						{
							br.close();
						}
						catch (IOException e)
						{
							M_log.warn("syncWithRosterText(): on close of reader " + e);
						}
					}
				}
			}
		});
	}

	@Override
	public void purge(Site site)
	{
		clearSiteRosters(site, true);
	}

	@Override
	public void remove(final User user)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeUsersTx(user);
			}
		}, "remove");
	}

	@Override
	public void removeMapping(final Site site, final Roster roster)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeMappingTx(site, roster);

				// check for any site's blocked users no longer in the site
				removeOrphanedBlockedTx(site);
			}
		}, "removeMapping");
	}

	@Override
	public void removeMappings(final Roster roster)
	{
		final List<Site> rosterSites = sitesForRoster(roster);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeMappingsTx(roster);

				for (Site site : rosterSites)
				{
					// check for any site's blocked users no longer in the site
					removeOrphanedBlockedTx(site);
				}
			}
		}, "removeMappings");
	}

	@Override
	public void removeMappings(final Site site)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeMappingsTx(site);

				// check for any site's blocked users no longer in the site
				removeOrphanedBlockedTx(site);
			}
		}, "removeMappings");
	}

	@Override
	public void removeRoster(final Roster roster)
	{
		final List<Site> rosterSites = sitesForRoster(roster);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeRosterTx(roster);

				for (Site site : rosterSites)
				{
					// check for any site's blocked users no longer in the site
					removeOrphanedBlockedTx(site);
				}
			}
		}, "removeRoster");
	}

	@Override
	public List<Roster> rostersForSite(Site site)
	{
		List<Roster> rv = rostersForSiteTx(site);
		return rv;
	}

	@Override
	public List<Site> sitesForRoster(Roster roster)
	{
		List<Site> rv = sitesForRosterTx(roster);
		return rv;
	}

	@Override
	public List<SiteMember> sitesForUser(User user)
	{
		return sitesFullTx(user);
	}

	@Override
	public String sqlJoinItemBySiteFragment(String joinSiteFrom)
	{
		return "JOIN SITE S ON "
				+ joinSiteFrom
				+ " = S.ID AND (S.PUBLISHED = 1 OR ((S.PUBLISH_ON IS NOT NULL OR S.UNPUBLISH_ON IS NOT NULL) AND (S.PUBLISH_ON IS NULL OR UNIX_TIMESTAMP(NOW())*1000 >S.PUBLISH_ON) AND (UNPUBLISH_ON IS NULL OR UNIX_TIMESTAMP(NOW())*1000 < S.UNPUBLISH_ON)))"
				+ " JOIN ROSTER_SITE RS ON S.ID = RS.SITE_ID"
				+ " JOIN ROSTER_MEMBER RM ON RS.ROSTER_ID = RM.ROSTER_ID AND RM.USER_ID=? AND RM.ACTIVE = '1'"
				+ " LEFT OUTER JOIN ROSTER_BLOCKED B ON S.ID=B.SITE_ID AND RM.USER_ID=B.USER_ID AND B.USER_ID IS NULL";
	}

	@Override
	public String sqlSelectClientFragment()
	{
		return "C.ID, C.NAME, C.ABBREVIATION, C.IID_CODE";
	}

	@Override
	public Integer sqlSelectClientFragmentNumFields()
	{
		return Integer.valueOf(4);
	}

	@Override
	public String sqlSelectTermFragment()
	{
		return "T.ID, T.NAME, T.ABBREVIATION";
	}

	@Override
	public Integer sqlSelectTermFragmentNumFields()
	{
		return Integer.valueOf(3);
	}

	@Override
	public boolean start()
	{
		M_log.info("RosterServiceImpl: start");
		return true;
	}

	@Override
	public void unblock(final Site site, final User user)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeBlockedTx(site, user);
			}
		}, "updateRoster");
	}

	@Override
	public void updateRoster(final Roster roster)
	{
		final List<Site> rosterSites = sitesForRoster(roster);

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				// don't force the load if it's not already loaded
				MembershipImpl membership = (MembershipImpl) ((RosterImpl) roster).membership;
				if ((membership == null) || (!membership.mutable) || (!membership.changed)) return;

				// what memberships to remove - entries in orig not in modified
				for (User u : membership.origMemberMap.keySet())
				{
					if (membership.memberMap.get(u) == null)
					{
						removeMemberTx(roster, u);
					}
				}

				// what memberships to add - entries in modified not in orig
				for (User u : membership.memberMap.keySet())
				{
					if (membership.origMemberMap.get(u) == null)
					{
						Member m = membership.memberMap.get(u);
						insertMemberTx(roster, m.getUser(), m.getRole(), m.isActive());
					}
				}

				// what memberships to update
				for (User u : membership.memberMap.keySet())
				{
					Member orig = membership.origMemberMap.get(u);
					Member current = membership.memberMap.get(u);
					if ((orig != null) && (current != null))
					{
						if ((!orig.getRole().equals(current.getRole())) || (!orig.isActive().equals(current.isActive())))
						{
							if (((MemberImpl) current).memberId == null)
							{
								M_log.warn("updateRoster: no memberId");
							}

							// update
							updateMemberTx(((MemberImpl) current).memberId, current.getRole(), current.isActive());
						}
					}
				}

				for (Site site : rosterSites)
				{
					// check for any site's blocked users no longer in the site
					removeOrphanedBlockedTx(site);
				}
			}
		}, "updateRoster");

		// update the roster - let it re-read membership
		((RosterImpl) roster).membership = null;
	}

	@Override
	public Member userMemberInSite(User user, Site site)
	{
		return membershipFullTx(site, user);
	}

	@Override
	public Role userRoleInSite(User user, Site site)
	{
		// the admin has Role.admin in every site
		if (user.isAdmin()) return Role.admin;

		return roleTx(user.getId(), site.getId());
	}

	/**
	 * Block a user from a site.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 */
	protected void addBlockedTx(Site site, User user)
	{
		String sql = "INSERT INTO ROSTER_BLOCKED (SITE_ID, USER_ID) VALUES (?,?)";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = user.getId();

		sqlService().insert(sql, fields, "ID");
	}

	/**
	 * Transaction code for inserting a roster - site mapping.
	 * 
	 * @param site
	 *        The site.
	 * @param roster
	 *        The roster.
	 */
	protected void addMappingTx(Site site, Roster roster)
	{
		String sql = "INSERT INTO ROSTER_SITE (ROSTER_ID, SITE_ID) VALUES (?,?)";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = roster.getId();
		fields[i++] = site.getId();

		sqlService().insert(sql, fields, "ID");
	}

	/**
	 * Compute the name of a site's adhoc roster.
	 * 
	 * @param site
	 *        The site.
	 * @return The adhoc roster name.
	 */
	protected String adhocRosterName(Site site)
	{
		return adhocRosterPrefix() + site.getId().toString();
	}

	/**
	 * @return The prefix of a site's adhoc roster.
	 */
	protected String adhocRosterPrefix()
	{
		return "Adhoc";
	}

	/**
	 * Make sure a site with this name exists, creating it if needed, and that this roster exists, creating it if needed, and that these are mapped.
	 * 
	 * @param siteName
	 *        The exact site name.
	 * @param rosterName
	 *        The roster name.
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @param createRoster
	 *        if true, we create the roster (if needed) as well as the site.
	 * @return The site.
	 */
	protected Site assureSiteAndRoster(String siteName, String rosterName, Client client, Term term, boolean createRoster, RosterCache rosters)
	{
		Site site = siteService().get(siteName);
		if (site == null)
		{
			site = createSite(siteName, client, term);
		}

		// if we are directed to not create a roster, we are done
		if (!createRoster) return site;

		// find the roster
		Roster roster = rosters.get(rosterName, client, term);
		if (roster == null)
		{
			roster = addRoster(rosterName, Boolean.TRUE, client, term);
		}

		// make sure the site and roster are mapped
		if (!checkMapping(site, roster))
		{
			addMapping(site, roster);
		}

		return site;
	}

	/**
	 * Transaction code for checking if a user is blocked in a site.
	 * 
	 * @param Site
	 *        the site.
	 * @param User
	 *        the user.
	 * @return TRUE if the user is blocked, FALSE if not.
	 */
	protected Boolean checkBlockedTx(Site site, User user)
	{
		String sql = "SELECT ID FROM ROSTER_BLOCKED WHERE SITE_ID = ? AND USER_ID = ?";
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = user.getId();
		List<Long> rv = sqlService().select(sql, fields, new SqlService.Reader<Long>()
		{
			@Override
			public Long read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					return id;
				}
				catch (SQLException e)
				{
					M_log.warn("checkBlockedTx: " + e);
					return null;
				}
			}
		});

		return rv.size() == 1 ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Transaction code for checking a site - roster mapping.
	 */
	protected Boolean checkMapingTx(Site site, Roster roster)
	{
		String sql = "SELECT ID FROM ROSTER_SITE WHERE ROSTER_ID = ? AND SITE_ID = ?";
		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = roster.getId();
		fields[i++] = site.getId();
		List<Long> rv = sqlService().select(sql, fields, new SqlService.Reader<Long>()
		{
			@Override
			public Long read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long id = sqlService().readLong(result, i++);
					return id;
				}
				catch (SQLException e)
				{
					M_log.warn("checkMapingTx: " + e);
					return null;
				}
			}
		});

		return rv.size() == 1 ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Clear the site's rosters.
	 * 
	 * @param site
	 *        The site.
	 * @param complete
	 *        if true, remove them all, otherwise leave the instructors in the master roster.
	 */
	protected void clearSiteRosters(Site site, boolean complete)
	{
		// the rosters used by the site includes adhoc and master)
		List<Roster> rosters = rostersForSite(site);

		// remove the site from being mapped to all of its rosters (leaves the adhoc and master mapped)
		removeMappings(site);

		// of those rosters, remove any that no longer are being used by any sites.
		// TODO: should we?
		for (Roster roster : rosters)
		{
			List<Site> sites = sitesForRoster(roster);
			if (sites.isEmpty())
			{
				removeRoster(roster);
			}
		}

		// remove the site's adhoc roster
		Roster adhoc = getAdhocRoster(site);
		if (adhoc != null) removeRoster(adhoc);

		// remove the site's master roster if requested
		if (complete)
		{
			Roster master = getMasterRoster(site);
			if (master != null) removeRoster(master);
		}
		else
		{
			// TODO: remove non-instructor users from the master roster
		}
	}

	/**
	 * Create a site from default settings.
	 * 
	 * @param name
	 *        The site name.
	 * @param client
	 *        The client.
	 * @param term
	 *        The term.
	 * @return The site.
	 */
	protected Site createDefaultSite(String name, Client client, Term term)
	{
		Site site = siteService().add(name, client, term, userService().get(UserService.ADMIN));

		site.setPublished(Boolean.TRUE);
		site.setSkin(siteService().getSkin(client));

		// tools
		Set<Tool> tools = new HashSet<Tool>();
		tools.add(Tool.home);
		tools.add(Tool.coursemap);
		tools.add(Tool.schedule);
		tools.add(Tool.announcement);
		tools.add(Tool.syllabus);
		tools.add(Tool.module);
		tools.add(Tool.assessment);
		tools.add(Tool.forum);
		tools.add(Tool.blog);
		tools.add(Tool.chat);
		tools.add(Tool.resource);
		tools.add(Tool.evaluation);
		tools.add(Tool.activity);
		tools.add(Tool.member);
		tools.add(Tool.message);

		site.setTools(tools);

		siteService().save(userService().get(UserService.ADMIN), site);

		return site;
	}

	/**
	 * Create a user with the data in the roster line.
	 * 
	 * @param rl
	 *        The roster line.
	 * @param institutionCode
	 *        The institution code.
	 * @return The user created, or null if we were unable to create a user.
	 */
	protected User createUser(RosterLine rl)
	{
		User rv = userService().add(userService().get(UserService.ADMIN));

		rv.setEid(rl.eid);
		rv.setNameFirst(rl.firstName);
		rv.setNameLast(rl.lastName);
		rv.setEmailOfficial(rl.email);
		rv.setPassword(rl.pw);
		List<Iid> iids = new ArrayList<Iid>();
		iids.add(userService().makeIid(rl.iid, rl.client.getIidCode()));
		rv.setIids(iids);

		// a roster user
		rv.setRosterUser(Boolean.TRUE);

		userService().save(userService().get(UserService.ADMIN), rv);

		return rv;
	}

	/**
	 * Process the roster files.
	 */
	protected void doProcessFiles()
	{
		// get the files ready to process
		List<File> files = getRosterFiles();

		// process each file found
		for (File file : files)
		{
			BufferedReader br = null;
			try
			{
				br = new BufferedReader(new FileReader(file));

				// no default client or term - all official roster files must have lines that identify sites that use standard title structure (term abbreviation ... client abbreviation)
				// TODO: need a client set if the file is the client's "official" student roster file
				processRosterBuffer(br, null, null, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, file.getName());
			}
			catch (FileNotFoundException e)
			{
				M_log.warn("processFiles(): " + e);
			}
			finally
			{
				if (br != null)
				{
					try
					{
						br.close();
					}
					catch (IOException e)
					{
						M_log.warn("processFiles(): on close of reader " + e);
					}
				}
			}

			// delete the file
			file.delete();
		}
	}

	/**
	 * Email the user that we just changed their EID.
	 * 
	 * @param rl
	 *        The roster line.
	 * @param user
	 *        The user.
	 */
	protected void emailEidChange(RosterLine rl, String oldEid, User user)
	{
		StringBuilder textMessage = new StringBuilder();
		// StringBuilder htmlMessage = new StringBuilder();

		String subject = "New Etudes User Id";

		textMessage.append("Dear " + user.getNameDisplay() + ":\n\n"
				+ "Your college has changed your Etudes login. This change is effective immediately.\n\n" + "Old user id: " + oldEid + "\n"
				+ "New user id: " + rl.eid + "\n\n" + "The password remains whatever you had before.\n\n"
				+ "If this was done in error, please contact Admissions & Records or your Distance Learning office.\n\n----------------------\n\n"
				+ "This is an automatic notification.  Do not reply to this email.");

		List<User> toUsers = new ArrayList<User>();
		toUsers.add(user);

		emailService().send(textMessage.toString(), textMessage.toString(), subject, toUsers);
	}

	/**
	 * Report eid changes from roster processing.
	 * 
	 * @param client
	 *        The client (site prefix).
	 * @param term
	 *        The term (site suffix).
	 * @param sites
	 *        The sites with no students loaded by roster.
	 */
	protected void emailNoRoster(Client client, Term term, List<Site> sites)
	{
		StringBuilder textMessage = new StringBuilder();
		// StringBuilder htmlMessage = new StringBuilder();

		String subject = "Roster Warnings: Sites Without Students: " + client.getName() + " - " + term.getName();

		textMessage.append("Roster processing has noticed these sites which have no roster loaded students, for client: " + client + " in term: "
				+ term + ".\n\n");

		for (Site s : sites)
		{
			textMessage.append(s.getName() + "\n");
		}

		List<User> toUsers = new ArrayList<User>();
		toUsers.add(userService().get(UserService.ADMIN));

		emailService().send(textMessage.toString(), textMessage.toString(), subject, toUsers);
	}

	/**
	 * Report bad lines, missing sections, and EID changes in roster processing.
	 * 
	 * @param badLines
	 *        The roster lines.
	 * @param missingSections
	 *        The missing sections.
	 * @param eidChagnes
	 *        The EID changes.
	 * @param source
	 *        describes the source of the text line (file name or pasted).
	 */
	protected void emailRosterReport(List<String> badLines, Set<String> missingSections, List<String> eidChanges, String source)
	{
		StringBuilder textMessage = new StringBuilder();
		// StringBuilder htmlMessage = new StringBuilder();

		String subject = "Roster Issues: " + source;

		// missing sections
		if (!missingSections.isEmpty())
		{
			textMessage.append("Etudes has discovered sections in your student file which do not exist in any of your sites (" + source + "):\n\n");

			for (String section : missingSections)
			{
				textMessage.append(section + "\n");
			}

			textMessage.append("\n\n");
		}

		// EID changes
		if (!eidChanges.isEmpty())
		{
			textMessage
					.append("Roster processing has changed user EIDs due to new data found in " + source + ".  These users have been emailed.\n\n");

			for (String line : eidChanges)
			{
				textMessage.append(line + "\n");
			}

			textMessage.append("\n\n");
		}

		// bad lines
		if (!badLines.isEmpty())
		{
			textMessage.append("Roster processing has rejected invalid roster lines from " + source + ":\n\n");

			for (String line : badLines)
			{
				textMessage.append(line + "\n");
			}
		}

		List<User> toUsers = new ArrayList<User>();
		toUsers.add(userService().get(UserService.ADMIN));

		emailService().send(textMessage.toString(), textMessage.toString(), subject, toUsers);
	}

	/**
	 * Transaction code for inserting a roster member.
	 * 
	 * @param roster
	 *        The roster.
	 */
	protected void insertMemberTx(Roster roster, User user, Role role, Boolean active)
	{
		String sql = "INSERT INTO ROSTER_MEMBER (ROSTER_ID, USER_ID, ROLE, ACTIVE) VALUES (?,?,?,?)";

		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = roster.getId();
		fields[i++] = user.getId();
		fields[i++] = role.getLevel();
		fields[i++] = active;

		sqlService().insert(sql, fields, "ID");
	}

	/**
	 * Transaction code for inserting a roster.
	 * 
	 * @param roster
	 *        The roster.
	 */
	protected void insertRosterTx(RosterImpl roster)
	{
		String sql = "INSERT INTO ROSTER (NAME, CLIENT_ID, TERM_ID, OFFICIAL) VALUES (?,?,?,?)";

		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = roster.getName();
		fields[i++] = roster.getClient().getId();
		fields[i++] = roster.getTerm().getId();
		fields[i++] = roster.isOfficial();

		Long id = sqlService().insert(sql, fields, "ID");
		roster.initId(id);
	}

	/**
	 * Compute the name of a site's master roster.
	 * 
	 * @param site
	 *        The site.
	 * @return The master roster name.
	 */
	protected String masterRosterName(Site site)
	{
		return masterRosterPrefix() + site.getId().toString();
	}

	/**
	 * @return The master roster name prefix.
	 */
	protected String masterRosterPrefix()
	{
		return "Master";
	}

	/**
	 * Transaction code for reading users in a site, from all the site's rosters - and setting membership's roster (info) attribution for each (i.e. which roster the user is in the site from).
	 * 
	 * @param siteId
	 *        The site id.
	 * @return The list of Member.
	 */
	protected List<Member> membershipFullTx(Site site)
	{
		String sql = "SELECT RM.ID, RM.ROLE, RM.ACTIVE, R.ID, R.NAME, R.OFFICIAL, B.USER_ID, " + sqlSelectClientFragment() + ", "
				+ sqlSelectTermFragment() + ", " + userService().sqlSelectFragment()
				+ " FROM ROSTER_SITE RS JOIN ROSTER_MEMBER RM ON RS.ROSTER_ID = RM.ROSTER_ID JOIN ROSTER R ON RS.ROSTER_ID = R.ID"
				+ " LEFT OUTER JOIN ROSTER_BLOCKED B ON RS.SITE_ID=B.SITE_ID AND RM.USER_ID=B.USER_ID"
				+ " LEFT OUTER JOIN CLIENT C ON R.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON R.TERM_ID = T.ID "
				+ userService().sqlJoinFragment("RM.USER_ID") + " WHERE RS.SITE_ID = ? GROUP BY " + userService().sqlGroupFragment()
				+ " ORDER BY RM.USER_ID ASC, RM.ROLE ASC";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<Member> full = sqlService().select(sql, fields, new SqlService.Reader<Member>()
		{
			@Override
			public Member read(ResultSet result)
			{
				try
				{
					int i = 1;

					Long memberId = sqlService().readLong(result, i++);
					Role role = Role.valueOf(sqlService().readInteger(result, i++));
					Boolean active = sqlService().readBoolean(result, i++);

					Long rosterId = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					Boolean official = sqlService().readBoolean(result, i++);
					Long blockedUserId = sqlService().readLong(result, i++);

					Client client = createClientFromResultSet(result, i);
					i += sqlSelectClientFragmentNumFields();

					Term term = createTermFromResultSet(result, i);
					i += sqlSelectTermFragmentNumFields();

					User user = userService().createFromResultSet(result, i);
					i += userService().sqlSelectFragmentNumFields();

					Roster roster = new RosterImpl(rosterId, name, official, client, term);

					MemberImpl m = new MemberImpl(user, role, active, Boolean.valueOf(blockedUserId != null), roster, memberId);
					return m;
				}
				catch (SQLException e)
				{
					M_log.warn("membershipFullTx: " + e);
					return null;
				}
			}
		});

		// the membership may contain a user multiple times - ordered by role lowest to highest - keep just the highest
		Map<User, Member> oneMembershipPerUser = new HashMap<User, Member>();
		for (Member m : full)
		{
			// adding them in list order assures the membership with the largest role value (sorted later) is the one that sticks
			oneMembershipPerUser.put(m.getUser(), m);
		}

		List<Member> rv = new ArrayList<Member>();
		rv.addAll(oneMembershipPerUser.values());

		// sort by user display name
		Collections.sort(rv, new Member.NameSortComparator());

		return rv;
	}

	/**
	 * Transaction code for reading users in a site, from all the site's rosters - and setting membership's roster (info) attribution for each (i.e. which roster the user is in the site from).
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 * @return The list of Member.
	 */
	protected Member membershipFullTx(Site site, User user)
	{
		String sql = "SELECT RM.ID, RM.ROLE, RM.ACTIVE, R.ID, R.NAME, R.OFFICIAL, B.USER_ID, " + sqlSelectClientFragment() + ", "
				+ sqlSelectTermFragment() + ", " + userService().sqlSelectFragment()
				+ " FROM ROSTER_SITE RS JOIN ROSTER_MEMBER RM ON RS.ROSTER_ID = RM.ROSTER_ID JOIN ROSTER R ON RS.ROSTER_ID = R.ID"
				+ " LEFT OUTER JOIN ROSTER_BLOCKED B ON RS.SITE_ID=B.SITE_ID AND RM.USER_ID=B.USER_ID"
				+ " LEFT OUTER JOIN CLIENT C ON R.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON R.TERM_ID = T.ID "
				+ userService().sqlJoinFragment("RM.USER_ID") + " WHERE RS.SITE_ID = ? AND RM.USER_ID = ? GROUP BY "
				+ userService().sqlGroupFragment() + " ORDER BY RM.USER_ID ASC, RM.ROLE ASC"; // TODO: where to put the user id check
		Object[] fields = new Object[2];
		fields[0] = site.getId();
		fields[1] = user.getId();
		List<Member> full = sqlService().select(sql, fields, new SqlService.Reader<Member>()
		{
			@Override
			public Member read(ResultSet result)
			{
				try
				{
					int i = 1;

					Long memberId = sqlService().readLong(result, i++);
					Role role = Role.valueOf(sqlService().readInteger(result, i++));
					Boolean active = sqlService().readBoolean(result, i++);

					Long rosterId = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					Boolean official = sqlService().readBoolean(result, i++);
					Long blockedUserId = sqlService().readLong(result, i++);

					Client client = createClientFromResultSet(result, i);
					i += sqlSelectClientFragmentNumFields();

					Term term = createTermFromResultSet(result, i);
					i += sqlSelectTermFragmentNumFields();

					User user = userService().createFromResultSet(result, i);
					i += userService().sqlSelectFragmentNumFields();

					Roster roster = new RosterImpl(rosterId, name, official, client, term);

					MemberImpl m = new MemberImpl(user, role, active, Boolean.valueOf(blockedUserId != null), roster, memberId);
					return m;
				}
				catch (SQLException e)
				{
					M_log.warn("membershipFullTx: " + e);
					return null;
				}
			}
		});

		// the membership may contain a user multiple times - ordered by role lowest to highest - keep just the highest
		Member rv = null;
		for (Member m : full)
		{
			// adding them in list order assures the membership with the largest role value (sorted later) is the one that sticks
			rv = m;
		}

		return rv;
	}

	/**
	 * Transaction code for reading active users in a site, from all the site's rosters.
	 * 
	 * @param siteId
	 *        The site id.
	 * @return The List of Member.
	 */
	protected List<Member> membershipTx(Site site)
	{
		String sql = "SELECT RM.USER_ID, MAX(RM.ROLE) FROM ROSTER_SITE RS JOIN ROSTER_MEMBER RM ON RS.ROSTER_ID = RM.ROSTER_ID LEFT OUTER JOIN ROSTER_BLOCKED B ON RS.SITE_ID=B.SITE_ID AND RM.USER_ID=B.USER_ID"
				+ " WHERE RS.SITE_ID = ? AND RM.ACTIVE = '1' AND B.USER_ID IS NULL GROUP BY RM.USER_ID";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<Member> rv = sqlService().select(sql, fields, new SqlService.Reader<Member>()
		{
			@Override
			public Member read(ResultSet result)
			{
				try
				{
					int i = 1;

					User user = userService().wrap(sqlService().readLong(result, i++));
					Role role = Role.valueOf(sqlService().readInteger(result, i++));

					MemberImpl m = new MemberImpl(user, role, Boolean.TRUE);
					return m;
				}
				catch (SQLException e)
				{
					M_log.warn("membershipTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Parse and validate syntax for a roster lime.
	 * 
	 * @param line
	 *        The roster line.
	 * @param client
	 *        The client for these lines - used only for lines that the site name does not identify a client.
	 * @param term
	 *        The term for these lines - used only for lines that the site name does not identify a term.
	 * @return The parsed line, or null if there is an error.
	 */
	protected RosterLine parseRosterLine(String line, Client client, Term term)
	{
		// tabs are the separator, parse the line.
		String[] parts = split(line, "\t");

		// we need 10 parts
		if (parts.length != 10) return null;

		RosterLine rv = new RosterLine();
		rv.line = line;

		// site title (collapsing multiple consecutive white space to a space)
		rv.siteName = parts[0].trim().replaceAll("\\s+", " ");
		if (rv.siteName.length() == 0) return null;

		// eid
		rv.eid = parts[1].trim();
		if (rv.eid.length() == 0) return null;

		// pw
		rv.pw = parts[2].trim();
		if (rv.pw.length() == 0) return null;

		// last name
		rv.lastName = parts[3].trim();
		if (rv.lastName.length() == 0) return null;

		// first name
		rv.firstName = parts[4].trim();
		if (rv.firstName.length() == 0) return null;

		// email
		rv.email = trimToNull(parts[5]);

		// role (and a user type, in case we need to add the user)
		String role = parts[6].trim();
		if (role.equalsIgnoreCase(TYPE_STUDENT))
		{
			rv.role = Role.student;
			rv.userType = TYPE_STUDENT;
		}
		else if (role.equalsIgnoreCase(TYPE_INSTRUCTOR))
		{
			rv.role = Role.instructor;
			rv.userType = TYPE_INSTRUCTOR;
		}
		else if (role.equalsIgnoreCase(TYPE_TA))
		{
			rv.role = Role.ta;
			rv.userType = TYPE_INSTRUCTOR;
		}
		// else if (role.equalsIgnoreCase(TYPE_EVALUATOR))
		// {
		// rv.role = Role.evaluator;
		// rv.userType = TYPE_INSTRUCTOR;
		// }
		else
			return null;

		// status
		String status = parts[7].trim();
		if (status.equalsIgnoreCase("E"))
			rv.active = Boolean.TRUE;
		else if (status.equalsIgnoreCase("D"))
			rv.active = Boolean.FALSE;
		else
			return null;

		// section
		rv.rosterName = parts[8].trim();
		if (rv.rosterName.length() == 0) return null;

		// iid
		rv.iid = parts[9].trim();
		if (rv.iid.length() == 0) return null;

		// set the client and term based on the (possibly partial) site name: first part is client code, last part is term code
		String[] nameParts = split(rv.siteName, " ");
		String clientAbbreviation = nameParts[0];
		String termAbbreviation = nameParts[nameParts.length - 1];
		rv.client = getClient(clientAbbreviation);
		rv.term = getTerm(termAbbreviation);

		if (rv.client == null) rv.client = client;
		if (rv.term == null) rv.term = term;

		return rv;
	}

	/**
	 * Process the roster lines from a pasted text or a roster file in the reader.
	 * 
	 * @param br
	 *        The reader containing the roster text.
	 * @param client
	 *        The client for these lines - used only for lines that the site name does not identify a client.
	 * @param term
	 *        The term for these lines - used only for lines that the site name does not identify a term.
	 * @param createInstructors
	 *        if TRUE, allow new instructors to be created.
	 * @param addToUG
	 *        if TRUE, add new instructors to the Users Group.
	 * @param createSitesWithoutRosters
	 *        if TRUE, allow sites to be created that have no rosters.
	 * @param source
	 *        either the file name or "pasted text" indicating the source of the roster lines.
	 */
	protected void processRosterBuffer(BufferedReader br, Client client, Term term, Boolean createInstructors, Boolean addToUG,
			Boolean createSitesWithoutRosters, String source)
	{
		// rosters we touched
		RosterCache rosters = new RosterCache();

		// lines that were invalid
		List<String> badLines = new ArrayList<String>();
		Set<String> missingRosters = new HashSet<String>();
		List<String> eidChanges = new ArrayList<String>();

		// some special directives
		boolean checkForDirectives = true;
		boolean createSitesDirective = false;
		boolean ignoreDropsDirective = false;
		boolean createInstructorsDirective = createInstructors == null ? false : createInstructors.booleanValue();
		boolean noRostersDirective = createSitesWithoutRosters == null ? false : createSitesWithoutRosters.booleanValue();
		boolean addToUsersGroupDirective = addToUG == null ? false : addToUG.booleanValue();

		// accept only student role lines from files that have a known client association - which indicate the file is registered as a daily client roster file.
		boolean onlyStudentLines = ((!source.equals(ADHOC_SOURCE)) && (client != null));

		RosterLine sampleLine = null;

		// get the UG roster
		Roster usersGroupRoster = this.getRoster(RosterService.USERSGROUP);

		// process each line - don't let any line's errors interfere with subsequent or past lines
		while (true)
		{
			try
			{
				// read lines till done
				String line = br.readLine();
				if (line == null) break;

				line = line.trim();

				// special directive recognition
				if (checkForDirectives)
				{
					if (source.equals(ADHOC_SOURCE))
					{
						// recognize special directive
						if (line.equals("#@CREATESITES@"))
						{
							createSitesDirective = true;
						}
						else if (line.equals("#@IGNOREDROPS@"))
						{
							ignoreDropsDirective = true;
						}
						else if (line.equals("#@CREATEINSTRUCTORS@"))
						{
							createInstructorsDirective = true;
						}
						else if (line.equals("#@NOROSTERS@"))
						{
							noRostersDirective = true;
						}
						else if (line.equals("#@ADDTOUG@"))
						{
							addToUsersGroupDirective = true;
						}
					}
				}

				// skip blank links
				if (line.length() == 0) continue;

				// skip comment lines
				if (line.startsWith("#")) continue;

				// all directives must be before any non-comment line
				checkForDirectives = false;

				// parse the line
				RosterLine rl = parseRosterLine(line, client, term);
				if (rl == null)
				{
					badLines.add("# parse error\n" + line);
					continue;
				}

				if (sampleLine == null) sampleLine = rl;

				// reject "D" lines for non-student roles
				if ((rl.role != Role.student) && (rl.active == Boolean.FALSE))
				{
					badLines.add("# non-student drop\n" + line);
					continue;
				}

				// reject an instructor role line if we are not allowing them
				if (onlyStudentLines && (rl.role != Role.student))
				{
					badLines.add("# only student role allowed\n" + line);
					continue;
				}

				// for the special create site directive
				if (createSitesDirective)
				{
					rl.role = Role.instructor;
					rl.eid = "admin";
					rl.iid = "admin";
				}

				// for the special ignore drops directive
				if (ignoreDropsDirective)
				{
					if (rl.active == Boolean.FALSE) continue;
				}

				// reject line if we don't have an institution code or client
				if (rl.client == null)
				{
					badLines.add("# unknown client\n" + line);
					continue;
				}
				if (rl.term == null)
				{
					badLines.add("# unknown term\n" + line);
					continue;
				}

				// find the user - identified by the line's iid
				User user = userService().findByIid(userService().makeIid(rl.iid, rl.client.getIidCode()));

				// process instructor lines
				if (rl.role == Role.instructor)
				{
					// the user must already exist, AND be a member of the user's group
					boolean inUg = usersGroupRoster.getMembership().findUser(user) != null;
					if ((user == null) || (!inUg))
					{
						// create the instructor user, unless we are directed not to
						if (!createInstructorsDirective)
						{
							if (user == null)
							{
								badLines.add("# instructor not found\n" + line);
							}
							else
							{
								badLines.add("# instructor not in Users Group\n" + line);
							}
							continue;
						}
						else
						{
							// assure the user exists
							if (user == null)
							{
								user = createUser(rl);
								if (user == null)
								{
									badLines.add("# could not create user\n" + line);
									continue;
								}
							}

							// assure the user is in the UG
							if ((!inUg) && addToUsersGroupDirective)
							{
								usersGroupRoster.getMembership().add(user, Role.student, true);
							}
						}
					}

					// create the site and roster if needed
					Site site = assureSiteAndRoster(rl.siteName, rl.rosterName, rl.client, rl.term, !noRostersDirective, rosters);

					// assure the user has the role in the site's master roster
					String name = masterRosterName(site);
					Roster master = rosters.get(name, site.getClient(), site.getTerm());
					if (master != null)
					{
						master.getMembership().assure(user, rl.role, Boolean.TRUE);
					}
					else
					{
						M_log.warn("processRosterBuffer: missing master roster for site: " + site.getId());
					}
				}

				// TA and Evaluator roles are applied in the site's master roster
				else if (/* (rl.role == Role.evaluator) || */(rl.role == Role.ta)) // TODO: evaluator
				{
					// site must exist - identified by site, not roster
					Site site = siteService().get(rl.siteName);
					if (site == null)
					{
						badLines.add("# site not found\n" + line);
						continue;
					}

					// create the user if needed
					if (user == null)
					{
						user = createUser(rl);
						if (user == null)
						{
							badLines.add("# could not create user\n" + line);
							continue;
						}
					}

					// assure the user has the role in the site's master roster
					String name = masterRosterName(site);
					Roster master = rosters.get(name, site.getClient(), site.getTerm());
					if (master != null)
					{
						master.getMembership().assure(user, rl.role, Boolean.TRUE);
					}
					else
					{
						M_log.warn("processRosterBuffer: missing master roster for site: " + site.getId());
					}
				}

				// for student lines, make sure the user is properly registered in the roster indicated by the line
				else
				{
					// get the roster - it must exist
					Roster roster = rosters.get(rl.rosterName, rl.client, rl.term);
					if (roster == null)
					{
						badLines.add("# missing roster\n" + line);
						missingRosters.add(rl.rosterName);
						continue;
					}

					// create the user if needed
					if (user == null)
					{
						user = createUser(rl);

						if (user == null)
						{
							badLines.add("# could not create user\n" + line);
							continue;
						}
					}

					// if we found a user, update it if the roster data has updates (unless the user has multiple IIDs, in which case we leave the user data alone)
					else
					{
						// check for multiple IIDs for the user
						List<Iid> iids = user.getIids();
						boolean multipleIids = (iids.size() > 1);
						if (!multipleIids)
						{
							if (!updateUser(user, rl, eidChanges))
							{
								badLines.add("# user updates could not be applied\n" + line);
								continue;
							}
						}
					}

					// assure the user's proper relationship to the roster (role, active)
					roster.getMembership().assure(user, rl.role, rl.active);
				}
			}
			catch (IOException e)
			{
				M_log.warn("processRosterBuffer(): " + e);
			}
		}

		// save the rosters
		for (Roster r : rosters.rosters.values())
		{
			updateRoster(r);
		}

		// save the users group
		updateRoster(usersGroupRoster);

		// report any issues
		if ((!badLines.isEmpty()) || (!missingRosters.isEmpty()) || (!eidChanges.isEmpty()))
		{
			emailRosterReport(badLines, missingRosters, eidChanges, source);
		}

		// check for sites with no roster-loaded students (only if we just processed an official student roster file)
		if (onlyStudentLines && (sampleLine != null))
		{
			List<Site> clientTermSites = siteService().find(sampleLine.client, sampleLine.term);
			List<Site> clientTermSitesNoRosterStudents = new ArrayList<Site>();
			for (Site cts : clientTermSites)
			{
				List<Roster> ctsRosters = rostersForSite(cts);
				boolean hasStudents = false;
				for (Roster r : ctsRosters)
				{
					List<Member> students = getRosterMembership(r).findRole(Role.student);
					if (!students.isEmpty())
					{
						hasStudents = true;
						break;
					}
				}

				if (!hasStudents) clientTermSitesNoRosterStudents.add(cts);
			}

			if (!clientTermSitesNoRosterStudents.isEmpty())
			{
				emailNoRoster(sampleLine.client, sampleLine.term, clientTermSitesNoRosterStudents);
			}
		}
	}

	/**
	 * Transaction code for reading all terms that currently have sites or rosters.
	 */
	protected List<Term> readActiveTermsTx()
	{
		String sql = "SELECT T.ID, T.NAME, T.ABBREVIATION FROM TERM T WHERE T.ID IN (SELECT DISTINCT TERM_ID FROM SITE UNION SELECT DISTINCT TERM_ID FROM ROSTER) ORDER BY T.ID DESC";
		List<Term> rv = sqlService().select(sql, null, new SqlService.Reader<Term>()
		{
			@Override
			public Term read(ResultSet result)
			{
				TermImpl term = new TermImpl();
				try
				{
					int i = 1;
					term.initId(sqlService().readLong(result, i++));
					term.initName(sqlService().readString(result, i++));
					term.initAbbreviation(sqlService().readString(result, i++));

					return term;
				}
				catch (SQLException e)
				{
					M_log.warn("readTermTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a client from the abbreviation.
	 * 
	 * @param abbreviation
	 *        The client abbreviation.
	 */
	protected Client readClientAbbrevTx(String abbreviation)
	{
		String sql = "SELECT ID, NAME, ABBREVIATION, IID_CODE FROM CLIENT WHERE ABBREVIATION = ?";
		Object[] fields = new Object[1];
		fields[0] = abbreviation;
		List<Client> rv = sqlService().select(sql, fields, new SqlService.Reader<Client>()
		{
			@Override
			public Client read(ResultSet result)
			{
				ClientImpl client = new ClientImpl();
				try
				{
					int i = 1;

					client.initId(sqlService().readLong(result, i++));
					client.initName(sqlService().readString(result, i++));
					client.initAbbreviation(sqlService().readString(result, i++));
					client.initIidCode(sqlService().readString(result, i++));

					return client;
				}
				catch (SQLException e)
				{
					M_log.warn("readClientAbbrevTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading all clients.
	 */
	protected List<Client> readClientsTx()
	{
		String sql = "SELECT ID, NAME, ABBREVIATION, IID_CODE FROM CLIENT ORDER BY NAME ASC";
		List<Client> rv = sqlService().select(sql, null, new SqlService.Reader<Client>()
		{
			@Override
			public Client read(ResultSet result)
			{
				ClientImpl client = new ClientImpl();
				try
				{
					int i = 1;
					client.initId(sqlService().readLong(result, i++));
					client.initName(sqlService().readString(result, i++));
					client.initAbbreviation(sqlService().readString(result, i++));
					client.initIidCode(sqlService().readString(result, i++));

					return client;
				}
				catch (SQLException e)
				{
					M_log.warn("readClientsTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a client.
	 * 
	 * @param id
	 *        The client id.
	 */
	protected Client readClientTx(final Long id)
	{
		String sql = "SELECT NAME, ABBREVIATION, IID_CODE FROM CLIENT WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<Client> rv = sqlService().select(sql, fields, new SqlService.Reader<Client>()
		{
			@Override
			public Client read(ResultSet result)
			{
				ClientImpl client = new ClientImpl();
				client.initId(id);
				try
				{
					int i = 1;

					client.initName(sqlService().readString(result, i++));
					client.initAbbreviation(sqlService().readString(result, i++));
					client.initIidCode(sqlService().readString(result, i++));

					return client;
				}
				catch (SQLException e)
				{
					M_log.warn("readClientTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading a term.
	 * 
	 * @param abbreviation
	 *        The term abbreviation.
	 */
	protected Term readTermAbbrevTx(String abbreviation)
	{
		String sql = "SELECT ID, NAME, ABBREVIATION FROM TERM WHERE ABBREVIATION = ?";
		Object[] fields = new Object[1];
		fields[0] = abbreviation;
		List<Term> rv = sqlService().select(sql, fields, new SqlService.Reader<Term>()
		{
			@Override
			public Term read(ResultSet result)
			{
				TermImpl term = new TermImpl();
				try
				{
					int i = 1;

					term.initId(sqlService().readLong(result, i++));
					term.initName(sqlService().readString(result, i++));
					term.initAbbreviation(sqlService().readString(result, i++));

					return term;
				}
				catch (SQLException e)
				{
					M_log.warn("readTermAbbrevTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading all terms.
	 */
	protected List<Term> readTermsTx()
	{
		String sql = "SELECT ID, NAME, ABBREVIATION FROM TERM ORDER BY ID DESC";
		List<Term> rv = sqlService().select(sql, null, new SqlService.Reader<Term>()
		{
			@Override
			public Term read(ResultSet result)
			{
				TermImpl term = new TermImpl();
				try
				{
					int i = 1;
					term.initId(sqlService().readLong(result, i++));
					term.initName(sqlService().readString(result, i++));
					term.initAbbreviation(sqlService().readString(result, i++));

					return term;
				}
				catch (SQLException e)
				{
					M_log.warn("readTermTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a term.
	 * 
	 * @param id
	 *        The term id.
	 */
	protected Term readTermTx(final Long id)
	{
		String sql = "SELECT NAME, ABBREVIATION FROM TERM WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<Term> rv = sqlService().select(sql, fields, new SqlService.Reader<Term>()
		{
			@Override
			public Term read(ResultSet result)
			{
				TermImpl term = new TermImpl();
				term.initId(id);
				try
				{
					int i = 1;

					term.initName(sqlService().readString(result, i++));
					term.initAbbreviation(sqlService().readString(result, i++));

					return term;
				}
				catch (SQLException e)
				{
					M_log.warn("readTermTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for removing a user's block in a site.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 */
	protected void removeBlockedTx(Site site, User user)
	{
		String sql = "DELETE FROM ROSTER_BLOCKED WHERE SITE_ID = ? AND USER_ID = ?";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = user.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing this roster from all sites.
	 * 
	 * @param roster
	 *        The roster.
	 */
	protected void removeMappingsTx(Roster roster)
	{
		String sql = "DELETE FROM ROSTER_SITE WHERE ROSTER_ID = ?";

		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = roster.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing all "official" mappings from a site - except for adhoc and master.
	 * 
	 * @param site
	 *        The site.
	 */
	protected void removeMappingsTx(Site site)
	{
		String sql = "DELETE ROSTER_SITE FROM ROSTER_SITE JOIN ROSTER ON ROSTER_SITE.ROSTER_ID = ROSTER.ID WHERE ROSTER_SITE.SITE_ID = ? AND ROSTER.NAME != ? AND ROSTER.NAME != ?";

		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = site.getId();
		fields[i++] = adhocRosterName(site);
		fields[i++] = masterRosterName(site);

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a roster - site mapping.
	 * 
	 * @param site
	 *        The site.
	 * @param roster
	 *        The roster.
	 */
	protected void removeMappingTx(Site site, Roster roster)
	{
		String sql = "DELETE FROM ROSTER_SITE WHERE ROSTER_ID = ? AND SITE_ID =?";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = roster.getId();
		fields[i++] = site.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a roster member.
	 * 
	 * @param roster
	 *        The roster.
	 */
	protected void removeMemberTx(Roster roster, User user)
	{
		String sql = "DELETE FROM ROSTER_MEMBER WHERE ROSTER_ID = ? AND USER_ID = ?";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = roster.getId();
		fields[i++] = user.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code to remove any blocked member in this site who no longer has any membership in the site from any site roster.
	 * 
	 * @param site
	 *        The site.
	 */
	protected void removeOrphanedBlockedTx(Site site)
	{
		String sql = "DELETE FROM ROSTER_BLOCKED WHERE USER_ID NOT IN (SELECT RM.USER_ID FROM ROSTER_SITE RS JOIN ROSTER_MEMBER RM ON RS.ROSTER_ID = RM.ROSTER_ID WHERE RS.SITE_ID = ?)";
		Object[] fields = new Object[1];
		fields[0] = site.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a roster.
	 * 
	 * @param roster
	 *        The roster.
	 */
	protected void removeRosterTx(Roster roster)
	{
		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = roster.getId();

		String sql = "DELETE FROM ROSTER_SITE WHERE ROSTER_ID = ?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM ROSTER_MEMBER WHERE ROSTER_ID = ?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM ROSTER WHERE ID = ?";
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code to remove a user from all rosters.
	 * 
	 * @param user
	 *        The user.
	 */
	protected void removeUsersTx(User user)
	{
		String sql = "DELETE FROM ROSTER_MEMBER WHERE USER_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		sqlService().update(sql, fields);

		// TODO: may need key on user_id
		sql = "DELETE FROM ROSTER_BLOCKED WHERE USER_ID = ?";
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for checking a user's role in a site.
	 * 
	 * @param userId
	 *        The user id.
	 * @param siteId
	 *        The site id.
	 * @return The user's best role, or Role.none if the user has no role in the site.
	 */
	protected Role roleTx(final Long userId, final Long siteId)
	{
		// user may have multiple roles in the site, due to the user being in multiple of the site's rosters - we will return the one with the highest level
		String sql = "SELECT MAX(RM.ROLE) FROM ROSTER_MEMBER RM JOIN ROSTER_SITE RS ON RM.ROSTER_ID = RS.ROSTER_ID"
				+ " LEFT OUTER JOIN ROSTER_BLOCKED B ON RS.SITE_ID = B.SITE_ID AND RM.USER_ID = B.USER_ID"
				+ " WHERE RM.USER_ID = ? AND RS.SITE_ID = ? AND RM.ACTIVE = '1' AND B.USER_ID IS NULL";
		Object[] fields = new Object[2];
		fields[0] = userId;
		fields[1] = siteId;
		List<Role> rv = sqlService().select(sql, fields, new SqlService.Reader<Role>()
		{
			@Override
			public Role read(ResultSet result)
			{
				try
				{
					int i = 1;

					return Role.valueOf(sqlService().readInteger(result, i++));
				}
				catch (SQLException e)
				{
					M_log.warn("roleTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? Role.none : rv.get(0);
	}

	protected List<Roster> rosterByClientTermTx(final Client client, final Term term)
	{
		String sql = "SELECT R.ID, R.NAME, R.OFFICIAL FROM ROSTER R WHERE R.CLIENT_ID = ? AND R.TERM_ID = ? ORDER BY R.NAME ASC";
		Object[] fields = new Object[2];
		fields[0] = client.getId();
		fields[1] = term.getId();

		List<Roster> rv = sqlService().select(sql, fields, new SqlService.Reader<Roster>()
		{
			@Override
			public Roster read(ResultSet result)
			{
				try
				{
					int i = 1;

					Long rosterId = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					Boolean official = sqlService().readBoolean(result, i++);

					Roster roster = new RosterImpl(rosterId, name, official, client, term);
					// Note: membership not read

					return roster;
				}
				catch (SQLException e)
				{
					M_log.warn("rosteByClientTermTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	protected Roster rosterByNameTx(String name, final Client client, final Term term)
	{
		String sql = "SELECT R.ID, R.NAME, R.OFFICIAL FROM ROSTER R WHERE R.NAME= ? AND R.CLIENT_ID = ? AND R.TERM_ID = ?";
		Object[] fields = new Object[3];
		fields[0] = name;
		fields[1] = client.getId();
		fields[2] = term.getId();

		List<Roster> rv = sqlService().select(sql, fields, new SqlService.Reader<Roster>()
		{
			@Override
			public Roster read(ResultSet result)
			{
				try
				{
					int i = 1;

					Long rosterId = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					Boolean official = sqlService().readBoolean(result, i++);

					Roster roster = new RosterImpl(rosterId, name, official, client, term);
					// Note: membership not read

					return roster;
				}
				catch (SQLException e)
				{
					M_log.warn("rosterByNameTx: " + e);
					return null;
				}
			}
		});

		return rv.size() == 1 ? rv.get(0) : null;
	}

	/**
	 * Transaction code for reading a roster's membership.
	 * 
	 * @param roster
	 *        For this roster.
	 * @return the List of Member.
	 */
	protected List<Member> rosterMembershipTx(final Roster roster)
	{
		String sql = "SELECT RM.ID, RM.ROLE, RM.ACTIVE, " + userService().sqlSelectFragment() + " FROM ROSTER_MEMBER RM "
				+ userService().sqlJoinFragment("RM.USER_ID") + " WHERE RM.ROSTER_ID = ? GROUP BY " + userService().sqlGroupFragment();

		Object[] fields = new Object[1];
		fields[0] = roster.getId();
		List<Member> rv = sqlService().select(sql, fields, new SqlService.Reader<Member>()
		{
			@Override
			public Member read(ResultSet result)
			{
				try
				{
					int i = 1;

					Long id = sqlService().readLong(result, i++);
					Role role = Role.valueOf(sqlService().readInteger(result, i++));
					Boolean active = sqlService().readBoolean(result, i++);

					User user = userService().createFromResultSet(result, i);
					i += userService().sqlSelectFragmentNumFields();

					MemberImpl m = new MemberImpl(user, role, active, null, roster, id);
					return m;
				}
				catch (SQLException e)
				{
					M_log.warn("rosterMembershipTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	protected List<Roster> rostersForSiteTx(Site site)
	{
		String sql = "SELECT R.ID, R.NAME, R.OFFICIAL, " + sqlSelectClientFragment() + ", " + sqlSelectTermFragment()
				+ " FROM ROSTER_SITE RS JOIN ROSTER R ON RS.ROSTER_ID = R.ID"
				+ " LEFT OUTER JOIN CLIENT C ON R.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON R.TERM_ID = T.ID"
				+ " WHERE RS.SITE_ID = ? ORDER BY R.NAME ASC";
		Object[] fields = new Object[1];
		fields[0] = site.getId();

		List<Roster> rv = sqlService().select(sql, fields, new SqlService.Reader<Roster>()
		{
			@Override
			public Roster read(ResultSet result)
			{
				try
				{
					int i = 1;

					Long rosterId = sqlService().readLong(result, i++);
					String name = sqlService().readString(result, i++);
					Boolean official = sqlService().readBoolean(result, i++);

					Client client = createClientFromResultSet(result, i);
					i += sqlSelectClientFragmentNumFields();

					Term term = createTermFromResultSet(result, i);
					i += sqlSelectTermFragmentNumFields();

					Roster roster = new RosterImpl(rosterId, name, official, client, term);
					// Note: membership not read

					return roster;
				}
				catch (SQLException e)
				{
					M_log.warn("rostersForSiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	protected Roster rosterTx(final Long id)
	{
		String sql = "SELECT R.NAME, R.OFFICIAL, " + sqlSelectClientFragment() + ", " + sqlSelectTermFragment() + " FROM ROSTER R"
				+ " LEFT OUTER JOIN CLIENT C ON R.CLIENT_ID = C.ID LEFT OUTER JOIN TERM T ON R.TERM_ID = T.ID WHERE R.ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;

		List<Roster> rv = sqlService().select(sql, fields, new SqlService.Reader<Roster>()
		{
			@Override
			public Roster read(ResultSet result)
			{
				try
				{
					int i = 1;

					String name = sqlService().readString(result, i++);
					Boolean official = sqlService().readBoolean(result, i++);

					Client client = createClientFromResultSet(result, i);
					i += sqlSelectClientFragmentNumFields();

					Term term = createTermFromResultSet(result, i);
					i += sqlSelectTermFragmentNumFields();

					Roster roster = new RosterImpl(id, name, official, client, term);
					// Note: membership not read

					return roster;
				}
				catch (SQLException e)
				{
					M_log.warn("rosterTx: " + e);
					return null;
				}
			}
		});

		return rv.size() == 1 ? rv.get(0) : null;
	}

	protected List<Site> sitesForRosterTx(Roster roster)
	{
		String sql = "SELECT RS.SITE_ID FROM ROSTER_SITE RS LEFT OUTER JOIN SITE S ON RS.SITE_ID = S.ID WHERE RS.ROSTER_ID = ? ORDER BY S.NAME ASC";
		Object[] fields = new Object[1];
		fields[0] = roster.getId();

		List<Site> rv = sqlService().select(sql, fields, new SqlService.Reader<Site>()
		{
			@Override
			public Site read(ResultSet result)
			{
				try
				{
					int i = 1;

					Site site = siteService().wrap(sqlService().readLong(result, i++));
					return site;
				}
				catch (SQLException e)
				{
					M_log.warn("sitesFoRosterTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for finding the sites that this user has access to. Ordered by term (DESC) then site name (ASC). Reads the full site info (tools and links)
	 * 
	 * @param user
	 *        The user.
	 * @return The SiteMember list for the user.
	 */
	protected List<SiteMember> sitesFullTx(final User user)
	{
		final Map<Long, Site> sites = new HashMap<Long, Site>();

		String sql = "SELECT MAX(RM.ROLE), "
				+ siteService().sqlSelectFragment()
				+ " FROM ROSTER_MEMBER RM JOIN ROSTER_SITE RS ON RM.ROSTER_ID = RS.ROSTER_ID LEFT OUTER JOIN ROSTER_BLOCKED B ON RS.SITE_ID=B.SITE_ID AND RM.USER_ID=B.USER_ID "
				+ siteService().sqlJoinFragment("RS.SITE_ID")
				+ " WHERE RM.USER_ID = ? AND RM.ACTIVE = '1' AND B.USER_ID IS NULL GROUP BY S.ID ORDER BY T.ID DESC, S.NAME ASC";

		Object[] fields = new Object[1];
		fields[0] = user.getId();
		List<SiteMember> rv = sqlService().select(sql, fields, new SqlService.Reader<SiteMember>()
		{
			@Override
			public SiteMember read(ResultSet result)
			{
				try
				{
					int i = 1;
					Role role = Role.valueOf(sqlService().readInteger(result, i++));
					Site site = siteService().createFromResultSet(result, i);
					i += siteService().sqlSelectFragmentNumFields();

					SiteMemberImpl membership = new SiteMemberImpl(user, role, site);

					// stash for later filling in tools and links
					sites.put(site.getId(), site);

					// pre-init so if we have no tools or links for a site, it's marked as loaded
					((SiteImpl) site).initLink(null);

					return membership;
				}
				catch (SQLException e)
				{
					M_log.warn("sitesFullTx(member): " + e);
					return null;
				}
			}
		});

		// get all the links for these sites
		sql = "SELECT L.SITE_ID, L.ID, L.TITLE, L.URL, L.POSITION "
				+ " FROM ROSTER_MEMBER RM JOIN ROSTER_SITE RS ON RM.ROSTER_ID = RS.ROSTER_ID LEFT OUTER JOIN ROSTER_BLOCKED B ON RS.SITE_ID=B.SITE_ID AND RM.USER_ID=B.USER_ID "
				+ " JOIN SITE_LINK L ON RS.SITE_ID = L.SITE_ID"
				+ " WHERE RM.USER_ID = ? AND RM.ACTIVE = '1' AND B.USER_ID IS NULL ORDER BY L.POSITION ASC";
		sqlService().select(sql, fields, new SqlService.Reader<Long>()
		{
			@Override
			public Long read(ResultSet result)
			{
				try
				{
					int i = 1;
					Long siteId = sqlService().readLong(result, i++);

					Site s = sites.get(siteId);
					if (s != null)
					{
						Long id = sqlService().readLong(result, i++);
						String title = sqlService().readString(result, i++);
						String url = sqlService().readString(result, i++);
						Integer position = sqlService().readInteger(result, i++);
						Link link = s.wrap(id, title, url, position);

						((SiteImpl) s).initLink(link);
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("sitesFullTx(links): " + e);
					return null;
				}
			}
		});

		return rv;
	}

	protected void updateMemberTx(Long id, Role role, Boolean active)
	{
		String sql = "UPDATE ROSTER_MEMBER SET ROLE = ?, ACTIVE = ? WHERE ID = ?";

		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = role.getLevel();
		fields[i++] = active;
		fields[i++] = id;

		sqlService().update(sql, fields);
	}

	/**
	 * If a user record's EID, lastName or firstName has changed in the roster line, update the user record, and report to the user via email if the EID has changed.
	 * 
	 * @param user
	 *        The user record.
	 * @param rl
	 *        The roster line.
	 * @return true if all went well, false if there was a problem.
	 */
	protected boolean updateUser(User user, RosterLine rl, List<String> eidChanges)
	{
		// EID change
		if (differentIgnoreCase(user.getEid(), rl.eid))
		{
			String oldEid = user.getEid();
			String eidReport = "EID change for " + user.getNameSort() + " EID: " + oldEid + " IID: " + user.getIidDisplay() + "    to: "
					+ rl.lastName + ", " + rl.firstName + " EID: " + rl.eid + "   email: " + user.getEmailOfficial() + ", " + rl.email;

			user.setEid(rl.eid);
			user.setNameLast(rl.lastName);
			user.setNameFirst(rl.firstName);
			user.setEmailOfficial(rl.email);

			userService().save(userService().get(UserService.ADMIN), user);

			// email and report
			emailEidChange(rl, oldEid, user);
			eidChanges.add(eidReport);

			return true;
		}

		// just name, email change - not reported
		if (differentIgnoreCase(user.getNameLast(), rl.lastName) || differentIgnoreCase(user.getNameFirst(), rl.firstName)
				|| differentIgnoreCase(user.getEmailOfficial(), rl.email))
		{
			user.setNameLast(rl.lastName);
			user.setNameFirst(rl.firstName);
			user.setEmailOfficial(rl.email);

			userService().save(userService().get(UserService.ADMIN), user);

			return true;
		}

		return true;
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
	}

	/**
	 * @return The registered CronService.
	 */
	private CronService cronService()
	{
		return (CronService) Services.get(CronService.class);
	}

	/**
	 * @return The registered EmailService.
	 */
	private EmailService emailService()
	{
		return (EmailService) Services.get(EmailService.class);
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
