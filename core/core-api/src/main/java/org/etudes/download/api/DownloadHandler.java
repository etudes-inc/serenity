/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/download/api/DownloadHandler.java $
 * $Id: DownloadHandler.java 11568 2015-09-06 20:29:53Z ggolden $
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

package org.etudes.download.api;

import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

public interface DownloadHandler
{
	/**
	 * Authorize a download of an item held by this tool item.
	 * 
	 * @param authenticatedUser
	 *        The user making the request.
	 * @param holder
	 *        The holding tool item.
	 * @return true if authorized, false if not.
	 */
	boolean authorize(User authenticatedUser, ToolItemReference holder);
}
