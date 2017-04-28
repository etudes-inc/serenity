/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sitecontent/webapp/ArchivedSiteImpl.java $
 * $Id: ArchivedSiteImpl.java 10052 2015-02-10 04:32:42Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015 Etudes, Inc.
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

package org.etudes.sitecontent.webapp;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.roster.api.Client;
import org.etudes.roster.api.Term;
import org.etudes.sitecontent.api.ArchivedSite;

/**
 * ArchivedSiteImpl implements ArchivedSite
 */
public class ArchivedSiteImpl implements ArchivedSite
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ArchivedSiteImpl.class);

	protected Date archivedOn = null;

	protected Client client = null;

	protected Long id = null;

	protected String name = null;

	protected Long siteId = null;

	protected Term term = null;

	public ArchivedSiteImpl(Long id, Term term, Client client, Long siteId, String name, Date archivedOn)
	{
		this.id = id;
		this.term = term;
		this.client = client;
		this.siteId = siteId;
		this.name = name;
		this.archivedOn = archivedOn;
	}

	@Override
	public Date getArchivedOn()
	{
		return this.archivedOn;
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
	public String getName()
	{
		return this.name;
	}

	@Override
	public Long getSiteId()
	{
		return this.siteId;
	}

	@Override
	public Term getTerm()
	{
		return this.term;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}
}
