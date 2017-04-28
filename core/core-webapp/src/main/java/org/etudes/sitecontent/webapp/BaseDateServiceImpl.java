/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sitecontent/webapp/BaseDateServiceImpl.java $
 * $Id: BaseDateServiceImpl.java 10509 2015-04-17 21:50:49Z ggolden $
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.sitecontent.api.BaseDateService;
import org.etudes.sitecontent.api.DateProvider;
import org.etudes.sitecontent.api.DateRange;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * BaseDateServiceImpl implements BaseDateService.
 */
public class BaseDateServiceImpl implements BaseDateService, Service
{
	class DateRangeImpl implements DateRange
	{
		protected Date max;
		protected Date min;
		protected Tool tool;

		public DateRangeImpl(Tool tool, Date min, Date max)
		{
			this.tool = tool;
			this.min = min;
			this.max = max;
		}

		@Override
		public Date getMax()
		{
			return this.max;
		}

		@Override
		public Date getMin()
		{
			return this.min;
		}

		@Override
		public Tool getTool()
		{
			return this.tool;
		}
	}

	/** Our log. */
	private static Log M_log = LogFactory.getLog(BaseDateServiceImpl.class);

	/**
	 * Construct
	 */
	public BaseDateServiceImpl()
	{
		M_log.info("PurgeServiceImpl: construct");
	}

	@Override
	public Date adjustDateByDays(Date date, int days)
	{
		if (date == null) return date;
		if (days == 0) return date;

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, days);
		return calendar.getTime();
	}

	@Override
	public void adjustDatesByDays(Site site, int days, User adjustingUser)
	{
		if (days == 0) return;

		// for all tools that handle site content
		Map<Tool, Service> handlers = Services.getHandlers(DateProvider.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				if (s.getValue() instanceof DateProvider)
				{
					((DateProvider) s.getValue()).adjustDatesByDays(site, days, adjustingUser);
				}
			}
		}
	}

	@Override
	public Date computeBaseDate(List<DateRange> ranges)
	{
		Date rv = null;
		for (DateRange range : ranges)
		{
			if (rv == null)
			{
				rv = range.getMin();
			}
			else if (range.getMin().before(rv))
			{
				rv = range.getMin();
			}
		}

		return rv;
	}

	@Override
	public int computeDayDifference(Date d1, Date d2)
	{
		GregorianCalendar first = new GregorianCalendar();
		GregorianCalendar last = new GregorianCalendar();

		int factor = 1;

		if (d1.before(d2))
		{
			first.setTime(d1);
			last.setTime(d2);
		}
		else
		{
			first.setTime(d2);
			last.setTime(d1);
			factor = -1;
		}

		int days = 0;

		// if the years are different, add the days to complete the first year, the days of the years between,
		// and the days get to the date in the second year
		if (first.get(Calendar.YEAR) != last.get(Calendar.YEAR))
		{
			// add the days to complete the first year
			days = first.getActualMaximum(Calendar.DAY_OF_YEAR) - first.get(Calendar.DAY_OF_YEAR);

			// add the days of all the years between
			GregorianCalendar tmp = (GregorianCalendar) first.clone();
			for (int year = first.get(Calendar.YEAR) + 1; year < last.get(Calendar.YEAR); year++)
			{
				tmp.set(Calendar.YEAR, year);
				int maxDays = tmp.getActualMaximum(Calendar.DAY_OF_YEAR);
				days += maxDays;
			}

			// add the days to the last date in that year
			days += last.get(Calendar.DAY_OF_YEAR);
		}

		// otherwise, in the same year, set the days to the difference
		else
		{
			days = last.get(Calendar.DAY_OF_YEAR) - first.get(Calendar.DAY_OF_YEAR);
		}

		return days * factor;
	}

	@Override
	public void computeMinMax(Date[] minMax, Date candidate)
	{
		if (candidate != null)
		{
			if (minMax[0] == null)
			{
				minMax[0] = candidate;
				minMax[1] = candidate;
			}
			else
			{
				if (candidate.before(minMax[0]))
				{
					minMax[0] = candidate;
				}
				if (candidate.after(minMax[1]))
				{
					minMax[1] = candidate;
				}
			}
		}
	}

	@Override
	public List<DateRange> getRanges(Site site)
	{
		List<DateRange> rv = new ArrayList<DateRange>();

		// for all tools that handle site content
		Map<Tool, Service> handlers = Services.getHandlers(DateProvider.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				if (s.getValue() instanceof DateProvider)
				{
					DateRange range = ((DateProvider) s.getValue()).getDateRange(site);
					if (range != null) rv.add(range);
				}
			}
		}

		return rv;
	}

	@Override
	public DateRange newDateRange(Tool tool, Date[] minMax)
	{
		return new DateRangeImpl(tool, minMax[0], minMax[1]);
	}

	@Override
	public boolean start()
	{
		M_log.info("BaseDateServiceImpl: start");
		return true;
	}
}
