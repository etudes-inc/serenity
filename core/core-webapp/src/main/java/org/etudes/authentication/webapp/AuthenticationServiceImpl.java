/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/authentication/webapp/AuthenticationServiceImpl.java $
 * $Id: AuthenticationServiceImpl.java 11178 2015-06-30 19:46:08Z ggolden $
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

package org.etudes.authentication.webapp;

import static org.etudes.util.Different.different;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.authentication.api.AuthenticationService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.sql.api.SqlService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * AuthenticationServiceImpl implements AuthenticationService.
 */
public class AuthenticationServiceImpl implements AuthenticationService, Service
{
	/** The max size for the user-agent database field, in characters. */
	protected static int MAX_USER_AGENT = 255;

	protected static byte[] noAddress =
	{ 0, 0, 0, 0 };

	/** Our log. */
	private static Log M_log = LogFactory.getLog(AuthenticationServiceImpl.class);

	/**
	 * Construct
	 */
	public AuthenticationServiceImpl()
	{
		M_log.info("AuthenticationServiceImpl: construct");
	}

	@Override
	public User authenticateByToken(String token, String ipAddress)
	{
		User rv = null;

		try
		{
			// find the authentication
			AuthenticationImpl auth = readAuthenticationAndUserTx(Long.valueOf(token));
			if (auth != null)
			{
				// check that the IP address matches
				if (!different(parseIpAddress(ipAddress), auth.getIpAddress()))
				{
					// check for closed
					if (!auth.isClosed())
					{
						// check for expired
						if (!auth.isExpired())
						{
							// we can use it - if we find the user
							rv = auth.getUser();
						}
					}
				}
			}
		}
		catch (NumberFormatException e)
		{
			M_log.warn("authenticateByToken: invalid token: " + token);
		}

		return rv;
	}

	@Override
	public void cancelAuthenticationToken(String token)
	{
		try
		{
			final Long id = Long.valueOf(token);

			sqlService().transact(new Runnable()
			{
				@Override
				public void run()
				{
					closeAuthenticationTx(id);
				}
			}, "createAuthenticationToken:browser");
		}
		catch (NumberFormatException e)
		{
			M_log.warn("cancelAuthenticationToken: invalid token: " + token);
		}
	}

	@Override
	public void clear(final User user)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				removeUserTx(user);
			}
		}, "clear(user)");
	}

	@Override
	public String createAuthenticationToken(User u, String ipAddress, String browser)
	{
		// trim browser to first MAX_USER_AGENT characters
		if (browser.length() > MAX_USER_AGENT)
		{
			browser = browser.substring(0, MAX_USER_AGENT);
		}
		final String browserTrimmed = browser;

		// read / register the browser string
		final Long[] browserId = new Long[1];
		browserId[0] = null;
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				browserId[0] = readBrowserTx(browserTrimmed);
				if (browserId[0] == null)
				{
					browserId[0] = insertBrowserTx(browserTrimmed);

					// if this fails, read
					// TODO: ? not sure this is right ?
					if (browserId[0] == null)
					{
						browserId[0] = readBrowserTx(browserTrimmed);
					}
				}
			}
		}, "createAuthenticationToken:browser");

		// create an authentication
		final AuthenticationImpl auth = new AuthenticationImpl(new Date(), browserId[0], parseIpAddress(ipAddress), u.getId());

		// save it
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				insertAuthenticaitonTx(auth);
			}
		}, "insertAuthentication");

		// return the id as a token
		return auth.getId().toString();
	}

	@Override
	public String getBroswerUserAgent(final Long id)
	{
		String rv = readBrowserTx(id);
		return rv;
	}

	@Override
	public boolean start()
	{
		M_log.info("AuthenticationServiceImpl: start");
		return true;
	}

	/**
	 * Transaction code for closing an authentication.
	 * 
	 * @param id
	 *        The authentication id.
	 */
	protected void closeAuthenticationTx(Long id)
	{
		String sql = "UPDATE AUTHENTICATION SET CLOSED=? WHERE ID=?";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = Boolean.TRUE;
		fields[i++] = id;

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for inserting an authentication.
	 * 
	 * @param auth
	 *        The authentication.
	 */
	protected void insertAuthenticaitonTx(AuthenticationImpl auth)
	{
		String sql = "INSERT INTO AUTHENTICATION (USER_ID, AUTHENTICATED_ON, IP, BROWSER_ID, CLOSED) VALUES (?,?,?,?,?)";

		Object[] fields = new Object[5];
		int i = 0;
		fields[i++] = auth.getUserId();
		fields[i++] = auth.getAuthenticatedOn();
		fields[i++] = auth.getIpAddress();
		fields[i++] = auth.getBrowserId();
		fields[i++] = auth.isClosed();

		Long id = sqlService().insert(sql, fields, "ID");
		auth.initId(id);
	}

	/**
	 * Transaction code for inserting a browse user-agent. It will fail if the record exists.
	 * 
	 * @param browser
	 *        The browser user-agent string.
	 * @return The new record's id.
	 */
	protected Long insertBrowserTx(String browser)
	{
		String sql = "INSERT INTO AUTHENTICATION_BROWSER (USER_AGENT) VALUES (?)";

		Object[] fields = new Object[1];
		int i = 0;
		fields[i++] = browser;

		Long id = sqlService().insert(sql, fields, "ID");
		return id;
	}

	/**
	 * Convert an IP address string into a numeric IP address
	 * 
	 * @param ipAddress
	 *        The IP address string.
	 * @return The numeric representation of the IP address as 16 (for IPv6) byte array - for IPv4, the address is in the first 4 bytes, the remainder are 0.
	 */
	protected byte[] parseIpAddress(String ipAddress)
	{
		byte[] rv = null;

		try
		{
			InetAddress addr = InetAddress.getByName(ipAddress);

			rv = addr.getAddress();
			if (rv.length == 4)
			{
				byte[] longrv = new byte[16];
				System.arraycopy(rv, 0, longrv, 0, 4);
				rv = longrv;
			}
		}
		catch (UnknownHostException e)
		{
			M_log.warn("parseIpAddress: " + ipAddress + " : " + e.toString());
			rv = noAddress;
		}

		return rv;
	}

	/**
	 * Transaction code for reading an authentication AND user.
	 * 
	 * @param id
	 *        The authentication id / token.
	 */
	protected AuthenticationImpl readAuthenticationAndUserTx(final Long id)
	{
		// read the auth info, and the user info, in one request
		String sql = "SELECT A.AUTHENTICATED_ON, A.RENEWED_ON, A.IP, A.BROWSER_ID, A.CLOSED, " + userService().sqlSelectFragment()
				+ " FROM AUTHENTICATION A " + userService().sqlJoinFragment("A.USER_ID") + " WHERE A.ID = ? GROUP BY " + userService().sqlGroupFragment();
		Object[] fields = new Object[1];
		fields[0] = id;
		List<AuthenticationImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<AuthenticationImpl>()
		{
			@Override
			public AuthenticationImpl read(ResultSet result)
			{
				AuthenticationImpl auth = new AuthenticationImpl();
				auth.initId(id);
				try
				{
					int i = 1;

					auth.initAuthenticatedOn(sqlService().readDate(result, i++));
					auth.initRenewedOn(sqlService().readDate(result, i++));
					auth.initIpAddress(sqlService().readBytes(result, i++));
					auth.initBrowserId(sqlService().readLong(result, i++));
					auth.initClosed(sqlService().readBoolean(result, i++));

					auth.initUser(userService().createFromResultSet(result, i));

					return auth;
				}
				catch (SQLException e)
				{
					M_log.warn("readUserTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading an authentication.
	 * 
	 * @param id
	 *        The authentication id / token.
	 */
	protected AuthenticationImpl readAuthenticationTx(final Long id)
	{
		String sql = "SELECT USER_ID, AUTHENTICATED_ON, RENEWED_ON, IP, BROWSER_ID, CLOSED FROM AUTHENTICATION WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<AuthenticationImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<AuthenticationImpl>()
		{
			@Override
			public AuthenticationImpl read(ResultSet result)
			{
				AuthenticationImpl auth = new AuthenticationImpl();
				auth.initId(id);
				try
				{
					int i = 1;

					auth.initUserId(sqlService().readLong(result, i++));
					auth.initAuthenticatedOn(sqlService().readDate(result, i++));
					auth.initRenewedOn(sqlService().readDate(result, i++));
					auth.initIpAddress(sqlService().readBytes(result, i++));
					auth.initBrowserId(sqlService().readLong(result, i++));
					auth.initClosed(sqlService().readBoolean(result, i++));

					return auth;
				}
				catch (SQLException e)
				{
					M_log.warn("readUserTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading a browser user-agent.
	 * 
	 * @param id
	 *        The browser user-agent id.
	 */
	protected String readBrowserTx(final Long id)
	{
		String sql = "SELECT USER_AGENT FROM AUTHENTICATION_BROWSER WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<String> rv = sqlService().select(sql, fields, new SqlService.Reader<String>()
		{
			@Override
			public String read(ResultSet result)
			{
				try
				{
					int i = 1;
					return sqlService().readString(result, i++);
				}
				catch (SQLException e)
				{
					M_log.warn("readBrowserTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading a browser user-agent id from the existing browser records.
	 * 
	 * @param browser
	 *        The browser user-agent string.
	 */
	protected Long readBrowserTx(final String browser)
	{
		String sql = "SELECT ID FROM AUTHENTICATION_BROWSER WHERE USER_AGENT = ?";
		Object[] fields = new Object[1];
		fields[0] = browser;
		List<Long> rv = sqlService().select(sql, fields, new SqlService.Reader<Long>()
		{
			@Override
			public Long read(ResultSet result)
			{
				try
				{
					int i = 1;
					return sqlService().readLong(result, i++);
				}
				catch (SQLException e)
				{
					M_log.warn("readBrowserTx: " + e);
					return null;
				}
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for removing tracking for a user.
	 * 
	 * @param user
	 *        The user.
	 */
	protected void removeUserTx(User user)
	{
		String sql = "DELETE FROM AUTHENTICATION WHERE USER_ID = ?";
		Object[] fields = new Object[1];
		fields[0] = user.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for renewing a non-closed authentication.
	 * 
	 * @param id
	 *        The authentication id.
	 * @param renewedOn
	 *        The renewal date.
	 */
	protected void renewAuthenticationTx(Long id, Date renewedOn)
	{
		String sql = "UPDATE AUTHENTICATION SET RENEWED_ON=? WHERE ID=? AND CLOSED=?";

		Object[] fields = new Object[3];
		int i = 0;
		fields[i++] = renewedOn;
		fields[i++] = id;
		fields[i++] = Boolean.FALSE;

		sqlService().update(sql, fields);
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
