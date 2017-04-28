/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/cdp/api/CdpService.java $
 * $Id: CdpService.java 10677 2015-05-01 21:11:37Z ggolden $
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

package org.etudes.cdp.api;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.etudes.user.api.User;

/**
 * CdpService ...
 */
public interface CdpService
{
	/**
	 * Find the handler for the given request prefix.
	 * 
	 * @param prefix
	 *        The request prefix.
	 * @return The handler for this prefix, or null if none found.
	 */
	CdpHandler getCdpHandler(String prefix);

	/**
	 * Parse a Boolean from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The Boolean, or null if not sent.
	 */
	Boolean readBoolean(Object param);

	/**
	 * Parse a date from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The date, or null if not sent.
	 */
	Date readDate(Object param);

	/**
	 * Parse a float from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The Float, or null if not sent.
	 */
	Float readFloat(Object param);

	/**
	 * Parse a set of IDs from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The List<Long> of ids, or null if not sent.
	 */
	List<Long> readIds(Object param);

	/**
	 * Parse an int from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The Integer, or null if not sent.
	 */
	Integer readInt(Object param);

	/**
	 * Parse a long from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The Long, or null if not sent.
	 */
	Long readLong(Object param);

	/**
	 * Parse a String from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The String, or null if not sent.
	 */
	String readString(Object param);

	/**
	 * Parse a set of strings from the client.
	 * 
	 * @param param
	 *        The value from the client.
	 * @return The List<String> of strings, or null if not sent.
	 */
	List<String> readStrings(Object param);

	/**
	 * Register a handler.
	 * 
	 * @param handler
	 *        The handler.
	 */
	void registerCdpHandler(CdpHandler handler);

	/**
	 * Format the date for transfer as seconds since the epoc.
	 * 
	 * @param date
	 *        The date.
	 * @return The date as a Long.
	 */
	Long sendDate(Date date);

	/**
	 * Format a float for transfer
	 * 
	 * @param f
	 *        The float value
	 * @return The float as a string.
	 */
	String sendFloat(float f);

	/**
	 * Format a float for transfer
	 * 
	 * @param f
	 *        The float value
	 * @return The float as a string.
	 */
	String sendFloat(Float f);

	/**
	 * Format an integer for transfer
	 * 
	 * @param i
	 *        The integer value
	 * @return The integer as a string.
	 */
	String sendInt(int i);

	/**
	 * Format an integer for transfer
	 * 
	 * @param i
	 *        The integer value
	 * @return The integer as a string.
	 */
	String sendInt(Integer i);

	/**
	 * Format a long for transfer
	 * 
	 * @param l
	 *        The long value
	 * @return The long as a string.
	 */
	String sendLong(long l);

	/**
	 * Format a long for transfer
	 * 
	 * @param l
	 *        The long value
	 * @return The long as a string.
	 */
	String sendLong(Long l);

	/**
	 * Unregister a handler.
	 * 
	 * @param handler
	 *        The handler.
	 */
	void unregisterCdpHandler(CdpHandler handler);
}
