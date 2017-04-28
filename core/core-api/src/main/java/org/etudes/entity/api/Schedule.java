/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/entity/api/Schedule.java $
 * $Id: Schedule.java 11575 2015-09-06 22:49:07Z ggolden $
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

package org.etudes.entity.api;

import static org.etudes.util.Different.different;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;
import org.etudes.sitecontent.api.BaseDateService;

public class Schedule
{
	public enum Status
	{
		// hard coded in support's etudes-extend.js, scheduleStatusIcon(), and elsewhere
		closed(4, "Closed"), open(3, "Open"), willOpen(1, "Will Open"), willOpenHide(2, "Hidden Until Open");

		private final Integer id;
		private final String title;

		private Status(int id, String title)
		{
			this.id = Integer.valueOf(id);
			this.title = title;
		}

		public Integer getId()
		{
			return this.id;
		}
	}

	protected Date allowUntil = null;
	protected boolean changed = false;
	protected Date due = null;
	protected Boolean hideUntilOpen = Boolean.FALSE;
	protected Date open = null;

	public Schedule()
	{
	}

	/**
	 * Create as a copy of another.
	 * 
	 * @param other
	 *        The other.
	 */
	public Schedule(Schedule other)
	{
		this.allowUntil = other.allowUntil;
		this.changed = false;
		this.due = other.due;
		this.hideUntilOpen = other.hideUntilOpen;
		this.open = other.open;
	}

	/**
	 * Adjust all dates by this days offset.
	 * 
	 * @param days
	 *        The days offset.
	 */
	public void adjustDatesByDays(int days)
	{
		setOpen(baseDateService().adjustDateByDays(getOpen(), days));
		setDue(baseDateService().adjustDateByDays(getDue(), days));
		setAllowUntil(baseDateService().adjustDateByDays(getAllowUntil(), days));
	}

	/**
	 * Clear the changed flag.
	 */
	public void clearChanged()
	{
		this.changed = false;
	}

	/**
	 * Compute the effective close date - the later of the due or allow until dates.
	 * 
	 * @return The effective close date.
	 */
	public Date computeClose()
	{
		if (getAllowUntil() != null) return getAllowUntil();
		return getDue();
	}

	/**
	 * @return The allow until date. May be null.
	 */
	public Date getAllowUntil()
	{
		return this.allowUntil;
	}

	/**
	 * @return The due date. May be null.
	 */
	public Date getDue()
	{
		return this.due;
	}

	/**
	 * @return The hide until open setting.
	 */
	public Boolean getHideUntilOpen()
	{
		return this.hideUntilOpen;
	}

	/**
	 * @return The open date. May be null.
	 */
	public Date getOpen()
	{
		return this.open;
	}

	public Status getStatus()
	{
		Date now = new Date();
		if ((this.open != null) && (this.open.after(now)))
		{
			if (this.hideUntilOpen) return Status.willOpenHide;

			return Status.willOpen;
		}
		else if ((computeClose() != null) && (computeClose().before(now)))
		{
			return Status.closed;
		}

		return Status.open;
	}

	/**
	 * Initialize the allow until date.
	 * 
	 * @param date
	 *        The allow until date.
	 */
	public void initAllowUntil(Date date)
	{
		this.allowUntil = date;
	}

	/**
	 * Initialize the due date.
	 * 
	 * @param date
	 *        The due date.
	 */
	public void initDue(Date date)
	{
		this.due = date;
	}

	/**
	 * Initialize the hide until open setting
	 * 
	 * @param hide
	 *        The hide until open setting.
	 */
	public void initHideUntilOpen(Boolean hide)
	{
		if (hide == null) hide = Boolean.FALSE;
		this.hideUntilOpen = hide;
	}

	/**
	 * Initialize the open date.
	 * 
	 * @param date
	 *        The open date.
	 */
	public void initOpen(Date date)
	{
		this.open = date;
	}

	/**
	 * @return true if there have been changes, false if not.
	 */
	public boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * Update from CDP parameters.
	 * 
	 * @param prefix
	 *        The parameter names prefix.
	 * @param parameters
	 *        The parameters.
	 */
	public void read(String prefix, Map<String, Object> parameters)
	{
		setOpen(cdpService().readDate(parameters.get(prefix + "open")));
		setDue(cdpService().readDate(parameters.get(prefix + "due")));
		setAllowUntil(cdpService().readDate(parameters.get(prefix + "allowUntil")));
		setHideUntilOpen(cdpService().readBoolean(parameters.get(prefix + "hide")));
	}

	/**
	 * Format schedule for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (this.open != null) rv.put("open", this.open);
		if (this.due != null) rv.put("due", this.due);
		if (this.allowUntil != null) rv.put("allowUntil", this.allowUntil);
		rv.put("hide", this.hideUntilOpen);
		rv.put("status", getStatus().getId());
		if (this.computeClose() != null) rv.put("close", this.computeClose());

		return rv;
	}

	/**
	 * Set the allow until date.
	 * 
	 * @param date
	 *        The new allow until date, or null to set no allow until date.
	 */
	public void setAllowUntil(Date date)
	{
		if (different(date, this.allowUntil))
		{
			// TODO: enforce allow until >= due, and both > open
			this.changed = true;
			this.allowUntil = date;
		}
	}

	/**
	 * Set the due date.
	 * 
	 * @param date
	 *        The new due date, or null to set no due date.
	 */
	public void setDue(Date date)
	{
		if (different(date, this.due))
		{
			// TODO: enforce allow until >= due, and both > open
			this.changed = true;
			this.due = date;
		}
	}

	/**
	 * Set the hide until open.
	 * 
	 * @param hide
	 *        The new hide until open setting.
	 */
	public void setHideUntilOpen(Boolean hide)
	{
		if (hide == null) hide = Boolean.FALSE;
		if (different(hide, this.hideUntilOpen))
		{
			this.changed = true;
			this.hideUntilOpen = hide;
		}
	}

	/**
	 * Set the open date.
	 * 
	 * @param date
	 *        The new open date, or null to set no open date.
	 */
	public void setOpen(Date date)
	{
		if (different(date, this.open))
		{
			// TODO: open < due <= allow until
			this.changed = true;
			this.open = date;
		}
	}

	/**
	 * @return The registered BaseDateService.
	 */
	private BaseDateService baseDateService()
	{
		return (BaseDateService) Services.get(BaseDateService.class);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}
