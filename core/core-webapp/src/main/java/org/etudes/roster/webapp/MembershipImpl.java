/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/roster/webapp/MembershipImpl.java $
 * $Id: MembershipImpl.java 12506 2016-01-10 01:58:40Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2016 Etudes, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.user.api.User;

/**
 * MembershipImpl implements Membership
 */
public class MembershipImpl implements Membership
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(RosterImpl.class);

	protected boolean changed = false;

	protected Map<User, Member> memberMap = new HashMap<User, Member>();

	protected boolean mutable = false;

	protected Map<User, Member> origMemberMap = new HashMap<User, Member>();

	/**
	 * Construct.
	 */
	public MembershipImpl(boolean mutable)
	{
		this.mutable = mutable;
	}

	@Override
	public void add(User user, Role role, Boolean active)
	{
		if (!mutable)
		{
			M_log.warn("add: immutable");
			return;
		}

		memberMap.put(user, new MemberImpl(user, role, active));
		changed = true;
	}

	@Override
	public void assure(User user, Role role, Boolean active)
	{
		if (!mutable)
		{
			M_log.warn("assure: immutable");
			return;
		}

		// if the user does not have membership in this roster, add it
		Member member = findUser(user);
		if (member == null)
		{
			add(user, role, active);
		}

		// update the membership if active has changed, or the role is better
		else
		{
			if (member.isActive() != active)
			{
				update(user, active);
			}

			if (!member.getRole().ge(role))
			{
				update(user, role);
			}
		}
	}

	@Override
	public List<Member> findRole(Role role)
	{
		List<Member> rv = new ArrayList<Member>();
		for (Member member : this.memberMap.values())
		{
			if (role.equals(member.getRole()))
			{
				rv.add(member);
			}
		}

		return rv;
	}

	@Override
	public Member findUser(User user)
	{
		return this.memberMap.get(user);
	}

	@Override
	public Collection<Member> getMembers()
	{
		return this.memberMap.values();
	}

	@Override
	public Collection<User> getUsers()
	{
		return this.memberMap.keySet();
	}

	@Override
	public void remove(User user)
	{
		if (!mutable)
		{
			M_log.warn("remove: immutable");
			return;
		}

		this.memberMap.remove(user);
		this.changed = true;
	}

	@Override
	public void update(User user, Boolean active)
	{
		if (!mutable)
		{
			M_log.warn("update: immutable");
			return;
		}

		MemberImpl m = (MemberImpl) this.memberMap.get(user);
		if (m != null)
		{
			m.setActive(active);
			this.changed = true;
		}
	}

	@Override
	public void update(User user, Role role)
	{
		if (!mutable)
		{
			M_log.warn("update: immutable");
			return;
		}

		MemberImpl m = (MemberImpl) this.memberMap.get(user);
		if (m != null)
		{
			m.setRole(role);
			this.changed = true;
		}
	}

	protected void initMembers(List<Member> members)
	{
		for (Member m : members)
		{
			this.memberMap.put(m.getUser(), m);
			if (this.mutable) this.origMemberMap.put(m.getUser(), ((MemberImpl) m).clone());
		}
	}
}
