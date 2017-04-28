/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/threadlocal/webapp/ThreadLocalServiceImpl.java $
 * $Id: ThreadLocalServiceImpl.java 8293 2014-06-20 21:42:19Z ggolden $
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

package org.etudes.threadlocal.webapp;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.service.api.Service;
import org.etudes.threadlocal.api.ThreadLocalService;

/**
 * ThreadLocalServiceImpl implements ThreadLocalService.
 */
public class ThreadLocalServiceImpl implements ThreadLocalService, Service
{
	/** The threadlocal map. */
	protected static final ThreadLocal<Map<String, Object>> threadMap = new ThreadLocal<Map<String, Object>>()
	{
		@Override
		protected Map<String, Object> initialValue()
		{
			return new HashMap<String, Object>();
		}
	};

	/** Our log. */
	private static Log M_log = LogFactory.getLog(ThreadLocalServiceImpl.class);

	/**
	 * Construct
	 */
	public ThreadLocalServiceImpl()
	{
		M_log.info("ThreadLocalServiceImpl: construct");
	}

	@Override
	public void clear()
	{
		Map<String, Object> map = threadMap.get();
		map.clear();
	}

	@Override
	public Object get(String key)
	{
		Map<String, Object> map = threadMap.get();
		return map.get(key);
	}

	@Override
	public void put(String key, Object value)
	{
		if (value == null)
		{
			remove(key);
		}
		else
		{
			Map<String, Object> map = threadMap.get();
			map.put(key, value);
		}
	}

	@Override
	public void remove(String key)
	{
		Map<String, Object> map = threadMap.get();
		map.remove(key);
	}

	@Override
	public boolean start()
	{
		M_log.info("ThreadLocalServiceImpl: start");
		return true;
	}
}
