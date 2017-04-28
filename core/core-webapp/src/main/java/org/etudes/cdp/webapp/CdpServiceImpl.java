/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/cdp/webapp/CdpServiceImpl.java $
 * $Id: CdpServiceImpl.java 10677 2015-05-01 21:11:37Z ggolden $
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

package org.etudes.cdp.webapp;

import static org.etudes.util.StringUtil.split;
import static org.etudes.util.StringUtil.trimToNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Service;

/**
 * CdpService ...
 */
public class CdpServiceImpl implements CdpService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(CdpServiceImpl.class);

	/** Map of request prefix -> cdp handler. */
	protected Map<String, CdpHandler> handlers = new HashMap<String, CdpHandler>();

	/**
	 * Construct
	 */
	public CdpServiceImpl()
	{
		M_log.info("CdpServiceImpl: construct");
	}

	@Override
	public CdpHandler getCdpHandler(String prefix)
	{
		return this.handlers.get(prefix);
	}

	@Override
	public Boolean readBoolean(Object param)
	{
		if (param == null) return null;
		return ("1".equals(param) || "true".equals(param)) ? Boolean.TRUE : Boolean.FALSE;
	}

	@Override
	public Date readDate(Object param)
	{
		if (param == null) return null;
		try
		{
			return new Date(Long.parseLong((String) param));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public Float readFloat(Object param)
	{
		if (param == null) return null;
		try
		{
			return Float.valueOf((String) param);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public List<Long> readIds(Object param)
	{
		if (param == null) return null;
		String str = (String) param;

		List<Long> rv = new ArrayList<Long>();

		if (str.indexOf("\t") != -1)
		{
			String[] idStrs = split(str, "\t");
			for (String s : idStrs)
			{
				s = trimToNull(s);
				if (s == null) continue;
				try
				{
					Long l = Long.valueOf(s);
					rv.add(l);
				}
				catch (NumberFormatException e)
				{
				}
			}
		}
		else if (str.indexOf(",") != -1)
		{
			String[] idStrs = split(str, ",");
			for (String s : idStrs)
			{
				s = trimToNull(s);
				if (s == null) continue;
				try
				{
					Long l = Long.valueOf(s);
					rv.add(l);
				}
				catch (NumberFormatException e)
				{
				}
			}
		}
		else
		{
			try
			{
				String s = trimToNull(str);
				if (s != null)
				{
					Long l = Long.valueOf(str);
					rv.add(l);
				}
			}
			catch (NumberFormatException e)
			{
			}
		}

		return rv;
	}

	@Override
	public Integer readInt(Object param)
	{
		if (param == null) return null;
		try
		{
			return Integer.valueOf((String) param);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public Long readLong(Object param)
	{
		if (param == null) return null;
		try
		{
			return Long.valueOf((String) param);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	@Override
	public String readString(Object param)
	{
		if (param == null) return null;
		return (String) param;
	}

	@Override
	public List<String> readStrings(Object param)
	{
		if (param == null) return null;
		String str = (String) param;

		List<String> rv = new ArrayList<String>();

		if (str.indexOf("\t") != -1)
		{
			String[] idStrs = split(str, "\t");
			for (String s : idStrs)
			{
				s = trimToNull(s);
				if (s == null) continue;
				rv.add(s);
			}
		}
		else if (str.indexOf(",") != -1)
		{
			String[] idStrs = split(str, ",");
			for (String s : idStrs)
			{
				s = trimToNull(s);
				if (s == null) continue;
				rv.add(s);
			}
		}
		else
		{
			String s = trimToNull(str);
			if (s != null)
			{
				rv.add(s);
			}
		}

		return rv;
	}

	@Override
	public void registerCdpHandler(CdpHandler handler)
	{
		this.handlers.put(handler.getPrefix(), handler);
	}

	@Override
	public Long sendDate(Date date)
	{
		if (date == null) return 0L;
		return date.getTime();
	}

	@Override
	public String sendFloat(float f)
	{
		return Float.toString(f);
	}

	@Override
	public String sendFloat(Float f)
	{
		if (f == null) return "0";
		return f.toString();
	}

	@Override
	public String sendInt(int i)
	{
		return Integer.toString(i);
	}

	@Override
	public String sendInt(Integer i)
	{
		if (i == null) return "0";
		return i.toString();
	}

	@Override
	public String sendLong(long l)
	{
		return Long.toString(l);
	}

	@Override
	public String sendLong(Long l)
	{
		if (l == null) return "0";
		return l.toString();
	}

	@Override
	public boolean start()
	{
		M_log.info("CdpServiceImpl: start");
		return true;
	}

	@Override
	public void unregisterCdpHandler(CdpHandler handler)
	{
		this.handlers.remove(handler);
	}
}
