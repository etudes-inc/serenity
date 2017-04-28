/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/java/org/etudes/syllabus/webapp/SyllabusAcceptanceImpl.java $
 * $Id: SyllabusAcceptanceImpl.java 10165 2015-02-26 23:24:48Z ggolden $
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

package org.etudes.syllabus.webapp;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.service.api.Services;
import org.etudes.syllabus.api.Syllabus;
import org.etudes.syllabus.api.SyllabusAcceptance;
import org.etudes.syllabus.api.SyllabusService;
import org.etudes.user.api.User;

/**
 * SyllabusAcceptanceImpl implements SyllabusAcceptance.
 */
public class SyllabusAcceptanceImpl implements SyllabusAcceptance
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SyllabusServiceImpl.class);

	protected User acceptedBy = null;

	protected Date acceptedOn = null;

	protected Syllabus syllabus = null;

	protected Long syllabusId = null;

	protected Boolean valid = Boolean.TRUE;

	SyllabusAcceptanceImpl(Syllabus syllabus, User user, Date date)
	{
		this.syllabus = syllabus;
		this.syllabusId = syllabus.getId();
		this.acceptedBy = user;
		this.acceptedOn = date;
	}

	@Override
	public User getAcceptedBy()
	{
		return this.acceptedBy;
	}

	@Override
	public Date getAcceptedOn()
	{
		return this.acceptedOn;
	}

	@Override
	public Syllabus getSyllabus()
	{
		if (this.syllabus == null)
		{
			this.syllabus = syllabusService().get(this.syllabusId);
		}

		return this.syllabus;
	}

	@Override
	public Boolean isValid()
	{
		return this.valid;
	}

	protected Long getSyllabusId()
	{
		return this.syllabusId;
	}

	protected void initAcceptedBy(User user)
	{
		this.acceptedBy = user;
	}

	protected void initAcceptedOn(Date date)
	{
		this.acceptedOn = date;
	}

	protected void initSyllabusId(Long id)
	{
		this.syllabusId = id;
	}

	protected void initValid(Boolean valid)
	{
		this.valid = valid;
	}

	/**
	 * @return The registered SyllabusService.
	 */
	private SyllabusService syllabusService()
	{
		return (SyllabusService) Services.get(SyllabusService.class);
	}
}
