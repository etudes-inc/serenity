/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sql/webapp/SqlServiceImpl.java $
 * $Id: SqlServiceImpl.java 9981 2015-02-01 20:34:07Z ggolden $
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

package org.etudes.sql.webapp;

import static org.etudes.util.StringUtil.trimToNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.sql.api.SqlService;
import org.etudes.threadlocal.api.ThreadLocalService;

/**
 * SqlServiceImpl implements SqlService.
 */
public class SqlServiceImpl implements SqlService, Service
{
	@SuppressWarnings("serial")
	class DeadlockException extends RuntimeException
	{
	}

	protected static final String TRANSACTION_CONNECTION_KEY = "SqlServiceImpl:TransactionConnection";

	/** Our log. */
	private static Log M_log = LogFactory.getLog(SqlServiceImpl.class);

	/** DBCP backed DataSource for database connections. */
	protected DataSource db = null;

	/** Configuration: number of on-deadlock retries for save. */
	protected int m_deadlockRetries = 5;

	/**
	 * Construct
	 */
	public SqlServiceImpl()
	{
		M_log.info("SqlServiceImpl: construct");

		// setup to get configured once all services are started
		Services.whenAvailable(ConfigService.class, new Runnable()
		{
			public void run()
			{
				// setup MySQL DBCP connection pooling
				BasicDataSource source = new BasicDataSource();
				source.setDriverClassName("com.mysql.jdbc.Driver");
				source.setUsername(configService().getString("SqlService.userName"));
				source.setPassword(configService().getString("SqlService.password"));
				source.setUrl(configService().getString("SqlService.url"));
				source.setConnectionProperties("characterEncoding=utf8");
				// source.setConnectionProperties("characterEncoding=utf8;alwaysSendSetIsolation=false;elideSetAutoCommits=true;useLocalSessionState=true;");
				source.setDefaultAutoCommit(false);
				source.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
				source.setValidationQuery("select 1 from DUAL");
				source.setInitialSize(10); // TODO: raise this to a production size, like 100
				source.setMaxIdle(10);
				source.setMinIdle(0);
				source.setMaxActive(configService().getInt("SqlService.maxActive", 10));
				source.setMaxWait(60000); // wait a minute to get a connection
				db = source;

				M_log.info("SqlServiceImpl: pool size: " + source.getMaxActive());
			}
		});
	}

	@Override
	public String idsToCsv(Set<Long> source)
	{
		StringBuilder rv = new StringBuilder();

		for (Long id : source)
		{
			rv.append(id.toString()).append(",");
		}

		if (rv.length() > 0)
		{
			rv.setLength(rv.length() - 1);
		}

		return rv.toString();
	}

	@Override
	public Long insert(String sql, Object[] fields, String autoColumn)
	{
		Connection transactionConnection = (Connection) threadLocalService().get(TRANSACTION_CONNECTION_KEY);

		Connection connection = null;
		PreparedStatement statement = null;
		boolean autoCommit = false;
		boolean resetAutoCommit = false;

		boolean success = false;
		Long rv = Long.valueOf(0);

		try
		{
			if (transactionConnection != null)
			{
				connection = transactionConnection;
			}
			else
			{
				connection = getConnection();

				// make sure we have do not have auto commit - will change and reset if needed
				autoCommit = connection.getAutoCommit();
				if (autoCommit)
				{
					connection.setAutoCommit(false);
					resetAutoCommit = true;
				}
			}

			if (autoColumn != null)
			{
				String[] autoColumns = new String[1];
				autoColumns[0] = autoColumn;
				statement = connection.prepareStatement(sql, autoColumns);
			}
			else
			{
				statement = connection.prepareStatement(sql);
			}

			// put in all the fields
			prepareStatement(statement, fields);

			// run it
			statement.executeUpdate();

			if (autoColumn != null)
			{
				ResultSet keys = statement.getGeneratedKeys();
				if (keys != null)
				{
					if (keys.next())
					{
						rv = new Long(keys.getLong(1));
					}
				}
			}

			// commit unless we are in a transaction (provided with a connection)
			if (transactionConnection == null)
			{
				connection.commit();
			}

			// indicate success
			success = true;
		}
		catch (SQLException e)
		{
			// is this due to a key constraint problem (mySql code 1062)
			boolean recordAlreadyExists = e.getErrorCode() == 1062;
			if (recordAlreadyExists) return null;

			// perhaps due to a mysql deadlock?
			if (e.getErrorCode() == 1213)
			{
				M_log.warn("insert(): deadlock: error code: " + e.getErrorCode() + " sql: " + sql + " binds: " + debugFields(fields) + " "
						+ e.toString());
				throw new DeadlockException();
			}
			else
			{
				// something else went wrong
				M_log.warn("insert(): error code: " + e.getErrorCode() + " sql: " + sql + " binds: " + debugFields(fields) + " ", e);
				throw new RuntimeException("SqlService.insert failure", e);
			}
		}
		catch (Exception e)
		{
			M_log.warn("insert(): " + e);
			throw new RuntimeException("SqlService.insert failure", e);
		}
		finally
		{
			try
			{
				if (statement != null) statement.close();

				if ((connection != null) && (transactionConnection == null))
				{
					// roll back on failure
					if (!success)
					{
						connection.rollback();
					}

					// if we changed the auto commit, reset here
					if (resetAutoCommit)
					{
						connection.setAutoCommit(autoCommit);
					}
					doneWithConnection(connection);
				}
			}
			catch (Exception e)
			{
				M_log.warn("insert(): " + e);
				throw new RuntimeException("SqlService.insert failure", e);
			}
		}

		return rv;
	}

	@Override
	public Boolean readBoolean(ResultSet result, int index) throws SQLException
	{
		String s = result.getString(index);
		if (s == null) return null;
		Boolean rv = Boolean.valueOf(s.equals("1"));
		return rv;
	}

	@Override
	public byte[] readBytes(ResultSet result, int index) throws SQLException
	{
		InputStream is = null;
		try
		{
			is = result.getBinaryStream(index);
			byte[] rv = IOUtils.toByteArray(is);
			return rv;
		}
		catch (IOException e)
		{
			M_log.warn("readBytes: " + e.toString());
		}
		finally
		{
			if (is != null) try
			{
				is.close();
			}
			catch (IOException e)
			{
				M_log.warn("readBytes: " + e.toString());
			}
		}

		return null;
	}

	@Override
	public Date readDate(ResultSet result, int index) throws SQLException
	{
		long time = result.getLong(index);
		if (time == 0) return null;
		return new Date(time);
	}

	@Override
	public Float readFloat(ResultSet result, int index) throws SQLException
	{
		String str = trimToNull(result.getString(index));
		if (str == null) return null;
		try
		{
			return Float.valueOf(str);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public Integer readInteger(ResultSet result, int index) throws SQLException
	{
		String str = trimToNull(result.getString(index));
		if (str == null) return null;
		try
		{
			return Integer.valueOf(str);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public Long readLong(ResultSet result, int index) throws SQLException
	{
		String str = trimToNull(result.getString(index));
		if (str == null) return null;
		try
		{
			return Long.valueOf(str);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public String readString(ResultSet result, int index) throws SQLException
	{
		return trimToNull(result.getString(index));
	}

	@Override
	public <T> List<T> select(String sql, Object[] fields, SqlService.Reader<T> reader)
	{
		if (sql == null) return null;
		if (reader == null) return null;

		Connection transactionConnection = (Connection) threadLocalService().get(TRANSACTION_CONNECTION_KEY);

		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		List<T> rv = new ArrayList<T>();

		try
		{
			// borrow a new connection if we are not provided with one to use
			if (transactionConnection != null)
			{
				connection = transactionConnection;
			}
			else
			{
				connection = getConnection();
			}

			// prepare the statement
			statement = connection.prepareStatement(sql);
			prepareStatement(statement, fields);

			// run the query
			result = statement.executeQuery();

			// process the results
			while (result.next())
			{
				try
				{
					T obj = reader.read(result);
					if (obj != null) rv.add(obj);
				}
				catch (Throwable t)
				{
					M_log.warn("dbRead: sql: " + sql + " fields: " + debugFields(fields) + " row: " + result.getRow(), t);
				}
			}
		}
		catch (Exception e)
		{
			M_log.warn("dbRead: sql: " + sql + " fields: " + debugFields(fields), e);
		}
		finally
		{
			try
			{
				if (result != null) result.close();
				if (statement != null) statement.close();

				// return the connection only if we have borrowed a new one for this call
				if (transactionConnection == null)
				{
					if (connection != null)
					{
						doneWithConnection(connection);
					}
				}
			}
			catch (Exception e)
			{
				M_log.warn("dbRead: sql: " + sql + " fields: " + debugFields(fields), e);
			}
		}

		return rv;
	}

	@Override
	public void select(String sql, Object[] fields, VoidReader reader)
	{
		if (sql == null) return;
		if (reader == null) return;

		Connection transactionConnection = (Connection) threadLocalService().get(TRANSACTION_CONNECTION_KEY);

		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;

		try
		{
			// borrow a new connection if we are not provided with one to use
			if (transactionConnection != null)
			{
				connection = transactionConnection;
			}
			else
			{
				connection = getConnection();
			}

			// prepare the statement
			statement = connection.prepareStatement(sql);
			prepareStatement(statement, fields);

			// run the query
			result = statement.executeQuery();

			// process the results
			while (result.next())
			{
				try
				{
					reader.read(result);
				}
				catch (Throwable t)
				{
					M_log.warn("dbRead: sql: " + sql + " fields: " + debugFields(fields) + " row: " + result.getRow(), t);
				}
			}
		}
		catch (Exception e)
		{
			M_log.warn("dbRead: sql: " + sql + " fields: " + debugFields(fields), e);
		}
		finally
		{
			try
			{
				if (result != null) result.close();
				if (statement != null) statement.close();

				// return the connection only if we have borrowed a new one for this call
				if (transactionConnection == null)
				{
					if (connection != null)
					{
						doneWithConnection(connection);
					}
				}
			}
			catch (Exception e)
			{
				M_log.warn("dbRead: sql: " + sql + " fields: " + debugFields(fields), e);
			}
		}
	}

	@Override
	public boolean start()
	{
		M_log.info("SqlServiceImpl: start");

		return true;
	}

	@Override
	public boolean transact(Runnable callback, String tag)
	{
		// if in a transaction already
		if (threadLocalService().get(TRANSACTION_CONNECTION_KEY) != null)
		{
			callback.run();
			return true;
		}

		// in case of deadlock we might retry
		for (int i = 0; i <= m_deadlockRetries; i++)
		{
			if (i > 0)
			{
				// make a little fuss
				M_log.warn("transact: deadlock: retrying (" + i + " / " + m_deadlockRetries + "): " + tag);

				// do a little wait, longer for each retry
				try
				{
					Thread.sleep(i * 100L);
				}
				catch (Exception ignore)
				{
				}
			}

			Connection connection = null;
			boolean autoCommit = false;
			boolean resetAutoCommit = false;

			try
			{
				connection = getConnection();

				// make sure we have do not have auto commit - will change and reset if needed
				autoCommit = connection.getAutoCommit();
				if (autoCommit)
				{
					connection.setAutoCommit(false);
					resetAutoCommit = true;
				}

				// store the connection in the thread
				threadLocalService().put(TRANSACTION_CONNECTION_KEY, connection);

				// run the transaction code
				callback.run();

				connection.commit();

				return true;
			}
			catch (DeadlockException e)
			{
				// roll back
				if (connection != null)
				{
					try
					{
						connection.rollback();
						M_log.warn("transact: deadlock: rolling back: " + tag);
					}
					catch (Exception ee)
					{
						M_log.warn("transact: (deadlock: rollback): " + tag + " : " + ee);
					}
				}

				// if this was the last attempt, throw to abort
				if (i == m_deadlockRetries)
				{
					M_log.warn("transact: deadlock: retry failure: " + tag);
					throw new RuntimeException("SqlService.transact deadlock", e);
				}
			}
			catch (RuntimeException e)
			{
				// roll back
				if (connection != null)
				{
					try
					{
						connection.rollback();
						M_log.warn("transact: rolling back: " + tag);
					}
					catch (Exception ee)
					{
						M_log.warn("transact: (rollback): " + tag + " : " + ee);
					}
				}
				M_log.warn("transact: failure: " + e);
				throw e;
			}
			catch (SQLException e)
			{
				// roll back
				if (connection != null)
				{
					try
					{
						connection.rollback();
						M_log.warn("transact: rolling back: " + tag);
					}
					catch (Exception ee)
					{
						M_log.warn("transact: (rollback): " + tag + " : " + ee);
					}
				}
				M_log.warn("transact: failure: " + e);
				throw new RuntimeException("SqlService.transact failure", e);
			}

			finally
			{
				if (connection != null)
				{
					// clear the connection from the thread
					threadLocalService().remove(TRANSACTION_CONNECTION_KEY);

					// if we changed the auto commit, reset here
					if (resetAutoCommit)
					{
						try
						{
							connection.setAutoCommit(autoCommit);
						}
						catch (SQLException e)
						{
							M_log.warn("transact: " + e);
						}
					}
					doneWithConnection(connection);
				}
			}
		}

		return false;
	}

	@Override
	public void update(String sql, Object[] fields)
	{
		insert(sql, fields, null);
	}

	/**
	 * Format the fields for logging
	 * 
	 * @param fields
	 *        The statement fields.
	 * @return The fields as a string
	 */
	protected String debugFields(Object[] fields)
	{
		StringBuilder buf = new StringBuilder();
		if (fields != null)
		{
			for (Object o : fields)
			{
				if (buf.length() > 0) buf.append(", ");
				if (o == null)
				{
					buf.append("null");
				}
				else
				{
					buf.append(o.toString());
				}
			}
		}

		return buf.toString();
	}

	/**
	 * Call when done with a connection.
	 */
	protected void doneWithConnection(Connection conn)
	{
		if (conn != null)
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				M_log.warn("doneWithConnection:", e);
			}
		}
	}

	/**
	 * Get a connection to the database.
	 */
	protected Connection getConnection() throws SQLException
	{
		Connection rv = this.db.getConnection();

		// Connection rv = DriverManager
		// .getConnection("jdbc:mysql://localhost:3306/e3?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=false&cachePrepStmts=true&prepStmtCacheSize=4096&prepStmtCacheSqlLimit=4096"
		// + "&user=e3&password=password");
		return rv;
	}

	/**
	 * Prepare a prepared statement with fields.
	 * 
	 * @param statement
	 *        The prepared statement to fill in.
	 * @param fields
	 *        The Object array of values to fill in.
	 * @throws SQLException
	 * @throws UnsupportedEncodingException
	 */
	protected void prepareStatement(PreparedStatement statement, Object[] fields) throws SQLException, UnsupportedEncodingException
	{
		// put in all the fields
		int pos = 1;
		if ((fields != null) && (fields.length > 0))
		{
			for (Object o : fields)
			{
				// null or null string get stored as a SQL null value
				if (o == null || (o instanceof String && ((String) o).length() == 0))
				{
					statement.setObject(pos, null);
					pos++;
				}

				// a date is stored as Java's milliseconds since epoc
				else if (o instanceof Date)
				{
					Date date = (Date) o;
					statement.setLong(pos, date.getTime());
					pos++;
				}

				else if (o instanceof Long)
				{
					long l = ((Long) o).longValue();
					statement.setLong(pos, l);
					pos++;
				}

				else if (o instanceof Integer)
				{
					int n = ((Integer) o).intValue();
					statement.setInt(pos, n);
					pos++;
				}

				else if (o instanceof Float)
				{
					float f = ((Float) o).floatValue();
					statement.setFloat(pos, f);
					pos++;
				}

				// Booleans are stored as char '0' or '1'
				else if (o instanceof Boolean)
				{
					String value = (Boolean) o ? "1" : "0";
					statement.setBytes(pos, value.getBytes("UTF-8"));
					pos++;
				}

				else if (o instanceof InputStream)
				{
					statement.setBinaryStream(pos, (InputStream) o);
					pos++;
				}

				else if (o instanceof byte[])
				{
					statement.setBinaryStream(pos, new ByteArrayInputStream((byte[]) o));
					pos++;
				}

				// TODO: add support for any other types

				// store as bytes of a UTF-8 encoded string
				else
				{
					String value = o.toString();
					statement.setBytes(pos, value.getBytes("UTF-8"));
					pos++;
				}
			}
		}
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
	}

	/**
	 * @return The registered ThreadLocalService.
	 */
	private ThreadLocalService threadLocalService()
	{
		return (ThreadLocalService) Services.get(ThreadLocalService.class);
	}
}
