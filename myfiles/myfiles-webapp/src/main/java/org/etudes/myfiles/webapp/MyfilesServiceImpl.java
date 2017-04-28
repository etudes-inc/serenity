/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/myfiles/myfiles-webapp/src/main/java/org/etudes/myfiles/webapp/MyfilesServiceImpl.java $
 * $Id: MyfilesServiceImpl.java 11568 2015-09-06 20:29:53Z ggolden $
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

package org.etudes.myfiles.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.download.api.DownloadHandler;
import org.etudes.myfiles.api.MyfilesService;
import org.etudes.service.api.Service;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

public class MyfilesServiceImpl implements MyfilesService, Service, DownloadHandler
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(MyfilesServiceImpl.class);

	@Override
	public boolean authorize(User authenticatedUser, ToolItemReference holder)
	{
		if ((authenticatedUser != null) && (holder.getSite().getId() == 0L) && (holder.getTool() == Tool.myfiles))
		{
			if ((authenticatedUser.getId() == holder.getItemId()) || (authenticatedUser.isAdmin()))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean start()
	{
		M_log.info("MyfilesServiceImpl: start");
		return true;
	}
}
