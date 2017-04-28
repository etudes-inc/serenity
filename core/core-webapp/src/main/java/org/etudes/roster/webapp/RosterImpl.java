/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/roster/webapp/RosterImpl.java $
 * $Id: RosterImpl.java 8535 2014-08-28 22:34:35Z ggolden $
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

import org.etudes.roster.api.Client;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Roster;
import org.etudes.roster.api.RosterService;
import org.etudes.roster.api.Term;
import org.etudes.service.api.Services;

/**
 * RosterImpl implements Roster
 */
public class RosterImpl implements Roster
{
	/** Our log. */
	// private static Log M_log = LogFactory.getLog(RosterInfoImpl.class);

	protected Client client = null;
	protected Long id = null;
	protected Membership membership = null;
	protected String name = null;
	protected Boolean official = null;
	protected Term term = null;

	/**
	 * Construct.
	 */
	public RosterImpl(Long id, String name, Boolean official, Client client, Term term)
	{
		this.id = id;
		this.name = name;
		this.official = official;
		this.client = client;
		this.term = term;
		// Note: membership not loaded
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof RosterImpl)) return false;
		RosterImpl other = (RosterImpl) obj;
		if (different(id, other.id)) return false;

		return true;
	}

	@Override
	public Client getClient()
	{
		return this.client;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Membership getMembership()
	{
		if (this.membership == null)
		{
			// load membership
			this.membership = rosterService().getRosterMembership(this);
		}

		return this.membership;
	}

	@Override
	public String getName()
	{
		return this.name;
	}

	@Override
	public Term getTerm()
	{
		return this.term;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		String hash = this.id.toString();
		result = prime * result + hash.hashCode();
		return result;
	}

	@Override
	public Boolean isAdhoc()
	{
		return this.name.startsWith(((RosterServiceImpl) rosterService()).adhocRosterPrefix());
	}

	@Override
	public Boolean isMaster()
	{
		return this.name.startsWith(((RosterServiceImpl) rosterService()).masterRosterPrefix());
	}

	@Override
	public Boolean isOfficial()
	{
		return this.official;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
	}
}
