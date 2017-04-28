/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/authentication/api/AuthenticationService.java $
 * $Id: AuthenticationService.java 10165 2015-02-26 23:24:48Z ggolden $
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

package org.etudes.authentication.api;

import org.etudes.user.api.User;

/**
 * The E3 Authentication service.
 */
public interface AuthenticationService
{
	/**
	 * Authenticate a request by token.
	 * 
	 * @param token
	 *        The authentication token.
	 * @param ipAddress
	 *        The user's IP address (v4 or v6).
	 * @return The user authenticated, or null if not authenticated.
	 */
	User authenticateByToken(String token, String ipAddress);

	/**
	 * Cancel this token - it will no longer be accepted to authentication a user.
	 * 
	 * @param token
	 *        The token to cancel.
	 */
	void cancelAuthenticationToken(String token);

	/**
	 * Remove all authentication history for this user.
	 * 
	 * @param user
	 *        The user.
	 */
	void clear(User user);

	/**
	 * Create a token that can be used for subsequent authentication for this user.
	 * 
	 * @param u
	 *        The user.
	 * @param ipAddress
	 *        The user's IP address (v4 or v6).
	 * @param browserUserAgent
	 *        The user's browser user-agent string.
	 * @return The authentication token.
	 */
	String createAuthenticationToken(User u, String ipAddress, String browserUserAgent);

	/**
	 * Return the browser user-agent string registered with this id.
	 * 
	 * @param id
	 *        The browser id.
	 * @return The browser user-agent string for this id, or null if not found.
	 */
	String getBroswerUserAgent(Long id);
}
