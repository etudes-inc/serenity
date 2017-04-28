/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/util/Cookies.java $
 * $Id: Cookies.java 9491 2014-12-08 21:42:09Z ggolden $
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

package org.etudes.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility class to deal with cookies.
 */
public class Cookies
{
	/**
	 * Read a cookie from the request.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param name
	 *        The cookie name.
	 * @return The Cookie, or null if not found.
	 */
	public static Cookie cookieRead(HttpServletRequest req, String name)
	{
		Cookie[] cookies = req.getCookies();
		Cookie rv = null;
		if (cookies != null)
		{
			for (int i = 0; i < cookies.length; i++)
			{
				if (cookies[i].getName().equals(name))
				{
					rv = cookies[i];
					break;
				}
			}
		}

		return rv;
	}

	/**
	 * Remove a session cookie.
	 * 
	 * @param res
	 *        The response object.
	 * @param name
	 *        The cookie name.
	 */
	public static void cookieRemove(HttpServletResponse res, String name)
	{
		Cookie c = new Cookie(name, "");
		c.setPath("/");
		c.setMaxAge(0);
		res.addCookie(c);
	}

	/**
	 * Write a session cookie to the response.
	 * 
	 * @param res
	 *        The response object.
	 * @param name
	 *        The cookie name.
	 * @param value
	 *        The cookie value.
	 */
	public static void cookieWrite(HttpServletResponse res, String name, String value)
	{
		Cookie c = new Cookie(name, value);
		c.setPath("/");
		c.setMaxAge(-1);
		res.addCookie(c);
	}
}
