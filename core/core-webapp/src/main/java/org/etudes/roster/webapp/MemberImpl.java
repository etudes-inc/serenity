/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/roster/webapp/MemberImpl.java $
 * $Id: MemberImpl.java 10921 2015-05-21 02:48:48Z ggolden $
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

package org.etudes.roster.webapp;

import static org.etudes.util.Different.different;

import java.util.HashMap;
import java.util.Map;

import org.etudes.file.api.Reference;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.Roster;
import org.etudes.user.api.User;

/**
 * MemberImpl implements Member.
 */
public class MemberImpl implements Member
{
	/** Our log. */
	// private static Log M_log = LogFactory.getLog(MembershipImpl.class);

	protected Boolean active = Boolean.TRUE;
	protected Boolean blocked = null;
	protected Long memberId = null;
	protected Role role = null;
	protected Roster roster = null;
	protected User user = null;

	/**
	 * Construct.
	 */
	public MemberImpl(User user, Role role, Boolean active)
	{
		this.user = user;
		this.role = role;
		this.active = active;
	}

	/**
	 * Construct.
	 */
	public MemberImpl(User user, Role role, Boolean active, Boolean blocked, Roster roster, Long memberId)
	{
		this.user = user;
		this.role = role;
		this.active = active;
		this.roster = roster;
		this.blocked = blocked;
		this.memberId = memberId;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof MemberImpl)) return false;
		MemberImpl other = (MemberImpl) obj;
		if (different(role, other.role)) return false;
		if (different(user, other.user)) return false;
		if (different(active, other.active)) return false;
		if (different(blocked, other.blocked)) return false;
		// don't care about roster or memberID
		return true;
	}

	@Override
	public Role getRole()
	{
		return this.role;
	}

	@Override
	public Roster getRoster()
	{
		return this.roster;
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
		String hash = this.role.toString() + this.user.getId().toString() + this.active.toString()
				+ ((this.blocked == null) ? "null" : this.blocked.toString());
		result = prime * result + hash.hashCode();
		return result;
	}

	@Override
	public Boolean isActive()
	{
		return this.active;
	}

	@Override
	public Boolean isBlocked()
	{
		return this.blocked;
	}

	@Override
	public Map<String, Object> send(Client client)
	{
		Map<String, Object> memberMap = new HashMap<String, Object>();

		memberMap.put("role", getRole().getLevel());
		memberMap.put("userId", getUser().getId());
		memberMap.put("nameDisplay", getUser().getNameDisplay());
		memberMap.put("nameSort", getUser().getNameSort());
		memberMap.put("eid", getUser().getEid());
		memberMap.put("iidFull", getUser().getIidDisplay());
		if (client != null)
		{
			memberMap.put("iid", getUser().getIidDisplay(client));
		}
		else
		{
			memberMap.put("iid", getUser().getIidDisplay());
		}

		Reference avatar = getUser().getAvatar();
		if (avatar != null)
		{
			String downloadUrl = avatar.getDownloadUrl();
			if (downloadUrl != null)
			{
				memberMap.put("avatar", downloadUrl);
			}
		}
		memberMap.put("admin", getUser().isAdmin());

		memberMap.put("active", isActive());
		memberMap.put("blocked", (isBlocked() == null) ? Boolean.FALSE : isBlocked());
		memberMap.put("rosterName", getRoster().getName());
		memberMap.put("rosterId", getRoster().getId());
		memberMap.put("official", getRoster().isOfficial());
		memberMap.put("adhoc", getRoster().isAdhoc());
		memberMap.put("master", getRoster().isMaster());

		return memberMap;
	}

	protected MemberImpl clone()
	{
		MemberImpl rv = new MemberImpl(this.user, this.role, this.active, this.blocked, this.roster, this.memberId);

		return rv;
	}

	protected void setActive(Boolean active)
	{
		this.active = active;
	}

	protected void setRole(Role role)
	{
		this.role = role;
	}
}
