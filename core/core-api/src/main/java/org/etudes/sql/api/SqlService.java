/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sql/api/SqlService.java $
 * $Id: SqlService.java 9981 2015-02-01 20:34:07Z ggolden $
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

package org.etudes.sql.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * E3 DB / SQL access service.
 */
public interface SqlService
{
	interface Reader<T>
	{
		/**
		 * Read fields from this result set, creating one object which is returned.
		 * 
		 * @param result
		 *        The SQL ResultSet, set to the proper record.
		 * @return The object read.
		 */
		T read(ResultSet result);
	}

	interface VoidReader
	{
		/**
		 * Read fields from this result set.
		 * 
		 * @param result
		 *        The SQL ResultSet, set to the proper record.
		 */
		void read(ResultSet result);
	}

	/**
	 * Convert the items in the Long item ids to a comma separated string.
	 * 
	 * @param source
	 *        The source set.
	 * @return The csv string.
	 */
	String idsToCsv(Set<Long> source);

	/**
	 * Process an insert query, with an auto-increment column (optional), returning the auto-increment value.
	 * 
	 * @param sql
	 *        The SQL query.
	 * @param fields
	 *        The array of fields for parameters.
	 * @param autoColumn
	 *        The name of the auto-increment column, or null if there is none.
	 * @return The auto-increment value, or null if there is none. If no auto-increment value is requested, a 0 is returned if the insert was successful.
	 */
	Long insert(String sql, Object[] fields, String autoColumn);

	/**
	 * Read a Boolean value from a result set.
	 * 
	 * @param result
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Boolean value.
	 * @throws SQLException
	 */
	Boolean readBoolean(ResultSet result, int index) throws SQLException;

	/**
	 * Read a byte array (from a BINARY data type) from a result set.
	 * 
	 * @param result
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The byte[] value (possibly null).
	 * @throws SQLException
	 */
	byte[] readBytes(ResultSet result, int index) throws SQLException;

	/**
	 * Read a Date value from a result set.
	 * 
	 * @param result
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Date value.
	 * @throws SQLException
	 */
	Date readDate(ResultSet result, int index) throws SQLException;

	/**
	 * Read a Float value from a result set.
	 * 
	 * @param result
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Float value.
	 * @throws SQLException
	 */
	Float readFloat(ResultSet result, int index) throws SQLException;

	/**
	 * Read an Integer value from a result set.
	 * 
	 * @param result
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Integer value.
	 * @throws SQLException
	 */
	Integer readInteger(ResultSet result, int index) throws SQLException;

	/**
	 * Read a Long value from a result set.
	 * 
	 * @param result
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Long value.
	 * @throws SQLException
	 */
	Long readLong(ResultSet result, int index) throws SQLException;

	/**
	 * Read a string value from a result set.
	 * 
	 * @param result
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The string value.
	 * @throws SQLException
	 */
	String readString(ResultSet result, int index) throws SQLException;

	/**
	 * Process a select query, filling in with fields, and return the results as a List, one per record read, as populated by the reader.
	 * 
	 * @param type
	 *        The type of the return objects in the list.
	 * @param sql
	 *        The SQL query.
	 * @param fields
	 *        The array of fields for parameters.
	 * @param reader
	 *        The reader object to read each record.
	 * @return The List of results, one per record.
	 */
	<T> List<T> select(String sql, Object[] fields, Reader<T> reader);

	/**
	 * Process a select query, filling in with fields, calling the reader for each record, with no return.
	 * 
	 * @param sql
	 *        The SQL query.
	 * @param fields
	 *        The array of fields for parameters.
	 * @param reader
	 *        The reader object to read each record.
	 * @return The List of results, one per record.
	 */
	void select(String sql, Object[] fields, VoidReader reader);

	/**
	 * Run the callback code in a transaction, assuring a single SQL connection for any database calls in the code. On deadlock, retry a few times.
	 * 
	 * @param callback
	 *        The code to run.
	 * @param tag
	 *        A string used when logging failure.
	 * @return true if successful, false if not.
	 */
	boolean transact(Runnable callback, String tag);

	/**
	 * Process an update query, with an auto-increment column (optional), returning the auto-increment value.
	 * 
	 * @param sql
	 *        The SQL query.
	 * @param fields
	 *        The array of fields for parameters.
	 * @param autoColumn
	 *        The name of the auto-increment column, or null if there is none.
	 * @return The auto-increment value, or null if there is none.
	 */
	void update(String sql, Object[] fields);
}
