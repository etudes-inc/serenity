/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/authentication/api/Authentication.java $
 * $Id: Authentication.java 8293 2014-06-20 21:42:19Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

package org.etudes.authentication.api;

import java.util.Date;

import org.etudes.user.api.User;

/**
 * Authentication models an Etudes user authentication / login event.
 */
public interface Authentication
{
	/**
	 * @return The date of authentication.
	 */
	Date getAuthenticatedOn();

	/**
	 * @return The user's browser user-agent string.
	 */
	String getBrowserUserAgent();

	/**
	 * @return The user's browser id.
	 */
	Long getBrowserId();

	/**
	 * @return The latest authenticated or renewed date.
	 */
	Date getEffectiveOn();

	/**
	 * @return The date the authentication will expire. It can then be renewed.
	 */
	Date getExpiresOn();

	/**
	 * @return The authentication id / token.
	 */
	Long getId();

	/**
	 * @return The user IP address, as a byte[] (4 or 16 bytes).
	 */
	byte[] getIpAddress();

	/**
	 * @return The date the authentication was last renewed, or null if it has not been.
	 */
	Date getRenewedOn();

	/**
	 * @return The authenticated user.
	 */
	User getUser();

	/**
	 * @return TRUE if the authentication has been closed, FALSE if not.
	 */
	Boolean isClosed();

	/**
	 * @return TRUE if the authentication has expired, FALSE if not, if it is good.
	 */
	Boolean isExpired();
}
