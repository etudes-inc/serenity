/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/roster/webapp/SiteMemberImpl.java $
 * $Id: SiteMemberImpl.java 8472 2014-08-15 22:14:07Z ggolden $
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

package org.etudes.roster.webapp;

import static org.etudes.util.Different.different;

import org.etudes.roster.api.Role;
import org.etudes.roster.api.Roster;
import org.etudes.roster.api.SiteMember;
import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * SiteMemberImpl implements SiteMember.
 */
public class SiteMemberImpl implements SiteMember
{
	/** Our log. */
	// private static Log M_log = LogFactory.getLog(SiteMemberImpl.class);

	protected Role role = null;
	protected Site site = null;
	protected User user = null;

	/**
	 * Construct.
	 */
	public SiteMemberImpl(User user, Role role, Site site)
	{
		this.user = user;
		this.role = role;
		this.site = site;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SiteMemberImpl)) return false;
		SiteMemberImpl other = (SiteMemberImpl) obj;
		if (different(role, other.role)) return false;
		if (different(user, other.user)) return false;
		if (different(site, other.site)) return false;
		// don't care about roster
		return true;
	}

	@Override
	public Role getRole()
	{
		return this.role;
	}

	@Override
	public Site getSite()
	{
		return this.site;
	}

	@Override
	public User getUser()
	{
		return this.user;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		String hash = this.role.toString() + this.user.getId().toString() + this.site.getId().toString();
		result = prime * result + hash.hashCode();
		return result;
	}
}
