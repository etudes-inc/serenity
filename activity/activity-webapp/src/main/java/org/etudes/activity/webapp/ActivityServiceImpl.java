/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/activity/activity-webapp/src/main/java/org/etudes/activity/webapp/ActivityServiceImpl.java $
 * $Id: ActivityServiceImpl.java 10433 2015-04-06 19:57:18Z ggolden $
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

package org.etudes.activity.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.sql.api.SqlService;
import org.etudes.activity.api.ActivityService;

/**
 * ActivityServiceImpl implements ActivityService.
 */
public class ActivityServiceImpl implements ActivityService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ActivityServiceImpl.class);

	/**
	 * Construct
	 */
	public ActivityServiceImpl()
	{
		M_log.info("ActivityServiceImpl: construct");
	}

	@Override
	public boolean start()
	{
		M_log.info("ActivityServiceImpl: start");
		return true;
	}

	/**
	 * @return The registered SqlService.
	 */
	private SqlService sqlService()
	{
		return (SqlService) Services.get(SqlService.class);
	}
}
