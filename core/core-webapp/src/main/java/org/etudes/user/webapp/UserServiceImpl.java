/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/user/webapp/UserServiceImpl.java $
 * $Id: UserServiceImpl.java 12054 2015-11-10 22:01:23Z ggolden $
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

package org.etudes.user.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.email.api.EmailService;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.sql.api.SqlService;
import org.etudes.threadlocal.api.ThreadLocalService;
import org.etudes.user.api.Iid;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * TestServiceImpl implements TestService.
 */
public class UserServiceImpl implements UserService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(UserServiceImpl.class);

	/**
	 * Construct
	 */
	public UserServiceImpl()
	{
		M_log.info("UserServiceImpl: construct");
	}

	@Override
	public User add(User addedBy)
	{
		UserImpl rv = new UserImpl();

		rv.setLoaded();
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());

		save(addedBy, rv);

		return rv;
	}

	@Override
	public User check(Long id)
	{
		UserImpl rv = getCached(id);
		if (rv == null)
		{
			rv = checkUserTx(id);
		}

		return rv;
	}

	@Override
	public Integer count(String search)
	{
		Integer rv = countUserTx(search);
		return rv;
	}

	@Override
	public User createFromResultSet(ResultSet result, int i) throws SQLException
	{
		return createFromResultSet(result, i, null);
	}

	@Override
	public List<User> find(String search, Sort sort, Integer pageNum, Integer pageSize)
	{
		List<User> rv = findUserPageTx(search, sort, pageNum, pageSize);
		return rv;
	}

	@Override
	public List<User> findByEid(String eid)
	{
		List<User> rv = findUserByEidTx(eid);
		return rv;
	}

	@Override
	public List<User> findByEmail(String email)
	{
		List<User> rv = findUserByEmailTx(email);
		return rv;
	}

	@Override
	public User findByIid(Iid iid)
	{
		User rv = readIidUserTx(iid);
		return rv;
	}

	@Override
	public User get(Long id)
	{
		UserImpl rv = getCached(id);
		if (rv == null)
		{
			rv = new UserImpl();
			rv.initId(id);
			readUserTx(rv);
		}

		return rv;
	}

	@Override
	public List<Iid> getUserIid(User user)
	{
		List<Iid> rv = readIidTx(user);
		return rv;
	}

	@Override
	public Boolean isProtected(User user)
	{
		if (ADMIN.equals(user.getId())) return Boolean.TRUE;
		if (HELPDESK.equals(user.getId())) return Boolean.TRUE;
		if (SYSTEM.equals(user.getId())) return Boolean.TRUE;

		return Boolean.FALSE;
	}

	@Override
	public Iid makeIid(String display)
	{
		return new IidImpl(display);
	}

	@Override
	public Iid makeIid(String iid, String code)
	{
		return new IidImpl(iid, code);
	}

	@Override
	public void refresh(User user)
	{
		UserImpl cached = getCached(user.getId());
		if (cached != null)
		{
			((UserImpl) user).init(cached);
		}

		else
		{
			readUserTx((UserImpl) user);
		}
	}

	@Override
	public void remove(final User user)
	{
		// skip remove of protected users
		if (isProtected(user)) return;

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeIidTx(user);
				removeUserTx(user);
			}
		}, "removeUser");

		// deal with the avatar
		Reference avatar = user.getAvatar();
		if (avatar != null)
		{
			fileService().remove(avatar);
		}
	}

	@Override
	public Boolean resetPassword(String email)
	{
		// find the user with this email
		List<User> found = findUserByEmailTx(email);

		if (found.size() != 1) return Boolean.FALSE;

		// generate a new password
		Random generator = new Random(System.currentTimeMillis());
		Integer num = new Integer(generator.nextInt(Integer.MAX_VALUE));
		if (num.intValue() < 0) num = new Integer(num.intValue() * -1);
		String pw = num.toString();

		User user = found.get(0);
		user.setPassword(pw);
		save(user, user);

		String subject = "New Etudes User Id";
		String textMessage = "Dear " + user.getNameDisplay() + ":\n\n" + "As you requested, your password has been reset. \n\n"
				+ "Your Etudes user id is: " + user.getEid() + "\n\n" + "Your temporary password is: " + pw + "\n\n"
				+ "Upon logging on, you will be asked to establish a new, \"strong\" password.\n\n"
				+ "Once you change your password, you will be able to access your class(es).\n\n" + "----------------------\n\n"
				+ "This is an automatic notification.  Do not reply to this email.";

		List<User> toUsers = new ArrayList<User>();
		toUsers.add(user);

		emailService().send(textMessage.toString(), textMessage.toString(), subject, toUsers);

		return Boolean.TRUE;
	}

	@Override
	public void save(User savedBy, final User user)
	{
		if (((UserImpl) user).isChanged() || user.getId() == null)
		{
			if (((UserImpl) user).isChanged())
			{
				// set modified by/on
				((UserImpl) user).initModifiedBy(savedBy);
				((UserImpl) user).initModifiedOn(new Date());

				// deal with the signature and avatar
				((UserImpl) user).saveSignatureAndAvatar(savedBy);
			}

			// insert or update
			if (user.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						insertUserTx((UserImpl) user);
						updateIids((UserImpl) user);
					}
				}, "insertUser");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						updateUserTx((UserImpl) user);
						updateIids((UserImpl) user);
					}
				}, "updateUser");
			}

			((UserImpl) user).clearChanged();
		}
	}

	@Override
	public String sqlGroupFragment()
	{
		return "USER_U.ID";
	}

	@Override
	public String sqlJoinFragment(String on)
	{
		return "LEFT OUTER JOIN USER USER_U ON " + on + " = USER_U.ID LEFT OUTER JOIN USER_IID USER_I ON USER_U.ID = USER_I.USER_ID";
	}

	@Override
	public String sqlSelectFragment()
	{
		return "USER_U.ID, USER_U.AVATAR, USER_U.AIM, USER_U.FACEBOOK, USER_U.GOOGLEPLUS, USER_U.LINKEDIN, USER_U.SKYPE, USER_U.TWITTER, USER_U.WEB, USER_U.CREATED_BY, USER_U.CREATED_ON,"
				+ " USER_U.EID, USER_U.EMAIL_EXPOSED, USER_U.EMAIL_OFFICIAL, USER_U.EMAIL_USER, USER_U.PASSWORD, USER_U.NAME_FIRST, USER_U.NAME_LAST, USER_U.MODIFIED_BY, USER_U.MODIFIED_ON,"
				+ " USER_U.INTERESTS, USER_U.LOCATION, USER_U.OCCUPATION, USER_U.ROSTER, USER_U.SIGNATURE, USER_U.TIMEZONE, GROUP_CONCAT(USER_I.IID, '@', USER_I.CODE)";
	}

	@Override
	public Integer sqlSelectFragmentNumFields()
	{
		return Integer.valueOf(27);
	}

	@Override
	public boolean start()
	{
		M_log.info("UserServiceImpl: start");
		return true;
	}

	@Override
	public Boolean strongPassword(String pw)
	{
		String trim = pw.trim();
		if (trim.length() < 8) return Boolean.FALSE;
		if (!Pattern.matches(".*?[A-Z].*?", trim)) return Boolean.FALSE;
		if (!Pattern.matches(".*?[a-z].*?", trim)) return Boolean.FALSE;
		if (!Pattern.matches(".*?[0-9].*?", trim)) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	@Override
	public void willNeed(Set<User> users)
	{
		if (users.isEmpty()) return;

		// filter out any loaded users, collect the remaining ids
		Set<Long> needed = new HashSet<Long>();
		for (User u : users)
		{
			if (!((UserImpl) u).isLoaded())
			{
				needed.add(u.getId());
			}
		}

		// read (and cache) the users
		if (!needed.isEmpty()) readUsersTx(needed);
	}

	@Override
	public User wrap(Long id)
	{
		if (id == null) return null;

		UserImpl rv = getCached(id);
		if (rv == null)
		{
			rv = new UserImpl();
			rv.initId(id);
		}

		return rv;
	}

	/**
	 * Cache this user in the thread cache.
	 * 
	 * @param user
	 *        The user to cache.
	 */
	protected void cache(UserImpl user)
	{
		if ((user != null) && (user.isLoaded()))
		{
			@SuppressWarnings("unchecked")
			Map<Long, UserImpl> usersMap = (Map<Long, UserImpl>) threadLocalService().get("USERS");
			if (usersMap == null)
			{
				usersMap = new HashMap<Long, UserImpl>();
				threadLocalService().put("USERS", usersMap);
			}

			// cache a copy
			UserImpl copy = new UserImpl();
			copy.init(user);
			usersMap.put(user.getId(), user);
		}
	}

	/**
	 * Transaction code for checking a user.
	 * 
	 * @param id
	 *        The user id.
	 * @return a wrapped user object if found, null if not.
	 */
	protected UserImpl checkUserTx(final Long id)
	{
		String sql = "SELECT ID FROM USER WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<UserImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<UserImpl>()
		{
			@Override
			public UserImpl read(ResultSet result)
			{
				UserImpl user = new UserImpl();
				user.initId(id);

				return user;
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for counting users based on criteria.
	 * 
	 * @param search
	 *        The search criteria.
	 * @return The number of users found.
	 */
	protected Integer countUserTx(String search)
	{
		String sql = "SELECT COUNT(1) FROM USER";
		int numFields = 0;

		if (search != null)
		{
			sql += " WHERE CONCAT(NAME_LAST, ', ', NAME_FIRST) LIKE ?";
			numFields++;
		}

		int i = 0;
		Object[] fields = new Object[numFields];
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

	protected User createFromResultSet(ResultSet result, int i, UserImpl user) throws SQLException
	{
		if (user == null) user = new UserImpl();

		user.initId(sqlService().readLong(result, i++));
		user.initAvatar(sqlService().readLong(result, i++));
		user.initConnectAim(sqlService().readString(result, i++));
		user.initConnectFacebook(sqlService().readString(result, i++));
		user.initConnectGooglePlus(sqlService().readString(result, i++));
		user.initConnectLinkedIn(sqlService().readString(result, i++));
		user.initConnectSkype(sqlService().readString(result, i++));
		user.initConnectTwitter(sqlService().readString(result, i++));
		user.initConnectWeb(sqlService().readString(result, i++));
		user.initCreatedBy(wrap(sqlService().readLong(result, i++)));
		user.initCreatedOn(sqlService().readDate(result, i++));
		user.initEid(sqlService().readString(result, i++));
		user.initEmailExposed(sqlService().readBoolean(result, i++));
		user.initEmailOfficial(sqlService().readString(result, i++));
		user.initEmailUser(sqlService().readString(result, i++));
		user.initPassword(sqlService().readString(result, i++));
		user.initNameFirst(sqlService().readString(result, i++));
		user.initNameLast(sqlService().readString(result, i++));
		user.initModifiedBy(wrap(sqlService().readLong(result, i++)));
		user.initModifiedOn(sqlService().readDate(result, i++));
		user.initProfileInterests(sqlService().readString(result, i++));
		user.initProfileLocation(sqlService().readString(result, i++));
		user.initProfileOccupation(sqlService().readString(result, i++));
		user.initRosterUser(sqlService().readBoolean(result, i++));
		user.initSignature(sqlService().readLong(result, i++));
		user.initTimeZone(sqlService().readString(result, i++));

		String iids = sqlService().readString(result, i++);
		user.initIids(iids);

		user.setLoaded();

		cache(user);

		return user;
	}

	/**
	 * Transaction code for finding users by eid.
	 * 
	 * @param eid
	 *        The eid.
	 * @return The list of users found.
	 */
	protected List<User> findUserByEidTx(String eid)
	{
		String sql = "SELECT " + sqlSelectFragment()
				+ " FROM USER USER_U LEFT OUTER JOIN USER_IID USER_I ON USER_U.ID = USER_I.USER_ID WHERE USER_U.EID = ? GROUP BY "
				+ sqlGroupFragment();
		Object[] fields = new Object[1];
		fields[0] = eid;
		List<User> rv = sqlService().select(sql, fields, new SqlService.Reader<User>()
		{
			@Override
			public User read(ResultSet result)
			{
				try
				{
					int i = 1;
					User user = createFromResultSet(result, i);
					return user;
				}
				catch (SQLException e)
				{
					M_log.warn("findUserByEidTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for finding users by email.
	 * 
	 * @param email
	 *        The email.
	 * @return The list of users found.
	 */
	protected List<User> findUserByEmailTx(String email)
	{
		String sql = "SELECT "
				+ sqlSelectFragment()
				+ " FROM USER USER_U LEFT OUTER JOIN USER_IID USER_I ON USER_U.ID = USER_I.USER_ID WHERE USER_U.EMAIL_OFFICIAL = ? OR USER_U.EMAIL_USER = ? GROUP BY "
				+ sqlGroupFragment();
		Object[] fields = new Object[2];
		fields[0] = email;
		fields[1] = email;
		List<User> rv = sqlService().select(sql, fields, new SqlService.Reader<User>()
		{
			@Override
			public User read(ResultSet result)
			{
				try
				{
					int i = 1;
					User user = createFromResultSet(result, i);
					return user;
				}
				catch (SQLException e)
				{
					M_log.warn("findUserByEmailTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading users based on criteria.
	 * 
	 * @param search
	 *        The search criteria.
	 * @return The list of users found.
	 */
	protected List<User> findUserPageTx(String search, Sort sort, Integer pageNum, Integer pageSize)
	{
		String sql = "SELECT " + sqlSelectFragment() + " FROM USER USER_U LEFT OUTER JOIN USER_IID USER_I ON USER_U.ID = USER_I.USER_ID";
		int numFields = 0;

		if (search != null)
		{
			sql += " WHERE CONCAT(NAME_LAST, ', ', NAME_FIRST) LIKE ?";
			numFields++;
		}

		sql += " GROUP BY " + sqlGroupFragment() + " ORDER BY NAME_LAST ASC, NAME_FIRST ASC LIMIT ?, ?";
		numFields += 2;

		int i = 0;
		Object[] fields = new Object[numFields];
		if (search != null)
		{
			fields[i++] = "%" + search + "%";
		}
		fields[i++] = Integer.valueOf((pageNum - 1) * pageSize);
		fields[i++] = Integer.valueOf(pageSize);

		List<User> rv = sqlService().select(sql, fields, new SqlService.Reader<User>()
		{
			@Override
			public User read(ResultSet result)
			{
				try
				{
					int i = 1;
					User user = createFromResultSet(result, i);
					return user;
				}
				catch (SQLException e)
				{
					M_log.warn("findUserPageTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Check the thread cache for this user.
	 * 
	 * @param id
	 *        The user ID.
	 * @return The User cached, or null if not found.
	 */
	protected UserImpl getCached(Long id)
	{
		UserImpl rv = null;

		@SuppressWarnings("unchecked")
		Map<Long, UserImpl> usersMap = (Map<Long, UserImpl>) threadLocalService().get("USERS");

		if (usersMap != null)
		{
			rv = usersMap.get(id);
		}

		return rv;
	}

	/**
	 * Transaction code for inserting a user IID.
	 * 
	 * @param user
	 *        The user.
	 * @param iid
	 *        The IID.
	 */
	protected void insertIidTx(UserImpl user, Iid iid)
	{
		String sql = "INSERT INTO USER_IID (USER_ID, IID, CODE) VALUES (?,?,?)";

		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = user.getId();
		fields[i++] = iid.getId();
		fields[i++] = iid.getCode();

		sqlService().insert(sql, fields, "ID");
	}

	/**
	 * Transaction code for inserting a user.
	 * 
	 * @param user
	 *        The user.
	 */
	protected void insertUserTx(UserImpl user)
	{
		String sql = "INSERT INTO USER (AVATAR, AIM, FACEBOOK, GOOGLEPLUS, LINKEDIN, SKYPE, TWITTER, WEB, CREATED_BY, CREATED_ON,"
				+ " EID, EMAIL_EXPOSED, EMAIL_OFFICIAL, EMAIL_USER, PASSWORD, NAME_FIRST, NAME_LAST, MODIFIED_BY, MODIFIED_ON,"
				+ " INTERESTS, LOCATION, OCCUPATION, ROSTER, SIGNATURE, TIMEZONE) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[25];
		int i = 0;
		fields[i++] = user.getAvatarReferenceId();
		fields[i++] = user.getConnectAim();
		fields[i++] = user.getConnectFacebook();
		fields[i++] = user.getConnectGooglePlus();
		fields[i++] = user.getConnectLinkedIn();
		fields[i++] = user.getConnectSkype();
		fields[i++] = user.getConnectTwitter();
		fields[i++] = user.getConnectWeb();
		fields[i++] = (user.getCreatedBy() == null ? UserService.SYSTEM : user.getCreatedBy().getId());
		fields[i++] = user.getCreatedOn();
		fields[i++] = user.getEid();
		fields[i++] = user.isEmailExposed();
		fields[i++] = user.getEmailOfficial();
		fields[i++] = user.getEmailUser();
		fields[i++] = user.getPasswordEncoded();
		fields[i++] = user.getNameFirst();
		fields[i++] = user.getNameLast();
		fields[i++] = (user.getModifiedBy() == null ? UserService.SYSTEM : user.getModifiedBy().getId());
		fields[i++] = user.getModifiedOn();
		fields[i++] = user.getProfileInterests();
		fields[i++] = user.getProfileLocation();
		fields[i++] = user.getProfileOccupation();
		fields[i++] = user.isRosterUser();
		fields[i++] = user.getSignatureReferenceId();
		fields[i++] = user.getTimeZone();

		Long id = sqlService().insert(sql, fields, "ID");
		((UserImpl) user).initId(id);
	}

	protected List<Iid> readIidTx(User user)
	{

		String sql = "SELECT IID, CODE FROM USER_IID WHERE USER_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		List<Iid> rv = sqlService().select(sql, fields, new SqlService.Reader<Iid>()
		{
			@Override
			public Iid read(ResultSet result)
			{
				try
				{
					int i = 1;
					String id = sqlService().readString(result, i++);
					String code = sqlService().readString(result, i++);
					Iid iid = new IidImpl(id, code);

					return iid;
				}
				catch (SQLException e)
				{
					M_log.warn("readIidTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	protected User readIidUserTx(Iid iid)
	{
		String sql = "SELECT " + sqlSelectFragment() + " FROM USER_IID IID " + sqlJoinFragment("IID.USER_ID")
				+ " WHERE IID.CODE = ? AND IID.IID = ? GROUP BY IID.USER_ID";
		Object[] fields = new Object[2];
		fields[0] = iid.getCode();
		fields[1] = iid.getId();

		List<User> rv = sqlService().select(sql, fields, new SqlService.Reader<User>()
		{
			@Override
			public User read(ResultSet result)
			{
				try
				{
					int i = 1;
					User user = createFromResultSet(result, i);
					return user;
				}
				catch (SQLException e)
				{
					M_log.warn("readIidUserTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading a bunch of users.
	 * 
	 * @param ids
	 *        The list of user ids to read.
	 * @return The List of users read.
	 */
	protected List<UserImpl> readUsersTx(Set<Long> ids)
	{
		String sql = "SELECT " + sqlSelectFragment()
				+ " FROM USER USER_U LEFT OUTER JOIN USER_IID USER_I ON USER_U.ID = USER_I.USER_ID WHERE USER_U.ID IN (" + sqlService().idsToCsv(ids)
				+ ") GROUP BY " + sqlGroupFragment();

		List<UserImpl> rv = sqlService().select(sql, null, new SqlService.Reader<UserImpl>()
		{
			@Override
			public UserImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					UserImpl user = (UserImpl) createFromResultSet(result, i);
					return user;
				}
				catch (SQLException e)
				{
					M_log.warn("readUsersTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a user.
	 * 
	 * @param user
	 *        The user.
	 * @return The UserImpl read, or null if the user id is not found.
	 */
	protected void readUserTx(final UserImpl user)
	{
		String sql = "SELECT " + sqlSelectFragment()
				+ " FROM USER USER_U LEFT OUTER JOIN USER_IID USER_I ON USER_U.ID = USER_I.USER_ID WHERE USER_U.ID = ? GROUP BY "
				+ sqlGroupFragment();
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		sqlService().select(sql, fields, new SqlService.Reader<UserImpl>()
		{
			@Override
			public UserImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					createFromResultSet(result, i, user);
					return user;
				}
				catch (SQLException e)
				{
					M_log.warn("readUserTx: " + e);
					return null;
				}
			}
		});
	}

	/**
	 * Transaction code for removing a user's IIDs.
	 * 
	 * @param user
	 *        The user.
	 * @param iid
	 *        The IID.
	 */
	protected void removeIidTx(User user)
	{
		String sql = "DELETE FROM USER_IID WHERE USER_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a user IID.
	 * 
	 * @param user
	 *        The user.
	 * @param iid
	 *        The IID.
	 */
	protected void removeIidTx(UserImpl user, Iid iid)
	{
		String sql = "DELETE FROM USER_IID WHERE CODE = ? AND IID = ?";
		Object[] fields = new Object[2];
		fields[0] = iid.getCode();
		fields[1] = iid.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing a user.
	 * 
	 * @param user
	 *        The user.
	 */
	protected void removeUserTx(User user)
	{
		String sql = "DELETE FROM USER WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		sqlService().update(sql, fields);
		((UserImpl) user).initId(null);
	}

	protected void updateIids(UserImpl user)
	{
		// don't force the load if it's not already loaded
		if (user.iids == null) return;

		// what iids to remove - entries in orig not in modified
		for (Iid i : user.origIids)
		{
			if (!user.iids.contains(i))
			{
				removeIidTx(user, i);
			}
		}

		// what iids to add - entries in modified not in orig
		for (Iid i : user.iids)
		{
			if (!user.origIids.contains(i))
			{
				insertIidTx(user, i);
			}
		}
	}

	/**
	 * Transaction code for updating an existing user.
	 * 
	 * @param user
	 *        The user.
	 */
	protected void updateUserTx(UserImpl user)
	{
		String sql = "UPDATE USER SET AVATAR=?, AIM=?, FACEBOOK=?, GOOGLEPLUS=?, LINKEDIN=?, SKYPE=?, TWITTER=?, WEB=?, CREATED_BY=?, CREATED_ON=?,"
				+ " EID=?, EMAIL_EXPOSED=?, EMAIL_OFFICIAL=?, EMAIL_USER=?, PASSWORD=?, NAME_FIRST=?, NAME_LAST=?, MODIFIED_BY=?, MODIFIED_ON=?,"
				+ " INTERESTS=?, LOCATION=?, OCCUPATION=?, ROSTER=?, SIGNATURE=?, TIMEZONE=? WHERE ID=?";

		Object[] fields = new Object[26];
		int i = 0;
		fields[i++] = user.getAvatarReferenceId();
		fields[i++] = user.getConnectAim();
		fields[i++] = user.getConnectFacebook();
		fields[i++] = user.getConnectGooglePlus();
		fields[i++] = user.getConnectLinkedIn();
		fields[i++] = user.getConnectSkype();
		fields[i++] = user.getConnectTwitter();
		fields[i++] = user.getConnectWeb();
		fields[i++] = (user.getCreatedBy() == null ? UserService.SYSTEM : user.getCreatedBy().getId());
		fields[i++] = user.getCreatedOn();
		fields[i++] = user.getEid();
		fields[i++] = user.isEmailExposed();
		fields[i++] = user.getEmailOfficial();
		fields[i++] = user.getEmailUser();
		fields[i++] = user.getPasswordEncoded();
		fields[i++] = user.getNameFirst();
		fields[i++] = user.getNameLast();
		fields[i++] = (user.getModifiedBy() == null ? UserService.SYSTEM : user.getModifiedBy().getId());
		fields[i++] = user.getModifiedOn();
		fields[i++] = user.getProfileInterests();
		fields[i++] = user.getProfileLocation();
		fields[i++] = user.getProfileOccupation();
		fields[i++] = user.isRosterUser();
		fields[i++] = user.getSignatureReferenceId();
		fields[i++] = user.getTimeZone();

		fields[i++] = user.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * @return The registered EmailService.
	 */
	private EmailService emailService()
	{
		return (EmailService) Services.get(EmailService.class);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}

	/**
	 * @return The registered SqlService.
	 */
	private SqlService sqlService()
	{
		return (SqlService) Services.get(SqlService.class);
	}

	/**
	 * @return The registered ThreadLocalService.
	 */
	private ThreadLocalService threadLocalService()
	{
		return (ThreadLocalService) Services.get(ThreadLocalService.class);
	}
}
