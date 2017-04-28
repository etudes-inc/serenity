/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/authentication/webapp/AuthenticationImpl.java $
 * $Id: AuthenticationImpl.java 8428 2014-08-01 23:43:30Z ggolden $
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

package org.etudes.authentication.webapp;

import static org.etudes.util.Different.different;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.authentication.api.Authentication;
import org.etudes.authentication.api.AuthenticationService;
import org.etudes.service.api.Services;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * User implementation.
 */
public class AuthenticationImpl implements Authentication
{
	/** The max duration for an authentication, before it expires, in ms. */
	protected static int MAX_AUTHENTICATION_DURATION = 24 * 60 * 60 * 1000; // a day

	/** Our log. */
	private static Log M_log = LogFactory.getLog(AuthenticationImpl.class);

	protected Date authenticatedOn = null;

	protected Long browserId = null;

	protected Boolean closed = null;

	protected Long id = null;

	protected byte[] ipAddress = null;

	protected Date renewedOn = null;

	/** User might be an object set, or an id. */
	protected User user = null;
	protected Long userId = null;

	/**
	 * Construct.
	 */
	public AuthenticationImpl()
	{
	}

	/**
	 * Construct as a new authentication.
	 * 
	 * @param authenticatedOn
	 *        The authenticated on date.
	 * @param browserUserAgentId
	 *        The browser user agent id.
	 * @param ipAddress
	 *        The user's IP address.
	 * @param userId
	 *        The authenticated user id.
	 */
	public AuthenticationImpl(Date authenticatedOn, Long browserId, byte[] ipAddress, Long userId)
	{
		this.authenticatedOn = authenticatedOn;
		this.browserId = browserId;
		this.ipAddress = ipAddress;
		this.userId = userId;
		this.closed = Boolean.FALSE;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof AuthenticationImpl)) return false;
		AuthenticationImpl other = (AuthenticationImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public Date getAuthenticatedOn()
	{
		return this.authenticatedOn;
	}

	@Override
	public Long getBrowserId()
	{
		return this.browserId;
	}

	@Override
	public String getBrowserUserAgent()
	{
		return authenticationService().getBroswerUserAgent(this.browserId);
	}

	@Override
	public Date getEffectiveOn()
	{
		return this.renewedOn == null ? this.authenticatedOn : this.renewedOn;
	}

	@Override
	public Date getExpiresOn()
	{
		Date rv = new Date(getEffectiveOn().getTime() + MAX_AUTHENTICATION_DURATION);
		return rv;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public byte[] getIpAddress()
	{
		return this.ipAddress;
	}

	@Override
	public Date getRenewedOn()
	{
		return this.renewedOn;
	}

	@Override
	public User getUser()
	{
		if (this.user != null)
		{
			return this.user;
		}
		else if (this.userId != null)
		{
			return userService().get(this.userId);
		}

		return null;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public Boolean isClosed()
	{
		return this.closed;
	}

	@Override
	public Boolean isExpired()
	{
		return (new Date().after(getExpiresOn()));
	}

	protected Long getUserId()
	{
		if (this.user != null)
		{
			return this.user.getId();
		}

		return this.userId;
	}

	protected void initAuthenticatedOn(Date date)
	{
		this.authenticatedOn = date;
	}

	protected void initBrowserId(Long id)
	{
		this.browserId = id;
	}

	protected void initClosed(Boolean closed)
	{
		this.closed = closed;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initIpAddress(byte[] ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	protected void initRenewedOn(Date date)
	{
		this.renewedOn = date;
	}

	protected void initUser(User user)
	{
		this.user = user;
		this.userId = null;
	}

	protected void initUserId(Long userId)
	{
		this.userId = userId;
		this.user = null;
	}

	/**
	 * @return The registered AuthenticationService.
	 */
	private AuthenticationService authenticationService()
	{
		return (AuthenticationService) Services.get(AuthenticationService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}
