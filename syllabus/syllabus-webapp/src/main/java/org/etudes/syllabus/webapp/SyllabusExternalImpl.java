/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/java/org/etudes/syllabus/webapp/SyllabusExternalImpl.java $
 * $Id: SyllabusExternalImpl.java 11454 2015-08-15 04:13:37Z ggolden $
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

import static org.etudes.util.Different.different;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;
import org.etudes.syllabus.api.SyllabusExternal;

/**
 * SyllabusExternalImpl implements SyllabusExternal.
 */
public class SyllabusExternalImpl implements SyllabusExternal
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SyllabusServiceImpl.class);

	protected boolean changed = false;

	protected Integer height = null;

	protected Boolean newWindow = Boolean.FALSE;

	protected String url = null;

	@Override
	public Integer getHeight()
	{
		return this.height;
	}

	@Override
	public Boolean getNewWindow()
	{
		return this.newWindow;
	}

	@Override
	public String getUrl()
	{
		return this.url;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		String url = cdpService().readString(parameters.get(prefix + "url"));
		Integer height = cdpService().readInt(parameters.get(prefix + "height"));
		String target = cdpService().readString(parameters.get(prefix + "target"));

		setUrl(url);
		setHeight(height);
		setNewWindow("W".equals(target));
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (getUrl() != null) rv.put("url", getUrl());
		if (getHeight() != null) rv.put("height", getHeight());
		rv.put("target", (getNewWindow() ? "W" : "I"));

		return rv;
	}

	@Override
	public void setHeight(Integer height)
	{
		if (different(height, this.height))
		{
			this.changed = true;
			this.height = height;
		}
	}

	@Override
	public void setNewWindow(Boolean newWindow)
	{
		if (newWindow == null) newWindow = Boolean.FALSE;
		if (different(newWindow, this.newWindow))
		{
			this.changed = true;
			this.newWindow = newWindow;
		}
	}

	@Override
	public void setUrl(String url)
	{
		if (different(url, this.url))
		{
			this.changed = true;
			this.url = url;
		}
	}

	/**
	 * Mark the blog as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected void initHeight(Integer height)
	{
		this.height = height;
	}

	protected void initNewWindow(Boolean newWindow)
	{
		if (newWindow == null) newWindow = Boolean.FALSE;
		this.newWindow = newWindow;
	}

	protected void initUrl(String url)
	{
		this.url = url;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}
